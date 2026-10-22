package com.yomahub.tlog.context;

import com.yomahub.tlog.constant.TLogConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.alibaba.ttl.threadpool.TtlExecutors;

import static org.junit.jupiter.api.Assertions.*;

class TLogContextTest {

    @AfterEach
    void clean() {
        TLogContext.removeTraceId();
        TLogContext.removeSpanId();
        TLogLabelGenerator.setLabelPattern(null);
    }

    @Test
    void labelGeneratorToleratesNullValues() {
        TLogLabelGenerator.setLabelPattern("<$preApp><$preHost><$preIp><$currIp><$traceId><$spanId>");
        String label = TLogLabelGenerator.generateTLogLabel(null, null, null, null, "t1", null);
        assertEquals("<NONE><NONE><NONE><NONE><t1><NONE>", label);
    }

    @Test
    void blankPatternFallsBackToDefault() {
        TLogLabelGenerator.setLabelPattern("  ");
        assertEquals(TLogLabelGenerator.DEFAULT_LABEL_PATTERN, TLogLabelGenerator.getLabelPattern());
    }

    @Test
    void nextSpanIdWorksWithoutEntryInitialization() {
        TLogContext.putTraceId("t1");
        assertEquals("0.1", SpanIdGenerator.generateNextSpanId());
        assertEquals("0.2", SpanIdGenerator.generateNextSpanId());
    }

    @Test
    void childSpanIdsAreSequential() {
        TLogContext.putSpanId("0.3");
        assertEquals("0.3.1", SpanIdGenerator.generateNextSpanId());
        assertEquals("0.3.2", SpanIdGenerator.generateNextSpanId());
    }

    @Test
    void removeSpanIdAlsoClearsIndex() {
        TLogContext.putSpanId("0.3");
        SpanIdGenerator.generateNextSpanId();
        TLogContext.removeSpanId();
        assertNull(SpanIdGenerator.spanIndex.get());
        assertNull(TLogContext.getSpanId());
    }

    @Test
    void spanIndexIsSharedAcrossTtlThreadsSoSpanIdsDoNotCollide() throws Exception {
        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        ExecutorService pool = TtlExecutors.getTtlExecutorService(Executors.newFixedThreadPool(4));
        Set<String> spanIds = ConcurrentHashMap.newKeySet();
        Future<?>[] futures = new Future[100];
        for (int i = 0; i < futures.length; i++) {
            futures[i] = pool.submit(() -> spanIds.add(SpanIdGenerator.generateNextSpanId()));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        assertEquals(100, spanIds.size());
    }

    @Test
    void propagatorInjectsAllHeadersOnlyWhenTraceIdPresent() {
        Map<String, String> headers = new HashMap<>();
        assertFalse(TLogPropagator.inject(headers::put));
        assertTrue(headers.isEmpty());

        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        assertTrue(TLogPropagator.inject(headers::put, "app-a"));
        assertEquals("t1", headers.get(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.1", headers.get(TLogConstants.TLOG_SPANID_KEY));
        assertEquals("app-a", headers.get(TLogConstants.PRE_IVK_APP_KEY));
        assertNotNull(headers.get(TLogConstants.PRE_IVK_APP_HOST));
        assertNotNull(headers.get(TLogConstants.PRE_IP_KEY));
    }

    @Test
    void propagatorUsesUnknownAppNameOutsideSpring() {
        Map<String, String> headers = new HashMap<>();
        TLogContext.putTraceId("t1");
        TLogPropagator.inject(headers::put);
        assertEquals(TLogConstants.UNKNOWN, headers.get(TLogConstants.PRE_IVK_APP_KEY));
    }
}
