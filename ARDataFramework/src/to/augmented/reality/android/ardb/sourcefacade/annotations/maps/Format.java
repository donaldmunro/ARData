package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the map image type which can be changed per query and will override the
 * {@literal @}MapsSource format option if not null.
 * The field that is annotated must be a
 * {@link to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor.MapImageType} (Google),
 * {@link to.augmented.reality.android.ardb.http.maps.MapQuestRequestor.MapImageType} (MapQuest) or
 * a {{@link to.augmented.reality.android.ardb.sourcefacade.maps.ImageFormat} (any API).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Format { }
