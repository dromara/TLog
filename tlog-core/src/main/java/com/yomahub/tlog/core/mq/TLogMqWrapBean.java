package com.yomahub.tlog.core.mq;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogPropagator;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
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
        boolean injected = TLogPropagator.inject((key, value) -> {
            switch (key) {
                case TLogConstants.TLOG_TRACE_KEY:
                    setTraceId(value);
                    break;
                case TLogConstants.TLOG_SPANID_KEY:
                    setSpanId(value);
                    break;
                case TLogConstants.PRE_IVK_APP_KEY:
                    setPreIvkApp(value);
                    break;
                case TLogConstants.PRE_IVK_APP_HOST:
                    setPreIvkHost(value);
                    break;
                case TLogConstants.PRE_IP_KEY:
                    setPreIp(value);
                    break;
                default:
                    break;
            }
        });
        if (!injected) {
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次发送不传递traceId");
        }
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }
}
