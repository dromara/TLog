package com.yomahub.tlog.core.annotation;


import com.yomahub.tlog.core.convert.AspectLogConvert;

import java.lang.annotation.*;

/**
 * TLog的自定义注解的切面类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface TLogAspect {

    String[] value() default {};

    String str() default "";

    String joint() default ",";

    String pattern() default "[{}]";

    Class<? extends AspectLogConvert> convert() default AspectLogConvert.class;

}
