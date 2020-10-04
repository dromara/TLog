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
            //logback的增强
            CtClass cc = null;
            try{
                ClassPool pool = ClassPool.getDefault();
                pool.importPackage("com.yomahub.tlog.core.enhance.LogbackSyncEnhance");

                cc = pool.get("ch.qos.logback.classic.pattern.MessageConverter");

                if(cc != null){
                    CtMethod ctMethod = cc.getDeclaredMethod("convert");
                    ctMethod.setBody("{return LogbackEnhance.enhance($1);}");
                    cc.toClass();
                }
            }catch (Exception e){
            }

            //log4j的同步日志增强
            try{
                ClassPool pool = ClassPool.getDefault();
                pool.importPackage("com.yomahub.tlog.core.enhance.Log4jSyncEnhance");

                cc = pool.get("org.apache.log4j.spi.LoggingEvent");

                if(cc != null){
                    CtMethod ctMethod = cc.getDeclaredMethod("getRenderedMessage");
                    ctMethod.setBody("{return Log4jEnhance.enhance(renderedMessage,message,logger);}");
                    cc.toClass();
                }
            }catch (Exception e){
            }

            //log4j异步日志方式的增强
            try{
                ClassPool pool = ClassPool.getDefault();
                pool.importPackage("com.yomahub.tlog.core.enhance.Log4jASyncEnhance");

                cc = pool.get("org.apache.log4j.AppenderSkeleton");

                if(cc != null){
                    CtMethod ctMethod = cc.getDeclaredMethod("doAppend");
                    ctMethod.setBody("{Log4jASyncEnhance.enhance($1,closed,name,this);}");
                    cc.toClass();
                }
            }catch (Exception e){
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
