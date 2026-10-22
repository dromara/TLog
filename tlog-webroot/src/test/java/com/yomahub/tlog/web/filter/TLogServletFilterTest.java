package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * javax 版 Filter 测试；jakarta 版逻辑一致，由 Spring Boot 3/4 冒烟应用覆盖
 */
class TLogServletFilterTest {

    @Test
    void readsHeaderWritesResponseAndCleansUp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(TLogConstants.TLOG_TRACE_KEY, "t1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                seen.set(TLogContext.getTraceId());
            }
        });

        new TLogServletFilter().doFilter(request, response, chain);

        assertEquals("t1", seen.get());
        assertEquals("t1", response.getHeader(TLogConstants.TLOG_TRACE_KEY));
        assertNull(TLogContext.getTraceId(), "请求结束后应清理上下文");
    }

    @Test
    void cleansUpEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockFilterChain chain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                throw new IllegalStateException("boom");
            }
        });
        assertThrows(IllegalStateException.class,
                () -> new TLogServletFilter().doFilter(request, new MockHttpServletResponse(), chain));
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void responseHeaderCanBeDisabled() throws Exception {
        TLogServletFilter filter = new TLogServletFilter();
        filter.setResponseHeaderEnabled(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
        assertNull(response.getHeader(TLogConstants.TLOG_TRACE_KEY));
    }

    @Test
    void nestedEntryKeepsOuterContextUntilOuterCompletes() throws Exception {
        TLogServletFilter filter = new TLogServletFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(TLogConstants.TLOG_TRACE_KEY, "outer");
        AtomicReference<String> afterInner = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws javax.servlet.ServletException, java.io.IOException {
                // 模拟同一请求被 Filter 再次包裹（例如 forward / 重复注册）
                MockHttpServletRequest inner = new MockHttpServletRequest("GET", "/inner");
                inner.addHeader(TLogConstants.TLOG_TRACE_KEY, "inner");
                filter.doFilter(inner, new MockHttpServletResponse(), new MockFilterChain());
                afterInner.set(TLogContext.getTraceId());
            }
        });
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertEquals("outer", afterInner.get(), "内层不应覆盖或清理外层上下文");
        assertNull(TLogContext.getTraceId());
    }
}
