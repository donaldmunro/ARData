package to.augmented.reality.android.ardb.sourcefacade;

import android.database.sqlite.SQLiteDatabase;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * Defines spatial source operations
 */
public interface ISpatialSource
//=============================
{
   enum SPATIAL_OPERATION {RADIUS, BOUNDING_BOX, USER_DEFINED, UNDEFINED };

   /**
    * @return a name identifying this source.
    */
   String getName();

   void setLocalDatabase(SQLiteDatabase localDatabase);

   /**
    * Set query named parameters.
    * @param parameters The parameters as name, value pairs
    */
   void setParameters(Map<String, Object> parameters);

   /**
    * If using DataPoint callbacks the projection (SELECT columns) types must be defines as n array of
    * DataType
    * @param projectionTypes The projection (SELECT columns) types
    */
   void setProjectionTypes(DataType[] projectionTypes);

   /**
    * Execute bounding box query
    * @param centerLatitude The centre latitude
    * @param centerLongitude The centre longitude
    * @param token The identifying token for the callback
    * @param callback The callback
    * @return A Future representing the thread on which the query is executing.
    */
   Future<?> boundingBox(double centerLatitude, double centerLongitude, double width, double height,
                    ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback);

   /**
    * Execute radius query
    * @param centerLatitude The centre latitude
    * @param centerLongitude The centre longitude
    * @param token The identifying token for the callback
    * @param callback The callback
    * @return A Future representing the thread on which the query is executing.
    */
   Future<?> radius(double centerLatitude, double centerLongitude, double radius,
                    ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback);

   /**
    * Execute a user defined spatial query.
    * @param latitude Query latitude
    * @param longitude Query longitude
    * @param extraParameters Implementation specific parameters
    * @param token
    * @param callback
    * @return A Future representing the thread on which the query is executing.
    */
   Future<?> spatialQuery(double latitude, double longitude, ImmutableAnything extraParameters,
                          ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback);

   /**
    * @return The text of the last query if applicable
    */
   String getLastQuery();
}
