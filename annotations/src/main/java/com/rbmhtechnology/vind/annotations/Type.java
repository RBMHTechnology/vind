package com.rbmhtechnology.vind.annotations;

import java.lang.annotation.*;

/**
 * the type of the indexed item.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Type {

    /**
     * The type-name to store in the index. If left blank, the {@link Class#getSimpleName()} is used.
     * @return {@link String} the type to be stored in the index.
     */
    String name() default "";

}
