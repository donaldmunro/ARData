package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the optional SparQL OFFSET clause
 * Example<br>
 * <code>
 *    {@literal @}Offset(20)<br>
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SparQLOffset { int value(); }