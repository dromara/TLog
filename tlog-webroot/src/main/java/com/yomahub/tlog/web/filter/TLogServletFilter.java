package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.TLogContextSnapshot;
import com.yomahub.tlog.web.common.TLogWebCommon;
import com.yomahub.tlog.web.common.TLogWebAsyncSupport;
import org.springframework.core.Ordered;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * javax.servlet 的 TLog 过滤器（Spring Boot 2.x / Servlet 3.1 ~ 4.0）
 * <p>
 * jakarta 版本见 {@link TLogJakartaServletFilter}，两者逻辑一致，由自动装配根据运行环境择一注册
 *
 * @author Bryan.Zhang
 * @since 1.3.5
 */
public class TLogServletFilter implements Filter, Ordered {

    private boolean responseHeaderEnabled = true;

    private boolean asyncContextEnabled = true;

    // Run after the encoding filter, while still preceding security filters.
    private int order = Ordered.HIGHEST_PRECEDENCE + 1;

    @Override
    public void init(FilterConfig filterConfig) {
    }

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
    public void destroy() {
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
