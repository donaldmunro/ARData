package to.augmented.reality.android.ardb.jdbc;

import android.content.Context;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.spi.ISpatialFunctions;

import java.util.Locale;

public class SQLServerRequestor extends JdbcRequestor implements IJdbcRequestor, ISpatialFunctionProvider
//=======================================================================================================
{
   @Override protected String tag() { return this.getClass().getName(); }

   protected SQLServerSpatialFunctions spatialFunctions = null;

   public SQLServerRequestor(ActiveObject activeObject) { super(activeObject); }

   public SQLServerRequestor(Context context, ActiveObject activeObject) { super(context, activeObject); }

   @Override public DatabaseType type() { return DatabaseType.SQLSERVER; }

   @Override
   protected String onGetDriver() { return MS_SQLSERVER_DRIVER; }

   @Override protected int onGetPort() { return SQLSERVER_TCP_PORT; }

   @Override
   protected String onGetUrl(String host, Integer port, String database, String user, String password,
                             long connectionTimeout, long timeout, boolean isSSL)
   //------------------------------------------------------------------------------------------------------
   {
      String url = null;
      if ((host == null) || (host.isEmpty()))
         host = "127.0.0.1";
      if ((port == null) || (port < 0))
         port = 1433;
      if (onGetDriver().equals(MS_SQLSERVER_DRIVER))
      {
         url = String.format(Locale.getDefault(), "jdbc:sqlserver://%s:%d;user=%s;password=%s", host, port, user, password);
         if ( (database != null) && (! database.trim().isEmpty()) )
            url += ";databaseName=" + database;
         if (connectionTimeout > 0)
            url += ";loginTimeout=" + connectionTimeout;
         if (isSSL)
            url += ";encrypt=true";
      }
      else if (onGetDriver().equals(JTDS_SQLSERVER_DRIVER))
      {
         url = String.format(Locale.getDefault(), "jdbc:jtds:sqlserver://%s:%d", host, port);
         if ( (database != null) && (! database.trim().isEmpty()) )
            url += "/" + database;
         if ( (user != null) && (! user.trim().isEmpty()) )
            url += ";user=" + user + ((password == null) ? "" : ";password=" + password);
         if (connectionTimeout > 0)
            url += ";loginTimeout=" + connectionTimeout;
         if (timeout > 0)
            url += ";socketTimeout=" + timeout;
         if (isSSL)
            url += ";ssl=request";
      }

      return url;
   }

   @Override public ISpatialFunctions spatialFunctions()
   //---------------------------------------------------
   {
      if (spatialFunctions == null)
         spatialFunctions = new SQLServerSpatialFunctions();
      return spatialFunctions;
   }
}
