package com.yomahub.tlog.springboot;

import com.yomahub.tlog.task.spring.SpringScheduledTaskAop;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Scheduled AOP自动装配类
 * <p>
 * 以 tlog-task 中的类作为条件，未引入 tlog-task 的 starter（如 gateway）不会因类缺失而启动失败
 *
 * @author Bryan.Zhang
 * @since 1.3.4
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "scheduled.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"com.yomahub.tlog.task.spring.SpringScheduledTaskAop", "org.aspectj.lang.annotation.Aspect"})
public class TLogSpringScheduledAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SpringScheduledTaskAop.class)
    public SpringScheduledTaskAop springScheduledTaskAop() {
        return new SpringScheduledTaskAop();
    }
}
