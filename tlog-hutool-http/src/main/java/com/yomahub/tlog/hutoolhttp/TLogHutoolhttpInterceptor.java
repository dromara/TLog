package com.yomahub.tlog.hutoolhttp;

import cn.hutool.http.HttpBase;
import cn.hutool.http.HttpInterceptor;
import cn.hutool.http.HttpRequest;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hutool http组件的拦截器
 * @author Bryan.Zhang
 * @since 1.3.5
 */
public class TLogHutoolhttpInterceptor implements HttpInterceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(HttpBase httpBase) {
        String traceId = TLogContext.getTraceId();
        if(StringUtils.isNotBlank(traceId)) {
            String appName = TLogSpringAware.getProperty("spring.application.name");

            httpBase.header(TLogConstants.TLOG_TRACE_KEY, traceId);
            httpBase.header(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
            httpBase.header(TLogConstants.PRE_IVK_APP_KEY, appName);
            httpBase.header(TLogConstants.PRE_IVK_APP_HOST, LocalhostUtil.getHostName());
            httpBase.header(TLogConstants.PRE_IP_KEY, LocalhostUtil.getHostIp());
        } else {
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
        }
    }
}
