package to.augmented.reality.android.ardb.sourcefacade.maps;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.http.maps.GoogleMapRequestor;
import to.augmented.reality.android.ardb.http.maps.IMapRequestor;
import to.augmented.reality.android.ardb.http.maps.MapQuestRequestor;
import to.augmented.reality.android.ardb.http.maps.POI;
import to.augmented.reality.android.ardb.sourcefacade.ColumnContext;
import to.augmented.reality.android.ardb.sourcefacade.IAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.AnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.BoundingBox;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Format;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageDensity;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageFile;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageHeight;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.ImageWidth;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Key;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapCircle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapOutputImage;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapType;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapsSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.POIs;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.Zoom;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MapsAnnotationProcessor extends AnnotationProcessor implements IAnnotationProcessor, Cloneable
//=========================================================================================================
{
   static final private String TAG = MapsAnnotationProcessor.class.getName();

   @Override protected String tag() { return TAG; }

   @Override public Object clone() throws CloneNotSupportedException { return super.clone(); }

   private ISpatialSource.SPATIAL_OPERATION operation;
   @Override public ISpatialSource.SPATIAL_OPERATION operation() { return operation; }

   double radius =-1;
   @Override public double circleRadius() { return radius; }

   double width =-1;
   @Override public double bboxWidth() { return width; }

   double height =-1;
   @Override public double bboxHeight() { return height; }

   private MapSource api = null;

   private int imageWidth =-1, imageHeight =-1;

   private Field imageField = null;

   private Handler imageViewHandler = null;

   @Override public Object[] userDefOpParameters() { return null; }

   public MapsAnnotationProcessor() {}

   public MapsAnnotationProcessor(Context context) { super(context); }

   @Override
   public ISpatialSource createAnnotated(String sourceName, Context context, ActiveObject activeObject, Object instance)
   //------------------------------------------------------------------------------------------------------------------
   {
      final Class<?> C = instance.getClass();
      MapsSource mapSourceAnno  = C.getAnnotation(MapsSource.class);
      if (mapSourceAnno == null)
         throw new RuntimeException("Maps source annotation class must be annotated with the MapsSource annotation");
      final String name = mapSourceAnno.name();
      api = mapSourceAnno.source();
      String key = mapSourceAnno.key();
      if ( (key == null) || (key.trim().isEmpty()) )
      {
         key = extractMethodOrField(C, instance, Key.class);
         if ( (key == null) || (key.trim().isEmpty()) )
         {
            String s = "Maps API key must be specified either in the @MapsSource annotation or using an @Key " +
                       "annotation on a method or field.";
            Log.e(TAG, s);
            throw new RuntimeException(s);
         }
      }
      final ImageFormat format = mapSourceAnno.format();
      imageWidth = mapSourceAnno.width();
      imageHeight = mapSourceAnno.height();
      final long connectionTimeout = mapSourceAnno.connectionTimeout();
      final TimeUnit connectionTimeoutUnit = mapSourceAnno.connectionTimeoutUnit();
      final long readTimeout = mapSourceAnno.readTimeout();
      final TimeUnit readTimeoutUnit = mapSourceAnno.readTimeoutUnit();
      MapsDataSource source = new MapsDataSource(name, key, api, context, activeObject, this, instance);
      source.setConnectionTimeout(connectionTimeout, connectionTimeoutUnit);
      source.setReadTimeout(readTimeout, readTimeoutUnit);
      source.setMapImageType(MapsDataSource.getImageFormat(format, api));
      MapCircle circleAnno = C.getAnnotation(MapCircle.class);
      if (circleAnno != null)
      {
         radius = circleAnno.radius();
         operation = ISpatialSource.SPATIAL_OPERATION.RADIUS;
      }
      else
      {
         BoundingBox boxAnno = C.getAnnotation(BoundingBox.class);
         if (boxAnno != null)
         {
            width = boxAnno.width();
            height = boxAnno.height();
            operation = ISpatialSource.SPATIAL_OPERATION.BOUNDING_BOX;
         }
         else
            operation = ISpatialSource.SPATIAL_OPERATION.USER_DEFINED;
      }

      return source;
   }

   @Override
   public Map<String, Object> processParameters(Object instance, Map<String, Object> parameters)
   //-------------------------------------------------------------------------------------------
   {
      Class<?> C = instance.getClass();
      if (parameters == null)
         parameters = new HashMap<>();
      Field[] fields = C.getDeclaredFields();
      imageField = null;
      for (Field field : fields)
      {
         field.setAccessible(true);
         String v = getAnnotatedField(field, ImageFile.class, false, instance, String.class, File.class);
         if (v != null)
         {
            File f = new File(v);
            File parentDir = f.getParentFile();
            if (parentDir == null)
               parentDir = new File(".");
            if (! parentDir.exists())
               parentDir.mkdirs();
            parentDir.setWritable(true);
            if (! f.canWrite())
               throw new RuntimeException("@OutputFile annotated variable " + field.getName() +
                                          " contains non-writable file " + f);
            parameters.put("OutputFile", new File(v));
         }

         v = getAnnotatedField(field, ImageDensity.class, true, instance, Number.class);
         if (v != null)
         {
            Integer density = parseInt(v);
            if (density == null)
            {
               Log.w(TAG, "Could not parse image density " + v + " as an integer. Defaulting to 1");
               density = 1;
            }
            parameters.put("ImageDensity", density);
         }

         v = getAnnotatedField(field, ImageHeight.class, true, instance, Number.class);
         if (v != null)
         {
            Integer height = parseInt(v);
            if (height == null)
               throw new RuntimeException("Could not parse image height " + v + " as an integer");
            imageHeight = height;
            parameters.put("ImageHeight", height);
         }

         v = getAnnotatedField(field, ImageWidth.class, true, instance, Number.class);
         if (v != null)
         {
            Integer width = parseInt(v);
            if (width == null)
               throw new RuntimeException("Could not parse image width " + v + " as an integer");
            imageWidth = width;
            parameters.put("ImageWidth", width);
         }

         v = getAnnotatedField(field, Zoom.class, true, instance, Number.class);
         if (v != null)
         {
            Integer zoom = parseInt(v);
            if (zoom == null)
               throw new RuntimeException("Could not parse zoom " + v + " as an integer");
            parameters.put("Zoom", zoom);
         }

         Annotation annoMapType = field.getAnnotation(MapType.class);
         if (annoMapType != null)
         {
            IMapRequestor.IMapType mapType = null;
            if (field.getType() == GoogleMapRequestor.MapType.class)
            {
               if (api != MapSource.GOOGLE)
                  throw new RuntimeException("GoogleMapRequestor.MapType can only be used where source = MapSource.GOOGLE");
               try { mapType = (IMapRequestor.IMapType) field.get(instance); } catch (Exception _e) { mapType = null; }
            }
            else if (field.getType() == MapQuestRequestor.MapType.class)
            {
               if (api != MapSource.MAPQUEST)
                  throw new RuntimeException("MapQuestRequestor.MapType can only be used where source = MapSource.MAPQUEST");
               try { mapType = (IMapRequestor.IMapType) field.get(instance); } catch (Exception _e) { mapType = null; }
            }
            else if (field.getType() == MapFormat.class)
            {
               MapFormat mapFormat;
               try { mapFormat = (MapFormat) field.get(instance); } catch (Exception _e) { mapFormat = null; }
               if (mapFormat != null)
                  mapType = MapsDataSource.getMapType(mapFormat, api);
            }
            if (mapType == null)
               mapType = MapsDataSource.getMapType(MapFormat.MAP, api);
            parameters.put("MapType", mapType);
         }

         Annotation annoMapImageType = field.getAnnotation(Format.class);
         if (annoMapImageType != null)
         {
            IMapRequestor.IMapImageType imageType = null;
            if (field.getType() == GoogleMapRequestor.MapImageType.class)
               try { imageType = (IMapRequestor.IMapImageType) field.get(instance); } catch (Exception _e) { imageType = null; }
            else if (field.getType() == MapQuestRequestor.MapImageType.class)
               try { imageType = (IMapRequestor.IMapImageType) field.get(instance); } catch (Exception _e) { imageType = null; }
            else if (field.getType() == ImageFormat.class)
            {
               ImageFormat imgFormat;
               try { imgFormat = (ImageFormat) field.get(instance); } catch (Exception _e) { imgFormat = null; }
               if (imgFormat != null)
                  imageType = MapsDataSource.getImageFormat(imgFormat, api);
            }
            if (imageType == null)
            {
               MapsSource mapSourceAnno  = C.getAnnotation(MapsSource.class);
               if (mapSourceAnno != null)
                  imageType = MapsDataSource.getImageFormat(mapSourceAnno.format(), api);
            }
            if (imageType == null)
               imageType = MapsDataSource.getImageFormat(ImageFormat.PNG, api);
            parameters.put("Format", imageType);
         }

         Annotation annoPois = field.getAnnotation(POIs.class);
         if (annoPois != null)
         {
            List<POI> pois = null;
            if (field.getType() == List.class)
               try { pois = (List<POI>) field.get(instance); } catch (Exception e) { Log.e(TAG, "", e); pois = null; }
            else if (field.getType() == POI[].class)
            {
               POI[] apoi = null;
               try { apoi = (POI[]) field.get(instance); } catch (Exception e) { Log.e(TAG, "", e); apoi = null; pois = null; }
               if (apoi != null)
               {
                  pois = new ArrayList<>(apoi.length);
                  for (POI poi : apoi)
                     pois.add(poi);
               }
            }
            if (pois == null)
               throw new RuntimeException("@POIs must annotate a List<POI> or POI[] type field");
            parameters.put("POIs", pois);
         }
         Annotation imageAnno = field.getAnnotation(MapOutputImage.class);
         if (imageAnno != null)
         {
            if (imageField != null)
               throw new RuntimeException("Only one @MapOutputImage field can be defined (" + imageField.getName() +
               " already defined when " + field.getName() + " encountered)");
            if ( (field.getType() != Bitmap.class) && (field.getType() != ImageView.class) )
               throw new RuntimeException("@MapOutputImage can only annotate Bitmap or ImageView fields");
            imageField = field;
         }
      }
      if (! parameters.containsKey("OutputFile"))
         parameters.put("OutputFile", null);
      if (! parameters.containsKey("ImageDensity"))
         parameters.put("ImageDensity", 1);
      if (! parameters.containsKey("Zoom"))
         parameters.put("Zoom", -1);
      if (! parameters.containsKey("MapType"))
         parameters.put("MapType", MapsDataSource.getMapType(MapFormat.MAP, api));
      if (! parameters.containsKey("Format"))
         parameters.put("Format", MapsDataSource.getImageFormat(ImageFormat.PNG, api));
      if (! parameters.containsKey("ImageHeight"))
      {
         if (imageHeight > 0)
            parameters.put("ImageHeight", imageHeight);
         else
            throw new RuntimeException("Image height must be specified either in a class wide @MapsSource annotation" +
                                       "or in a @ImageHeight annotated variable");
      }
      if (! parameters.containsKey("ImageWidth"))
      {
         if (imageWidth > 0)
            parameters.put("ImageWidth", imageWidth);
         else
            throw new RuntimeException("Image width must be specified either in a class wide @MapsSource annotation" +
                                       "or in a @ImageWidth annotated variable");
      }
      return parameters;
   }

   @Override
   public void processCursor(Object instance, Cursor cursor, ColumnContext[] projectionColumns)
         throws IllegalAccessException
   //------------------------------------------------------------------------------------------
   {
      if (imageField == null)
      {
         Class<?> C = instance.getClass();
         Field[] fields = C.getDeclaredFields();
         for (Field field : fields)
         {
            Annotation imageAnno = field.getAnnotation(MapOutputImage.class);
            if (imageAnno != null)
            {
               imageField = field;
               break;
            }
         }
         if (imageField == null)
         {
            Log.w(TAG, "Field with @MapOutputImage not found.");
            return;
         }
      }

      if ( (imageField.getType() != Bitmap.class) && (imageField.getType() != ImageView.class) )
         throw new RuntimeException("@MapOutputImage can only annotate Bitmap or ImageView fields");
      byte[] data = null;
      File imageFile = null;
      if (cursor.getType(0) == Cursor.FIELD_TYPE_BLOB)
         data = cursor.getBlob(0);
      else if (cursor.getType(0) == Cursor.FIELD_TYPE_STRING)
         imageFile = new File(cursor.getString(0));
      else
      {
         Log.e(TAG, "ImageCursor was null ???");
         return;
      }
      imageField.setAccessible(true);
      final Bitmap bmp;
      if (data != null)
         bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
      else
         bmp = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
      if (imageField.getType() == Bitmap.class)
         imageField.set(instance, bmp);
      else if (imageField.getType() == ImageView.class)
      {
         final ImageView imgvw = (ImageView) imageField.get(instance);
         if (imgvw != null)
         {
            if (context == null)
               throw new RuntimeException("Setting ImageView image requires a valid not null context in MapsAnnotationProcessor");
            if (imageViewHandler == null)
               imageViewHandler = new Handler(context.getMainLooper());
            imageViewHandler.post(new Runnable()
            {
               @Override
               public void run() { imgvw.setImageBitmap(bmp); }
            });
         }

      }
      else
         throw new RuntimeException("@MapOutputImage can only annotate Bitmap or ImageView fields");
   }

   private Integer parseInt(String v)
   //---------------------------
   {
      if (v == null) return null;
      v = v.trim();
      Integer I;
      try
      {
         I = Integer.parseInt(v);
      }
      catch (Exception _e)
      {
         try { BigDecimal bd = new BigDecimal(v); I = bd.intValue(); } catch (Exception _ee) { I = null; }
      }
      return I;
   }

   private Double parseDouble(String v)
   //---------------------------
   {
      if (v == null) return null;
      v = v.trim();
      Double D;
      try
      {
         D = Double.parseDouble(v);
      }
      catch (Exception _e)
      {
         try { BigDecimal bd = new BigDecimal(v); D = bd.doubleValue(); } catch (Exception _ee) { D = null; }
      }
      return D;
   }
}
