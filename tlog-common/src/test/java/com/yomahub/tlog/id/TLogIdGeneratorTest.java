package com.yomahub.tlog.id;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TLogIdGeneratorTest {
    @Test
    void legacyFieldIsIsolatedDuringConcurrentGenerateCallsAndReleasedAfterwards() throws Exception {
        CountDownLatch firstInside = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        TLogIdGenerator generator = new TLogIdGenerator() {
            @Override
            public String generateTraceId() {
                if ("A".equals(getExtData().get("id"))) {
                    firstInside.countDown();
                    try {
                        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                        // Give the competing caller a chance to overwrite the legacy field.
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        throw new AssertionError(e);
                    }
                }
                return (String) extData.get("id");
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> generator.generate(Collections.singletonMap("id", "A")));
            assertTrue(firstInside.await(5, TimeUnit.SECONDS));
            Future<String> b = pool.submit(() -> {
                secondStarted.countDown();
                return generator.generate(Collections.singletonMap("id", "B"));
            });
            assertEquals("A", a.get(5, TimeUnit.SECONDS));
            assertEquals("B", b.get(5, TimeUnit.SECONDS));
            assertNull(generator.extData);
            assertNull(generator.getExtData());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void nestedGenerationAndFailureRestoreOuterData() {
        TLogIdGenerator generator = new TLogIdGenerator() {
            @Override
            public String generateTraceId() {
                Map<String, Object> original = getExtData();
                if ("outer".equals(original.get("id"))) {
                    assertThrows(IllegalStateException.class,
                            () -> generate(Collections.singletonMap("id", "inner")));
                    assertSame(original, extData);
                    assertSame(original, getExtData());
                    return "outer";
                }
                throw new IllegalStateException("inner");
            }
        };
        assertEquals("outer", generator.generate(Collections.singletonMap("id", "outer")));
        assertNull(generator.extData);
        assertNull(generator.getExtData());
    }
}
