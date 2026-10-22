package com.yomahub.tlog.springboot;

import com.yomahub.tlog.resttemplate.TLogRestTemplateInterceptor;
import com.yomahub.tlog.webflux.client.TLogWebClientFilter;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 客户端自动埋点
 * <p>
 * 通过 BeanPostProcessor 为容器中的 {@link RestTemplate}、{@code WebClient.Builder}、{@code RestClient.Builder} 添加 TLog 拦截器。
 * 不依赖 Spring Boot 各版本的 Customizer 接口（RestClientCustomizer 在 SB 3.2 才出现，SB 4 又调整了包名），
 * 因此同一份代码适用于 Spring Boot 2 ~ 4。用户自行 new 的客户端需手工添加拦截器。
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "http-client.enabled"}, havingValue = "true", matchIfMissing = true)
public class TLogHttpClientAutoConfiguration {

    @Configuration
    @ConditionalOnClass(name = {"org.springframework.web.client.RestTemplate", "com.yomahub.tlog.resttemplate.TLogRestTemplateInterceptor"})
    static class RestTemplateConfiguration {

        @Bean
        public static BeanPostProcessor tLogRestTemplatePostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof RestTemplate) {
                        RestTemplate restTemplate = (RestTemplate) bean;
                        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
                        if (!containsTLog(interceptors)) {
                            List<ClientHttpRequestInterceptor> merged = new ArrayList<>(interceptors);
                            merged.add(new TLogRestTemplateInterceptor());
                            restTemplate.setInterceptors(merged);
                        }
                    }
                    return bean;
                }
            };
        }
    }

    @Configuration
    @ConditionalOnClass(name = {"org.springframework.web.client.RestClient", "com.yomahub.tlog.resttemplate.TLogRestTemplateInterceptor"})
    static class RestClientConfiguration {

        @Bean
        public static BeanPostProcessor tLogRestClientBuilderPostProcessor() {
            return new RestClientBuilderPostProcessor(RestClientConfiguration.class.getClassLoader());
        }
    }

    @Configuration
    @ConditionalOnClass(name = {"org.springframework.web.reactive.function.client.WebClient", "com.yomahub.tlog.webflux.client.TLogWebClientFilter"})
    static class WebClientConfiguration {

        @Bean
        public static BeanPostProcessor tLogWebClientBuilderPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof WebClient.Builder) {
                        ((WebClient.Builder) bean).filters(filters -> {
                            if (filters.stream().noneMatch(f -> f instanceof TLogWebClientFilter)) {
                                filters.add(new TLogWebClientFilter());
                            }
                        });
                    }
                    return bean;
                }
            };
        }
    }

    static boolean containsTLog(List<ClientHttpRequestInterceptor> interceptors) {
        for (ClientHttpRequestInterceptor interceptor : interceptors) {
            if (interceptor instanceof TLogRestTemplateInterceptor) {
                return true;
            }
        }
        return false;
    }
}
