package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.web.common.TLogWebCommon;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 支持servlet
 * @author Bryan.Zhang
 * @since 1.3.5
 */
public class TLogServletFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse){
            TLogWebCommon.loadInstance().preHandle((HttpServletRequest)request);
            //把traceId放入response的header，为了方便有些人有这样的需求，从前端拿整条链路的traceId
            ((HttpServletResponse)response).addHeader(TLogConstants.TLOG_TRACE_KEY, TLogContext.getTraceId());
        }
        chain.doFilter(request, response);

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            // 放置在doFilter后面, 否则未进入方法就清空了
            TLogWebCommon.loadInstance().afterCompletion();
        }
    }

    @Override
    public void destroy() {

    }
}
