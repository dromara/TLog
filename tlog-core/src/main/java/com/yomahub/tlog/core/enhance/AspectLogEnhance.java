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
            pool.importPackage("com.yomahub.tlog.core.enhance.LogbackEnhance");
            pool.importPackage("com.yomahub.tlog.core.enhance.Log4jEnhance");

            //logback的增强
            CtClass cc = null;
            try{
                cc = pool.get("ch.qos.logback.classic.pattern.MessageConverter");

                if(cc != null){
                    CtMethod ctMethod = cc.getDeclaredMethod("convert");
                    ctMethod.setBody("{return LogbackEnhance.enhance($1);}");
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
                    ctMethod.setBody("{return Log4jEnhance.enhance(renderedMessage,message,logger);}");
                    cc.toClass();
                }
            }catch (Exception e){
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
