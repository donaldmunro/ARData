package to.augmented.reality.android.ardb.sourcefacade.annotations.sparql;

import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies selection (WHERE) parameters for a JDBC source
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SparQLWhereParameter
//------------------------------
{
   /**
    * The name of the parameter which will be used in the SparQL statement. Defaults to the column name if it
    * is specified and if not then to the annotated variable name.
    */
   String name() default "";

   /**
    * The name of the variable (excluding the ?) in the query to which this WHERE parameter pertains. If not specified then it
    * defaults to the variable name which must then correspond to a bind variable in the query.
    */
   String variable() default "";

   /**
    * When constructing the Where clause apply the database specific function from ISpatialFunctions
    * (see {@link ISpatialFunctions}) to convert the column to spatial
    * (ie the annotated variable can be a String containing a WKT representation of a GEOMETRY or GEOGRAPHY
    * value which is converted to spatial form using the relevant function in the where clause).
    * Set this to false if doing this manually in the where clause.
    * @return
    */
   boolean textToSpatial() default false;
}
