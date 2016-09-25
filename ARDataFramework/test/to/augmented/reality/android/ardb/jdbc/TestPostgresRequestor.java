package to.augmented.reality.android.ardb.jdbc;

import org.junit.*;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestPostgresRequestor
//===============================
{
   static String HOST = "127.0.0.1"; //"10.0.2.2";


   @Test
   public void testSpatialQuery() throws Exception
   //---------------------------------------
   {
      ActiveObject ao = new ActiveObject("Postgres", 5);
      PostgresRequestor requestor = new PostgresRequestor(ao);
      StringBuilder errbuf = new StringBuilder();
      Connection connection = requestor.connect(HOST, IJdbcRequestor.POSTGRES_TCP_PORT, "eidb", "eidb", "eidb",
                                                30, TimeUnit.SECONDS, 30, TimeUnit.SECONDS, false, errbuf);
      assertNotNull(errbuf.toString(), connection);
      ao.stop();
   }
}
