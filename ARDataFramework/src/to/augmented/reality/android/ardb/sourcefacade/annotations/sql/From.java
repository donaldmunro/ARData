package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the database table to be used for a JDBC data source.
 * If two or more tables are specified then a {@literal @}Join annotation ({@link Join}) should also be specified.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface From
//========================
{
   /**
    * Table name
    * {@literal @}From("emp")
    */
   String value();

   /**
    * Table alias
    * Can be used in conjunction with {@literal @}Join when specifying join conditions. For example:
    * <code>
    * {@literal @}From(name="devices", alias = "d")
    * {@literal @}Join(name="location", alias= "l", joinType=JoinType.INNER, joinCondition="d.id = l.id")
    * </code>
    */
   String alias() default "";
}
