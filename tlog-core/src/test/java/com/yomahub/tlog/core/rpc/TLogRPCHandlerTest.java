package com.yomahub.tlog.core.rpc;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TLogRPCHandlerTest {

    private final TLogRPCHandler handler = new TLogRPCHandler();

    @AfterEach
    void clean() {
        handler.cleanThreadLocal();
        TLogRPCHandler.setW3cTraceContextEnabled(true);
    }

    @Test
    void readsAllTLogHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(TLogConstants.TLOG_TRACE_KEY, "t1");
        headers.put(TLogConstants.TLOG_SPANID_KEY, "0.2");
        headers.put(TLogConstants.PRE_IVK_APP_KEY, "app-a");
        headers.put(TLogConstants.PRE_IVK_APP_HOST, "host-a");
        headers.put(TLogConstants.PRE_IP_KEY, "10.0.0.1");

        handler.processProviderSide(headers::get);

        assertEquals("t1", TLogContext.getTraceId());
        assertEquals("0.2", TLogContext.getSpanId());
        assertEquals("app-a", TLogContext.getPreIvkApp());
        assertEquals("<0.2><t1>", AspectLogContext.getLogValue());
    }

    @Test
    void generatesTraceIdWhenMissing() {
        handler.processProviderSide(name -> null);
        assertNotNull(TLogContext.getTraceId());
        assertEquals("0", TLogContext.getSpanId());
        assertEquals(TLogConstants.UNKNOWN, TLogContext.getPreIvkApp());
    }

    @Test
    void reusesW3cTraceIdWhenTLogHeaderMissing() {
        Map<String, String> headers = new HashMap<>();
        headers.put(TLogConstants.W3C_TRACE_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        handler.processProviderSide(headers::get);
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", TLogContext.getTraceId());
    }

    @Test
    void tlogHeaderWinsOverW3c() {
        Map<String, String> headers = new HashMap<>();
        headers.put(TLogConstants.TLOG_TRACE_KEY, "t1");
        headers.put(TLogConstants.W3C_TRACE_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        handler.processProviderSide(headers::get);
        assertEquals("t1", TLogContext.getTraceId());
    }

    @Test
    void w3cCanBeDisabled() {
        TLogRPCHandler.setW3cTraceContextEnabled(false);
        Map<String, String> headers = new HashMap<>();
        headers.put(TLogConstants.W3C_TRACE_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        handler.processProviderSide(headers::get);
        assertNotEquals("4bf92f3577b34da6a3ce929d0e0e4736", TLogContext.getTraceId());
    }

    @Test
    void cleanRemovesEverything() {
        handler.processProviderSide(name -> null);
        handler.cleanThreadLocal();
        assertNull(TLogContext.getTraceId());
        assertNull(TLogContext.getSpanId());
        assertNull(AspectLogContext.getLogValue());
    }

    @Test
    void w3cParserRejectsInvalidValues() {
        assertNull(W3cTraceContext.parseTraceId(null));
        assertNull(W3cTraceContext.parseTraceId("garbage"));
        assertNull(W3cTraceContext.parseTraceId("00-00000000000000000000000000000000-00f067aa0ba902b7-01"));
        assertNull(W3cTraceContext.parseTraceId("ff-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"));
        assertNull(W3cTraceContext.parseTraceId("00-4BF92F3577B34DA6A3CE929D0E0E4736-00f067aa0ba902b7-01"));
        // 未来版本允许在末尾追加字段
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736",
                W3cTraceContext.parseTraceId("01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01-extra"));
    }
}
