package com.rbmhtechnology.vind.annotations;

import java.lang.annotation.*;

/**
 * Do not add this field to the Search-Index.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Ignore {}
