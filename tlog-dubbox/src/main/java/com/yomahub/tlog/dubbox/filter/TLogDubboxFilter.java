package com.yomahub.tlog.dubbox.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dubbox的调用拦截器
 * @author Bryan.Zhang
 * @date 2020/9/11
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER},order = -10000)
public class TLogDubboxFilter implements Filter {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String side = invoker.getUrl().getParameter(Constants.SIDE_KEY);

        if(side.equals(Constants.CONSUMER)){
            String preIvkApp = invocation.getAttachment(TLogConstants.PRE_IVK_APP_KEY);
            String traceId = invocation.getAttachment(TLogConstants.TLOG_TRACE_KEY);

            if(StringUtils.isBlank(preIvkApp)){
                preIvkApp = TLogConstants.UNKNOWN;
            }

            //如果从隐式传参里没有获取到，则重新生成一个traceId
            if(StringUtils.isBlank(traceId)){
                traceId = UniqueIdGenerator.generateHexId();
                log.warn("[TLOG]可能上一个节点[{}]没有没有正确传递traceId,重新生成traceId[{}]",preIvkApp,traceId);
            }

            //往日志切面器里放一个日志前缀
            AspectLogContext.putLogValue("<"+traceId+">");

            //往TLog上下文里放一个当前的traceId
            TLogContext.putTraceId(traceId);

        }else if(side.equals(Constants.PROVIDER)){
            String traceId = TLogContext.getTraceId();

            if(StringUtils.isNotBlank(traceId)){
                String appName = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);

                RpcContext.getContext().setAttachment(TLogConstants.TLOG_TRACE_KEY,traceId);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_KEY,appName);
            }else{
                log.warn("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
            }
        }
        return invoker.invoke(invocation);
    }
}
