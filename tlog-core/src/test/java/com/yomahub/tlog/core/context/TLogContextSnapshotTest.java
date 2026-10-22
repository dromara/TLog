package com.yomahub.tlog.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TLogContextSnapshotTest {
    private final TLogRPCHandler handler = new TLogRPCHandler();

    @AfterEach
    void clean() {
        handler.cleanThreadLocal();
        MDC.remove("business");
    }

    @Test
    void nestedScopesRestoreEveryTLogValueAndPreserveOtherContexts() {
        TransmittableThreadLocal<String> business = new TransmittableThreadLocal<>();
        try {
            handler.processProviderSide(key -> "tlogTraceId".equals(key) ? "request" : null);
            TLogContext.putPreIvkApp("upstream");
            AtomicInteger originalIndex = SpanIdGenerator.spanIndex.get();
            TLogContextSnapshot request = TLogContextSnapshot.capture();
            business.set("later-business");
            MDC.put("business", "later-mdc");
            handler.processProviderSide(key -> "tlogTraceId".equals(key) ? "outer" : null);
            AtomicInteger outerIndex = SpanIdGenerator.spanIndex.get();

            assertThrows(IllegalStateException.class, () -> {
                try (TLogContextSnapshot.Scope ignored = request.restore()) {
                    assertEquals("request", TLogContext.getTraceId());
                    assertEquals("upstream", TLogContext.getPreIvkApp());
                    assertEquals("request", MDC.get("tlogTraceId"));
                    assertEquals("<0><request>", AspectLogContext.getLogValue());
                    assertSame(originalIndex, SpanIdGenerator.spanIndex.get());
                    assertEquals("0.1", SpanIdGenerator.generateNextSpanId());
                    assertEquals("later-business", business.get());
                    assertEquals("later-mdc", MDC.get("business"));
                    throw new IllegalStateException("business failure");
                }
            });
            assertEquals("outer", TLogContext.getTraceId());
            assertEquals("outer", MDC.get("tlogTraceId"));
            assertSame(outerIndex, SpanIdGenerator.spanIndex.get());
            assertEquals("later-mdc", MDC.get("business"));
            try (TLogContextSnapshot.Scope ignored = request.restore()) {
                assertEquals("0.2", SpanIdGenerator.generateNextSpanId());
            }
        } finally {
            business.remove();
        }
    }

    @Test
    void crossThreadScopeClearsOnlyItsFieldsAndRestoresWorker() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> MDC.put("business", "worker")).get();
            handler.processProviderSide(key -> "tlogTraceId".equals(key) ? "request" : null);
            TLogContextSnapshot snapshot = TLogContextSnapshot.capture();
            pool.submit(() -> {
                try (TLogContextSnapshot.Scope ignored = snapshot.restore()) {
                    assertEquals("request", TLogContext.getTraceId());
                    assertEquals("worker", MDC.get("business"));
                    assertEquals("0.1", SpanIdGenerator.generateNextSpanId());
                }
                assertNull(TLogContext.getTraceId());
                assertNull(TLogContext.getSpanId());
                assertNull(SpanIdGenerator.spanIndex.get());
                assertNull(MDC.get("tlogTraceId"));
                assertEquals("worker", MDC.get("business"));
            }).get();
            assertEquals("0.2", SpanIdGenerator.generateNextSpanId());
        } finally {
            pool.shutdownNow();
        }
    }
}
