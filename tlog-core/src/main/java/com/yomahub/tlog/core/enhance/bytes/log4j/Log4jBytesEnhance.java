package com.yomahub.tlog.core.enhance.bytes.log4j;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Category;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RendererSupport;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Log4j的字节增强器
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
public class Log4jBytesEnhance {

    private static Field headFilterField;

    private static Field field;

    public static void enhance(LoggingEvent event, boolean closed, String appenderName, AppenderSkeleton thisInstance) {
        String resultLog;
        if (StringUtils.isNotBlank(AspectLogContext.getLogValue())) {
            resultLog = StrUtil.format("{} {}", AspectLogContext.getLogValue(), event.getMessage());
        } else {
            resultLog = (String) event.getMessage();
        }

        if (field == null) {
            field = ReflectUtil.getField(LoggingEvent.class, "message");
            field.setAccessible(true);
        }

        try {
            field.set(event, resultLog);
        } catch (IllegalAccessException e) {
        }


        if (closed) {
            LogLog.error("Attempted to append to closed appender named [" + appenderName + "].");
            return;
        }

        if (!thisInstance.isAsSevereAsThreshold(event.getLevel())) {
            return;
        }

        if (headFilterField == null) {
            headFilterField = ReflectUtil.getField(thisInstance.getClass(), "headFilter");
        }

        Filter f = (Filter) ReflectUtil.getFieldValue(thisInstance, headFilterField);

        FILTER_LOOP:
        while (f != null) {
            switch (f.decide(event)) {
                case Filter.DENY:
                    return;
                case Filter.ACCEPT:
                    break FILTER_LOOP;
                case Filter.NEUTRAL:
                    f = f.getNext();
            }
        }

        //这里为什么不缓存appendMethod对象，是因为日志一开始ConsoleAppender的父类覆盖了append方法，这样一来理论上每次method对象还不一样
        //这可能会导致Log4j的异步日志增加反射消耗，这里要怎么改进呢，暂时没想到
        Method appendMethod = ReflectUtil.getMethodByName(thisInstance.getClass(), "append");
        try {
            appendMethod.invoke(thisInstance, event);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
