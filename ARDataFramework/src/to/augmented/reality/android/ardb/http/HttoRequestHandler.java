package to.augmented.reality.android.ardb.http;

import android.net.Uri;
import android.util.Log;
import to.augmented.reality.android.ardb.util.Md5Encrypter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttoRequestHandler implements IHttpRequestHandler
//============================================================
{
   final static private String TAG = HttoRequestHandler.class.getName();

   static public String USER_AGENT = "Mozilla/5.0 (Android; Mobile; rv:13.0) Gecko/13.0 Firefox/13.0";

   @Override
   public int get(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, StringBuffer result,
                  Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------------
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      int code = get(uri, header, connectionTimeout, readTimeout, baos, cache, mustAbort, errbuf);
      result.append(baos.toString());
      return code;
   }

   @Override
   public int get(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, File outputFile,
                  Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf)
   //---------------------------------------------------------------------------------------------------------
   {
      BufferedOutputStream bw = null;
      int code = 500;
      try
      {
         bw = new BufferedOutputStream(new FileOutputStream(outputFile));
         code = get(uri, header, connectionTimeout, readTimeout, bw, cache, mustAbort, errbuf);
      }
      catch (Exception e)
      {
         errbuf.append("HttoRequestHandler.get: ").append(e.getMessage());
      }
      finally
      {
         if (bw != null)
            try { bw.close(); } catch (Exception _e) {}
      }
      return code;
   }


   @Override
   public int get(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout,
                  OutputStream resultStream, Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------------
   {
      BufferedReader response = null;
      HttpURLConnection connection = null;
      InputStream is = null;
      int responseCode = -1;
      if (header == null)
         header = new HashMap<>();
      String contentEncoding;
      String encoding = header.get("Accept");
      if (encoding == null)
      {
         Log.w(TAG, "Accept encoding not specified");
         encoding = "text/html,application/xhtml+xml,application/xml,text/plain;q=0.9,*/*;q=0.8";
         header.put("Accept", encoding);
      }
      if (errbuf == null)
         errbuf = new StringBuilder();
      File cacheFile = null;
      BufferedInputStream bis = null;
      try
      {
         final String hash = new Md5Encrypter(uri.toString()).getHexHash();
         if (cache != null)
         {
            cacheFile = cache.hit(hash);
            //Note: Must call processCacheHit even if cacheFile is null to set etag
            responseCode = processCacheHit(cacheFile, cache, hash, header, resultStream, mustAbort);
            switch (responseCode)
            {
               case 200: return 200;
               case -1: cache = null;
               default: break;
            }
         }
         connection = request(uri, "GET", header, connectionTimeout, readTimeout, errbuf);
         if (connection == null)
            return 500;
         try
         {
            is = connection.getInputStream();
         }
         catch (IOException _e)
         {
            Log.e(TAG, "Connecting to " + uri, _e);
            is = null;
         }
         try
         {
            responseCode = connection.getResponseCode();
         }
         catch (Exception _e)
         {
            Log.e(TAG, "", _e);
            responseCode = -2;
         }
         if (responseCode == 304) // Can reuse cached file
         {
            cache.updateAge(hash);
            return threeOFour(cache, hash, resultStream, mustAbort);
         }
         else if (cache != null)
            cache.delete(hash);

         if ( (is == null) || ((responseCode / 100) != 2) )
         {
            HttpUtil.getHttpError(responseCode, connection, encoding, errbuf);
            return responseCode;
         }
         contentEncoding = connection.getContentEncoding();
         if (cache != null)
            cacheFile = cache.writeCache(connection, hash, is, mustAbort);
         if ( (cacheFile != null) && (cacheFile.exists()) )
         {
            try
            {
               bis = new BufferedInputStream(new FileInputStream(cacheFile), 32768);
               HttpUtil.saveHttpResponse(bis, "gzip", resultStream, false, mustAbort);
            }
            catch (Exception ee)
            {
               Log.e(TAG, "", ee);
               return 500;
            }
         }
         else
            HttpUtil.saveHttpResponse(is, contentEncoding, resultStream, false, mustAbort);
      }
      catch (SocketTimeoutException e)
      {
         if (errbuf != null)
            errbuf.append("Time out (").append(e.getMessage()).append(")");
         responseCode = 598;
      }
      catch (SecurityException e)
      {
         Log.e(TAG, "Check if application has Internet permission in AndroidManifest.xml", e);
         throw new RuntimeException(e);
      }
      catch (Exception e)
      {
         if ( (responseCode == -2) && (connection != null) )
         {
            try { responseCode = connection.getResponseCode(); } catch (Exception _e) { responseCode = -2; }
            if (is != null)
            {
               try
               {
                  HttpUtil.saveHttpResponse(is, connection.getContentEncoding(), resultStream, false, mustAbort);
               }
               catch (Exception _e)
               {
                  Log.e(TAG, "", e);
               }
            }
            HttpUtil.getHttpError(responseCode, connection, encoding, errbuf);
            if (responseCode == -1)
               responseCode = 500;
         }
         else if (connection == null)
            responseCode = 500;
         errbuf.append("HttoRequestHandler.get: Exception ").append(e.getMessage());
         Log.e(TAG, errbuf.toString(), e);
      }
      finally
      {
         if (resultStream != null)
            try { resultStream.close(); } catch (Exception _e) { }
         if (response != null)
            try { response.close(); } catch (Exception _e) {}
         if (connection != null)
            try { connection.disconnect(); } catch (Exception _e) {}
      }
      return responseCode;
   }

   @Override
   public int post(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, StringBuilder postData,
                   StringBuffer result, Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      int code = post(uri, header, connectionTimeout, readTimeout, postData, baos, cache, mustAbort, errbuf);
      result.append(baos.toString());
      return code;
   }

   @Override
   public int post(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout, StringBuilder postData,
                   File outputFile, Cache cache, AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      BufferedOutputStream bw = null;
      int code = 500;
      try
      {
         bw = new BufferedOutputStream(new FileOutputStream(outputFile));
         code = post(uri, header, connectionTimeout, readTimeout, postData, bw, cache, mustAbort, errbuf);
      }
      catch (Exception e)
      {
         errbuf.append("HttoRequestHandler.get: ").append(e.getMessage());
      }
      finally
      {
         if (bw != null)
            try { bw.close(); } catch (Exception _e) {}
      }
      return code;
   }

   @Override
   public int post(Uri uri, Map<String, String> header, long connectionTimeout, long readTimeout,
                   StringBuilder postData, OutputStream resultStream, Cache cache,
                   AtomicBoolean mustAbort, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      DataOutputStream dos = null;
      HttpURLConnection connection = null;
      InputStream is = null;
      int responseCode = -2;
      if (header == null)
         header = new HashMap<>();
      String encoding = header.get("Accept");
      if (encoding == null)
      {
         Log.w(TAG, "Accept encoding not specified");
         encoding = "text/html,application/xhtml+xml,application/xml,text/plain;q=0.9,*/*;q=0.8";
         header.put("Accept", encoding);
      }
      if (errbuf == null)
         errbuf = new StringBuilder();
      File cacheFile = null;
      BufferedInputStream bis = null;
      String contentEncoding;
      try
      {
         String hash = new Md5Encrypter(uri.toString()).getHexHash();
         if (cache != null)
         {
            cacheFile = cache.hit(hash);
            //Note: Must call processCacheHit even if cacheFile is null to set etag
            responseCode = processCacheHit(cacheFile, cache, hash, header, resultStream, mustAbort);
            switch (responseCode)
            {
               case 200: return 200;
               case -1: cache = null;
               default: break;
            }
         }
         connection = request(uri, "POST", header, connectionTimeout, readTimeout, errbuf);
         if (connection == null)
            return 500;
         connection.setDoInput(true);
         connection.setDoOutput(true);
         if (connection.getRequestProperty("Content-Type") == null)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
         if (postData == null)
            postData = new StringBuilder();
         connection.setRequestProperty("Content-Length", "" + Integer.toString(postData.length()));
         dos = new DataOutputStream (connection.getOutputStream ());
         dos.write(postData.toString().getBytes());
         dos.flush();

         try { is = connection.getInputStream(); } catch (Exception _e) { is = null; }
         try { responseCode = connection.getResponseCode(); } catch (Exception _e) { responseCode = -2; }
         if (responseCode == 304) // Can reuse cached file
         {
            cache.updateAge(hash);
            return threeOFour(cache, hash, resultStream, mustAbort);
         }
         else if (cache != null)
            cache.delete(hash);

         if ( (is == null) || ((responseCode / 100) != 2) )
         {
            HttpUtil.getHttpError(responseCode, connection, encoding, errbuf);
            return responseCode;
         }
         contentEncoding = connection.getContentEncoding();
         if (cache != null)
            cacheFile = cache.writeCache(connection, hash, is, mustAbort);
         if ( (cacheFile != null) && (cacheFile.exists()) )
         {
            try
            {
               bis = new BufferedInputStream(new FileInputStream(cacheFile), 32768);
               HttpUtil.saveHttpResponse(bis, "gzip", resultStream, false, mustAbort);
            }
            catch (Exception ee)
            {
               Log.e(TAG, "", ee);
               return 500;
            }
         }
         else
            HttpUtil.saveHttpResponse(is, contentEncoding, resultStream, false, mustAbort);
      }
      catch (SocketTimeoutException e)
      {
         if (errbuf != null)
            errbuf.append("Time out (").append(e.getMessage()).append(")");
         responseCode = 598;
      }
      catch (Exception e)
      {
         if ( (responseCode == -2) && (connection != null) )
         {
            try { responseCode = connection.getResponseCode(); } catch (Exception _e) { responseCode = -2; }
            HttpUtil.getHttpError(responseCode, connection, encoding, errbuf);
            if (is != null)
            {
               try
               {
                  HttpUtil.saveHttpResponse(is, connection.getContentEncoding(), resultStream, false, mustAbort);
               }
               catch (Exception _e)
               {
                  Log.e(TAG, "", e);
               }
            }
         }
         else if (connection == null)
            responseCode = 500;
         errbuf.append(" HttoRequestHandler.post: Exception ").append(e.getMessage());
         Log.e(TAG, errbuf.toString(), e);
      }
      finally
      {
         if (resultStream != null)
            try { resultStream.close(); } catch (Exception _e) { }
         if (is != null)
            try { is.close(); } catch (Exception _e) {}
         if (dos != null)
            try { dos.close(); } catch (Exception _e) {}
         if (connection != null)
            try { connection.disconnect(); } catch (Exception _e) {}
      }
      return responseCode;
   }

   protected HttpURLConnection request(Uri uri, String method, Map<String, String> header,
                                       long connectionTimeout, long readTimeout, StringBuilder errbuf)
   //--------------------------------------------------------------------------------------------------
   {
      HttpURLConnection connection = null;
      try
      {
         URL url = new URL(uri.toString());
         connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod(method);
         connection.setInstanceFollowRedirects(true);
         header.put("Accept-Encoding", "gzip;q=1.0, deflate;q=0.5, identity;q=0.3");
         Set<Map.Entry<String, String>> es = header.entrySet();
         for (Map.Entry<String, String> e : es)
            connection.addRequestProperty(e.getKey(), e.getValue());
         connection.setConnectTimeout((int) connectionTimeout);
         connection.setReadTimeout((int) readTimeout);
      }
      catch (Exception e)
      {
         if (errbuf == null)
            errbuf = new StringBuilder();
         errbuf.append("request(").append(uri).append(", ").append(method).append("): ").append(e.getMessage());
         Log.e(TAG, errbuf.toString(), e);
         if (connection != null)
            try { connection.disconnect(); } catch (Exception _e) {}
         return null;
      }
      return connection;
   }

   private int threeOFour(Cache cache, String hash, OutputStream resultStream, AtomicBoolean mustAbort)
   //--------------------------------------------------------------------------------------------------
   {
      File cacheFile = cache.getCacheFile(hash);
      if (cacheFile == null)
         return 500;
      else
      {
         BufferedInputStream bis = null;

         //String contentEncoding = ;
         try
         {
            bis = new BufferedInputStream(new FileInputStream(cacheFile), 32768);
            HttpUtil.saveHttpResponse(bis, "gzip", resultStream, false, mustAbort);
            return 304;
         }
         catch (Exception ee)
         {
            Log.e(TAG, "", ee);
            cache.delete(hash);
            return 500;
         }
         finally
         {
            if (bis != null)
               try { bis.close(); } catch (Exception _e) {}
         }
      }
   }

   private int processCacheHit(File cacheFile, Cache cache, String hash, Map<String, String> header,
                               OutputStream resultStream, AtomicBoolean mustAbort)
   //----------------------------------------------------------------------------------------------
   {
      BufferedInputStream bis = null;
      if (cacheFile != null)
      {
         try
         {
            bis = new BufferedInputStream(new FileInputStream(cacheFile), 32768);
            HttpUtil.saveHttpResponse(bis, "gzip", resultStream, false, mustAbort);
            return 200;
         }
         catch (Exception ee)
         {
            Log.e(TAG, "", ee);
            cache.delete(hash);
            return -1;
         }
         finally
         {
            if (bis != null)
               try { bis.close(); } catch (Exception _ee) { }
         }
      }
      if ( (cache != null) && (cache.getEtag() != null) && (! cache.getEtag().trim().isEmpty()) )
         header.put("If-None-Match", cache.getEtag().trim());
      return 0;
   }

   protected void println(final Writer writer, final String line) throws IOException
   //--------------------------------------------------------------------------------
   {
      final String s = line + EOL;
      final char[] chars = s.toCharArray();
      writer.write(chars, 0, chars.length);
   }
}
