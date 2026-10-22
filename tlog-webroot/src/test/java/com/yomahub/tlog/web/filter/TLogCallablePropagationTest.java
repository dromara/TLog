package com.yomahub.tlog.web.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.web.common.TLogWebAsyncSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class TLogCallablePropagationTest {
    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
        MDC.clear();
    }

    @Test
    void frameworkDefaultExecutorReceivesTraceAndMdc() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).addFilters(new TLogServletFilter()).build();
        MvcResult initial = mvc.perform(get("/callable")).andReturn();
        String trace = initial.getResponse().getHeader(TLogConstants.TLOG_TRACE_KEY);
        assertNotNull(trace);
        assertEquals(trace + "|" + trace, initial.getAsyncResult(5000));
        assertNull(TLogContext.getTraceId());
        assertNull(MDC.get(TLogConstants.TLOG_TRACE_KEY));
        assertEquals(trace + "|" + trace, mvc.perform(asyncDispatch(initial)).andReturn().getResponse().getContentAsString());
    }

    @Test
    void callableFailureRestoresReusedWorkerAndPreservesBusinessMdc() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> MDC.put("business", "worker-value")).get();
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controller()).addFilters(new TLogServletFilter()).build();
            mvc.getDispatcherServlet().getWebApplicationContext().getBean(RequestMappingHandlerAdapter.class)
                    .setTaskExecutor(new ConcurrentTaskExecutor(pool));
            MvcResult failed = mvc.perform(get("/failure").header(TLogConstants.TLOG_TRACE_KEY, "failed-request")).andReturn();
            assertTrue(failed.getAsyncResult(5000) instanceof IllegalStateException);
            assertNull(pool.submit(TLogContext::getTraceId).get());
            assertNull(pool.submit(() -> MDC.get(TLogConstants.TLOG_TRACE_KEY)).get());
            assertEquals("worker-value", pool.submit(() -> MDC.get("business")).get());

            MvcResult next = mvc.perform(get("/callable").header(TLogConstants.TLOG_TRACE_KEY, "next-request")).andReturn();
            assertEquals("next-request|next-request", next.getAsyncResult(5000));
            assertNull(pool.submit(TLogContext::getTraceId).get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void asyncSwitchCanDisableMvcCallableInstrumentation() throws Exception {
        TLogServletFilter filter = new TLogServletFilter();
        filter.setAsyncContextEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        assertNull(WebAsyncUtils.getAsyncManager(request).getCallableInterceptor(TLogWebAsyncSupport.class.getName()));
    }

    @RestController
    public static class Controller {
        @GetMapping("/callable")
        public Callable<String> callable() {
            return () -> TLogContext.getTraceId() + "|" + MDC.get(TLogConstants.TLOG_TRACE_KEY);
        }

        @GetMapping("/failure")
        public Callable<String> failure() {
            return () -> {
                assertEquals("failed-request", TLogContext.getTraceId());
                assertEquals("failed-request", MDC.get(TLogConstants.TLOG_TRACE_KEY));
                throw new IllegalStateException("callable failed");
            };
        }
    }
}
