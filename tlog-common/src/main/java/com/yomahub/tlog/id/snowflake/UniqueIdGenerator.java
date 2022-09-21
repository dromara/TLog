package com.yomahub.tlog.id.snowflake;

import cn.hutool.core.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

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
 * </pre>UNIQUE
 *
 */
public class UniqueIdGenerator {

	private static final Logger log = LoggerFactory.getLogger(UniqueIdGenerator.class);

    public static final long EPOCH;

    private static final long SEQUENCE_BITS = 6L;

    private static final long WORKER_ID_BITS = 10L;

    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;

    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;

    private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;

    private static final long WORKER_ID_MAX_VALUE = 1L << WORKER_ID_BITS;

    private static AbstractClock clock = AbstractClock.systemClock();

    private static long workerId;

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

    private static long sequence;

    private static long lastTime;

    static void initWorkerId() {
        InetAddress address = getLocalAddress();
        byte[] ipAddressByteArray = address.getAddress();
        setWorkerId((long) (((ipAddressByteArray[ipAddressByteArray.length - 2] & 0B11) << Byte.SIZE) + (ipAddressByteArray[ipAddressByteArray.length - 1] & 0xFF)));
    }

    private static InetAddress getLocalAddress() {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                if (addresses.hasMoreElements()) {
                    return addresses.nextElement();
                }
            }
        } catch (Exception e) {
            log.debug("Error when getting host ip address: <{}>.", e.getMessage());
            throw new IllegalStateException("Cannot get LocalHost InetAddress, please check your network!");
        }
        return null;
    }

    /**
     * 设置工作进程Id.
     *
     * @param workerId 工作进程Id
     */
    public static void setWorkerId(final Long workerId) {
        Assert.isTrue(workerId >= 0L && workerId < WORKER_ID_MAX_VALUE);
        UniqueIdGenerator.workerId = workerId;
    }

    /**
     * 生成Id.
     *
     * @return 返回@{@link Long}类型的Id
     */
    public static Long generateId() {
        long time = clock.millis();
        Assert.isTrue(lastTime <= time, "Clock is moving backwards, last time is {} milliseconds, current time is {} milliseconds", lastTime, time);
        if (lastTime == time) {
            if (0L == (sequence = ++sequence & SEQUENCE_MASK)) {
                time = waitUntilNextTime(time);
            }
        } else {
            sequence = 0;
        }
        lastTime = time;
        if (log.isDebugEnabled()) {
            log.debug("{}-{}-{}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(lastTime)), workerId, sequence);
        }
        return ((time - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS) | (workerId << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
    }

    public static String generateStringId() {
        return generateId().toString();
    }

    private static long waitUntilNextTime(final long lastTime) {
        long time = clock.millis();
        while (time <= lastTime) {
            time = clock.millis();
        }
        return time;
    }
}
