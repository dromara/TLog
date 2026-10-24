package com.yomahub.tlog.springboot;

import com.yomahub.tlog.kafka.TLogKafkaProducerInterceptor;
import com.yomahub.tlog.kafka.spring.TLogKafkaRecordInterceptor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 与 Spring Boot 自带的 KafkaAutoConfiguration 一起装配，验证生产者工厂与监听容器工厂都被埋点
 */
class TLogKafkaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class, TLogCommonAutoConfiguration.class,
                    TLogKafkaAutoConfiguration.class));

    @Test
    void bootKafkaBeansAreInstrumented() {
        runner.withPropertyValues("spring.kafka.producer.properties.interceptor.classes=com.example.UserInterceptor")
                .run(context -> {
                    DefaultKafkaProducerFactory<?, ?> producerFactory = context.getBean(DefaultKafkaProducerFactory.class);
                    assertThat(producerFactory.getConfigurationProperties().get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG))
                            .isEqualTo("com.example.UserInterceptor," + TLogKafkaProducerInterceptor.class.getName());
                    assertThat(recordInterceptor(context.getBean(ConcurrentKafkaListenerContainerFactory.class)))
                            .isInstanceOf(TLogKafkaRecordInterceptor.class);
                });
    }

    @Test
    void userRecordInterceptorBeanIsKeptAsDelegate() {
        runner.withUserConfiguration(UserInterceptorConfig.class).run(context -> {
            Object current = recordInterceptor(context.getBean(ConcurrentKafkaListenerContainerFactory.class));
            assertThat(current).isInstanceOf(TLogKafkaRecordInterceptor.class);
            assertThat(((TLogKafkaRecordInterceptor<?, ?>) current).getDelegate())
                    .isSameAs(context.getBean("userRecordInterceptor"));
        });
    }

    @Test
    void switchesDisableKafkaInstrumentation() {
        for (String property : new String[]{"tlog.enabled=false", "tlog.kafka.enabled=false"}) {
            runner.withPropertyValues(property).run(context -> {
                assertThat(context).doesNotHaveBean("tLogKafkaBeanPostProcessor");
                assertThat(context.getBean(DefaultKafkaProducerFactory.class).getConfigurationProperties())
                        .doesNotContainKey(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
                assertThat(recordInterceptor(context.getBean(ConcurrentKafkaListenerContainerFactory.class))).isNull();
            });
        }
    }

    private static Object recordInterceptor(Object factory) {
        Field field = ReflectionUtils.findField(factory.getClass(), "recordInterceptor");
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, factory);
    }

    @Configuration
    static class UserInterceptorConfig {
        @Bean
        RecordInterceptor<Object, Object> userRecordInterceptor() {
            return new RecordInterceptor<Object, Object>() {
                @Override
                public org.apache.kafka.clients.consumer.ConsumerRecord<Object, Object> intercept(
                        org.apache.kafka.clients.consumer.ConsumerRecord<Object, Object> record) {
                    return record;
                }
            };
        }
    }
}
