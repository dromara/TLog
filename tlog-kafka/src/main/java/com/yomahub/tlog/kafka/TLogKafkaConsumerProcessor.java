package com.yomahub.tlog.kafka;

import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.mq.TLogMqRunner;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * 原生 Kafka 消费者的处理器：按消息 header 建立 TLog 上下文，处理完后清理
 * <p>
 * Kafka 的 ConsumerInterceptor 拿到的是整批消息，且不在业务处理的时机执行，不能用来建立上下文。
 * 自己写 poll 循环的应用对每条消息调用 {@link #process}；Spring Kafka 的 @KafkaListener 由自动装配处理
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogKafkaConsumerProcessor {

    private static final TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    private TLogKafkaConsumerProcessor() {
    }

    public static <K, V> void process(ConsumerRecord<K, V> record, TLogMqRunner<ConsumerRecord<K, V>> runner) {
        if (!TLogRuntime.isEnabled()) {
            runner.mqConsume(record);
            return;
        }
        tLogRPCHandler.processProviderSide(name -> TLogKafkaHeaders.read(record.headers(), name));
        try {
            runner.mqConsume(record);
        } finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }
}
