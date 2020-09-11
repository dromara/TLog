package com.yomahub.tlog.web;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * web controller的拦截器
 * @author Bryan.Zhang
 * @date 2020/9/11
 */
public class TLogWebInterceptor implements HandlerInterceptor {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = request.getHeader(TLogConstants.TLOG_TRACE_KEY);
        String preIvkApp = request.getHeader(TLogConstants.PRE_IVK_APP_KEY);
        if(StringUtils.isBlank(preIvkApp)){
            preIvkApp = TLogConstants.UNKNOWN;
        }

        if(StringUtils.isBlank(traceId)){
            traceId = UniqueIdGenerator.generateHexId();
            log.warn("[TLOG]可能上一个节点[{}]没有没有正确传递traceId,重新生成traceId[{}]",preIvkApp,traceId);
        }
        TLogContext.putTraceId(traceId);
        AspectLogContext.putLogValue("<" + traceId + ">");
        return true;
    }
}
