package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify additional where clauses when using {@literal @}Circle or {@literal @}BoundingBox or the entire
 * selection (WHERE) clause for a generic query. When used in conjunction with {@literal @}WhereParameter the where
 * clause specifies named parameters by prefixing a name with a colon eh WHERE id = :id. There should then be a
 * corresponding @WhereParameter annotated parameter named id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Where
//=====================
{
   /**
    * The WHERE clause
    */
   String value();
}
