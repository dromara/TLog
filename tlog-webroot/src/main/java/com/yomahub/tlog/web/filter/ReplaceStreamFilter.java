package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.web.wrapper.RequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 使用重写后的httpServletRequestWrapper 替换 httpServletRequest
 * @author jing.li
 * @since 1.2.5
 */
public class ReplaceStreamFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ReplaceStreamFilter.class);
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest requestWrapper = new RequestWrapper((HttpServletRequest) request);
        chain.doFilter(requestWrapper, response);
    }

    @Override
    public void destroy() {
    }
}
