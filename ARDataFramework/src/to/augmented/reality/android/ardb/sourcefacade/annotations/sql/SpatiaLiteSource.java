package to.augmented.reality.android.ardb.sourcefacade.annotations.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a class as being SpatiaLite annotated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpatiaLiteSource
//================================
{
   /**
    * Specifies a name for this data source.
    * For example {@literal @}JdbcSource(name="mysource"....
    */
   String name();

   /**
    * Specifies the SpatiaLite database file. If left blank then it should be specified using an
    * {@literal @}DatabaseFile annotated String or File variable or specified programatically on
    * the {@link SpatiaLiteSource} instance.
    * @return
    */
   String file() default "";
}
