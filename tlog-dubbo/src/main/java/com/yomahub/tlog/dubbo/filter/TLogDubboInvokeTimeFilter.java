package com.yomahub.tlog.dubbo.filter;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.util.TLogJsonUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dubbo的时间统计调用拦截器
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
@Activate(group = {CommonConstants.PROVIDER}, order = -9000)
public class TLogDubboInvokeTimeFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TLogDubboInvokeTimeFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!TLogRuntime.isEnabled() || !TLogContext.enableInvokeTimePrint()) {
            return invoker.invoke(invocation);
        }
        String interfaceName = invoker.getInterface().getSimpleName();
        String methodName = invocation.getMethodName();
        if (log.isInfoEnabled()) {
            log.info("[TLOG]开始调用接口[{}]的方法[{}],参数为:{}", interfaceName, methodName,
                    TLogJsonUtil.toJsonForLog(invocation.getArguments()));
        }
        long start = System.nanoTime();
        try {
            return invoker.invoke(invocation);
        } finally {
            log.info("[TLOG]结束接口[{}]中方法[{}]的调用,耗时为:{}毫秒", interfaceName, methodName,
                    (System.nanoTime() - start) / 1_000_000);
        }
    }
}
