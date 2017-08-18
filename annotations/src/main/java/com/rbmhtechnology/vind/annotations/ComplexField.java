package com.rbmhtechnology.vind.annotations;

import com.rbmhtechnology.vind.annotations.language.Language;

import java.lang.annotation.*;
import java.util.function.Function;

import static com.rbmhtechnology.vind.annotations.language.Language.*;

/**
 * Created by fonso on 27.03.17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ComplexField {

    /**
     * The name of this field in the Index. If left out, the name of the annotated Field will be used.
     */
    String name() default "";

    /**
     * <strong>EXPERT:</strong>
     * Mark this field as {@code indexed}. Indexed fields can be part of a query.
     * @return boolean flag true if the field is indexed or false when not.
     */
    boolean indexed() default true;

    Operator store () default @Operator(function = NullFunction.class);
    Operator facet () default @Operator(function = NullFunction.class);
    Operator suggestion () default @Operator(function = NullFunction.class);
    Operator fullText () default @Operator(function = NullFunction.class);
    Operator advanceFilter () default @Operator(function = NullFunction.class);
    Operator sort () default @Operator(function = NullFunction.class);

    /**
     * Enable language-specific analysis of this field in the full-text index.
     * If set empty or {@code null} a generic (language-agnostic) analysis will be used.
     * @return {@link Language} of the complex field. By default none.
     */
    Language language() default None;

    /**
     * Add a fixed index-time boost for this field.
     * Matches in <em>boosted</em> fields will be ranked higher in the result list.
     * @return float number with the boost of the field. By default 1.
     */
    float boost() default 1f;

    class NullFunction implements Function {

        @Override
        public Object apply(Object o) {
            return null;
        }
    }
}

