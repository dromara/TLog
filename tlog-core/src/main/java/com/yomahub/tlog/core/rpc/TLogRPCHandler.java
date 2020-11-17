package com.yomahub.tlog.core.rpc;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class TLogRPCHandler {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public void processProviderSide(TLogLabelBean labelBean){
        if(StringUtils.isBlank(labelBean.getPreIvkApp())){
            labelBean.setPreIvkApp(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIvkApp(labelBean.getPreIvkApp());

        if(StringUtils.isBlank(labelBean.getPreIp())){
            labelBean.setPreIp(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIp(labelBean.getPreIp());

        //如果从隐式传参里没有获取到，则重新生成一个traceId
        if(StringUtils.isBlank(labelBean.getTraceId())){
            labelBean.setTraceId(UniqueIdGenerator.generateStringId());
            log.debug("[TLOG]可能上一个节点[{}]没有没有正确传递traceId,重新生成traceId[{}]",labelBean.getPreIvkApp(),labelBean.getTraceId());
        }

        //往TLog上下文里放当前获取到的spanId，如果spanId为空，会放入初始值
        TLogContext.putSpanId(labelBean.getSpanId());

        //往TLog上下文里放一个当前的traceId
        TLogContext.putTraceId(labelBean.getSpanId());

        //生成日志标签
        String tlogLabel = TLogLabelGenerator.generateTLogLabel(labelBean.getPreIvkApp(),
                labelBean.getPreIp(),
                labelBean.getTraceId(),
                TLogContext.getSpanId());

        //往日志切面器里放一个日志前缀
        AspectLogContext.putLogValue(tlogLabel);

        //如果有MDC，则往MDC中放入日志标签
        if(TLogContext.hasTLogMDC()){
            MDC.put(TLogConstants.MDC_KEY, tlogLabel);
        }
    }

    public void cleanThreadLocal(){
        //移除ThreadLocal里的数据
        TLogContext.removePreIvkApp();
        TLogContext.removePreIp();
        TLogContext.removeTraceId();
        TLogContext.removeSpanId();
        AspectLogContext.remove();
        if(TLogContext.hasTLogMDC()){
            MDC.remove(TLogConstants.MDC_KEY);
        }
    }
}
