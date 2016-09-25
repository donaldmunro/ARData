package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the map operation as a bounding box with the specified width and height. Use the {@literal @}Latitude and
 * {@literal @}Longitude field annotations to specify the centre.
 * of the MapsSource annotation..
 * The field that is annotated must be a MapOp enum.
 * or MapQuest.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BoundingBox
//=======================
{
   double width();

   double height();
}
