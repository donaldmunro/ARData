package to.augmented.reality.android.ardb.jdbc;

import android.content.Context;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class PostgresRequestor extends JdbcRequestor implements IJdbcRequestor, ISpatialFunctionProvider
//======================================================================================================
{
   @Override protected String tag() { return this.getClass().getName(); }

   protected PostgresSpatialFunctions spatialFunctions = null;

   public PostgresRequestor(ActiveObject activeObject) { super(activeObject); }

   public PostgresRequestor(Context context, ActiveObject activeObject) { super(context, activeObject); }

   @Override public DatabaseType type() { return DatabaseType.POSTGRES; }

   @Override protected String onGetDriver() { return POSTGRES_DRIVER; }

   @Override protected int onGetPort() { return POSTGRES_TCP_PORT; }

   @Override
   protected String onGetUrl(String host, Integer port, String database, String user, String password,
                             long connectionTimeout, long timeout, boolean isSSL)
   //------------------------------------------------------------------------------------------------------
   {
      if ( (host == null) || (host.isEmpty()) )
         host = "127.0.0.1";
      if ( (port == null) || (port < 0) )
         port = 5432;
      if (database == null)
         database = "template1";
      String url =  String.format(Locale.getDefault(), "jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                                  host, port, database, user, password);
      if (connectionTimeout > 0)
         url += "&loginTimeout=" + connectionTimeout;
      if (timeout > 0)
         url += "&socketTimeout=" + timeout;
      if (isSSL)
         url += "&ssl=true";
      url += "&logUnclosedConnections=true";
      return url;
   }

   @Override public ISpatialFunctions spatialFunctions()
   //---------------------------------------------------
   {
      if (spatialFunctions == null)
         spatialFunctions = new PostgresSpatialFunctions();
      return spatialFunctions;
   }
}
