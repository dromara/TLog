package com.yomahub.tlog.dubbox.filter;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TLogDubboxFilterTest {

    @AfterEach
    void clean() {
        TLogRuntime.setEnabled(true);
        new TLogRPCHandler().cleanThreadLocal();
        RpcContext.removeContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void disabledSpiFilterDoesNotCreateOrPropagateContext() {
        TLogRuntime.setEnabled(false);
        TLogDubboxFilter filter = new TLogDubboxFilter();
        Invoker<Object> provider = mock(Invoker.class);
        when(provider.getUrl()).thenReturn(URL.valueOf("dubbo://localhost:20880/demo?side=provider"));
        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(TLogConstants.TLOG_TRACE_KEY)).thenReturn("upstream");
        when(provider.invoke(invocation)).thenAnswer(i -> {
            assertNull(TLogContext.getTraceId());
            return mock(Result.class);
        });
        filter.invoke(provider, invocation);
        verify(provider).invoke(invocation);

        Invoker<Object> consumer = mock(Invoker.class);
        when(consumer.getUrl()).thenReturn(URL.valueOf("dubbo://localhost:20880/demo?side=consumer"));
        TLogContext.putTraceId("outgoing");
        filter.invoke(consumer, invocation);
        assertNull(RpcContext.getContext().getAttachment(TLogConstants.TLOG_TRACE_KEY));
        verify(consumer).invoke(invocation);
    }
}
