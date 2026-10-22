package com.yomahub.tlog.dubbo.filter;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TLogDubboFilterTest {

    private final TLogDubboFilter filter = new TLogDubboFilter();

    @AfterEach
    void clean() {
        TLogRuntime.setEnabled(true);
        new TLogRPCHandler().cleanThreadLocal();
        RpcContext.removeContext();
    }

    @SuppressWarnings("unchecked")
    private Invoker<Object> invoker(String side) {
        Invoker<Object> invoker = mock(Invoker.class);
        URL url = URL.valueOf("dubbo://127.0.0.1:20880/demo?" + CommonConstants.APPLICATION_KEY + "=app-a"
                + (side == null ? "" : "&" + CommonConstants.SIDE_KEY + "=" + side));
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    @Test
    void providerSideBuildsContextAndCleansUp() {
        Invoker<Object> invoker = invoker(CommonConstants.PROVIDER_SIDE);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(TLogConstants.TLOG_TRACE_KEY)).thenReturn("t1");
        when(invocation.getAttachment(TLogConstants.TLOG_SPANID_KEY)).thenReturn("0.1");
        AtomicReference<String> seen = new AtomicReference<>();
        when(invoker.invoke(any())).thenAnswer(i -> {
            seen.set(TLogContext.getTraceId() + "/" + TLogContext.getSpanId());
            return mock(Result.class);
        });

        filter.invoke(invoker, invocation);

        assertEquals("t1/0.1", seen.get());
        assertNull(TLogContext.getTraceId(), "调用结束后应清理上下文");
    }

    @Test
    void consumerSideWritesAttachments() {
        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        Invoker<Object> invoker = invoker(CommonConstants.CONSUMER_SIDE);

        filter.invoke(invoker, mock(Invocation.class));

        RpcContext context = RpcContext.getContext();
        assertEquals("t1", context.getAttachment(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.1", context.getAttachment(TLogConstants.TLOG_SPANID_KEY));
        assertEquals("app-a", context.getAttachment(TLogConstants.PRE_IVK_APP_KEY));
    }

    @Test
    void missingSideDoesNotThrow() {
        Invoker<Object> invoker = invoker(null);
        Invocation invocation = mock(Invocation.class);
        assertDoesNotThrow(() -> filter.invoke(invoker, invocation));
        verify(invoker).invoke(invocation);
    }

    @Test
    void globalSwitchDisablesSpiProviderAndConsumerFilters() {
        TLogRuntime.setEnabled(false);
        Invoker<Object> provider = invoker(CommonConstants.PROVIDER_SIDE);
        Invocation incoming = mock(Invocation.class);
        when(incoming.getAttachment(TLogConstants.TLOG_TRACE_KEY)).thenReturn("upstream");
        when(provider.invoke(incoming)).thenAnswer(i -> {
            assertNull(TLogContext.getTraceId());
            return mock(Result.class);
        });
        filter.invoke(provider, incoming);
        verify(provider).invoke(incoming);

        TLogContext.putTraceId("outgoing");
        filter.invoke(invoker(CommonConstants.CONSUMER_SIDE), mock(Invocation.class));
        assertNull(RpcContext.getContext().getAttachment(TLogConstants.TLOG_TRACE_KEY));
    }
}
