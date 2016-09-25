package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify that a field contains a file name to use as a SpatiaLite database file.
 * The annotated field should be a String or a File.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DatabaseFile { }
