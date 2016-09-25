package to.augmented.reality.android.ardb.sourcefacade.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the spatial query to be a bounding box of the specified width and height.
 * Note the spatial query type can be changed using methods in ISpatialSource
 * (see {@link to.augmented.reality.android.ardb.sourcefacade.ISpatialSource})
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BoundingBox
//===========================
{
   /**
    * The name of the spatial (Geography/Geometry) database column to use as the bounding box centre
    */
   String column() default "";

   /**
    * The width of the bounding box
    */
   double width() default -1;

   /**
    * The height of the bounding box
    */
   double height() default -1;
}
