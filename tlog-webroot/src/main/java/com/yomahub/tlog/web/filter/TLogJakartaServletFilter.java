package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.TLogContextSnapshot;
import com.yomahub.tlog.web.common.TLogWebCommon;
import com.yomahub.tlog.web.common.TLogWebAsyncSupport;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * jakarta.servlet 的 TLog 过滤器（Spring Boot 3.x / 4.x，Servlet 5.0 ~ 6.1）
 * <p>
 * 与 {@link TLogServletFilter} 逻辑一致；{@code init/destroy} 在 Servlet 5+ 中为 default 方法，无需实现
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogJakartaServletFilter implements Filter, Ordered {

    private boolean responseHeaderEnabled = true;

    private boolean asyncContextEnabled = true;

    // Run after the encoding filter, while still preceding security filters.
    private int order = Ordered.HIGHEST_PRECEDENCE + 1;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        TLogWebCommon common = TLogWebCommon.loadInstance();
        Object savedContext = httpRequest.getAttribute(TLogWebCommon.REQUEST_CONTEXT_ATTRIBUTE);
        common.preHandle(httpRequest::getHeader,
                savedContext instanceof TLogContextSnapshot ? (TLogContextSnapshot) savedContext : null);
        long start = -1L;
        try {
            if (!(savedContext instanceof TLogContextSnapshot)) {
                // Keep the first trace/span for ASYNC and ERROR redispatches of this request.
                httpRequest.setAttribute(TLogWebCommon.REQUEST_CONTEXT_ATTRIBUTE, TLogContextSnapshot.capture());
            }
            if (asyncContextEnabled) {
                TLogWebAsyncSupport.register(httpRequest);
            }
            if (TLogContext.enableInvokeTimePrint()) {
                start = common.beforeInvoke(httpRequest.getRequestURI(), httpRequest.getParameterMap());
            }
            if (responseHeaderEnabled && !httpResponse.containsHeader(TLogConstants.TLOG_TRACE_KEY)) {
                httpResponse.setHeader(TLogConstants.TLOG_TRACE_KEY, TLogContext.getTraceId());
            }
            chain.doFilter(request, response);
        } finally {
            try {
                common.afterInvoke(httpRequest.getRequestURI(), start);
            } finally {
                common.afterCompletion();
            }
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setAsyncContextEnabled(boolean asyncContextEnabled) {
        this.asyncContextEnabled = asyncContextEnabled;
    }

    public void setResponseHeaderEnabled(boolean responseHeaderEnabled) {
        this.responseHeaderEnabled = responseHeaderEnabled;
    }
}
