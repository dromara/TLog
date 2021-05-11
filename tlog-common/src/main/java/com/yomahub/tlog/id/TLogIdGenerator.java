package com.yomahub.tlog.id;

import java.util.Map;

public abstract class TLogIdGenerator {

    protected Map<String, Object> extData;

    abstract public String generateTraceId();

    public TLogIdGenerator withExtData(Map<String, Object> extData) {
        this.extData = extData;
        return this;
    }
}
