package to.augmented.reality.android.ardb.sourcefacade.annotations.maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify a field as containing marked Points of Interest (POIs). The annotated field must
 * be a list of POI's (List{@literal <}POI{@literal>}) or array of POIs (POI[]).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.FIELD} )
public @interface POIs { }
