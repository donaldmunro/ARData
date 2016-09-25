package to.augmented.reality.android.ardb.sourcefacade;

import android.content.Context;
import android.database.Cursor;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;

import java.util.Map;

public interface IAnnotationProcessor
//===================================
{
   ISpatialSource createAnnotated(String sourceName, Context context, ActiveObject activeObject,
                                  Object instance);

   Map<String, Object> processParameters(Object instance, Map<String, Object> parameters);

   void processCursor(Object instance, Cursor cursor, ColumnContext[] projectionColumns) throws IllegalAccessException;

   ISpatialSource.SPATIAL_OPERATION operation();

   double circleRadius();

   double bboxWidth();

   double bboxHeight();

   Object[] userDefOpParameters();
}
