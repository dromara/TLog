package com.yomahub.tlog.resttemplate;

import com.yomahub.tlog.context.TLogPropagator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate / RestClient 的拦截器
 * <p>
 * 只调用 {@link HttpHeaders#set(String, String)}，该方法描述符在 Spring 5 ~ 7 中一致
 *
 * @author Bryan.Zhang
 * @since 1.3.6
 */
public class TLogRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        TLogPropagator.inject(headers::set);
        return execution.execute(request, body);
    }
}
