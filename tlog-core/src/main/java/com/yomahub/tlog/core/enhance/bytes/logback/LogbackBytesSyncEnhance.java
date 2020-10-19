package com.yomahub.tlog.core.enhance.bytes.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.WarnStatus;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Logback的字节增强类
 * @author Bryan.Zhang
 * @since 2020/9/29
 */
public class LogbackBytesSyncEnhance {

    private static Field guardField;

    private static Field startedField;

    private static Field statusRepeatCountField;

    private static Field exceptionCountField;

    private static Field nameField;

    private static Field messageField;

    private static Method appendMethod;

    public static final int ALLOWED_REPEATS = 3;

    public static void enhance(Object event, UnsynchronizedAppenderBase thisObj){
        if (guardField == null){
            guardField = ReflectUtil.getField(thisObj.getClass(),"guard");
        }

        if (startedField == null){
            startedField = ReflectUtil.getField(thisObj.getClass(),"started");
        }

        if (statusRepeatCountField == null){
            statusRepeatCountField = ReflectUtil.getField(thisObj.getClass(),"statusRepeatCount");
        }

        if (exceptionCountField == null){
            exceptionCountField = ReflectUtil.getField(thisObj.getClass(),"exceptionCount");
        }

        if (nameField == null){
            nameField = ReflectUtil.getField(thisObj.getClass(),"name");
        }

        if (appendMethod == null){
            appendMethod = ReflectUtil.getMethod(thisObj.getClass(), "append", Object.class);
        }

        if (messageField == null){
            messageField = ReflectUtil.getField(event.getClass(), "formattedMessage");
        }

        if(StringUtils.isNotBlank(AspectLogContext.getLogValue())){
            LoggingEvent loggingEvent = (LoggingEvent)event;
            String resultLog = StrUtil.format("{} {}", AspectLogContext.getLogValue(),loggingEvent.getMessage());
            ReflectUtil.setFieldValue(event, messageField, resultLog);
        }

        ThreadLocal<Boolean> guard;
        boolean started;
        int statusRepeatCount,exceptionCount;
        String name;

        try{
            guard = (ThreadLocal<Boolean>)ReflectUtil.getFieldValue(thisObj, guardField);
            started = (boolean)ReflectUtil.getFieldValue(thisObj, startedField);
            statusRepeatCount = (int)ReflectUtil.getFieldValue(thisObj, statusRepeatCountField);
            exceptionCount = (int)ReflectUtil.getFieldValue(thisObj, exceptionCountField);
            name = (String)ReflectUtil.getFieldValue(thisObj, nameField);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (Boolean.TRUE.equals(guard.get())) {
            return;
        }

        try {
            guard.set(Boolean.TRUE);

            if (!started) {
                if (statusRepeatCount++ < ALLOWED_REPEATS) {
                    thisObj.addStatus(new WarnStatus("Attempted to append to non started appender [" + name + "].", thisObj));
                }
                return;
            }

            if (thisObj.getFilterChainDecision(event) == FilterReply.DENY) {
                return;
            }

            // ok, we now invoke derived class' implementation of append
            ReflectUtil.invoke(thisObj,appendMethod,event);
        } catch (Exception e) {
            if (exceptionCount++ < ALLOWED_REPEATS) {
                thisObj.addError("Appender [" + name + "] failed to append.", e);
            }
        } finally {
            guard.set(Boolean.FALSE);
        }
    }
}
