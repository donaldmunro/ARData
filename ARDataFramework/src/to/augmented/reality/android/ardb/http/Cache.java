package to.augmented.reality.android.ardb.http;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final public class Cache
//======================
{
   static final String TAG = Cache.class.getSimpleName();

   private final long cacheAge;
   public long getCacheAge() { return cacheAge; }

   private final TimeUnit cacheAgeUnit;
   public TimeUnit getCacheAgeUnit() { return cacheAgeUnit; }

   private final File cacheDir;
   public File getCacheDir() { return cacheDir; }

   public File getCacheFile(String hash)
   //-----------------------------------
   {
      File cacheFile = new File(cacheDir, hash + ".cache");
      return (cacheFile.exists()) ? cacheFile : null;
   }

   private final boolean isOverideNoCache;
   public boolean isOverideNoCache() { return isOverideNoCache; }

   private final boolean isOverideHttpCacheAge;
   public boolean isOverideHttpCacheAge() { return isOverideHttpCacheAge; }

   private boolean isHit = false;
   public boolean isHit() { return isHit; }

   private final Properties cacheProperties = new Properties();

   /**
    * Constructor for HTTP only caching ie no override on HTTP server cache timeout parameters.
    * @param cacheDir The directory for cache storage
    * @throws FileNotFoundException
    */
   public Cache(File cacheDir) throws FileNotFoundException
   //--------------------------------------------------------------------------------------------
   {
      this(0, null, cacheDir, false, false);
   }

   /**
    * Full constructor allowing optional overide of HTTP server cache timeout parameters.
    * @param cacheAge The age after which the cached file is considered stale.
    * @param cacheAgeUnit The time unit in which cacheAge is expressed.
    * @param cacheDir The directory for cache storage
    * @param isOverideNoCache Override the no-cache and no-store server parameters.
    * @param isOverideCacheAge Override the server age parameters with the value specified in cacheAge
    * @throws FileNotFoundException
    */
   public Cache(long cacheAge, TimeUnit cacheAgeUnit, File cacheDir, boolean isOverideNoCache,
                boolean isOverideCacheAge) throws FileNotFoundException
   //---------------------------------------------------------------------------------------------
   {
      isHit = false;
      this.cacheAge = cacheAge;
      this.cacheAgeUnit = cacheAgeUnit;
      if (cacheDir == null)
      {
         cacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
         this.cacheDir = new File(cacheDir, "ardata_cache");
         if (! this.cacheDir.exists())
            this.cacheDir.mkdirs();
         this.cacheDir.setWritable(true);
      }
      else
         this.cacheDir = cacheDir;
      if (! this.cacheDir.isDirectory())
      {
         Log.e(TAG, "Specify a valid cache directory");
         throw new FileNotFoundException("Specify a valid cache directory");
      }
      this.isOverideNoCache = isOverideNoCache;
      this.isOverideHttpCacheAge = isOverideCacheAge;
   }

   public String getEtag() { return cacheProperties.getProperty("etag"); }

   public boolean isNoCache()
   //------------------------
   {
      String s = cacheProperties.getProperty("isNoCache");
      return ((s != null) ? Boolean.parseBoolean(s) : false);
   }

   public boolean isNoStore()
   //------------------------
   {
      String s = cacheProperties.getProperty("isNoStore");
      return ((s != null) ? Boolean.parseBoolean(s) : false);
   }

   public boolean hasCacheControl()
   //------------------------------
   {
      String s = cacheProperties.getProperty("hasCacheControl");
      return ((s != null) ? Boolean.parseBoolean(s) : false);
   }

   public boolean isMustRevalidate()
   {
      String s = cacheProperties.getProperty("mustRevalidate");
      return ((s != null) ? Boolean.parseBoolean(s) : false);
   }

   public long getMaxAge()
   {
      String s = cacheProperties.getProperty("maxAge");
      return ((s != null) ? Long.parseLong(s) : 0L);
   }

   public long getStaleWhileRevalidate()
   {
      String s = cacheProperties.getProperty("staleWhileRevalidate");
      return ((s != null) ? Long.parseLong(s) : 0L);
   }

   public long getExpires()
   {
      String s = cacheProperties.getProperty("expires");
      return ((s != null) ? Long.parseLong(s) : 0L);
   }

   public long getLastModified()
   {
      String s = cacheProperties.getProperty("lastModified");
      return ((s != null) ? Long.parseLong(s) : 0L);
   }

   public long getServerTimestamp()
   {
      String s = cacheProperties.getProperty("serverTimestamp");
      return ((s != null) ? Long.parseLong(s) : 0L);
   }

   public long getTimestamp()
   {
      String s = cacheProperties.getProperty("timestamp");
      return ((s != null) ? Long.parseLong(s) : 0L);
   }

   public long getTtl()
   {
      String s = cacheProperties.getProperty("ttl");
      return ((s != null) ? Long.parseLong(s) : 0L);
   }

   synchronized public File writeCache(HttpURLConnection connection, String hash, InputStream is, AtomicBoolean mustAbort)
   //---------------------------------------------------------------------------------------------------------------------
   {
      File propertyFile = new File(cacheDir, hash + ".prop");
      propertyFile.delete();
      File cacheFile = new File(cacheDir, hash + ".cache");
      cacheFile.delete();
      isHit = false;
      BufferedOutputStream bos = null;
      BufferedInputStream bis = null;
      PrintWriter pw = null;
      try
      {
         Map<String, List<String>> headers = connection.getHeaderFields();
         List<String> valueList = headers.get("Cache-Control");
         if (valueList != null)
         {
            cacheProperties.put("hasCacheControl", "true");
            for (String cachControl : valueList)
            {
               String[] tokens = cachControl.split(",");
               for (int i = 0; i < tokens.length; i++)
               {
                  String token = tokens[i].trim();
                  if (token.equals("no-cache"))
                     cacheProperties.put("isNoCache", "true");
                  else if (token.equals("no-store"))
                     cacheProperties.put("isNoStore", "true");
                  else if (token.startsWith("max-age="))
                  {
                     try
                     {
                        long maxAge = Long.parseLong(token.substring(8).trim());
                        cacheProperties.put("maxAge", Long.toString(maxAge));
                     }
                     catch (Exception e)
                     {
                     }
                  }
                  else if (token.startsWith("stale-while-revalidate="))
                  {
                     try
                     {
                        long staleWhileRevalidate = Long.parseLong(token.substring(23).trim());
                        cacheProperties.put("staleWhileRevalidate", Long.toString(staleWhileRevalidate));
                     }
                     catch (Exception e)
                     {
                     }
                  } else if (token.equals("must-revalidate") || token.equals("proxy-revalidate"))
                     cacheProperties.put("mustRevalidate", "true");
               }
            }
         }
         else
            cacheProperties.put("hasCacheControl", "false");

         valueList = headers.get("ETag");
         if ( (valueList != null) && (valueList.size() > 0) )
            cacheProperties.put("etag", valueList.get(0));
         cacheProperties.put("expires", Long.toString(connection.getExpiration()));
         cacheProperties.put("lastModified", Long.toString(connection.getLastModified()));
//         serverTimestamp = maxDate(headers.get("Date"));
         cacheProperties.put("serverTimestamp", Long.toString(connection.getHeaderFieldDate("Date", 0L)));
         cacheProperties.put("timestamp", Long.toString(System.currentTimeMillis()));

         if ( ( (isNoCache()) || (isNoStore()) ) && (! isOverideNoCache) )
            return cacheFile;
         long httpTtl = 0;
         long maxage = getMaxAge();
         if (maxage > 0)
            httpTtl = System.currentTimeMillis() + maxage*1000 + (isMustRevalidate() ? 0 : getStaleWhileRevalidate()*1000);
         else
         {
            if ( (getServerTimestamp() > 0) && (getExpires() > getServerTimestamp()) )
               httpTtl = System.currentTimeMillis() + (getExpires() - getServerTimestamp());
         }
         cacheProperties.put("ttl", Long.toString(httpTtl));

         final String encoding = connection.getContentEncoding();
         bos = new BufferedOutputStream(new FileOutputStream(propertyFile));
         cacheProperties.store(bos, hash);
         if (! propertyFile.exists())
            return cacheFile;
         bos.close(); bos = null;
         bos = new BufferedOutputStream(new FileOutputStream(cacheFile), 32768);
         HttpUtil.saveHttpResponse(is, encoding, bos, true, mustAbort);
         bos.close(); bos = null;
         if (! cacheFile.exists())
         {
            propertyFile.delete();
            return null;
         }
         return cacheFile;
      }
      catch (Exception _e)
      {
         Log.e(TAG, "", _e);
         return cacheFile;
      }
      finally
      {
         if (bos != null)
            try { bos.close(); } catch (Exception _e) {}
         if (bis != null)
            try { bis.close(); } catch (Exception _e) {}
      }
   }

   synchronized public boolean updateAge(String hash)
   //------------------------------------------------
   {
      File propertyFile = new File(cacheDir, hash + ".prop");
      if (! propertyFile.exists())
         return false;
      File cacheFile = new File(cacheDir, hash + ".cache");
      if (! cacheFile.exists())
      {
         Log.w(TAG, "Cache file " + cacheFile.getAbsolutePath() + " disappeared");
         propertyFile.delete();
         return false;
      }
      BufferedInputStream bis = null;
      BufferedOutputStream bos = null;
      try
      {
         bis = new BufferedInputStream(new FileInputStream(propertyFile));
         cacheProperties.load(bis);
         bis.close(); bis = null;
         long httpTtl = 0;
         long maxage = getMaxAge();
         if (maxage > 0)
            httpTtl = System.currentTimeMillis() + maxage*1000 + (isMustRevalidate() ? 0 : getStaleWhileRevalidate()*1000);
         else
         {
            if ( (getServerTimestamp() > 0) && (getExpires() > getServerTimestamp()) )
               httpTtl = System.currentTimeMillis() + (getExpires() - getServerTimestamp());
         }
         cacheProperties.put("ttl", Long.toString(httpTtl));
         cacheProperties.put("timestamp", Long.toString(System.currentTimeMillis()));
         bos = new BufferedOutputStream(new FileOutputStream(propertyFile));
         cacheProperties.store(bos, hash);
         return true;
      }
      catch (Exception e)
      {
         Log.e(TAG, "", e);
         return false;
      }
      finally
      {
         if (bos != null)
            try { bos.close(); } catch (Exception _e) {}
         if (bis != null)
            try { bis.close(); } catch (Exception _e) {}
      }
   }

   synchronized public File hit(String hash)
   //---------------------------------------
   {
      File propertyFile = new File(cacheDir, hash + ".prop");
      if (! propertyFile.exists())
         return null;
      File cacheFile = new File(cacheDir, hash + ".cache");
      if (! cacheFile.exists())
      {
         Log.w(TAG, "Cache file " + cacheFile.getAbsolutePath() + " disappeared");
         propertyFile.delete();
         return null;
      }
      BufferedInputStream bis = null;
      try
      {
         bis = new BufferedInputStream(new FileInputStream(propertyFile));
         cacheProperties.load(bis);
         if (isOverideHttpCacheAge)
         {
            long ts = getTimestamp();
            long ms = TimeUnit.MILLISECONDS.convert(cacheAge, cacheAgeUnit);
            if ( (ts + ms) > System.currentTimeMillis() )
            {
               isHit = true;
               return cacheFile;
            }
         }
         if (getTtl() > System.currentTimeMillis())
         {
            isHit = true;
            return cacheFile;
         }
      }
      catch (Exception e)
      {
         Log.e(TAG, "", e);
         return null;
      }
      return null;
   }

   synchronized public void delete(String hash)
   //------------------------------------------
   {
      File propertyFile = new File(cacheDir, hash + ".prop");
      if (propertyFile.exists())
         propertyFile.delete();
      File cacheFile = new File(cacheDir, hash + ".cache");
      if (cacheFile.exists())
         cacheFile.delete();
   }

   synchronized public void purgeAged()
   //----------------------------------
   {
      File[] files = cacheDir.listFiles(new FileFilter()
      {
         @Override public boolean accept(File f) { return (f.getName().endsWith(".prop")); }
      });
      for (File f : files)
      {
         BufferedInputStream bis = null;
         Properties properties = new Properties();
         try
         {
            bis = new BufferedInputStream(new FileInputStream(f));
            properties.load(bis);
            long timeout1 = getTtl(), timeout2 = -1;
            if (isOverideHttpCacheAge)
            {
               long ts = getTimestamp();
               long ms = TimeUnit.MILLISECONDS.convert(cacheAge, cacheAgeUnit);
               timeout2 = ts + ms;
            }
            long timeout = Math.max(timeout1, timeout2);
            if (timeout < System.currentTimeMillis())
            {
               String name = f.getName();
               f.delete();
               int p = name.lastIndexOf('.');
               if (p >= 0)
               {
                  name = name.substring(0, p);
                  File cacheFile = new File(cacheDir, name + ".cache");
                  if (cacheFile.exists())
                     cacheFile.delete();
               }
            }
         }
         catch (Exception e)
         {
            Log.e(TAG, "", e);
         }
      }
   }

   synchronized public void purgeAll()
   //----------------------------------
   {
      File[] files = cacheDir.listFiles(new FileFilter()
      {
         @Override
         public boolean accept(File f) { return ( (f.getName().endsWith(".prop")) || (f.getName().endsWith(".cache")) ); }
      });
      for (File f : files)
         f.delete();
   }

//   private long maxDate(List<String> valueList)
//   //------------------------------------------
//   {
//      if (valueList == null)
//         return -1;
//      long max =-1;
//      for (String exps : valueList)
//      {
//         long exp = parseHttpDate(exps).getTime();
//         if (exp > max)
//            max = exp;
//      }
//      return max;
//   }

//   static private SimpleDateFormat RFC1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
//   static private SimpleDateFormat RFC1036 = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz");
//   static private SimpleDateFormat ASCTIME = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
//   static private SimpleDateFormat[] DATE_FORMATS = {RFC1123, RFC1036, ASCTIME};
//
//   private static final Date YEAR_START;
//   static
//   {
//      final Calendar calendar = Calendar.getInstance();
//      calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
//      calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
//      calendar.set(Calendar.MILLISECOND, 0);
//      YEAR_START = calendar.getTime();
//   }
//
//   public static Date parseHttpDate(String s)
//   //----------------------------------------
//   {
//      if (s == null) return null;
//      s = s.trim();
//      if (s.length() > 1 && s.startsWith("'") && s.endsWith("'"))
//         s = s.substring (1, s.length() - 1);
//      Date date = null;
//      for (SimpleDateFormat sdf : DATE_FORMATS)
//      {
//         sdf.set2DigitYearStart(YEAR_START);
//         final ParsePosition pos = new ParsePosition(0);
//         try { date = sdf.parse(s, pos); } catch (Exception e) { date = null; continue; }
//         if (pos.getIndex() != 0) return date;
//      }
//      return date;
//   }
}
