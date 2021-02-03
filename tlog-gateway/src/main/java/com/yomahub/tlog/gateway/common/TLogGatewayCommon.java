package com.yomahub.tlog.gateway.common;

import cn.hutool.core.net.NetUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Consumer;

/**
 * @Author: zs
 * @Date: 2021/2/3 16:42
 */
public class TLogGatewayCommon extends TLogRPCHandler {

    private final static Logger log = LoggerFactory.getLogger(TLogGatewayCommon.class);

    private static volatile TLogGatewayCommon tLogWebCommon;

    private static final Integer FIRST = 0;

    public static TLogGatewayCommon loadInstance() {
        if (tLogWebCommon == null) {
            synchronized (TLogGatewayCommon.class) {
                if (tLogWebCommon == null) {
                    tLogWebCommon = new TLogGatewayCommon();
                }
            }
        }
        return tLogWebCommon;
    }

    public ServerWebExchange preHandle(ServerWebExchange exchange, String appName) {
        String traceId = null;
        String spanId = null;
        String preIvkApp = null;
        String preIvkHost = null;
        String preIp = null;
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

        processProviderSide(labelBean);

        if(StringUtils.isNotBlank(labelBean.getTraceId())){
            String hostName = TLogConstants.UNKNOWN;
            try{
                hostName = InetAddress.getLocalHost().getHostName();
            }catch (Exception e){}

            String finalHostName = hostName;
            Consumer<HttpHeaders> httpHeaders = httpHeader -> {
                httpHeader.set(TLogConstants.TLOG_TRACE_KEY, labelBean.getTraceId());
                httpHeader.set(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
                httpHeader.set(TLogConstants.PRE_IVK_APP_KEY, appName);
                httpHeader.set(TLogConstants.PRE_IVK_APP_HOST, finalHostName);
                httpHeader.set(TLogConstants.PRE_IP_KEY, NetUtil.getLocalhostStr());
            };
            ServerHttpRequest serverHttpRequest = exchange.getRequest().mutate().headers(httpHeaders).build();
            return exchange.mutate().request(serverHttpRequest).build();
        }else{
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
            return exchange;
        }
    }

}
