package com.yomahub.tlog.springboot;

import com.yomahub.tlog.resttemplate.TLogRestTemplateInterceptor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * 为 {@code RestClient.Builder}（Spring 6.1+）添加 TLog 拦截器
 * <p>
 * TLog 以 Spring 5 API 编译，{@code RestClient} 不在编译期可见，因此通过反射调用
 * {@code RestClient.Builder#requestInterceptors(Consumer)}，该方法在 Spring 6.1 ~ 7 中签名一致
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
class RestClientBuilderPostProcessor implements BeanPostProcessor {

    private static final String BUILDER_CLASS = "org.springframework.web.client.RestClient$Builder";

    private final Class<?> builderClass;

    private final Method requestInterceptors;

    RestClientBuilderPostProcessor(ClassLoader classLoader) {
        Class<?> clazz = null;
        Method method = null;
        if (ClassUtils.isPresent(BUILDER_CLASS, classLoader)) {
            clazz = ClassUtils.resolveClassName(BUILDER_CLASS, classLoader);
            method = ReflectionUtils.findMethod(clazz, "requestInterceptors", Consumer.class);
        }
        this.builderClass = method == null ? null : clazz;
        this.requestInterceptors = method;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (builderClass != null && builderClass.isInstance(bean)) {
            Consumer<List<ClientHttpRequestInterceptor>> customizer = interceptors -> {
                if (!TLogHttpClientAutoConfiguration.containsTLog(interceptors)) {
                    interceptors.add(new TLogRestTemplateInterceptor());
                }
            };
            ReflectionUtils.invokeMethod(requestInterceptors, bean, customizer);
        }
        return bean;
    }
}
