package to.augmented.reality.android.ardb.jdbc;

import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class SQLServerSpatialFunctions  implements ISpatialFunctions
//==================================================================
{
   final int decimals;
   private final String decimalFormat;

   public SQLServerSpatialFunctions() { this(6); }

   public SQLServerSpatialFunctions(int decimals) { this.decimals = decimals; decimalFormat = "%." + decimals + "f"; }

   @Override
   public String spatialPoint(double latitude, double longitude)
   //-----------------------------------------------------------
   {
      String format = String.format(Locale.getDefault(), "POINT(%s %s)", decimalFormat, decimalFormat);
      return String.format(Locale.getDefault(), "geography::STGeomFromText('" + format + "', 4326)", longitude, latitude);
   }

   @Override
   public String spatialPoint(String latitude, String longitude)
   {
      return String.format(Locale.getDefault(), "geography::STGeomFromText('POINT(%s %s)', 4326)", longitude, latitude);
   }

   @Override
   public String spatialToString(String spatialColumnName) { return spatialColumnName + ".STAsText()"; }

   @Override
   public String stringToSpatial(String spatialColumnName) { return "geography::STGeomFromText(" + spatialColumnName + ",4326)"; }

   @Override
   public String inCircle(String spatialColumnName, double pointLatitude, double pointLongitude,
                          double radius)
   //-----------------------------------------------------------------------------------------------------
   {
      String point = String.format(Locale.getDefault(), "'POINT(%s, %s)'", decimalFormat, decimalFormat);
      String buffer = String.format(Locale.getDefault(), "STBuffer(%s)", decimalFormat);
      return String.format(Locale.getDefault(),
                           "%s.STIntersects(geography::STPointFromText(" + point + ", 4326)." + buffer + "=1",
                           spatialColumnName, pointLongitude, pointLatitude, radius);
   }

   @Override
   public String inCircle(String spatialColumnName, String pointLatitude, String pointLongitude,
                          String radius)
   //-----------------------------------------------------------------------------------------------------
   {
      return String.format(Locale.getDefault(),
                           "%s.STIntersects(geography::STPointFromText('POINT(%s, %s)', 4326).STBuffer(%s)=1",
                           spatialColumnName, pointLongitude, pointLatitude, radius);
   }

   @Override
   public String inRectangle(String spatialColumnName, double startLatitude, double startLongitude,
                             double endLatitude, double endLongitude)
   //-----------------------------------------------------------------------------------------------------------
   {
      final String minLatitude = String.format(Locale.getDefault(), decimalFormat, startLatitude);
      final String minLongitude = String.format(Locale.getDefault(), decimalFormat, startLongitude);
      final String maxLatitude = String.format(Locale.getDefault(), decimalFormat, endLatitude);
      final String maxLongitude = String.format(Locale.getDefault(), decimalFormat, endLongitude);
      String rectangle = "POLYGON(('" + minLongitude + ' '  + minLatitude + ", " +  maxLongitude + ' ' + minLatitude + ", "
            + maxLongitude + ' ' + maxLatitude + ", "
            + minLongitude + ' ' + maxLatitude + ", " + minLongitude + ' ' + minLatitude + "))";
      return spatialColumnName + ".STWithin(" + rectangle + ")";

   }

   @Override
   public String inRectangle(String spatialColumnName, String minLatitude, String minLongitude,
                             String maxLatitude, String maxLongitude)
   //-----------------------------------------------------------------------------------------------------------
   {
      String rectangle = "POLYGON(('" + minLongitude + ' '  + minLatitude + ", " +  maxLongitude + ' ' + minLatitude +
            ", " + maxLongitude + ' ' + maxLatitude + ", "
            + minLongitude + ' ' + maxLatitude + ", " + minLongitude + ' ' + minLatitude + "))";
      return spatialColumnName + ".STWithin(" + rectangle + ")";
   }
}
