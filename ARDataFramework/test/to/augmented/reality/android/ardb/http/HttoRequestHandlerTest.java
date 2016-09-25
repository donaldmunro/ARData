package to.augmented.reality.android.ardb.http;

import android.net.Uri;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import to.augmented.reality.android.test.BuildConfig;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Config(constants = BuildConfig.class, sdk = 22, manifest = "/ssd/Android/ARData/ARDataFramework/AndroidManifest.xml")
@RunWith(RobolectricGradleTestRunner.class)
public class HttoRequestHandlerTest
//==================================
{
   // We use nginx for tests
   static final String NGINX = "/usr/bin/nginx";
   static final String RESTY_NGINX = "/opt/nginx/sbin/nginx";
   static final String NGINX_GET_CONF = "/ssd/Android/ARData/ARDataFramework/httptest/nginx/conf/nginx.conf";
   static final String NGINX_POST_CONF = "/ssd/Android/ARData/ARDataFramework/httptest/nginx/conf/nginx-post.conf";
   static final String NGINX_ROOT = "/ssd/Android/ARData/ARDataFramework/httptest/nginx/";
   static final File NGINX_HTML = new File("/ssd/Android/ARData/ARDataFramework/httptest/nginx/html");
   static final int NGINX_PORT = 8088; // defined in NGINX_GET_CONF
   static final File CACHEDIR = new File("/ssd/Android/ARData/ARDataFramework/httptest/cache");
   static final String SU = "/bin/su";
   static final String PG = "/usr/bin/psql";
   static final String PG_CREATE_USER = "/usr/bin/createuser";
   static final String PG_DROP_USER = "/usr/bin/dropuser";
   static final String PG_DROP_DB = "/usr/bin/dropdb";
   static final String PG_CREATE_DB = "/usr/bin/createdb";
   static final String PG_DB = "ardftest";
   static final String PG_USER = "ardftest";

   static Process NGINX_PROCESS = null;

   static private void startNginx(String nginx, String configFileName) throws Exception
   //--------------------------------------------------------------------
   {
      new ProcessBuilder(nginx, "-s", "stop").start();
      File dir = new File(NGINX_ROOT,  "log");
      recursiveDelete(dir);
      dir.mkdirs();
      recursiveDelete(NGINX_HTML);
      NGINX_HTML.mkdirs();
      dir = new File(NGINX_HTML, "/cached/five_min");
      dir.mkdirs();
      assertTrue(dir.isDirectory());
      dir = new File(NGINX_HTML, "/cached/one_sec");
      dir.mkdirs();
      assertTrue(dir.isDirectory());

      NGINX_PROCESS = new ProcessBuilder(nginx, "-c", configFileName).start();

      Socket socket = null;
      boolean isRunning = false;
      for (int retry=0; retry<5; retry++)
      {
         try
         {
            socket = new Socket("localhost", NGINX_PORT);
            isRunning = true;
            break;
         }
         catch (Exception _e)
         {
            Thread.sleep(500);
            continue;
         }
         finally
         {
            if (socket != null)
               try { socket.close(); } catch (Exception _e) {}
         }
      }
      assertTrue("Error starting Nginx", isRunning);
   }

   static public void stopNginx(String nginx) throws IOException
   //--------------------------------------------------
   {
      new ProcessBuilder(nginx, "-s", "stop").start();
   }

   /*
    Use Nginx combined with Postgres to simulate a POST enabled web service, Requires ngx_postgres Nginx module
    (http://labs.frickle.com/nginx_ngx_postgres/) which should be available in OpenResty (http://openresty.org/)
    which is a Nginx variation. Openresty was configured using :
    ./configure --prefix=/opt --with-http_postgres_module

    Below assumes a Postgres database is running with OS user postgres being the owner
    */
   static public void setupPostgres() throws Exception
   //--------------------------------------------------
   {
      Process process = new ProcessBuilder(SU, "-l", "postgres", "-c", PG_DROP_DB + " " + PG_DB).start();
      exec(process, 0, false);
      process = new ProcessBuilder(SU, "-l", "postgres", "-c", PG_DROP_USER + " " + PG_USER).start();
      exec(process, 0, false);

      process = new ProcessBuilder(SU, "-l", "postgres", "-c", PG_CREATE_USER + " " + PG_USER).start();
      exec(process, 0, true);

      process = new ProcessBuilder(SU, "-l", "postgres", "-c", PG_CREATE_DB + " " + PG_DB).start();
      exec(process, 0, true);

      String sql = "CREATE TABLE test(col TEXT)";
      process = new ProcessBuilder(PG, PG_DB, PG_USER, "-c", sql).start();
      exec(process, 0, true);

      sql = "INSERT INTO test(col) VALUES ('Hello world')";
      process = new ProcessBuilder(PG, PG_DB, PG_USER, "-c", sql).start();
      exec(process, 0, true);
   }

   private static void exec(Process process, int expectedStatus, boolean isFail) throws Exception
   {
      int status = process.waitFor();
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;
      while ( (line = br.readLine()) != null)
         sb.append(line).append(' ');
      br.close();
      br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      while ( (line = br.readLine()) != null)
         sb.append(line).append(' ');
      if (isFail)
         assertEquals(sb.toString(), expectedStatus, status);
      else if (status != expectedStatus)
         System.err.println("WARN: " + sb.toString());
   }

   @Test
   public void testCompressed() throws Exception
   //------------------------------------------
   {
      HttoRequestHandler instance = new HttoRequestHandler();
      Uri uri = Uri.parse("http://httpbin.org/deflate");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StringBuilder errbuf = new StringBuilder();
      int responseCode = instance.get(uri, null, 30000, 10000, baos, null, null, errbuf);
      assertTrue(((responseCode / 100) == 2));
      JSONObject json = new JSONObject(baos.toString());
      assertTrue(Boolean.parseBoolean(json.getString("deflated")));
      baos.close();

      uri = Uri.parse("http://httpbin.org/gzip");
      baos = new ByteArrayOutputStream();
      responseCode = instance.get(uri, null, 30000, 10000, baos, null, null, errbuf);
      assertTrue(((responseCode / 100) == 2));
      json = new JSONObject(baos.toString());
      assertTrue(Boolean.parseBoolean(json.getString("gzipped")));
      baos.close();
   }

   @Test
   public void testGet() throws Exception
   //------------------------------------------
   {
      startNginx(NGINX, NGINX_GET_CONF);
      String html = "<!doctype html>\n<html lang=\"\">\n<head></head>\n<body>\n<p>Hello world</p>\n</body>\n</html>";
      File htmlFile = new File(NGINX_HTML, "file.html");
      FileWriter fw = null;
      try
      {
         fw = new FileWriter(htmlFile);
         fw.write(html);
         fw.close();
         HttoRequestHandler instance = new HttoRequestHandler();
         Uri uri = Uri.parse("http://localhost:" + NGINX_PORT + "/file.html");
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         StringBuilder errbuf = new StringBuilder();
         int responseCode = instance.get(uri, null, 30000, 10000, baos, null, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals(html, baos.toString());
         baos.close();

         baos = new ByteArrayOutputStream();
         Cache cache = new Cache(CACHEDIR);
         // not cached as not in cache specified directory (see httptest/nginx/conf/nginx.conf)
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals(html, baos.toString());
         assertFalse(cache.hasCacheControl());
         baos.close();

         //Test caching
         recursiveDelete(CACHEDIR);
         CACHEDIR.mkdirs();
         String filename = "/cached/five_min/file.html";
         htmlFile = new File(NGINX_HTML, filename);
         fw = new FileWriter(htmlFile);
         fw.write(html);
         fw.close();

         baos = new ByteArrayOutputStream();
         cache = new Cache(CACHEDIR);
         uri = Uri.parse("http://localhost:" + NGINX_PORT + filename);
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals(html, baos.toString());
         assertTrue(cache.hasCacheControl());

         baos = new ByteArrayOutputStream();
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals(html, baos.toString());
         assertTrue(cache.isHit());
         baos.close();

         // Test HTTP Cache miss for unchanged file (ie 304 response)
         recursiveDelete(CACHEDIR);
         CACHEDIR.mkdirs();
         filename = "/cached/one_sec/file.html";
         htmlFile = new File(NGINX_HTML, filename);
         fw = new FileWriter(htmlFile);
         fw.write(html);
         fw.close();
         uri = Uri.parse("http://localhost:" + NGINX_PORT + filename);
         cache = new Cache(CACHEDIR);
         baos = new ByteArrayOutputStream();
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         baos.close();
         Thread.sleep(1200);
         baos = new ByteArrayOutputStream();
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertEquals(304, responseCode);
         assertEquals(html, baos.toString());
         assertFalse(cache.isHit());
         baos.close();

         // Test HTTP Cache miss for changed file (ie new server request with 200 response and cache miss)
         recursiveDelete(CACHEDIR);
         CACHEDIR.mkdirs();
         uri = Uri.parse("http://localhost:" + NGINX_PORT + filename);
         cache = new Cache(CACHEDIR);
         baos = new ByteArrayOutputStream();
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         baos.close();
         htmlFile = new File(NGINX_HTML, filename);
         String html2 = "<!doctype html>\n<html lang=\"\">\n<head></head>\n<body>\n<p>Goodbye cruel world, I'm leaving you now</p>\n</body>\n</html>";
         fw = new FileWriter(htmlFile);
         fw.write(html2);
         fw.close();
         Thread.sleep(1200);
         baos = new ByteArrayOutputStream();
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals(html2, baos.toString());
         assertFalse(cache.isHit());
         baos.close();

         // Test local Cache timeout override
         recursiveDelete(CACHEDIR);
         CACHEDIR.mkdirs();
         htmlFile = new File(NGINX_HTML, filename);
         fw = new FileWriter(htmlFile);
         fw.write(html);
         fw.close();
         uri = Uri.parse("http://localhost:" + NGINX_PORT + filename);
         cache = new Cache(5, TimeUnit.MINUTES, CACHEDIR, true, true); //Should use our 5 minute value over HTTP 1 second
         baos = new ByteArrayOutputStream();
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         baos.close();
         htmlFile = new File(NGINX_HTML, filename);
         fw = new FileWriter(htmlFile);
         fw.write(html2);
         fw.close();
         Thread.sleep(1200);
         baos = new ByteArrayOutputStream();
         responseCode = instance.get(uri, null, 30000, 10000, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals(html, baos.toString()); // not html2
         assertTrue(cache.isHit());
         baos.close();
      }
      finally
      {
         if (fw != null)
            try { fw.close(); } catch (Exception _e) {}
//         htmlFile.delete();
         stopNginx(NGINX);
      }
   }

   @Test
   public void testPost() throws Exception
   {
      setupPostgres();
      startNginx(RESTY_NGINX, NGINX_POST_CONF);
      FileWriter fw = null;
      StringBuilder errbuf = new StringBuilder();
      try
      {
         recursiveDelete(CACHEDIR);
         CACHEDIR.mkdirs();
         HttoRequestHandler instance = new HttoRequestHandler();
         Uri uri = Uri.parse("http://localhost:" + NGINX_PORT + "/post");

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         Cache cache = new Cache(CACHEDIR);
         Map<String, String> headers = new HashMap<>();
         headers.put("Accept", "text/plain");
         int responseCode = instance.post(uri, headers, 30000, 10000, null, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals("Hello world", baos.toString());
         assertTrue(cache.hasCacheControl());
         baos.close();

         baos = new ByteArrayOutputStream();
         responseCode = instance.post(uri, headers, 30000, 10000, null, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals("Hello world", baos.toString());
         assertTrue(cache.isHit());
         baos.close();

         // 304 won't work as Nginx has no way of knowing whether the database contents have changed
//         Thread.sleep(3200);
//         baos = new ByteArrayOutputStream();
//         responseCode = instance.post(uri, headers, 30000, 10000, null, baos, cache, errbuf);
//         assertEquals(304, responseCode);
//         assertEquals("Hello world", baos.toString());
//         assertFalse(cache.isHit());
//         baos.close();

         recursiveDelete(CACHEDIR);
         CACHEDIR.mkdirs();
         baos = new ByteArrayOutputStream();
         cache = new Cache(5, TimeUnit.MINUTES, CACHEDIR, true, true); //Should use our 5 minute value over HTTP 1 second
         responseCode = instance.post(uri, headers, 30000, 10000, null, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals("Hello world", baos.toString());
         assertTrue(cache.hasCacheControl());
         baos.close();

         Thread.sleep(3200);
         baos = new ByteArrayOutputStream();
         responseCode = instance.post(uri, headers, 30000, 10000, null, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         assertEquals("Hello world", baos.toString());
         assertTrue(cache.isHit());
         baos.close();

         baos = new ByteArrayOutputStream();
//         String sql = "TRUNCATE TABLE test";
//         Process process = new ProcessBuilder(PG, PG_DB, PG_USER, "-c", sql).start();
//         exec(process, 0, true);
//         uri = Uri.parse("http://localhost:" + NGINX_PORT + "/post/insert");
//         uri = Uri.parse("http://requestb.in/1hpn39g1");
         uri = Uri.parse("http://httpbin.org/post");
         StringBuilder postData = new StringBuilder("col=Goodbye%20cruel%20world%2C%20I%27m%20leaving%20you%20now");
         responseCode = instance.post(uri, headers, 30000, 10000, postData, baos, cache, null, errbuf);
         assertTrue(((responseCode / 100) == 2));
         JSONObject json = new JSONObject(baos.toString());
//         assertEquals("Goodbye cruel world, I'm leaving you now", baos.toString());
         JSONObject formData = json.getJSONObject("form");
         assertEquals("Goodbye cruel world, I'm leaving you now", formData.getString("col"));
      }
      finally
      {
         if (fw != null)
            try { fw.close(); } catch (Exception _e) {}
//         htmlFile.delete();
         stopNginx(RESTY_NGINX);
      }
   }

   public static void recursiveDelete(final File dir)
   //------------------------------------------------
   {
      final File[] ls = dir.listFiles();
      if (ls != null)
      {
         for (int k = 0; k < ls.length; k++)
         {
            final File e = ls[k];
            if (e.isDirectory())
                   recursiveDelete(e);
                else
                   e.delete();
             }
      }
      dir.delete();
      if (dir.exists())
      {
         System.out.println("Warning: Failed to delete " + dir);
      }
   }
}
