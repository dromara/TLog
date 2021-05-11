package com.yomahub.tlog.core.context;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.yomahub.tlog.constant.TLogConstants;
import org.apache.logging.log4j.ThreadContext;
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

    private static Boolean isLog4j2AsyncLoggerContextSelector;

    public static void putLogValue(String logValue) {
        logValueTL.set(logValue);
        if (isLog4j2AsyncLoggerContextSelector()) {
            ThreadContext.put(TLogConstants.MDC_KEY, logValue);
        }
    }

    public static String getLogValue() {
        String result = logValueTL.get();
        if (StrUtil.isBlank(result) && isLog4j2AsyncLoggerContextSelector()){
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
        if (ObjectUtil.isNull(isLog4j2AsyncLoggerContextSelector)){
            boolean flag1 = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
                    .equals(System.getProperty("Log4jContextSelector"));

            boolean flag2;
            try{
                Class.forName("org.apache.logging.log4j.core.pattern.LogEventPatternConverter");
                flag2 = true;
            } catch (Exception e){
                flag2 = false;
            }

            boolean result = flag1 || flag2;
            isLog4j2AsyncLoggerContextSelector = result;
        }
        return isLog4j2AsyncLoggerContextSelector;
    }
}
