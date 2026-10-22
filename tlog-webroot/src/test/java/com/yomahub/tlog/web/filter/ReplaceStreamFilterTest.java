package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.web.wrapper.RequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceStreamFilterTest {

    private ReplaceStreamFilter filter() throws Exception {
        ReplaceStreamFilter filter = new ReplaceStreamFilter();
        filter.init(new MockFilterConfig());
        return filter;
    }

    @Test
    void excludedPathStillReachesChain() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter().doFilter(new MockHttpServletRequest("GET", "/app.js"), new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest(), "排除路径必须放行到后续链路");
        assertFalse(chain.getRequest() instanceof RequestWrapper);
    }

    @Test
    void bodyIsRepeatableAndPreservesNewlinesAndEncoding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api");
        request.setCharacterEncoding("UTF-8");
        request.setContent("第一行\n第二行".getBytes(StandardCharsets.UTF_8));
        MockFilterChain chain = new MockFilterChain();

        filter().doFilter(request, new MockHttpServletResponse(), chain);

        RequestWrapper wrapper = (RequestWrapper) chain.getRequest();
        assertEquals("第一行\n第二行", wrapper.getBodyString());
        String first = wrapper.getReader().lines().collect(Collectors.joining("\n"));
        String second = wrapper.getReader().lines().collect(Collectors.joining("\n"));
        assertEquals(first, second);
        assertTrue(wrapper.getInputStream().isReady());
    }
}
