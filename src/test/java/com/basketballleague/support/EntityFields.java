package com.basketballleague.support;

import java.lang.reflect.Field;

/**
 * Sets a field that only JPA or the database writes (id, version, loaded state) in unit tests.
 */
public final class EntityFields {

    private EntityFields() {
    }

    public static <T> T withField(T entity, String fieldName, Object value) {
        try {
            final Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot set " + fieldName + " on " + entity.getClass().getSimpleName(), e);
        }
    }
}
