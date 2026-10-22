package com.yomahub.tlog.id.snowflake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class UniqueIdGeneratorTest {

    @AfterEach
    void restoreClock() {
        UniqueIdGenerator.setClock(AbstractClock.systemClock());
        UniqueIdGenerator.setIdPrefix(null);
    }

    @Test
    void idsAreUniqueAndIncreasingInSingleThread() {
        long last = -1;
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            long id = UniqueIdGenerator.generateId();
            assertTrue(id > last, "ID 必须单调递增");
            assertTrue(ids.add(id));
            last = id;
        }
    }

    @Test
    void idsAreUniqueUnderConcurrency() throws Exception {
        int threads = 8;
        int perThread = 20_000;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                for (int i = 0; i < perThread; i++) {
                    ids.add(UniqueIdGenerator.generateId());
                }
                latch.countDown();
            });
        }
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(threads * perThread, ids.size());
    }

    @Test
    void smallClockBackwardsWaitsAndStaysMonotonic() {
        AtomicLong now = new AtomicLong(System.currentTimeMillis() + 1000);
        UniqueIdGenerator.setClock(new AbstractClock() {
            @Override
            public long millis() {
                // 每次读取前进 1ms，模拟回拨后时钟逐渐追平
                return now.getAndIncrement();
            }
        });
        long before = UniqueIdGenerator.generateId();
        now.addAndGet(-5);
        long after = UniqueIdGenerator.generateId();
        assertTrue(after > before);
    }

    @Test
    void largeClockBackwardsDoesNotThrowAndStaysUnique() {
        long base = System.currentTimeMillis() + 10_000;
        AtomicLong now = new AtomicLong(base);
        UniqueIdGenerator.setClock(new AbstractClock() {
            @Override
            public long millis() {
                return now.get();
            }
        });
        long before = UniqueIdGenerator.generateId();
        now.set(base - 5_000);
        Set<Long> ids = new HashSet<>();
        long last = before;
        // 超过一个毫秒内序列容量（4096），验证序列耗尽时逻辑时钟前进且不阻塞
        for (int i = 0; i < 10_000; i++) {
            long id = assertDoesNotThrow(UniqueIdGenerator::generateId);
            assertTrue(id > last);
            assertTrue(ids.add(id));
            last = id;
        }
    }

    @Test
    void prefixIsAppliedOnlyWhenConfigured() {
        assertTrue(UniqueIdGenerator.generateStringId().matches("\\d+"));
        UniqueIdGenerator.setIdPrefix("ab");
        assertTrue(UniqueIdGenerator.generateStringId().startsWith("ab"));
    }

    @Test
    void invalidWorkerIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> UniqueIdGenerator.setWorkerId(-1L));
        assertThrows(IllegalArgumentException.class, () -> UniqueIdGenerator.setWorkerId(1024L));
        assertThrows(IllegalArgumentException.class, () -> UniqueIdGenerator.setWorkerId(null));
    }
}
