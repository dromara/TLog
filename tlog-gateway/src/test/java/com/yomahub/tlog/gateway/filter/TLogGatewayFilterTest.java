package com.yomahub.tlog.gateway.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogGatewayFilterTest {

    @BeforeEach
    @AfterEach
    void reset() {
        TLogRuntime.setEnabled(true);
        new TLogRPCHandler().cleanThreadLocal();
    }

    @Test
    void forwardsHeadersAndChildSpanWhileRestoringTheCallingThread() {
        TLogGatewayFilter filter = new TLogGatewayFilter();
        MockServerWebExchange exchange = exchange("gateway-request");
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        TLogContext.putTraceId("caller");
        MDC.put(TLogConstants.TLOG_TRACE_KEY, "caller");
        Mono<Void> result = filter.filter(exchange, next -> {
            forwarded.set(next);
            assertEquals("gateway-request", TLogContext.getTraceId());
            assertEquals("gateway-request", MDC.get(TLogConstants.TLOG_TRACE_KEY));
            assertEquals("0.4", TLogContext.getSpanId());
            return Mono.empty();
        });
        assertEquals("caller", TLogContext.getTraceId(), "过滤器组装阶段不能泄漏请求上下文");
        result.block();

        assertEquals("gateway-request", forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.4.1", forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
        assertNotNull(forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.PRE_IVK_APP_KEY));
        assertNotNull(forwarded.get().getRequest().getHeaders().getFirst(TLogConstants.PRE_IP_KEY));
        assertEquals("gateway-request", exchange.getResponse().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("caller", TLogContext.getTraceId());
        assertEquals("caller", MDC.get(TLogConstants.TLOG_TRACE_KEY));
    }

    @Test
    void responseHeaderCanBeDisabledWithoutDisablingDownstreamPropagation() {
        TLogGatewayFilter filter = new TLogGatewayFilter(false);
        MockServerWebExchange exchange = exchange("hidden-response");
        filter.filter(exchange, next -> {
            assertEquals("hidden-response", next.getRequest().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
            assertEquals("0.4.1", next.getRequest().getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
            return Mono.empty();
        }).block();
        assertNull(exchange.getResponse().getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void concurrentGatewayRequestsKeepMdcAcrossSchedulersAndLeaveNoWorkerState() throws Exception {
        TLogGatewayFilter filter = new TLogGatewayFilter();
        Scheduler worker = Schedulers.newSingle("tlog-gateway-test");
        try {
            List<Mono<Void>> requests = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                String trace = "gateway-" + i;
                requests.add(filter.filter(exchange(trace), next -> Mono.defer(() -> {
                    assertEquals(trace, TLogContext.getTraceId());
                    assertEquals(trace, MDC.get(TLogConstants.TLOG_TRACE_KEY));
                    assertEquals("0.4.1", next.getRequest().getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
                    return Mono.delay(Duration.ofMillis(1), worker).publishOn(worker)
                            .doOnNext(tick -> {
                                assertEquals(trace, TLogContext.getTraceId());
                                assertEquals(trace, MDC.get(TLogConstants.TLOG_TRACE_KEY));
                            }).then();
                })).subscribeOn(worker));
            }
            Flux.merge(requests).blockLast(Duration.ofSeconds(10));
            CompletableFuture<String> leftover = new CompletableFuture<>();
            worker.schedule(() -> leftover.complete(TLogContext.getTraceId() + ":" + MDC.get(TLogConstants.TLOG_TRACE_KEY)));
            assertEquals("null:null", leftover.get(5, TimeUnit.SECONDS));
            assertNull(TLogContext.getTraceId());
            assertNull(MDC.get(TLogConstants.TLOG_TRACE_KEY));
        } finally {
            worker.dispose();
        }
    }

    private MockServerWebExchange exchange(String trace) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/route")
                .header(TLogConstants.TLOG_TRACE_KEY, trace)
                .header(TLogConstants.TLOG_SPANID_KEY, "0.4"));
    }
}
