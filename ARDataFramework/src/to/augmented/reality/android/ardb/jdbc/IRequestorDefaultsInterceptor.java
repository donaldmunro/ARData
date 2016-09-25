package to.augmented.reality.android.ardb.jdbc;

import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileNotFoundException;

public interface IRequestorDefaultsInterceptor
//============================================
{
   /**
    * Create the instance of a IJdbcRequestHandler that will be used to perform low level JDBC operations.
    * @return The IJdbcRequestHandler or <i>null</i> to use the default handler.
    */
   IJdbcRequestHandler onCreateRequestHandler();

   /**
    * The the JDBC driver to use to connect to the database.
    * @param type The database type
    * @return The driver name or null to use the default.
    */
   String onGetDriver(DatabaseType type);

   /**
    * The TCP port to connecto to the database
    * @param type The database type
    * @return The TCP port or -1 to use the default
    */
   int onGetPort(DatabaseType type);

   /**
    * Get the JDBC URL used to connect to the database.
    * @param type The database type
    * @param host The network address of the database server.
    * @param port The network port of the database server.
    * @param database The database name
    * @param user The database user
    * @param password The database password
    * @param connectionTimeout The database connection timeout in milliseconds.
    * @param timeout The database read timeout in milliseconds.
    * @param isSSL <i>true</i> if a SSL (secure) connection is required.
    * @return The JDBC URL or null to use the default.
    */
   String onGetUrl(DatabaseType type, String host, Integer port, String database, String user, String password,
                   long connectionTimeout, long timeout, boolean isSSL);

   /**
    * Get the directory to use for caching local copies of database queries.
    * @return The local (Android) database directory or null to use the default.
    */
   File onGetDatabaseDir() throws FileNotFoundException;

   /**
    * Get the local (Android) database used for caching local copies of database queries.
    * @param databaseDir The directory to use when creating the database (@link #onGetDatabaseDir)
    * @param errbuf Error messages will be returned in here (can be null).
    * @return The local database or null to use the default.
    */
   SQLiteDatabase onGetAndroidDatabase(File databaseDir, StringBuilder errbuf);
}
