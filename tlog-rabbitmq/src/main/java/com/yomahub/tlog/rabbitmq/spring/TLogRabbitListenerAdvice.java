package com.yomahub.tlog.rabbitmq.spring;

import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.rabbitmq.TLogRabbitHeaders;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.Message;

/**
 * Spring AMQP 监听容器的 advice：在消费线程上按 header 建立上下文，监听方法返回或抛异常后清理
 * <p>
 * 监听容器用 adviceChain 代理 {@code invokeListener(Channel, Object data)}。自动装配把它放在 adviceChain 的最外层，
 * 用户的 advice（例如 Spring Boot 开启重试时的 RetryInterceptor）保留在内层，所以同一条消息的多次重试共用一个 traceId。
 * <p>
 * 批量监听时 data 是 {@code List<Message>}，一批消息可能来自多条链路，这里不处理；
 * 请对每条消息使用 {@link TLogRabbitMessageProcessor}
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogRabbitListenerAdvice implements MethodInterceptor {

    private final TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        if (!TLogRuntime.isEnabled() || arguments.length < 2 || !(arguments[1] instanceof Message)) {
            return invocation.proceed();
        }
        Message message = (Message) arguments[1];
        tLogRPCHandler.processProviderSide(name -> TLogRabbitHeaders.read(message.getMessageProperties().getHeaders(), name));
        try {
            return invocation.proceed();
        } finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }
}
