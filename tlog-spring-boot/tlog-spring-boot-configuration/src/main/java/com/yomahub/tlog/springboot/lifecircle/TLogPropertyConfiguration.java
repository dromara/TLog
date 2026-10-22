package com.yomahub.tlog.springboot.lifecircle;

import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.spring.TLogPropertyInit;
import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.springboot.TLogCommonAutoConfiguration;
import com.yomahub.tlog.springboot.property.TLogProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TLog的参数自动装配类
 * <p>
 * 默认值由 {@link TLogProperty} 字段初始值提供，2.0 起不再通过 {@code @PropertySource} 注入默认配置文件
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TLogProperty.class)
@AutoConfigureAfter(TLogCommonAutoConfiguration.class)
public class TLogPropertyConfiguration {

    @Bean
    @ConditionalOnMissingBean(TLogPropertyInit.class)
    public TLogPropertyInit tLogPropertyInit(TLogProperty tLogProperty, ObjectProvider<TLogIdGenerator> idGenerators,
                                             TLogSpringAware springAware) {
        TLogRPCHandler.setW3cTraceContextEnabled(tLogProperty.isW3cTraceContext());
        TLogPropertyInit tLogPropertyInit = new TLogPropertyInit();
        tLogPropertyInit.setPattern(tLogProperty.getPattern());
        tLogPropertyInit.setEnableInvokeTimePrint(tLogProperty.enableInvokeTimePrint());
        tLogPropertyInit.setIdGenerator(tLogProperty.getIdGenerator());
        tLogPropertyInit.setMdcEnable(tLogProperty.getMdcEnable());
        // 容器中声明了唯一的 TLogIdGenerator Bean 时优先使用
        tLogPropertyInit.setIdGeneratorInstance(idGenerators.getIfUnique());
        return tLogPropertyInit;
    }
}
