package to.augmented.reality.android.ardb.http.maps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.Cache;
import to.augmented.reality.android.ardb.http.HTTP_METHOD;
import to.augmented.reality.android.ardb.http.HttpRequestor;
import to.augmented.reality.android.ardb.http.IHttpRequestorCallback;
import to.augmented.reality.android.ardb.http.MIME_TYPES;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

abstract public class MapRequestor implements IMapRequestor
//=========================================================
{
   static final private String TAG = MapRequestor.class.getName();

   abstract protected String tag();

   abstract protected Uri url(String key, Anything anyParameters, boolean isAutoLabel);

   abstract protected Anything onPrepareParameters(Anything anything, Float upperLeftlatitude, Float upperLeftlongitude,
                                                   Float lowerRightLatitude, Float lowerRightLongitude, Integer density,
                                                   IMapType type, Integer imageWidth, Integer imageHeight,
                                                   IMapImageType imageType, POI[] pois);

   abstract protected Anything onPrepareParameters(Anything anything, float latitude, float longitude, Integer zoom,
                                                   Integer density, IMapType type, Integer imageWidth,
                                                   Integer imageHeight, IMapImageType imageType, POI[] pois);

   abstract protected MIME_TYPES getMimeType(IMapImageType imageType, StringBuilder extb);

   abstract protected IMapImageType typeFromFile(IMapImageType imageType, File imageFile);

   protected String userAgent;

   final HttpRequestor requestor;

   Cache cacheInfo;
   @Override public void setHttpCache(Cache httpCache) { cacheInfo = httpCache; }

   public MapRequestor(ActiveObject activeObject) { this(activeObject, "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0", null); }

   public MapRequestor(ActiveObject activeObject, String userAgent) { this(activeObject, userAgent, null); }

   /**
    * @param activeObject The Active Object instance to use for multithreading.
    * @param userAgent User agent to send in request header (defaults to
    *                  'Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0' if <i>userAgent</i>
    *                  is empty or null.
    * @param cacheInfo A Cache instance specifying caching parameters or null for no caching. Use the same
    *                  Cache instance (or at least the same information in the Cache) when executing multiple
    *                  calls.
    */
   public MapRequestor(ActiveObject activeObject, String userAgent, Cache cacheInfo)
   //--------------------------------------------------------------
   {
      requestor = new HttpRequestor(activeObject);
      this.userAgent = userAgent;
      this.cacheInfo = cacheInfo;
   }

   /**
    * Asynchronous request method.
    * @param key Your MapQuest API key See <a href="http://developer.mapquest.com/">http://developer.mapquest.com/</a>
    * @param connectionTimeout Connection timeout
    * @param connectionTimeoutUnit Time unit for <i>connectionTimeout</i>
    * @param readTimeout Read time out.
    * @param readTimeoutUnit Time unit for <i>readTimeout</i>
    * @param callback The asynchronous callback to invoke on completion or when an error occurs.
    * @param token A token which can be used to identify the request when the callback is invoked.

    * @param mustAbort Setting <i>mustAbort</i> to false in another thread can be used to abort the request. Ignored if
    *                  null
    * @param errbuf If the request fails before the HTTP request is invoked then errbuf should contain a descriptive
    *               error message
    * @return A <i>Future</i> for the concurrently executing request (or completed request if concurrency is not
    * specified for the requestor) or <i>null</i> if a request setup error occurred
    */
   @Override
   public Future<?> zoom(String key, float centreLatitude, float centreLongitude, int zoom, int scale, IMapType type,
                         IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois, File imageFile,
                         long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                         TimeUnit readTimeoutUnit, final IMapRequestorCallback callback, Anything token,
                         AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
      {
         callback.onError(token, - 1, "MapRequestor.zoom requires imageWidth and imageHeight to be > 0", null);
         return null;
      }
      Integer density = (scale > 0) ? scale : null;
      imageType = typeFromFile(imageType, imageFile);
      Anything parameters = onPrepareParameters(null, centreLatitude, centreLongitude, zoom, density, type, imageWidth,
                                                imageHeight, imageType, pois);
      return asyncGetMap(key, parameters, imageType, connectionTimeout, connectionTimeoutUnit,
                         readTimeout, readTimeoutUnit, false, imageFile, mustAbort, token, callback, errbuf);
   }

   @Override
   public Bitmap zoom(String key, float centreLatitude, float centreLongitude, int zoom, int scale, IMapType type,
                      IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois,
                      long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                      TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
         throw new RuntimeException("MapRequestor.zoom requires imageWidth and imageHeight to be > 0");
      Integer density = (scale > 0) ? scale : null;
      Anything parameters = onPrepareParameters(null, centreLatitude, centreLongitude, zoom, density, type, imageWidth,
                                                imageHeight, imageType, pois);
      URI uri = makeURI(key, parameters, true, errbuf);
      if (uri == null)
         return null;
      StringBuilder extb = new StringBuilder();
      MIME_TYPES mimeType = getMimeType(imageType, extb);
      final String ext = extb.toString();
      ByteArrayOutputStream outputTo = new ByteArrayOutputStream();
      int code = requestor.request(HTTP_METHOD.GET, uri, userAgent, mimeType, null, parameters, connectionTimeout,
                                   connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo, mustAbort,
                                   errbuf);
      if ((code / 100) == 2)
      {
         byte[] bytes = outputTo.toByteArray();
         return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      }
      return null;
   }

   @Override
   public File zoom(String key, float centreLatitude, float centreLongitude, int zoom, int scale, IMapType type,
                    IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois, File imageFile,
                    long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                    TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      if (imageFile == null)
         return null;
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
         throw new RuntimeException("MapRequestor.zoom requires imageWidth and imageHeight to be > 0");
      Integer density = (scale > 0) ? scale : null;
      imageType = typeFromFile(imageType, imageFile);
      Anything parameters = onPrepareParameters(null, centreLatitude, centreLongitude, zoom, density, type, imageWidth,
                                                imageHeight, imageType, pois);
      URI uri = makeURI(key, parameters, true, errbuf);
      if (uri == null)
         return null;
      StringBuilder extb = new StringBuilder();
      MIME_TYPES mimeType = getMimeType(imageType, extb);
      final String ext = extb.toString();
      BufferedOutputStream outputTo;
      try
      {
         outputTo = new BufferedOutputStream(new FileOutputStream(imageFile), 32768);
      }
      catch (Exception e)
      {
         Log.e(TAG, "Creating " + imageFile, e);
         return null;
      }

      int code = requestor.request(HTTP_METHOD.GET, uri, userAgent, mimeType, null, parameters, connectionTimeout,
                                   connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo, mustAbort,
                                   errbuf);
      if ((code / 100) == 2)
      {
         try { outputTo.close(); } catch (Exception e) {}
         return imageFile;
      }
      return null;
   }

   @Override
   public Future<?> box(String key, float topLeftLatitude, float topLeftLongitude, float bottomRightLatitude,
                        float bottomRightLongitude, int scale, IMapType type, IMapImageType imageType,
                        int imageWidth, int imageHeight, POI[] pois, File imageFile,
                        long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                        TimeUnit readTimeoutUnit, final IMapRequestorCallback callback, Anything token,
                        AtomicBoolean mustAbort, StringBuilder errbuf)
   //-------------------------------------------------------------------------------------------------------
   {
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
      {
         callback.onError(token, - 1, "MapRequestor.box requires imageWidth and imageHeight to be > 0", null);
         return null;
      }
      Integer density = (scale > 0) ? scale : null;
      imageType = typeFromFile(imageType, imageFile);
      Anything parameters = onPrepareParameters(null, topLeftLatitude, topLeftLongitude, bottomRightLatitude,
                                                bottomRightLongitude, density, type, imageWidth,
                                                imageHeight, imageType, pois);
      return asyncGetMap(key, parameters, imageType, connectionTimeout, connectionTimeoutUnit,
                         readTimeout, readTimeoutUnit, false, imageFile, mustAbort, token, callback, errbuf);
   }

   @Override
   public Bitmap box(String key, float topLeftLatitude, float topLeftLongitude, float bottomRightLatitude,
                     float bottomRightLongitude, int scale, IMapType type,
                     IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois,
                     long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                     TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
         throw new RuntimeException("MapRequestor.box requires imageWidth and imageHeight to be > 0");
      Integer density = (scale > 0) ? scale : null;
      Anything parameters = onPrepareParameters(null, topLeftLatitude, topLeftLongitude, bottomRightLatitude,
                                                bottomRightLongitude, density, type, imageWidth,
                                                imageHeight, imageType, pois);
      URI uri = makeURI(key, parameters, false, errbuf);
      if (uri == null)
         return null;
      StringBuilder extb = new StringBuilder();
      MIME_TYPES mimeType = getMimeType(imageType, extb);
      final String ext = extb.toString();
      ByteArrayOutputStream outputTo = new ByteArrayOutputStream();
      int code = requestor.request(HTTP_METHOD.GET, uri, userAgent, mimeType, null, parameters, connectionTimeout,
                                   connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo, mustAbort,
                                   errbuf);
      if ((code / 100) == 2)
      {
         byte[] bytes = outputTo.toByteArray();
         return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      }
      return null;
   }

   @Override
   public File box(String key, float topLeftLatitude, float topLeftLongitude, float bottomRightLatitude,
                   float bottomRightLongitude, int scale, IMapType type,
                   IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois, File imageFile,
                   long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                   TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      if (imageFile == null)
         return null;
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
         throw new RuntimeException("MapRequestor.box requires imageWidth and imageHeight to be > 0");
      Integer density = (scale > 0) ? scale : null;
      imageType = typeFromFile(imageType, imageFile);
      Anything parameters = onPrepareParameters(null, topLeftLatitude, topLeftLongitude, bottomRightLatitude,
                                                bottomRightLongitude, density, type, imageWidth,
                                                imageHeight, imageType, pois);
      URI uri = makeURI(key, parameters, false, errbuf);
      if (uri == null)
         return null;
      StringBuilder extb = new StringBuilder();
      MIME_TYPES mimeType = getMimeType(imageType, extb);
      final String ext = extb.toString();
      BufferedOutputStream outputTo;
      try
      {
         outputTo = new BufferedOutputStream(new FileOutputStream(imageFile), 32768);
      }
      catch (Exception e)
      {
         Log.e(TAG, "Creating " + imageFile, e);
         return null;
      }

      int code = requestor.request(HTTP_METHOD.GET, uri, userAgent, mimeType, null, parameters, connectionTimeout,
                                   connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo, mustAbort,
                                   errbuf);
      if ((code / 100) == 2)
      {
         try { outputTo.close(); } catch (Exception e) {}
         return imageFile;
      }
      return null;
   }

   @Override
   public Future<?> pois(String key, int scale, IMapType type, IMapImageType imageType, int imageWidth, int imageHeight,
                         POI[] pois, File imageFile, long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                         TimeUnit readTimeoutUnit, IMapRequestorCallback callback, Anything token,
                         AtomicBoolean mustAbort, StringBuilder errbuf)
   //-------------------------------------------------------------------------------------------------------------------
   {
      imageType = typeFromFile(imageType, imageFile);
      if ( (pois == null) || (pois.length == 0) )
      {
         callback.onError(token, - 1, "MapRequestor.pois requires pois parameter to be not null and not empty", null);
         return null;
      }
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
      {
         callback.onError(token, - 1, "MapRequestor.pois requires imageWidth and imageHeight to be > 0", null);
         return null;
      }
      Integer density = (scale > 0) ? scale : null;
      Anything parameters = onPrepareParameters(null, null, null, null, null, density, type, imageWidth, imageHeight,
                                                imageType, pois);
      return asyncGetMap(key, parameters, imageType, connectionTimeout, connectionTimeoutUnit,
                         readTimeout, readTimeoutUnit, false, imageFile, mustAbort, token, callback, errbuf);
   }

   @Override
   public Bitmap pois(String key, int scale, IMapType type, IMapImageType imageType, int imageWidth, int imageHeight,
                      POI[] pois, long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                      TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf)
   //----------------------------------------------------------------------------------------------------------------
   {
      if ( (pois == null) || (pois.length == 0) )
         throw new RuntimeException("MapRequestor.pois requires pois parameter to be not null and not empty");
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
         throw new RuntimeException("MapRequestor.pois requires imageWidth and imageHeight to be > 0");
      Integer density = (scale > 0) ? scale : null;
      Anything parameters = onPrepareParameters(null, null, null, null, null, density, type, imageWidth, imageHeight,
                                                imageType, pois);
      URI uri = makeURI(key, parameters, false, errbuf);
      if (uri == null)
         return null;
      StringBuilder extb = new StringBuilder();
      MIME_TYPES mimeType = getMimeType(imageType, extb);
      final String ext = extb.toString();
      ByteArrayOutputStream outputTo = new ByteArrayOutputStream();
      int code = requestor.request(HTTP_METHOD.GET, uri, userAgent, mimeType, null, parameters, connectionTimeout,
                                   connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo, mustAbort,
                                   errbuf);
      if ((code / 100) == 2)
      {
         byte[] bytes = outputTo.toByteArray();
         return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      }
      return null;
   }

   @Override
   public File pois(String key, int scale, IMapType type, IMapImageType imageType, int imageWidth, int imageHeight,
                    POI[] pois, File imageFile, long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                    TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf)
   //----------------------------------------------------------------------------------------------------------------
   {
      if (imageFile == null)
         return null;
      imageType = typeFromFile(imageType, imageFile);
      if ( (pois == null) || (pois.length == 0) )
         throw new RuntimeException("MapRequestor.pois requires pois parameter to be not null and not empty");
      if ( (imageWidth <= 0) || (imageHeight <= 0) )
         throw new RuntimeException("MapRequestor.pois requires imageWidth and imageHeight to be > 0");
      Integer density = (scale > 0) ? scale : null;
      Anything parameters = onPrepareParameters(null, null, null, null, null, density, type, imageWidth, imageHeight,
                                                imageType, pois);
      URI uri = makeURI(key, parameters, false, errbuf);
      if (uri == null)
         return null;
      StringBuilder extb = new StringBuilder();
      MIME_TYPES mimeType = getMimeType(imageType, extb);
      final String ext = extb.toString();
      BufferedOutputStream outputTo;
      try
      {
         outputTo = new BufferedOutputStream(new FileOutputStream(imageFile), 32768);
      }
      catch (Exception e)
      {
         Log.e(TAG, "Creating " + imageFile, e);
         return null;
      }

      int code = requestor.request(HTTP_METHOD.GET, uri, userAgent, mimeType, null, parameters, connectionTimeout,
                                   connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo, mustAbort,
                                   errbuf);
      if ((code / 100) == 2)
      {
         try { outputTo.close(); } catch (Exception e) {}
         return imageFile;
      }
      return null;
   }


   protected Future<?> asyncGetMap(String key, Anything parameters, IMapImageType imageType, long connectionTimeout,
                                   TimeUnit connectionTimeoutUnit, long readTimeout, TimeUnit readTimeoutUnit,
                                   boolean isAutoLabel, final File imageFile, AtomicBoolean mustAbort, Anything token,
                                   final IMapRequestorCallback callback, StringBuilder errbuf)
   //--------------------------------------------------------------------------------------------------------------
   {
      if (errbuf == null)
         errbuf = new StringBuilder();
      URI uri = makeURI(key, parameters, isAutoLabel, errbuf);
      if (uri == null)
      {
         callback.onError(token, -1, errbuf, null);
         return null;
      }
      if (token != null)
         token.put("url", uri.toString());
      StringBuilder extb = new StringBuilder();
      MIME_TYPES mimeType = getMimeType(imageType, extb);
      final String ext = extb.toString();
      final OutputStream outputTo;
      if (imageFile == null)
         outputTo = new ByteArrayOutputStream();
      else
      {
         try
         {
            outputTo = new BufferedOutputStream(new FileOutputStream(imageFile), 32768);
         }
         catch (Exception e)
         {
            callback.onError(token, -1, "Error creating file " + imageFile, e);
            return null;
         }
      }
      final HttpCallback httpCallback = new HttpCallback(callback, outputTo, imageFile, mimeType);
      return requestor.request(HTTP_METHOD.GET, uri, userAgent, mimeType, null, parameters, connectionTimeout,
                               connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo,
                               httpCallback, token, mustAbort, errbuf);
   }

   private URI makeURI(String key, Anything parameters, boolean isAutoLabel, StringBuilder errbuf)
   //---------------------------------------------------------------------------------------------
   {
      Uri auri = null;
      URI uri;
      try
      {
         auri = url(key, parameters, isAutoLabel);
         return new URI(auri.toString());
      }
      catch (Exception e)
      {
         if (errbuf != null)
         {
            errbuf.append("Invalid URL ").append(((auri == null) ? "" : auri));
            Log.e(tag(), errbuf.toString(), e);
         }
         return null;
      }
   }

   class HttpCallback implements IHttpRequestorCallback
   //==================================================
   {
      final IMapRequestorCallback callback;

      final OutputStream outputTo;

      final File f;

      final MIME_TYPES mimeType;

      public HttpCallback(IMapRequestorCallback callback, OutputStream outputTo, File file, MIME_TYPES mimeType)
      //--------------------------------------------------------------------------------------------------------
      {
         this.outputTo = outputTo;
         this.callback = callback;
         f = file;
         this.mimeType = mimeType;
      }

      @Override
      public void onResponse(Anything token, int code)
      //----------------------------------------------
      {
         if (outputTo instanceof ByteArrayOutputStream)
         {
            ByteArrayOutputStream baos = (ByteArrayOutputStream) outputTo;
            byte[] bytes = baos.toByteArray();
            //Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            callback.onMapBitmap(token, 0, bytes, mimeType);
         } else
         {
            try
            {
               outputTo.close();
            }
            catch (Exception _e)
            {
            }
            callback.onMapFile(token, 0, f);
         }
      }

      @Override
      public void onError(Anything token, int code, CharSequence message,
                          Throwable exception)
      //----------------------------------------------------------------
      {
         callback.onError(token, code, message, exception);
      }
   }

//      public Future<?> radius(HTTP_METHOD method, String key, String userAgent, float centreLatitude, float centreLongitude,
//                              int zoom, MapType type, MapImageType mapType, int imageWidth, int imageHeight,
//                              MapPOI[] pois, long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
//                              TimeUnit readTimeoutUnit, Cache cacheInfo, OutputStream outputTo,
//                              ICursorQueryCallback callback, Anything token, AtomicBoolean mustAbort, StringBuilder errbuf)
//      //-----------------------------------------------------------------------------------------------------------------
//      {
//         return null;
//      }
//
}
