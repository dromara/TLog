package com.yomahub.tlog.web;

import com.yomahub.tlog.web.interceptor.TLogWebInterceptor;
import com.yomahub.tlog.web.interceptor.TLogWebInvokeTimeInterceptor;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * TLog webconfig类（javax，供非 Spring Boot 的 Spring MVC 项目在 XML 中声明）
 * <p>
 * Spring Boot 项目由自动装配注册 Servlet Filter，无需声明本类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TLogWebInterceptor()).order(Ordered.HIGHEST_PRECEDENCE);
        registry.addInterceptor(new TLogWebInvokeTimeInterceptor()).order(Ordered.HIGHEST_PRECEDENCE);
    }
}
