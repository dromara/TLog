package com.yomahub.tlog.core.thread;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogExecutorsTest {

    private final TLogRPCHandler handler = new TLogRPCHandler();

    private final ExecutorService raw = Executors.newSingleThreadExecutor();

    @AfterEach
    void clean() {
        handler.cleanThreadLocal();
        MDC.remove("business");
        raw.shutdownNow();
    }

    /**
     * 先让线程池创建好线程，避免 InheritableThreadLocal 在建线程时“碰巧”继承
     */
    private void warmUp(ExecutorService pool) throws Exception {
        pool.submit(() -> {
        }).get();
    }

    @Test
    void wrappedExecutorServicePropagatesAndRestores() throws Exception {
        ExecutorService pool = TLogExecutors.wrap(raw);
        warmUp(pool);
        handler.processProviderSide(name -> null);
        String traceId = TLogContext.getTraceId();

        assertEquals(traceId, pool.submit(TLogContext::getTraceId).get());
        handler.cleanThreadLocal();
        assertNull(pool.submit(TLogContext::getTraceId).get(), "任务结束后执行线程不应残留上下文");
    }

    @Test
    void wrappedExecutorWorksWithCompletableFuture() throws Exception {
        warmUp(raw);
        handler.processProviderSide(name -> null);
        String traceId = TLogContext.getTraceId();
        String inner = CompletableFuture.supplyAsync(TLogContext::getTraceId, TLogExecutors.wrap((java.util.concurrent.Executor) raw))
                .get(5, TimeUnit.SECONDS);
        assertEquals(traceId, inner);
    }

    @Test
    void unwrappedExecutorDoesNotPropagate() throws Exception {
        warmUp(raw);
        handler.processProviderSide(name -> null);
        assertNull(raw.submit(TLogContext::getTraceId).get());
    }

    @Test
    void inheritableTaskWorksWithoutReflectionOnJdkInternals() throws Exception {
        warmUp(raw);
        handler.processProviderSide(name -> null);
        String traceId = TLogContext.getTraceId();
        AtomicReference<String> seen = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        raw.execute(new TLogInheritableTask() {
            @Override
            public void runTask() {
                seen.set(TLogContext.getTraceId());
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(traceId, seen.get());
        handler.cleanThreadLocal();
        assertNull(raw.submit(TLogContext::getTraceId).get());
    }

    @Test
    void allExecutorServiceEntrypointsPropagateMdcAndRestoreWorkerAfterFailure() throws Exception {
        raw.submit(() -> MDC.put("business", "worker")).get();
        handler.processProviderSide(name -> "tlogTraceId".equals(name) ? "request" : null);
        MDC.put("business", "caller");
        ExecutorService pool = TLogExecutors.wrap(raw);
        Callable<String> state = () -> TLogContext.getTraceId() + ":" + MDC.get("tlogTraceId") + ":" + MDC.get("business");
        String expected = "request:request:caller";
        assertEquals(expected, pool.submit(state).get());
        assertEquals(expected, pool.invokeAll(Arrays.asList(state, state)).get(0).get());
        assertEquals(expected, pool.invokeAny(Arrays.asList(state, state)));
        assertEquals(expected, pool.invokeAll(Arrays.asList(state), 5, TimeUnit.SECONDS).get(0).get());
        assertEquals(expected, pool.invokeAny(Arrays.asList(state), 5, TimeUnit.SECONDS));
        AtomicReference<String> seen = new AtomicReference<>();
        assertEquals("done", pool.submit(() -> seen.set(MDC.get("business")), "done").get());
        assertEquals("caller", seen.get());
        assertThrows(java.util.concurrent.ExecutionException.class, () -> pool.submit(() -> {
            MDC.put("business", "changed");
            throw new IllegalStateException("failure");
        }).get());
        assertEquals("worker", raw.submit(() -> MDC.get("business")).get());
        assertNull(raw.submit(TLogContext::getTraceId).get());
        assertNull(raw.submit(() -> MDC.get("tlogTraceId")).get());
        assertSame(pool, TLogExecutors.wrap(pool));
    }

    @Test
    void scheduledAndRepeatedTasksKeepSubmissionMdcWithoutLeakingIt() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            scheduler.submit(() -> MDC.put("business", "worker")).get();
            ScheduledExecutorService wrapped = TLogExecutors.wrap(scheduler);
            MDC.put("business", "caller");
            assertEquals("caller", wrapped.schedule(() -> MDC.get("business"), 0, TimeUnit.MILLISECONDS).get());
            for (boolean fixedRate : new boolean[]{true, false}) {
                CountDownLatch twice = new CountDownLatch(2);
                AtomicReference<String> failure = new AtomicReference<>();
                Runnable task = () -> {
                    if (!"caller".equals(MDC.get("business"))) {
                        failure.set(String.valueOf(MDC.get("business")));
                    }
                    MDC.put("business", "task-change");
                    twice.countDown();
                };
                ScheduledFuture<?> future = fixedRate
                        ? wrapped.scheduleAtFixedRate(task, 0, 5, TimeUnit.MILLISECONDS)
                        : wrapped.scheduleWithFixedDelay(task, 0, 5, TimeUnit.MILLISECONDS);
                assertTrue(twice.await(5, TimeUnit.SECONDS));
                future.cancel(false);
                assertNull(failure.get());
                assertEquals("worker", scheduler.submit(() -> MDC.get("business")).get());
            }
            assertSame(wrapped, TLogExecutors.wrap(wrapped));
        } finally {
            scheduler.shutdownNow();
        }
    }
}
