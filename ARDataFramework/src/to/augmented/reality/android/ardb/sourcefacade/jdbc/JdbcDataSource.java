package to.augmented.reality.android.ardb.sourcefacade.jdbc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.util.Util;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;
import to.augmented.reality.android.ardb.jdbc.DatabaseType;
import to.augmented.reality.android.ardb.jdbc.IJdbcConnectCallback;
import to.augmented.reality.android.ardb.jdbc.JdbcRequestHandler;
import to.augmented.reality.android.ardb.jdbc.JdbcRequestor;
import to.augmented.reality.android.ardb.jdbc.PostgresRequestor;
import to.augmented.reality.android.ardb.jdbc.SQLServerRequestor;
import to.augmented.reality.android.ardb.sourcefacade.CursorCallback;
import to.augmented.reality.android.ardb.sourcefacade.DataType;
import to.augmented.reality.android.ardb.sourcefacade.IAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialQueryResult;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.SQLDataSource;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class JdbcDataSource extends SQLDataSource implements ISpatialSource
//=========================================================================
{
   final private String TAG = JdbcDataSource.class.getName();

   protected JdbcRequestor requestor;
   protected String sourceName;
   protected final DatabaseType databaseType;
   protected String host;
   protected int port;
   protected String user;
   protected String password;
   protected String database;
   protected long connectionTimeout;
   protected long readTimeout;

   @Override public void setProjectionTypes(DataType[] projectionTypes) { this.projectionTypes = projectionTypes; }

   protected IAnnotationProcessor annotationProcessor = null;
   protected Object annotatedInstance = null;
   protected  ISpatialSource.SPATIAL_OPERATION lastOp = null;
   protected double lastLatitude =0, lastLongitude =0, lastRadius =0, lastWidth =0, lastHeight =0;

   public JdbcDataSource(String sourceName, Context context, ActiveObject ao, DatabaseType databaseType)
   //--------------------------------------------------------------------------------------------------
   {
      this(sourceName, context, ao, databaseType, null);
   }

   public JdbcDataSource(String sourceName, Context context, ActiveObject ao, DatabaseType databaseType,
                         IAnnotationProcessor processor)
   //-------------------------------------------------------------------------------------------
   {
      this.sourceName = sourceName;
      this.databaseType = databaseType;
      annotationProcessor = processor;
      requestor = onGetRequestor(databaseType, context, ao);
   }

   public void configureConnection(String host, int port, String user, String password, String database,
                                   long connectionTimeout, long readTimeout)
   //--------------------------------------------------------------------------------------------------
   {
      this.host = host;
      if (port <= 0)
         this.port = requestor.onGetPort(databaseType);
      else
         this.port = port;
      this.user = user;
      this.password = password;
      this.database = database;
      this.connectionTimeout = connectionTimeout;
      this.readTimeout = readTimeout;
   }

   public void configureAnnotationProcessor(IAnnotationProcessor processor, Object instance)
   //---------------------------------------------------------------------------------------
   {
      this.annotationProcessor = processor;
      this.annotatedInstance = instance;
   }

   @Override public String getName() { return sourceName; }

   @Override public void setLocalDatabase(SQLiteDatabase androidDatabase) { requestor.setLocalDatabase(androidDatabase); }

   public void setAnnotatedInstance(Object instance) { this.annotatedInstance = instance; }

   protected String onGetWhere(String where)
   //---------------------------------------
   {
      String nowhere = ((this.where == null) ? "" : this.where);
      if ( (where == null) || (where.trim().isEmpty()) )
         return nowhere;
      else
      {
         if (nowhere.trim().isEmpty())
            return where;
         else
         {
            Map<String, int[]> paramMap = new HashMap<>();
            int[] count = new int[1];
            JdbcRequestHandler.parseParameters(nowhere, paramMap, count);
            if (count[0] > 0)
            {
               for (String param : paramMap.keySet())
               {
                  WhereParameterProperties prop = whereParameters.get(param);
                  if (prop == null)
                     prop = whereParameters.get(":" + param);
                  if ( (prop != null) && (prop.isTextToSpatial) )
                     nowhere = nowhere.replaceAll(":" + param, requestor.spatialFunctions().stringToSpatial(":" + param));
               }
            }
            final boolean isStartAnd = (Util.startWithWordPattern("AND").matcher(where).matches());
            final boolean isStartOr = (Util.startWithWordPattern("OR").matcher(where).matches());
            return nowhere + (((isStartAnd) || (isStartOr) ) ? where : " AND " + where);
         }
      }
   }

   protected JdbcRequestor onGetRequestor(DatabaseType databaseType, Context context, ActiveObject ao)
   //-------------------------------------------------------------------------------------------------
   {
      switch (databaseType)
      {
         case POSTGRES:
            return new PostgresRequestor(context, ao);
         case SQLSERVER:
            return new SQLServerRequestor(context, ao);
      }
      throw new RuntimeException("Unknown database type");
   }

   @Override
   public Future<?> boundingBox(double centerLatitude, double centerLongitude, double width, double height,
                                ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback)
   //-------------------------------------------------------------------------------------------------------------
   {
      final String whereColumn = onGetSpatialWhereColumn();
      if ( (whereColumn == null) || (whereColumn.trim().isEmpty()) )
         throw new RuntimeException("Spatial selection column not specified");
      String spatialWhere = requestor.spatialFunctions().inRectangle(whereColumn,
                                                                     centerLatitude - height / 2,
                                                                     centerLongitude - width / 2,
                                                                     centerLatitude + height / 2,
                                                                     centerLongitude + width / 2);
      String sql = select(spatialWhere, requestor.spatialFunctions());
      Map<String, Object> parameters = null;
      if (annotationProcessor != null)
         parameters = annotationProcessor.processParameters(annotatedInstance, null);
      token.put("query", sql);
      StringBuilder errbuf = new StringBuilder();
      Future<?> future = executeQuery(sql, parameters, callbackType, token, callback, errbuf);
      if (future == null)
      {
         Log.e(TAG, "JdbcDataSource.boundingBox: Error executing Jdbc Query: " + errbuf.toString());
         return null;
      }
      lastOp = SPATIAL_OPERATION.BOUNDING_BOX;
      lastLatitude = centerLatitude;
      lastLongitude = centerLongitude;
      lastWidth = width;
      lastHeight = height;
      return future;
   }

   @Override
   public Future<?> radius(double centerLatitude, double centerLongitude, double radius,
                           ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback)
   //----------------------------------------------------------------------------------------------
   {
      final String whereColumn = onGetSpatialWhereColumn();
      if ( (whereColumn == null) || (whereColumn.trim().isEmpty()) )
         throw new RuntimeException("Spatial selection column not specified");
      String spatialWhere = requestor.spatialFunctions().inCircle(whereColumn, centerLatitude, centerLongitude, radius);
      String sql = select(spatialWhere, requestor.spatialFunctions());
      Map<String, Object> parameters = null;
      if (annotationProcessor != null)
         parameters = annotationProcessor.processParameters(annotatedInstance, null);
      token.put("query", sql);
      StringBuilder errbuf = new StringBuilder();
      Future<?> future = executeQuery(sql, parameters, callbackType, token, callback, errbuf);
      if (future == null)
      {
         Log.e(TAG, "JdbcDataSource.radius: Error executing Jdbc Query: " + errbuf.toString());
         return null;
      }
      lastOp = SPATIAL_OPERATION.RADIUS;
      lastLatitude = centerLatitude;
      lastLongitude = centerLongitude;
      lastRadius = radius;
      return future;
   }

   @Override
   public Future<?> spatialQuery(double latitude, double longitude, ImmutableAnything extraParameters,
                                 ISpatialQueryResult.CALLBACK_TYPE callbackType,
                                 Anything token, ISpatialQueryResult callback)
   //-------------------------------------------------------------------------------------------------------
   {
      String where = getWhere();
      if (where == null)
      {
         callback.onError(sourceName, token, "spatialQuery: Where clause " + where + " does not contain any paramaters named " +
               ":location and (:latitude or :longitude)", null);
         return null;
      }
      if ( (where.indexOf(":location") < 0) && ( (where.indexOf(":latitude") < 0) || (where.indexOf(":longitude") < 0) ) )
      {
         callback.onError(sourceName, token, "spatialQuery: Where clause " + where + " does not contain any paramaters named " +
                          ":location and (:latitude or :longitude)", null);
         return null;
      }
      if (parameters == null)
         parameters = new HashMap<>();

//      String where = (String) criteria[0];
//      String locationParamName = getQueryParam(criteria, 1, ":location");
//      String latitudeParamName = getQueryParam(criteria, 2, ":latitude");
//      String longitudeParamName = getQueryParam(criteria, 3, ":longitude");

      String sql = select(null, requestor.spatialFunctions());
      if (annotationProcessor != null)
         annotationProcessor.processParameters(annotatedInstance, parameters);

      if (where.indexOf(":location") > 0)
         parameters.put("location", requestor.spatialFunctions().stringToSpatial(String.format("POINT(%.7f %.7f)",
                                                                                               longitude, latitude)));
      else
      {
         if (where.indexOf(":latitude") > 0)
            parameters.put("latitude", String.format("%.7f", latitude));
         if (where.indexOf(":longitude") > 0)
            parameters.put("longitude", String.format("%.7f", longitude));
      }
      token.put("query", sql);
      StringBuilder errbuf = new StringBuilder();
      Future<?> future = executeQuery(sql, parameters, callbackType, token, callback, errbuf);
      if (future == null)
      {
         Log.e(TAG, "JdbcDataSource.spatialQuery: Error executing Jdbc Query: " + errbuf.toString());
         return null;
      }
      lastOp = SPATIAL_OPERATION.USER_DEFINED;
      lastLatitude = latitude;
      lastLongitude = longitude;
      return future;
   }

   @Override
   public String getLastQuery()
   //----------------------
   {
      final String whereColumn = onGetSpatialWhereColumn();
      String spatialWhere = null;
      if ( (whereColumn != null) && (! whereColumn.trim().isEmpty()) )
      {
         switch (lastOp)
         {
            case RADIUS:
               spatialWhere = requestor.spatialFunctions().inCircle(whereColumn, lastLatitude, lastLongitude, lastRadius);
               break;
            case BOUNDING_BOX:
               spatialWhere = requestor.spatialFunctions().inRectangle(whereColumn, lastLatitude, lastLongitude, lastWidth,
                                                                       lastHeight);
               break;
         }
      }
      return select(spatialWhere, requestor.spatialFunctions());
   }

   protected Future<?> executeQuery(final String query, final Map<String, Object> parameters,
                                    ISpatialQueryResult.CALLBACK_TYPE callbackType, final Anything token,
                                    final ISpatialQueryResult callback, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------
   {
      final CursorCallback queryCallback = new CursorCallback(sourceName, callbackType, token, projectionColumns,
                                                              projectionTypes, callback,
                                                              annotationProcessor, annotatedInstance);
      return requestor.connect(host, port, database, user, password, connectionTimeout, TimeUnit.MILLISECONDS,
                               readTimeout, TimeUnit.MILLISECONDS, false,
      new IJdbcConnectCallback()
      //========================
      {
         @Override
         public void onConnected(Anything token, Connection connection)
         //------------------------------------------------------------
         {
            requestor.query(connection, query, 0, TimeUnit.SECONDS, parameters, token, queryCallback, null);
         }

         @Override
         public void onError(Anything token, CharSequence message, Throwable exception)
         //----------------------------------------------------------------------------
         {
            queryCallback.onJdbcError(null, token, "Database connection failed", exception);
         }
      }, token, errbuf);
   }

}
