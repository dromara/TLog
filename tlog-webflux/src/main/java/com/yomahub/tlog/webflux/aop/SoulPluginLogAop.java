package com.yomahub.tlog.webflux.aop;

import com.yomahub.tlog.webflux.common.TLogWebFluxCommon;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;

/**
 * SoulPluginAspect
 *
 * @author naah
 * @since 1.3.0
 */
@Aspect
@Order(value = 10)
public class SoulPluginLogAop {

    @Before(value = "execution(* org.dromara.soul.plugin..*.execute(..))")
    public void startLog(JoinPoint joinPoint) {
        // 请求的 Reactor Context 已由过滤器恢复；插件之间不得重新初始化 span 计数器。
        if (!TLogRuntime.isEnabled() || TLogContext.getTraceId() != null) {
            return;
        }
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof ServerWebExchange) {
            TLogWebFluxCommon.loadInstance().processExchange((ServerWebExchange) args[0]);
        }
    }
}
