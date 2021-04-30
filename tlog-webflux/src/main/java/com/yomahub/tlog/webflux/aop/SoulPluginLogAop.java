package com.yomahub.tlog.webflux.aop;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * SoulPluginAspect
 *
 * @author naah
 * @since 1.3.0
 */
@Aspect
@Order(value = 10)
public class SoulPluginLogAop {

    private static final Integer FIRST = 0;
    private static TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    @Before(value = "execution(* org.dromara.soul.plugin..*.execute(..))")
    public void startLog(JoinPoint joinPoint) {
        String traceId = null;
        String spanId = null;
        String preIvkApp = null;
        String preIvkHost = null;
        String preIp = null;

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length < 1) {
            return;
        }
        if (args[0] == null || !(args[0] instanceof ServerWebExchange)) {
            return;
        }
        ServerWebExchange exchange = (ServerWebExchange) args[0];
        HttpHeaders headers = exchange.getRequest().getHeaders();
        List<String> traceIds = headers.get(TLogConstants.TLOG_TRACE_KEY);
        if (traceIds != null && traceIds.size() > 0) {
            traceId = traceIds.get(FIRST);
        }
        List<String> spanIds = headers.get(TLogConstants.TLOG_SPANID_KEY);
        if (spanIds != null && spanIds.size() > 0) {
            spanId = spanIds.get(FIRST);
        }
        List<String> preIvkApps = headers.get(TLogConstants.PRE_IVK_APP_KEY);
        if (preIvkApps != null && preIvkApps.size() > 0) {
            preIvkApp = preIvkApps.get(FIRST);
        }
        List<String> preIvkHosts = headers.get(TLogConstants.PRE_IVK_APP_HOST);
        if (preIvkHosts != null && preIvkHosts.size() > 0) {
            preIvkHost = preIvkHosts.get(FIRST);
        }
        List<String> preIps = headers.get(TLogConstants.PRE_IP_KEY);
        if (preIps != null && preIps.size() > 0) {
            preIp = preIps.get(FIRST);
        }

        TLogLabelBean labelBean = new TLogLabelBean(preIvkApp, preIvkHost, preIp, traceId, spanId);

        tLogRPCHandler.processProviderSide(labelBean);

    }

}
