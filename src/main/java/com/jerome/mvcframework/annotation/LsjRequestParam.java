package com.jerome.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author liusj
 */
@Target(value = ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LsjRequestParam {
    String value() default "";
}
