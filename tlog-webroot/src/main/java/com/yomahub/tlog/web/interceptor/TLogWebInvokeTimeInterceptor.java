package com.yomahub.tlog.web.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.yomahub.tlog.context.TLogContext;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TLog web的调用时间统计拦截器
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogWebInvokeTimeInterceptor extends AbsTLogWebHandlerMethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TLogWebInvokeTimeInterceptor.class);

    private final TransmittableThreadLocal<StopWatch> invokeTimeTL = new TransmittableThreadLocal<>();

    @Override
    public boolean preHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (TLogContext.enableInvokeTimePrint()) {
            String url = request.getRequestURI();

            // 打印请求参数
            // 先屏蔽掉打印body的功能，目前只打印get参数
            /*if (isJson(request)) {
                String jsonParam = new RequestWrapper(request).getBodyString();
                log.info("[TLOG]开始请求URL[{}],参数为:{}", url, jsonParam);
            } else {
                String parameters = JSONUtil.toJsonStr(request.getParameterMap());
                log.info("[TLOG]开始请求URL[{}],参数为:{}", url, parameters);
            }*/

            String parameters = JSON.toJSONString(request.getParameterMap());
            log.info("[TLOG]开始请求URL[{}],参数为:{}", url, parameters);

            StopWatch stopWatch = new StopWatch();
            invokeTimeTL.set(stopWatch);
            stopWatch.start();
        }
        return true;
    }

    @Override
    public void postHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletionByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (TLogContext.enableInvokeTimePrint()) {
            StopWatch stopWatch = invokeTimeTL.get();
            stopWatch.stop();
            log.info("[TLOG]结束URL[{}]的调用,耗时为:{}毫秒", request.getRequestURI(), stopWatch.getTime());
            invokeTimeTL.remove();
        }
    }

    /**
     * 判断本次请求的数据类型是否为json
     *
     * @param request request
     * @return boolean
     */
    private boolean isJson(HttpServletRequest request) {
        if (request.getContentType() != null) {
            return request.getContentType().equals(MediaType.APPLICATION_JSON_VALUE) ||
                    request.getContentType().equals(MediaType.APPLICATION_JSON_UTF8_VALUE);
        }
        return false;
    }
}
