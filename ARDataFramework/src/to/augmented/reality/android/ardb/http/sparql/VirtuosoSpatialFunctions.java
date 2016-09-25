package to.augmented.reality.android.ardb.http.sparql;

import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class VirtuosoSpatialFunctions implements ISpatialFunctions
//==============================================================
{
   private final int decimals;

   private String decimalFormat;

   public VirtuosoSpatialFunctions() { this(6);}

   public VirtuosoSpatialFunctions(int decimals)
   //--------------------------------------------
   {
      this.decimals = decimals;
      decimalFormat = "%." + decimals + "f";
   }

   @Override
   public String spatialPoint(double latitude, double longitude)
   //-----------------------------------------------------------
   {
      final String lat = String.format(Locale.getDefault(), decimalFormat, latitude);
      final String lon = String.format(Locale.getDefault(), decimalFormat, longitude);
      return String.format(Locale.getDefault(), "bif:st_point (%s, %s)", lon, lat);
   }

   @Override
   public String spatialPoint(String latitude, String longitude)
   //-----------------------------------------------------------
   {
      return String.format(Locale.getDefault(), "bif:st_point (%s, %s)", longitude, latitude);
   }

   @Override
   public String spatialToString(String spatialColumnName)
   //-----------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      if (spatialColumnName == null)
         return "bif:st_astext";
      return String.format(Locale.getDefault(), "bif:st_astext(%s)", spatialColumnName);
   }

   @Override public String stringToSpatial(String spatialColumnName)
   //---------------------------------------------------------------
   {
      if (spatialColumnName == null)
         return "bif:st_geomfromtext";
      return String.format(Locale.getDefault(), "bif:st_geomfromtext(%s)", spatialColumnName);
   }

   @Override
   public String inCircle(String spatialColumnName, double pointLatitude, double pointLongitude, double radius)
   //----------------------------------------------------------------------------------------------------------
   {
      //?m  geo:geometry  spatialColumnName
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      final String point = spatialPoint(pointLatitude, pointLongitude);
      final String rad = String.format(Locale.getDefault(), decimalFormat, radius/1000); // Virtuoso st_intersects in Km's
      return String.format(Locale.getDefault(), "FILTER ( bif:st_intersects (%s, %s, %s)", spatialColumnName, point, rad);
   }

   @Override
   public String inCircle(String spatialColumnName, String pointLatitude, String pointLongitude, String radius)
   //----------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      final String point = spatialPoint(pointLatitude, pointLongitude);
      if (radius.trim().matches("[-+]?\\d+(\\.\\d+)?"))
      {
         try
         {
            Double D = Double.parseDouble(radius.trim());
            radius = Double.toString(D/1000); // Virtuoso st_intersects in Km's
         }
         catch (Exception e)
         {
         }
      }
      return String.format(Locale.getDefault(), "FILTER ( bif:st_intersects (%s, %s, %s)", spatialColumnName, point, radius);
   }

   @Override
   public String inRectangle(String spatialColumnName, double startLatitude, double startLongitude, double endLatitude,
                             double endLongitude)
   //-----------------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      final String minLatitude = String.format(Locale.getDefault(), decimalFormat, startLatitude);
      final String minLongitude = String.format(Locale.getDefault(), decimalFormat, startLongitude);
      final String maxLatitude = String.format(Locale.getDefault(), decimalFormat, endLatitude);
      final String maxLongitude = String.format(Locale.getDefault(), decimalFormat, endLongitude);
      return String.format(Locale.getDefault(),
                           "FILTER ( bif:st_intersects ( bif:st_geomfromtext ( \"BOX(%s %s, %s %s)\" ), %s ) )",
                           minLongitude, minLatitude, maxLongitude, maxLatitude, spatialColumnName);
   }

   @Override
   public String inRectangle(String spatialColumnName, String minLatitude, String minLongitude, String maxLatitude,
                             String maxLongitude)
   //-------------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      return String.format(Locale.getDefault(),
                           "FILTER ( bif:st_intersects ( bif:st_geomfromtext ( \"BOX(%s %s, %s %s)\" ), %s ) )",
                           minLongitude, minLatitude, maxLongitude, maxLatitude, spatialColumnName);
   }
}
