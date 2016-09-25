package to.augmented.reality.android.ardb.util;

import org.junit.Test;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.jdbc.IJdbcRequestor;
import to.augmented.reality.android.ardb.jdbc.PostgresRequestor;
import to.augmented.reality.android.ardb.jdbc.QueryMetaData;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class DatabaseUtilsTest
{
   static String HOST = "127.0.0.1";

   @Test
   public void testCreateLocalSQL() throws Exception
   //---------------------------------------------------
   {
      ActiveObject ao = new ActiveObject("Postgres", 5);
      Connection connection = null;
      Statement st = null;
      ResultSet rs = null;
      String sql =
      "SELECT sensor_id, sensor_type.description AS type_description, location_id, unit, " +
            "sensor.description AS description " +
            "FROM sensor INNER JOIN sensor_type ON sensor.sensor_type_id = sensor_type.sensor_type_id";
      try
      {
         StringBuilder errbuf = new StringBuilder();
         PostgresRequestor requestor = new PostgresRequestor(ao);
         connection = requestor.connect(HOST, IJdbcRequestor.POSTGRES_TCP_PORT, "eidb", "eidb", "eidb",
                                                   30, TimeUnit.SECONDS, 30, TimeUnit.SECONDS, false, errbuf);
         assertNotNull(errbuf.toString(), connection);
         st = connection.createStatement();
         rs = st.executeQuery(sql);
         StringBuilder create = new StringBuilder();
         StringBuilder insert = new StringBuilder();
         StringBuilder select = new StringBuilder();
         DatabaseUtils.createLocalSQL(new QueryMetaData(rs.getMetaData()), "test", true, false, create, insert, select);
         assertTrue(true);
      }
      finally
      {
         try { ao.stop(); } catch (Exception _e) {}
         if (rs != null)
            try { rs.close(); } catch (Exception _e) {}
         if (st != null)
            try { st.close(); } catch (Exception _e) {}
         if (connection != null)
            try { connection.close(); } catch (Exception _e) {}
      }
   }
}
