package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.web.wrapper.RequestWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 使用重写后的httpServletRequestWrapper 替换 httpServletRequest
 *
 * @author jing.li
 * @since 1.2.5
 */
public class ReplaceStreamFilter implements Filter {

    private String[] excludedPathArray;

    private static final String DEFAULT_FILTER_EXCLUDED_PATHS = "/static/*,*.html,*.js,*.ico,*.jpg,*.png,*.css";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        excludedPathArray = DEFAULT_FILTER_EXCLUDED_PATHS.split(",");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (!isFilterExcludeRequest(httpServletRequest)) {
            ServletRequest requestWrapper = new RequestWrapper(httpServletRequest);
            chain.doFilter(requestWrapper, response);
        }

    }

    @Override
    public void destroy() {
    }

    /**
     * 判断是否是过滤器直接放行的请求(主要用于静态资源的放行)
     * @Param request http请求
     * @return boolean
     */
    private boolean isFilterExcludeRequest(HttpServletRequest request) {
        if (null != excludedPathArray && excludedPathArray.length > 0) {
            String url = request.getRequestURI();
            for (String ecludedUrl : excludedPathArray) {
                if (ecludedUrl.startsWith("*.")) {
                    // 如果配置的是后缀匹配, 则把前面的*号干掉，然后用endWith来判断
                    if (url.endsWith(ecludedUrl.substring(1))) {
                        return true;
                    }
                } else if (ecludedUrl.endsWith("/*")) {
                    if (!ecludedUrl.startsWith("/")) {
                        // 前缀匹配，必须要是/开头
                        ecludedUrl = "/" + ecludedUrl;

                    }
                    // 如果配置是前缀匹配, 则把最后的*号干掉，然后startWith来判断
                    String prffixStr = request.getContextPath() + ecludedUrl.substring(0, ecludedUrl.length() - 1);
                    if (url.startsWith(prffixStr)) {
                        return true;
                    }
                } else {
                    // 如果不是前缀匹配也不是后缀匹配,那就是全路径匹配
                    if (!ecludedUrl.startsWith("/")) {
                        // 全路径匹配，也必须要是/开头
                        ecludedUrl = "/" + ecludedUrl;
                    }
                    String targetUrl = request.getContextPath() + ecludedUrl;
                    if (url.equals(targetUrl)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
