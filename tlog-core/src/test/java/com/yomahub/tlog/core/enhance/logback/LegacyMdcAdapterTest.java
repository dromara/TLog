package com.yomahub.tlog.core.enhance.logback;

import org.junit.jupiter.api.Test;
import org.slf4j.spi.MDCAdapter;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
class LegacyMdcAdapterTest {
    static class CustomLegacyAdapter extends org.slf4j.TLogLogbackTTLMdcAdapter {
    }

    @Test
    void legacyConstructionInheritanceAndInstanceMethodsRemainAvailable() {
        CustomLegacyAdapter adapter = new CustomLegacyAdapter();
        MDCAdapter api = adapter;
        api.put("key", "value");
        assertEquals("value", api.get("key"));
        assertEquals("value", adapter.getPropertyMap().get("key"));
        api.setContextMap(Collections.singletonMap("replacement", "new"));
        assertNull(api.get("key"));
        assertEquals("new", api.getCopyOfContextMap().get("replacement"));
        api.remove("replacement");
        assertNull(api.get("replacement"));
        api.clear();
        assertNull(api.getCopyOfContextMap());
    }
}
