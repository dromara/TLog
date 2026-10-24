package com.yomahub.tlog.kafka.spring;

import com.yomahub.tlog.kafka.TLogKafkaProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 为容器中的 Spring Kafka 组件添加 TLog 埋点
 * <ul>
 *     <li>{@link DefaultKafkaProducerFactory}：在 {@code interceptor.classes} 末尾追加 {@link TLogKafkaProducerInterceptor}</li>
 *     <li>监听容器工厂（@KafkaListener）和作为 Bean 声明的监听容器：用 {@link TLogKafkaRecordInterceptor} 包住原有的 RecordInterceptor</li>
 * </ul>
 * 不注册 RecordInterceptor 类型的 Bean：Spring Boot 只在该类型 Bean 唯一时才使用它，会与用户自己的拦截器冲突。
 * 2.8 中工厂和容器没有公开的 getRecordInterceptor，因此通过字段读取，字段名在 2.8 ~ 4.x 中一致
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogKafkaBeanPostProcessor implements BeanPostProcessor {

    private static final String RECORD_INTERCEPTOR_FIELD = "recordInterceptor";

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DefaultKafkaProducerFactory) {
            instrumentProducerFactory((DefaultKafkaProducerFactory<?, ?>) bean);
        } else if (bean instanceof AbstractKafkaListenerContainerFactory) {
            AbstractKafkaListenerContainerFactory<?, ?, ?> factory = (AbstractKafkaListenerContainerFactory<?, ?, ?>) bean;
            instrumentListener(factory, AbstractKafkaListenerContainerFactory.class, factory::setRecordInterceptor);
        } else if (bean instanceof AbstractMessageListenerContainer) {
            AbstractMessageListenerContainer<?, ?> container = (AbstractMessageListenerContainer<?, ?>) bean;
            instrumentListener(container, AbstractMessageListenerContainer.class, container::setRecordInterceptor);
        }
        return bean;
    }

    static void instrumentProducerFactory(DefaultKafkaProducerFactory<?, ?> factory) {
        Object current = factory.getConfigurationProperties().get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
        List<String> interceptors = interceptorClassNames(current);
        String tlog = TLogKafkaProducerInterceptor.class.getName();
        if (!interceptors.contains(tlog)) {
            interceptors.add(tlog);
            factory.updateConfigs(Collections.singletonMap(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    String.join(",", interceptors)));
        }
    }

    /**
     * interceptor.classes 可以是逗号分隔的字符串、Class，或二者组成的集合
     */
    static List<String> interceptorClassNames(Object value) {
        List<String> names = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                names.addAll(interceptorClassNames(item));
            }
        } else if (value instanceof Class) {
            names.add(((Class<?>) value).getName());
        } else if (value != null) {
            for (String name : StringUtils.commaDelimitedListToStringArray(value.toString())) {
                if (StringUtils.hasText(name)) {
                    names.add(name.trim());
                }
            }
        }
        return names;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void instrumentListener(Object target, Class<?> declaringClass,
                                           Consumer<RecordInterceptor> setter) {
        Field field = ReflectionUtils.findField(declaringClass, RECORD_INTERCEPTOR_FIELD);
        if (field == null) {
            return;
        }
        ReflectionUtils.makeAccessible(field);
        RecordInterceptor current = (RecordInterceptor) ReflectionUtils.getField(field, target);
        if (!(current instanceof TLogKafkaRecordInterceptor)) {
            setter.accept(new TLogKafkaRecordInterceptor(current));
        }
    }
}
