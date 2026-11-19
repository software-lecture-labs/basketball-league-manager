package com.basketballleague.support;

import java.lang.reflect.Field;

/**
 * Sets the database-generated id of an entity in unit tests, where no database assigns it.
 */
public final class EntityIds {

    private EntityIds() {
    }

    public static <T> T withId(T entity, long id) {
        try {
            final Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}
