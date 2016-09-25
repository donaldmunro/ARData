package to.augmented.reality.android.ardb.sourcefacade;

import android.database.Cursor;
import android.graphics.Bitmap;
import to.augmented.reality.android.ardb.anything.Anything;

public interface ISpatialQueryResult
//==================================
{
   enum CALLBACK_TYPE { ANNOTATED_OBJECT, DATAPOINT,  RAW_CURSOR, IMAGE }

   void onDatasetStart(String sourceName, Anything token);

   void onImageAvailable(String sourceName, Anything token, Bitmap image);

   void onCursorAvailable(String sourceName, Anything token, Cursor cursor);

   void onDataPointAvailable(String sourceName, Anything token, DataPoint data);

   void onAnnotationAvailable(String sourceName, Anything token, Object annotated);

   void onError(String sourceName, Anything token, CharSequence message, Throwable exception);

   void onDatasetEnd(String sourceName, Anything token);
}
