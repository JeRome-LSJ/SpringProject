package com.jerome.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author liusj
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LsjAutowire {
    String value() default "";
}
