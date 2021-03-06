package com.onyx.persistence.annotations

/**
 * This annotation is used to indicate method that is invoked after inserting or updating a ManagedEntity.
 *
 *
 * @author Tim Osborn
 *
 * @since 1.0.0
 * `
 * @PostPersist
 * public void onPostPersist()
 * {
 *      persistedObjects ++;
 * }
 *
 * @see com.onyx.persistence.ManagedEntity
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class PostPersist
