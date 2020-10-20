package com.yomahub.tlog.core.enhance.bytes.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AsyncAppenderBase;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LogbackBytesAsyncEnhance {

    private static Method method1;

    private static Method method2;

    private static Method method3;

    private static Method method4;

    private static Field field;

    public static void enhance(Object eventObject, AsyncAppenderBase thisInstance) {
        if(method1 == null){
            method1 = ReflectUtil.getMethod(thisInstance.getClass(),"isQueueBelowDiscardingThreshold");
        }

        if(method2 == null){
            method2 = ReflectUtil.getMethod(thisInstance.getClass(),"isDiscardable",Object.class);
        }

        if(method3 == null){
            method3 = ReflectUtil.getMethod(thisInstance.getClass(),"preprocess",Object.class);
        }

        if(method4 == null){
            method4 = ReflectUtil.getMethod(thisInstance.getClass(),"put",Object.class);
        }

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

        boolean flag1,flag2;
        try {
            flag1 = ReflectUtil.invoke(thisInstance,method1);
            flag2 = ReflectUtil.invoke(thisInstance,method2,eventObject);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (flag1 && flag2) {
            return;
        }

        try{
            ReflectUtil.invoke(thisInstance, method3, eventObject);
            ReflectUtil.invoke(thisInstance, method4, eventObject);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
