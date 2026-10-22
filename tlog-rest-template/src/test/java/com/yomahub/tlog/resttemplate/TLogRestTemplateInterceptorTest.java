package com.yomahub.tlog.resttemplate;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class TLogRestTemplateInterceptorTest {

    @AfterEach
    void clean() {
        TLogContext.removeTraceId();
        TLogContext.removeSpanId();
    }

    private final ClientHttpRequestExecution execution =
            (request, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK);

    @Test
    void injectsHeadersWhenTraceIdPresent() throws Exception {
        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, java.net.URI.create("http://x"));

        new TLogRestTemplateInterceptor().intercept(request, new byte[0], execution);

        assertEquals("t1", request.getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.1", request.getHeaders().getFirst(TLogConstants.TLOG_SPANID_KEY));
    }

    @Test
    void repeatedInterceptDoesNotDuplicateHeaders() throws Exception {
        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, java.net.URI.create("http://x"));
        TLogRestTemplateInterceptor interceptor = new TLogRestTemplateInterceptor();

        interceptor.intercept(request, new byte[0], execution);
        interceptor.intercept(request, new byte[0], execution);

        assertEquals(1, request.getHeaders().get(TLogConstants.TLOG_TRACE_KEY).size());
    }

    @Test
    void noTraceIdNoHeaders() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, java.net.URI.create("http://x"));
        new TLogRestTemplateInterceptor().intercept(request, new byte[0], execution);
        assertNull(request.getHeaders().getFirst(TLogConstants.TLOG_TRACE_KEY));
    }
}
