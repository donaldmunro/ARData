package to.augmented.reality.android.ardb.sourcefacade.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify a field or method that contains or returns a URL.
 * Example:
 * <code>
 * @Url private String getUrl();
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.FIELD} )
public @interface Url { }
