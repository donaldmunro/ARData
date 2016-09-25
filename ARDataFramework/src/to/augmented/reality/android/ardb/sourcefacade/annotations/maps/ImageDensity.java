package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Image density AKA scale. Specifies pixel density. Valid values 1 and 2. The annotated field must be
 * a number
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ImageDensity { }
