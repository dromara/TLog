package com.yomahub.tlog.kafka.spring;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.kafka.TLogKafkaProducerInterceptor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TLogKafkaSpringTest {

    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
    }

    @Test
    void recordInterceptorScopesContextToOneRecordAndKeepsDelegate() {
        List<String> calls = new ArrayList<>();
        RecordInterceptor<String, String> user = new RecordInterceptor<String, String>() {
            @Override
            public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record, Consumer<String, String> consumer) {
                calls.add("intercept:" + TLogContext.getTraceId());
                return record;
            }

            // spring-kafka 2.8 中该方法为抽象方法
            public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record) {
                return intercept(record, null);
            }

            @Override
            public void afterRecord(ConsumerRecord<String, String> record, Consumer<String, String> consumer) {
                calls.add("after:" + TLogContext.getTraceId());
            }
        };
        TLogKafkaRecordInterceptor<String, String> interceptor = new TLogKafkaRecordInterceptor<>(user);

        ConsumerRecord<String, String> record = record("t1");
        assertSame(record, interceptor.intercept(record, null));
        assertEquals("t1", TLogContext.getTraceId());
        assertEquals("t1", MDC.get(TLogConstants.TLOG_TRACE_KEY));
        interceptor.afterRecord(record, null);
        assertNull(TLogContext.getTraceId());
        assertNull(MDC.get(TLogConstants.MDC_KEY));
        assertEquals(Arrays.asList("intercept:t1", "after:t1"), calls);
    }

    @Test
    void clearThreadStateIsSafetyNet() {
        TLogKafkaRecordInterceptor<String, String> interceptor = new TLogKafkaRecordInterceptor<>();
        interceptor.intercept(record("t2"), null);
        interceptor.clearThreadState(null);
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void producerFactoryKeepsUserInterceptorsAndIsIdempotent() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, Arrays.asList("com.example.UserInterceptor"));
        DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(configs);
        TLogKafkaBeanPostProcessor processor = new TLogKafkaBeanPostProcessor();
        processor.postProcessAfterInitialization(factory, "producerFactory");
        processor.postProcessAfterInitialization(factory, "producerFactory");
        assertEquals("com.example.UserInterceptor," + TLogKafkaProducerInterceptor.class.getName(),
                factory.getConfigurationProperties().get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG));
    }

    @Test
    void interceptorClassNamesAcceptAllConfigShapes() {
        assertEquals(Arrays.asList("a.A", "b.B"), TLogKafkaBeanPostProcessor.interceptorClassNames(" a.A , b.B "));
        assertEquals(Arrays.asList(String.class.getName(), "c.C"),
                TLogKafkaBeanPostProcessor.interceptorClassNames(Arrays.asList(String.class, "c.C")));
        assertTrue(TLogKafkaBeanPostProcessor.interceptorClassNames(null).isEmpty());
    }

    @Test
    void listenerFactoryWrapsExistingInterceptorOnce() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        RecordInterceptor<String, String> user = new RecordInterceptor<String, String>() {
            @Override
            public ConsumerRecord<String, String> intercept(ConsumerRecord<String, String> record) {
                return record;
            }
        };
        factory.setRecordInterceptor(user);
        TLogKafkaBeanPostProcessor processor = new TLogKafkaBeanPostProcessor();
        processor.postProcessAfterInitialization(factory, "kafkaListenerContainerFactory");
        processor.postProcessAfterInitialization(factory, "kafkaListenerContainerFactory");
        Object current = recordInterceptor(factory);
        assertTrue(current instanceof TLogKafkaRecordInterceptor);
        assertSame(user, ((TLogKafkaRecordInterceptor<?, ?>) current).getDelegate());
    }

    private static Object recordInterceptor(Object factory) {
        Field field = ReflectionUtils.findField(factory.getClass(), "recordInterceptor");
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, factory);
    }

    private static ConsumerRecord<String, String> record(String trace) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0, "k", "v");
        record.headers().add(TLogConstants.TLOG_TRACE_KEY, trace.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
