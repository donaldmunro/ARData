package to.augmented.reality.android.ardb.tests.tiledtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.StackView;
import android.widget.TextView;
import android.widget.Toast;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.http.maps.POI;
import to.augmented.reality.android.ardb.sourcefacade.ARDataSourceAggregator;
import to.augmented.reality.android.ardb.sourcefacade.DataPoint;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialQueryResult;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
//=================================================
{
   TextView textLatitude, textLongitude, googleLabelText, mapQuestLabelText; //, spatiaLiteLabelText;
   ImageView googleMapImg, mapQuestImg, spatiaLiteImg;
   Spinner spatiaLiteLabelSpinner;
   ArrayAdapter<String> spatiaLiteLabelSpinnerAdapter;
   StackView spatiaLiteStackImg;
   StackAdapter stackAdapter = new StackAdapter();
   Button buttonStartStop;
   private SQLiteDatabase localDatabase;
   private GoogleMapQuery googleAnno;
   private MapQuestQuery mapQuestAnno;
   private ARDataSourceAggregator sources;
   private double latitude = -34.007994, longitude = 25.669628;
   private ExecutorService locationStepper;
   private boolean isStepping = true;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   //-------------------------------------------------
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      textLatitude = (TextView) findViewById(R.id.latitudeText);
      textLongitude = (TextView) findViewById(R.id.longitudeText);
      googleLabelText = (TextView) findViewById(R.id.googleLabel);
      mapQuestLabelText = (TextView) findViewById(R.id.mapQuestLabel);
      googleMapImg = (ImageView) findViewById(R.id.googleImg);
      mapQuestImg = (ImageView) findViewById(R.id.mapQuestImg);
//      spatiaLiteImg = (ImageView) findViewById(R.id.spatiaLiteImg);
//      spatiaLiteLabelText = (TextView) findViewById(R.id.spatiaLabel);
      spatiaLiteLabelSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
      spatiaLiteLabelSpinner = (Spinner) findViewById(R.id.spatiaLabelSpinner);
      spatiaLiteLabelSpinner.setAdapter(spatiaLiteLabelSpinnerAdapter);
      spatiaLiteStackImg = (StackView) findViewById(R.id.spatiaLiteStack);
      spatiaLiteStackImg.setAdapter(stackAdapter);
      buttonStartStop = (Button) findViewById(R.id.buttonStopStart);
      spatiaLiteLabelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
         {
            String s = (String) spatiaLiteLabelSpinner.getSelectedItem();
            int pos = spatiaLiteLabelSpinnerAdapter.getPosition(s);
            if (pos >= 0)
               spatiaLiteStackImg.setSelection(pos);
         }

         @Override public void onNothingSelected(AdapterView<?> parent) { }
      });

      RelativeLayout spatiaLayout = (RelativeLayout) findViewById(R.id.spatiaLayout);
      final ActiveObject ao = new ActiveObject("threadpool", 5);
      Anything token = new Anything();
      token.put("sender", "MainActivity");

      File f = new File(getExternalFilesDir(null), getPackageName());
      f.mkdirs();
      f.setWritable(true, true);
      f = new File(f, "facade_test.sqlite");
      f.delete();
      localDatabase = SQLiteDatabase.openOrCreateDatabase(f, null);
      sources = new ARDataSourceAggregator("main", this, ao, localDatabase, token, new ResultCallback());
      final SpatiaLiteQuery spatiaAnno = new SpatiaLiteQuery();
      AssetManager assets = getAssets();
      f = new File(getExternalCacheDir(), "ardata");
      f.mkdirs();
      spatiaAnno.databaseFile = new File(f, "spatialite.sqlite");
      BufferedInputStream bis = null;
      BufferedOutputStream bos = null;
      try
      {
         bis = new BufferedInputStream(assets.open("spatialite.sqlite"), 32768);
         bos = new BufferedOutputStream(new FileOutputStream(spatiaAnno.databaseFile), 32768);
         int ch;
         while ( (ch = bis.read()) >= 0) bos.write(ch);
      }
      catch (Exception e)
      {
         Toast.makeText(this, "Error copying SpatiaLite database - Spatialite tile disabled", Toast.LENGTH_LONG).show();
      }
      finally
      {
         if (bis != null)
            try { bis.close(); } catch (Exception _e) {}
         if (bos != null)
            try { bos.close(); } catch (Exception _e) {}
      }
      googleAnno = new GoogleMapQuery(googleMapImg);
      mapQuestAnno = new MapQuestQuery(mapQuestImg);
      ISpatialSource[] srcs;
      try
      {
         srcs = sources.registerAnnotated(spatiaAnno, googleAnno, mapQuestAnno);
         //srcs = sources.registerAnnotated(spatiaAnno);
      }
      catch (Exception e)
      {
         Toast.makeText(MainActivity.this, "Error initialising data sources", Toast.LENGTH_LONG).show();
         return;
      }
      for (int i=0; i<srcs.length; i++)
         sources.setSensitivity(srcs[i], 20);
      locationStepper = Executors.newSingleThreadExecutor();
      locationStepper.submit(new Runnable()
      {
         @Override
         public void run()
         //---------------
         {
            boolean isGoingDown = true, isGoingUp = false;
            if (googleMapImg.getWidth() > 0)
               googleAnno.imageWidth = googleMapImg.getWidth();
            else
               googleAnno.imageWidth = 400;
            if (googleMapImg.getHeight() > 0)
               googleAnno.imageHeight = googleMapImg.getHeight();
            else
               googleAnno.imageHeight = 400;
            if (mapQuestImg.getWidth() > 0)
               mapQuestAnno.imageWidth = mapQuestImg.getWidth();
            else
               mapQuestAnno.imageWidth = 400;
            if (mapQuestImg.getHeight() > 0)
               mapQuestAnno.imageHeight = mapQuestImg.getHeight();
            else
               mapQuestAnno.imageHeight = 400;
            while (true)
            {
               if (! isStepping)
               {
                  try { Thread.sleep(300); } catch (Exception _e) {}
                  continue;
               }
               try
               {
                  if (isGoingDown)
                  {
                     deltaLocation(5.0, 3 * Math.PI / 2);
                     //stackAdapter.clear();
                     changeLocation();
                     try { Thread.sleep(800); } catch (Exception _e) {}
                     if (latitude <= -34.010635)
                     {
                        isGoingDown = false;
                        isGoingUp = true;
                        latitude = -34.010635; longitude = 25.667643;
                     }
                  } else if (isGoingUp)
                  {
                     deltaLocation(5.0, Math.PI / 2);
                     changeLocation();
                     try { Thread.sleep(500); } catch (Exception _e) {}
                     if (latitude >= -34.008323)
                     {
                        isGoingDown = true;
                        isGoingUp = false;
                        latitude = -34.007994; longitude = 25.669628;
                     }
                  }
                  //,
               }
               catch (Exception e)
               {
                  Log.e("MainActivity", "", e);
               }
            }
         }
      });
   }

   static final double RADIUS = 6378137; // Earth's radius

   private void deltaLocation(double delta, double theta)
   //----------------------------------------------------
   {
      double de = delta * Math.cos(theta);
      double dn = delta * Math.sin(theta);
      double dLat = dn/RADIUS;
      double dLon = de/(RADIUS*Math.cos(Math.PI * this.latitude / 180));
      float latitude = (float) (this.latitude + dLat * 180/Math.PI);
      float longitude = (float) (this.longitude + dLon * 180/Math.PI);
      this.latitude = latitude;
      this.longitude = longitude;
   }

   private void changeLocation()
   //---------------------------
   {
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
      googleAnno.pois.clear();
      googleAnno.pois.add(new POI((float) latitude, (float) longitude, "X"));
      mapQuestAnno.pois.clear();
      mapQuestAnno.pois.add(new POI((float) latitude, (float) longitude, "X"));
      sources.onLocationChanged(location);
      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            textLatitude.setText(Double.toString(latitude));
            textLongitude.setText(Double.toString(longitude));
         }
      });
   }

   public void onStartStop(View view)
   {
      if (isStepping)
      {
         buttonStartStop.setText("Start");
         isStepping = false;
      }
      else
      {
         buttonStartStop.setText("Stop");
         isStepping = true;
      }

   }

   class ResultCallback implements ISpatialQueryResult
   //=================================================
   {
      @Override
      public void onDatasetStart(String sourceName, Anything token)
      {
         if (sourceName.equals("SpatiaLite"))
         {
            MainActivity.this.runOnUiThread(new Runnable()
            {
               @Override
               public void run()
               {
                  stackAdapter.clear();
                  stackAdapter.notifyDataSetChanged();
//                 spatiaLiteLabelText.setText("");
                  spatiaLiteLabelSpinnerAdapter.clear();
                  spatiaLiteLabelSpinnerAdapter.notifyDataSetChanged();
               }
            });
         }
      }

      @Override
      public void onImageAvailable(String sourceName, Anything token, Bitmap image)
      {
         int a = 1;
         a = 2;
      }

      @Override
      public void onCursorAvailable(String sourceName, Anything token, Cursor cursor)
      {
         int a = 1;
         a = 2;
      }

      @Override
      public void onDataPointAvailable(String sourceName, Anything token, DataPoint data)
      {
         int a = 1;
         a = 2;
      }

      @Override
      public void onAnnotationAvailable(String sourceName, Anything token, Object annotated)
      //------------------------------------------------------------------------------------
      {
         if (sourceName.equals("SpatiaLite"))
         {
            final SpatiaLiteQuery instance = (SpatiaLiteQuery) annotated;
            MainActivity.this.runOnUiThread(new Runnable()
            {
               public void run()
               {
//                  String text = spatiaLiteLabelText.getText().toString();
//                  spatiaLiteLabelText.setText(text + "," + instance.description);
                  spatiaLiteLabelSpinnerAdapter.add(instance.description);
//                  spatiaLiteImg.setImageBitmap(BitmapFactory.decodeByteArray(instance.image, 0, instance.image.length));
                  stackAdapter.add(BitmapFactory.decodeByteArray(instance.image, 0, instance.image.length));
                  stackAdapter.notifyDataSetChanged();
               }
            });

         }
      }

      @Override
      public void onError(String sourceName, Anything token, final CharSequence message, Throwable exception)
      //-----------------------------------------------------------------------------------------------------
      {
         Log.e("MainActivity", message.toString(), exception);
         MainActivity.this.runOnUiThread(new Runnable()
         {
            public void run() { Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG). show(); }
         });
      }

      @Override
      public void onDatasetEnd(String sourceName, Anything token)
      {

      }
   }

   class StackAdapter extends BaseAdapter
   //=====================================
   {
      final List<Bitmap> images = new ArrayList<>();

      public void add(Bitmap image) { images.add(image); }

      public void clear() { images.clear(); }

      public int getCount() { return images.size(); }

      public Object getItem(int position) { return position; }

      public long getItemId(int position) { return position; }

      public View getView(int position, View view, ViewGroup parent)
      //------------------------------------------------------------
      {
         if (view == null)
         {
            LayoutInflater vi = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = vi.inflate(R.layout.stackitem, null, false);
         }
         ImageView imageView = (ImageView) view.findViewById(R.id.spatiaLiteImg);
         if (images.size() > 0)
            imageView.setImageBitmap(images.get(position));
         else
            imageView.setImageResource(R.drawable.ic_launcher);
         return view;
      }
   }
}
