package com.jerome.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author liusj
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LsjRequestMapping {
    String value() default "";
}
