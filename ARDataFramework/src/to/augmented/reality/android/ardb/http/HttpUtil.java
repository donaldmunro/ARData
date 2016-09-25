package to.augmented.reality.android.ardb.http;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class HttpUtil
//-------------------
{
   static public void appendGET(Uri.Builder uriBuilder, String k, String[] values)
   //-----------------------------------------------------------------------------
   {
      if ( (values == null) || (values.length == 0) )
         return;
      for (String v : values)
      {
         if ( (v == null) || (v.trim().isEmpty()) )
            continue;
         uriBuilder.appendQueryParameter(k, v);
      }
   }

   static public void appendPOST(StringBuilder data, String k, String[] values) throws UnsupportedEncodingException
   //--------------------------------------------------------------------------------------------------------------
   {
      if ( (values == null) || (values.length == 0) )
         return;
      if (data.length() > 0)
         data.append('&');
      for (String v : values)
      {
         if ( (v == null) || (v.trim().isEmpty()) )
            continue;
         data.append(k).append('=').append(URLEncoder.encode(v, "UTF-8")).append('&');
      }
      if ( (data.length() > 0) && (data.charAt(data.length() - 1) == '&') )
         data.deleteCharAt(data.length() - 1);
   }

   static public void getHttpError(int code, HttpURLConnection connection, String encoding, StringBuilder msg)
   //----------------------------------------------------------------------------------------------
   {
      BufferedReader response = null;
      InputStream is = null;
      String line = null;
      try
      {
         switch (code)
         {
            case 400:
            case 500:
               msg.append(connection.getResponseMessage());
               try { is = connection.getErrorStream(); } catch (Exception _e) { is = null; }
               if (is != null)
               {
                  response = new BufferedReader(new InputStreamReader(is));
                  msg.append("\n");
                  while ( (line = response.readLine()) != null)
                     msg.append(line).append(' ');
               }
               break;

            case 406:
               msg.append("406: Accept encoding ").append(encoding).append(" not accepted");
               break;

            case -1:
               try
               {
                  try { is = connection.getErrorStream(); } catch (Exception _e) { is = null; }
                  if (is != null)
                  {
                     response = new BufferedReader(new InputStreamReader(is));
                     while ( (line = response.readLine()) != null)
                        msg.append(line).append(' ');
                  }
               }
               catch (Exception _e)
               {
                  msg.append("ERROR: Reading error stream: ").append(_e.getMessage());
               }
               break;
         }
      }
      catch (Exception _e)
      {
         code = 500;
         msg.append(_e.getMessage());
      }
      finally
      {
         if (response != null)
            try { response.close(); } catch (Exception _e) {}
      }
   }

   static public void saveHttpResponse(InputStream is, String encoding, OutputStream os, boolean mustCompress,
                                       AtomicBoolean mustAbort)
         throws IOException
   //---------------------------------------------------------------------------------------------------------
   {
//      String line = null;
//      if (isText)
//      {
//         BufferedReader response = null;
//         PrintWriter result = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
//         if ((encoding == null) || (encoding.equalsIgnoreCase("identity")))
//            response = new BufferedReader(new InputStreamReader(is));
//         else if (encoding.equalsIgnoreCase("gzip"))
//
//            response = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)));
//         else if (encoding.equalsIgnoreCase("deflate"))
//         {  // Adapted from http://thushw.blogspot.com/2014/05/decoding-html-pages-with-content.html
//            byte[] header = new byte[2];
//            int count = 0;
//            while (count < 2)
//            {
//               int read = is.read(header, count, 2 - count);
//               if (read == -1) break;
//               count += read;
//            }
//
//            boolean hasHeader = isZlibHeader(header);
//            response = new BufferedReader(new InputStreamReader(
//                       new InflaterInputStream(new HeaderInputStream(is,header), new Inflater(! hasHeader))));
//         }
//         while ((line = response.readLine()) != null)
//            result.println(line);
//         try { result.flush(); } catch (Exception _e) {}
//         try { result.close();} catch (Exception _e) {}
//      }
//      else
//      {
      final int bufsize = 32768;
      if ( (encoding == null) || (encoding.trim().isEmpty()) )
         encoding = "identity";
      final InputStream response;
      if (encoding.equalsIgnoreCase("identity"))
         response = new BufferedInputStream(is, bufsize);
      else if (encoding.equalsIgnoreCase("gzip"))
      {
         if (mustCompress)
            response = new BufferedInputStream(is, bufsize);
         else
            response = new GZIPInputStream(is, bufsize);
      }
      else if (encoding.equalsIgnoreCase("deflate"))
      {  // Adapted from http://thushw.blogspot.com/2014/05/decoding-html-pages-with-content.html
         byte[] header = new byte[2];
         int count = 0;
         while (count < 2)
         {
            int read = is.read(header, count, 2 - count);
            if (read == -1) break;
            count += read;
         }

         boolean hasHeader = isZlibHeader(header);
         response = new BufferedInputStream(
               new InflaterInputStream(new HeaderInputStream(is, header), new Inflater(! hasHeader), bufsize));
      }
      else
         response = new BufferedInputStream(is, bufsize);
      int read = -1;
      byte[] buf = new byte[bufsize];
      final OutputStream ws;
      if ( (mustCompress) && (! encoding.equalsIgnoreCase("gzip")) )
         ws = new GZIPOutputStream(os, bufsize);
      else
         ws = new BufferedOutputStream(os, bufsize);
      while ((read = response.read(buf)) != -1)
      {
         ws.write(buf, 0, read);
         if ( (mustAbort != null) && (mustAbort.get()) )
            break;
      }
      try { ws.flush();} catch (Exception _e) {}
      try { ws.close();} catch (Exception _e) {}
//      }
   }

   private static boolean isZlibHeader(byte[] bytes)
   //----------------------------------------
   {
      char byte1 = (char)(bytes[0] & 0xFF);
      char byte2 = (char)(bytes[1] & 0xFF);
      return byte1 == 0x78 && (byte2 == 0x01 || byte2 == 0x9c || byte2 == 0xDA);
   }

   static class HeaderInputStream extends InputStream
   //================================================
   {
      final byte[] header;
      int count = 0, size;
      final InputStream wrappee;

      public HeaderInputStream(InputStream is, byte[] header) { wrappee = is; this.header = header; size = header.length; }

      @Override
      public int read() throws IOException
      //-----------------------------------
      {
         if (count < size)
            return header[count++];
         else
            return wrappee.read();
      }
   }
}
