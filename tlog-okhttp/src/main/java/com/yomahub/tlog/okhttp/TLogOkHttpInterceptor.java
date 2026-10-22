package com.yomahub.tlog.okhttp;

import com.yomahub.tlog.context.TLogPropagator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * OkHttp的拦截器
 *
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public class TLogOkHttpInterceptor implements Interceptor {

    @Override
    public Response intercept(final Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        TLogPropagator.inject(builder::header);
        return chain.proceed(builder.build());
    }
}
