package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.web.interceptor.TLogWebInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.DispatcherType;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogServletLifecycleTest {
    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
        TLogContext.setEnableInvokeTimePrint(false);
    }

    @Test
    void disabledTimingDoesNotParseRequestParameters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public Map<String, String[]> getParameterMap() {
                throw new AssertionError("Tracing must not trigger form parsing when timing is disabled");
            }
        };
        new TLogServletFilter().doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertNotNull(TLogContext.getTraceId()));
        assertNull(TLogContext.getTraceId());
        assertTrue(new TLogServletFilter().getOrder() > Integer.MIN_VALUE,
                "The character encoding filter must run before TLog parameter logging");
    }

    @Test
    void parameterParsingFailureDoesNotContaminateNextRequest() throws Exception {
        TLogContext.setEnableInvokeTimePrint(true);
        TLogServletFilter filter = new TLogServletFilter();
        MockHttpServletRequest bad = new MockHttpServletRequest() {
            @Override
            public Map<String, String[]> getParameterMap() {
                throw new IllegalStateException("Malformed form");
            }
        };
        bad.addHeader("tlogTraceId", "bad");
        assertThrows(IllegalStateException.class, () ->
                filter.doFilter(bad, new MockHttpServletResponse(), (req, res) -> fail("Must not run")));
        assertNull(TLogContext.getTraceId());
        MockHttpServletRequest next = new MockHttpServletRequest();
        next.addHeader("tlogTraceId", "next");
        filter.doFilter(next, new MockHttpServletResponse(), (req, res) ->
                assertEquals("next", TLogContext.getTraceId()));
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void headerFailureRollsBackEntryDepth() throws Exception {
        TLogServletFilter filter = new TLogServletFilter();
        MockHttpServletRequest bad = new MockHttpServletRequest() {
            @Override
            public String getHeader(String name) {
                throw new IllegalStateException("Header decoding failed");
            }
        };
        assertThrows(IllegalStateException.class, () ->
                filter.doFilter(bad, new MockHttpServletResponse(), (req, res) -> fail("Must not run")));
        MockHttpServletRequest next = new MockHttpServletRequest();
        next.addHeader("tlogTraceId", "healthy");
        filter.doFilter(next, new MockHttpServletResponse(), (req, res) ->
                assertEquals("healthy", TLogContext.getTraceId()));
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void redispatchKeepsOriginalTraceWithoutRestoringStaleWorkerContext() throws Exception {
        TLogServletFilter filter = new TLogServletFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> firstTrace = new AtomicReference<>();
        AtomicReference<String> firstSpan = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> {
            firstTrace.set(TLogContext.getTraceId());
            firstSpan.set(TLogContext.getSpanId());
        });
        assertNull(TLogContext.getTraceId());
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            for (DispatcherType type : new DispatcherType[]{DispatcherType.ASYNC, DispatcherType.ERROR}) {
                request.setDispatcherType(type);
                Future<?> result = worker.submit(() -> {
                    TLogContext.putTraceId("worker-context");
                    try {
                        filter.doFilter(request, response, (req, res) -> {
                            assertEquals(firstTrace.get(), TLogContext.getTraceId());
                            assertEquals(firstSpan.get(), TLogContext.getSpanId());
                        });
                        assertNull(TLogContext.getTraceId(), "A stale container thread trace must not be resurrected");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        new TLogRPCHandler().cleanThreadLocal();
                    }
                });
                result.get();
            }
        } finally {
            worker.shutdownNow();
        }
        assertEquals(firstTrace.get(), response.getHeader("tlogTraceId"));
    }

    @Test
    void legacyMvcInterceptorCleansAsyncThreadAndRestoresOnRedispatch() throws Exception {
        TLogWebInterceptor interceptor = new TLogWebInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(new Controller(), "handle");
        assertTrue(interceptor.preHandle(request, response, handler));
        String trace = TLogContext.getTraceId();
        interceptor.afterConcurrentHandlingStarted(request, response, handler);
        assertNull(TLogContext.getTraceId());

        request.setDispatcherType(DispatcherType.ASYNC);
        assertTrue(interceptor.preHandle(request, response, handler));
        assertEquals(trace, TLogContext.getTraceId());
        interceptor.afterCompletion(request, response, handler, null);
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void nestedErrorDispatchKeepsOuterContextAndCleansAfterFailure() throws Exception {
        TLogServletFilter filter = new TLogServletFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("tlogTraceId", "outer-request");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(javax.servlet.ServletException.class, () ->
                filter.doFilter(request, response, (req, res) -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    try {
                        filter.doFilter(request, response, (innerRequest, innerResponse) -> {
                            assertEquals("outer-request", TLogContext.getTraceId());
                            throw new javax.servlet.ServletException("error dispatch failed");
                        });
                    } finally {
                        assertEquals("outer-request", TLogContext.getTraceId());
                    }
                }));
        assertNull(TLogContext.getTraceId());
        MockHttpServletRequest next = new MockHttpServletRequest();
        next.addHeader("tlogTraceId", "next-request");
        filter.doFilter(next, new MockHttpServletResponse(), (req, res) ->
                assertEquals("next-request", TLogContext.getTraceId()));
        assertNull(TLogContext.getTraceId());
    }

    public static class Controller {
        public void handle() { }
    }
}
