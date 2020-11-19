package com.yomahub.tlog.web.common;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.core.enhance.logback.AspectLogbackMDCConverter;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TLog web这块的逻辑封装类
 *
 * @author Bryan.Zhang
 * @since 1.1.5
 */
public class TLogWebCommon extends TLogRPCHandler {

    private static Logger log = LoggerFactory.getLogger(TLogWebCommon.class);

    private static TLogWebCommon tLogWebCommon;

    public static TLogWebCommon loadInstace() {
        if (tLogWebCommon == null) {
            tLogWebCommon = new TLogWebCommon();
        }
        return tLogWebCommon;
    }

    public void preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            String traceId = request.getHeader(TLogConstants.TLOG_TRACE_KEY);
            String spanId = request.getHeader(TLogConstants.TLOG_SPANID_KEY);
            String preIvkApp = request.getHeader(TLogConstants.PRE_IVK_APP_KEY);
            String preIp = request.getHeader(TLogConstants.PRE_IP_KEY);

            TLogLabelBean labelBean = new TLogLabelBean(preIvkApp, preIp, traceId, spanId);

            processProviderSide(labelBean);
        }
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            cleanThreadLocal();
        }
    }
}
