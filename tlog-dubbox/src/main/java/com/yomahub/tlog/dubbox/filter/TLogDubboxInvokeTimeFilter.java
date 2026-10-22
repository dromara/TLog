package com.yomahub.tlog.dubbox.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.util.TLogJsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dubbox的时间统计调用拦截器
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
@Activate(group = {Constants.PROVIDER}, order = -9000)
public class TLogDubboxInvokeTimeFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TLogDubboxInvokeTimeFilter.class);

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
