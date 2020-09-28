package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.core.context.AspectLogContext;

import java.lang.reflect.Field;

public class AspectLogbackAsyncAppender extends AsyncAppender {

    private Field field;

    @Override
    protected void append(ILoggingEvent eventObject) {
        if(eventObject instanceof LoggingEvent){
            LoggingEvent loggingEvent = (LoggingEvent)eventObject;
            String resultLog = StrUtil.format("{} {}", AspectLogContext.getLogValue(),loggingEvent.getMessage());

            if(field == null){
                field = ReflectUtil.getField(LoggingEvent.class,"formattedMessage");
                field.setAccessible(true);
            }

            try {
                field.set(loggingEvent,resultLog);
            } catch (IllegalAccessException e) {
            }
            eventObject = loggingEvent;
        }
        super.append(eventObject);
    }
}
