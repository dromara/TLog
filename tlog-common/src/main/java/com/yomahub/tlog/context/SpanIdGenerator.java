package com.yomahub.tlog.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * spanId生成器
 * <p>
 * spanId 形如 {@code 0.1.2}：每向下游发起一次调用，子 spanId 为“当前 spanId.自增序号”。
 * 序号计数器随 TTL 传递到子线程并共享，保证同一请求内并发调用生成的子 spanId 不重复。
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class SpanIdGenerator {

    public static final String ROOT_SPAN_ID = "0";

    public static final TransmittableThreadLocal<String> currentSpanIdTL = new TransmittableThreadLocal<>();

    public static final TransmittableThreadLocal<AtomicInteger> spanIndex = new TransmittableThreadLocal<>();

    public static void putSpanId(String spanId) {
        if (StringUtils.isBlank(spanId)) {
            spanId = ROOT_SPAN_ID;
        }
        currentSpanIdTL.set(spanId);
        spanIndex.set(new AtomicInteger(0));
    }

    public static String getSpanId() {
        return currentSpanIdTL.get();
    }

    public static void removeSpanId() {
        currentSpanIdTL.remove();
        spanIndex.remove();
    }

    public static String generateNextSpanId() {
        String currentSpanId = getSpanId();
        AtomicInteger index = spanIndex.get();
        if (StringUtils.isBlank(currentSpanId) || index == null) {
            // 未经 TLog 入口初始化（例如手动设置了 traceId），从根 span 开始计数
            putSpanId(currentSpanId);
            currentSpanId = getSpanId();
            index = spanIndex.get();
        }
        return currentSpanId + "." + index.incrementAndGet();
    }
}
