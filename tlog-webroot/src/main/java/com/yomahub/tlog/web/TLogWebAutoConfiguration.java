package com.yomahub.tlog.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnClass(WebMvcConfigurer.class)
public class TLogWebAutoConfiguration {
    @Bean
    public TLogWebConfig tLogWebConfig(){
        return new TLogWebConfig();
    }
}
