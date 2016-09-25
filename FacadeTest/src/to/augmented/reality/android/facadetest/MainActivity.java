package to.augmented.reality.android.facadetest;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor;
import to.augmented.reality.android.ardb.sourcefacade.ARDataSourceAggregator;
import to.augmented.reality.android.ardb.sourcefacade.DataPoint;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialQueryResult;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Format;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageDensity;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageHeight;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageWidth;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapCircle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapOutputImage;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapType;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapsSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Zoom;
import to.augmented.reality.android.ardb.sourcefacade.maps.MapSource;

import java.io.File;
import java.lang.reflect.Method;

@MapsSource(name = "GoogleMaps", key = "AIzaSyDBocUN5N4JcQSCJpkXCfNt38IzPDaTrr4", source = MapSource.GOOGLE)
@MapCircle(radius = 1000)
public class MainActivity extends Activity
{
   final static private String TAG = MainActivity.class.getSimpleName();

   ARDataSourceAggregator sources = null;

   ReflectiveAdapter adapter = null;

   EditText editLatitude, editLongitude;
   private SQLiteDatabase localDatabase;

   @ImageWidth int imageWidth = 300;

   @ImageHeight int imageHeight = 300;

   @ImageDensity int density = 2;

   @Zoom int zoom = 17;

   @MapType GoogleMapRequestor.MapType mapType = GoogleMapRequestor.MapType.roadmap;

   @Format GoogleMapRequestor.MapImageType imageType = GoogleMapRequestor.MapImageType.png;

   @MapOutputImage ImageView mapImage;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   //------------------------------------------------
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      editLatitude = (EditText) findViewById(R.id.editTextLatitude);
      editLongitude = (EditText) findViewById(R.id.editTextLongitude);
      mapImage = (ImageView) findViewById(R.id.image);
      final EditText editIp = (EditText) findViewById(R.id.editTextIp);
      if (isEmulator())
         editIp.setText("10.0.2.2");
      else
         editIp.setText("192.168.1.2");
      final ListView listView = (ListView) findViewById(R.id.listViewResults);
      final Button buttonOK = (Button) findViewById(R.id.buttonOK);
      adapter = new ReflectiveAdapter(this, 0);
      listView.setAdapter(adapter);
      final ActiveObject ao = new ActiveObject("threadpool", 5);
      Anything token = new Anything();
      token.put("sender", "MainActivity");
      File f = new File(getExternalFilesDir(null), getPackageName());
      f.mkdirs();
      f.setWritable(true, true);
      f = new File(f, "facade_test.sqlite");
      localDatabase = SQLiteDatabase.openOrCreateDatabase(f, null);
      sources = new ARDataSourceAggregator("denial", this, ao, localDatabase, token, new ResultCallback());
      final JdbcCircleQuery jdbcSource = new JdbcCircleQuery();
      final SparQLCircleQuery sparQLSource = new SparQLCircleQuery();
//      final GoogleMapQuery gmapSource = new GoogleMapQuery();

      buttonOK.setOnClickListener(new View.OnClickListener()
       //=======================================================
       {
          @Override
          public void onClick(View v)
          //-----------------------------------
          {
             final String address = editIp.getText().toString().trim();
             if (address.trim().isEmpty())
             {
                Toast.makeText(MainActivity.this, "Address must be specified", Toast.LENGTH_LONG).show();
                return;
             }
             jdbcSource.host = address;
             try
             {
                ISpatialSource[] srcs = sources.registerAnnotated(jdbcSource, sparQLSource, MainActivity.this);
                //Alternately test using image callback instead of automatic annotation image setting
                //sources.setCallbackType(srcs[2], ISpatialQueryResult.CALLBACK_TYPE.IMAGE);
             }
             catch (Exception e)
             {
                Log.e(TAG, "", e);
                throw new RuntimeException(e.getMessage(), e);
             }
             sparQLSource.altitude = 200;

//             SparqlDataSource sparqlDataSource = new SparqlDataSource("test", MainActivity.this, SparQLDialect.GEOSPARQL, ao);
//             sparqlDataSource.setSelect("SELECT DISTINCT ?name ?position");
//             sparqlDataSource.setWhere("WHERE {\n" +
//                                             "   ?L a :Location .\n" +
//                                             "   ?L :hasWGS84Geolocation ?geom .\n" +
//                                             "   ?geom geo:asWKT ?position .\n" +
//                                             "   ?L rdfs:label ?name .\n" +
//                                             "}");
//             sparqlDataSource.setOrderBy("ORDER BY ?name");
//             sparqlDataSource.setSpatialWhereSubjectName("?position");
//             sparqlDataSource.addPrefix(new String[]{ "prefix : <http://augmented.reality.to/semantic/owl#>" } );
//             try
//             {
//               sparqlDataSource.setEndpoint("http://" + address + ":8080/parliament/sparql");
//               sources.addSpatialSource(sparqlDataSource, 1000);
//               sources.setCallbackType(sparqlDataSource, ISpatialQueryResult.CALLBACK_TYPE.RAW_CURSOR);
//             }
//             catch (Exception e)
//             {
//                Toast.makeText(MainActivity.this, "Exception creating SparQL source: " + e.getMessage(),
//                               Toast.LENGTH_LONG).show();
//             }
             changeLocation();
          }
       });
   }

   private void changeLocation()
   //---------------------------
   {
      String lat = editLatitude.getText().toString();
      if (lat.trim().isEmpty())
         return;
      double latitude = Double.parseDouble(lat);
      String lon = editLongitude.getText().toString();
      if (lon.trim().isEmpty())
         return;
      double longitude = Double.parseDouble(lon);
      Location location = new Location(LocationManager.GPS_PROVIDER);
      location.setTime(System.currentTimeMillis());
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
         location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
      else
      {
         // Kludge because some location APIs requires elapsedtime in nanos but call is not available in all Android versions.
         try
         {
            Method makeCompleteMethod = null;
            makeCompleteMethod = Location.class.getMethod("makeComplete");
            if (makeCompleteMethod != null)
               makeCompleteMethod.invoke(location);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
      location.setLatitude(latitude);
      location.setLongitude(longitude);
      location.setAltitude(0);
      location.setAccuracy(1.0f);
      sources.onLocationChanged(location);
   }

   public static boolean isEmulator()
   //--------------------------------
   {
      int rating = 0;

      if (Build.PRODUCT.equals("sdk") ||
            Build.PRODUCT.equals("google_sdk") ||
            Build.PRODUCT.equals("sdk_x86") ||
            Build.PRODUCT.equals("vbox86p"))
         rating++;

      if (Build.MANUFACTURER.equals("unknown") ||
            Build.MANUFACTURER.equals("Genymotion"))
         rating++;

      if (Build.BRAND.equals("generic") ||
            Build.BRAND.equals("generic_x86"))
         rating++;

      if (Build.DEVICE.equals("generic") ||
            Build.DEVICE.equals("generic_x86") ||
            Build.DEVICE.equals("vbox86p"))
         rating++;

      if (Build.MODEL.equals("sdk") ||
            Build.MODEL.equals("google_sdk") ||
            Build.MODEL.equals("Android SDK built for x86"))
         rating++;

      if (Build.HARDWARE.equals("goldfish") ||
            Build.HARDWARE.equals("vbox86"))
         rating++;

      if (Build.FINGERPRINT.contains("generic/sdk/generic") ||
            Build.FINGERPRINT.contains("generic_x86/sdk_x86/generic_x86") ||
            Build.FINGERPRINT.contains("generic/google_sdk/generic") ||
            Build.FINGERPRINT.contains("generic/vbox86p/vbox86p"))
         rating++;

      return rating > 4;
   }

   class ResultCallback implements ISpatialQueryResult
   //=================================================
   {

      @Override public void onDatasetStart(String sourceName, Anything token) { }

      @Override
      public void onImageAvailable(String sourceName, Anything token, final Bitmap image)
      //---------------------------------------------------------------------------
      {
         if (sourceName.equals("GoogleMaps"))
         {
            MainActivity.this.runOnUiThread(new Runnable()
            {
               public void run() { mapImage.setImageBitmap(image); }
            });
         }
      }

      @Override
      public void onCursorAvailable(String sourceName, Anything token, Cursor cursor)
      //-----------------------------------------------------------------------------
      {
         while (cursor.moveToNext())
         {
//            String[] columnNames = cursor.getColumnNames();
            final JdbcCircleQuery instance = new JdbcCircleQuery();
            instance.source = "SparQL";
            instance.description = "SparQL-" + cursor.getString(1);
            instance.location = cursor.getString(2);
            MainActivity.this.runOnUiThread(new Runnable()
            {
               public void run() { adapter.add(instance); }
            });
         }
         MainActivity.this.runOnUiThread(new Runnable()
         {
            public void run() { adapter.notifyDataSetChanged(); }
         });
      }

      @Override
      public void onDataPointAvailable(String sourceName, Anything token, DataPoint data)
      {

      }

      @Override
      public void onAnnotationAvailable(String sourceName, Anything token, final Object annotated)
      //------------------------------------------------------------------------------------
      {
//            final JdbcCircleQuery instance = (JdbcCircleQuery) annotated;
         if (annotated instanceof SparQLCircleQuery)
         {
            SparQLCircleQuery instance = (SparQLCircleQuery) annotated;
            instance.name = "SparQL-" + instance.name;
         }
         MainActivity.this.runOnUiThread(new Runnable()
         {
            public void run()
            {
               adapter.add(annotated);
               adapter.notifyDataSetChanged();
            }
         });

      }

      @Override
      public void onError(String sourceName, Anything token, final CharSequence message, Throwable exception)
      {
         Log.e(TAG, "Error: " + message);
         MainActivity.this.runOnUiThread(new Runnable()
         {
            public void run()
            {
               Toast.makeText(MainActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            }
         });
      }

      @Override public void onDatasetEnd(String sourceName, Anything token) { }
   }
}
