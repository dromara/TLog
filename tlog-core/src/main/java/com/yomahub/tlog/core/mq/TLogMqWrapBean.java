package com.yomahub.tlog.core.mq;

import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;

/**
 * 针对于Mq的包装Bean
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogMqWrapBean<T> extends TLogLabelBean implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(TLogMqWrapBean.class);
    private static final long serialVersionUID = -9125086965312434222L;

    private T t;

    public TLogMqWrapBean() {
    }

    public TLogMqWrapBean(T t) {
        this.t = t;
        String traceId = TLogContext.getTraceId();

        if (StringUtils.isNotBlank(traceId)) {
            String appName = TLogSpringAware.getProperty("spring.application.name");

            this.setTraceId(traceId);
            this.setPreIvkApp(appName);
            this.setPreIvkHost(LocalhostUtil.getHostName());
            this.setPreIp(LocalhostUtil.getHostIp());
            this.setSpanId(SpanIdGenerator.generateNextSpanId());
        } else {
            log.warn("[TLOG]本地kafka客户端没有正确传递traceId,本次发送不传递traceId");
        }
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }
}
