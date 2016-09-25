package to.augmented.reality.android.ardb.jdbc;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import to.augmented.reality.android.test.BuildConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

@Config(constants = BuildConfig.class, sdk = 21)
@RunWith(RobolectricGradleTestRunner.class)
public class JdbcRequestHandlerTest
//==================================
{

   @Test
   public void testConnect() throws Exception
   //----------------------------------------
   {
      JdbcRequestHandler instance = new JdbcRequestHandler();
      StringBuilder errbuf = new StringBuilder();
      Connection connection = null;
      try
      {
         connection = instance.connect("org.postgresql.Driver", postgresUrl(null, null, "eidb", "eidb", "eidb", 30, 15,
                                                                            false), errbuf);
      }
      finally
      {
         if (connection != null)
            try { connection.close(); } catch (Exception e) {}
      }
      assertNotNull(errbuf.toString(), connection);
   }

   @Test
   public void testPrepare() throws Exception
   //----------------------------------------
   {
      JdbcRequestHandler instance = new JdbcRequestHandler();
      StringBuilder errbuf = new StringBuilder();
      Connection connection = null;
      PreparedStatement pst = null;
      ResultSet rs = null;
      Map<String, Object> params = new HashMap<>();
      Map<String, int[]> paramIndices = new HashMap<>();
      try
      {
         connection = instance.connect("org.postgresql.Driver", postgresUrl(null, null, "eidb", "eidb", "eidb", 30, 15,
                                                                            false), errbuf);
         assertNotNull(errbuf.toString(), connection);
         String sql = "SELECT feature_id, name FROM feature WHERE feature_id = :id";
         params.put("id", Integer.valueOf(1));
         pst = instance.prepare(connection, sql, params, paramIndices);
         assertNotNull(pst);
         pst.close(); pst = null;
         params.clear();
         paramIndices.clear();

         sql = "select unit, description from sensor where sensor_type_id = :typeid and location_id = :locid";
         params.put("typeid", Integer.valueOf(2));
         params.put("locid", Integer.valueOf(848));
         pst = instance.prepare(connection, sql, params, paramIndices);
         assertNotNull(pst);
         rs = pst.executeQuery();
         assertNotNull(rs);
         assertTrue(rs.next());
         assertEquals("KL", rs.getString(1));
         assertEquals("SMALL", rs.getString(2));
         assertTrue(rs.next());
         assertEquals("KL", rs.getString(1));
         assertEquals("BIG", rs.getString(2));
         rs.close(); rs = null;
         pst.close(); pst = null;
         params.clear();
         paramIndices.clear();

         sql= "select description from sensor s inner join sensor_location sl on s.sensor_id = sl.sensor_id " +
               "where s.location_id = :locid and sl.location_id = :locid";
         params.put("locid", Integer.valueOf(775));
         pst = instance.prepare(connection, sql, params, paramIndices);
         assertNotNull(pst);
         pst.close(); pst = null;
         params.clear();
         paramIndices.clear();

         sql= "select description from sensor s inner join sensor_location sl on s.sensor_id = sl.sensor_id " +
               "where s.location_id = :locid and s.sensor_id!=':locid'";
         params.put("locid", Integer.valueOf(786));
         pst = instance.prepare(connection, sql, params, paramIndices);
         assertNotNull(pst);
         rs = pst.executeQuery();
         assertNotNull(rs);
         assertTrue(rs.next());
         pst.close(); pst = null;
         params.clear();
         paramIndices.clear();
      }
      finally
      {
         if (rs != null)
            try { rs.close(); } catch (Exception e) {}
         if (pst != null)
            try { pst.close(); } catch (Exception e) {}
         if (connection != null)
            try { connection.close(); } catch (Exception e) {}
      }

   }

   @Test
   public void testSelect() throws Exception
   //----------------------------------------
   {
      ShadowApplication app = ShadowApplication.getInstance();
      Context context = app.getApplicationContext();
      JdbcRequestHandler instance = new JdbcRequestHandler();
      StringBuilder errbuf = new StringBuilder();
      Connection connection = null;
      PreparedStatement pst = null;
      ResultSet rs = null;
      Map<String, Object> params = new HashMap<>();
      Map<String, int[]> paramIndices = new HashMap<>();
      File databaseDir = null, databaseFile = null;
      SQLiteDatabase localDatabase = null;
      Cursor cursor = null;
      try
      {
         File dir = context.getExternalFilesDir(null);
         if (dir == null)
         {
            dir = File.createTempFile("ardata", ".tmp");
            dir.delete();
            dir.mkdirs();
         }
         String name = context.getPackageName();
         if (name == null)
            name = "to.augmented.reality.android.ardb";
         databaseDir = new File(dir, name);
         databaseDir = new File(databaseDir, "ardata");
         if (! databaseDir.exists())
         {
            databaseDir.mkdirs();
            assertTrue(databaseDir.exists());
            assertTrue(databaseDir.isDirectory());
         }
         databaseFile = new File(databaseDir, "test.sqlite");
         localDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
         assertNotNull(localDatabase);
         connection = instance.connect("org.postgresql.Driver", postgresUrl(null, null, "eidb", "eidb", "eidb", 30, 15,
                                                                            false), errbuf);
         assertNotNull(errbuf.toString(), connection);
         String sql = "select description, ST_AsText(wgs84_location) as location from location " +
               "where wgs84_location && ST_MakeEnvelope(:minlatitude, :minlongitude, :maxlatitude, :maxlongitude, 4326)";
         params.put("minlatitude", -34);
         params.put("minlongitude", 24);
         params.put("maxlatitude", -35);
         params.put("maxlongitude", 26);
         pst = instance.prepare(connection, sql, params, paramIndices);
         cursor = instance.select(pst, localDatabase, "test", true, errbuf);
         assertNotNull(cursor);
      }
      finally
      {
         if (rs != null)
            try { rs.close(); } catch (Exception e) {}
         if (pst != null)
            try { pst.close(); } catch (Exception e) {}
         if (connection != null)
            try { connection.close(); } catch (Exception e) {}
         if (cursor != null)
            try { cursor.close(); } catch (Exception e) {}
         if (localDatabase != null)
            try { localDatabase.close(); } catch (Exception e) {}
         if ( (databaseFile != null) && (databaseFile.exists()) )
            databaseFile.delete();
         if ( (databaseDir != null) && (databaseDir.exists()) )
            databaseDir.delete();
      }
   }

   String postgresUrl(String host, Integer port, String database, String user, String password,
                      int connectionTimeout, int timeout, boolean isSSL)
   //------------------------------------------------------------------------------------------------------
   {
      if ( (host == null) || (host.isEmpty()) )
         host = "127.0.0.1"; //"10.0.2.2";
      if ( (port == null) || (port < 0) )
         port = 5432;
      if (database == null)
         database = "";
      String url =  String.format(Locale.getDefault(), "jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                                  host, port, database, user, password);
      if (connectionTimeout > 0)
         url += "&loginTimeout=" + connectionTimeout;
      if (timeout > 0)
         url += "&socketTimeout=" + timeout;
      if (isSSL)
         url += "&ssl=true";
      return url;
   }
}
