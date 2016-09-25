package to.augmented.reality.android.ardb.sourcefacade.spatialite;


import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class SpatiaLiteSpatialFunctions implements ISpatialFunctions
//================================================================
{
   final int decimals;
   private final String decimalFormat;

   public SpatiaLiteSpatialFunctions() { this(6); }

   public SpatiaLiteSpatialFunctions(int decimals) { this.decimals = decimals; decimalFormat = "%." + decimals + "f"; }

   @Override
   public String spatialPoint(double latitude, double longitude)
   //-----------------------------------------------------------
   {
      String format = String.format(Locale.getDefault(), "MakePoint(%s, %s, 4326)", decimalFormat, decimalFormat);
      return String.format(Locale.getDefault(), format, longitude, latitude);
   }

   @Override
   public String spatialPoint(String latitude, String longitude)
   //-----------------------------------------------------------
   {
      return String.format(Locale.getDefault(), "MakePoint(%s, %s, 4326)", longitude, latitude);
   }

   @Override
   public String spatialToString(String spatialColumnName)
   //-----------------------------------------------------
   {
      if (spatialColumnName == null)
         return "AsText";
      return "AsText(" + spatialColumnName + ")";
   }

   @Override
   public String stringToSpatial(String spatialColumnName)
   //----------------------------------------------------
   {
      if (spatialColumnName == null)
         return "GeomFromText";
      return "GeomFromText(" + spatialColumnName + ",4326)";
   }

   @Override
   public String inCircle(String spatialColumnName, double pointLatitude, double pointLongitude,
                          double radius)
   //-----------------------------------------------------------------------------------------------------
   {
      String ptfmt = String.format(Locale.getDefault(), "MakePoint(%s, %s, 4326)", decimalFormat, decimalFormat);
      String point = String.format(ptfmt, pointLongitude, pointLatitude);

      return String.format("MbrWithin(Transform(%s, 25832), BuildCircleMbr(X(Transform(%s, 25832)), Y(Transform(%s, 25832)), " +
                           decimalFormat + ", 25832))", spatialColumnName, point, point, radius);
   }

   @Override
   public String inCircle(String spatialColumnName, String pointLatitude, String pointLongitude,
                          String radius)
   //-----------------------------------------------------------------------------------------------------
   {
      String point = String.format("MakePoint(%s, %s, 4326)", pointLongitude, pointLatitude);
      return String.format("MbrWithin(Transform(%s, 25832), BuildCircleMbr(X(Transform(%s, 25832)), Y(Transform(%s, 25832)), " +
                           "%s, 25832))", spatialColumnName, point, point, radius);
   }

   @Override
   public String inRectangle(String spatialColumnName, double minLatitude, double minLongitude,
                             double maxLatitude, double maxLongitude)
   //------------------------------------------------------------------------------------------------------------------
   {
      String format = String.format(Locale.getDefault(), "BuildMbr(%s, %s, %s, %s, 4326)", decimalFormat,
                                    decimalFormat, decimalFormat, decimalFormat);
      return String.format("MbrWithin(%s, " + format + ")", spatialColumnName, minLongitude, minLatitude, maxLongitude, maxLatitude);
      //MbrWithin(geom, BuildMbr(25.99, 33.999 ,26.01,34.001, 4326))
   }

   @Override
   public String inRectangle(String spatialColumnName, String minLatitude, String minLongitude,
                             String maxLatitude, String maxLongitude)
   //-----------------------------------------------------------------------------------------------
   {
      return String.format("MbrWithin(%s, BuildMbr(%s, %s, %s, %s, 4326))", spatialColumnName,
                           minLongitude, minLatitude, maxLongitude, maxLatitude);
   }
}
