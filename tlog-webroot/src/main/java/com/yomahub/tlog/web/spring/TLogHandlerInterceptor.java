package com.yomahub.tlog.web.spring;

import com.yomahub.tlog.web.common.TLogWebCommon;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//这个类主要针对非springboot
public class TLogHandlerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        TLogWebCommon.preHandle(request, response, handler);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TLogWebCommon.afterCompletion(request, response, handler);
    }
}
