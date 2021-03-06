package com.jerome.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author liusj
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LsjService {
    String value() default "";
}
