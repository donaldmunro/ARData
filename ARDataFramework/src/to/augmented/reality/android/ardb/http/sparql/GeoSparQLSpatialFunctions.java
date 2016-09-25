package to.augmented.reality.android.ardb.http.sparql;

import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class GeoSparQLSpatialFunctions implements ISpatialFunctions
//=================================================================
{
   private final int decimals;

   private String decimalFormat;

   public GeoSparQLSpatialFunctions() { this(6);}

   public GeoSparQLSpatialFunctions(int decimals)
   //--------------------------------------------
   {
      this.decimals = decimals;
      decimalFormat = "%." + decimals + "f";
   }

   @Override
   public String spatialPoint(double latitude, double longitude)
   //-----------------------------------------------------------
   {
      String format = String.format(Locale.getDefault(), "\"POINT(%s %s)\"^^geo:wktLiteral", decimalFormat, decimalFormat);
      return String.format(Locale.getDefault(), format, longitude, latitude);
   }

   @Override
   public String spatialPoint(String latitude, String longitude)
   {
      return String.format(Locale.getDefault(), "\"POINT(%s %s)\"^^geo:wktLiteral", longitude, latitude);
   }

   @Override public String spatialToString(String spatialColumnName) { return ""; }

   @Override
   public String stringToSpatial(String spatialColumnName) { return ""; }

   @Override
   public String inCircle(String spatialColumnName, double pointLatitude, double pointLongitude, double radius)
   //----------------------------------------------------------------------------------------------------------
   {
      String point = spatialPoint(pointLatitude, pointLongitude);
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      String dist = String.format(Locale.getDefault(), decimalFormat, radius);
      return String.format(Locale.getDefault(), "FILTER (geof:distance(%s, %s, units:metre) < %s)", spatialColumnName, point,
                           dist);
   }

   @Override
   public String inCircle(String spatialColumnName, String pointLatitude, String pointLongitude, String radius)
   //-----------------------------------------------------------------------------------------------------------
   {
      String point = spatialPoint(pointLatitude, pointLongitude);
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      return String.format(Locale.getDefault(), "FILTER (geof:distance(%s, %s, units:m) < %s)", spatialColumnName, point,
                           radius);
   }

   @Override
   public String inRectangle(String spatialColumnName, double startLatitude, double startLongitude, double endLatitude,
                             double endLongitude)
   //--------------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      final String minLatitude = String.format(Locale.getDefault(), decimalFormat, startLatitude);
      final String minLongitude = String.format(Locale.getDefault(), decimalFormat, startLongitude);
      final String maxLatitude = String.format(Locale.getDefault(), decimalFormat, endLatitude);
      final String maxLongitude = String.format(Locale.getDefault(), decimalFormat, endLongitude);
      return  String.format(Locale.getDefault(),
                            "FILTER(geof:within(%s, POLYGON((\n" +
                            "%s %s,\n" +
                            "%s %s,\n" +
                            "%s %s,\n" +
                            "%s %s,\n" +
                            "%s %s\n" +
                            "))\"^^geo:wktLiteral))",
                            spatialColumnName, minLongitude, minLatitude, maxLongitude, minLatitude,
                            maxLongitude, maxLatitude, minLongitude, maxLatitude, minLongitude, minLatitude);
   }

   @Override
   public String inRectangle(String spatialColumnName, String minLatitude, String minLongitude, String maxLatitude,
                             String maxLongitude)
   //--------------------------------------------------------------------------------------------------------------
   {
      if (! spatialColumnName.startsWith("?"))
         spatialColumnName = "?" + spatialColumnName;
      return String.format(Locale.getDefault(),
                            "FILTER(geof:within(%s, POLYGON((\n" +
                                  "%s %s,\n" +
                                  "%s %s,\n" +
                                  "%s %s,\n" +
                                  "%s %s,\n" +
                                  "%s %s\n" +
                                  "))\"^^geo:wktLiteral))",
                            spatialColumnName, minLongitude, minLatitude, maxLongitude, minLatitude,
                            maxLongitude, maxLatitude, minLongitude, maxLatitude, minLongitude, minLatitude);
   }
}
