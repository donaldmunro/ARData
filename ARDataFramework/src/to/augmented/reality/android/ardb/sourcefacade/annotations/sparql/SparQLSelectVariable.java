package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a member variable as a bound variable in a SparQL SELECT for purposes of retrieval of values following
 * a query.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SparQLSelectVariable
//================================
{
   /**
    * The bound variable name (without a leading ?). Defaults to the name of the annotated variable if not specified
    * which must then correspond to a bound variable name in the query.
    */
   String name() default "";

   /**
    * The column alias.
    */
   String alias() default "";
}
