package com.yomahub.tlog.webflux.filter;

import com.yomahub.tlog.webflux.common.TLogWebFluxCommon;
import com.yomahub.tlog.webflux.context.TLogReactorContext;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * webflux 的全局拦截器
 *
 * @author naah
 * @since 1.3.0
 */
public class TLogWebFluxFilter implements WebFilter, Ordered {

    private boolean responseHeader = true;
    private boolean forwardHeaders;

    public TLogWebFluxFilter() {
        TLogReactorContext.install();
    }

    public void setResponseHeader(boolean responseHeader) {
        this.responseHeader = responseHeader;
    }

    /** Soul / ShenYu 等真正转发 HTTP 请求的网关使用此模式。 */
    public void setForwardHeaders(boolean forwardHeaders) {
        this.forwardHeaders = forwardHeaders;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return TLogWebFluxCommon.loadInstance().filter(exchange, chain::filter, responseHeader, forwardHeaders);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
