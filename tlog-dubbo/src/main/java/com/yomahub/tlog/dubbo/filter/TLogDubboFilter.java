package com.yomahub.tlog.dubbo.filter;

import com.yomahub.tlog.context.TLogPropagator;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

/**
 * dubbo的调用拦截器，兼容 Dubbo 2.7.x 与 3.x
 * <p>
 * 消费端写入 {@code RpcContext.getContext().setAttachment}，该 API 在 Dubbo 3 中仍保留且语义一致
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -10000)
public class TLogDubboFilter extends TLogRPCHandler implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!TLogRuntime.isEnabled()) {
            return invoker.invoke(invocation);
        }
        String side = invoker.getUrl().getParameter(CommonConstants.SIDE_KEY);

        if (CommonConstants.PROVIDER_SIDE.equals(side)) {
            processProviderSide(invocation::getAttachment);
            try {
                return invoker.invoke(invocation);
            } finally {
                cleanThreadLocal();
            }
        }

        if (CommonConstants.CONSUMER_SIDE.equals(side)) {
            String appName = invoker.getUrl().getParameter(CommonConstants.APPLICATION_KEY);
            RpcContext context = RpcContext.getContext();
            TLogPropagator.inject(context::setAttachment, appName);
        }
        return invoker.invoke(invocation);
    }
}
