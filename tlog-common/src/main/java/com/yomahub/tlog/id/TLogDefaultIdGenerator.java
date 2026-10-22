package com.yomahub.tlog.id;

import com.yomahub.tlog.id.snowflake.UniqueIdGenerator;

/**
 * TLog 默认的 traceId 生成器，基于雪花算法
 *
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public class TLogDefaultIdGenerator extends TLogIdGenerator {

    @Override
    public String generateTraceId() {
        return UniqueIdGenerator.generateStringId();
    }
}
