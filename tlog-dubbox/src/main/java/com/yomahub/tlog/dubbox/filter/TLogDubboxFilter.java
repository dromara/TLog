package com.yomahub.tlog.dubbox.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dubbox的调用拦截器
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = -10000)
public class TLogDubboxFilter extends TLogRPCHandler implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TLogDubboxFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result;
        String side = invoker.getUrl().getParameter(Constants.SIDE_KEY);

        if (side.equals(Constants.PROVIDER_SIDE)) {
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
        } else if (side.equals(Constants.CONSUMER_SIDE)) {
            String traceId = TLogContext.getTraceId();

            if (StringUtils.isNotBlank(traceId)) {
                String appName = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);

                RpcContext.getContext().setAttachment(TLogConstants.TLOG_TRACE_KEY, traceId);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_KEY, appName);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_HOST, LocalhostUtil.getHostName());
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IP_KEY, LocalhostUtil.getHostIp());
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
