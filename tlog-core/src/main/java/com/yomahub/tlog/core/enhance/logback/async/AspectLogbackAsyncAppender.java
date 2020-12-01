package com.yomahub.tlog.core.enhance.logback.async;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.HashSet;

/**
 * Logback的异步日志增强appender
 *
 * @author Bryan.Zhang
 * @since 1.1.1
 */
public class AspectLogbackAsyncAppender extends AsyncAppender {

    private Field field;

    private static final Cache<LoggingEvent, Integer> lruCache = CacheUtil.newLRUCache(2048);

    @Override
    protected void append(ILoggingEvent eventObject) {
        if(eventObject instanceof LoggingEvent){
            LoggingEvent loggingEvent = (LoggingEvent)eventObject;

            if (!lruCache.containsKey(loggingEvent)){
                String resultLog;

                if(StringUtils.isNotBlank(AspectLogContext.getLogValue())){
                    resultLog = StrUtil.format("{} {}", AspectLogContext.getLogValue(),loggingEvent.getFormattedMessage());
                    if(field == null){
                        field = ReflectUtil.getField(LoggingEvent.class,"formattedMessage");
                        field.setAccessible(true);
                    }

                    try {
                        field.set(loggingEvent,resultLog);
                    } catch (IllegalAccessException e) {
                    }
                }
                eventObject = loggingEvent;
                lruCache.put(loggingEvent, Integer.MIN_VALUE);
            }
        }
        super.append(eventObject);
    }
}
