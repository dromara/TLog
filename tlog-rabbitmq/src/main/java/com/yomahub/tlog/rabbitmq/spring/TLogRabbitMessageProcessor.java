package com.yomahub.tlog.rabbitmq.spring;

import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.mq.TLogMqRunner;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.rabbitmq.TLogRabbitHeaders;
import org.springframework.amqp.core.Message;

/**
 * Spring AMQP 消息的处理器：按 header 建立 TLog 上下文，处理完后清理
 * <p>
 * 用于批量监听（{@code List<Message>}）或自己调用 {@code RabbitTemplate#receive} 的场景，对每条消息调用一次
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogRabbitMessageProcessor {

    private static final TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    private TLogRabbitMessageProcessor() {
    }

    public static void process(Message message, TLogMqRunner<Message> runner) {
        if (!TLogRuntime.isEnabled()) {
            runner.mqConsume(message);
            return;
        }
        tLogRPCHandler.processProviderSide(name -> TLogRabbitHeaders.read(message.getMessageProperties().getHeaders(), name));
        try {
            runner.mqConsume(message);
        } finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }
}
