package com.yomahub.tlog.kafka.spring;

import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.kafka.TLogKafkaHeaders;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * Spring Kafka 单条消息监听的拦截器：在监听容器的消费线程上按 header 建立上下文
 * <p>
 * 上下文在 {@link #intercept} 中建立，在 {@link #afterRecord}（每条消息处理完，含失败）中清理；
 * 容器每轮 poll 结束时还会调用 {@link #clearThreadState}，作为异常退出时的兜底清理。
 * 用户原有的 RecordInterceptor 作为 delegate 保留，并在 TLog 上下文内执行。
 * <p>
 * 以 spring-kafka 2.8 编译；3.x / 4.x 中 {@code intercept(record, consumer)} 及其余方法签名不变。
 * 批量监听（BatchListener）一批消息可能来自多条链路，不经过本拦截器，请对每条消息使用
 * {@link com.yomahub.tlog.kafka.TLogKafkaConsumerProcessor}
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogKafkaRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

    private final TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    private final RecordInterceptor<K, V> delegate;

    public TLogKafkaRecordInterceptor() {
        this(null);
    }

    public TLogKafkaRecordInterceptor(RecordInterceptor<K, V> delegate) {
        this.delegate = delegate;
    }

    public RecordInterceptor<K, V> getDelegate() {
        return delegate;
    }

    /**
     * spring-kafka 2.x 的抽象方法，3.x 起已移除
     */
    public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record) {
        return intercept(record, null);
    }

    @Override
    public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        if (TLogRuntime.isEnabled()) {
            tLogRPCHandler.processProviderSide(name -> TLogKafkaHeaders.read(record.headers(), name));
        }
        return delegate == null ? record : delegate.intercept(record, consumer);
    }

    @Override
    public void success(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        if (delegate != null) {
            delegate.success(record, consumer);
        }
    }

    @Override
    public void failure(ConsumerRecord<K, V> record, Exception exception, Consumer<K, V> consumer) {
        if (delegate != null) {
            delegate.failure(record, exception, consumer);
        }
    }

    @Override
    public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        try {
            if (delegate != null) {
                delegate.afterRecord(record, consumer);
            }
        } finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }

    @Override
    public void setupThreadState(Consumer<?, ?> consumer) {
        if (delegate != null) {
            delegate.setupThreadState(consumer);
        }
    }

    @Override
    public void clearThreadState(Consumer<?, ?> consumer) {
        try {
            if (delegate != null) {
                delegate.clearThreadState(consumer);
            }
        } finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }
}
