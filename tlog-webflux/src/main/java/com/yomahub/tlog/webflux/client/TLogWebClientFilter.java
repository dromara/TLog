package com.yomahub.tlog.webflux.client;

import com.yomahub.tlog.context.TLogPropagator;
import com.yomahub.tlog.webflux.context.TLogReactorContext;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 的 TLog 过滤器
 * <p>
 * 在订阅时优先读取 Reactor Context 中的请求上下文；同步调用沿用当前线程。用法：
 * <pre>
 * WebClient.builder().filter(new TLogWebClientFilter()).build();
 * </pre>
 * Spring Boot 下容器中的 {@code WebClient.Builder} 会自动添加本过滤器。
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public class TLogWebClientFilter implements ExchangeFilterFunction {

    public TLogWebClientFilter() {
        TLogReactorContext.install();
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return TLogReactorContext.defer(snapshot -> {
            ClientRequest.Builder builder = ClientRequest.from(request);
            boolean injected = TLogPropagator.inject((name, value) -> builder.headers(headers -> headers.set(name, value)));
            return next.exchange(injected ? builder.build() : request);
        });
    }
}
