package com.yomahub.tlog.core.context;

/**
 * 日志切面的上下文，用于管理当前线程以及子线程的的增强内容
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
public class AspectLogContext {

    private static InheritableThreadLocal<String> logValueTL = new InheritableThreadLocal<>();

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
