package com.yomahub.tlog.feign.filter;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class TLogFeignFilterTest {
    @AfterEach
    void clear() {
        TLogContext.removeTraceId();
        TLogContext.removeSpanId();
        TLogRuntime.setEnabled(true);
        TLogRuntime.setAppName(null);
    }

    @Test
    void retryReplacesHeadersWithoutDuplicatingOrLosingUserHeaders() {
        RequestTemplate request = new RequestTemplate();
        request.header("Authorization", "token");
        TLogContext.putTraceId("trace-retry");
        TLogContext.putSpanId("0");
        TLogRuntime.setAppName("caller");
        TLogFeignFilter filter = new TLogFeignFilter();
        filter.apply(request);
        filter.apply(request);
        assertEquals(Collections.singletonList("trace-retry"), request.headers().get("tlogTraceId"));
        assertEquals(Collections.singletonList("0.2"), request.headers().get("tlogSpanId"));
        assertEquals(Collections.singletonList("token"), request.headers().get("Authorization"));
    }

    @Test
    void disabledRuntimeDoesNotInjectHeaders() {
        RequestTemplate request = new RequestTemplate();
        TLogContext.putTraceId("disabled");
        TLogRuntime.setEnabled(false);
        new TLogFeignFilter().apply(request);
        assertTrue(request.headers().isEmpty());
    }
}
