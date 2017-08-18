package com.rbmhtechnology.vind.annotations;

import java.lang.annotation.*;

/**
 * Field-level configuration on how the field is put into the Search-Index.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Field {

    /**
     * The name of this field in the Index. If left out, the name of the annotated Field will be used.
     * @return {@link String} name of the field.
     */
    String name() default "";

    /**
     * <strong>EXPERT:</strong>
     * Mark this field as {@code indexed}. Indexed fields can be part of a query.
     * @return boolean flag whether the field is indexed or not. Default true.
     */
    boolean indexed() default true;

    /**
     * <strong>EXPERT:</strong>
     * Mark this field as {@code stored}. Stored fields can be retrieved in a query.
     * @return boolean flag whether the field is stored or not. Default true.
     */
    boolean stored() default true;

}
