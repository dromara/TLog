package com.yomahub.tlog.web;

import com.yomahub.aspectlog.context.AspectLogContext;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TLogWebInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = request.getHeader(TLogConstants.TLOG_TRACE_KEY);
        if(StringUtils.isBlank(traceId)){
            traceId = UniqueIdGenerator.generateHexId();
        }
        AspectLogContext.putLogValue(traceId);
        return true;
    }
}
