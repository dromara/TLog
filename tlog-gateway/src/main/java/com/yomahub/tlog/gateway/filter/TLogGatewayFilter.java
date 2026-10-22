package com.yomahub.tlog.gateway.filter;

import com.yomahub.tlog.webflux.common.TLogWebFluxCommon;
import com.yomahub.tlog.webflux.context.TLogReactorContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * gateway 的全局拦截器
 *
 * @author zs
 * @since 1.2.4-BETA1
 */
public class TLogGatewayFilter implements GlobalFilter, Ordered {

    private boolean responseHeader = true;

    public TLogGatewayFilter() {
        this(true);
    }

    public TLogGatewayFilter(boolean responseHeader) {
        this.responseHeader = responseHeader;
        TLogReactorContext.install();
    }

    public void setResponseHeader(boolean responseHeader) {
        this.responseHeader = responseHeader;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return TLogWebFluxCommon.loadInstance().filter(exchange, chain::filter, responseHeader, true);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
