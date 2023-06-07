package com.yomahub.tlog.id.snowflake;

import static org.testng.Assert.assertTrue;

import cn.hutool.core.collection.ConcurrentHashSet;
import java.util.Set;
import org.testng.annotations.Test;

/**
 * 单元测试：Id生成器
 *
 * @author javalover123
 * @date 2023/6/7
 */
public class UniqueIdGeneratorTest {

    private Set<Long> ids = new ConcurrentHashSet<>(128);

    @Test(invocationCount = 128, threadPoolSize = 3)
    public void testGenerateId() {
        final Long id = UniqueIdGenerator.generateId();
        assertTrue(ids.add(id), "id exists," + id);
    }

}