package to.augmented.reality.android.ardb.http.maps;

import android.net.Uri;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.MIME_TYPES;

import java.io.File;

public class MapQuestRequestor extends MapRequestor
//=================================================
{
   static final private String TAG = MapQuestRequestor.class.getName();

   static final public String MAP_QUEST_URL = "http://www.mapquestapi.com/staticmap/v4/getmap";

   public enum MapType implements IMapType {map, satellite, hybrid }

   public enum MapImageType implements IMapImageType { png, gif, jpg }

   protected String tag() { return TAG; }

   public MapQuestRequestor(ActiveObject activeObject) { super(activeObject); }

   @Override
   protected Uri url(String key, Anything anyParameters, boolean isAutoLabel)
   //------------------------------------------------------------------------
   {
      if ( (key == null) || (key.trim().isEmpty()) )
      {
         Log.e(TAG, "MapQuestRequestor requires MapQuest API key (http://developer.mapquest.com)");
         return null;
      }
      Uri.Builder uriBuilder = Uri.parse(MAP_QUEST_URL).buildUpon();
      uriBuilder.appendQueryParameter("key", key);
      final String centre = anyParameters.getAsString("centre", null);
      final String type = anyParameters.getAsString("type", "map");
      final String imageType = anyParameters.getAsString("imagetype", "jpg");
      final String imageSize = anyParameters.getAsString("imagesize", null);
      if (imageSize == null)
      {
         Log.e(TAG, "MapQuestRequestor requires image size");
         return null;
      }
      Object[] pois = anyParameters.get("pois").asArray(null);
      if (centre != null)
      {
         uriBuilder.appendQueryParameter("center", centre);
         int zoom = anyParameters.get("zoom").asInt(-1);
         int scale = anyParameters.get("density").asInt(-1);
         if ( (zoom < 0) && (scale > 0) )
            uriBuilder.appendQueryParameter("scale", Integer.toString(scale));
         else if ( (zoom <= 0) || (zoom > 18) )
         {
            Log.e(TAG, "MapQuestRequestor requires MapQuest zoom between 1 and 18 for centred requests. Defaulting to 14");
            uriBuilder.appendQueryParameter("zoom", "14");
         }
         else
            uriBuilder.appendQueryParameter("zoom", Integer.toString(zoom));
      }
      else
      {
         String box = anyParameters.getAsString("box", null);
         if (box == null)
         {
            if ( (pois == null) || (pois.length < 4) )
            {
               Log.e(TAG, "MapRequestor requires either centre and zoom or more than marker to specify image");
               return null;
            }
         }
         else
            uriBuilder.appendQueryParameter("bestfit", box);
      }
      uriBuilder.appendQueryParameter("type", type);
      uriBuilder.appendQueryParameter("imagetype", imageType);
      uriBuilder.appendQueryParameter("size", imageSize);
      if ( (pois != null) && (pois.length > 0) )
      {
         StringBuilder poibuf = new StringBuilder();
         int c = 1;
         for (Object o : pois)
         {
            if (! (o instanceof POI) )
               continue;
            POI poi  = (POI) o;
            if (poi.isVisible())
            {
               final String label;
               if ((poi.label == null) || (poi.label.length() == 0))
                  //if (isAutoLabel) //MapQuest requires a label
                  label = Integer.toString(c++);
               else
                  label = poi.label;
               if (poi.getColor() != null)
               {
                  if (poi.getColor() != POI.POIColor.def)
                     poibuf.append(poi.getColor().toString()).append('-');
               }
               poibuf.append(label).append(',').append(String.format("%.6f,%.6f", poi.latitude, poi.longitude));
               if ((poi.xOffset > 0) && (poi.yOffset > 0))
                  poibuf.append(String.format(",%d,%d", poi.xOffset, poi.yOffset));
            }
            else
               poibuf.append(',').append(String.format("%.6f,%.6f", poi.latitude, poi.longitude));
            poibuf.append('|');
         }
         poibuf.deleteCharAt(poibuf.length() - 1);
         uriBuilder.appendQueryParameter("pois", poibuf.toString());
      }
      return uriBuilder.build();
   }

   protected Anything onPrepareParameters(Anything anything, Float upperLeftlatitude, Float upperLeftlongitude,
                                          Float lowerRightLatitude, Float lowerRightLongitude, Integer density,
                                          IMapType maptype, Integer imageWidth, Integer imageHeight,
                                          IMapImageType imagetype, POI[] pois)
   //-------------------------------------------------------------------------------------------------------
   {
      if (anything == null)
         anything = new Anything();
      if ( (upperLeftlatitude != null) && (upperLeftlongitude != null) &&
           (lowerRightLatitude != null) && (lowerRightLongitude != null) )
         anything.put("box", String.format("%.6f,%.6f,%.6f,%.6f", upperLeftlatitude, upperLeftlongitude, lowerRightLatitude,
                                           lowerRightLongitude));
      if ( (density != null) && (density > 0) )
         anything.put("density", density);
      MapType type;
      try
      {
         type = (MapType) maptype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapType should be a " + MapQuestRequestor.class.getName() + ".MapType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }
      if (type != null)
         anything.put("type", type.toString());
      if ( (imageHeight != null) && (imageWidth != null) )
         anything.put("imagesize", Integer.toString(imageWidth) +  "," + Integer.toString(imageHeight));
      MapImageType imageType;
      try
      {
         imageType = (MapImageType) imagetype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapImageType should be a " + MapQuestRequestor.class.getName() + ".MapImageType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }
      if (imageType != null)
         anything.put("imagetype", imageType.toString());
      if ( (pois != null) && (pois.length > 0) )
      {
         Anything list = new Anything();
         for (POI poi : pois)
            list.add(poi);
         anything.put("pois", list);
      }
      return anything;
   }

   protected Anything onPrepareParameters(Anything anything, float latitude, float longitude, Integer zoom,
                                          Integer density, IMapType maptype, Integer imageWidth, Integer imageHeight,
                                          IMapImageType imagetype, POI[] pois)
   //-------------------------------------------------------------------------------------------------------
   {
      if (anything == null)
         anything = new Anything();
      anything.put("centre", String.format("%.6f,%.6f", latitude, longitude));
      if (zoom != null)
         anything.put("zoom", zoom);
      if ( (density != null) && (density > 0) )
         anything.put("density", density);
      MapType type;
      try
      {
         type = (MapType) maptype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapType should be a " + MapQuestRequestor.class.getName() + ".MapType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }
      if (type != null)
         anything.put("type", type.toString());
      if ( (imageHeight != null) && (imageWidth != null) )
         anything.put("imagesize", Integer.toString(imageWidth) +  "," + Integer.toString(imageHeight));
      MapImageType imageType;
      try
      {
         imageType = (MapImageType) imagetype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapImageType should be a " + MapQuestRequestor.class.getName() + ".MapImageType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }
      if (imageType != null)
         anything.put("imagetype", imageType.toString());
      if ( (pois != null) && (pois.length > 0) )
      {
         Anything list = new Anything();
         for (POI poi : pois)
            list.add(poi);
         anything.put("pois", list);
      }
      return anything;
   }

   protected MIME_TYPES getMimeType(IMapImageType imagetype, StringBuilder extb)
   //------------------------------------------------------------------------
   {
      MIME_TYPES mimeType = MIME_TYPES.ANY;
      final String ext;
      MapImageType imageType;
      try
      {
         imageType = (MapImageType) imagetype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapImageType should be a " + MapQuestRequestor.class.getName() + ".MapImageType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }
      switch (imageType)
      {
         case png:   mimeType = MIME_TYPES.PNG; ext = ".png"; break;
         case jpg:   mimeType = MIME_TYPES.JPG; ext = ".jpg"; break;
         case gif:   mimeType = MIME_TYPES.GIF; ext = ".gif"; break;
         default:    ext = "";
      }
      if (extb != null)
         extb.append(ext);
      return mimeType;
   }

   protected IMapImageType typeFromFile(IMapImageType imageType, File imageFile)
   //---------------------------------------------------------------------------
   {
      if (imageFile == null)
         return imageType;
      String fext = null;
      if (imageFile != null)
      {
         String name = imageFile.getName();
         int p = name.lastIndexOf('.');
         if (p >= 0)
            fext = name.substring(p).trim().toLowerCase();
      }
      if (fext.equals(".png"))
         return MapImageType.png;
      else if (fext.equals(".gif"))
         return MapImageType.gif;
      else
         return MapImageType.jpg;

   }
}
