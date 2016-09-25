package to.augmented.reality.android.ardb.spi;

public interface ISpatialFunctions
//====================================
{
   String spatialPoint(double latitude, double longitude);

   String spatialPoint(String latitude, String longitude);

   String spatialToString(String spatialColumnName);

   String stringToSpatial(String spatialColumnName);

   String inCircle(String spatialColumnName, double pointLatitude, double pointLongitude, double radius);

   String inCircle(String spatialColumnName, String pointLatitude, String pointLongitude, String radius);

   String inRectangle(String spatialColumnName, double minLatitude, double minLongitude,
                      double maxLatitude, double maxLongitude);

   String inRectangle(String spatialColumnName, String minLatitude, String minLongitude,
                      String maxLatitude, String maxLongitude);
}
