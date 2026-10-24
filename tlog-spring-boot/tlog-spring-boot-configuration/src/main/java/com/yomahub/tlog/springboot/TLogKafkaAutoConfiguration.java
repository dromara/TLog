package com.yomahub.tlog.springboot;

import com.yomahub.tlog.kafka.spring.TLogKafkaBeanPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Kafka 的自动装配类：通过消息 header 透传 TLog 上下文
 * <p>
 * 生产端为 {@code DefaultKafkaProducerFactory} 添加 producer 拦截器，消费端为监听容器工厂添加 RecordInterceptor。
 * 用户已有的 interceptor.classes 和 RecordInterceptor 都会保留
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "kafka.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.kafka.core.KafkaTemplate",
        "org.springframework.kafka.listener.RecordInterceptor",
        "com.yomahub.tlog.kafka.spring.TLogKafkaBeanPostProcessor"})
public class TLogKafkaAutoConfiguration {

    @Bean
    public static BeanPostProcessor tLogKafkaBeanPostProcessor() {
        return new TLogKafkaBeanPostProcessor();
    }
}
