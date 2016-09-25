package to.augmented.reality.android.ardb.http;

import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IHttpRequestor
//==============================
{
   /**
    * Asynchronous request method.
    * @param method HTTP method for request
    * @param uri Request URI/URL
    * @param userAgent User agent to send in request header (defaults to
    *                  'Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0' if <i>userAgent</i>
    *                  is empty or null unless <i>header</i> contains a 'User-Agent' key)
    * @param encoding The ACCEPT encoding used to specify the mime type which is desired for the response.
        *             An 'Accept' key in <i>header</i> overrides the <i>encoding</i> parameter.
    * @param header Other HTTP request headers to send to server.
    * @param parameters Implementation specific parameters.
    * @param connectionTimeout Connection timeout
    * @param connectionTimeoutUnit Time unit for <i>connectionTimeout</i>
    * @param readTimeout Read time out.
    * @param readTimeoutUnit Time unit for <i>readTimeout</i>
    * @param outputTo A OutputStream derived instance into which the HTTP response is written.
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
   Future<?> request(HTTP_METHOD method, URI uri, String userAgent, MIME_TYPES encoding, Map<String, String> header,
                     ImmutableAnything parameters, long connectionTimeout, TimeUnit connectionTimeoutUnit,
                     long readTimeout, TimeUnit readTimeoutUnit, OutputStream outputTo, Cache cacheInfo,
                     IHttpRequestorCallback callback, Anything token, AtomicBoolean mustAbort, StringBuilder errbuf);

   /**
    * Synchronous request method.
    * @param method HTTP method for request
    * @param uri Request URI/URL
    * @param userAgent User agent to send in request header (defaults to
    *                  'Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0' if <i>userAgent</i>
    *                  is empty or null unless <i>header</i> contains a 'User-Agent' key)
    * @param encoding The ACCEPT encoding used to specify the mime type which is desired for the response.
    *             An 'Accept' key in <i>header</i> overrides the <i>encoding</i> parameter.
    * @param header Other HTTP request headers to send to server.
    * @param parameters Implementation specific parameters.
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
   int request(HTTP_METHOD method, URI uri, String userAgent, MIME_TYPES encoding, Map<String, String> header,
               ImmutableAnything parameters, long connectionTimeout, TimeUnit connectionTimeoutUnit,
               long readTimeout, TimeUnit readTimeoutUnit, OutputStream outputTo, Cache cacheInfo,
               AtomicBoolean mustAbort, StringBuilder errbuf);
}
