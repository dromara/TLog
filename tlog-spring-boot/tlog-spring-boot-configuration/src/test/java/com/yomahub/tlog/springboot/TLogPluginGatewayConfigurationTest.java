package com.yomahub.tlog.springboot;

import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.webflux.filter.TLogWebFluxFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TLogPluginGatewayConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    @AfterEach
    void reset() {
        TLogRuntime.setEnabled(true);
        new TLogRPCHandler().cleanThreadLocal();
    }

    @Test
    void soulRespectsWebGlobalAndResponseHeaderSwitches() throws Exception {
        verifySwitches(TLogSoulGatewayAutoConfiguration.class,
                "org.dromara.soul.web.handler.SoulWebHandler", "pluginAop");
    }

    @Test
    void shenyuRespectsWebGlobalAndResponseHeaderSwitches() throws Exception {
        verifySwitches(TLogShenyuGatewayAutoConfiguration.class,
                "org.apache.shenyu.web.handler.ShenyuWebHandler", "pluginLogAop");
    }

    private void verifySwitches(Class<?> configuration, String handlerName, String pluginBean) throws Exception {
        // 仅在本测试的独立 ClassLoader 中提供条件类，不能污染其他自动装配测试的 classpath。
        int dot = handlerName.lastIndexOf('.');
        String source = "package " + handlerName.substring(0, dot) + "; public class "
                + handlerName.substring(dot + 1) + " {}";
        Path javaFile = temporaryDirectory.resolve(handlerName.replace('.', '/') + ".java");
        Files.createDirectories(javaFile.getParent());
        Files.write(javaFile, source.getBytes(StandardCharsets.UTF_8));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("构建要求完整 JDK 17+").isNotNull();
        assertThat(compiler.run(null, null, null, "-proc:none", "--release", "8", "-d",
                temporaryDirectory.toString(), javaFile.toString())).isZero();

        try (URLClassLoader loader = new URLClassLoader(new URL[]{temporaryDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            ApplicationContextRunner runner = new ApplicationContextRunner().withClassLoader(loader)
                    .withConfiguration(AutoConfigurations.of(configuration));
            runner.run(context -> {
                assertThat(context).hasSingleBean(TLogWebFluxFilter.class);
                assertThat(context).hasBean(pluginBean);
                MockServerWebExchange exchange = MockServerWebExchange.from(
                        MockServerHttpRequest.get("/route").header("tlogTraceId", "forwarded"));
                context.getBean(TLogWebFluxFilter.class).filter(exchange, forwarded -> {
                    assertThat(forwarded.getRequest().getHeaders().getFirst("tlogSpanId")).isEqualTo("0.1");
                    return Mono.empty();
                }).block();
            });
            runner.withPropertyValues("tlog.web.enabled=false").run(context -> {
                assertThat(context).doesNotHaveBean(TLogWebFluxFilter.class);
                assertThat(context).doesNotHaveBean(pluginBean);
            });
            runner.withPropertyValues("tlog.enabled=false").run(context -> {
                assertThat(context).doesNotHaveBean(TLogWebFluxFilter.class);
                assertThat(context).doesNotHaveBean(pluginBean);
            });
            runner.withPropertyValues("tlog.web.response-header=false").run(context -> {
                MockServerWebExchange exchange = MockServerWebExchange.from(
                        MockServerHttpRequest.get("/route").header("tlogTraceId", "hidden"));
                context.getBean(TLogWebFluxFilter.class).filter(exchange, next -> Mono.empty()).block();
                assertThat(exchange.getResponse().getHeaders().getFirst("tlogTraceId")).isNull();
            });
        }
    }
}
