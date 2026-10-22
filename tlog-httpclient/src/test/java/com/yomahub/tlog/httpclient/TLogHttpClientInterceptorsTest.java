package com.yomahub.tlog.httpclient;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TLogHttpClientInterceptorsTest {
    @AfterEach
    void clear() {
        TLogContext.removeTraceId();
        TLogContext.removeSpanId();
        TLogRuntime.setEnabled(true);
    }

    @Test
    void httpClient4OverwritesTraceAndAllocatesDistinctSpansOnRetry() {
        org.apache.http.message.BasicHttpRequest request = new org.apache.http.message.BasicHttpRequest("GET", "/");
        request.setHeader("tlogTraceId", "stale");
        TLogContext.putTraceId("client4");
        TLogContext.putSpanId("0");
        TLogHttpClientInterceptor interceptor = new TLogHttpClientInterceptor();
        interceptor.process(request, null);
        interceptor.process(request, null);
        assertEquals("client4", request.getFirstHeader("tlogTraceId").getValue());
        assertEquals("0.2", request.getFirstHeader("tlogSpanId").getValue());
        assertEquals(1, request.getHeaders("tlogTraceId").length);
    }

    @Test
    void httpClient5OverwritesTraceAndAllocatesDistinctSpansOnRetry() {
        org.apache.hc.core5.http.message.BasicHttpRequest request = new org.apache.hc.core5.http.message.BasicHttpRequest("GET", "/");
        request.setHeader("tlogTraceId", "stale");
        TLogContext.putTraceId("client5");
        TLogContext.putSpanId("0");
        TLogHttpClient5Interceptor interceptor = new TLogHttpClient5Interceptor();
        interceptor.process(request, null, null);
        interceptor.process(request, null, null);
        assertEquals("client5", request.getFirstHeader("tlogTraceId").getValue());
        assertEquals("0.2", request.getFirstHeader("tlogSpanId").getValue());
        assertEquals(1, request.getHeaders("tlogTraceId").length);
    }

    @Test
    void bothClientsAreNoOpsWithoutContextOrWhenDisabled() {
        org.apache.http.message.BasicHttpRequest four = new org.apache.http.message.BasicHttpRequest("GET", "/");
        org.apache.hc.core5.http.message.BasicHttpRequest five = new org.apache.hc.core5.http.message.BasicHttpRequest("GET", "/");
        new TLogHttpClientInterceptor().process(four, null);
        new TLogHttpClient5Interceptor().process(five, null, null);
        assertNull(four.getFirstHeader("tlogTraceId"));
        assertNull(five.getFirstHeader("tlogTraceId"));
        TLogContext.putTraceId("disabled");
        TLogRuntime.setEnabled(false);
        new TLogHttpClientInterceptor().process(four, null);
        new TLogHttpClient5Interceptor().process(five, null, null);
        assertNull(four.getFirstHeader("tlogTraceId"));
        assertNull(five.getFirstHeader("tlogTraceId"));
    }
}
