package com.yomahub.tlog.rabbitmq.spring;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.config.BaseRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 为容器中的 Spring AMQP 组件添加 TLog 埋点
 * <ul>
 *     <li>{@link RabbitTemplate}：在 beforePublishPostProcessors 末尾追加 {@link TLogRabbitPublishPostProcessor}</li>
 *     <li>监听容器工厂（@RabbitListener）：把 {@link TLogRabbitListenerAdvice} 放在 adviceChain 最外层，
 *     并为监听方法的返回值回复追加 {@link TLogRabbitPublishPostProcessor}</li>
 *     <li>作为 Bean 声明的监听容器：同样处理 adviceChain；容器在初始化时按 adviceChain 生成代理，所以要在初始化之前修改</li>
 * </ul>
 * 用户原有的 post processor 与 advice（例如重试）都保留。2.4 中 RabbitTemplate、工厂没有公开 getter，
 * 因此通过字段读取，字段名在 2.4 ~ 4.x 中一致
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogRabbitBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof AbstractMessageListenerContainer) {
            AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) bean;
            Advice[] chain = (Advice[]) field(AbstractMessageListenerContainer.class, "adviceChain", container);
            if (!containsTLog(chain)) {
                container.setAdviceChain(withTLogFirst(chain));
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof RabbitTemplate) {
            instrumentTemplate((RabbitTemplate) bean);
        } else if (bean instanceof BaseRabbitListenerContainerFactory) {
            instrumentFactory((BaseRabbitListenerContainerFactory<?>) bean);
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    static void instrumentTemplate(RabbitTemplate template) {
        Collection<MessagePostProcessor> current = (Collection<MessagePostProcessor>)
                field(RabbitTemplate.class, "beforePublishPostProcessors", template);
        if (current == null || current.stream().noneMatch(p -> p instanceof TLogRabbitPublishPostProcessor)) {
            template.addBeforePublishPostProcessors(new TLogRabbitPublishPostProcessor());
        }
    }

    static void instrumentFactory(BaseRabbitListenerContainerFactory<?> factory) {
        Advice[] chain = factory.getAdviceChain();
        if (!containsTLog(chain)) {
            factory.setAdviceChain(withTLogFirst(chain));
        }
        MessagePostProcessor[] reply = (MessagePostProcessor[])
                field(BaseRabbitListenerContainerFactory.class, "beforeSendReplyPostProcessors", factory);
        List<MessagePostProcessor> merged = reply == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(reply));
        if (merged.stream().noneMatch(p -> p instanceof TLogRabbitPublishPostProcessor)) {
            merged.add(new TLogRabbitPublishPostProcessor());
            factory.setBeforeSendReplyPostProcessors(merged.toArray(new MessagePostProcessor[0]));
        }
    }

    private static boolean containsTLog(Advice[] chain) {
        return chain != null && Arrays.stream(chain).anyMatch(a -> a instanceof TLogRabbitListenerAdvice);
    }

    /**
     * TLog 在最外层：重试等用户 advice 在 TLog 上下文内执行
     */
    private static Advice[] withTLogFirst(Advice[] chain) {
        List<Advice> merged = new ArrayList<>();
        merged.add(new TLogRabbitListenerAdvice());
        if (chain != null) {
            merged.addAll(Arrays.asList(chain));
        }
        return merged.toArray(new Advice[0]);
    }

    private static Object field(Class<?> declaringClass, String name, Object target) {
        Field field = ReflectionUtils.findField(declaringClass, name);
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, target);
    }
}
