package com.yomahub.tlog.core.enhance.bytes.log4j;

import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.log4j.Category;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RendererSupport;

/**
 * Log4j的字节增强器
 * @author Bryan.Zhang
 * @since 2020/9/29
 */
public class Log4jBytesSyncEnhance {

    public static String enhance(String renderedMessage, Object message, Category logger){
        if(renderedMessage == null && message != null) {
            if(message instanceof String){
                renderedMessage = (String) message;
            }else {
                LoggerRepository repository = logger.getLoggerRepository();
                if(repository instanceof RendererSupport) {
                    RendererSupport rs = (RendererSupport) repository;
                    renderedMessage= rs.getRendererMap().findAndRender(message);
                } else {
                    renderedMessage = message.toString();
                }
            }
        }
        String logValue = AspectLogContext.getLogValue();
        if(logValue != null){
            renderedMessage = logValue + " " + renderedMessage;
        }
        return renderedMessage;
    }
}
