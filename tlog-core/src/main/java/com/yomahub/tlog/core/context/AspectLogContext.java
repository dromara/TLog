package com.yomahub.tlog.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 日志切面的上下文，用于管理当前线程以及子线程的的增强内容
 * 这里有一段硬代码，为了适配log4j2的异步队列模式的日志
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
public class AspectLogContext {

    private static TransmittableThreadLocal<String> logValueTL = new TransmittableThreadLocal<>();

    public static void putLogValue(String logValue) {
        logValueTL.set(logValue);
    }

    public static String getLogValue() {
        return logValueTL.get();
    }

    public static void remove() {
        logValueTL.remove();
    }
}
