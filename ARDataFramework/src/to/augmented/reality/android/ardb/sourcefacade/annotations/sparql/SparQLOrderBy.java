package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the SparQL ORDER BY clause.<br>
 * Example<br>
 * <code>
 *    {@literal @}OrderBy("ORDER BY ?population")
 * </code>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SparQLOrderBy
{ String value(); }