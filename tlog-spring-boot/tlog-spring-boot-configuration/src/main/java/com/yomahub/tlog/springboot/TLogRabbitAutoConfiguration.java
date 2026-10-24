package com.yomahub.tlog.springboot;

import com.yomahub.tlog.rabbitmq.spring.TLogRabbitBeanPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AMQP（RabbitMQ）的自动装配类：通过消息 header 透传 TLog 上下文
 * <p>
 * 生产端为 {@code RabbitTemplate} 添加发送前处理器，消费端为监听容器工厂添加 advice 和回复处理器。
 * 用户已有的 post processor 和 advice 都会保留
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "rabbitmq.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.amqp.rabbit.core.RabbitTemplate",
        "com.yomahub.tlog.rabbitmq.spring.TLogRabbitBeanPostProcessor"})
public class TLogRabbitAutoConfiguration {

    @Bean
    public static BeanPostProcessor tLogRabbitBeanPostProcessor() {
        return new TLogRabbitBeanPostProcessor();
    }
}
