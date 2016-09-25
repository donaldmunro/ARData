package to.augmented.reality.android.ardb.http.sparql;

import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class SparQLSpatialFunctions implements ISpatialFunctions
//==============================================================
{
   private final int decimals;

   private String decimalFormat;

   public SparQLSpatialFunctions() { this(6);}

   public SparQLSpatialFunctions(int decimals)
   //--------------------------------------------
   {
      this.decimals = decimals;
      decimalFormat = "%." + decimals + "f";
   }

   @Override
   public String spatialPoint(double latitude, double longitude)
   {
      final String lat = String.format(Locale.getDefault(), decimalFormat, latitude);
      final String lon = String.format(Locale.getDefault(), decimalFormat, longitude);
      return String.format(Locale.getDefault(), "point geo:lat %s ; geo:long %s .", lat, lon);
   }

   @Override
   public String spatialPoint(String latitude, String longitude)
   {
      return String.format(Locale.getDefault(), "point geo:lat %s ; geo:long %s .", latitude,longitude);
   }

   @Override public String spatialToString(String spatialColumnName) { return ""; }

   @Override public String stringToSpatial(String spatialColumnName) { return ""; }

   @Override
   public String inCircle(String spatialColumnName, double pointLatitude, double pointLongitude, double radius)
   //----------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      final String minLatitude = String.format(Locale.getDefault(), decimalFormat, pointLatitude - radius);
      final String minLongitude = String.format(Locale.getDefault(), decimalFormat, pointLongitude - radius);
      final String maxLatitude = String.format(Locale.getDefault(), decimalFormat, pointLatitude + radius);
      final String maxLongitude = String.format(Locale.getDefault(), decimalFormat, pointLongitude + radius);
      return String.format(Locale.getDefault(), "%s geo:lat ?lat .\n" +
                                 "%s geo:long ?lon .\n" +
                                 " FILTER ( ?lat > %s && ?lat < %s && ?lon > %s  && ?lon < %s )",
                           spatialColumnName, spatialColumnName, minLatitude, maxLatitude, minLongitude, maxLongitude);
   }

   @Override
   public String inCircle(String spatialColumnName, String pointLatitude, String pointLongitude, String radius)
   //----------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      final String rad = String.format(Locale.getDefault(), decimalFormat, radius);
      return String.format(Locale.getDefault(), "%s geo:lat ?lat .\n" +
                                                "%s geo:long ?lon .\n" +
                                                " FILTER ( ?lat > %s - %s && ?lat < %s + %s && ?lon > %s - %s && ?lon < %s + %s )",
                           spatialColumnName, spatialColumnName, pointLongitude, rad, pointLongitude, rad, pointLatitude,
                           rad, pointLatitude, rad);
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
      return String.format(Locale.getDefault(), "%s geo:lat ?lat .\n" +
                                                "%s geo:long ?lon .\n" +
                                                " FILTER ( (?lat > %s && ?lat < %s) && (?lon > %s  && ?lon < %s) )",
                           spatialColumnName, spatialColumnName, minLatitude, maxLatitude, minLongitude, maxLongitude);
   }

   @Override
   public String inRectangle(String spatialColumnName, String minLatitude, String minLongitude, String maxLatitude,
                             String maxLongitude)
   //-------------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      return String.format(Locale.getDefault(), "%s geo:lat ?lat .\n" +
                                 "%s geo:long ?lon .\n" +
                                 " FILTER ( (?lat > %s && ?lat < %s) && (?lon > %s  && ?lon < %s) )",
                           spatialColumnName, spatialColumnName, minLatitude, maxLatitude, minLongitude, maxLongitude);
   }
}
