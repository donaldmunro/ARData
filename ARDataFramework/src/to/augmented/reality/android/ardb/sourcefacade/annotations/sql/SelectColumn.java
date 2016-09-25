package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a member variable as a column item in a projection (SELECT) list.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SelectColumn
//================================
{
   /**
    * The zero based sequence number for this selection column within the select list.
    */
   int sequence();

   /**
    * The table name or alias that the column is associated with
    */
   String table() default "";

   /**
    * The column name. Defaults to the name of the annotated variable if not specified which must then correspond to a
    * column name in the table or an expression involving a column. In the case of an expression  providing an alias is
    * advisable to simplify query processing.
    */
   String column() default "";

   /**
    * The column alias.
    */
   String alias() default "";

   /**
    * If the column is a spatial type and {@literal @}spatialToText is specified then the value is converted to s WKT
    * String in the callback.
    */
   boolean spatialToText() default false;

   /**
    * If the column is a string type containing a WKT representation of a spatial geometry and {@literal @}textToSpatial
    * is specified then the value is converted to a JTS Geometry type on callback.
    */
   boolean textToSpatial() default false;
}
