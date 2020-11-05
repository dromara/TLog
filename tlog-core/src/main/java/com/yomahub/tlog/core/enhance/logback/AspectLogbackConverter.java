package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * @author Bryan.Zhang
 */
public class AspectLogbackConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        String logValue = AspectLogContext.getLogValue();

        if(AspectLogbackMDCConverter.hasTLogMDC){
            MDC.put(TLogConstants.MDC_KEY, logValue);
            return event.getFormattedMessage();
        }else{
            if(StringUtils.isBlank(logValue)){
                return event.getFormattedMessage();
            }else{
                return logValue + " " + event.getFormattedMessage();
            }
        }
    }
}
