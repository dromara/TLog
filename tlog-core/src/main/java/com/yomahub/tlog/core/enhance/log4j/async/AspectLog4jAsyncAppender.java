package com.yomahub.tlog.core.enhance.log4j.async;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.lang.reflect.Field;

/**
 * Log4j的异步日志增强appender
 * @author Bryan.Zhang
 * @since 2020/9/30
 */
public class AspectLog4jAsyncAppender extends AsyncAppender {

    private Field field;

    public void doAppend(LoggingEvent event) {
        String resultLog;
        if(StringUtils.isNotBlank(AspectLogContext.getLogValue())){
            resultLog = StrUtil.format("{} {}", AspectLogContext.getLogValue(),event.getMessage());
        }else{
            resultLog = (String)event.getMessage();
        }

        if(field == null){
            field = ReflectUtil.getField(LoggingEvent.class,"message");
            field.setAccessible(true);
        }

        try {
            field.set(event,resultLog);
        } catch (IllegalAccessException e) {
        }

        super.doAppend(event);
    }
}
