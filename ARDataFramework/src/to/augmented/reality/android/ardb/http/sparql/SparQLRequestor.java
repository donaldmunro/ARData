package to.augmented.reality.android.ardb.http.sparql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.spi.ISpatialFunctions;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;
import to.augmented.reality.android.ardb.http.Cache;
import to.augmented.reality.android.ardb.http.HTTP_METHOD;
import to.augmented.reality.android.ardb.http.HttpRequestor;
import to.augmented.reality.android.ardb.http.HttpRequestorThread;
import to.augmented.reality.android.ardb.http.HttpUtil;
import to.augmented.reality.android.ardb.http.IHttpRequestorCallback;
import to.augmented.reality.android.ardb.http.MIME_TYPES;
import to.augmented.reality.android.ardb.http.sparql.parsers.Parseable;
import to.augmented.reality.android.ardb.http.sparql.parsers.XMLParse;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;
import to.augmented.reality.android.ardb.jdbc.ISpatialFunctionProvider;

import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SparQLRequestor extends HttpRequestor implements ISparqlRequestor, ISpatialFunctionProvider
//===============================================================================================
{
   private static final String TAG = SparQLRequestor.class.getName();
   ISpatialFunctions spatialFunctions;

   public SparQLRequestor(SparQLDialect dialect) { this(null, dialect); }

   public SparQLRequestor(ActiveObject activeObject, SparQLDialect dialect)
   //---------------------------------------------------------------------
   {
      super(activeObject);
      switch (dialect)
      {
         case GEOSPARQL:   spatialFunctions = new GeoSparQLSpatialFunctions(); break;
         case VIRTUOSO:    spatialFunctions = new VirtuosoSpatialFunctions(); break;
         case SPARQL:      spatialFunctions = new SparQLSpatialFunctions(); break;
         case STRABON:     throw new RuntimeException("Strabon not yet implemented");
      }
   }

   /**
    * @param method HTTP method for request
    * @param endpoint SPARQL endpoint url
    * @param userAgent User agent to send in request header (defaults to
    *                  'Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0' if <i>userAgent</i>
    *                  is empty or null unless <i>header</i> contains a 'User-Agent' key)
    * @param header Other HTTP request headers to send to server.
    * @param parameters <i>SparQLRequestor</i> specific parameters:<br>
    *                   <ul>
    *                   <li>query - The SPARQL query to send to server. Mandatory</li>
    *                   <li>defaultGraphUris - SPARQL default graph URIs. Can be an Anything list of string or URI
    *                   or a single string/uri</li>
    *                   <li>namedGraphUris - SPARQL named graph URIs. Can be a Anything list of string or URI
    *                   or a single string/uri</li>
    *                   </ul>
    * @param connectionTimeout Connection timeout
    * @param connectionTimeoutUnit Time unit for <i>connectionTimeout</i>
    * @param readTimeout Read time out.
    * @param readTimeoutUnit Time unit for <i>readTimeout</i>
    * @param cacheInfo A Cache instance specifying caching parameters or null for no caching. Use the same
    *                  Cache instance (or at least the same information in the Cache) when executing multiple
    *                  calls.
    * @param callback The asynchronous callback to invoke on completion or when an error occurs.
    * @param token A token which can be used to identify the request when the callback is invoked.
    * @param errbuf If the request fails before the HTTP request is invoked then errbuf should contain a descriptive
    *               error message
    * @return A <i>Future</i> for the concurrently executing request (or completed request if concurrency is not
    * specified for the requestor) or <i>null</i> if a request setup error occurred
    */
   @Override
   public Future<?> request(HTTP_METHOD method, URI endpoint, String userAgent, MIME_TYPES encoding,
                            Map<String, String> header, ImmutableAnything parameters, long connectionTimeout,
                            TimeUnit connectionTimeoutUnit, long readTimeout, TimeUnit readTimeoutUnit, Cache cacheInfo,
                            SQLiteDatabase localDatabase, String tableName, final ICursorQueryCallback callback,
                            Anything token, AtomicBoolean mustAbort, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------
   {
      Future<Integer> requestFuture;
      Future<Cursor> parseFuture = null;
      if (errbuf == null)
         errbuf = new StringBuilder();
      try
      {
         if (callback == null)
         {
            errbuf.append("Synchronous call to ").append(this.getClass().getName()).append("with null callback");
            Log.e(TAG, errbuf.toString());
            throw new RuntimeException(errbuf.toString());
         }
         if (activeObject == null)
         {
            errbuf.append("Synchronous call to ").append(this.getClass().getName()).append( " with no active object scheduler");
            Log.e(TAG, errbuf.toString());
            throw new RuntimeException(errbuf.toString());
         }
         Parseable parser;
         switch (encoding)
         {
            case SPARQL_XML: parser = new XMLParse(); break;
            default: throw new RuntimeException("Only XML SparQL result format (application/sparql-results+xml) supported");
         }

         final PipedOutputStream pos = new PipedOutputStream();
         final PipedInputStream pis = new PipedInputStream(pos, 32768);
         if (mustAbort == null)
            mustAbort = new AtomicBoolean(false);
         if (header == null)
            header = new HashMap<>();
         //Passing null as callback to HttpRequestorThread bacause callback is called in SparQLCursorThread when parsing is complete.
         HttpRequestorThread t = createThread(method, endpoint, userAgent, encoding, header, parameters,
                                              connectionTimeout,
                                              connectionTimeoutUnit, readTimeout, readTimeoutUnit, pos, cacheInfo,
          new IHttpRequestorCallback()
          //==========================
          {
             @Override public void onResponse(Anything token, int code) { }
             @Override
             public void onError(Anything token, int code, CharSequence message,
                                 Throwable exception)
             //------------------------------------------------------------------
             {
                if (callback != null)
                   callback.onError(token, code, "SparQLRequestor.request: HTTP request returned " + code, null);
             }
          },
                                              token, mustAbort, errbuf);
         requestFuture = (Future<Integer>) activeObject.scheduleWithFuture(t);
         SparQLCursorThread cursorThread = new SparQLCursorThread(t, pis, pos, parser, localDatabase, tableName,
                                                                  callback, token, mustAbort, requestFuture, errbuf);
         parseFuture = (Future<Cursor>) activeObject.scheduleWithFuture(cursorThread);
      }
      catch (Exception e)
      {
         errbuf.append(e.getMessage());
      }
      return parseFuture;
   }

   @Override
   public Cursor request(HTTP_METHOD method, URI endpoint, String userAgent, MIME_TYPES encoding,
                         Map<String, String> header, ImmutableAnything parameters, long connectionTimeout,
                         TimeUnit connectionTimeoutUnit, long readTimeout, TimeUnit readTimeoutUnit, Cache cacheInfo,
                         SQLiteDatabase localDatabase, String tableName, AtomicBoolean mustAbort, StringBuilder errbuf)
   //--------------------------------------------------------------------------------------------------------
   {
      ExecutorService executor = null;
      Future<Integer> requestFuture;
      try
      {
         Parseable parser;
         switch (encoding)
         {
            case SPARQL_XML: parser = new XMLParse(); break;
            default: throw new RuntimeException("Only XML SparQL result format (application/sparql-results+xml) supported");
         }

         final PipedOutputStream pos = new PipedOutputStream();
         final PipedInputStream pis = new PipedInputStream(pos, 32768);
         if (mustAbort == null)
            mustAbort = new AtomicBoolean(false);
         if (header == null)
            header = new HashMap<>();
         //Passing null as callback to HttpRequestorThread bacause callback is called in SparQLCursorThread when parsing is complete.
         HttpRequestorThread t = createThread(method, endpoint, userAgent, encoding, header, parameters, connectionTimeout,
                                              connectionTimeoutUnit, readTimeout, readTimeoutUnit, pos, cacheInfo,
                                              null, null, mustAbort, errbuf);
         executor = Executors.newSingleThreadExecutor(new ThreadFactory()
         {
            @Override public Thread newThread(Runnable r)
            //---------------------------------------------
            {
               Thread t = new Thread(r);
               t.setDaemon(true);
               t.setName("SparQLRequestor-Sync");
               return t;
            }
         });
         requestFuture = executor.submit(t);
         SparQLCursorThread cursorThread = new SparQLCursorThread(t, pis, pos, parser, localDatabase, tableName,
                                                                  null, null, mustAbort, requestFuture, errbuf);
         Cursor cursor = cursorThread.call();
         requestFuture.get();
         return cursor;
      }
      catch (Exception e)
      {
         errbuf.append(e.getMessage());
         Log.e(TAG, "", e);
         return null;
      }
   }

   protected HttpRequestorThread createThread(HTTP_METHOD method, URI endpoint, String userAgent, MIME_TYPES encoding,
                                              Map<String, String> header, ImmutableAnything parameters,
                                              long connectionTimeout, TimeUnit connectionTimeoutUnit,
                                              long readTimeout, TimeUnit readTimeoutUnit,
                                              OutputStream outputTo, Cache cacheInfo, IHttpRequestorCallback callback,
                                              Anything token, AtomicBoolean mustAbort, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------
   {
      try
      {
         String query = parameters.getImmutable("query").asString(null);
         if (query == null)
         {
            if (errbuf != null)
               errbuf.append("No 'query' parameter specified in parameters");
            return null;
         }
         if ((userAgent == null) || (userAgent.trim().isEmpty()))
            userAgent = "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0";
         if (! header.containsKey("User-Agent"))
            header.put("User-Agent", userAgent);
         //         if (! header.containsKey("Accept-Charset"))
         header.put("Accept-Charset", "UTF-8");
         if (! header.containsKey("Accept"))
         {
            if (encoding != null)
               header.put("Accept", encoding.toString());
            else
               header.put("Accept", MIME_TYPES.SPARQL_Turtle.toString());
         }
         ImmutableAnything defaultGraphUriList = parameters.getImmutable("defaultGraphUris");
         Object[] defaultGraphUris = defaultGraphUriList.asArray("");
         ImmutableAnything namedGraphUriList = parameters.getImmutable("namedGraphUris");
         Object[] namedGraphUris = namedGraphUriList.asArray("");
         StringBuilder postData = new StringBuilder();
         String[] defGraphUris = strings(defaultGraphUris), nameGraphUris = strings(namedGraphUris);
         Uri uri = makeUri(query, endpoint, defGraphUris, nameGraphUris, method, postData);
         long connTimeoutMs = TimeUnit.MILLISECONDS.convert(connectionTimeout, connectionTimeoutUnit);
         long readTimeoutMs = TimeUnit.MILLISECONDS.convert(readTimeout, readTimeoutUnit);
         return new HttpRequestorThread(method, uri, header, postData, connTimeoutMs, readTimeoutMs, outputTo,
                                        mustAbort, cacheInfo, callback, token);
      }
      catch (Exception e)
      {
         if (errbuf != null)
            errbuf.append(e.getMessage());
         Log.e(TAG, "", e);
         return null;
      }
   }

   private String[] strings(Object[] objects)
   //-------------------------------------------------
   {
      String[] strings = null;
      if ( (objects != null) && (objects.length > 0) )
      {
         strings = new String[objects.length];
         int i = 0;
         for (Object o : objects)
         {
            if (o instanceof String)
               strings[i++] = (String) o;
            else if (o instanceof URI)
               strings[i++] = ((URI) o).toString();
            else
               strings[i++] = "";
         }
      }
      return strings;
   }

   protected Uri makeUri(String query, URI endpoint, String[] defaultGraphUris, String[] namedGraphUris,
                         HTTP_METHOD method, StringBuilder postData)
         throws UnsupportedEncodingException
   //--------------------------------------------------------------------------------
   {
      Uri uri = null;
      Uri.Builder uriBuilder = Uri.parse(endpoint.toString()).buildUpon();
      switch (method)
      {
         case GET:
            if ( (defaultGraphUris != null) && (defaultGraphUris.length > 0) )
               HttpUtil.appendGET(uriBuilder, "default-graph-uri", defaultGraphUris);
            if ( (namedGraphUris != null) && (namedGraphUris.length > 0) )
               HttpUtil.appendGET(uriBuilder, "named-graph-uri", namedGraphUris);
            uriBuilder.appendQueryParameter("query", query);
            uri = uriBuilder.build();
            break;

         case POST:
            uri = uriBuilder.build();
            postData.append("query=").append(URLEncoder.encode(query, "UTF-8"));
            if ( (defaultGraphUris != null) && (defaultGraphUris.length > 0) )
               HttpUtil.appendPOST(postData, "default-graph-uri", defaultGraphUris);
            if ( (namedGraphUris != null) && (namedGraphUris.length > 0) )
               HttpUtil.appendPOST(postData, "named-graph-uri", namedGraphUris);
//            Log.i(LOGTAG, "POST data " + sb.toString());
            break;
      }
      URI U;

      return uri;
   }

   @Override public ISpatialFunctions spatialFunctions() { return spatialFunctions; }
}
