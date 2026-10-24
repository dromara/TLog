package com.yomahub.tlog.rabbitmq.spring;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TLogRabbitSpringTest {

    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
        TLogRuntime.setEnabled(true);
    }

    /**
     * 与监听容器相同的调用方式：对 invokeListener(Channel, Object data) 生成代理
     */
    interface Delegate {
        void invokeListener(Object channel, Object data);
    }

    private static Delegate proxy(Delegate target, Advice... advices) {
        ProxyFactory factory = new ProxyFactory();
        for (Advice advice : advices) {
            factory.addAdvice(advice);
        }
        factory.addInterface(Delegate.class);
        factory.setTarget(target);
        return (Delegate) factory.getProxy();
    }

    @Test
    void adviceScopesContextToOneMessageAndUserAdviceRunsInside() {
        List<String> calls = new ArrayList<>();
        MethodInterceptor userRetry = invocation -> {
            calls.add("retry:" + TLogContext.getTraceId());
            return invocation.proceed();
        };
        Delegate delegate = proxy((channel, data) -> {
            calls.add("listener:" + TLogContext.getTraceId() + ":" + TLogContext.getSpanId() + ":" + MDC.get(TLogConstants.TLOG_TRACE_KEY));
            throw new IllegalStateException("boom");
        }, new TLogRabbitListenerAdvice(), userRetry);

        assertThrows(IllegalStateException.class, () -> delegate.invokeListener(null, message("t1")));
        assertEquals(Arrays.asList("retry:t1", "listener:t1:0.1:t1"), calls);
        assertNull(TLogContext.getTraceId());
        assertNull(MDC.get(TLogConstants.MDC_KEY));
    }

    @Test
    void adviceSkipsBatchesAndRespectsSwitch() {
        List<String> seen = new ArrayList<>();
        Delegate delegate = proxy((channel, data) -> seen.add(String.valueOf(TLogContext.getTraceId())), new TLogRabbitListenerAdvice());
        delegate.invokeListener(null, Collections.singletonList(message("t1")));
        TLogRuntime.setEnabled(false);
        delegate.invokeListener(null, message("t2"));
        assertEquals(Arrays.asList("null", "null"), seen);
    }

    @Test
    void messageProcessorBuildsAndCleansContext() {
        List<String> seen = new ArrayList<>();
        TLogRabbitMessageProcessor.process(message("t3"), m -> seen.add(TLogContext.getTraceId()));
        assertEquals(Collections.singletonList("t3"), seen);
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void publishPostProcessorWritesHeadersOnlyWithContext() {
        Message message = new Message(new byte[0], new MessageProperties());
        new TLogRabbitPublishPostProcessor().postProcessMessage(message);
        assertTrue(message.getMessageProperties().getHeaders().isEmpty());

        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        new TLogRabbitPublishPostProcessor().postProcessMessage(message);
        assertEquals("t1", message.getMessageProperties().getHeader(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.1", message.getMessageProperties().getHeader(TLogConstants.TLOG_SPANID_KEY));
    }

    @Test
    @SuppressWarnings("unchecked")
    void templateKeepsUserPostProcessorsAndIsIdempotent() {
        RabbitTemplate template = new RabbitTemplate();
        MessagePostProcessor user = m -> m;
        template.addBeforePublishPostProcessors(user);
        TLogRabbitBeanPostProcessor processor = new TLogRabbitBeanPostProcessor();
        processor.postProcessAfterInitialization(template, "rabbitTemplate");
        processor.postProcessAfterInitialization(template, "rabbitTemplate");
        List<MessagePostProcessor> current = new ArrayList<>((Collection<MessagePostProcessor>) field(template, "beforePublishPostProcessors"));
        assertEquals(2, current.size());
        assertSame(user, current.get(0));
        assertTrue(current.get(1) instanceof TLogRabbitPublishPostProcessor);
    }

    @Test
    void factoryPutsTLogOutermostAndAddsReplyPostProcessorOnce() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        MethodInterceptor userRetry = invocation -> invocation.proceed();
        MessagePostProcessor userReply = m -> m;
        factory.setAdviceChain(userRetry);
        factory.setBeforeSendReplyPostProcessors(userReply);
        TLogRabbitBeanPostProcessor processor = new TLogRabbitBeanPostProcessor();
        processor.postProcessAfterInitialization(factory, "rabbitListenerContainerFactory");
        processor.postProcessAfterInitialization(factory, "rabbitListenerContainerFactory");

        Advice[] chain = factory.getAdviceChain();
        assertEquals(2, chain.length);
        assertTrue(chain[0] instanceof TLogRabbitListenerAdvice);
        assertSame(userRetry, chain[1]);
        MessagePostProcessor[] reply = (MessagePostProcessor[]) field(factory, "beforeSendReplyPostProcessors");
        assertEquals(2, reply.length);
        assertSame(userReply, reply[0]);
        assertTrue(reply[1] instanceof TLogRabbitPublishPostProcessor);
    }

    @Test
    void containerBeanIsInstrumentedBeforeInitialization() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(mock(ConnectionFactory.class));
        TLogRabbitBeanPostProcessor processor = new TLogRabbitBeanPostProcessor();
        processor.postProcessBeforeInitialization(container, "container");
        processor.postProcessBeforeInitialization(container, "container");
        Advice[] chain = (Advice[]) field(container, "adviceChain");
        assertEquals(1, chain.length);
        assertTrue(chain[0] instanceof TLogRabbitListenerAdvice);
    }

    private static Message message(String trace) {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(TLogConstants.TLOG_TRACE_KEY, trace);
        properties.setHeader(TLogConstants.TLOG_SPANID_KEY, "0.1");
        return new Message(new byte[0], properties);
    }

    private static Object field(Object target, String name) {
        Field field = ReflectionUtils.findField(target.getClass(), name);
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, target);
    }
}
