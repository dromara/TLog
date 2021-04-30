package com.yomahub.tlog.springboot;

import com.yomahub.tlog.web.TLogWebConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TLog web层面的自动装配类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(name = {"org.springframework.web.servlet.config.annotation.WebMvcConfigurer", "org.springframework.boot.web.servlet.FilterRegistrationBean"})
public class TLogWebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(TLogWebConfig.class)
    public TLogWebConfig tLogWebConfig(){
        return new TLogWebConfig();
    }

    //目前先屏蔽掉打印body的功能
    /*@Bean
    public FilterRegistrationBean<ReplaceStreamFilter> filterRegistration() {
        FilterRegistrationBean<ReplaceStreamFilter> registration = new FilterRegistrationBean<>();
        // 设置自定义拦截器
        registration.setFilter(new ReplaceStreamFilter());
        // 设置拦截路径
        registration.addUrlPatterns("/*");
        // 设置优先级（保证tlog过滤器最先执行）
        registration.setOrder(-999);
        return registration;
    }*/
}
