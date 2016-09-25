package to.augmented.reality.android.ardb.jdbc;


import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class PostgresSpatialFunctions implements ISpatialFunctions
//================================================================
{
   final int decimals;
   private final String decimalFormat;

   public PostgresSpatialFunctions() { this(6); }

   public PostgresSpatialFunctions(int decimals) { this.decimals = decimals; decimalFormat = "%." + decimals + "f"; }

   @Override
   public String spatialPoint(double latitude, double longitude)
   //-----------------------------------------------------------
   {
      String format = String.format(Locale.getDefault(), "ST_GeomFromText('POINT(%s %s)', 4326)", decimalFormat, decimalFormat);
      return String.format(Locale.getDefault(), format, longitude, latitude);
   }

   @Override
   public String spatialPoint(String latitude, String longitude)
   //-----------------------------------------------------------
   {
      return String.format(Locale.getDefault(), "ST_GeomFromText('POINT(%s %s)', 4326)", longitude, latitude);
   }

   @Override
   public String spatialToString(String spatialColumnName)
   //-----------------------------------------------------
   {
      if (spatialColumnName == null)
         return "ST_AsText";
      return "ST_AsText(" + spatialColumnName + ")";
   }

   @Override
   public String stringToSpatial(String spatialColumnName)
   //----------------------------------------------------
   {
      if (spatialColumnName == null)
         return "ST_GeomFromText";
      return "ST_GeomFromText(" + spatialColumnName + ",4326)";
   }

   @Override
   public String inCircle(String spatialColumnName, double pointLatitude, double pointLongitude,
                          double radius)
   //-----------------------------------------------------------------------------------------------------
   {
      String format = String.format(Locale.getDefault(), "ST_MakePoint(%s, %s), %s", decimalFormat, decimalFormat,
                                    decimalFormat);
      return String.format(Locale.getDefault(), "ST_DWITHIN(%s, " + format + ")", spatialColumnName,
                           pointLongitude, pointLatitude, radius);
   }

   @Override
   public String inCircle(String spatialColumnName, String pointLatitude, String pointLongitude,
                          String radius)
   //-----------------------------------------------------------------------------------------------------
   {
      return String.format(Locale.getDefault(), "ST_DWITHIN(%s, ST_MakePoint(%s, %s), %s)", spatialColumnName,
                           pointLongitude, pointLatitude, radius);
   }

   @Override
   public String inRectangle(String spatialColumnName, double minLatitude, double minLongitude,
                             double maxLatitude, double maxLongitude)
   //------------------------------------------------------------------------------------------------------------------
   {
      String format = String.format(Locale.getDefault(), "ST_MakeEnvelope(%s,%s, %s, %s, 4326)", decimalFormat,
                                    decimalFormat, decimalFormat, decimalFormat);
      return String.format(Locale.getDefault(), "%s && " + format, spatialColumnName, minLongitude, minLatitude,
                           maxLongitude, maxLatitude);
   }

   @Override
   public String inRectangle(String spatialColumnName, String minLatitude, String minLongitude,
                             String maxLatitude, String maxLongitude)
   //-----------------------------------------------------------------------------------------------
   {
      return String.format(Locale.getDefault(), "%s && ST_MakeEnvelope(%s, %s,%s, %s, 4326)", spatialColumnName,
                           minLongitude, minLatitude, maxLongitude, maxLatitude);
   }
}
