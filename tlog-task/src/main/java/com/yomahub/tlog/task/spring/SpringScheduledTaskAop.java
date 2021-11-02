package com.yomahub.tlog.task.spring;

import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于spring scheduled定时器的增强AOP
 * @author Bryan.Zhang
 * @since 1.3.4
 */
@Aspect
public class SpringScheduledTaskAop {

    private static final Logger log = LoggerFactory.getLogger(SpringScheduledTaskAop.class);

    private final TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
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
