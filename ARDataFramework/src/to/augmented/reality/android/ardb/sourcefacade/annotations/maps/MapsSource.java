package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;

import to.augmented.reality.android.ardb.sourcefacade.maps.ImageFormat;
import to.augmented.reality.android.ardb.sourcefacade.maps.MapSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MapsSource
//==========================
{
   /**
    * Specifies a name for this data source.
    * For example {@literal @}SparQlSource(name="mysource"....
    */
   String name();

   /**
    * The Maps API to use (Google/MapQuest)
    */
   MapSource source();

   /**
    * The Maps API key to use (or use {@literal @}Key)
    */
   String key() default "";

   /**
    * The output image format.
    */
   ImageFormat format() default ImageFormat.PNG;

   /**
    * The image width. Can also be specified as a {@literal @}ImageWidth annotated field in which case it
    * overrides the value specified here.
    * @return
    */
   int width() default -1;

   /**
    * The image height. Can also be specified as a {@literal @}ImageHeight annotated field in which case it
    * overrides the value specified here.
    * @return
    */
   int height() default -1;

   /**
    * Specifies connection timeout
    * For example {@literal @}SparQlSource(connectionTimeout = 60, connectionTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   long connectionTimeout() default 60;

   /**
    * Specifies database connection timeout unit
    * For example {@literal @}SparQlSource(connectionTimeout = 60, connectionTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   TimeUnit connectionTimeoutUnit() default TimeUnit.SECONDS;

   /**
    * Specifies database read timeout
    * For example {@literal @}SparQlSource(readTimeout = 60, readTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   long readTimeout() default 30;

   /**
    * Specifies database read timeout unit
    * For example {@literal @}SparQlSource(readTimeout = 60, readTimeoutUnit = TimeUnit.SECONDS  ...
    * Default 60 seconds
    */
   TimeUnit readTimeoutUnit() default TimeUnit.SECONDS;
}
