package com.yomahub.tlog.webflux.common;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogPropagator;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.context.TLogContextSnapshot;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.webflux.context.TLogReactorContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * WebFlux / Gateway 的入站处理
 * <p>
 * 只调用 {@link HttpHeaders#getFirst(String)} 与 {@link HttpHeaders#set(String, String)}，
 * 这两个方法的描述符在 Spring 5 ~ 7 中一致（Spring 7 的 HttpHeaders 不再实现 MultiValueMap，{@code get(Object)} 已移除）
 *
 * @author zs
 * @since 1.3.0
 */
public class TLogWebFluxCommon extends TLogRPCHandler {

    private static final TLogWebFluxCommon INSTANCE = new TLogWebFluxCommon();

    public static TLogWebFluxCommon loadInstance() {
        return INSTANCE;
    }

    /**
     * 从请求头建立上下文，并把 TLog 头写入转发给下游的请求（网关场景）
     */
    public ServerWebExchange preHandle(ServerWebExchange exchange) {
        return preHandle(exchange, TLogRuntime.getAppName());
    }

    /** 建立独立请求快照，并在订阅和每次响应式回调期间恢复它。 */
    public Mono<Void> filter(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> chain,
                             boolean responseHeader) {
        return filter(exchange, chain, responseHeader, true);
    }

    /** 普通 WebFlux 入口不修改请求；只有网关转发时才写入出站头并分配子 span。 */
    public Mono<Void> filter(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> chain,
                             boolean responseHeader, boolean forwardHeaders) {
        TLogReactorContext.install();
        if (!TLogRuntime.isEnabled()) {
            return chain.apply(exchange);
        }
        try (TLogContextSnapshot.Scope ignored = TLogContextSnapshot.capture().restore()) {
            ServerWebExchange forwarded;
            if (forwardHeaders) {
                forwarded = preHandle(exchange);
            } else {
                establishContext(exchange);
                forwarded = exchange;
            }
            if (responseHeader) {
                exchange.getResponse().getHeaders().set(TLogConstants.TLOG_TRACE_KEY,
                        com.yomahub.tlog.context.TLogContext.getTraceId());
            }
            TLogContextSnapshot snapshot = TLogContextSnapshot.capture();
            return TLogReactorContext.withContext(Mono.defer(() -> chain.apply(forwarded)), snapshot);
        }
    }

    public ServerWebExchange preHandle(ServerWebExchange exchange, String appName) {
        establishContext(exchange);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(httpHeaders -> TLogPropagator.inject(httpHeaders::set, appName))
                .build();
        return exchange.mutate().request(request).build();
    }

    private void establishContext(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        TLogLabelBean labelBean = readLabelBean(headers::getFirst);
        labelBean.putExtData(TLogConstants.WEBFLUX_EXCHANGE, exchange);
        processProviderSide(labelBean);

    }

    /**
     * 仅建立上下文（插件 AOP 场景，不修改请求）
     */
    public void processExchange(ServerWebExchange exchange) {
        processProviderSide(exchange.getRequest().getHeaders()::getFirst);
    }
}
