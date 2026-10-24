package com.yomahub.tlog.kafka;

import com.yomahub.tlog.context.TLogPropagator;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

/**
 * Kafka 消息 header 与 TLog 上下文之间的读写
 * <p>
 * header 名与 HTTP 透传一致（tlogTraceId、tlogSpanId、preIvkApp、preIvkHost、preIp），值为 UTF-8 字节
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogKafkaHeaders {

    private TLogKafkaHeaders() {
    }

    /**
     * 把当前 TLog 上下文写入 header，已存在的同名 header 会被替换
     *
     * @return 当前没有 traceId 时不写入并返回 false
     */
    public static boolean inject(Headers headers) {
        return TLogPropagator.inject((name, value) -> {
            headers.remove(name);
            headers.add(name, value.getBytes(StandardCharsets.UTF_8));
        });
    }

    /**
     * 读取最后一个同名 header，不存在时返回 null
     */
    public static String read(Headers headers, String name) {
        if (headers == null) {
            return null;
        }
        Header header = headers.lastHeader(name);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
