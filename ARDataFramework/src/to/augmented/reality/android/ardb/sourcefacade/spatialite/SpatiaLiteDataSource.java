package to.augmented.reality.android.ardb.sourcefacade.spatialite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.SpatialiteException;
import jsqlite.Stmt;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.sourcefacade.EmptyCursor;
import to.augmented.reality.android.ardb.util.Util;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;
import to.augmented.reality.android.ardb.util.DatabaseUtils;
import to.augmented.reality.android.ardb.jdbc.JdbcRequestHandler;
import to.augmented.reality.android.ardb.sourcefacade.CursorCallback;
import to.augmented.reality.android.ardb.sourcefacade.DataType;
import to.augmented.reality.android.ardb.sourcefacade.IAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialQueryResult;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.SQLDataSource;
import to.augmented.reality.android.ardb.sourcefacade.jdbc.WhereParameterProperties;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class SpatiaLiteDataSource extends SQLDataSource implements ISpatialSource
//===============================================================================
{
   static final String TAG = SpatiaLiteDataSource.class.getName();

   protected final Context context;
   protected String sourceName;
   @Override public String getName() { return sourceName; }

   protected File databaseFile = null;
   protected Database db = null;

   @Override public void setProjectionTypes(DataType[] projectionTypes) { this.projectionTypes = projectionTypes; }

   protected IAnnotationProcessor annotationProcessor = null;
   protected Object annotatedInstance = null;
   SpatiaLiteSpatialFunctions spatialFunctions = new SpatiaLiteSpatialFunctions();
   protected  ISpatialSource.SPATIAL_OPERATION lastOp = null;
   protected double lastLatitude =0, lastLongitude =0, lastRadius =0, lastWidth =0, lastHeight =0;

   protected ActiveObject activeObject;

   public SpatiaLiteDataSource(String sourceName, Context context, ActiveObject ao,
                               IAnnotationProcessor processor)
   //-------------------------------------------------------------------------------------------------
   {
      this.sourceName = sourceName;
      this.context = context;
      activeObject = ao;
      this.annotationProcessor = processor;
   }

   final public void open(File databaseFile) throws SpatialiteException
   //-----------------------------------------------------------------
   {
      this.databaseFile = databaseFile;
      db = new jsqlite.Database();
      db.open(databaseFile.getAbsolutePath(), Constants.SQLITE_OPEN_READONLY);
   }

   private SQLiteDatabase androidDatabase;

   @Override public void setLocalDatabase(SQLiteDatabase localDatabase) { androidDatabase = localDatabase; }

   /**
    * @param where - Any extra WHERE clauses. Note the where can contain parameters prefixed with a : (single colon)
    *                however the parameters embedded in the clause must contain surrounding quotes etc
    *                as opposed to for example JdbcDataSource/JDBCRequestor single colon parameters, where column types
    *                in the WHERE are inferred by the JDBC PreparedStatement.
    */
   protected void setWhere(String where) { this.where = where; }

   public void configureAnnotationProcessor(IAnnotationProcessor processor, Object instance)
   //---------------------------------------------------------------------------------------
   {
      this.annotationProcessor = processor;
      this.annotatedInstance = instance;
   }

   @Override
   public Future<?> boundingBox(double centerLatitude, double centerLongitude, double width, double height,
                                ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token,
                                ISpatialQueryResult callback)
   //------------------------------------------------------------------------------------------------------
   {
      if (db == null)
         throw new RuntimeException(sourceName + ": SpatiaLite database not opened");
      final String whereColumn = onGetSpatialWhereColumn();
      if ( (whereColumn == null) || (whereColumn.trim().isEmpty()) )
         throw new RuntimeException("Spatial selection column not specified");
      String spatialWhere = spatialFunctions.inRectangle(whereColumn, centerLatitude - height / 2,
                                                         centerLongitude - width / 2,
                                                         centerLatitude + height / 2, centerLongitude + width / 2);
      String sql = select(spatialWhere, spatialFunctions);
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
   //-------------------------------------------------------------------------------------------------------------------
   {
      if (db == null)
         throw new RuntimeException(sourceName + ": SpatiaLite database not opened");
      final String whereColumn = onGetSpatialWhereColumn();
      if ( (whereColumn == null) || (whereColumn.trim().isEmpty()) )
         throw new RuntimeException("Spatial selection column not specified");
      String spatialWhere = spatialFunctions.inCircle(whereColumn, centerLatitude, centerLongitude, radius);
      String sql = select(spatialWhere, spatialFunctions);
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
                                 ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token,
                                 ISpatialQueryResult callback)
   //-------------------------------------------------------------------------------------------------
   {
      if (db == null)
         throw new RuntimeException(sourceName + ": SpatiaLite database not opened");
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

      String sql = select(null, spatialFunctions);
      if (annotationProcessor != null)
         annotationProcessor.processParameters(annotatedInstance, parameters);

      if (where.indexOf(":location") > 0)
         parameters.put("location", spatialFunctions.stringToSpatial(String.format("POINT(%.7f %.7f)",
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
   //--------------------------
   {
      final String whereColumn = onGetSpatialWhereColumn();
      String spatialWhere = null;
      if ( (whereColumn != null) && (! whereColumn.trim().isEmpty()) )
      {
         switch (lastOp)
         {
            case RADIUS:
               spatialWhere = spatialFunctions.inCircle(whereColumn, lastLatitude, lastLongitude, lastRadius);
               break;
            case BOUNDING_BOX:
               spatialWhere = spatialFunctions.inRectangle(whereColumn, lastLatitude, lastLongitude, lastWidth,
                                                           lastHeight);
               break;
         }
      }
      return select(spatialWhere, spatialFunctions);
   }

   private Map<String, int[]> paramPositionMap = new HashMap<>();

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
            int[] count = new int[1];
            JdbcRequestHandler.parseParameters(nowhere, paramPositionMap, count);
            if (count[0] > 0)
            {
               for (String param : paramPositionMap.keySet())
               {
                  WhereParameterProperties prop = whereParameters.get(param);
                  if (prop == null)
                     prop = whereParameters.get(":" + param);
                  if ( (prop != null) && (prop.isTextToSpatial) )
                     nowhere = nowhere.replaceAll(":" + param, spatialFunctions.stringToSpatial("?"));
                  else
                     nowhere = nowhere.replaceAll(":" + param, "?");
               }
            }
            final boolean isStartAnd = (Util.startWithWordPattern("AND").matcher(where).matches());
            final boolean isStartOr = (Util.startWithWordPattern("OR").matcher(where).matches());
            return nowhere + (((isStartAnd) || (isStartOr) ) ? where : " AND " + where);
         }
      }
   }

   protected Future<?> executeQuery(final String query, final Map<String, Object> parameters,
                                    ISpatialQueryResult.CALLBACK_TYPE callbackType, final Anything token,
                                    final ISpatialQueryResult callback, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------
   {
      final CursorCallback queryCallback = new CursorCallback(sourceName, callbackType, token, projectionColumns,
                                                              projectionTypes, callback,
                                                              annotationProcessor, annotatedInstance);
      return activeObject.scheduleWithFuture(new QueryThread(query, parameters, token, queryCallback));
   }

   class QueryThread implements Callable<Cursor>
   //===========================================
   {
      private final String query;
      private final Anything token;
      private final Map<String, Object> parameters;
      private final CursorCallback callback;

      QueryThread(String query, Map<String, Object> parameters, Anything token, CursorCallback callback)
      //--------------------------------------------------------------------------------
      {
         this.query = query;
         this.token = token;
         this.parameters = parameters;
         this.callback = callback;
      }

      @Override
      public Cursor call() throws Exception
      //------------------------------------
      {
         StringBuilder create = new StringBuilder();
         StringBuilder insert = new StringBuilder();
         StringBuilder select = new StringBuilder();
         String localTableName = sourceName;
         try
         {
            Stmt stmt = db.prepare(query);
            bindParameters(stmt);
            if (! stmt.step())
               return new EmptyCursor();
            QueryMetaData metaData = new QueryMetaData(stmt);
            if (! DatabaseUtils.createLocalSQL(metaData, localTableName, true, true, create, insert, select))
            {
               callback.onError(token, -1, "Error creating local (Android) database copy", null);
               return null;
            }

            synchronized (androidDatabase)
            {
               if (DatabaseUtils.isAndroidTableExists(androidDatabase, localTableName))
                  androidDatabase.execSQL("DROP TABLE " + localTableName);
               androidDatabase.execSQL(create.toString());
            }
            ContentValues values = new ContentValues();
            androidDatabase.beginTransaction();
            try
            {
               Map<String, Integer> names = new HashMap<>();
               do
               {
                  values.clear();
                  names.clear();
                  for (int i = 0; i < metaData.getColumnCount(); i++)
                  {
                     //TODO: Optimize - reuse names
                     String name = metaData.getColumnAlias(i);
                     name = name.replace('?', '_');
                     Integer count = names.get(name);
                     if (count != null)
                     {
                        count++;
                        names.put(name, count);
                        name = name + count;
                     }
                     else
                        names.put(name, 0);

                     switch (metaData.getColumnType(i))
                     {
                        case Types.VARCHAR:
                           String v = stmt.column_string(i);
                           if (v == null)
                              values.putNull(name);
                           else
                              values.put(name, v);
                           break;
                        case Types.BIGINT:   values.put(name, stmt.column_long(i)); break;
                        case Types.BLOB:     values.put(name, stmt.column_bytes(i)); break;
                        case Types.DOUBLE:   values.put(name, stmt.column_double(i)); break;
                        case Types.NULL:     values.putNull(name); break;
                        default:             values.put(name, stmt.column(i).toString());
                     }
                  }
                  androidDatabase.insert(localTableName, null, values);
               } while(stmt.step());
               androidDatabase.setTransactionSuccessful();
            }
            finally
            {
               androidDatabase.endTransaction();
            }
            Cursor cursor = androidDatabase.rawQuery(select.toString(), null);
            callback.onQueried(token, cursor, 0);
            return cursor;
         }
         catch (Exception e)
         {
            Log.e(TAG, "", e);
            callback.onError(token, -1, e.getMessage(), e);
            return null;
         }
      }

      private void bindParameters(Stmt stmt) throws SpatialiteException
      //------------------------------------
      {
         for (String param : paramPositionMap.keySet())
         {
            WhereParameterProperties prop = whereParameters.get(param);
            if (prop == null)
               prop = whereParameters.get(":" + param);
            if (prop != null)
            {
               int[] apos = paramPositionMap.get(param);
               if ( (apos != null) && (apos.length > 0) )
               {
                  Object o =  parameters.get(prop.name);
                  if (o == null)
                  {
                     for (int pos : apos)
                        stmt.bind(pos);
                  }
                  else
                  {
                     String liteType = DatabaseUtils.sqlLiteType(prop.sqlType);
                     switch (liteType)
                     {
                        case "TEXT":
                           for (int pos : apos)
                           {
                              String val;
                              try
                              {
                                 val = (String) o;
                              }
                              catch (Exception _e)
                              {
                                 try { val = o.toString(); } catch (Exception _ee) { val = null; }
                              }
                              if (val != null)
                                 stmt.bind(pos, val);
                           }
                           break;
                        case "INTEGER":
                           for (int pos : apos)
                           {
                              Integer val;
                              try
                              {
                                 val = (Integer) o;
                              }
                              catch (Exception _e)
                              {
                                 try { val = Integer.parseInt(o.toString().trim()); } catch (Exception _ee) { val = null; }
                              }
                              if (val != null)
                                 stmt.bind(pos, val);
                           }
                           break;

                        case "BLOB":
                           for (int pos : apos)
                           {
                              byte[] val;
                              try { val = (byte[]) o; } catch (Exception _e) { val = null; }
                              if (val != null)
                                 stmt.bind(pos, val);
                           }
                           break;

                        case  "REAL":
                           for (int pos : apos)
                           {
                              Number val;
                              try
                              {
                                 val = (Number) o;
                              }
                              catch (Exception _e)
                              {
                                 try { val = Double.parseDouble(o.toString().trim()); } catch (Exception _ee) { val = null; }
                              }
                              if (val != null)
                                 stmt.bind(pos, val.doubleValue());
                           }
                           break;
                        case "NUMERIC":
                           for (int pos : apos)
                           {
                              BigDecimal val;
                              try
                              {
                                 val = (BigDecimal) o;
                              }
                              catch (Exception _e)
                              {
                                 try { val = new BigDecimal(o.toString().trim()); } catch (Exception _ee) { val = null; }
                              }
                              if (val != null)
                                 stmt.bind(pos, val.doubleValue());
                           }
                           break;
                     }
                  }
               }
            }
         }
      }
   }

}
