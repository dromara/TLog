package com.yomahub.tlog.web;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * web controller的拦截器
 * @author Bryan.Zhang
 * @since 2020/9/11
 */
public class TLogWebInterceptor implements HandlerInterceptor {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            String traceId = request.getHeader(TLogConstants.TLOG_TRACE_KEY);
            String spanId = request.getHeader(TLogConstants.TLOG_SPANID_KEY);
            String preIvkApp = request.getHeader(TLogConstants.PRE_IVK_APP_KEY);
            String preIp = request.getHeader(TLogConstants.PRE_IP_KEY);
            if(StringUtils.isBlank(preIvkApp)){
                preIvkApp = TLogConstants.UNKNOWN;
            }
            if(StringUtils.isBlank(preIp)){
                preIp = TLogConstants.UNKNOWN;
            }

            if(StringUtils.isBlank(traceId)){
                traceId = UniqueIdGenerator.generateStringId();
                log.debug("[TLOG]重新生成traceId[{}]",traceId);
            }

            //往TLog上下文里放当前获取到的spanId，如果spanId为空，会放入初始值
            TLogContext.putSpanId(spanId);

            //往TLog上下文里放一个当前的traceId
            TLogContext.putTraceId(traceId);

            //生成日志标签
            String tlogLabel = TLogLabelGenerator.generateTLogLabel(preIvkApp,preIp,traceId,TLogContext.getSpanId());

            //往日志切面器里放一个日志前缀
            AspectLogContext.putLogValue(tlogLabel);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if(handler instanceof HandlerMethod){
            //移除ThreadLocal里的数据
            TLogContext.removeTraceId();
            TLogContext.removeSpanId();
            AspectLogContext.remove();
        }
    }
}
