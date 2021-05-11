package com.yomahub.tlog.id;

import java.util.Map;

public class TLogIdGeneratorLoader {

    private static TLogIdGenerator idGenerator = new TLogDefaultIdGenerator();

    public static TLogIdGenerator getIdGenerator() {
        return idGenerator;
    }

    public static void setIdGenerator(TLogIdGenerator idGenerator) {
        TLogIdGeneratorLoader.idGenerator = idGenerator;
    }
}
