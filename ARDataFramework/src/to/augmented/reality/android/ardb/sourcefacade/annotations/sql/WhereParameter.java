package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

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
public @interface WhereParameter
//------------------------------
{
   /**
    * The name of the parameter which will be used in the SQL statement. Defaults to the column name if it
    * is specified and if not then to the annotated variable name.
    */
   String name() default "";

   /**
    * If the query is a join then this indicates the name of the table or the alias for the table this query
    * parameter pertains to.
    *
    */
   String table() default "";

   /**
    * The name of the column in the database table to which this WHERE parameter pertains. If not specified then it
    * defaults to the variable name which must then correspond to a column name in the table.
    */
   String column() default "";

   /**
    * The SQL type (see {@link java.sql.Types}) to use. Defaults to inferring the type from the Java type of the
    * annotated variable.
    */
   int SQLType() default Integer.MAX_VALUE;

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
