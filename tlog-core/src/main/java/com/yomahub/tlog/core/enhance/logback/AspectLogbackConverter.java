package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;

/**
 * 基于日志适配方式的logback的convert
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class AspectLogbackConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        //只有在MDC没有设置的情况下才加到message里
        if (!TLogContext.hasTLogMDC()) {
            String logValue = AspectLogContext.getLogValue();
            if (StringUtils.isBlank(logValue)) {
                return event.getFormattedMessage();
            } else {
                return logValue + " " + event.getFormattedMessage();
            }
        } else {
            return event.getFormattedMessage();
        }
    }
}
