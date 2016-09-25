package to.augmented.reality.android.ardb.jdbc;

import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

public class JtdsInterceptor implements IRequestorDefaultsInterceptor
//===================================================================
{
   @Override public IJdbcRequestHandler onCreateRequestHandler() { return null; }

   @Override
   public String onGetDriver(DatabaseType type)
   //------------------------------------------
   {
      if (type == DatabaseType.SQLSERVER)
         return IJdbcRequestor.JTDS_SQLSERVER_DRIVER;
      return null;
   }

   @Override
   public int onGetPort(DatabaseType type)
   //-------------------------------------
   {
      if (type == DatabaseType.SQLSERVER)
         return IJdbcRequestor.SQLSERVER_TCP_PORT;
      return -1;
   }

   @Override
   public String onGetUrl(DatabaseType type, String host, Integer port, String database, String user, String password,
                          long connectionTimeout, long timeout, boolean isSSL)
   //-----------------------------------------------------------------------------------------------------------------
   {
      String url = null;
      if (type == DatabaseType.SQLSERVER)
      {
         url = String.format(Locale.getDefault(), "jdbc:jtds:sqlserver://%s:%d", host, port);
         if ((database != null) && (! database.trim().isEmpty()))
            url += "/" + database;
         if ((user != null) && (! user.trim().isEmpty()))
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

   @Override public File onGetDatabaseDir() throws FileNotFoundException { return null; }

   @Override public SQLiteDatabase onGetAndroidDatabase(File databaseDir, StringBuilder errbuf) { return null; }
}
