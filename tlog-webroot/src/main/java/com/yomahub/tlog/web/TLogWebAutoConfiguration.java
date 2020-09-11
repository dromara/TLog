package com.yomahub.tlog.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Bryan.Zhang
 * @Date 2020/9/11
 */
@Configuration
@ConditionalOnClass(name = {"org.springframework.web.servlet.config.annotation.WebMvcConfigurer"})
public class TLogWebAutoConfiguration {
    @Bean
    public TLogWebConfig tLogWebConfig(){
        return new TLogWebConfig();
    }
}
