package com.yomahub.tlog.httpclient;

import cn.hutool.core.net.NetUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TLogHttpClientInterceptor implements HttpRequestInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(TLogHttpClientInterceptor.class);
    
    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        String traceId = TLogContext.getTraceId();
        if(StringUtils.isNotBlank(traceId)) {
            String appName = TLogSpringAware.getProperty("spring.application.name");

            request.addHeader(TLogConstants.TLOG_TRACE_KEY, traceId);
            request.addHeader(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
            request.addHeader(TLogConstants.PRE_IVK_APP_KEY, appName);
            request.addHeader(TLogConstants.PRE_IVK_APP_HOST, LocalhostUtil.getHostName());
            request.addHeader(TLogConstants.PRE_IP_KEY, LocalhostUtil.getHostIp());
        } else {
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
        }
    }
}