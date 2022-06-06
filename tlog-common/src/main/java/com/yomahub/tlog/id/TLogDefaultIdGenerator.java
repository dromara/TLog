package com.yomahub.tlog.id;

import cn.hutool.core.util.IdUtil;
import com.yomahub.tlog.utils.UniqueIdUtil;

public class TLogDefaultIdGenerator extends TLogIdGenerator{
    @Override
    public String generateTraceId() {
        return UniqueIdUtil.generateId().toString();
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
