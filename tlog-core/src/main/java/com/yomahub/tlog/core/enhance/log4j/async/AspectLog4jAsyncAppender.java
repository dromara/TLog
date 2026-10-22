package com.yomahub.tlog.core.enhance.log4j.async;

import cn.hutool.core.util.ReflectUtil;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.lang.reflect.Field;

/**
 * 基于日志适配方式的Log4j的异步日志增强appender
 * <p>
 * 在日志事件进入异步队列前（仍在业务线程上）把标签写入渲染后的消息，避免异步线程拿不到上下文
 *
 * @author Bryan.Zhang
 * @since 1.1.1
 */
public class AspectLog4jAsyncAppender extends AsyncAppender {

    private static final Field RENDERED_MESSAGE_FIELD = ReflectUtil.getField(LoggingEvent.class, "renderedMessage");

    static {
        if (RENDERED_MESSAGE_FIELD != null) {
            RENDERED_MESSAGE_FIELD.setAccessible(true);
        }
    }

    @Override
    public void doAppend(LoggingEvent event) {
        String logValue = AspectLogContext.getLogValue();
        if (RENDERED_MESSAGE_FIELD != null && !TLogContext.hasTLogMDC() && StringUtils.isNotBlank(logValue)) {
            String rendered = event.getRenderedMessage();
            if (rendered == null || !rendered.startsWith(logValue)) {
                try {
                    // 与同步模式保持一致：标签在前，消息在后
                    RENDERED_MESSAGE_FIELD.set(event, logValue + " " + rendered);
                } catch (IllegalAccessException ignored) {
                    // 写入失败时保持原消息
                }
            }
        }
        super.doAppend(event);
    }
}
