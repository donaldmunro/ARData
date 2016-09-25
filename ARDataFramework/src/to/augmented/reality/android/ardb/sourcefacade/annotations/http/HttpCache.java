package to.augmented.reality.android.ardb.sourcefacade.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HttpCache
//=====================
{
   long age() default -1;

   TimeUnit ageUnit() default TimeUnit.MILLISECONDS;

   String directory() default "";

   boolean overideNoCache() default false;

   boolean overideAge() default false;
}
