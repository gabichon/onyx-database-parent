package com.onyx.persistence.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation is used to indicate method that is invoked before removing a ManagedEntity.
 *
 *
 * @author Tim Osborn
 *
 * @since 1.0.0
 *
 * <pre>
 *     <code>
 *      {@literal @}Attribute
 *       protected Date persistedDate = null;
 *
 *      {@literal @}PreRemove
 *       public void onPreRemove()
 *       {
 *           Cache.MyCache.remove(this);
 *       }
 *     </code>
 * </pre>
 *
 * @see com.onyx.persistence.ManagedEntity
 *
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface PreRemove {
}
