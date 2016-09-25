package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify multiple join conditions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Joins
//======================
{
   /**
    * Specify multiple Joins as an array eg
    * {@literal @}Joins({{@literal @}Join(...), {@literal @}Join(...), ...})
    */
   Join[] value();
}
