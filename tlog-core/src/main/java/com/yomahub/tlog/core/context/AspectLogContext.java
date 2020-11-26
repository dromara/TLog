package com.yomahub.tlog.core.context;

import com.yomahub.tlog.constant.TLogConstants;
import org.apache.logging.log4j.ThreadContext;

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
        if (isLog4j2AsyncLoggerContextSelector()) {
            ThreadContext.put(TLogConstants.T_LOG_LABEL, logValue);
        }
    }

    public static String getLogValue() {
        return logValueTL.get();
    }

    public static void remove() {
        logValueTL.remove();
        if (isLog4j2AsyncLoggerContextSelector()) {
            ThreadContext.remove(TLogConstants.T_LOG_LABEL);
        }
    }

    // 如果是log4j2开启了异步日志
    private static boolean isLog4j2AsyncLoggerContextSelector() {
        return "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
                .equals(System.getProperty("Log4jContextSelector"));
    }
}
