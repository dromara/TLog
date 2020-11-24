package com.yomahub.tlog.dubbo.filter;

import cn.hutool.core.net.NetUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetAddress;

/**
 * dubbo的调用拦截器
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -10000)
public class TLogDubboFilter extends TLogRPCHandler implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TLogDubboFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result;
        String side = invoker.getUrl().getParameter(CommonConstants.SIDE_KEY);

        if (side.equals(CommonConstants.PROVIDER_SIDE)) {
            String preIvkApp = invocation.getAttachment(TLogConstants.PRE_IVK_APP_KEY);
            String preIvkHost = invocation.getAttachment(TLogConstants.PRE_IVK_APP_HOST);
            String preIp = invocation.getAttachment(TLogConstants.PRE_IP_KEY);
            String traceId = invocation.getAttachment(TLogConstants.TLOG_TRACE_KEY);
            String spanId = invocation.getAttachment(TLogConstants.TLOG_SPANID_KEY);

            TLogLabelBean labelBean = new TLogLabelBean(preIvkApp, preIvkHost, preIp, traceId, spanId);

            processProviderSide(labelBean);

            try {
                //调用dubbo
                result = invoker.invoke(invocation);
            } finally {
                cleanThreadLocal();
            }

            return result;
        } else if (side.equals(CommonConstants.CONSUMER_SIDE)) {
            String traceId = TLogContext.getTraceId();

            if (StringUtils.isNotBlank(traceId)) {
                String appName = invoker.getUrl().getParameter(CommonConstants.APPLICATION_KEY);
                String ip = NetUtil.getLocalhostStr();
                String hostName = TLogConstants.UNKNOWN;
                try{
                    hostName = InetAddress.getLocalHost().getHostName();
                }catch (Exception e){}

                RpcContext.getContext().setAttachment(TLogConstants.TLOG_TRACE_KEY, traceId);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_KEY, appName);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_HOST, hostName);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IP_KEY, ip);
                RpcContext.getContext().setAttachment(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
            } else {
                log.warn("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
            }
            result = invoker.invoke(invocation);
        } else {
            result = null;
        }
        return result;
    }
}
