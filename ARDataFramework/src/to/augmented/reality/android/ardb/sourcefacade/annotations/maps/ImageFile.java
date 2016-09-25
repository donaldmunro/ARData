package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify that a field contains a file name to use as an output file for images.
 * If this annotation is specified then images will be created as files and not in memory.
 * If the file name contained in the annotated field does not have an extension
 * then  one matching the image output type will be added. If the file name does
 * have a valid image extension (png, jpg, gif) and it differs from the image
 * output type then the image extension type will override the specified one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ImageFile { }
