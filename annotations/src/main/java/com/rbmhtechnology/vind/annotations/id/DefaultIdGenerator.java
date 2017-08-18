package com.rbmhtechnology.vind.annotations.id;

import com.rbmhtechnology.vind.annotations.Id;

import java.lang.reflect.Field;

/**
 * The default {@link IdGenerator}, using {@link Id#prefix()} and {@link Id#suffix()} for
 * composition/decomposition.
 */
public class DefaultIdGenerator implements IdGenerator {

    @Override
    public String compose(String fieldValue, Field idField, Class<?> clazz) {
        final Id id = idField.getAnnotation(Id.class);
        if (id == null) {
            throw new IllegalArgumentException("Missing Id-annotation on Id-annotated field? Weird...");
        } else {
            return id.prefix() + fieldValue + id.suffix();
        }
    }

    @Override
    public String decompose(String idValue, Field idField, Class<?> clazz) {
        final Id id = idField.getAnnotation(Id.class);
        if (id == null) {
            throw new IllegalArgumentException("Missing Id-annotation on Id-annotated field? Weird...");
        } else {
            return idValue.substring(id.prefix().length(), idValue.length() - id.suffix().length());
        }
    }
}
