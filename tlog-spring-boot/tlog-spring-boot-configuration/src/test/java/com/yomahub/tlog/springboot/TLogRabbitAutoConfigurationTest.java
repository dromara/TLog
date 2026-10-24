package com.yomahub.tlog.springboot;

import com.yomahub.tlog.rabbitmq.spring.TLogRabbitListenerAdvice;
import com.yomahub.tlog.rabbitmq.spring.TLogRabbitPublishPostProcessor;
import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 与 Spring Boot 自带的 RabbitAutoConfiguration 一起装配（不连 broker），验证模板和监听容器工厂都被埋点
 */
class TLogRabbitAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class, TLogCommonAutoConfiguration.class,
                    TLogRabbitAutoConfiguration.class));

    @Test
    void bootRabbitBeansAreInstrumentedAndRetryAdviceIsKept() {
        runner.withPropertyValues("spring.rabbitmq.listener.simple.retry.enabled=true").run(context -> {
            assertThat(publishProcessors(context.getBean(RabbitTemplate.class)))
                    .hasAtLeastOneElementOfType(TLogRabbitPublishPostProcessor.class);
            SimpleRabbitListenerContainerFactory factory = context.getBean(SimpleRabbitListenerContainerFactory.class);
            Advice[] chain = factory.getAdviceChain();
            assertThat(chain).hasSize(2);
            assertThat(chain[0]).isInstanceOf(TLogRabbitListenerAdvice.class);
            assertThat(chain[1].getClass().getName()).contains("Retry");
            assertThat((MessagePostProcessor[]) field(factory, "beforeSendReplyPostProcessors"))
                    .hasAtLeastOneElementOfType(TLogRabbitPublishPostProcessor.class);
        });
    }

    @Test
    void switchesDisableRabbitInstrumentation() {
        for (String property : new String[]{"tlog.enabled=false", "tlog.rabbitmq.enabled=false"}) {
            runner.withPropertyValues(property).run(context -> {
                assertThat(context).doesNotHaveBean("tLogRabbitBeanPostProcessor");
                Collection<?> processors = publishProcessors(context.getBean(RabbitTemplate.class));
                assertThat(processors == null || processors.isEmpty()).isTrue();
                assertThat(context.getBean(SimpleRabbitListenerContainerFactory.class).getAdviceChain()).isNull();
            });
        }
    }

    private static Collection<?> publishProcessors(RabbitTemplate template) {
        return (Collection<?>) field(template, "beforePublishPostProcessors");
    }

    private static Object field(Object target, String name) {
        Field field = ReflectionUtils.findField(target.getClass(), name);
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, target);
    }
}
