package com.yomahub.tlog.webflux.common;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author zs
 * @since 1.3.0
 */
public class TLogWebFluxCommon extends TLogRPCHandler {

    private final static Logger log = LoggerFactory.getLogger(TLogWebFluxCommon.class);

    private static volatile TLogWebFluxCommon tLogWebCommon;

    private static final Integer FIRST = 0;

    public static TLogWebFluxCommon loadInstance() {
        if (tLogWebCommon == null) {
            synchronized (TLogWebFluxCommon.class) {
                if (tLogWebCommon == null) {
                    tLogWebCommon = new TLogWebFluxCommon();
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
        labelBean.putExtData(TLogConstants.WEBFLUX_EXCHANGE, exchange);

        processProviderSide(labelBean);

        if(StringUtils.isNotBlank(labelBean.getTraceId())){
            Consumer<HttpHeaders> httpHeaders = httpHeader -> {
                httpHeader.set(TLogConstants.TLOG_TRACE_KEY, labelBean.getTraceId());
                httpHeader.set(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
                httpHeader.set(TLogConstants.PRE_IVK_APP_KEY, appName);
                httpHeader.set(TLogConstants.PRE_IVK_APP_HOST, LocalhostUtil.getHostName());
                httpHeader.set(TLogConstants.PRE_IP_KEY, LocalhostUtil.getHostIp());
            };
            ServerHttpRequest serverHttpRequest = exchange.getRequest().mutate().headers(httpHeaders).build();
            return exchange.mutate().request(serverHttpRequest).build();
        }else{
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
            return exchange;
        }
    }

}
