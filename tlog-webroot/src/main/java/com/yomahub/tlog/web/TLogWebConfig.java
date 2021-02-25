package com.yomahub.tlog.web;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.yomahub.tlog.web.interceptor.TLogWebInterceptor;
import com.yomahub.tlog.web.interceptor.TLogWebInvokeTimeInterceptor;
import org.springframework.core.Ordered;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.*;

import java.lang.reflect.Method;
import java.util.List;

/**
 * TLog webconfig类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogWebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration interceptorRegistration;
        interceptorRegistration = registry.addInterceptor(new TLogWebInterceptor());
        //这里是为了兼容springboot 1.5.X，1.5.x没有order这个方法
        try{
            Method method = ReflectUtil.getMethod(InterceptorRegistration.class, "order", Integer.class);
            if (ObjectUtil.isNotNull(method)){
                method.invoke(interceptorRegistration, Ordered.HIGHEST_PRECEDENCE);
            }
        }catch (Exception e){

        }
        interceptorRegistration = registry.addInterceptor(new TLogWebInvokeTimeInterceptor());
        //这里是为了兼容springboot 1.5.X，1.5.x没有order这个方法
        try{
            Method method = ReflectUtil.getMethod(InterceptorRegistration.class, "order", Integer.class);
            if (ObjectUtil.isNotNull(method)){
                method.invoke(interceptorRegistration, Ordered.HIGHEST_PRECEDENCE);
            }
        }catch (Exception e){

        }
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer pathMatchConfigurer) {

    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer contentNegotiationConfigurer) {

    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer asyncSupportConfigurer) {

    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer defaultServletHandlerConfigurer) {

    }

    @Override
    public void addFormatters(FormatterRegistry formatterRegistry) {

    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {

    }

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {

    }

    @Override
    public void addViewControllers(ViewControllerRegistry viewControllerRegistry) {

    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry viewResolverRegistry) {

    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> list) {

    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> list) {

    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> list) {

    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> list) {

    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> list) {

    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> list) {

    }

    @Override
    public Validator getValidator() {
        return null;
    }

    @Override
    public MessageCodesResolver getMessageCodesResolver() {
        return null;
    }
}
