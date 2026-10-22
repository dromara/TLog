package com.yomahub.tlog.core.enhance.logback.async;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import cn.hutool.core.util.ReflectUtil;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * Logback的异步日志增强appender
 * <p>
 * 在日志事件进入异步队列前（仍在业务线程上）把标签写入格式化后的消息，避免异步线程拿不到上下文
 *
 * @author Bryan.Zhang
 * @since 1.1.1
 */
public class AspectLogbackAsyncAppender extends AsyncAppender {

    private static final Field FORMATTED_MESSAGE_FIELD = ReflectUtil.getField(LoggingEvent.class, "formattedMessage");

    static {
        if (FORMATTED_MESSAGE_FIELD != null) {
            FORMATTED_MESSAGE_FIELD.setAccessible(true);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject instanceof LoggingEvent && FORMATTED_MESSAGE_FIELD != null) {
            String logValue = AspectLogContext.getLogValue();
            if (!TLogContext.hasTLogMDC() && StringUtils.isNotBlank(logValue)) {
                String formatted = eventObject.getFormattedMessage();
                if (formatted == null || !formatted.startsWith(logValue)) {
                    try {
                        FORMATTED_MESSAGE_FIELD.set(eventObject, logValue + " " + formatted);
                    } catch (IllegalAccessException ignored) {
                        // 写入失败时保持原消息
                    }
                }
            }
        }
        super.append(eventObject);
    }
}
