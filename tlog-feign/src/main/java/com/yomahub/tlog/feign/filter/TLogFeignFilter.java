package com.yomahub.tlog.feign.filter;


import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * feign的拦截器
 * @author Bryan.Zhang
 * @Date 2020/9/11
 */
public class TLogFeignFilter implements RequestInterceptor {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String traceId = TLogContext.getTraceId();

        if(StringUtils.isNotBlank(traceId)){
            requestTemplate.header(TLogConstants.TLOG_TRACE_KEY,traceId);
        }else{
            log.warn("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
        }
    }
}
