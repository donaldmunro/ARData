package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the map type which can be changed per query.
 * The field that is annotated must be a
 * {@link to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor.MapType} (Google),
 * {@link to.augmented.reality.android.ardb.http.maps.MapQuestRequestor.MapType} (MapQuest) or
 * a {{@link to.augmented.reality.android.ardb.sourcefacade.maps.MapFormat} (any API).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MapType { }
