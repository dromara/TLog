package com.yomahub.tlog.httpclient;

import com.yomahub.tlog.context.TLogPropagator;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * HttpClient的拦截器
 *
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public class TLogHttpClientInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(final HttpRequest request, final HttpContext context) {
        TLogPropagator.inject(request::setHeader);
    }
}
