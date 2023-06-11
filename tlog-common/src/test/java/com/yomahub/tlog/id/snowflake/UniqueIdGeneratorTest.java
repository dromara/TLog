package com.yomahub.tlog.id.snowflake;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.github.javatlacati.contiperf.PerfTest;
import com.github.javatlacati.contiperf.Required;
import com.github.javatlacati.contiperf.junit.ContiPerfRule;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * 单元测试：Id生成器
 *
 * @author javalover123
 * @date 2023/6/7
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UniqueIdGeneratorTest {

    /**
     * 性能测试：预热时长，毫秒
     */
    private static final int WARM_UP_MS = 300;

    /**
     * 性能测试：时长，毫秒
     */
    private static final int DURATION_MS = 3_000 + WARM_UP_MS;

    /**
     * 性能测试：吞吐量校验值
     */
    private static final int THROUGHPUT = 100_0000;

    /**
     * 性能测试：校验没有生成重复id 次数
     */
    private static final int INVOCATIONS = 100_0000;

    private static final Set<Long> ids = new ConcurrentHashSet<>((int) (INVOCATIONS / 0.7));

    @Rule
    public ContiPerfRule contiPerfRule = new ContiPerfRule();

    @AfterClass
    public static void tearDown() {
        Assert.assertEquals("generateId duplicated", INVOCATIONS, ids.size());
        ids.clear();
    }

    /**
     * 单机多线程并发，校验没有生成重复id
     */
    @Test
    @PerfTest(invocations = INVOCATIONS, threads = 4)
    public void generateId() {
        ids.add(UniqueIdGenerator.generateId());
    }

    /**
     * 性能测试：1线程
     */
    @Test
    @Required(throughput = THROUGHPUT)
    @PerfTest(duration = DURATION_MS, threads = 1, warmUp = WARM_UP_MS)
    public void generateId01Threads() {
        generateIdThreads();
    }

    @Test
    @Required(throughput = THROUGHPUT)
    @PerfTest(duration = DURATION_MS, threads = 2, warmUp = WARM_UP_MS)
    public void generateId02Threads() {
        generateIdThreads();
    }


    /**
     * 性能测试：4线程
     */
    @Test
    @Required(throughput = THROUGHPUT)
    @PerfTest(duration = DURATION_MS, threads = 4, warmUp = WARM_UP_MS)
    public void generateId04Threads() {
        generateIdThreads();
    }

    @Test
    @Required(throughput = THROUGHPUT)
    @PerfTest(duration = DURATION_MS, threads = 8, warmUp = WARM_UP_MS)
    public void generateId08Threads() {
        generateIdThreads();
    }

    @Test
    @Required(throughput = THROUGHPUT)
    @PerfTest(duration = DURATION_MS, threads = 16, warmUp = WARM_UP_MS)
    public void generateId16Threads() {
        generateIdThreads();
    }

    private void generateIdThreads() {
        UniqueIdGenerator.generateId();
    }

}