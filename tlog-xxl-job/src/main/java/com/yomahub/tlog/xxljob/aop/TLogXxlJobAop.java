package com.yomahub.tlog.xxljob.aop;

import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * XXlJOb的handler切面
 * @author Bryan.Zhang
 * @since 1.3.5
 */
@Aspect
public class TLogXxlJobAop {

    private final TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    @Pointcut("@annotation(com.xxl.job.core.handler.annotation.XxlJob)")
    public void cut() {
    }

    @Around("cut()")
    public Object around(ProceedingJoinPoint jp) throws Throwable {
        try{
            tLogRPCHandler.processProviderSide(new TLogLabelBean());
            return jp.proceed();
        }finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }
}
