package com.yomahub.tlog.core.context;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 一个请求的 TLog 上下文快照，可在其他线程临时恢复。
 * 只操作 TLog 自身的 ThreadLocal 和 MDC 字段，不改变业务的其他上下文。
 * span 计数器保留原引用，使同一请求的并行分支不会生成重复的子 span。
 */
public final class TLogContextSnapshot {

    private final String traceId = TLogContext.getTraceId();
    private final String spanId = TLogContext.getSpanId();
    private final AtomicInteger spanIndex = SpanIdGenerator.spanIndex.get();
    private final String preApp = TLogContext.getPreIvkApp();
    private final String preHost = TLogContext.getPreIvkHost();
    private final String preIp = TLogContext.getPreIp();
    private final String currIp = TLogContext.getCurrIp();
    private final String label = AspectLogContext.getLogValue();
    private final Map<String, String> mdc = new HashMap<>();

    private TLogContextSnapshot() {
        for (String key : TLogConstants.MDC_KEYS) {
            mdc.put(key, MDC.get(key));
        }
    }

    public static TLogContextSnapshot capture() {
        return new TLogContextSnapshot();
    }

    /** 安装此快照；关闭返回的作用域时恢复当前线程先前的 TLog 上下文。 */
    public Scope restore() {
        Scope scope = new Scope(capture());
        apply();
        return scope;
    }

    private void apply() {
        set(traceId, TLogContext::putTraceId, TLogContext::removeTraceId);
        set(spanId, SpanIdGenerator.currentSpanIdTL::set, SpanIdGenerator.currentSpanIdTL::remove);
        if (spanIndex == null) {
            SpanIdGenerator.spanIndex.remove();
        } else {
            SpanIdGenerator.spanIndex.set(spanIndex);
        }
        set(preApp, TLogContext::putPreIvkApp, TLogContext::removePreIvkApp);
        set(preHost, TLogContext::putPreIvkHost, TLogContext::removePreIvkHost);
        set(preIp, TLogContext::putPreIp, TLogContext::removePreIp);
        set(currIp, TLogContext::putCurrIp, TLogContext::removeCurrIp);
        set(label, AspectLogContext::putLogValue, AspectLogContext::remove);
        for (Map.Entry<String, String> entry : mdc.entrySet()) {
            if (entry.getValue() == null) {
                MDC.remove(entry.getKey());
            } else {
                MDC.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void set(String value, Consumer<String> setter, Runnable remover) {
        if (value == null) {
            remover.run();
        } else {
            setter.accept(value);
        }
    }

    /** 作用域须在调用 restore 的线程按嵌套顺序关闭。 */
    public static final class Scope implements AutoCloseable {
        private TLogContextSnapshot previous;

        private Scope(TLogContextSnapshot previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous != null) {
                previous.apply();
                previous = null;
            }
        }
    }
}
