package com.yomahub.tlog.core.rpc;

import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.function.Function;

/**
 * TLog的RPC处理逻辑的封装类
 * <p>
 * 所有入站适配（web、webflux、dubbo、任务、MQ 等）都通过这里建立上下文，通过 {@link #cleanThreadLocal()} 清理
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogRPCHandler {

    protected static final Logger log = LoggerFactory.getLogger(TLogRPCHandler.class);

    /**
     * 是否从 W3C {@code traceparent} 头中沿用 traceId，由自动装配根据配置设置
     */
    private static volatile boolean w3cTraceContextEnabled = true;

    /**
     * 从 header 读取函数构建标签并建立上下文
     *
     * @param headerReader 按 header 名读取值，返回 null 表示不存在
     */
    public void processProviderSide(Function<String, String> headerReader) {
        processProviderSide(readLabelBean(headerReader));
    }

    /**
     * 从 header 读取函数构造标签对象，供需要附加 extData 的调用方使用
     */
    public static TLogLabelBean readLabelBean(Function<String, String> headerReader) {
        String traceId = headerReader.apply(TLogConstants.TLOG_TRACE_KEY);
        if (StrUtil.isBlank(traceId) && w3cTraceContextEnabled) {
            traceId = W3cTraceContext.parseTraceId(headerReader.apply(TLogConstants.W3C_TRACE_PARENT));
        }
        return new TLogLabelBean(headerReader.apply(TLogConstants.PRE_IVK_APP_KEY),
                headerReader.apply(TLogConstants.PRE_IVK_APP_HOST),
                headerReader.apply(TLogConstants.PRE_IP_KEY),
                traceId,
                headerReader.apply(TLogConstants.TLOG_SPANID_KEY));
    }

    public void processProviderSide(TLogLabelBean labelBean) {
        if (StrUtil.isBlank(labelBean.getPreIvkApp())) {
            labelBean.setPreIvkApp(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIvkApp(labelBean.getPreIvkApp());

        if (StrUtil.isBlank(labelBean.getPreIvkHost())) {
            labelBean.setPreIvkHost(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIvkHost(labelBean.getPreIvkHost());

        if (StrUtil.isBlank(labelBean.getPreIp())) {
            labelBean.setPreIp(TLogConstants.UNKNOWN);
        }
        TLogContext.putPreIp(labelBean.getPreIp());

        //如果没有获取到，则重新生成一个traceId
        if (StrUtil.isBlank(labelBean.getTraceId())) {
            labelBean.setTraceId(TLogIdGeneratorLoader.getIdGenerator().generate(labelBean.getExtData()));
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
        for (String key : TLogConstants.MDC_KEYS) {
            MDC.remove(key);
        }
    }

    public static boolean isW3cTraceContextEnabled() {
        return w3cTraceContextEnabled;
    }

    public static void setW3cTraceContextEnabled(boolean enabled) {
        w3cTraceContextEnabled = enabled;
    }
}
