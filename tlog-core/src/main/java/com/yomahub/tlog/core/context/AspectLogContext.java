package com.yomahub.tlog.core.context;

import com.yomahub.tlog.constant.TLogConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

/**
 * 日志切面的上下文，用于管理当前线程以及子线程的的增强内容
 * 这里有一段硬代码，为了适配log4j2的异步队列模式的日志
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
public class AspectLogContext {

    private static InheritableThreadLocal<String> logValueTL = new InheritableThreadLocal<>();

    public static void putLogValue(String logValue) {
        logValueTL.set(logValue);
        if (isLog4j2AsyncLoggerContextSelector()) {
            ThreadContext.put(TLogConstants.MDC_KEY, logValue);
        }
    }

    public static String getLogValue() {
        String result = logValueTL.get();
        if (isLog4j2AsyncLoggerContextSelector()){
            result = ThreadContext.get(TLogConstants.MDC_KEY);
        }
        return result;
    }

    public static void remove() {
        logValueTL.remove();
        if (isLog4j2AsyncLoggerContextSelector()) {
            ThreadContext.remove(TLogConstants.MDC_KEY);
        }
    }

    // 如果是log4j2开启了异步日志,或者存在log4j2的包
    private static boolean isLog4j2AsyncLoggerContextSelector() {
        boolean flag1 = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
                .equals(System.getProperty("Log4jContextSelector"));

        boolean flag2;
        try{
            Class.forName("org.apache.logging.log4j.core.pattern.LogEventPatternConverter");
            flag2 = true;
        } catch (Exception e){
            flag2 = false;
        }

        return flag1 || flag2;
    }
}
