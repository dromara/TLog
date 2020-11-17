package com.yomahub.tlog.dubbox.filter;

import cn.hutool.core.net.NetUtil;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import utils.Times;

/**
 * dubbox的调用拦截器
 *
 * @author Bryan.Zhang
 * @since 2020/9/11
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = -10000)
public class TLogDubboxFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TLogDubboxFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        final Result[] result = new Result[1];

        Times.timeConsuming(true, "TLogDubboxFilter", log, new Times.Task() {

            @Override
            public void execute() {

                String side = invoker.getUrl().getParameter(Constants.SIDE_KEY);

                if (side.equals(Constants.PROVIDER_SIDE)) {
                    String preIvkApp = invocation.getAttachment(TLogConstants.PRE_IVK_APP_KEY);
                    String preIp = invocation.getAttachment(TLogConstants.PRE_IP_KEY);
                    String traceId = invocation.getAttachment(TLogConstants.TLOG_TRACE_KEY);
                    String spanId = invocation.getAttachment(TLogConstants.TLOG_SPANID_KEY);

                    if (StringUtils.isBlank(preIvkApp)) {
                        preIvkApp = TLogConstants.UNKNOWN;
                    }
                    if (StringUtils.isBlank(preIp)) {
                        preIp = TLogConstants.UNKNOWN;
                    }

                    TLogContext.putPreIvkApp(preIvkApp);
                    TLogContext.putPreIp(preIp);

                    //如果从隐式传参里没有获取到，则重新生成一个traceId
                    if (StringUtils.isBlank(traceId)) {
                        traceId = UniqueIdGenerator.generateStringId();
                        log.debug("[TLOG]可能上一个节点[{}]没有没有正确传递traceId,重新生成traceId[{}]", preIvkApp, traceId);
                    }

                    //往TLog上下文里放当前获取到的spanId，如果spanId为空，会放入初始值
                    TLogContext.putSpanId(spanId);

                    //往TLog上下文里放一个当前的traceId
                    TLogContext.putTraceId(traceId);

                    //生成日志标签
                    String tlogLabel = TLogLabelGenerator.generateTLogLabel(preIvkApp, preIp, traceId, TLogContext.getSpanId());

                    //往日志切面器里放一个日志前缀
                    AspectLogContext.putLogValue(tlogLabel);

                    //如果有MDC，则往MDC中放入日志标签
                    if (TLogContext.hasTLogMDC()) {
                        MDC.put(TLogConstants.MDC_KEY, tlogLabel);
                    }

                    try {
                        //调用dubbo
                        result[0] = invoker.invoke(invocation);
                    } finally {
                        //移除ThreadLocal里的数据
                        TLogContext.removePreIvkApp();
                        TLogContext.removePreIp();
                        TLogContext.removeTraceId();
                        TLogContext.removeSpanId();
                        AspectLogContext.remove();
                        if (TLogContext.hasTLogMDC()) {
                            MDC.remove(TLogConstants.MDC_KEY);
                        }
                    }
                } else if (side.equals(Constants.CONSUMER_SIDE)) {
                    String traceId = TLogContext.getTraceId();

                    if (StringUtils.isNotBlank(traceId)) {
                        String appName = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
                        String ip = NetUtil.getLocalhostStr();

                        RpcContext.getContext().setAttachment(TLogConstants.TLOG_TRACE_KEY, traceId);
                        RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_KEY, appName);
                        RpcContext.getContext().setAttachment(TLogConstants.PRE_IP_KEY, ip);
                        RpcContext.getContext().setAttachment(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
                    } else {
                        log.warn("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
                    }
                    result[0] = invoker.invoke(invocation);
                } else {
                    result[0] = null;
                }
            }
        });

        return result[0];
    }
}
