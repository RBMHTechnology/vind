package com.rbmhtechnology.vind.annotations.id;

import java.lang.reflect.Field;

/**
 * A generator function to compose/decompose an ID-value.
 * @see DefaultIdGenerator
 */
public interface IdGenerator {

    /**
     * Function to transform a POJO-ID into a Search-Index-ID.
     *
     * @param fieldValue the value in the POJO.
     * @param idField the {@code Field} annotated with {@code @Id}.
     * @param clazz the {@code Class}, the POJO.
     * @return the Id-Value in the search index.
     */
    String compose(String fieldValue, Field idField, Class<?> clazz);

    /**
     * Function to transform a Search-Index-ID into a POJO-ID.
     *
     * @param idValue the value in the search index.
     * @param idField the {@code Field} annotated with {@code @Id}.
     * @param clazz the {@code Class}, the POJO.
     * @return the value in the POJO.
     */
    String decompose(String idValue, Field idField, Class<?> clazz);

}
