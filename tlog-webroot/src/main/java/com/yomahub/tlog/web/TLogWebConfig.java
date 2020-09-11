package com.yomahub.tlog.web;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Bryan.Zhang
 * @since 2020/9/11
 */
public class TLogWebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TLogWebInterceptor());
    }
}
