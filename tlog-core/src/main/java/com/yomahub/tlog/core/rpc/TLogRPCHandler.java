package com.yomahub.tlog.core.rpc;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * TLog的RPC处理逻辑的封装类
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogRPCHandler {

    protected static final Logger log = LoggerFactory.getLogger(TLogRPCHandler.class);

    public void processProviderSide(TLogLabelBean labelBean) {
        if (StringUtils.isBlank(labelBean.getPreIvkApp())) {
            labelBean.setPreIvkApp(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIvkApp(labelBean.getPreIvkApp());

        if (StringUtils.isBlank(labelBean.getPreIvkHost())) {
            labelBean.setPreIvkHost(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIvkHost(labelBean.getPreIvkHost());

        if (StringUtils.isBlank(labelBean.getPreIp())) {
            labelBean.setPreIp(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIp(labelBean.getPreIp());

        //如果没有获取到，则重新生成一个traceId
        if (StringUtils.isBlank(labelBean.getTraceId())) {
            labelBean.setTraceId(TLogIdGeneratorLoader.getIdGenerator().withExtData(labelBean.getExtData()).generateTraceId());
            log.debug("[TLOG]可能上一个节点[{}]没有正确传递traceId,重新生成traceId[{}]", labelBean.getPreIvkApp(), labelBean.getTraceId());
        }

        //往TLog上下文里放当前获取到的spanId，如果spanId为空，会放入初始值
        TLogContext.putSpanId(labelBean.getSpanId());

        //往TLog上下文里放一个当前的traceId
        TLogContext.putTraceId(labelBean.getTraceId());

        //往TLog上下文里放当前的IP
        TLogContext.putCurrIp(LocalhostUtil.getHostIp());

        //生成日志标签
        String tlogLabel = TLogLabelGenerator.generateTLogLabel(labelBean.getPreIvkApp(),
                labelBean.getPreIvkHost(),
                labelBean.getPreIp(),
                TLogContext.getCurrIp(),
                labelBean.getTraceId(),
                TLogContext.getSpanId());

        //往日志切面器里放一个日志前缀
        AspectLogContext.putLogValue(tlogLabel);

        //目前无论是不是MDC，都往MDC里放参数，这样就避免了log4j2的特殊设置
        MDC.put(TLogConstants.MDC_KEY, tlogLabel);
        MDC.put(TLogConstants.TLOG_TRACE_KEY, TLogContext.getTraceId());
        MDC.put(TLogConstants.TLOG_SPANID_KEY, TLogContext.getSpanId());
        MDC.put(TLogConstants.CURR_IP_KEY, TLogContext.getCurrIp());
        MDC.put(TLogConstants.PRE_IP_KEY, TLogContext.getPreIp());
        MDC.put(TLogConstants.PRE_IVK_APP_HOST, TLogContext.getPreIvkHost());
        MDC.put(TLogConstants.PRE_IVK_APP_KEY, TLogContext.getPreIvkApp());
    }

    public void cleanThreadLocal() {
        //移除ThreadLocal里的数据
        TLogContext.removePreIvkApp();
        TLogContext.removePreIvkHost();
        TLogContext.removePreIp();
        TLogContext.removeCurrIp();
        TLogContext.removeTraceId();
        TLogContext.removeSpanId();
        AspectLogContext.remove();

        //移除MDC里的信息
        MDC.remove(TLogConstants.MDC_KEY);
        MDC.remove(TLogConstants.TLOG_TRACE_KEY);
        MDC.remove(TLogConstants.TLOG_SPANID_KEY);
        MDC.remove(TLogConstants.CURR_IP_KEY);
        MDC.remove(TLogConstants.PRE_IP_KEY);
        MDC.remove(TLogConstants.PRE_IVK_APP_HOST);
        MDC.remove(TLogConstants.PRE_IVK_APP_KEY);
    }
}
