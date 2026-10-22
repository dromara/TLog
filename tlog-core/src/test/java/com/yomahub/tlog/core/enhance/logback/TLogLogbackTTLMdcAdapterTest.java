package com.yomahub.tlog.core.enhance.logback;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogLogbackTTLMdcAdapterTest {
    @Test
    void inheritedMdcIsASnapshotAndExposedCopyDoesNotMutateIt() throws Exception {
        TLogLogbackTTLMdcAdapter adapter = new TLogLogbackTTLMdcAdapter();
        CountDownLatch read = new CountDownLatch(1);
        AtomicReference<String> childValue = new AtomicReference<>();
        adapter.put("key", "parent-at-creation");
        Thread child = new Thread(() -> {
            try {
                if (read.await(5, TimeUnit.SECONDS)) {
                    childValue.set(adapter.get("key"));
                    adapter.put("key", "child-change");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        adapter.put("key", "parent-later");
        child.start();
        read.countDown();
        child.join(5000);
        assertFalse(child.isAlive());
        assertEquals("parent-at-creation", childValue.get());
        assertEquals("parent-later", adapter.get("key"));
        Map<String, String> copy = adapter.getCopyOfContextMap();
        copy.put("key", "copy-change");
        assertEquals("parent-later", adapter.get("key"));
        adapter.clear();
    }

    @Test
    void slf4j2StackMethodsAreThreadLocalAndLastInFirstOut() throws Exception {
        TLogLogbackTTLMdcAdapter adapter = new TLogLogbackTTLMdcAdapter();
        adapter.pushByKey("stack", "first");
        adapter.pushByKey("stack", "second");
        AtomicReference<String> child = new AtomicReference<>();
        Thread thread = new Thread(() -> child.set(adapter.popByKey("stack")));
        thread.start();
        thread.join();
        assertNull(child.get());
        assertEquals("second", adapter.getCopyOfDequeByKey("stack").pop());
        assertEquals("second", adapter.popByKey("stack"));
        assertEquals("first", adapter.popByKey("stack"));
        assertNull(adapter.popByKey("stack"));
        adapter.pushByKey("stack", "cleared");
        adapter.clearDequeByKey("stack");
        assertNull(adapter.popByKey("stack"));
        adapter.clear();
    }
}
