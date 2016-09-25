package to.augmented.reality.android.ardb.http;

import android.net.Uri;
import android.util.Log;
import to.augmented.reality.android.ardb.anything.Anything;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpRequestorThread implements Callable<Integer>
//===========================================================
{
   static final private String TAG = HttpRequestorThread.class.getName();

   private final HTTP_METHOD method;
   private final Uri uri;
   private final Map<String, String> header;
   private final IHttpRequestorCallback callback;
   private final Anything token;
   private final OutputStream outputStream;
   private final long connectionTimeoutMs;
   private final long readTimeoutMs;
   private final StringBuilder postData;
   private final Cache cacheInfo;
   private final AtomicBoolean mustAbort;

   public HttpRequestorThread(HTTP_METHOD method, Uri uri, Map<String, String> header, StringBuilder postData,
                              long connectionTimeoutMs, long readTimeoutMs, OutputStream outputStream,
                              AtomicBoolean mustAbort, Cache cacheInfo)
   //------------------------------------------------------------------------------------------------
   {
      this(method, uri, header, postData, connectionTimeoutMs, readTimeoutMs, outputStream, mustAbort, cacheInfo, null, null);
   }

   public HttpRequestorThread(HTTP_METHOD method, Uri uri, Map<String, String> header, StringBuilder postData,
                              long connectionTimeoutMs, long readTimeoutMs, OutputStream outputStream,
                              AtomicBoolean mustAbort, Cache cacheInfo, IHttpRequestorCallback callback, Anything token)
   //------------------------------------------------------------------------------------------------------------------
   {
      this.method = method;
      this.uri = uri;
      this.header = header;
      this.postData = postData;
      this.connectionTimeoutMs = connectionTimeoutMs;
      this.readTimeoutMs = readTimeoutMs;
      this.outputStream = outputStream;
      this.cacheInfo = cacheInfo;
      this.callback = callback;
      this.token = token;
      this.mustAbort = mustAbort;
   }

   @Override
   public Integer call() throws Exception
   //------------------------------------
   {
      HttoRequestHandler handler = new HttoRequestHandler();
      StringBuilder errbuf = new StringBuilder();
      int retcode = 200;
      switch (method)
      {
         case GET:
            retcode = handler.get(uri, header, connectionTimeoutMs, readTimeoutMs, outputStream, cacheInfo, mustAbort,
                                  errbuf);
            break;
         case POST:
            retcode = handler.post(uri, header, connectionTimeoutMs, readTimeoutMs, postData, outputStream, cacheInfo,
                                   mustAbort, errbuf);
            break;
         default:
            throw new RuntimeException("HTTP method " + method + " not supported yet");
      }
      if (callback != null)
      {
         if ((retcode / 100) == 2)
            callback.onResponse(token, retcode);
         else
         {
            final String errm = "HttpRequestorThread: HTTP request returned error code " + retcode + ". URI: " + uri +
                                ". HTTP error: " + errbuf.toString();
            Log.e(TAG, errm);
            callback.onError(token, retcode, errm, null);
         }
      }
      return retcode;
   }
}
