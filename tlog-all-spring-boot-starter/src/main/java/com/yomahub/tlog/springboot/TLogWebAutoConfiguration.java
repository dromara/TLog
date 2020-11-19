package com.yomahub.tlog.springboot;

import com.yomahub.tlog.web.TLogWebConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TLog web层面的自动装配类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(name = {"org.springframework.web.servlet.config.annotation.WebMvcConfigurer"})
public class TLogWebAutoConfiguration {
    @Bean
    public TLogWebConfig tLogWebConfig(){
        return new TLogWebConfig();
    }
}
