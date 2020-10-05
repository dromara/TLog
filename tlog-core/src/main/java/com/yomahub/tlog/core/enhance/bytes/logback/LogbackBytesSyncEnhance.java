package com.yomahub.tlog.core.enhance.bytes.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.yomahub.tlog.core.context.AspectLogContext;

/**
 * Logback的字节增强类
 * @author Bryan.Zhang
 * @since 2020/9/29
 */
public class LogbackBytesSyncEnhance {

    public static String enhance(ILoggingEvent event){
        String logValue = AspectLogContext.getLogValue();
        if(logValue != null){
            return logValue + " " + event.getFormattedMessage();
        }else{
            return event.getFormattedMessage();
        }
    }
}
