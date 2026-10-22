package com.yomahub.tlog.web.interceptor;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.TLogContextSnapshot;
import com.yomahub.tlog.web.common.TLogWebCommon;
import com.yomahub.tlog.web.common.TLogWebAsyncSupport;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * web controller的拦截器（javax，供非 Spring Boot 的 Spring MVC 项目在 XML 中手工注册）
 * <p>
 * Spring Boot 项目由自动装配注册 Servlet Filter，无需使用本类
 *
 * @author Bryan.Zhang
 * @since 1.1.5
 */
public class TLogWebInterceptor extends AbsTLogWebHandlerMethodInterceptor implements AsyncHandlerInterceptor {

    @Override
    public boolean preHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object saved = request.getAttribute(TLogWebCommon.REQUEST_CONTEXT_ATTRIBUTE);
        TLogWebCommon common = TLogWebCommon.loadInstance();
        common.preHandle(request::getHeader, saved instanceof TLogContextSnapshot ? (TLogContextSnapshot) saved : null);
        try {
            if (!(saved instanceof TLogContextSnapshot)) {
                request.setAttribute(TLogWebCommon.REQUEST_CONTEXT_ATTRIBUTE, TLogContextSnapshot.capture());
            }
            TLogWebAsyncSupport.register(request);
            if (!response.containsHeader(TLogConstants.TLOG_TRACE_KEY)) {
                response.setHeader(TLogConstants.TLOG_TRACE_KEY, TLogContext.getTraceId());
            }
            return true;
        } catch (RuntimeException | Error failure) {
            common.afterCompletion();
            throw failure;
        }
    }

    @Override
    public void postHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletionByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TLogWebCommon.loadInstance().afterCompletion();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            TLogWebCommon.loadInstance().afterCompletion();
        }
    }
}
