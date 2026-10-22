package com.yomahub.tlog.httpclient;

import com.yomahub.tlog.context.TLogPropagator;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * HttpClient5的拦截器
 *
 * @author Bryan.Zhang
 * @since 1.3.3
 */
public class TLogHttpClient5Interceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, EntityDetails entityDetails, HttpContext context) {
        TLogPropagator.inject(request::setHeader);
    }
}
