package com.yomahub.tlog.springboot;

import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.aop.AspectLogAop;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.id.TLogDefaultIdGenerator;
import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.resttemplate.TLogRestTemplateInterceptor;
import com.yomahub.tlog.spring.TLogPropertyInit;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.springboot.lifecircle.TLogPropertyConfiguration;
import com.yomahub.tlog.web.filter.TLogJakartaServletFilter;
import com.yomahub.tlog.web.filter.TLogServletFilter;
import com.yomahub.tlog.webflux.client.TLogWebClientFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配的条件测试，运行在 Spring Boot 2.7 / Spring 5.3（javax）环境；jakarta 路径由 SB3/4 冒烟应用覆盖
 */
class TLogAutoConfigurationTest {

    private static final AutoConfigurations ALL = AutoConfigurations.of(
            TLogCommonAutoConfiguration.class,
            TLogPropertyConfiguration.class,
            TLogAspectAutoConfiguration.class,
            TLogAsyncAutoConfiguration.class,
            TLogSpringScheduledAutoConfiguration.class,
            TLogWebAutoConfiguration.class,
            TLogWebFluxAutoConfiguration.class,
            TLogHttpClientAutoConfiguration.class,
            TLogFeignAutoConfiguration.class,
            TLogGatewayAutoConfiguration.class,
            TLogXxljobAutoConfiguration.class);

    @AfterEach
    void reset() {
        TLogLabelGenerator.setLabelPattern(null);
        TLogIdGeneratorLoader.setIdGenerator(null);
        TLogRPCHandler.setW3cTraceContextEnabled(true);
    }

    @Test
    void servletAppRegistersJavaxFilterOnly() {
        new WebApplicationContextRunner().withConfiguration(ALL).run(context -> {
            assertThat(context).hasSingleBean(TLogServletFilter.class);
            assertThat(context).doesNotHaveBean(TLogJakartaServletFilter.class);
            assertThat(context).hasSingleBean(TLogSpringAware.class);
            assertThat(context).hasSingleBean(TLogPropertyInit.class);
            assertThat(context).hasSingleBean(AspectLogAop.class);
            assertThat(context).hasSingleBean(TaskDecorator.class);
        });
    }

    @Test
    void nonWebAppDoesNotRegisterFilter() {
        new ApplicationContextRunner().withConfiguration(ALL).run(context -> {
            assertThat(context).doesNotHaveBean(TLogServletFilter.class);
            assertThat(context).hasSingleBean(TLogPropertyInit.class);
        });
    }

    @Test
    void masterSwitchDisablesEverything() {
        new WebApplicationContextRunner().withConfiguration(ALL)
                .withPropertyValues("tlog.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TLogServletFilter.class);
                    assertThat(context).doesNotHaveBean(TLogPropertyInit.class);
                    assertThat(context).doesNotHaveBean(AspectLogAop.class);
                    assertThat(context).doesNotHaveBean(TaskDecorator.class);
                });
    }

    @Test
    void featureSwitchesWorkIndependently() {
        new WebApplicationContextRunner().withConfiguration(ALL)
                .withPropertyValues("tlog.web.enabled=false", "tlog.async.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TLogServletFilter.class);
                    assertThat(context).doesNotHaveBean(TaskDecorator.class);
                    assertThat(context).hasSingleBean(TLogPropertyInit.class);
                });
    }

    @Test
    void legacyPropertiesStillBind() {
        new ApplicationContextRunner().withConfiguration(ALL)
                .withPropertyValues("tlog.pattern=[$traceId]", "tlog.enableInvokeTimePrint=true", "tlog.w3c-trace-context=false")
                .run(context -> {
                    assertThat(TLogLabelGenerator.getLabelPattern()).isEqualTo("[$traceId]");
                    assertThat(TLogRPCHandler.isW3cTraceContextEnabled()).isFalse();
                });
    }

    public static class BeanIdGenerator extends TLogIdGenerator {
        @Override
        public String generateTraceId() {
            return "from-bean";
        }
    }

    @Configuration
    static class IdGeneratorConfig {
        @Bean
        BeanIdGenerator beanIdGenerator() {
            return new BeanIdGenerator();
        }
    }

    @Test
    void idGeneratorBeanIsPickedUp() {
        new ApplicationContextRunner().withConfiguration(ALL).withUserConfiguration(IdGeneratorConfig.class)
                .run(context -> assertThat(TLogIdGeneratorLoader.getIdGenerator().generate(null)).isEqualTo("from-bean"));
    }

    @Test
    void defaultIdGeneratorWhenNothingConfigured() {
        new ApplicationContextRunner().withConfiguration(ALL)
                .run(context -> assertThat(TLogIdGeneratorLoader.getIdGenerator()).isInstanceOf(TLogDefaultIdGenerator.class));
    }

    @Configuration
    static class UserDecoratorConfig {
        @Bean
        TaskDecorator userDecorator() {
            return runnable -> runnable;
        }
    }

    @Test
    void userTaskDecoratorIsNotOverridden() {
        new ApplicationContextRunner().withConfiguration(ALL).withUserConfiguration(UserDecoratorConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TaskDecorator.class);
                    assertThat(context).hasBean("userDecorator");
                });
    }

    @Configuration
    static class ClientConfig {
        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Test
    void httpClientsAreInstrumentedOnce() {
        new ApplicationContextRunner().withConfiguration(ALL).withUserConfiguration(ClientConfig.class)
                .run(context -> {
                    RestTemplate restTemplate = context.getBean(RestTemplate.class);
                    assertThat(restTemplate.getInterceptors()).hasAtLeastOneElementOfType(TLogRestTemplateInterceptor.class);
                    assertThat(restTemplate.getInterceptors().stream().filter(i -> i instanceof TLogRestTemplateInterceptor)).hasSize(1);

                    List<ExchangeFilterFunction> filters = new ArrayList<>();
                    context.getBean(WebClient.Builder.class).filters(filters::addAll);
                    assertThat(filters).hasAtLeastOneElementOfType(TLogWebClientFilter.class);
                });
    }

    @Test
    void httpClientInstrumentationCanBeDisabled() {
        new ApplicationContextRunner().withConfiguration(ALL).withUserConfiguration(ClientConfig.class)
                .withPropertyValues("tlog.http-client.enabled=false")
                .run(context -> assertThat(context.getBean(RestTemplate.class).getInterceptors()).isEmpty());
    }
}
