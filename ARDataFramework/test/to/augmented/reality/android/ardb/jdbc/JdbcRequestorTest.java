package to.augmented.reality.android.ardb.jdbc;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;
import to.augmented.reality.android.ardb.spi.ISpatialFunctions;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.test.BuildConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Config(constants = BuildConfig.class, sdk = 21, manifest = "/ssd/Android/ARData/ARDataFramework/AndroidManifest.xml")
@RunWith(RobolectricGradleTestRunner.class)
public class JdbcRequestorTest extends JdbcRequestor
//--------------------------------------------------
{

   static JdbcRequestorTest instance = new JdbcRequestorTest();

   JdbcRequestor delegate = null;


//   @BeforeClass static public void setUp() throws Exception { }

   // In Android studio remember to set Working Directory in Run->Edit configuration->Configuration_used_to_run_test to
   // [source dir]/ARData/ARDataFramework as manifest annotation in @Config above does not apply to in-IDE usage.
   @Test
   public void testSyncQuery() throws SQLException
   //---------------------------------------------
   {
      Connection connection = null;
      Cursor cursor = null;
      StringBuilder errbuf = new StringBuilder();
      ShadowApplication app = ShadowApplication.getInstance();
      Context context = app.getApplicationContext();
      delegate = new PostgresRequestor(context, null);
      delegate.setDefaultsInterceptor(this); // Make sure our SQLiteDatabase creator gets called first otherwise we crash
      String sql;
      try
      {
         connection = delegate.connect("127.0.0.1", "eidb", "eidb", "eidb", errbuf);
         assertNotNull(errbuf.toString(), connection);
         ddl(connection, "CREATE TEMP TABLE test(col1 bigint, col2 text)");
         ddl(connection, "INSERT INTO test(col1, col2) VALUES (1, '1')");

         sql = "SELECT col1, col2 FROM test WHERE col1 = :id";
         Map<String, Object> params = new HashMap<>();
         Map<String, int[]> paramIndices = new HashMap<>();
         params.put("id", Long.valueOf(1));
         cursor = delegate.query(connection, sql, 0, null, params, paramIndices, errbuf);
         assertNotNull(sql, cursor);
         assertTrue(cursor.moveToFirst());
         assertFalse(cursor.moveToNext());
         //cursor.close(); new query should close cursor
         Cursor oldcursor = cursor;

         // Test cache age
         ddl(connection, "INSERT INTO test(col1, col2) VALUES (1, '2')");
         cursor = delegate.query(connection, sql, 1, TimeUnit.DAYS, params, paramIndices, errbuf); // aged 1 day so should reuse old
         assertTrue(oldcursor.isClosed()); // new query should close cursor
         assertNotNull(sql, cursor);
         assertTrue(cursor.moveToFirst());
         assertFalse(cursor.moveToNext());

         // aged 1 ns so should refresh
         cursor = delegate.query(connection, sql, 1, TimeUnit.NANOSECONDS, params, paramIndices, errbuf);
         assertNotNull(sql, cursor);
         assertTrue(cursor.moveToFirst());
         assertTrue(cursor.moveToNext());
      }
      finally
      {
         if (delegate != null)
            try { delegate.close(); } catch (Exception e) {}
         delegate = null;
         if (connection != null)
            try { connection.close(); } catch (Exception e) {}
         if (cursor != null)
            try { cursor.close(); } catch (Exception e) {}
      }
   }

   @Test
   public void testAsyncQuery() throws Exception
   //--------------------------------------------
   {
      Connection connection = null;
      Cursor cursor = null;
      StringBuilder errbuf = new StringBuilder();
      ShadowApplication app = ShadowApplication.getInstance();
      Context context = app.getApplicationContext();
      ActiveObject ao = new ActiveObject("testAsyncQuery", 2);
      delegate = new PostgresRequestor(context, ao);
      delegate.setDefaultsInterceptor(this); // Make sure our SQLiteDatabase creator gets called first otherwise we crash
      String sql;
      try
      {
         connection = delegate.connect("127.0.0.1", "eidb", "eidb", "eidb", errbuf);
         assertNotNull(errbuf.toString(), connection);
         ddl(connection, "CREATE TEMP TABLE test(col1 bigint, col2 text)");
         ddl(connection, "INSERT INTO test(col1, col2) VALUES (1, '1')");

         sql = "SELECT col1, col2 FROM test WHERE col1 = :id";
         Map<String, Object> params = new HashMap<>();
         Map<String, int[]> paramIndices = new HashMap<>();
         params.put("id", Long.valueOf(1));
         Anything token = new Anything();
         token.put("key", "value");
         Future<Cursor> future = delegate.query(connection, sql, 0, null, params, token, new ICursorQueryCallback()
         {
            @Override public void onQueried(Anything token, Cursor cursor, int retcode) { }

            @Override public void onError(Anything token, int code, CharSequence message, Throwable exception) { }

            @Override
            public void onJdbcQueried(Connection conn, Anything token, Cursor cursor, Map<String, Object> params,
                                      Map<String, int[]> paramIndices)
            {
               assertEquals(token.get("key", Anything.NOTHING).asString(""), "value");
               assertNotNull(cursor);
               assertTrue(cursor.moveToFirst());
               assertFalse(cursor.moveToNext());
            }

            @Override
            public void onJdbcError(Connection conn, Anything token, CharSequence message, Throwable exception)
            {
               assertTrue(message.toString(), false);
            }
         }, errbuf);
         assertNotNull(future);
         final Cursor oldcursor = future.get();

         // Test cache age
         ddl(connection, "INSERT INTO test(col1, col2) VALUES (1, '2')");


         future = delegate.query(connection, sql, 1, TimeUnit.DAYS, params, token, new ICursorQueryCallback()
         {
            @Override public void onQueried(Anything token, Cursor cursor, int retcode) { }

            @Override public void onError(Anything token, int code, CharSequence message, Throwable exception) { }

            @Override
            public void onJdbcQueried(Connection conn, Anything token, Cursor cursor, Map<String, Object> params,
                                      Map<String, int[]> paramIndices)
            {
               assertTrue(oldcursor.isClosed()); // new query should close cursor
               assertNotNull(cursor);
               assertTrue(cursor.moveToFirst());
               assertFalse(cursor.moveToNext());
            }

            @Override
            public void onJdbcError(Connection conn, Anything token, CharSequence message, Throwable exception)
            {
               assertTrue(message.toString(), false);
            }
         }, errbuf); // aged 1 day so should reuse old
         assertNotNull(future);
         future.get();

         // aged 1 ns so should refresh
         future = delegate.query(connection, sql, 1, TimeUnit.NANOSECONDS, params, token, new ICursorQueryCallback()
         {
            @Override public void onQueried(Anything token, Cursor cursor, int retcode) { }

            @Override public void onError(Anything token, int code, CharSequence message, Throwable exception) { }

            @Override
            public void onJdbcQueried(Connection conn, Anything token, Cursor cursor, Map<String, Object> params,
                                      Map<String, int[]> paramIndices)
            {
               try
               {
                  assertNotNull(cursor);
                  assertTrue(cursor.moveToFirst());
                  assertTrue(cursor.moveToNext());
               }
               catch (Throwable e)
               {
                  throw new RuntimeException(e);
               }
            }

            @Override
            public void onJdbcError(Connection conn, Anything token, CharSequence message, Throwable exception)
            {
               try { assertTrue(message.toString(), false); } catch (Throwable e) { throw new RuntimeException(e); }
            }
         }, errbuf);
         assertNotNull(future);
         future.get();
      }
      finally
      {
         if (delegate != null)
            try { delegate.close(); } catch (Exception e) {}
         delegate = null;
         if (connection != null)
            try { connection.close(); } catch (Exception e) {}
         if (cursor != null)
            try { cursor.close(); } catch (Exception e) {}
      }
   }

   @Test
   public void testConcurrentAsyncQuery() throws Exception
   //--------------------------------------------
   {
      final int noQueries = 5;
      Connection connection = null;
      Cursor cursor = null;
      StringBuilder errbuf = new StringBuilder();
      ShadowApplication app = ShadowApplication.getInstance();
      Context context = app.getApplicationContext();
      ActiveObject ao = new ActiveObject("testAsyncQuery", noQueries);
      delegate = new PostgresRequestor(context, ao);
      delegate.setDefaultsInterceptor(this); // Make sure our SQLiteDatabase creator gets called first otherwise we crash
      String sql = "SELECT col1, col2 FROM testandrop WHERE col1 = :id";
      Future<Cursor>[] futures = new Future[noQueries];
      Connection[] connections = null;
      try
      {
         connection = delegate.connect("127.0.0.1", "eidb", "eidb", "eidb", errbuf);
         assertNotNull(errbuf.toString(), connection);
         assertNotNull(errbuf.toString(), connection);
         ddl(connection, "DROP TABLE IF EXISTS testandrop");
         ddl(connection, "CREATE TABLE testandrop(col1 bigint, col2 text, col3 text)");
         for (int i=0; i<noQueries; i++)
            ddl(connection, "INSERT INTO testandrop(col1, col2) VALUES (" + i + ", 'val " + i + "')");
         connections = new Connection[noQueries];
         Arrays.fill(connections, null);
         for (int i=0; i<noQueries; i++)
         {

            Connection conn = delegate.connect("127.0.0.1", "eidb", "eidb", "eidb", errbuf);
            connections[i] = conn;
            Map<String, Object> params = new HashMap<>();
            Map<String, int[]> paramIndices = new HashMap<>();
            params.put("id", new QueryParameter(Types.BIGINT, Long.valueOf(i)));
            Anything token = new Anything();
            token.put("key", i);
            futures[i] = delegate.query(conn, sql, 0, null, params, token, new ICursorQueryCallback()
            {
               @Override public void onQueried(Anything token, Cursor cursor, int retcode) { }

               @Override public void onError(Anything token, int code, CharSequence message, Throwable exception) { }

               @Override
               public void onJdbcQueried(Connection conn, Anything token, Cursor cursor, Map<String, Object> params,
                                         Map<String, int[]> paramIndices)
               {
                  try
                  {
                     long no = token.get("key").asLong(- 1);
                     assertTrue(no >= 0);
                     assertNotNull(cursor);
                     assertTrue(cursor.moveToFirst());
                     long id = cursor.getLong(0);
                     assertEquals(id, no);
                  }
                  catch (Exception e)
                  {
                     throw new RuntimeException(e);
                  }
               }

               @Override
               public void onJdbcError(Connection conn, Anything token, CharSequence message, Throwable exception)
               {
                  try { assertTrue(message.toString(), false); } catch (Exception e) { throw new RuntimeException(e); }
               }
            }, errbuf);
            assertNotNull(futures[i]);
         }
         for (int i=0; i<noQueries; i++)
            futures[i].get();
         for (int i=0; i<noQueries; i++)
            connections[i].close();
      }
      finally
      {
         try { ddl(connection, "DROP TABLE IF EXISTS testandrop"); } catch (Exception _e) {}
         if (delegate != null)
            try { delegate.disconnect(null); } catch (Exception e) {}
         delegate = null;

         if (connection != null)
            try { connection.close(); } catch (Exception e) {}
         if (cursor != null)
            try { cursor.close(); } catch (Exception e) {}
         if (connections != null)
         {
            for (int i=0; i<noQueries; i++)
               if (connections[i] != null)
                  connections[i].close();
         }
      }
   }

   //Override for Robolectric database creation
   @Override
   public SQLiteDatabase onGetAndroidDatabase(File databaseDir, StringBuilder errbuf)
   //--------------------------------------------------------------------------------
   {
      try
      {
         ShadowApplication app = ShadowApplication.getInstance();
         Context cont = app.getApplicationContext();
         assertNotNull(cont);
         return cont.openOrCreateDatabase("ardata", 0, null);
         //      ShadowSQLiteConnection conn = new ShadowSQLiteConnection();
      }
      catch (Throwable e)
      {
         e.printStackTrace(System.err);
         return null;
      }
   }

   @Override
   protected String tag() { return "JdbcRequestorTest"; }

   @Override
   protected String onGetUrl(String host, Integer port, String database, String user, String password,
                             long connectionTimeout, long timeout, boolean isSSL)
   {
      if (delegate == null)
         return "url";
      else
         return delegate.onGetUrl(host, port, database, user, password, connectionTimeout, timeout, isSSL);
   }

   @Override
   protected String onGetDriver()
   {
      if (delegate == null)
         return "driver";
      else
         return delegate.onGetDriver();
   }

   @Override
   protected int onGetPort()
   {
      if (delegate == null)
         return -1;
      else
         return delegate.onGetPort();
   }

   @Override
   public DatabaseType type()
   {
      if (delegate == null)
         return DatabaseType.POSTGRES;
      else
         return delegate.type();
   }


   private void ddl(Connection connection, String sql) throws SQLException
   //---------------------------------------------------------------------
   {
      Statement st = null;
      try
      {
         st = connection.createStatement();
         st.executeUpdate(sql);
      }
      catch (SQLException e)
      {
         System.err.println(sql + " : " + e.getMessage());
         throw e;
      }
      finally
      {
         if (st != null)
            try { st.close(); } catch (Exception e) {}
      }
   }

   @Override
   public ISpatialFunctions spatialFunctions()
   {
      return (delegate == null)
             ? null
             : delegate.spatialFunctions();
   }

   /*
   @Test
   public void testPreProcess() throws Exception
   //-------------------------------------------
   {
      StringBuilder queryBuf = new StringBuilder();
      String sql;
      ParsedPreparedStatement pps;
      List<String> cols;
//      sql = "select concat(unit, ' ',description), sensor_type_id*3, location_id*2 from sensor where sensor_type_id = 1";
//      assertTrue(instance.preProcess(sql, queryBuf) == QUERY_TYPE.SELECT);
//      sql = "select concat(unit, ' ',description), sensor_type_id*3, location_id*2 from sensor WHERE sensor_type_id = ?";
//      assertTrue(instance.preProcess(sql, queryBuf) == QUERY_TYPE.SELECT);
//      sql = "select concat(unit, ' ',description), sensor_type_id*3, location_id*2 from sensor WHERE sensor_type_id = ? AND location_id < ?";
//      assertTrue(instance.preProcess(sql, queryBuf) == QUERY_TYPE.SELECT);

//      sql = "select concat(col1, ' ',col2), col3, col4 from table WHERE col1 = ? AND col2 BETWEEN ? AND ?";
//      pps = instance.preProcess(sql, queryBuf);
//      assertNotNull(pps);
//      assertEquals(pps.getQueryType(), ParsedPreparedStatement.QUERY_TYPE.SELECT);
//      assertEquals(3, pps.getCount());
//      cols = pps.getColumns();
//      assertEquals(2, cols.size());
//      assertEquals("sensor_type_id", cols.get(0));
//      assertEquals("location_id", cols.get(1));

//      sql = "SELECT col1, col2 FROM table1 WHERE col2 IN (?, ?, ?)";
//      pps = instance.preProcess(sql, queryBuf);
//      assertNotNull(pps);
//      assertEquals(pps.getQueryType(), ParsedPreparedStatement.QUERY_TYPE.SELECT);
//      assertEquals(3, pps.getCount());
//      cols = pps.getColumns();
//      assertEquals(1, cols.size());
//      assertEquals("col2", cols.get(0));
                                                 /ssd/Android/ARData/ARDataFramework/AndroidManifest.xml
      sql = "select description, ST_AsText(wgs84_location) as location from location where wgs84_location && ST_MakeEnvelope(-34, 24,-35, 26,4326)";
      pps = instance.preProcess(sql, queryBuf);

      sql = "select description, ST_AsText(wgs84_location) as location from location where ST_DWITHIN(wgs84_location, ST_MakePoint(-35, 24), 5000)";
      pps = instance.preProcess(sql, queryBuf);

      sql = "select description, ST_AsText(wgs84_location) as location from location where ST_Intersects(wgs84_location, ST_Buffer(ST_MakePoint(-34.5, 24), 1000))";
      pps = instance.preProcess(sql, queryBuf);

      // Oracle
      sql = "SELECT c.name FROM cola_markets c WHERE SDO_WITHIN_DISTANCE(c.shape," +
            "  SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1,1003,3)," +
            "    SDO_ORDINATE_ARRAY(4,6, 8,8))," +
            "  'distance=10') = 'TRUE'";
      pps = instance.preProcess(sql, queryBuf);

      //SQL Server
      sql = "select wgs84_location.STAsText() from locations where wgs84_location.STIntersects(geography::STPointFromText('POINT(-34.5, 24)', 4326).STBuffer(1000))=1";
      pps = instance.preProcess(sql, queryBuf);
   }
   */
}
