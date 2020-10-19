package com.yomahub.tlog.core.enhance.bytes.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.WarnStatus;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.yomahub.tlog.core.context.AspectLogContext;

import java.lang.reflect.Field;

/**
 * Logback的字节增强类
 * @author Bryan.Zhang
 * @since 2020/9/29
 */
public class LogbackBytesSyncEnhance {

    private static Field guardField;

    private static Field startedField;

    public static void enhance(ILoggingEvent event, UnsynchronizedAppenderBase thisObj){

        if (guardField == null){
            guardField = ReflectUtil.getField(LoggingEvent.class,"guardField");
            guardField.setAccessible(true);
        }

        ThreadLocal<Boolean> guardFieldValue = null;
        try{
            guardFieldValue = (ThreadLocal<Boolean>)guardField.get(thisObj);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (Boolean.TRUE.equals(guardFieldValue.get())) {
            return;
        }

        try {
            guardFieldValue.set(Boolean.TRUE);

            if (!this.started) {
                if (statusRepeatCount++ < ALLOWED_REPEATS) {
                    addStatus(new WarnStatus("Attempted to append to non started appender [" + name + "].", this));
                }
                return;
            }

            if (getFilterChainDecision(eventObject) == FilterReply.DENY) {
                return;
            }

            // ok, we now invoke derived class' implementation of append
            this.append(eventObject);

        } catch (Exception e) {
            if (exceptionCount++ < ALLOWED_REPEATS) {
                addError("Appender [" + name + "] failed to append.", e);
            }
        } finally {
            guard.set(Boolean.FALSE);
        }

    }
}
