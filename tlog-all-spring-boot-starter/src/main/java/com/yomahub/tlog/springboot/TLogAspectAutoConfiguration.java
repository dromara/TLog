package com.yomahub.tlog.springboot;

import com.yomahub.tlog.core.aop.AspectLogAop;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TLogAspectAutoConfiguration {

    @Bean
    public AspectLogAop aspectLogAop(){
        return new AspectLogAop();
    }
}
