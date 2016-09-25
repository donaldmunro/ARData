package to.augmented.reality.android.ardb.http.maps;

import android.net.Uri;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.MIME_TYPES;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleMapRequestor extends MapRequestor
//=================================================
{
   static final private String TAG = GoogleMapRequestor.class.getName();

   static final public String GOOGLE_URL = "https://maps.googleapis.com/maps/api/staticmap";

   public enum MapType implements IMapType {roadmap, satellite, terrain, hybrid }

   public enum MapImageType implements IMapImageType
   //===============================================
   {
      png("png"),
      png32("png32"),
      gif("gif"),
      jpg("jpg"),
      jpg_baseline("jpg-baseline"); // the hyphen here is the reason for all the extra baggage

      private static final Map<String, MapImageType> lookup = new HashMap<>();

      static
      {
         for(MapImageType mit : EnumSet.allOf(MapImageType.class))
            lookup.put(mit.toString(), mit);
      }

      private String name;

      private MapImageType(String name) { this.name = name; }

      @Override
      public String toString(){ return name; }
   }

   protected String tag() { return TAG; }

   public GoogleMapRequestor(ActiveObject activeObject) { super(activeObject); }

   @Override
   protected Uri url(String key, Anything anyParameters, boolean isAutoLabel)
   //------------------------------------------------------------------------
   {
      if ( (key == null) || (key.trim().isEmpty()) )
      {
         Log.e(TAG, "GoogleMapRequestor requires Google API key for static maps API (https://console.developers.google.com/project)");
         return null;
      }
      Uri.Builder uriBuilder = Uri.parse(GOOGLE_URL).buildUpon();
      uriBuilder.appendQueryParameter("key", key);
      final String centre = anyParameters.getAsString("centre", null);
      final String type = anyParameters.getAsString("type", "map");
      final String imageType = anyParameters.getAsString("imagetype", "jpg");
      final String imageSize = anyParameters.getAsString("imagesize", null);
      final Object[] pois = anyParameters.get("pois").asArray(null);
      if (imageSize == null)
      {
         Log.e(TAG, "GoogleMapRequestor requires image size");
         return null;
      }
      if (centre != null)
      {
         uriBuilder.appendQueryParameter("center", centre);
         int zoom = anyParameters.get("zoom").asInt(-1);
         if ( (zoom <= 0) || (zoom > 21) )
         {
            Log.e(TAG, "GoogleMapRequestor requires MapQuest zoom between 1 and 18 for centred requests. Defaulting to 14");
            uriBuilder.appendQueryParameter("zoom", "14");
         }
         else
            uriBuilder.appendQueryParameter("zoom", Integer.toString(zoom));
      }
      else
      {
         if ( (pois == null) || (pois.length < 4) )
         {
            Log.e(TAG, "GoogleMapRequestor requires either centre and zoom or more than marker to specify image");
            return null;
         }
      }
      int scale = anyParameters.get("density").asInt(- 1);
      if (scale > 0)
         uriBuilder.appendQueryParameter("scale", Integer.toString(scale));
      uriBuilder.appendQueryParameter("maptype", type);
      uriBuilder.appendQueryParameter("format", imageType);
      uriBuilder.appendQueryParameter("size", imageSize);

      if ( (pois != null) && (pois.length > 0) )
      {
         StringBuilder poibuf = new StringBuilder();
         StringBuilder vbuf = new StringBuilder();
         int c = 0;
         for (Object o : pois)
         {
            if (! (o instanceof POI) )
               continue;
            POI poi  = (POI) o;
            if (poi.isVisible())
            {
               String label = null;
               if ((poi.size != null) && (poi.size != POI.POISize.def))
                  poibuf.append("size:").append(poi.size.toString()).append('|');
               if (poi.getColor() != null)
               {
                  if (poi.getColor() == POI.POIColor.user_defined)
                     poibuf.append("color:").append(poi.getUserDefinedColor()).append('|');
                  else if (poi.getColor() != POI.POIColor.def)
                     poibuf.append("color:").append(poi.getColor().toString()).append('|');
               }
               if ((poi.label == null) || (poi.label.length() == 0))
               {
                  if (isAutoLabel)
                     label = Integer.toString(c++);
               } else
                  label = poi.label;
               if (label != null)
                  poibuf.append("label:").append(label).append('|');
               poibuf.append(String.format("%.6f,%.6f", poi.latitude, poi.longitude));
               uriBuilder.appendQueryParameter("markers", poibuf.toString());
               poibuf.setLength(0);
            }
            else
               vbuf.append(String.format("%.6f,%.6f", poi.latitude, poi.longitude)).append("|");
         }
         if (vbuf.length() > 0)
            vbuf.deleteCharAt(vbuf.length() - 1);
         uriBuilder.appendQueryParameter("visible", vbuf.toString());
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
      if ( (density != null) && (density > 0) )
         anything.put("density", density);
      MapType type;
      try
      {
         type = (MapType) maptype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapType should be a " + GoogleMapRequestor.class.getName() + ".MapType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }

      if (type != null)
         anything.put("type", type.toString());
      if ( (imageHeight != null) && (imageWidth != null) )
         anything.put("imagesize", Integer.toString(imageWidth) +  "x" + Integer.toString(imageHeight));
      MapImageType imageType;
      try
      {
         imageType = (MapImageType) imagetype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapImageType should be a " + GoogleMapRequestor.class.getName() + ".MapImageType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }

      if (imageType != null)
         anything.put("imagetype", imageType.toString());
      List<POI> markerList = new ArrayList<>();
      if ( (upperLeftlatitude != null) && (upperLeftlongitude != null) &&
           (lowerRightLatitude != null) && (lowerRightLongitude != null) )
      {
         markerList.add(new POI(upperLeftlatitude, upperLeftlongitude, ""));
         markerList.add(new POI(lowerRightLatitude, lowerRightLongitude, ""));
      }
      if (pois != null)
      {
         for (POI poi : pois)
            markerList.add(poi);
      }
      pois = markerList.toArray(new POI[markerList.size()]);
      Anything list = new Anything();
      for (POI poi : pois)
         list.add(poi);
      anything.put("pois", list);
      return anything;
   }

   protected Anything onPrepareParameters(Anything anything, float latitude, float longitude, Integer zoom,
                                          Integer scale, IMapType maptype, Integer imageWidth, Integer imageHeight,
                                          IMapImageType imagetype, POI[] pois)
   //-------------------------------------------------------------------------------------------------------
   {
      if (anything == null)
         anything = new Anything();
      anything.put("centre", String.format("%.6f,%.6f", latitude, longitude));
      if (zoom != null)
         anything.put("zoom", zoom);
      if ( (scale != null) && (scale > 0) )
         anything.put("scale", scale);
      MapType type;
      try
      {
         type = (MapType) maptype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapType should be a " + GoogleMapRequestor.class.getName() + ".MapType";
         Log.e(TAG, err, e);
         throw new RuntimeException(err, e);
      }
      if (type != null)
         anything.put("type", type.toString());
      if ( (imageHeight != null) && (imageWidth != null) )
         anything.put("imagesize", Integer.toString(imageWidth) +  "x" + Integer.toString(imageHeight));
      MapImageType imageType;
      try
      {
         imageType = (MapImageType) imagetype;
      }
      catch (ClassCastException e)
      {
         final String err = "Hack alert: The IMapImageType should be a " + GoogleMapRequestor.class.getName() + ".MapImageType";
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
         final String err = "Hack alert: The IMapImageType should be a " + GoogleMapRequestor.class.getName() + ".MapImageType";
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
      {
         if ( ((MapImageType) imageType) != MapImageType.jpg_baseline)
            return MapImageType.jpg;
      }
      return imageType;
   }
}
