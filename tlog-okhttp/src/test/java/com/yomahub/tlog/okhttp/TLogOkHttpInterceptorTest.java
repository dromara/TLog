package com.yomahub.tlog.okhttp;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogOkHttpInterceptorTest {

    @AfterEach
    void clean() {
        TLogContext.removeTraceId();
        TLogContext.removeSpanId();
    }

    /**
     * 拦截在发出网络请求之前，记录最终请求并直接返回响应
     */
    private Request execute() throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        Interceptor terminal = chain -> {
            captured.set(chain.request());
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body(ResponseBody.create(null, "")).build();
        };
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new TLogOkHttpInterceptor())
                .addInterceptor(terminal)
                .build();
        client.newCall(new Request.Builder().url("http://localhost/").build()).execute().close();
        return captured.get();
    }

    @Test
    void injectsHeaders() throws Exception {
        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        Request request = execute();
        assertEquals("t1", request.header(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.1", request.header(TLogConstants.TLOG_SPANID_KEY));
        assertEquals(TLogConstants.UNKNOWN, request.header(TLogConstants.PRE_IVK_APP_KEY));
    }

    @Test
    void worksWithoutSpringAndWithoutTraceId() throws Exception {
        Request request = assertDoesNotThrow(this::execute);
        assertNull(request.header(TLogConstants.TLOG_TRACE_KEY));
    }
}
