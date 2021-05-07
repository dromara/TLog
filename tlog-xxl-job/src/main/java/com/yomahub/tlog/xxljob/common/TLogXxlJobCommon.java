package com.yomahub.tlog.xxljob.common;

import cn.hutool.core.net.NetUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author zs
 * @since 1.3.0
 */
public class TLogXxlJobCommon extends TLogRPCHandler {

    private final static Logger log = LoggerFactory.getLogger(TLogXxlJobCommon.class);

    private static volatile TLogXxlJobCommon tLogXxlJobCommon;

    private static final Integer FIRST = 0;

    public static TLogXxlJobCommon loadInstance() {
        if (tLogXxlJobCommon == null) {
            synchronized (TLogXxlJobCommon.class) {
                if (tLogXxlJobCommon == null) {
                    tLogXxlJobCommon = new TLogXxlJobCommon();
                }
            }
        }
        return tLogXxlJobCommon;
    }

    public FullHttpRequest handle(FullHttpRequest msg, String appName) {
        String traceId = null;
        String spanId = null;
        String preIvkApp = null;
        String preIvkHost = null;
        String preIp = null;
        HttpHeaders headers = msg.headers();
        List<String> traceIds = headers.getAll(TLogConstants.TLOG_TRACE_KEY);
        if (traceIds != null && traceIds.size() > 0) {
            traceId = traceIds.get(FIRST);
        }
        List<String> spanIds = headers.getAll(TLogConstants.TLOG_SPANID_KEY);
        if (spanIds != null && spanIds.size() > 0) {
            spanId = spanIds.get(FIRST);
        }
        List<String> preIvkApps = headers.getAll(TLogConstants.PRE_IVK_APP_KEY);
        if (preIvkApps != null && preIvkApps.size() > 0) {
            preIvkApp = preIvkApps.get(FIRST);
        }
        List<String> preIvkHosts = headers.getAll(TLogConstants.PRE_IVK_APP_HOST);
        if (preIvkHosts != null && preIvkHosts.size() > 0) {
            preIvkHost = preIvkHosts.get(FIRST);
        }
        List<String> preIps = headers.getAll(TLogConstants.PRE_IP_KEY);
        if (preIps != null && preIps.size() > 0) {
            preIp = preIps.get(FIRST);
        }

        TLogLabelBean labelBean = new TLogLabelBean(preIvkApp, preIvkHost, preIp, traceId, spanId);

        processProviderSide(labelBean);

        if(StringUtils.isNotBlank(labelBean.getTraceId())){
            String hostName = TLogConstants.UNKNOWN;
            try{
                hostName = NetUtil.getLocalHostName();
            }catch (Exception e){}
            String finalHostName = hostName;
            msg.headers().add(TLogConstants.TLOG_TRACE_KEY, labelBean.getTraceId());
            msg.headers().add(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
            msg.headers().add(TLogConstants.PRE_IVK_APP_KEY, appName);
            msg.headers().add(TLogConstants.PRE_IVK_APP_HOST, finalHostName);
            msg.headers().add(TLogConstants.PRE_IP_KEY, NetUtil.getLocalhostStr());
            return msg;
        }else{
            log.debug("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
            return msg;
        }
    }
}
