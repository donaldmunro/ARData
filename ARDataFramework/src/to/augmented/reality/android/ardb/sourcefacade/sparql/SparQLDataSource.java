package to.augmented.reality.android.ardb.sourcefacade.sparql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;
import to.augmented.reality.android.ardb.util.Util;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;
import to.augmented.reality.android.ardb.http.Cache;
import to.augmented.reality.android.ardb.http.HTTP_METHOD;
import to.augmented.reality.android.ardb.http.MIME_TYPES;
import to.augmented.reality.android.ardb.http.sparql.SparQLDialect;
import to.augmented.reality.android.ardb.http.sparql.SparQLRequestor;
import to.augmented.reality.android.ardb.sourcefacade.CursorCallback;
import to.augmented.reality.android.ardb.sourcefacade.DataType;
import to.augmented.reality.android.ardb.sourcefacade.IAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialQueryResult;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.ColumnContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class SparQLDataSource implements ISpatialSource
//===================================================
{
   static final private String TAG = SparQLDataSource.class.getName();

   static final private List<String> DEFAULT_PREFIXES = new ArrayList<String>()
   {{
      add("PREFIX fn: <http://www.w3.org/2005/xpath-functions#>");
      add("PREFIX owl: <http://www.w3.org/2002/07/owl#>");
//      add("PREFIX par: <http://parliament.semwebcentral.org/parliament#>");
      add("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
      add("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>");
      add("PREFIX time: <http://www.w3.org/2006/time#>");
      add("PREFIX sf: <http://www.opengis.net/ont/sf#>");
      add("PREFIX xml: <http://www.w3.org/XML/1998/namespace>");
      add("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>");
      add("PREFIX geo: <http://www.opengis.net/ont/geosparql#>");
      add("PREFIX geof: <http://www.opengis.net/def/function/geosparql/>");
      add("PREFIX gml: <http://www.opengis.net/ont/gml#>");
      add("PREFIX units: <http://www.opengis.net/def/uom/OGC/1.0/>");
      add("PREFIX foaf: <http://xmlns.com/foaf/0.1/>");
   }};

   private static final String PARAMETER_DELIMETER = "::";

   protected String sourceName;
   protected final Context context;

   protected HTTP_METHOD httpMethod = HTTP_METHOD.GET;
   public void setMethod(HTTP_METHOD method) { this.httpMethod = method; }

   protected URI endpoint;
   public void setEndpoint(String endpoint) throws URISyntaxException { this.endpoint = new URI(endpoint); }
   public void setEndpoint(URI uri) { this.endpoint = uri; }
   protected String userAgent = "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0";
   public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

   final protected Map<String, String> headers = new HashMap<>();
   public void addHeader(String k, String v) { headers.put(k, v); }
   protected long connectionTimeout = 60, readTimeout = 60;
   protected TimeUnit connectionTimeoutUnit = TimeUnit.SECONDS, readTimeoutUnit = TimeUnit.SECONDS;
   protected Cache httpCache = null;
   protected final AtomicBoolean mustAbort = new AtomicBoolean(false);

   protected SparQLRequestor requestor;
   protected Map<String, Object> parameters;
   protected DataType[] projectionTypes;

   final private List<String> prefixes = new ArrayList<>();
   final private List<String> defaultGraphUris = new ArrayList<>();
   final private List<String> namedGraphUris = new ArrayList<>();
   protected String select = null, where = null, orderBy = null, limit = null, offset = null, groupBy = null, having = null;
   protected List<String> from;
   protected String spatialWhereSubjectName = null;

   protected IAnnotationProcessor annotationProcessor;
   protected Object annotatedInstance = null;
   final private List<ColumnContext> annotationColumns = new ArrayList<>();
   protected  ISpatialSource.SPATIAL_OPERATION lastOp = null;
   protected double lastLatitude =0, lastLongitude =0, lastRadius =0, lastWidth =0, lastHeight =0;

   //TODO: Add further parse support for JSON or Turtle
   final private MIME_TYPES encoding = MIME_TYPES.SPARQL_XML;
   protected SQLiteDatabase localDatabase = null;

   public SparQLDataSource(String sourceName, Context context, SparQLDialect dialect, ActiveObject ao)
   //--------------------------------------------------------------------------------------------------------
   {
      this(sourceName, context, dialect, ao, null, null);
   }

   public SparQLDataSource(String sourceName, Context context, SparQLDialect dialect, ActiveObject ao,
                           IAnnotationProcessor processor, Object annotationInstance)
   //----------------------------------------------------------------------------------------------------------
   {
      this.sourceName = sourceName;
      this.context = context;
      this.annotationProcessor = processor;
      this.annotatedInstance = annotationInstance;
      requestor = onGetRequestor(ao, dialect);
      setPrefixes(DEFAULT_PREFIXES.toArray(new String[DEFAULT_PREFIXES.size()]));
   }

   public void setHttpCache(File cacheDir, boolean isOverideNoCache, boolean isOverideCacheAge)
         throws FileNotFoundException
   //------------------------------------------------------------------------------------------
   {
      httpCache = new Cache(5, TimeUnit.MINUTES, cacheDir,isOverideNoCache, isOverideCacheAge);
   }

   public void setHttpCache(Cache cache) { httpCache =cache; }

   public void setConnectionTimeout(long connectionTimeout, TimeUnit connectionTimeoutUnit)
   //--------------------------------------------------------------------------------------
   {
      this.connectionTimeout = connectionTimeout;
      this.connectionTimeoutUnit = connectionTimeoutUnit;
   }

   public void setReadTimeout(long readTimeout, TimeUnit readTimeoutUnit)
   //--------------------------------------------------------------------------------------
   {
      this.readTimeout = readTimeout;
      this.readTimeoutUnit = readTimeoutUnit;
   }

   public void setSpatialWhereSubjectName(String name) { this.spatialWhereSubjectName = name; }

   public void setPrefixes(String[] prefixes)
   //----------------------------------------
   {
      this.prefixes.clear();
      if ( (prefixes == null) || (prefixes.length == 0) )
         return;
      for (String prefix : prefixes)
         this.prefixes.add(prefix);
   }

   public void addPrefix(String[] prefixes)
   //------------------------------------
   {
      if (prefixes == null) return;
      for (String prefix : prefixes)
         this.prefixes.add(prefix);
   }

   public boolean removePrefix(String prefixOrPartof)
   //------------------------------------------------
   {
      if (! prefixes.remove(prefixOrPartof))
      {
         for (String prefix : prefixes)
         {
            if (prefix.toLowerCase().contains(prefixOrPartof.toLowerCase()))
               return prefixes.remove(prefix);
         }
      }
      return false;
   }

   public String[] getPrefixes() { return prefixes.toArray(new String[0]); }

   public void setDefaultGraphUris(String[] uris)
   //----------------------------------------
   {
      this.defaultGraphUris.clear();
      if ( (uris == null) || (uris.length == 0) )
         return;
      for (String uri : uris)
         this.defaultGraphUris.add(uri);
   }

   public void addDefaultGraphUri(String uri) { this.defaultGraphUris.add(uri); }

   public boolean removeDefaultGraphUri(String uriOrPartof)
   //------------------------------------------------
   {
      if (! defaultGraphUris.remove(uriOrPartof))
      {
         for (String uri : defaultGraphUris)
         {
            if (uri.toLowerCase().contains(uriOrPartof.toLowerCase()))
               return defaultGraphUris.remove(uri);
         }
      }
      return false;
   }

   public String[] getDefaultGraphUris() { return defaultGraphUris.toArray(new String[0]); }

   public void setNamedGraphUris(String[] uris)
   //----------------------------------------
   {
      this.namedGraphUris.clear();
      for (String uri : uris)
         this.namedGraphUris.add(uri);
   }

   public void addNamedGraphUri(String uri) { this.namedGraphUris.add(uri); }

   public boolean removeNamedGraphUri(String uriOrPartof)
   //------------------------------------------------
   {
      if (! namedGraphUris.remove(uriOrPartof))
      {
         for (String uri : namedGraphUris)
         {
            if (uri.toLowerCase().contains(uriOrPartof.toLowerCase()))
               return namedGraphUris.remove(uri);
         }
      }
      return false;
   }

   public String[] getNamedGraphUris() { return namedGraphUris.toArray(new String[0]); }

   public void addAnnotationColumn(String column) { addAnnotationColumn(column, null); }

   public void addAnnotationColumn(String column, String alias)
   //----------------------------------------------------------
   {
      this.annotationColumns.add(new ColumnContext(column, alias));
   }

   public boolean removeAnnotationColumn(String column)
   //--------------------------------------------------
   {
      for (ColumnContext cc : annotationColumns)
      {
         if (cc.getName().toLowerCase().equals(column.toLowerCase()))
            return annotationColumns.remove(cc);
      }
      return false;
   }

   public ColumnContext[] getAnnotationColumns() { return annotationColumns.toArray(new ColumnContext[0]); }

   public void setSelect(String select) { this.select = select; }

   public String getSelect() { return select; }

   /**
    * @param where - Any extra SPARQL WHERE clauses. Note the where can contain parameters prefixed with a :: (double colon)
    *                however the parameters embedded in the clause must contain surrounding quotes, SPARQL suffixes etc
    *                as opposed to for example JDBCRequestor single colon parameters, where column types in the WHERE
    *                are inferred by the JDBC PreparedStatement.
    */
   public void setWhere(String where) { this.where = where; }

   public String getWhere() { return where; }

   public void setOrderBy(String orderBy) { this.orderBy = orderBy; }

   public String getOrderBy() { return orderBy; }

   public void setLimit(String limit) { this.limit = limit; }

   public String getLimit() { return limit; }

   public void setOffset(String offset) { this.offset = offset; }

   public String getOffset() { return offset; }

   public void setGroupBy(String groupBy) { this.groupBy = groupBy; }

   public String getGroupBy() { return groupBy; }

   public void setHaving(String having) { this.having = having; }

   public String getHaving() { return having; }

   public void setFrom(String... froms)
   //----------------------------------
   {
      if (froms == null)
      {
         from = null;
         return;
      }
      from = new ArrayList<>(froms.length);
      for (String from : froms)
         this.from.add(from);
   }

   public String[] getFrom()
   //-----------------------
   {
      if (from == null)
         return new String[0];
      return from.toArray(new String[from.size()]);
   }

   public void addFrom(String from)
   //-------------------------------
   {
      if (this.from == null)
         this.from = new ArrayList<>();
      this.from.add(from);
   }

   @Override
   public String getLastQuery()
   //--------------------------
   {
      final String whereSubject = onGetSpatialWhereSubject();
      String[] appendFilters = null;
      if ( (lastOp != null) && (whereSubject != null) || (! whereSubject.trim().isEmpty()) )
      {
         appendFilters = new String[1];
         switch (lastOp)
         {
            case BOUNDING_BOX:
               appendFilters[0] = requestor.spatialFunctions().inRectangle(whereSubject,
                                                                           lastLatitude - lastHeight / 2,
                                                                           lastLongitude - lastWidth / 2,
                                                                           lastLatitude + lastHeight / 2,
                                                                           lastLongitude + lastWidth / 2);
               break;
            case RADIUS:
               appendFilters[0] = requestor.spatialFunctions().inCircle(whereSubject, lastLatitude, lastLongitude, lastRadius);
               break;
         }
      }
      Map<String, Object> params = null;
      if (annotationProcessor != null)
         params = annotationProcessor.processParameters(annotatedInstance, null);
      return getQuery(appendFilters, params);
   }

   protected String onPreProcessWhere(String[] appendFilters, Map<String, Object> params)
   //----------------------------------------------------------------------------------------
   {
      String modifiedWhere = "", v;
      if ( (where == null) || (where.trim().isEmpty()) )
         modifiedWhere = " WHERE { }";
      else
      {
         if (Util.startWithWordPattern("WHERE").matcher(where).matches())
            modifiedWhere = where;
         else
            modifiedWhere = "WHERE " + where;

      }
      if ( (appendFilters != null) && (appendFilters.length > 0) )
      {
         int p = modifiedWhere.lastIndexOf('}');
         if (p >= 0)
         {
            StringBuilder sb = new StringBuilder(modifiedWhere.substring(0, p)).append('\n');
            for (String filter : appendFilters)
               sb.append(filter).append('\n');
            sb.append(" }");
            modifiedWhere = sb.toString();
         }
      }
      if (params != null)
      {
         for (String name : params.keySet())
         {
            String paramname;
            if (name.startsWith(PARAMETER_DELIMETER))
               paramname = name;
            else
               paramname = PARAMETER_DELIMETER + name;
            if (modifiedWhere.contains(paramname))
            {
               Object o = params.get(name);
               try { v = (String) o; }
               catch (ClassCastException _e)
               {
                  try { v = o.toString(); } catch (Exception _ee) { v = null; }
               }
               if (v != null)
                  modifiedWhere = modifiedWhere.replace(paramname, v);
            }
         }
      }

      if (modifiedWhere.equals(" WHERE { }"))
         return "";
      return modifiedWhere;
   }

   protected String getQuery(String[] appendFilters, Map<String, Object> params)
   //-------------------------------------------------------------------------------
   {
      StringBuilder queryBuf = new StringBuilder();
      if ( (prefixes != null) && (prefixes.size() > 0) )
      {
         for (String prefix : prefixes)
            queryBuf.append(prefix).append((prefix.endsWith("\n")) ? "" : "\n");
      }
      if (! Util.startWithWordPattern("SELECT").matcher(select).matches())
         queryBuf.append("SELECT ");
      queryBuf.append(select).append((select.endsWith("\n")) ? "" : "\n");
      if ( (from != null) && (from.size() > 0) )
      {
         for (String from : this.from)
         {
            if ( (from != null) && (from.trim().length() > 0) )
            {
               if (! Util.startWithWordPattern("FROM").matcher(from).matches())
                  queryBuf.append("FROM ");
               queryBuf.append(from).append('\n');
            }
         }
      }
      queryBuf.append(onPreProcessWhere(appendFilters, params)).append('\n');
      if ( (groupBy != null) && (! groupBy.trim().isEmpty()) )
      {
         if (! Util.startWithWordPattern("GROUP BY").matcher(groupBy).matches())
            queryBuf.append("GROUP BY ");
         queryBuf.append(groupBy).append('\n');
         if ( (having != null) && (! having.trim().isEmpty()) )
         {
            if (! Util.startWithWordPattern("HAVING").matcher(having).matches())
               queryBuf.append("HAVING ");
            queryBuf.append(having).append('\n');
         }
      }
      else if ( (orderBy != null) && (! orderBy.trim().isEmpty()) )
      {
         if (! Util.startWithWordPattern("ORDER BY").matcher(orderBy).matches())
            queryBuf.append("ORDER BY ");
         queryBuf.append(orderBy).append('\n');
      }
      if ( (limit != null) && (! limit.trim().isEmpty()) )
      {
         if (! Util.startWithWordPattern("LIMIT").matcher(limit).matches())
            queryBuf.append("LIMIT ");
         queryBuf.append(limit).append('\n');
      }
      if ( (offset != null) && (! offset.trim().isEmpty()) )
      {
         if (! Util.startWithWordPattern("OFFSET").matcher(offset).matches())
            queryBuf.append("OFFSET ");
         queryBuf.append(offset);
      }
      return queryBuf.toString();
   }


   protected SparQLRequestor onGetRequestor(ActiveObject ao, SparQLDialect dialect) { return new SparQLRequestor(ao, dialect); }

   protected String onGetSpatialWhereSubject() { return spatialWhereSubjectName; }

   @Override public String getName() { return sourceName; }

   @Override public void setLocalDatabase(SQLiteDatabase androidDatabase) { localDatabase = androidDatabase; }

   @Override public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

   @Override public void setProjectionTypes(DataType[] projectionTypes) { this.projectionTypes = projectionTypes; }

   @Override
   public Future<?> boundingBox(double centerLatitude, double centerLongitude, double width, double height,
                                ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback)
   //-------------------------------------------------------------------------------------------------------------------
   {
      final String whereSubject = onGetSpatialWhereSubject();
      if ( (whereSubject == null) || (whereSubject.trim().isEmpty()) )
         throw new RuntimeException("Spatial selection column not specified");
      String[] appendFilters = new String[1];
      appendFilters[0] = requestor.spatialFunctions().inRectangle(whereSubject,
                                                                  centerLatitude - height / 2,
                                                                  centerLongitude - width / 2,
                                                                  centerLatitude + height / 2,
                                                                  centerLongitude + width / 2);
      StringBuilder errbuf = new StringBuilder();
      if (annotationProcessor != null)
         parameters = annotationProcessor.processParameters(annotatedInstance, null);
      String query = getQuery(appendFilters, parameters);
      token.put("query", query);
      Future<?> future = executeQuery(query, parameters, null, callbackType, token, callback, errbuf);
      if (future == null)
      {
         Log.e(TAG, "SparqlDataSource.boundingBox: Error executing SparQL Query: " + errbuf.toString());
         return null;
      }
      lastLatitude = centerLatitude;
      lastLongitude = centerLongitude;
      lastWidth = width;
      lastHeight = height;
      lastOp = SPATIAL_OPERATION.BOUNDING_BOX;
      return future;
   }

   @Override
   public Future<?> radius(double centerLatitude, double centerLongitude, double radius,
                           ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token, ISpatialQueryResult callback)
   //--------------------------------------------------------------------------------------------------------------
   {
      final String whereSubject = onGetSpatialWhereSubject();
      if ( (whereSubject == null) || (whereSubject.trim().isEmpty()) )
         throw new RuntimeException("Spatial selection column not specified");
      String[] appendFilters = new String[1];
      appendFilters[0] = requestor.spatialFunctions().inCircle(whereSubject, centerLatitude, centerLongitude, radius);
      StringBuilder errbuf = new StringBuilder();
      if (annotationProcessor != null)
         parameters = annotationProcessor.processParameters(annotatedInstance, null);
      String query = getQuery(appendFilters, parameters);
      token.put("query", query);
      Future<?> future = executeQuery(query, parameters, null, callbackType, token, callback, errbuf);
      if (future == null)
      {
         Log.e(TAG, "SparqlDataSource.radius: Error executing SparQL Query: " + errbuf.toString());
         return null;
      }
      lastLatitude = centerLatitude;
      lastLongitude = centerLongitude;
      lastOp = SPATIAL_OPERATION.RADIUS;
      lastRadius = radius;
      return future;
   }

   @Override
   public Future<?> spatialQuery(double latitude, double longitude, ImmutableAnything extraParameters,
                                 ISpatialQueryResult.CALLBACK_TYPE callbackType, Anything token,
                                 ISpatialQueryResult callback)
   //---------------------------------------------------------------------------------------------------------
   {
      String latParam = PARAMETER_DELIMETER + "latitude";
      String longParam = PARAMETER_DELIMETER + "longitude";
      String err = "spatialQuery: Where clause " + where + " does not contain any paramaters named " + latParam +
                   " and " + longParam;
      if (where == null)
      {
         callback.onError(sourceName, token, err, null);
         return null;
      }
      if ( (where.indexOf(latParam) < 0) || (where.indexOf(longParam) < 0) )
      {
         callback.onError(sourceName, token, err, null);
         return null;
      }
      if (parameters == null)
         parameters = new HashMap<>();
      parameters.put("latitude", Double.toString(latitude));
      parameters.put("longitude", Double.toString(longitude));
      StringBuilder errbuf = new StringBuilder();
      if (annotationProcessor != null)
         parameters = annotationProcessor.processParameters(annotatedInstance, null);
      String query = getQuery(null, parameters);
      token.put("query", query);
      Future<?> future = executeQuery(query, parameters, null, callbackType, token, callback, errbuf);
      if (future == null)
      {
         Log.e(TAG, "SparqlDataSource.spatialQuery: Error executing SparQL Query: " + errbuf.toString());
         return null;
      }
      lastLatitude = latitude;
      lastLongitude = longitude;
      lastOp = SPATIAL_OPERATION.USER_DEFINED;
      return future;
   }

   protected Future<?> executeQuery(final String query, final Map<String, Object> parameters,
                                    ICursorQueryCallback queryCallback,
                                    ISpatialQueryResult.CALLBACK_TYPE callbackType, final Anything token,
                                    final ISpatialQueryResult callback, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------
   {
      if (localDatabase == null)
      {
         Log.e(TAG, "SparQLDataSource needs to be supplied with a local Android database. Use ARDataSources.setLocalDatabase");
         throw new RuntimeException("SparQLDataSource needs to be supplied with a local Android database. Use ARDataSources.setLocalDatabase");
      }
      final ColumnContext[] projectionColumns = getAnnotationColumns();
      if (queryCallback == null)
         queryCallback = new CursorCallback(sourceName, callbackType, token, projectionColumns, projectionTypes,
                                            callback, annotationProcessor, annotatedInstance);
      Anything params = new Anything();
      params.put("query", query);
      if (! defaultGraphUris.isEmpty())
      {
         Anything uris = new Anything();
         uris.addStrings(defaultGraphUris);
         params.put("defaultGraphUris", uris);
      }
      if (! namedGraphUris.isEmpty())
      {
         Anything uris = new Anything();
         uris.addStrings(namedGraphUris);
         params.put("namedGraphUris", uris);
      }
      return requestor.request(httpMethod, endpoint, userAgent, encoding, headers, params, connectionTimeout,
                               connectionTimeoutUnit, readTimeout, readTimeoutUnit, httpCache, localDatabase, sourceName,
                               queryCallback, token, mustAbort, errbuf);
   }
}
