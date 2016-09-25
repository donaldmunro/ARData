package my.maptest;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.MIME_TYPES;
import to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor;
import to.augmented.reality.android.ardb.http.maps.IMapRequestorCallback;
import to.augmented.reality.android.ardb.http.maps.MapQuestRequestor;
import to.augmented.reality.android.ardb.http.maps.POI;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
{

   private EditText editLatitude;
   private EditText editLongitude;
   private EditText editKey;
   private Spinner spinnerApi;
   private Spinner spinnerZoom;
   private ActiveObject activeObject = new ActiveObject("ao", 5);
   private MapQuestRequestor mapQuestRequestor = null;
   private GoogleMapRequestor googleMapRequestor = null;
   private ImageView imageView;
   private EditText editBottomLatitude;
   private EditText editBottomLongitude;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   //------------------------------------------------
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      spinnerApi = (Spinner) findViewById(R.id.spinnerApi);
      spinnerZoom = (Spinner) findViewById(R.id.spinnerZoom);
      editKey = (EditText) findViewById(R.id.editKey);
      editLatitude = (EditText) findViewById(R.id.editTextLatitude);
      editLongitude = (EditText) findViewById(R.id.editTextLongitude);
      editBottomLatitude = (EditText) findViewById(R.id.editTextBottomLatitude);
      editBottomLongitude = (EditText) findViewById(R.id.editTextBottomLongitude);
      imageView = (ImageView) findViewById(R.id.imageViewMap);
      final Button buttonOK = (Button) findViewById(R.id.buttonOK);
      buttonOK.setOnClickListener(new View.OnClickListener()
      //=======================================================
      {
         @Override
         public void onClick(View v)
         //-----------------------------------
         {
            String api = (String) spinnerApi.getSelectedItem();
            int zoom = 16;
            try { zoom = Integer.parseInt(spinnerZoom.getSelectedItem().toString().trim()); } catch (Exception e) { zoom = -1; }
            if (zoom < 0)
            {
               Toast.makeText(MainActivity.this, "Invalid zoom", Toast.LENGTH_LONG).show();
               return;
            }
            String key  = editKey.getText().toString().trim();
//            if (key.trim().isEmpty())
//            {
//               Toast.makeText(MainActivity.this, "Invalid key", Toast.LENGTH_LONG).show();
//               return;
//            }
            String lat = editLatitude.getText().toString().trim();
            float latitude = 0;
            try { latitude = Float.parseFloat(lat); } catch (Exception e) { lat = ""; }
            if (lat.trim().isEmpty())
            {
               Toast.makeText(MainActivity.this, "Invalid latitude", Toast.LENGTH_LONG).show();
               return;
            }
            String lon = editLongitude.getText().toString().trim();
            float longitude = 0;
            try { longitude = Float.parseFloat(lon); } catch (Exception e) { lon = ""; }
            if (lon.trim().isEmpty())
            {
               Toast.makeText(MainActivity.this, "Invalid longitude", Toast.LENGTH_LONG).show();
               return;
            }
            float bottomLatitude = Float.NaN, bottomLongitude = Float.NaN;
            String lat2 = editBottomLatitude.getText().toString().trim();
            if ( (! lat2.isEmpty()) && (! lat2.equals(lat)) )
               try { bottomLatitude = Float.parseFloat(lat2); } catch (Exception e) { bottomLatitude = Float.NaN; }
            String lon2 = editBottomLongitude.getText().toString().trim();
            if ( (! lon2.isEmpty()) && (! lon2.equals(lon)) )
               try { bottomLongitude = Float.parseFloat(lon2); } catch (Exception e) { bottomLongitude = Float.NaN; }

            Anything token = new Anything();
            StringBuilder errbuf = new StringBuilder();
            final MapRequestorCallback callback = new MapRequestorCallback();
            if (api.equals("MapQuest"))
            {
               if (key.trim().isEmpty())
                  key = "VBtbjNHKuiJfvZNcAKbpcZAWAIAslZCX";
               if (mapQuestRequestor == null)
                   mapQuestRequestor = new MapQuestRequestor(activeObject);
               POI[] pois = new POI[2];
               pois[0] = new POI(-34.00931f, 25.6685f, "c");
               pois[1] = new POI(-34.00941f, 25.6655f, "1", POI.POIColor.green, POI.POISize.def, -20, -20);
               if ( (Float.isNaN(bottomLatitude)) || (Float.isNaN(bottomLongitude)) )
                  mapQuestRequestor.zoom(key, latitude, longitude, zoom, 1, MapQuestRequestor.MapType.map,
                                         MapQuestRequestor.MapImageType.png, 300, 300, pois, null, 1, TimeUnit.MINUTES,
                                         1, TimeUnit.MINUTES, callback, token, null, errbuf);
               else
                  mapQuestRequestor.box(key, latitude, longitude, bottomLatitude, bottomLongitude, 1,
                                        MapQuestRequestor.MapType.map, MapQuestRequestor.MapImageType.png, 300, 300,
                                        pois, null, 1, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, callback, token, null,
                                        errbuf);
            } else if (api.equals("Google"))
            {
               if (key.trim().isEmpty())
                  key = "AIzaSyDBocUN5N4JcQSCJpkXCfNt38IzPDaTrr4";
               if (googleMapRequestor == null)
                  googleMapRequestor = new GoogleMapRequestor(activeObject);
               POI[] pois = new POI[2];
               pois[0] = new POI(-34.00931f, 25.6685f, "c");
               pois[0].setColor(POI.POIColor.blue);
               pois[1] = new POI(-34.00941f, 25.6655f, "1");
               pois[1].setUserDefinedColor("0xFFFF00");
               if ( (Float.isNaN(bottomLatitude)) || (Float.isNaN(bottomLongitude)) )
                  googleMapRequestor.zoom(key, latitude, longitude, zoom, 1, GoogleMapRequestor.MapType.terrain,
                                          GoogleMapRequestor.MapImageType.png, 300, 300, pois, null, 1, TimeUnit.MINUTES,
                                          1, TimeUnit.MINUTES, callback, token, null, errbuf);
               else
                  googleMapRequestor.box(key, latitude, longitude, bottomLatitude, bottomLongitude, 1,
                                         GoogleMapRequestor.MapType.terrain, GoogleMapRequestor.MapImageType.png, 300, 300,
                                         pois, null, 1, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, callback, token, null,
                                         errbuf);
            }
         }
      });
   }

   class MapRequestorCallback implements IMapRequestorCallback
   //=========================================================
   {

      @Override
      public void onMapBitmap(Anything token, int code, final byte[] imageData, MIME_TYPES mimeType)
      {
         Log.i("uri", token.getAsString("url", ""));
         runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageData, 0, imageData.length));
            }
         });
      }

      @Override public void onMapFile(Anything token, int code, File f) { }

      @Override
      public void onError(Anything token, int code, final CharSequence message, Throwable exception)
      //---------------------------------------------------------------------------------------------
      {
         runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
         });
      }
   }
}
