package to.augmented.reality.android.ardb.http.maps;

import android.graphics.Bitmap;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.Cache;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IMapRequestor
//============================
{

   interface IMapType { }

   interface IMapImageType {}

   void setHttpCache(Cache httpCache);

   Future<?> zoom(String key, float centreLatitude, float centreLongitude, int zoom, int scale, IMapType type,
                  IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois, File imageFile,
                  long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                  TimeUnit readTimeoutUnit, IMapRequestorCallback callback, Anything token,
                  AtomicBoolean mustAbort, StringBuilder errbuf);

   Bitmap zoom(String key, float centreLatitude, float centreLongitude, int zoom, int scale, IMapType type,
               IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois,
               long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
               TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf);

   File zoom(String key, float centreLatitude, float centreLongitude, int zoom, int scale, IMapType type,
             IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois, File imageFile,
             long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
             TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf);

   Future<?> box(String key, float topLeftLatitude, float topLeftLongitude, float bottomRightLatitude,
                 float bottomRightLongitude, int scale, IMapType type, IMapImageType imageType,
                 int imageWidth, int imageHeight, POI[] pois, File imageFile,
                 long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                 TimeUnit readTimeoutUnit, IMapRequestorCallback callback, Anything token,
                 AtomicBoolean mustAbort, StringBuilder errbuf);

   Bitmap box(String key, float topLeftLatitude, float topLeftLongitude, float bottomRightLatitude,
              float bottomRightLongitude, int scale, IMapType type,
              IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois,
              long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
              TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf);

   File box(String key, float topLeftLatitude, float topLeftLongitude, float bottomRightLatitude,
            float bottomRightLongitude, int scale, IMapType type,
            IMapImageType imageType, int imageWidth, int imageHeight, POI[] pois, File imageFile,
            long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
            TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf);

   Future<?> pois(String key, int scale, IMapType type, IMapImageType imageType,
                  int imageWidth, int imageHeight, POI[] pois, File imageFile,
                  long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                  TimeUnit readTimeoutUnit, IMapRequestorCallback callback, Anything token,
                  AtomicBoolean mustAbort, StringBuilder errbuf);

   Bitmap pois(String key, int scale, IMapType type, IMapImageType imageType, int imageWidth, int imageHeight,
               POI[] pois, long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
               TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf);

   File pois(String key, int scale, IMapType type, IMapImageType imageType, int imageWidth, int imageHeight,
             POI[] pois, File imageFile, long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
             TimeUnit readTimeoutUnit, AtomicBoolean mustAbort, StringBuilder errbuf);

}
