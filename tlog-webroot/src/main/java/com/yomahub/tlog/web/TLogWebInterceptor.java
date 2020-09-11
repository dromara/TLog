package com.yomahub.tlog.web;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * web controller的拦截器
 * @author Bryan.Zhang
 * @Date 2020/9/11
 */
public class TLogWebInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = request.getHeader(TLogConstants.TLOG_TRACE_KEY);
        if(StringUtils.isBlank(traceId)){
            traceId = UniqueIdGenerator.generateHexId();
            TLogContext.putTraceId(traceId);
        }
        AspectLogContext.putLogValue("<" + traceId + ">");
        return true;
    }
}
