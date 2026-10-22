package com.yomahub.tlog.context;

import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

class TLogPlainJavaTest {
    @Test
    void propagatesInAnIsolatedClasspathWithoutAnySpringJar() throws Exception {
        Class<?>[] dependencies = {TLogPropagator.class, StrUtil.class, TransmittableThreadLocal.class,
                StringUtils.class, LoggerFactory.class};
        URL[] urls = new URL[dependencies.length];
        for (int i = 0; i < dependencies.length; i++) {
            urls[i] = dependencies[i].getProtectionDomain().getCodeSource().getLocation();
        }
        try (URLClassLoader loader = new URLClassLoader(urls, null)) {
            assertThrows(ClassNotFoundException.class,
                    () -> loader.loadClass("org.springframework.context.ApplicationContextAware"));
            Class<?> context = loader.loadClass(TLogContext.class.getName());
            Class<?> runtime = loader.loadClass(TLogRuntime.class.getName());
            Class<?> propagator = loader.loadClass(TLogPropagator.class.getName());
            context.getMethod("putTraceId", String.class).invoke(null, "plain-trace");
            runtime.getMethod("setAppName", String.class).invoke(null, "plain-app");
            Map<String, String> headers = new HashMap<>();
            assertEquals(true, propagator.getMethod("inject", BiConsumer.class)
                    .invoke(null, (BiConsumer<String, String>) headers::put));
            assertEquals("plain-trace", headers.get("tlogTraceId"));
            assertEquals("plain-app", headers.get("preIvkApp"));
            assertEquals("0.1", headers.get("tlogSpanId"));
            runtime.getMethod("setEnabled", boolean.class).invoke(null, false);
            headers.clear();
            assertEquals(false, propagator.getMethod("inject", BiConsumer.class)
                    .invoke(null, (BiConsumer<String, String>) headers::put));
            assertTrue(headers.isEmpty());
        }
    }
}
