package com.yomahub.tlog.id;


public class TLogIdGeneratorLoader {

    private static TLogIdGenerator idGenerator = new TLogDefaultIdGenerator();

    public static TLogIdGenerator getIdGenerator() {
        return idGenerator;
    }

    public static void setIdGenerator(TLogIdGenerator idGenerator) {
        TLogIdGeneratorLoader.idGenerator = idGenerator;
    }
}
