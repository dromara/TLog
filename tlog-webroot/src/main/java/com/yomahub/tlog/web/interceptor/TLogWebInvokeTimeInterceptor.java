package com.yomahub.tlog.web.interceptor;

import com.yomahub.tlog.web.common.TLogWebCommon;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TLog web的调用时间统计拦截器（javax，供非 Spring Boot 的 Spring MVC 项目手工注册）
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogWebInvokeTimeInterceptor extends AbsTLogWebHandlerMethodInterceptor {

    private static final String START_ATTR = TLogWebInvokeTimeInterceptor.class.getName() + ".START";

    @Override
    public boolean preHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long start = TLogWebCommon.loadInstance().beforeInvoke(request.getRequestURI(), request.getParameterMap());
        if (start >= 0) {
            // 存在 request 属性中而不是 ThreadLocal，异步请求在其他线程完成时也能正确取到
            request.setAttribute(START_ATTR, start);
        }
        return true;
    }

    @Override
    public void postHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletionByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (start instanceof Long) {
            request.removeAttribute(START_ATTR);
            TLogWebCommon.loadInstance().afterInvoke(request.getRequestURI(), (Long) start);
        }
    }
}
