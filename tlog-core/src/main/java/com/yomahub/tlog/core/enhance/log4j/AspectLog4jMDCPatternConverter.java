package com.yomahub.tlog.core.enhance.log4j;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Arrays;
import java.util.Map;

/**
 * log4j的MDC转换器，这个类copy的是PatternParser的一个内部类<br>
 * 目的是当pattern里有mdc配置的话，设置全局变量mdc为true
 *
 * @author Bryan.Zhang
 * @since 1.1.5
 */
public class AspectLog4jMDCPatternConverter extends PatternConverter {
    private String key;

    AspectLog4jMDCPatternConverter(FormattingInfo formattingInfo, String key) {
        super(formattingInfo);
        TLogContext.setHasTLogMDC(true);
        this.key = key;
    }

    public String convert(LoggingEvent event) {
        if (key == null) {
            StringBuffer buf = new StringBuffer("{");
            Map properties = event.getProperties();
            if (properties.size() > 0) {
                Object[] keys = properties.keySet().toArray();
                Arrays.sort(keys);
                for (int i = 0; i < keys.length; i++) {
                    buf.append('{');
                    buf.append(keys[i]);
                    buf.append(',');
                    buf.append(properties.get(keys[i]));
                    buf.append('}');
                }
            }
            buf.append('}');
            return buf.toString();
        } else {
            Object val = event.getMDC(key);
            if (val == null) {
                return null;
            } else {
                return val.toString();
            }
        }
    }
}
