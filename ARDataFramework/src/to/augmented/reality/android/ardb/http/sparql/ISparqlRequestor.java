package to.augmented.reality.android.ardb.http.sparql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;
import to.augmented.reality.android.ardb.http.Cache;
import to.augmented.reality.android.ardb.http.HTTP_METHOD;
import to.augmented.reality.android.ardb.http.IHttpRequestor;
import to.augmented.reality.android.ardb.http.MIME_TYPES;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ISparqlRequestor extends IHttpRequestor
//=======================================================
{
   /**
    * Asynchronous request method.
    * @param method HTTP method for request
    * @param endpoint Request URI/URL
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
    * @param callback The asynchronous callback to invoke on completion or when an error occurs.
    * @param token A token which can be used to identify the request when the callback is invoked.
    * @param localDatabase A instance of a local Android (SQLite) database in which to create result tables
    * @param tableName Name of the table to create for this request. Overwrites any existing table by this name.
    * @param cacheInfo A Cache instance specifying caching parameters or null for no caching. Use the same
    *                  Cache instance (or at least the same information in the Cache) when executing multiple
    *                  calls.
    * @param mustAbort Setting <i>mustAbort</i> to false in another thread can be used to abort the request. Ignored if
    *                  null
    * @param errbuf If the request fails before the HTTP request is invoked then errbuf should contain a descriptive
    *               error message
    * @return A <i>Future</i> for the concurrently executing request (or completed request if concurrency is not
    * specified for the requestor) or <i>null</i> if a request setup error occurred
    */
   Future<?> request(HTTP_METHOD method, URI endpoint, String userAgent, MIME_TYPES encoding, Map<String, String> header,
                     ImmutableAnything parameters, long connectionTimeout, TimeUnit connectionTimeoutUnit,
                     long readTimeout, TimeUnit readTimeoutUnit, Cache cacheInfo, SQLiteDatabase localDatabase,
                     String tableName, ICursorQueryCallback callback, Anything token, AtomicBoolean mustAbort,
                     StringBuilder errbuf);

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
    * @param cacheInfo A Cache instance specifying caching parameters or null for no caching. Use the same
    *                  Cache instance (or at least the same information in the Cache) when executing multiple
    *                  calls.
    * @param localDatabase A instance of a local Android (SQLite) database in which to create result tables
    * @param tableName Name of the table to create for this request. Overwrites any existing table by this name.
    * @param mustAbort Setting <i>mustAbort</i> to false in another thread can be used to abort the request. Ignored if
    *                  null
    * @param errbuf If the request fails before the HTTP request is invoked then errbuf should contain a descriptive
    *               error message
    * @return An Android Cursor for the table specified in tableName containing the results.
    */
   Cursor request(HTTP_METHOD method, URI uri, String userAgent, MIME_TYPES encoding, Map<String, String> header,
                  ImmutableAnything parameters, long connectionTimeout, TimeUnit connectionTimeoutUnit,
                  long readTimeout, TimeUnit readTimeoutUnit, Cache cacheInfo,
                  SQLiteDatabase localDatabase, String tableName, AtomicBoolean mustAbort, StringBuilder errbuf);
}
