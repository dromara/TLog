package com.yomahub.tlog.rabbitmq;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.mq.TLogMqRunner;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;

import java.util.Map;

/**
 * 原生 RabbitMQ 客户端（amqp-client）的消费端处理器：按消息 header 建立 TLog 上下文，处理完后清理
 * <pre>
 * channel.basicConsume(queue, autoAck, TLogRabbitConsumerProcessor.wrap((tag, delivery) -&gt; { ... }), tag -&gt; { });
 * </pre>
 * Spring AMQP 的 @RabbitListener 由自动装配处理
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogRabbitConsumerProcessor {

    private static final TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    private TLogRabbitConsumerProcessor() {
    }

    public static void process(Delivery delivery, TLogMqRunner<Delivery> runner) {
        if (!TLogRuntime.isEnabled()) {
            runner.mqConsume(delivery);
            return;
        }
        open(delivery);
        try {
            runner.mqConsume(delivery);
        } finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }

    /**
     * 包装 DeliverCallback，回调在 TLog 上下文内执行
     */
    public static DeliverCallback wrap(DeliverCallback callback) {
        return (consumerTag, delivery) -> {
            if (!TLogRuntime.isEnabled()) {
                callback.handle(consumerTag, delivery);
                return;
            }
            open(delivery);
            try {
                callback.handle(consumerTag, delivery);
            } finally {
                tLogRPCHandler.cleanThreadLocal();
            }
        };
    }

    private static void open(Delivery delivery) {
        Map<String, Object> headers = delivery.getProperties() == null ? null : delivery.getProperties().getHeaders();
        tLogRPCHandler.processProviderSide(name -> TLogRabbitHeaders.read(headers, name));
    }
}
