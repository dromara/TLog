package com.yomahub.tlog.id.snowflake;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 自生成Id生成器.
 *
 * <p>
 * 长度为64bit,从高位到低位依次为
 * </p>
 *
 * <pre>
 * 1bit   符号位
 * 41bits 时间偏移量从2017年4月1日零点到现在的毫秒数
 * 10bits 机器IP二进制最后10位,例如机器的IP为192.168.1.108,二进制表示:11000000 10101000 00000001 01101100,截取最后10位 01 01101100,转为十进制364,设置workerId为364.
 * 12bits 同一个毫秒内的自增量
 * </pre>
 *
 * <p>
 * 2.0 起：无可用网卡时 workerId 随机生成而不是初始化失败；发生小幅时钟回拨（不超过 {@link #MAX_BACKWARD_MS} 毫秒）时等待追平，
 * 超出则沿用上次时间戳继续递增序列，保证 ID 单调且不影响业务调用。
 * </p>
 */
public class UniqueIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(UniqueIdGenerator.class);

    public static final long EPOCH;

    /**
     * Should be 12, update by javalover123. Thanks to https://gitee.com/yu120/sequence
     */
    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_BITS = 10L;

    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;

    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;

    private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;

    private static final long WORKER_ID_MAX_VALUE = 1L << WORKER_ID_BITS;

    /**
     * 可容忍的时钟回拨毫秒数，在此范围内自旋等待时钟追平
     */
    private static final long MAX_BACKWARD_MS = 10L;

    private static AbstractClock clock = AbstractClock.systemClock();

    /**
     * ID前缀，当 workerId 相同时(容器、多数据中心 等场景，IP相同)可降低重复概率；默认为空，不改变 ID 格式
     */
    private static volatile String idPrefix = "";

    private static long workerId;

    private static long sequence;

    private static long lastTime;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.APRIL, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH = calendar.getTimeInMillis();
        initWorkerId();
    }

    static void initWorkerId() {
        InetAddress address = getLocalAddress();
        if (address == null) {
            long randomWorkerId = ThreadLocalRandom.current().nextLong(WORKER_ID_MAX_VALUE);
            log.warn("[TLOG]未获取到本机IP地址，workerId 随机取值为 {}", randomWorkerId);
            setWorkerId(randomWorkerId);
            return;
        }
        byte[] ipAddressByteArray = address.getAddress();
        setWorkerId((long) (((ipAddressByteArray[ipAddressByteArray.length - 2] & 0B11) << Byte.SIZE) + (ipAddressByteArray[ipAddressByteArray.length - 1] & 0xFF)));
    }

    private static InetAddress getLocalAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // 优先 IPv4，与 1.x 的 workerId 取值保持一致
                    if (address instanceof Inet4Address) {
                        return address;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error when getting host ip address: <{}>.", e.getMessage());
        }
        return null;
    }

    /**
     * ID前缀，当 workerId 相同时(容器、多数据中心 等场景，IP相同)，降低重复概率
     *
     * @param idPrefix ID前缀
     */
    public static void setIdPrefix(String idPrefix) {
        UniqueIdGenerator.idPrefix = StringUtils.defaultIfBlank(idPrefix, "");
    }

    /**
     * 设置工作进程Id.
     *
     * @param workerId 工作进程Id
     */
    public static void setWorkerId(final Long workerId) {
        if (workerId == null || workerId < 0L || workerId >= WORKER_ID_MAX_VALUE) {
            throw new IllegalArgumentException("workerId 取值范围为 [0, " + WORKER_ID_MAX_VALUE + ")，当前为 " + workerId);
        }
        UniqueIdGenerator.workerId = workerId;
    }

    /**
     * 仅供测试替换时钟
     */
    static void setClock(AbstractClock clock) {
        UniqueIdGenerator.clock = clock;
    }

    /**
     * 生成Id.
     *
     * @return 返回@{@link long}类型的Id
     */
    public static synchronized long generateId() {
        long time = clock.millis();
        if (time < lastTime) {
            time = handleClockBackwards(time);
        }
        if (lastTime == time) {
            if (0L == (sequence = (sequence + 1) & SEQUENCE_MASK)) {
                time = waitUntilNextTime(time);
            }
        } else {
            sequence = 0;
        }
        lastTime = time;
        return ((time - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS) | (workerId << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
    }

    public static String generateStringId() {
        String prefix = idPrefix;
        String id = Long.toString(generateId());
        return prefix.isEmpty() ? id : prefix + id;
    }

    /**
     * 时钟回拨处理：小幅回拨自旋等待；大幅回拨沿用上次时间戳，由序列号保证唯一和单调
     */
    private static long handleClockBackwards(long time) {
        long offset = lastTime - time;
        if (offset <= MAX_BACKWARD_MS) {
            while (time < lastTime) {
                time = clock.millis();
            }
            return time;
        }
        log.warn("[TLOG]检测到时钟回拨{}毫秒，沿用上次时间戳生成ID", offset);
        return lastTime;
    }

    private static long waitUntilNextTime(final long lastTime) {
        long time = clock.millis();
        if (time < lastTime - MAX_BACKWARD_MS) {
            // 仍处于大幅回拨期间，序列号用尽时逻辑时钟前进 1 毫秒，避免长时间阻塞
            return lastTime + 1;
        }
        while (time <= lastTime) {
            time = clock.millis();
        }
        return time;
    }
}
