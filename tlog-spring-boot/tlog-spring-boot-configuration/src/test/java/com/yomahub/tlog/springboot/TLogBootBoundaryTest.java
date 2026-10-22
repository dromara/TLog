package com.yomahub.tlog.springboot;

import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.spring.TLogPropertyInit;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.springboot.lifecircle.TLogPropertyConfiguration;
import com.yomahub.tlog.web.filter.TLogServletFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TLogBootBoundaryTest {
    @BeforeEach
    @AfterEach
    void reset() {
        new TLogSpringAware().setApplicationContext(null);
        TLogIdGeneratorLoader.setIdGenerator(null);
    }

    @Test
    void servletApplicationWithoutWebrootSkipsServletConfiguration() {
        new WebApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("com.yomahub.tlog.web"))
                .withConfiguration(AutoConfigurations.of(TLogWebAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("tLogServletFilter");
                    assertThat(context).doesNotHaveBean("tLogJakartaServletFilter");
                });
    }

    @Test
    void servletRegistrationCoversAsyncAndErrorDispatches() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TLogWebAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FilterRegistrationBean<?> registration = context.getBean("tLogServletFilterRegistration", FilterRegistrationBean.class);
                    assertThat(registration.getFilter()).isSameAs(context.getBean(TLogServletFilter.class));
                    assertThat(registration.getOrder()).isEqualTo(Integer.MIN_VALUE + 1);
                    ServletContext servletContext = mock(ServletContext.class);
                    FilterRegistration.Dynamic dynamic = mock(FilterRegistration.Dynamic.class);
                    when(servletContext.addFilter(eq("tlogFilter"), any(javax.servlet.Filter.class))).thenReturn(dynamic);
                    registration.onStartup(servletContext);
                    verify(dynamic).setAsyncSupported(true);
                    verify(dynamic).addMappingForUrlPatterns(
                            EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR), false, "/*");
                });
    }

    @Test
    void disabledMasterSwitchStillInitializesRuntimeState() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(TLogCommonAutoConfiguration.class))
                .withPropertyValues("tlog.enabled=false", "spring.application.name=disabled-app")
                .run(context -> {
                    assertThat(context).hasSingleBean(TLogSpringAware.class);
                    assertThat(TLogRuntime.isEnabled()).isFalse();
                    assertThat(TLogRuntime.getAppName()).isEqualTo("disabled-app");
                });
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(TLogCommonAutoConfiguration.class))
                .run(context -> assertThat(TLogRuntime.isEnabled()).isTrue());
    }

    @Test
    void runtimeIsConfiguredBeforeUserPostConstruct() {
        new ApplicationContextRunner().withUserConfiguration(StartupConfiguration.class)
                .withConfiguration(AutoConfigurations.of(TLogCommonAutoConfiguration.class))
                .withPropertyValues("tlog.enabled=false", "spring.application.name=startup-app")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    StartupProbe probe = context.getBean(StartupProbe.class);
                    assertThat(probe.enabled).isFalse();
                    assertThat(probe.appName).isEqualTo("startup-app");
                });
    }

    static class StartupProbe {
        boolean enabled;
        String appName;

        @PostConstruct
        public void init() {
            enabled = TLogRuntime.isEnabled();
            appName = TLogRuntime.getAppName();
        }
    }

    @Configuration
    static class StartupConfiguration {
        @Bean
        StartupProbe startupProbe() {
            return new StartupProbe();
        }
    }

    @Test
    void earlyPropertyInitStillInjectsConfiguredIdGenerator() {
        new ApplicationContextRunner().withUserConfiguration(EarlyInitialization.class)
                .withConfiguration(AutoConfigurations.of(TLogCommonAutoConfiguration.class, TLogPropertyConfiguration.class))
                .withPropertyValues("tlog.id-generator=" + InjectedIdGenerator.class.getName())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(TLogIdGeneratorLoader.getIdGenerator().generateTraceId()).isEqualTo("injected");
                });
    }

    static class Dependency { }

    public static class InjectedIdGenerator extends TLogIdGenerator {
        @Autowired
        Dependency dependency;

        @Override
        public String generateTraceId() {
            return dependency == null ? "missing" : "injected";
        }
    }

    @Configuration
    static class EarlyInitialization {
        @Bean
        Object early(TLogPropertyInit propertyInit) {
            return new Object();
        }

        @Bean
        Dependency dependency() {
            return new Dependency();
        }
    }
}
