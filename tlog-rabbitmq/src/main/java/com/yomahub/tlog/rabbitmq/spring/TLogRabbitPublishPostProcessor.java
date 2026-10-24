package com.yomahub.tlog.rabbitmq.spring;

import com.yomahub.tlog.context.TLogPropagator;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * Spring AMQP 发送前的处理器，把 TLog 上下文写入消息 header
 * <p>
 * 由自动装配加入 {@code RabbitTemplate} 的 beforePublishPostProcessors 和 @RabbitListener 的回复处理器；
 * RabbitTemplate 在调用线程执行它，因此能读到业务线程的上下文
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogRabbitPublishPostProcessor implements MessagePostProcessor {

    @Override
    public Message postProcessMessage(Message message) {
        TLogPropagator.inject(message.getMessageProperties()::setHeader);
        return message;
    }
}
