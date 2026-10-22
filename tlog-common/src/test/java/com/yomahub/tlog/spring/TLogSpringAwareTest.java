package com.yomahub.tlog.spring;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.id.TLogDefaultIdGenerator;
import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import com.yomahub.tlog.utils.LocalhostUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TLogSpringAwareTest {

    public static class FixedIdGenerator extends TLogIdGenerator {
        @Override
        public String generateTraceId() {
            Map<String, Object> ext = getExtData();
            return ext == null ? "fixed" : "fixed-" + ext.get("k");
        }
    }

    @AfterEach
    void reset() {
        TLogSpringAware.reset();
        TLogIdGeneratorLoader.setIdGenerator(null);
    }

    @Test
    void staticAccessorsAreSafeWithoutSpring() {
        assertNull(TLogSpringAware.getProperty("any"));
        assertNull(TLogSpringAware.getBean("any"));
        assertNull(TLogSpringAware.getBean(String.class));
        assertEquals(TLogConstants.UNKNOWN, TLogSpringAware.getAppName());
        assertNotNull(TLogSpringAware.registerBean(FixedIdGenerator.class));
    }

    @Test
    void appNameIsReadFromEnvironment() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("t",
                Collections.singletonMap("spring.application.name", "order-service")));
        context.refresh();
        new TLogSpringAware().setApplicationContext(context);
        assertEquals("order-service", TLogSpringAware.getAppName());
        context.close();
    }

    @Test
    void propertyInitLoadsIdGeneratorByClassName() {
        TLogPropertyInit init = new TLogPropertyInit();
        init.setIdGenerator(FixedIdGenerator.class.getName());
        init.afterPropertiesSet();
        assertEquals("fixed", TLogIdGeneratorLoader.getIdGenerator().generate(null));
    }

    @Test
    void propertyInitPrefersInstance() {
        TLogPropertyInit init = new TLogPropertyInit();
        init.setIdGenerator("not.exists.Generator");
        init.setIdGeneratorInstance(new FixedIdGenerator());
        init.afterPropertiesSet();
        assertTrue(TLogIdGeneratorLoader.getIdGenerator() instanceof FixedIdGenerator);
    }

    @Test
    void propertyInitReportsOriginalCause() {
        TLogPropertyInit init = new TLogPropertyInit();
        init.setIdGenerator("not.exists.Generator");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, init::afterPropertiesSet);
        assertTrue(e.getMessage().contains("not.exists.Generator"));
        assertNotNull(e.getCause());
    }

    @Test
    void propertyInitRejectsWrongType() {
        TLogPropertyInit init = new TLogPropertyInit();
        init.setIdGenerator(String.class.getName());
        assertThrows(IllegalArgumentException.class, init::afterPropertiesSet);
    }

    @Test
    void defaultGeneratorClassNameKeepsDefault() {
        TLogPropertyInit init = new TLogPropertyInit();
        init.setIdGenerator(TLogDefaultIdGenerator.class.getName());
        init.afterPropertiesSet();
        assertTrue(TLogIdGeneratorLoader.getIdGenerator() instanceof TLogDefaultIdGenerator);
    }

    @Test
    void extDataIsThreadScopedAndCleared() {
        TLogIdGenerator generator = new FixedIdGenerator();
        Map<String, Object> ext = new HashMap<>();
        ext.put("k", "v");
        assertEquals("fixed-v", generator.generate(ext));
        assertEquals("fixed", generator.generateTraceId());
    }

    @Test
    void localhostNeverReturnsNull() {
        assertNotNull(LocalhostUtil.getHostIp());
        assertNotNull(LocalhostUtil.getHostName());
    }
}
