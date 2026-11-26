package com.basketballleague.support;

/**
 * Sets the database-generated id of an entity in unit tests, where no database assigns it.
 */
public final class EntityIds {

    private EntityIds() {
    }

    public static <T> T withId(T entity, long id) {
        return EntityFields.withField(entity, "id", id);
    }
}
