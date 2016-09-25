package to.augmented.reality.android.ardb.http;

import android.net.Uri;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;

import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpRequestor implements IHttpRequestor
//==================================================
{
   private static final String TAG = HttpRequestor.class.getName();

   protected ActiveObject activeObject = null;

   public HttpRequestor() {}

   public HttpRequestor(ActiveObject activeObject) { this.activeObject = activeObject; }

   /**
    * @param method HTTP method for request
    * @param host HTTP host
    * @param userAgent User agent to send in request header (defaults to
    *                  'Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0' if <i>userAgent</i>
    *                  is empty or null unless <i>header</i> contains a 'User-Agent' key)
    * @param encoding The ACCEPT encoding.
    * @param header Other HTTP request headers to send to server.
    * @param parameters <i>HttpRequestor</i> specific parameters:<br>
    *                   <ul>
    *                   <li>data - A Map containg key-value pairs of query parameters/form-encoded pairs to send to
    *                   server in query string (GET) or request body (POST)</li>
    *                   <li>text - true (boolean) or "true" (string) if output is textual eg HTML else false if binary</li>
    *                   </ul>
    * @param connectionTimeout Connection timeout
    * @param connectionTimeoutUnit Time unit for <i>connectionTimeout</i>
    * @param readTimeout Read time out.
    * @param readTimeoutUnit Time unit for <i>readTimeout</i>
    * @param outputTo A Writer instance into which the HTTP response is written.
    * @param cacheInfo A Cache instance specifying caching parameters or null for no caching. Use the same
    *                  Cache instance (or at least the same information in the Cache) when executing multiple
    *                  calls.
    * @param callback The asynchronous callback to invoke on completion or when an error occurs.
    * @param token A token which can be used to identify the request when the callback is invoked.
    * @param mustAbort Setting <i>mustAbort</i> to false in another thread can be used to abort the request. Ignored if
    *                  null
    * @param errbuf If the request fails before the HTTP request is invoked then errbuf should contain a descriptive
    *               error message
    * @return A <i>Future</i> for the concurrently executing request (or completed request if concurrency is not
    * specified for the requestor) or <i>null</i> if a request setup error occurred
    */
   @Override
   public Future<?> request(HTTP_METHOD method, URI host, String userAgent, MIME_TYPES encoding,
                            Map<String, String> header, ImmutableAnything parameters,
                            long connectionTimeout, TimeUnit connectionTimeoutUnit,
                            long readTimeout, TimeUnit readTimeoutUnit, OutputStream outputTo, Cache cacheInfo,
                            IHttpRequestorCallback callback, Anything token, AtomicBoolean mustAbort,
                            StringBuilder errbuf)
   //---------------------------------------------------------------------------------------------------------------
   {
      Future<?> f = null;
      try
      {
         if (callback == null)
         {
            StringBuilder sb = new StringBuilder("Synchronous call to ").append(this.getClass().getName()).
                  append("with null callback");
            Log.e(TAG, sb.toString());
            throw new RuntimeException(sb.toString());
         }
         if (mustAbort != null)
            mustAbort.set(false);
         if (header == null)
            header = new HashMap<>();
         HttpRequestorThread t = createThread(method, host, userAgent, encoding, header, parameters, connectionTimeout,
                                              connectionTimeoutUnit,  readTimeout, readTimeoutUnit, outputTo, cacheInfo,
                                              callback, token, mustAbort, errbuf);
         if (activeObject == null)
         {
            StringBuilder sb = new StringBuilder("Synchronous call to ").append(this.getClass().getName()).
                               append(" with no active object scheduler");
            Log.e(TAG, sb.toString());
            throw new RuntimeException(sb.toString());
         }
         f = activeObject.scheduleWithFuture(t);
      }
      catch (Exception e)
      {
         Log.e(TAG, "", e);
         if (errbuf != null)
            errbuf.append(e.getMessage());
      }
      return f;
   }

   /**
    * Synchronous request method.
    * @param method HTTP method for request
    * @param host Request URI/URL
    * @param userAgent User agent to send in request header (defaults to
    *                  'Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0' if <i>userAgent</i>
    *                  is empty or null unless <i>header</i> contains a 'User-Agent' key)
    * @param encoding The ACCEPT encoding used to specify the mime type which is desired for the response.
    *             An 'Accept' key in <i>header</i> overrides the <i>encoding</i> parameter.
    * @param header Other HTTP request headers to send to server.
    * @param parameters <i>HttpRequestor</i> specific parameters:<br>
    *                   <ul>
    *                   <li>data - A Map containg key-value pairs of query parameters/form-encoded pairs to send to
    *                   server in query string (GET) or request body (POST)</li>
    *                   <li>text - true (boolean) or "true" (string) if output is textual eg HTML else false if binary</li>
    *                   </ul>
    * @param connectionTimeout Connection timeout
    * @param connectionTimeoutUnit Time unit for <i>connectionTimeout</i>
    * @param readTimeout Read time out.
    * @param readTimeoutUnit Time unit for <i>readTimeout</i>
    * @param outputTo A OutputStream derived instance into which the HTTP response is written.
    * @param cacheInfo A Cache instance specifying caching parameters or null for no caching. Use the same
    *                  Cache instance (or at least the same information in the Cache) when executing multiple
    *                  calls.
    * @param mustAbort Setting <i>mustAbort</i> to false in another thread can be used to abort the request. Ignored if
    *                  null
    * @param errbuf If the request fails before the HTTP request is invoked then errbuf should contain a descriptive
    *               error message
    * @return HTTP status code (2XX eg 200 is OK) or -1 if a request setup error occurred
    */
   @Override
   public int request(HTTP_METHOD method, URI host, String userAgent, MIME_TYPES encoding, Map<String, String> header,
                      ImmutableAnything parameters, long connectionTimeout, TimeUnit connectionTimeoutUnit,
                      long readTimeout, TimeUnit readTimeoutUnit, OutputStream outputTo, Cache cacheInfo,
                      AtomicBoolean mustAbort, StringBuilder errbuf)
   //----------------------------------------------------------------------------------------------------------
   {
      try
      {
         if (header == null)
            header = new HashMap<>();
         HttpRequestorThread t = createThread(method, host, userAgent, encoding, header, parameters, connectionTimeout,
                                              connectionTimeoutUnit, readTimeout, readTimeoutUnit, outputTo, cacheInfo,
                                              null, null, mustAbort, errbuf);
         return t.call();
      }
      catch (Exception e)
      {
         Log.e(TAG, "", e);
         if (errbuf != null)
            errbuf.append(e.getMessage());
      }
      return -1;
   }

   protected HttpRequestorThread createThread(HTTP_METHOD method, URI host, String userAgent, MIME_TYPES encoding,
                                              Map<String, String> header, ImmutableAnything parameters,
                                              long connectionTimeout, TimeUnit connectionTimeoutUnit,
                                              long readTimeout, TimeUnit readTimeoutUnit,
                                              OutputStream outputTo, Cache cacheInfo, IHttpRequestorCallback callback,
                                              Anything token, AtomicBoolean mustAbort, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------
   {
      if ((userAgent == null) || (userAgent.trim().isEmpty()))
         userAgent = "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0";
      if (header == null)
         header = new HashMap<>();
      if (! header.containsKey("User-Agent"))
         header.put("User-Agent", userAgent);
      if (! header.containsKey("Accept-Charset"))
         header.put("Accept-Charset", "UTF-8");
      if ( (! header.containsKey("Accept")) && (encoding != null) )
            header.put("Accept", encoding.toString());

      ImmutableAnything data = parameters.getImmutable("data");
      Iterator<Map.Entry<String, Anything>> it = data.mapIterator();
      Uri.Builder uriBuilder = Uri.parse(host.toString()).buildUpon();
      StringBuilder postData = new StringBuilder();
      for (; it.hasNext(); )
      {
         Map.Entry<String, Anything> e = it.next();
         Anything v = e.getValue();
         if (v.isList())
         {
            if (method == HTTP_METHOD.GET)
               HttpUtil.appendGET(uriBuilder, e.getKey(), (String[]) v.asArray(new String[0]));
            else
               try { HttpUtil.appendPOST(postData, e.getKey(), (String[]) v.asArray(new String[0])); } catch (Exception _e) {}
         }
         else
         {
            String s = v.asString(null);
            if (s != null)
            {
               if (method == HTTP_METHOD.GET)
                  uriBuilder.appendQueryParameter(e.getKey(), s);
               else
               {
                  String[] as = new String[1];
                  as[0] = s;
                  try { HttpUtil.appendPOST(postData, e.getKey(), as); } catch (Exception _e) {}
               }
            }
         }
      }
      long connTimeoutMs = TimeUnit.MILLISECONDS.convert(connectionTimeout, connectionTimeoutUnit);
      long readTimeoutMs = TimeUnit.MILLISECONDS.convert(readTimeout, readTimeoutUnit);
      Uri uri = uriBuilder.build();
      return new HttpRequestorThread(method, uri, header, postData, connTimeoutMs, readTimeoutMs, outputTo, mustAbort,
                                     cacheInfo, callback, token);
   }
}
