package com.yomahub.tlog.web.interceptor;

import com.alibaba.fastjson.JSON;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.web.wrapper.RequestWrapper;
import com.yomahub.tlog.web.common.TLogWebCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * web controller的拦截器
 *
 * @author Bryan.Zhang
 * @since 1.1.5
 */
public class TLogWebInterceptor extends AbsTLogWebHandlerMethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TLogWebInterceptor.class);

    @Override
    public boolean preHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        TLogWebCommon.loadInstance().preHandle(request, response, handler);
        //把traceId放入response的header，为了方便有些人有这样的需求，从前端拿整条链路的traceId
        response.addHeader(TLogConstants.TLOG_TRACE_KEY, TLogContext.getTraceId());
        String url = request.getRequestURI();
        if ("/error".equals(url)) {
            return true;
        }
        // 打印请求参数
        if (isJson(request)) {
            // json请求
            String jsonParam = new RequestWrapper(request).getBodyString();
            log.info("[TLOG]开始请求URL[{}],参数为:{}", url, jsonParam);
        } else {
            String parameters = JSON.toJSONString(request.getParameterMap());
            log.info("[TLOG]开始请求URL[{}],参数为:{}", url, parameters);
        }
        return true;
    }

    @Override
    public void postHandleByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletionByHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TLogWebCommon.loadInstance().afterCompletion(request, response, handler);
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
