package to.augmented.reality.android.ardb.http;

import android.net.Uri;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IHttpRequestHandler
//==================================
{
   static final public String EOL = System.getProperty("line.separator");

   int get(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, StringBuffer result,
           Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf);

   int get(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, File outputFile,
           Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf);

   int get(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, OutputStream resultStream,
           Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf);

   int post(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, StringBuilder postData,
            StringBuffer result, Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf);

   int post(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, StringBuilder postData,
            File outputFile, Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf);

   int post(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout,
            StringBuilder postData, OutputStream resultStream, Cache cache,
            AtomicBoolean mustAbort, StringBuilder errbuf);
}
