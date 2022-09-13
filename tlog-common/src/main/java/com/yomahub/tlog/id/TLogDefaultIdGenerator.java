package com.yomahub.tlog.id;

import cn.hutool.core.util.IdUtil;

public class TLogDefaultIdGenerator extends TLogIdGenerator{
    @Override
    public String generateTraceId() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
