package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import to.augmented.reality.android.ardb.sourcefacade.jdbc.JoinType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify a join condition
 * Example:
 * <code>
 *    @Table(names={"products", "order"}, aliases = {"p", "o"})
 *    @Join(joinType=INNER, joinCondition="p.productcode = o.productcode")
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Join
//====================
{
   /**
    * Table name
    * {@literal @}Join(name="emp") or {@literal @}Join(name={"emp", "dept"})
    */
   String table();

   /**
    * Table alias
    * Can be used in conjunction with {@literal @}From when specifying join conditions. For example:
    * <code>
    * {@literal @}From(name="devices", alias = "d")
    * {@literal @}Join(name="location", alias= "l", joinType=JoinType.INNER, joinCondition="d.id = l.id")
    * </code>
    */
   String alias() default "";

   /**
    * The join type
    */
   JoinType joinType();

   /**
    * The join (WHERE) condition (ignored for Cartesian (CROSS OUTER) joins)
    */
   String joinCondition();
}
