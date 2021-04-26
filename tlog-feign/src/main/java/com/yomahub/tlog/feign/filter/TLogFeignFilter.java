package com.yomahub.tlog.feign.filter;

import cn.hutool.core.net.NetUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;

/**
 * feign的拦截器
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogFeignFilter implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TLogFeignFilter.class);

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String traceId = TLogContext.getTraceId();

        if(StringUtils.isNotBlank(traceId)){
            String hostName = TLogConstants.UNKNOWN;
            try{
                hostName = NetUtil.getLocalHostName();
            }catch (Exception e){}

            requestTemplate.header(TLogConstants.TLOG_TRACE_KEY, traceId);
            requestTemplate.header(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
            requestTemplate.header(TLogConstants.PRE_IVK_APP_KEY, appName);
            requestTemplate.header(TLogConstants.PRE_IVK_APP_HOST, hostName);
            requestTemplate.header(TLogConstants.PRE_IP_KEY, NetUtil.getLocalhostStr());
        }else{
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
        }
    }
}
