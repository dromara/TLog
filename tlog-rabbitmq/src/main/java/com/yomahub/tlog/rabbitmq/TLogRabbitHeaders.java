package com.yomahub.tlog.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.yomahub.tlog.context.TLogPropagator;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息 header 与 TLog 上下文之间的读写
 * <p>
 * header 名与 HTTP 透传一致（tlogTraceId、tlogSpanId、preIvkApp、preIvkHost、preIp）。
 * 原生客户端收到的字符串 header 是 {@code LongString}，Spring AMQP 会转成 String，这里两种都能读
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogRabbitHeaders {

    private TLogRabbitHeaders() {
    }

    /**
     * 把当前 TLog 上下文写入 header，已存在的同名 header 会被替换
     *
     * @return 当前没有 traceId 时不写入并返回 false
     */
    public static boolean inject(Map<String, Object> headers) {
        return TLogPropagator.inject(headers::put);
    }

    /**
     * 原生客户端发送前调用：返回带 TLog header 的新 BasicProperties，原对象不变
     * <pre>
     * channel.basicPublish(exchange, routingKey, TLogRabbitHeaders.inject(props), body);
     * </pre>
     *
     * @param props 可以为 null
     * @return 当前没有 traceId 时原样返回 props
     */
    public static AMQP.BasicProperties inject(AMQP.BasicProperties props) {
        AMQP.BasicProperties source = props == null ? new AMQP.BasicProperties() : props;
        Map<String, Object> headers = source.getHeaders() == null
                ? new HashMap<>() : new HashMap<>(source.getHeaders());
        if (!inject(headers)) {
            return props;
        }
        return source.builder().headers(headers).build();
    }

    /**
     * 读取 header，不存在时返回 null
     */
    public static String read(Map<String, ?> headers, String name) {
        if (headers == null) {
            return null;
        }
        Object value = headers.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return new String((byte[]) value, StandardCharsets.UTF_8);
        }
        // LongString#toString 按 UTF-8 解码
        return value.toString();
    }
}
