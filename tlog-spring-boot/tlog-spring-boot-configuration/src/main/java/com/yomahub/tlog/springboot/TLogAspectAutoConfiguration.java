package com.yomahub.tlog.springboot;

import com.yomahub.tlog.core.aop.AspectLogAop;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义埋点的springboot自动装配类
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
@Configuration
public class TLogAspectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AspectLogAop.class)
    public AspectLogAop aspectLogAop() {
        return new AspectLogAop();
    }
}
