package com.yomahub.tlog.webflux.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.webflux.client.TLogWebClientFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogWebFluxFilterTest {

    @AfterEach
    void clean() {
        TLogRuntime.setEnabled(true);
        new TLogRPCHandler().cleanThreadLocal();
    }

    @Test
    void readsHeadersWithoutRewritingTheInboundRequestAndCleansUp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/x").header(TLogConstants.TLOG_TRACE_KEY, "t1")
                        .header(TLogConstants.TLOG_SPANID_KEY, "0.2")
                        .header(TLogConstants.PRE_IVK_APP_KEY, "upstream-app"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        AtomicReference<String> traceInChain = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            forwarded.set(ex);
            traceInChain.set(TLogContext.getTraceId());
            return Mono.empty();
        };

        new TLogWebFluxFilter().filter(exchange, chain).block();

        assertEquals("t1", traceInChain.get());
        // 普通应用仍看到原始入站信息，只有真正的客户端调用才应生成子 span。
        assertEquals("t1", forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.2", forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
        assertEquals("upstream-app", forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.PRE_IVK_APP_KEY));
        assertNull(TLogContext.getTraceId(), "请求结束后应清理上下文");
    }

    @Test
    void generatesTraceIdWhenAbsent() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        new TLogWebFluxFilter().filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();
        assertNull(forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        assertNotNull(exchange.getResponse().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
    }

    @Test
    void interleavedDeferredRequestsDoNotShareThreadContext() {
        TLogWebFluxFilter filter = new TLogWebFluxFilter();
        MockServerWebExchange a = exchange("A");
        MockServerWebExchange b = exchange("B");
        Mono<Void> first = filter.filter(a, ex -> Mono.fromRunnable(() -> assertEquals("A", TLogContext.getTraceId())));
        Mono<Void> second = filter.filter(b, ex -> Mono.fromRunnable(() -> assertEquals("B", TLogContext.getTraceId())));
        assertNull(TLogContext.getTraceId(), "组装两个请求不应污染调用线程");
        first.block();
        second.block();
        assertNull(TLogContext.getTraceId());
        assertEquals("A", a.getResponse().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
    }

    @Test
    void webClientPropagatesAfterAnAsynchronousBoundary() {
        AtomicReference<String> traceSent = new AtomicReference<>();
        AtomicReference<String> spanSent = new AtomicReference<>();
        WebClient client = WebClient.builder().filter(new TLogWebClientFilter())
                .exchangeFunction(request -> {
                    traceSent.set(request.headers().getFirst(TLogConstants.TLOG_TRACE_KEY));
                    spanSent.set(request.headers().getFirst(TLogConstants.TLOG_SPANID_KEY));
                    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                }).build();
        new TLogWebFluxFilter().filter(exchange("upstream"), ex -> {
            assertNull(ex.getRequest().getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
            return Mono.delay(Duration.ofMillis(1)).publishOn(Schedulers.parallel())
                    .flatMap(tick -> client.get().uri("http://unused.example/").retrieve().bodyToMono(Void.class));
        })
                .block(Duration.ofSeconds(5));
        assertEquals("upstream", traceSent.get());
        assertEquals("0.1", spanSent.get(), "WebFlux 入站不能提前占用第一次出站调用的 span");
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void responseHeaderAndGlobalSwitchAreRespected() {
        TLogWebFluxFilter filter = new TLogWebFluxFilter();
        filter.setResponseHeader(false);
        MockServerWebExchange exchange = exchange("hidden");
        filter.filter(exchange, ex -> Mono.empty()).block();
        assertNull(exchange.getResponse().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        TLogRuntime.setEnabled(false);
        filter.filter(exchange("disabled"), ex -> {
            assertNull(TLogContext.getTraceId());
            assertNull(ex.getRequest().getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
            return Mono.empty();
        }).block();
    }

    @Test
    void pluginGatewayModeAllocatesOneChildSpanForTheForwardedRequest() {
        TLogWebFluxFilter filter = new TLogWebFluxFilter();
        filter.setForwardHeaders(true);
        filter.filter(exchange("gateway"), forwarded -> {
            assertEquals("0", TLogContext.getSpanId());
            assertEquals("gateway", forwarded.getRequest().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
            assertEquals("0.1", forwarded.getRequest().getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
            return Mono.empty();
        }).block();
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void ordinaryWebFluxStillPassesTheOriginalExchangeToCustomIdGenerators() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/custom"));
        TLogIdGenerator previous = TLogIdGeneratorLoader.getIdGenerator();
        TLogIdGeneratorLoader.setIdGenerator(new TLogIdGenerator() {
            @Override
            public String generateTraceId() {
                assertSame(exchange, getExtData().get(TLogConstants.WEBFLUX_EXCHANGE));
                return "custom-webflux";
            }
        });
        try {
            new TLogWebFluxFilter().filter(exchange, incoming -> {
                assertEquals("custom-webflux", TLogContext.getTraceId());
                assertNull(incoming.getRequest().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
                return Mono.empty();
            }).block();
            assertEquals("custom-webflux", exchange.getResponse().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        } finally {
            TLogIdGeneratorLoader.setIdGenerator(previous);
        }
    }

    private MockServerWebExchange exchange(String trace) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/test")
                .header(TLogConstants.TLOG_TRACE_KEY, trace));
    }
}
