package to.augmented.reality.android.ardb.jdbc;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;
import to.augmented.reality.android.ardb.anything.Anything;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface IJdbcRequestor extends Closeable
//===============================================
{
   int POSTGRES_TCP_PORT = 5432;
   String POSTGRES_DRIVER = "org.postgresql.Driver";

   int SQLSERVER_TCP_PORT = 1433;
   String MS_SQLSERVER_DRIVER = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
   String JTDS_SQLSERVER_DRIVER = "net.sourceforge.jtds.jdbc.Driver";

   DatabaseType type();

   void setDefaultsInterceptor(IRequestorDefaultsInterceptor interceptor);

   Future<Connection> connect(String host, String database, String user, String password,
                              IJdbcConnectCallback callback, Anything token,
                              StringBuilder errbuf);

   Future<Connection> connect(String host, int port, String database, String user, String password,
                              long connectionTimeout, TimeUnit connectionTimeoutUnit,
                              long readTimeout, TimeUnit readTimeoutUnit, boolean isSSL,
                              IJdbcConnectCallback callback, Anything token, StringBuilder errbuf);

   Connection connect(String host, String database, String user, String password, StringBuilder errbuf)
         throws SQLException;

   Connection connect(String host, int port, String database, String user, String password,
                      long connectionTimeout, TimeUnit connectionTimeoutUnit,
                      long readTimeout, TimeUnit readTimeoutUnit, boolean isSSL,
                      StringBuilder errbuf) throws SQLException;

   void disconnect(Connection connection);

   boolean isConnected(Connection connection, int timeoutSeconds);

   Future<Boolean> isConnected(Connection connection, int timeoutSeconds, IJdbcConnectCallback callback, Anything token);

   void setLocalDatabase(SQLiteDatabase localDatabase);

   Future<Cursor> query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                        Map<String, Object> params, Anything token, ICursorQueryCallback callback,
                        StringBuilder errbuf);

   Future<Cursor> query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                        Map<String, Object> params, SQLiteDatabase localDatabase,
                        Anything token, ICursorQueryCallback callback,
                        StringBuilder errbuf);

   Cursor query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                Map<String, Object> params, Map<String, int[]> paramIndices,
                StringBuilder errbuf);

   Cursor query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                Map<String, Object> params, Map<String, int[]> paramIndices, SQLiteDatabase localDatabase,
                StringBuilder errbuf);

   Future<Integer> modify(Connection connection, String query, Map<String, Object> params, IJdbcModifyCallback callback,
                          Anything token, StringBuilder errbuf);

   int modify(Connection connection, String query, Map<String, Object> params, Map<String, int[]> paramIndices,
              StringBuilder errbuf);

   void destroy(String query);

   void disconnectCursor(String query);

   void addInvalidationListener(String query, JdbcRequestor.ICursorInvalidatedListener listener);
}
