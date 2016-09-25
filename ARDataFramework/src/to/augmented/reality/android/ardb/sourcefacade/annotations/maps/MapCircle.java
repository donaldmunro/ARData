package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the map operation as a radius with the specified radius. Use the {@literal @}Latitude and
 * {@literal @}Longitude field annotations to specify the centre.
 * The field that is annotated must be a MapOp enum.
 * or MapQuest.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MapCircle
//=======================
{
   double radius();
}
