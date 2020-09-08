package com.yomahub.tlog.core.annotation;


import com.yomahub.tlog.core.convert.AspectLogConvert;

import java.lang.annotation.*;

/**
 * @author Bryan.Zhang
 * 切面日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AspectLog {

    String[] value() default {};

    String joint() default "-";

    String pattern() default "[{}]";

    Class<? extends AspectLogConvert> convert() default AspectLogConvert.class;

}
