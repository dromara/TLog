package com.yomahub.tlog.springboot;

import com.yomahub.tlog.spring.TLogSpringAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TLogCommonAutoConfiguration {

    @Bean
    public TLogSpringAware tLogSpringAware(){
        return new TLogSpringAware();
    }
}
