package com.yomahub.tlog.dubbox.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.yomahub.tlog.context.TLogPropagator;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;

/**
 * dubbox的调用拦截器
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = -10000)
public class TLogDubboxFilter extends TLogRPCHandler implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!TLogRuntime.isEnabled()) {
            return invoker.invoke(invocation);
        }
        String side = invoker.getUrl().getParameter(Constants.SIDE_KEY);

        if (Constants.PROVIDER_SIDE.equals(side)) {
            processProviderSide(invocation::getAttachment);
            try {
                return invoker.invoke(invocation);
            } finally {
                cleanThreadLocal();
            }
        }

        if (Constants.CONSUMER_SIDE.equals(side)) {
            String appName = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
            RpcContext context = RpcContext.getContext();
            TLogPropagator.inject(context::setAttachment, appName);
        }
        return invoker.invoke(invocation);
    }
}
