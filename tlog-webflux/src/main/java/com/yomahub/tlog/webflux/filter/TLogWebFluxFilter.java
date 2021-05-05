package com.yomahub.tlog.webflux.filter;
import com.yomahub.tlog.webflux.common.TLogWebFluxCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.function.Consumer;

/**
 * webflux 的全局拦截器
 *
 * @author naah
 * @since 1.3.0
 */
public class TLogWebFluxFilter implements WebFilter, Ordered {

    @Value("${spring.application.name}")
    private String appName;

    private static final Logger log = LoggerFactory.getLogger(TLogWebFluxFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(TLogWebFluxCommon.loadInstance().preHandle(exchange, appName))
                .doFinally(signalType -> TLogWebFluxCommon.loadInstance().cleanThreadLocal());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
