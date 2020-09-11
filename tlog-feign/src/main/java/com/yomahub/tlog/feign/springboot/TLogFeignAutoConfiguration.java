package com.yomahub.tlog.feign.springboot;

import com.yomahub.tlog.feign.filter.TLogFeignFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TLog Feign 配置的springboot自动装配类
 * @author Bryan.Zhang
 * @Date 2020/9/11
 */
@Configuration
public class TLogFeignAutoConfiguration {

    @Bean
    public TLogFeignFilter tLogFeignFilter(){
        return new TLogFeignFilter();
    }
}
