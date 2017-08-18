package com.rbmhtechnology.vind.annotations;

import java.lang.annotation.*;

/**
 * Use this field for faceting.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Facet {

    /**
     * Put this field into the type-ahead suggestions.
     * @return boolean flag, true when added to suggestion false when not. Default true.
     */
    boolean suggestion() default true;

}
