package to.augmented.reality.android.ardb.sourcefacade.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the spatial query to be a radius with the specified radius.
 * Note the spatial query type can be changed using methods in ISpatialSource
 * (see {@link to.augmented.reality.android.ardb.sourcefacade.ISpatialSource})
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Circle
//======================
{
   /**
    * The name of the spatial (Geography/Geometry) database column to use as the bounding box centre
    */
   String column();

   /**
    * The radius od the radius query.
    */
   double radius();
}
