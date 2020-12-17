package com.yomahub.tlog.id;

import com.yomahub.tlog.id.snowflake.UniqueIdGenerator;

public class TLogDefaultIdGenerator implements TLogIdGenerator{
    @Override
    public String generateTraceId() {
        return UniqueIdGenerator.generateStringId();
    }
}
