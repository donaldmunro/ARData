package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the SparQL Having clause.<br>
 * Example<br>
 * <code>
 *    {@literal @}Having("HAVING (count(distinct *) > 1)")
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SparQLHaving { String value(); }