package to.augmented.reality.android.ardb.sourcefacade.maps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;
import to.augmented.reality.android.ardb.http.Cache;
import to.augmented.reality.android.ardb.http.MIME_TYPES;
import to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor;
import to.augmented.reality.android.ardb.http.maps.IMapRequestor;
import to.augmented.reality.android.ardb.http.maps.IMapRequestorCallback;
import to.augmented.reality.android.ardb.http.maps.POI;
import to.augmented.reality.android.ardb.http.maps.MapQuestRequestor;
import to.augmented.reality.android.ardb.sourcefacade.ColumnContext;
import to.augmented.reality.android.ardb.sourcefacade.DataPoint;
import to.augmented.reality.android.ardb.sourcefacade.DataType;
import to.augmented.reality.android.ardb.sourcefacade.IAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialQueryResult;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.ImageCursor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapsDataSource implements ISpatialSource
//===================================================
{
   static final private String TAG = MapsDataSource.class.getName();

   protected final String sourceName;
   protected final Context context;
   protected final IAnnotationProcessor annotationProcessor;
   protected final Object annotatedInstance;
   protected final IMapRequestor requestor;
   protected Cache httpCache = null;
   protected final AtomicBoolean mustAbort = new AtomicBoolean(false);
   protected long connectionTimeout = 60, readTimeout = 60;
   protected TimeUnit connectionTimeoutUnit = TimeUnit.SECONDS, readTimeoutUnit = TimeUnit.SECONDS;
   protected SQLiteDatabase localDatabase = null;

   private String key;
   private int imageWidth, imageHeight;
   private String lastQuery = "";

   public void setImageSize(int width, int height) { imageWidth = width; imageHeight = height; }
   public int[] getImageSize() { return new int[] { imageWidth, imageHeight}; }

   public void setKey(String key) { this.key = key; }
   public String getKey() { return key; }

   private File outputFile = null;
   public void setOutputFile(File f) { this.outputFile = f; }
   public File getOutputFile() { return outputFile; }

   private int zoom = -1;
   public void setZoom(int zoom) { this.zoom = zoom; }
   public int getZoom() { return zoom; }

   private int density = 1;
   public void setDensity(int scale) { this.density = scale; }
   public int getDensity() { return density; }

   IMapRequestor.IMapType mapType = null;
   public void setMapType(IMapRequestor.IMapType type) { mapType = type; }
   public void setGoogleMapType(GoogleMapRequestor.MapType mapType) { this.mapType = mapType; }
   public void setMapquestMapType(MapQuestRequestor.MapType mapType) { this.mapType = mapType; }

   IMapRequestor.IMapImageType imageType;
   public void setMapImageType(IMapRequestor.IMapImageType type) { imageType = type; }
   public void setGoogleMapImageType(GoogleMapRequestor.MapImageType type) { imageType = type; }
   public void setMapquestMapImageType(MapQuestRequestor.MapImageType type) { imageType = type; }

   final private List<POI> POIs = new ArrayList<>();
   public void addPOI(POI poi) { POIs.add(poi); }
   public void removePOI(POI poi) { POIs.remove(poi); }
   public POI[] getPOIS() { return POIs.toArray(new POI[POIs.size()]); }
   protected void insertPOI(int pos, POI poi)
   //-------------------------------------------
   {
      if (pos >= POIs.size())
         POIs.add(poi);
      else
         POIs.add(pos, poi);
   }

   protected IMapRequestor onGetRequestor(ActiveObject ao, MapSource source)
   //-----------------------------------------------------------------------
   {
      if (source == MapSource.GOOGLE)
         return new GoogleMapRequestor(ao);
      else if (source == MapSource.MAPQUEST)
            return new MapQuestRequestor(ao);
      throw new RuntimeException("Unknown map source: " + source);
   }

   public MapsDataSource(String sourceName, String apiKey, MapSource source, Context context, ActiveObject ao)
   //--------------------------------------------------------------------------------------------------------
   {
      this(sourceName, apiKey, source, context, ao, null, null);
   }


   public MapsDataSource(String sourceName, String apiKey, MapSource source, Context context, ActiveObject ao,
                         IAnnotationProcessor processor, Object annotationInstance)
   //------------------------------------------------------------------------------------------
   {
      this.sourceName = sourceName;
      this.key = apiKey;
      this.context = context;
      this.annotationProcessor = processor;
      this.annotatedInstance = annotationInstance;
      requestor = onGetRequestor(ao, source);
   }

   public void setHttpCache(File cacheDir, boolean isOverideNoCache, boolean isOverideCacheAge)
         throws FileNotFoundException
   //------------------------------------------------------------------------------------------
   {
      httpCache = new Cache(5, TimeUnit.MINUTES, cacheDir,isOverideNoCache, isOverideCacheAge);
      requestor.setHttpCache(httpCache);
   }

   public void setHttpCache(Cache cache) { httpCache = cache; requestor.setHttpCache(httpCache); }

   public void setConnectionTimeout(long connectionTimeout, TimeUnit connectionTimeoutUnit)
   //--------------------------------------------------------------------------------------
   {
      this.connectionTimeout = connectionTimeout;
      this.connectionTimeoutUnit = connectionTimeoutUnit;
   }

   public void setReadTimeout(long readTimeout, TimeUnit readTimeoutUnit)
   //--------------------------------------------------------------------------------------
   {
      this.readTimeout = readTimeout;
      this.readTimeoutUnit = readTimeoutUnit;
   }

   @Override public String getName() { return sourceName; }

   @Override
   public void setLocalDatabase(SQLiteDatabase localDatabase) { this.localDatabase = localDatabase; }

   @Override
   public void setParameters(Map<String, Object> parameters) {  }

   @Override
   public void setProjectionTypes(DataType[] projectionTypes) { }

   @Override
   public Future<?> boundingBox(double centerLatitude, double centerLongitude, double width, double height,
                                ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token,
                                ISpatialQueryResult callback)
   //-------------------------------------------------------------------------------------------------------
   {
      POIs.clear();
      IMapRequestorCallback  mapCallback = new MapRequestorCallback(token, callback, callbackType);
      StringBuilder errbuf = new StringBuilder();
      POIs.clear();
      if (annotationProcessor != null)
      {
         Map<String, Object> parameters = annotationProcessor.processParameters(annotatedInstance, null);
         outputFile = (File) parameters.get("OutputFile");
         imageWidth = (Integer) parameters.get("ImageWidth");
         imageHeight = (Integer) parameters.get("ImageHeight");
         density = (Integer) parameters.get("ImageDensity");
         imageType = (IMapRequestor.IMapImageType) parameters.get("Format");
         mapType = (IMapRequestor.IMapType) parameters.get("MapType");
         List L = (List) parameters.get("POIs");
         if (L != null)
            POIs.addAll(L);
      }
      lastQuery = String.format("boundingBox(%.6f, %.6f, %.6f, %.6f", centerLatitude, centerLongitude, width, height);
      return requestor.box(key, (float) (centerLatitude - height / 2), (float) (centerLongitude - width / 2),
                           (float) (centerLatitude + height / 2), (float) (centerLongitude + width / 2), density,
                           mapType,
                           imageType, imageWidth, imageHeight, getPOIS(), outputFile, connectionTimeout,
                           connectionTimeoutUnit, readTimeout, readTimeoutUnit, mapCallback, token, mustAbort, errbuf);
   }

   final double R = 6378137; // Earth's radius

   @Override
   public Future<?> radius(double centerLatitude, double centerLongitude, double radius,
                           ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback)
   //------------------------------------------------------------------------------------------------------------------
   {
      POIs.clear();
      if (annotationProcessor != null)
      {
         Map<String, Object> parameters = annotationProcessor.processParameters(annotatedInstance, null);
         outputFile = (File) parameters.get("OutputFile");
         imageWidth = (Integer) parameters.get("ImageWidth");
         imageHeight = (Integer) parameters.get("ImageHeight");
         density = (Integer) parameters.get("ImageDensity");
         imageType = (IMapRequestor.IMapImageType) parameters.get("Format");
         mapType = (IMapRequestor.IMapType) parameters.get("MapType");
         List L = (List) parameters.get("POIs");
         if (L != null)
            POIs.addAll(L);
      }
      final double fortyFive = 0.785398163;
      int i = 0;
      for (double theta=0; theta < 2.0*Math.PI; theta += fortyFive)
      {
         double de = radius * Math.cos(theta);
         double dn = radius * Math.sin(theta);
         double dLat = dn/R;
         double dLon = de/(R*Math.cos(Math.PI * centerLatitude / 180));
         float latitude = (float) (centerLatitude + dLat * 180/Math.PI);
         float longitude = (float) (centerLongitude + dLon * 180/Math.PI);
//         Log.i("RADIUS", String.format("%.6f, %.6f", latitude, longitude));
         insertPOI(i++, new POI(latitude, longitude, false, null));
      }
      StringBuilder errbuf = new StringBuilder();
      IMapRequestorCallback  mapCallback = new MapRequestorCallback(token, callback, callbackType);
      lastQuery = String.format("%.6f, %.6f, %.6f", centerLatitude, centerLongitude, radius);
      return requestor.pois(key, density, mapType, imageType, imageWidth, imageHeight, getPOIS(), outputFile,
                            connectionTimeout, connectionTimeoutUnit, readTimeout, readTimeoutUnit, mapCallback, token,
                            mustAbort, errbuf);
   }

   @Override
   public Future<?> spatialQuery(double latitude, double longitude, ImmutableAnything extraParameters,
                                 ISpatialQueryResult.CALLBACK_TYPE callbackType,
                                 Anything token, ISpatialQueryResult callback)
   //-------------------------------------------------------------------------------------------------
   {
      POIs.clear();
      Integer zoom = null;
      if (annotationProcessor != null)
      {
         Map<String, Object> parameters = annotationProcessor.processParameters(annotatedInstance, null);
         outputFile = (File) parameters.get("OutputFile");
         imageWidth = (Integer) parameters.get("ImageWidth");
         imageHeight = (Integer) parameters.get("ImageHeight");
         density = (Integer) parameters.get("ImageDensity");
         imageType = (IMapRequestor.IMapImageType) parameters.get("Format");
         mapType = (IMapRequestor.IMapType) parameters.get("MapType");
         zoom = (Integer) parameters.get("Zoom");
         List L = (List) parameters.get("POIs");
         if (L != null)
            POIs.addAll(L);
      }
      if ( ((zoom == null) || (zoom < 0)) && (extraParameters != null) )
      {
         zoom = extraParameters.getImmutable("zoom").asInt(- 1);
         if (zoom < 0)
            zoom = this.zoom;
      }
      if (zoom < 0)
      {
         String errm = MapsDataSource.class.getName() + ".spatialQuery requires 'zoom' parameter be set for the data " +
                       "source as an annotation or in extraParameters";
         callback.onError(sourceName, token, errm, null);
         return null;
      }

      IMapRequestorCallback  mapCallback = new MapRequestorCallback(token, callback, callbackType);
      StringBuilder errbuf = new StringBuilder();
      lastQuery = String.format("%.6f, %.6f, zoom = %d", latitude, longitude, zoom);
      return requestor.zoom(key, (float) latitude, (float) longitude, zoom, density, mapType, imageType,
                            imageWidth, imageHeight, getPOIS(), outputFile, connectionTimeout,
                            connectionTimeoutUnit,
                            readTimeout, readTimeoutUnit, mapCallback, token, mustAbort, errbuf);
   }

   @Override public String getLastQuery() { return lastQuery; }

   static public IMapRequestor.IMapType getMapType(MapFormat mapFormat, MapSource api)
   //---------------------------------------------------------------------------------
   {
      IMapRequestor.IMapType mapType = null;
      switch (mapFormat)
      {
         case MAP:
            switch (api)
            {
               case GOOGLE:   mapType = GoogleMapRequestor.MapType.roadmap; break;
               case MAPQUEST: mapType = MapQuestRequestor.MapType.map; break;
               default:       throw new RuntimeException("Unsupported api " + api);
            }
            break;
         case SATELLITE:
            switch (api)
            {
               case GOOGLE:   mapType = GoogleMapRequestor.MapType.satellite; break;
               case MAPQUEST: mapType = MapQuestRequestor.MapType.satellite; break;
               default:       throw new RuntimeException("Unsupported api " + api);
            }
            break;
         case HYBRID:
            switch (api)
            {
               case GOOGLE:   mapType = GoogleMapRequestor.MapType.hybrid; break;
               case MAPQUEST: mapType = MapQuestRequestor.MapType.hybrid; break;
               default:       throw new RuntimeException("Unsupported api " + api);
            }
            break;
         case TERRAIN:
            switch (api)
            {
               case GOOGLE:   mapType = GoogleMapRequestor.MapType.terrain; break;
               case MAPQUEST: mapType = MapQuestRequestor.MapType.map; Log.w(TAG, "Terrain map type not supported by MapQuest API"); break;
               default:       throw new RuntimeException("Unsupported api " + api);
            }
            break;


      }
      return mapType;
   }

   static public IMapRequestor.IMapImageType getImageFormat(ImageFormat imageFormat, MapSource api)
   //-----------------------------------------------------------------------------------------------
   {
      IMapRequestor.IMapImageType imageType = null;
      switch (imageFormat)
      {
         case PNG:
            switch (api)
            {
               case GOOGLE:   imageType = GoogleMapRequestor.MapImageType.png; break;
               case MAPQUEST: imageType = MapQuestRequestor.MapImageType.png; break;
               default:       throw new RuntimeException("Unsupported API");
            }
            break;
         case JPG:
            switch (api)
            {
               case GOOGLE:   imageType = GoogleMapRequestor.MapImageType.jpg; break;
               case MAPQUEST: imageType = MapQuestRequestor.MapImageType.jpg; break;
               default:       throw new RuntimeException("Unsupported API");
            }
            break;
         case GIF:
            switch (api)
            {
               case GOOGLE:   imageType = GoogleMapRequestor.MapImageType.gif; break;
               case MAPQUEST: imageType = MapQuestRequestor.MapImageType.gif; break;
               default:       throw new RuntimeException("Unsupported API");
            }
            break;
         case NON_PROGRESSIVE_JPG:
            switch (api)
            {
               case GOOGLE:   imageType = GoogleMapRequestor.MapImageType.jpg_baseline; break;
               case MAPQUEST:
                  imageType = MapQuestRequestor.MapImageType.jpg;
                  Log.w(TAG, "MapQuest does not support non progessive jpg images");
                  break;
               default:       throw new RuntimeException("Unsupported API");
            }
            break;
      }
      return imageType;
   }

   class MapRequestorCallback implements IMapRequestorCallback
   //=========================================================
   {
      private final Anything token;
      private final ISpatialQueryResult callback;
      private final ISpatialQueryResult.CALLBACK_TYPE callbackType;

      public MapRequestorCallback(Anything token, ISpatialQueryResult callback,
                                  ISpatialQueryResult.CALLBACK_TYPE callbackType)
      //--------------------------------------------------------------------------
      {
         this.token = token;
         this.callback = callback;
         this.callbackType = callbackType;
      }

      @Override
      public void onMapBitmap(Anything token, int code, byte[] imageData, MIME_TYPES mimeType)
      //-----------------------------------------------------------------------------------------
      {
         switch (callbackType)
         {
            case IMAGE:
               Bitmap bmp;
               try
               {
                  bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
               }
               catch (Exception e)
               {
                  bmp = null;
                  Log.e(TAG, "", e);
                  callback.onError(sourceName, token, "Error decoding image", e);
                  return;
               }
               callback.onDatasetStart(sourceName, token);
               callback.onImageAvailable(sourceName, token, bmp);
               callback.onDatasetEnd(sourceName, token);
               break;
            case ANNOTATED_OBJECT:
               if (annotationProcessor != null)
               {
                  try
                  {
                     annotationProcessor.processCursor(annotatedInstance, new ImageCursor("map", imageData),
                                                       new ColumnContext[] { new ColumnContext("map", "map") });
                     callback.onDatasetStart(sourceName, token);
                     callback.onAnnotationAvailable(sourceName, token, annotatedInstance);
                     callback.onDatasetEnd(sourceName, token);
                  }
                  catch (Exception e)
                  {
                     Log.e(TAG, "Exception processing image cursor", e);
                     callback.onError(sourceName, token, "Exception processing image cursor", e);
                  }
               }
               else
                  callback.onError(sourceName, token, "No annotation processor", null);
               break;
            case RAW_CURSOR:
               Cursor cursor = createCursor(imageData, null);
               if (cursor != null)
                  callback.onCursorAvailable(sourceName, token, cursor);
               break;
            case DATAPOINT:
               DataPoint dp = new DataPoint(DataType.BLOB);
               dp.set(imageData);
               callback.onDatasetStart(sourceName, token);
               callback.onDataPointAvailable(sourceName, token, dp);
               callback.onDatasetEnd(sourceName, token);
         }
         imageData = null;
      }

      @Override public void onMapFile(Anything token, int code, File f)
      //----------------------------------------------------------------
      {
         switch (callbackType)
         {
            case IMAGE:
               Bitmap bmp;
               try
               {
                  bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
               }
               catch (Exception e)
               {
                  bmp = null;
                  Log.e(TAG, "", e);
                  callback.onError(sourceName, token, "Error decoding image", e);
                  return;
               }
               callback.onImageAvailable(sourceName, token, bmp);
               break;
            case ANNOTATED_OBJECT:
               if (annotationProcessor != null)
               {
                  try
                  {
                     annotationProcessor.processCursor(annotatedInstance, new ImageCursor("map", f),
                                                       new ColumnContext[] { new ColumnContext("map", "map") });
                  }
                  catch (Exception e)
                  {
                     Log.e(TAG, "Exception processing image cursor", e);
                     callback.onError(sourceName, token, "Exception processing image cursor", e);
                  }
               }
               else
                  callback.onError(sourceName, token, "No annotation processor", null);
               break;
            case RAW_CURSOR:
               Cursor cursor = createCursor(null, f);
               if (cursor != null)
                  callback.onCursorAvailable(sourceName, token, cursor);
               break;
            case DATAPOINT:
               DataPoint dp = new DataPoint(DataType.BLOB);
               BufferedInputStream bis = null;
               try
               {
                  bis = new BufferedInputStream(new FileInputStream(f), 32768);
                  byte[] data = new byte[(int) f.length()];
                  bis.read(data);
                  dp.set(data);
               }
               catch (Exception _e)
               {
                  Log.e(TAG, "Image Datapoint error: " + f, _e);
                  callback.onError(sourceName, token, "Image Datapoint error: " + f, _e);
                  return;
               }
               finally
               {
                  if (bis != null)
                     try { bis.close(); } catch (Exception _e) {}
               }
               callback.onDataPointAvailable(sourceName, token, dp);
               break;
         }
      }

      @Override
      public void onError(Anything token, int code, final CharSequence message, Throwable e)
      //---------------------------------------------------------------------------------------------
      {
         callback.onError(sourceName, this.token, message, e);
      }

      private Cursor createCursor(byte[] imageData, File f)
      //---------------------------------------------------
      {
         if ( (imageData == null) && ( (f != null) && (f.exists()) ) )
         {
            long len = f.length();
            BufferedInputStream bis = null;
            try
            {
               bis = new BufferedInputStream(new FileInputStream(f), 32768);
               imageData = new byte[(int) len];
               bis.read(imageData);
            }
            catch (Exception e)
            {
               Log.e(TAG, "Error reading image file " + f, e);
               callback.onError(sourceName, token, "Image file load error: " + f, e);
               return null;
            }
            finally
            {
               if (bis != null)
                  try { bis.close(); } catch (Exception _e) {}
            }
         }

         if (localDatabase == null)
            return new ImageCursor("map", imageData);

         ContentValues databaseRowValues = new ContentValues();
         databaseRowValues.put("map", imageData);
         String action = "Creating local database table";
         try
         {
            localDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + sourceName +
                                        "(__id__ INTEGER PRIMARY KEY AUTOINCREMENT, map BLOB)");
            action = "Inserting map into local database table";
            long id = localDatabase.insert(sourceName, null, databaseRowValues);
            action = "Querying map from local database table";
            return localDatabase.query(sourceName, new String[] { "map"}, "__id__ = " + id, null, null, null, null);
         }
         catch (Exception e)
         {
            Log.e(TAG, action, e);
            return new ImageCursor("map", imageData);
         }
      }
   }
}
