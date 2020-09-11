package com.yomahub.tlog.core.enhance;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * @author Bryan.Zhang
 * 利用javassit进行对LOG框架进行切面增强
 */
public class AspectLogEnhance {

    public static void enhance(){
        try{
            ClassPool pool = ClassPool.getDefault();
            pool.importPackage("com.yomahub.tlog.core.context");

            //logback的增强
            CtClass cc = null;
            try{
                cc = pool.get("ch.qos.logback.classic.pattern.MessageConverter");

                if(cc != null){
                    CtMethod ctMethod = cc.getDeclaredMethod("convert");
                    ctMethod.setBody("{String logValue = AspectLogContext.getLogValue();if(logValue != null){return logValue + \" \" + $1.getFormattedMessage();}else{return $1.getFormattedMessage();}}");
                    cc.toClass();
                }
            }catch (Exception e){
            }

            //log4j的增强
            try{
                pool.importPackage("org.apache.log4j.spi.LoggerRepository");
                pool.importPackage("org.apache.log4j.spi.RendererSupport");
                cc = pool.get("org.apache.log4j.spi.LoggingEvent");

                if(cc != null){
                    CtMethod ctMethod = cc.getDeclaredMethod("getRenderedMessage");
                    ctMethod.setBody("{if(renderedMessage == null && message != null) {if(message instanceof String)renderedMessage = (String) message;else {LoggerRepository repository = logger.getLoggerRepository();if(repository instanceof RendererSupport) {RendererSupport rs = (RendererSupport) repository;renderedMessage= rs.getRendererMap().findAndRender(message);} else {renderedMessage = message.toString();}}}String logValue = AspectLogContext.getLogValue();if(logValue != null){renderedMessage = logValue + \" \" + renderedMessage;}return renderedMessage;}");
                    cc.toClass();
                }
            }catch (Exception e){
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
