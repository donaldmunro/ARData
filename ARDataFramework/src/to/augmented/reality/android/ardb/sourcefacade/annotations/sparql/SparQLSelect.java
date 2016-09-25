package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the SparQL SELECT clause.<br>
 * Example<br>
 * <code>
 *    {@literal @}Select("SELECT ?country ?population")
 * </code>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SparQLSelect { String value(); }