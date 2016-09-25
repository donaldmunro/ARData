package to.augmented.reality.android.ardb.sourcefacade.sparql;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.sparql.SparQLDialect;
import to.augmented.reality.android.ardb.sourcefacade.DataPoint;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialQueryResult;
import to.augmented.reality.android.test.BuildConfig;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Config(constants = BuildConfig.class, sdk = 21, manifest = "/ssd/Android/ARData/ARDataFramework/AndroidManifest.xml")
@RunWith(RobolectricGradleTestRunner.class)
public class SparqlDataSourceTest
//===============================
{
   static final private boolean IS_DEBUG = true;

   @Test
   public void testCircle() throws Exception
   //---------------------------------
   {
      ShadowApplication app = ShadowApplication.getInstance();
      assertNotNull(app);
      Context context = app.getApplicationContext();
      assertNotNull(context);
      SQLiteDatabase database = null;
      try
      {
         database = context.openOrCreateDatabase("ardata", 0, null);
         assertNotNull(database);
         ActiveObject ao = new ActiveObject("test", 5);
         SparQLDataSource instance = new SparQLDataSource("test", context, SparQLDialect.GEOSPARQL, ao);
         instance.setSelect("SELECT DISTINCT ?name ?position");
         instance.setWhere("WHERE {\n" +
                                 "   ?L a :Location .\n" +
                                 "   ?L :hasWGS84Geolocation ?geom .\n" +
                                 "   ?geom geo:asWKT ?position .\n" +
                                 "   ?L rdfs:label ?name .\n" +
                                 "}");
         instance.setOrderBy("ORDER BY ?name");
         instance.setSpatialWhereSubjectName("?position");
         instance.setEndpoint("http://127.0.0.1:8080/parliament/sparql");
         instance.addPrefix(new String[] { "prefix : <http://augmented.reality.to/semantic/owl#>" });
         Anything token = new Anything("test");
         // data in test database is wrong way around for latitude/longitude
         //Future<?> future = instance.radius(-34.00931, 25.6685, 2000, ISpatialQueryResult.CALLBACK_TYPE.RAW_CURSOR, token,
         Future<?> future = instance.radius(25.6685, -34.00931, 2000, ISpatialQueryResult.CALLBACK_TYPE.RAW_CURSOR, token,
                                            new ISpatialQueryResult()
          //======================
          {
             @Override public void onDatasetStart(String sourceName, Anything token) { }

             @Override public void onImageAvailable(String sourceName, Anything token, Bitmap image) { }

             @Override
             public void onCursorAvailable(String sourceName, Anything token, Cursor cursor)
             {
                assertNotNull(cursor);
                assertTrue(cursor.moveToNext());
                String[] names = cursor.getColumnNames();
                System.out.println(names[0]);
             }

             @Override
             public void onDataPointAvailable(String sourceName, Anything token, DataPoint data)
             { }

             @Override
             public void onAnnotationAvailable(String sourceName, Anything token,
                                               Object annotated)
             { }

             @Override
             public void onError(String sourceName, Anything token, CharSequence message, Throwable exception)
             {
                assertTrue(message.toString(), false);
             }

             @Override public void onDatasetEnd(String sourceName, Anything token) { }
          });
         assertNotNull(future);
         if (IS_DEBUG)
            future.get();
         else
            future.get(30, TimeUnit.SECONDS);
      }
      finally
      {
         if (database != null)
            try { database.close(); } catch (Exception _e) {}
      }
   }
}
