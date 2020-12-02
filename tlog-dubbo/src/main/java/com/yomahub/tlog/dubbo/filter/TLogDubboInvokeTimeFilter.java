package com.yomahub.tlog.dubbo.filter;

import com.alibaba.fastjson.JSON;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dubbo的时间统计调用拦截器
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
@Activate(group = {CommonConstants.PROVIDER}, order = -9000)
public class TLogDubboInvokeTimeFilter extends TLogRPCHandler implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TLogDubboInvokeTimeFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (TLogContext.enableInvokeTimePrint()) {
            Result result;
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                log.info("[TLOG]开始调用接口[{}]的方法[{}],参数为:{}", invoker.getInterface().getSimpleName(),
                        invocation.getMethodName(),
                        JSON.toJSONString(invocation.getArguments()));
                //调用dubbo
                result = invoker.invoke(invocation);
            } finally {
                stopWatch.stop();
                log.info("[TLOG]结束接口[{}]中方法[{}]的调用,耗时为:{}毫秒", invoker.getInterface().getSimpleName(),
                        invocation.getMethodName(),
                        stopWatch.getTime());
            }
            return result;
        }

        return invoker.invoke(invocation);
    }
}
