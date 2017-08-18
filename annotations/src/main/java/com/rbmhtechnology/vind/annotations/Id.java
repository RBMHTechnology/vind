package com.rbmhtechnology.vind.annotations;

import com.rbmhtechnology.vind.annotations.id.DefaultIdGenerator;
import com.rbmhtechnology.vind.annotations.id.IdGenerator;

import java.lang.annotation.*;

/**
 * The <strong>unique</strong> id of an item in the Search-Index.
 * Values will be converted to a {@code String}-representation and optionally extended with prefix/suffix.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Id {

    /**
     * A prefix that will be prepended to an ID value.
     * @return {@link String} prefix to be prepended to the ID. By default empty.
     */
    String prefix() default "";

    /**
     * A suffix that will be appended to an ID value
     * @return {@link String} suffix to be appended to the ID. By default empty.
     */
    String suffix() default "";

    /**
     * A generator to compose/decompose the ID value.
     * @return {@link IdGenerator} class to automatically create the ids.
     */
    Class<? extends IdGenerator> generator() default DefaultIdGenerator.class;

}
