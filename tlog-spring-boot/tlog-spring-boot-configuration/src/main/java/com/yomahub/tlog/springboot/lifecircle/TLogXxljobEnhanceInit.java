package com.yomahub.tlog.springboot.lifecircle;

import com.yomahub.tlog.xxljob.enhance.XxlJobEnhance;
import org.springframework.beans.factory.InitializingBean;

public class TLogXxljobEnhanceInit implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        XxlJobEnhance.enhance();
    }
}
