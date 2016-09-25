package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the SparQL WHERE clause.<br>
 * Example<br>
 * <code>
 *    {@literal @}Where("WHERE { ?country a type:LandlockedCountries ; prop:population ?population . }")
 * </code>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SparQLWhere { String value(); }