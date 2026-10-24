package com.yomahub.tlog.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Kafka 原生生产者拦截器，把 TLog 上下文写入消息 header
 * <p>
 * {@code KafkaProducer#send} 在调用线程上执行 {@link #onSend}，因此能读到业务线程的上下文。
 * 原生客户端通过 {@code interceptor.classes=com.yomahub.tlog.kafka.TLogKafkaProducerInterceptor} 接入；
 * Spring Kafka 的 {@code DefaultKafkaProducerFactory} 由自动装配添加
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogKafkaProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

    private static final Logger log = LoggerFactory.getLogger(TLogKafkaProducerInterceptor.class);

    @Override
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        try {
            TLogKafkaHeaders.inject(record.headers());
        } catch (IllegalStateException e) {
            // 已发送过的 ProducerRecord 的 header 是只读的，重复发送时保留原 header
            log.debug("[TLOG]Kafka消息header为只读，本次发送不写入traceId");
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
