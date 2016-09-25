package to.augmented.reality.android.ardb.sourcefacade;

import android.database.Cursor;
import android.net.Uri;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.util.Util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.util.Map;

public class CursorCallback implements ICursorQueryCallback
//=========================================================
{
   static final private String TAG = CursorCallback.class.getName();

   private String sourceName;
   private ISpatialQueryResult.CALLBACK_TYPE callbackType;
   private final Anything token;
   private final ISpatialQueryResult callback;
   private IAnnotationProcessor annotationProcessor = null;
   private Object annotatedInstance = null;
   private DataType[] types;
   private ColumnContext[] columns;

   private Class<?> C;
   private Method cloneMethod;
   private Constructor constructor;

   public CursorCallback(String sourceName, ISpatialQueryResult.CALLBACK_TYPE callbackType, final Anything token,
                         ColumnContext[] projectionColumns, DataType[] dataTypes, final ISpatialQueryResult callback)
   //----------------------------------------------------------------------------------------
   {
      this(sourceName, callbackType, token, projectionColumns, dataTypes, callback, null, null);
   }

   public CursorCallback(String sourceName, ISpatialQueryResult.CALLBACK_TYPE callbackType, final Anything token,
                         ColumnContext[] columns, DataType[] dataTypes, ISpatialQueryResult callback,
                         IAnnotationProcessor processor, Object annotatedInstance)
   //---------------------------------------------------------------------------------------------------------------
   {
      this.sourceName = sourceName;
      this.token = token;
      this.callback = callback;
      this.callbackType = callbackType;
      this.columns = columns;
      this.types = dataTypes;
      this.annotationProcessor = processor;
      this.annotatedInstance = annotatedInstance;
      if (annotatedInstance != null)
      {
         C = annotatedInstance.getClass();
         cloneMethod = Util.getCloneMethod(C, annotatedInstance);
         constructor = Util.getEmptyConstructor(C, annotatedInstance);
      }
   }

   @Override
   public void onQueried(Anything token, Cursor cursor, int retcode)
   //---------------------------------------------------------------
   {
      try
      {
         processCursor(cursor);
      }
      catch (Exception e)
      {
         String query = token.getAsString("query", null);
//            Log.e(TAG, "Exception processing query results " + ((query != null) ? query : ""), e);
         callback.onError(sourceName, token, "Exception processing query results " + ((query != null) ? query : ""), e);
      }
   }

   @Override
   public void onError(Anything token, int code, CharSequence message, Throwable exception)
   //--------------------------------------------------------------------------------------
   {
      if (token == null)
         token = this.token;
      callback.onError(sourceName, token, message, exception);
   }

   @Override
   public void onJdbcQueried(Connection connection, Anything token, Cursor cursor, Map<String, Object> params,
                             Map<String, int[]> paramIndices)
   //---------------------------------------------------------------------------------------------------------------
   {
      try
      {
         processCursor(cursor);
      }
      catch (Exception e)
      {
         String query = token.getAsString("query", null);
//            Log.e(TAG, "Exception processing query results " + ((query != null) ? query : ""), e);
         callback.onError(sourceName, token, "Exception processing query results " + ((query != null) ? query : ""), e);
      }
      finally
      {
         if (connection != null)
            try { connection.close(); } catch (Exception _e) {}
      }
   }

   @Override
   public void onJdbcError(Connection connection, Anything token, CharSequence message, Throwable exception)
   //----------------------------------------------------------------------------
   {
      if (connection != null)
         try { connection.close(); } catch (Exception _e) {}
      if (token == null)
         token = this.token;
      callback.onError(sourceName, token, message, exception);
   }

   protected void processCursor(Cursor cursor)
   //---------------------------------------
   {
      switch (callbackType)
      {
         case RAW_CURSOR:
            callback.onCursorAvailable(sourceName, token, cursor);
            break;
         case DATAPOINT:
            if ((types == null) || (types.length == 0))
               throw new RuntimeException("QueryCallback requires projection types to be set.");
            Object[] values = new Object[types.length];
            WKTReader wktReader = null;
            callback.onDatasetStart(sourceName, token);
            while (cursor.moveToNext())
            {
               for (int i = 0; i < types.length; i++)
               {
                  switch (types[i])
                  {
                     case WKT:
                        String s = cursor.getString(i);
                        values[i] = s;
                        if (wktReader == null)
                           wktReader = new WKTReader();
                        try
                        {
                           values[i] = wktReader.read(s);
                           break;
                        }
                        catch (ParseException e) { types[i] = DataType.STRING; }
                     case STRING:
                        values[i] = cursor.getString(i);
                        break;
                     case BLOB:
                     case IMAGE:
                        values[i] = cursor.getBlob(i);
                        break;
                     case FLOAT:
                        values[i] = cursor.getFloat(i);
                        break;
                     case DOUBLE:
                        values[i] = cursor.getDouble(i);
                        break;
                     case INT:
                        values[i] = cursor.getInt(i);
                        break;
                     case LONG:
                        values[i] = cursor.getLong(i);
                        break;
                     case URI:
                        String url = cursor.getString(i);
                        try { values[i] = new URI(url); }
                        catch (URISyntaxException e)
                        {
                           values[i] = Uri.parse(url);
                        }
                        break;
                  }
               }
               DataPoint dp = new DataPoint(types);
               dp.set(values);
               callback.onDataPointAvailable(sourceName, token, dp);
            }
            callback.onDatasetEnd(sourceName, token);
            break;
         case ANNOTATED_OBJECT:
            if (annotationProcessor == null)
               throw new RuntimeException("Annotation processor not set");
            callback.onDatasetStart(sourceName, token);
            while (cursor.moveToNext())
            {
               Object instance = null;
               if (cloneMethod != null)
                  try { instance = cloneMethod.invoke(annotatedInstance); } catch (Exception _e) { instance = null; }
               if (instance == null)
                  try { instance = constructor.newInstance(); } catch (Exception _e) { instance = null; }
               if (instance == null)
                  throw new RuntimeException("Annotated class " + C.getName() + " must implement Cloneable or at a" +
                                             "minimum have a noargs constructor");

               cursor.getType(0);
               try
               {
                  annotationProcessor.processCursor(instance, cursor, columns);
                  callback.onAnnotationAvailable(sourceName, token, instance);
               }
               catch (Exception ee)
               {
//                        Log.e(TAG, "Error setting annotated fields", ee);
                  callback.onError(sourceName, token, "Error setting annotated fields: " + ee.getMessage(), ee);
                  break;
               }
            }
            callback.onDatasetEnd(sourceName, token);
      }
   }
}
