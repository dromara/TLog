package com.yomahub.tlog.okhttp;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.utils.LocalhostUtil;
import okhttp3.Interceptor;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * OkHttp的拦截器
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public class TLogOkHttpInterceptor implements Interceptor {
    
    private final Logger log = LoggerFactory.getLogger(TLogOkHttpInterceptor.class);
    
    @Override
    public Response intercept(final Chain chain) throws IOException {
        Builder builder = chain.request().newBuilder();
        String traceId = TLogContext.getTraceId();
        if (StringUtils.isNotBlank(traceId)) {
            String appName = TLogSpringAware.getProperty("spring.application.name");
            builder.header(TLogConstants.TLOG_TRACE_KEY, traceId);
            builder.header(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
            builder.header(TLogConstants.PRE_IVK_APP_KEY, appName);
            builder.header(TLogConstants.PRE_IVK_APP_HOST, LocalhostUtil.getHostName());
            builder.header(TLogConstants.PRE_IP_KEY, LocalhostUtil.getHostIp());
        } else {
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
        }

        return chain.proceed(builder.build());
    }
}
