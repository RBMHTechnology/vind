package com.rbmhtechnology.vind.annotations;

import com.rbmhtechnology.vind.annotations.language.Language;

import java.lang.annotation.*;

/**
 * Enable full-text search for this field. In most cases you will want to do this.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface FullText {

    /**
     * Enable language-specific analysis of this field in the full-text index.
     * If set empty or {@code null} a generic (language-agnostic) analysis will be used.
     * @return {@link Language} of the complex field. By default none.
     */
    Language language() default Language.None;

    /**
     * Add a fixed index-time boost for this field.
     * Matches in <em>boosted</em> fields will be ranked higher in the result list.
     * @return float number with the boost of the field. By default 1.
     */
    float boost() default 1f;
}
