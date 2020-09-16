package com.yomahub.tlog.springboot;

import com.yomahub.tlog.springboot.property.TLogProperty;
import com.yomahub.tlog.springboot.property.TLogPropertyInit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TLogProperty.class)
@ConditionalOnProperty(prefix = "tlog", name = "pattern")
public class TLogPropertyConfiguration {

    @Bean
    public TLogPropertyInit tLogPropertyInit(TLogProperty tLogProperty){
        return new TLogPropertyInit(tLogProperty.getPattern());
    }
}
