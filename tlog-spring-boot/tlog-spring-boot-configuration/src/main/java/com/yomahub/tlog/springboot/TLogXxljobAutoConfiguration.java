package com.yomahub.tlog.springboot;

import com.yomahub.tlog.springboot.lifecircle.TLogXxljobEnhanceInit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * xxljob的自动装配类
 * @author Bryan.Zhang
 * @since 1.3.0
 */

@Configuration
@ConditionalOnClass(name = {"com.xxl.job.core.server.EmbedServer"})
public class TLogXxljobAutoConfiguration {

    public TLogXxljobEnhanceInit tLogXxljobEnhanceInit(){
        return new TLogXxljobEnhanceInit();
    }
}
