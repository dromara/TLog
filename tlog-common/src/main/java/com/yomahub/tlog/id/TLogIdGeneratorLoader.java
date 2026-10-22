package com.yomahub.tlog.id;

/**
 * traceId 生成器的持有者
 *
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public class TLogIdGeneratorLoader {

    private static volatile TLogIdGenerator idGenerator = new TLogDefaultIdGenerator();

    public static TLogIdGenerator getIdGenerator() {
        return idGenerator;
    }

    public static void setIdGenerator(TLogIdGenerator idGenerator) {
        TLogIdGeneratorLoader.idGenerator = idGenerator == null ? new TLogDefaultIdGenerator() : idGenerator;
    }
}
