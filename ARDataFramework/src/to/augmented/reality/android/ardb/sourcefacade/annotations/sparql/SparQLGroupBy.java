package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the SparQL GROUP BY clause.<br>
 * Example<br>
 * <code>
 *    {@literal @}GroupBy("GROUP BY ?country_type")
 * </code>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SparQLGroupBy { String value(); }
