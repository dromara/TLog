package com.yomahub.tlog.gateway.filter;

import com.yomahub.tlog.webflux.common.TLogWebFluxCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.function.Consumer;

/**
 * gateway 的全局拦截器
 *
 * @author zs
 * @since 1.2.4-BETA1
 */
public class TLogGatewayFilter implements GlobalFilter, Ordered {

    @Value("${spring.application.name}")
    private String appName;

    private static final Logger log = LoggerFactory.getLogger(TLogGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(TLogWebFluxCommon.loadInstance().preHandle(exchange, appName))
                .doFinally(signalType -> TLogWebFluxCommon.loadInstance().cleanThreadLocal());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
