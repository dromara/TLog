package com.yomahub.tlog.context;

import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.utils.LocalhostUtil;

import java.util.function.BiConsumer;

/**
 * TLog 出站传播器：把当前上下文写入下游请求的 header / attachment
 * <p>
 * 所有客户端适配（Feign、RestTemplate、OkHttp、HttpClient、Dubbo、MQ 等）都通过这里写入，保证字段与格式一致
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogPropagator {

    private TLogPropagator() {
    }

    /**
     * 将当前 TLog 上下文写入下游
     *
     * @param writer header 写入函数，形如 {@code (name, value) -> request.header(name, value)}
     * @return 当前没有 traceId 时不写入并返回 false
     */
    public static boolean inject(BiConsumer<String, String> writer) {
        return inject(writer, TLogRuntime.getAppName());
    }

    /**
     * 将当前 TLog 上下文写入下游，并显式指定本应用名（Dubbo 等框架可从自身配置获得）
     */
    public static boolean inject(BiConsumer<String, String> writer, String appName) {
        if (!TLogRuntime.isEnabled()) {
            return false;
        }
        String traceId = TLogContext.getTraceId();
        if (StrUtil.isBlank(traceId)) {
            return false;
        }
        writer.accept(TLogConstants.TLOG_TRACE_KEY, traceId);
        writer.accept(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
        writer.accept(TLogConstants.PRE_IVK_APP_KEY, StrUtil.isBlank(appName) ? TLogConstants.UNKNOWN : appName);
        writer.accept(TLogConstants.PRE_IVK_APP_HOST, LocalhostUtil.getHostName());
        writer.accept(TLogConstants.PRE_IP_KEY, LocalhostUtil.getHostIp());
        return true;
    }
}
