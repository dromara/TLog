package com.yomahub.tlog.springboot;

import com.yomahub.tlog.xxljob.aop.TLogXxlJobAop;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxljob的自动装配类
 *
 * @author Bryan.Zhang
 * @since 1.3.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"com.xxl.job.core.handler.annotation.XxlJob", "com.yomahub.tlog.xxljob.aop.TLogXxlJobAop"})
public class TLogXxljobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TLogXxlJobAop.class)
    public TLogXxlJobAop tLogXxlJobAop() {
        return new TLogXxlJobAop();
    }
}
