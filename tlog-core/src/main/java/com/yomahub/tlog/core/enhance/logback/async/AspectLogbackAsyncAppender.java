package com.yomahub.tlog.core.enhance.logback.async;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * Logback的异步日志增强appender
 *
 * @author Bryan.Zhang
 * @since 1.1.1
 */
public class AspectLogbackAsyncAppender extends AsyncAppender {

    private Field field;

    @Override
    protected void append(ILoggingEvent eventObject) {
        if(eventObject instanceof LoggingEvent){
            LoggingEvent loggingEvent = (LoggingEvent)eventObject;
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
        }
        super.append(eventObject);
    }
}
