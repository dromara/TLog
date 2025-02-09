package com.yomahub.tlog.web.common;

import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TLog web这块的逻辑封装类
 *
 * @author Bryan.Zhang
 * @since 1.1.5
 */
public class TLogWebCommon extends TLogRPCHandler {

    private final static Logger log = LoggerFactory.getLogger(TLogWebCommon.class);

    private static volatile TLogWebCommon tLogWebCommon;

    public static TLogWebCommon loadInstance() {
        if (tLogWebCommon == null) {
            synchronized (TLogWebCommon.class) {
                if (tLogWebCommon == null) {
                    tLogWebCommon = new TLogWebCommon();
                }
            }
        }
        return tLogWebCommon;
    }

    public void preHandle(HttpServletRequest request) {
        String traceId = StrUtil.nullToDefault(TLogContext.getTraceId(), request.getHeader(TLogConstants.TLOG_TRACE_KEY));
        String spanId = StrUtil.nullToDefault(TLogContext.getSpanId(), request.getHeader(TLogConstants.TLOG_SPANID_KEY));
        String preIvkApp = StrUtil.nullToDefault(TLogContext.getPreIvkApp(), request.getHeader(TLogConstants.PRE_IVK_APP_KEY));
        String preIvkHost = StrUtil.nullToDefault(TLogContext.getPreIvkHost(), request.getHeader(TLogConstants.PRE_IVK_APP_HOST));
        String preIp = StrUtil.nullToDefault(TLogContext.getPreIp(), request.getHeader(TLogConstants.PRE_IP_KEY));

        TLogLabelBean labelBean = new TLogLabelBean(preIvkApp, preIvkHost, preIp, traceId, spanId);

        processProviderSide(labelBean);
    }

    public void afterCompletion() {
        cleanThreadLocal();
    }
}
