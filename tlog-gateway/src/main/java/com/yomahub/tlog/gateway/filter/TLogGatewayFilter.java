package com.yomahub.tlog.gateway.filter;

import cn.hutool.core.net.NetUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.gateway.common.TLogGatewayCommon;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;

/**
 * gateway 的全局拦截器
 *
 * @author zs
 * @since 1.0.0
 */
public class TLogGatewayFilter implements GlobalFilter, Ordered {

    @Value("${spring.application.name}")
    private String appName;

    private static final Logger log = LoggerFactory.getLogger(TLogGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(TLogGatewayCommon.loadInstance().preHandle(exchange,appName));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
