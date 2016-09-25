package to.augmented.reality.android.ardb.jdbc;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;
import to.augmented.reality.android.ardb.util.Md5Encrypter;
import to.augmented.reality.android.ardb.concurrency.NoFuture;
import to.augmented.reality.android.ardb.anything.Anything;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class JdbcRequestor implements IJdbcRequestor, IRequestorDefaultsInterceptor, ISpatialFunctionProvider
//====================================================================================================================
{
   static final private String TAG = JdbcRequestor.class.getName();

//   protected static TeaEncrypter ENCRYPTER;
//   static
//   {
//      Date now = new Date();
//      BigInteger B = new BigInteger(Long.valueOf(now.getTime()*1000).toString(), 10);
//      String s = B.toString(16);
//      for (int i=s.length(); i<16; i++)
//         s += '0';
//      ENCRYPTER = new TeaEncrypter(s.getBytes());
//   }

   protected Context context;
   protected ActiveObject activeObject;
   protected volatile SQLiteDatabase localDatabase;
   final protected  IJdbcRequestHandler defaultHandler = new JdbcRequestHandler();
   protected File databaseDir = null;
   protected String databaseName = null;
   protected File databaseFile = null;
   private boolean isDatabaseDisconnected = false;

   public interface ICursorInvalidatedListener
   {
      void onCursorInvalidated(String query, Cursor oldCursor, Cursor newCursor);
   }

   protected static class TableInfo
   //==============================
   {
      String query;
      long accessTime;
      Cursor cursor;
      boolean isCursorDisconnected = false;
      List<ICursorInvalidatedListener> invalidationListeners = null;

      public TableInfo(String query, long accessTime, Cursor cursor)
      //------------------------------------------------------------
      {
         this.query = query;
         this.accessTime = accessTime;
         this.cursor = cursor;
      }

      synchronized public void addInvalidationListener(ICursorInvalidatedListener listener)
      //----------------------------------------------------------------------
      {
         if (invalidationListeners == null)
            invalidationListeners = new ArrayList<>();
         invalidationListeners.add(listener);
      }

      synchronized void notifyListeners(Cursor newCursor)
      //-------------------------------------------------
      {
         if (invalidationListeners != null)
         {
            for (ICursorInvalidatedListener listener : invalidationListeners)
               listener.onCursorInvalidated(query, this.cursor, newCursor);
         }
      }
   }

   final private ConcurrentMap<String, TableInfo> tables = new ConcurrentHashMap<>();
   private IRequestorDefaultsInterceptor defaultsInterceptor = this;
//   final protected List<IRequestorDefaultsInterceptor> interceptors =
//         Collections.synchronizedList(new ArrayList<IRequestorDefaultsInterceptor>());

   public JdbcRequestor() { }

   public JdbcRequestor(Context context) { this.context = context; }

   public JdbcRequestor(ActiveObject activeObject) { this.activeObject = activeObject; }

   public JdbcRequestor(Context context, ActiveObject activeObject) { this(context, activeObject, null, null); }

   public JdbcRequestor(Context context, ActiveObject activeObject, File androidDatabaseDir, String androidDatabaseName)
   //------------------------------------------------------------------------------------------------------------------
   {
      if (androidDatabaseDir != null)
      {
         if (! androidDatabaseDir.exists())
            throw new RuntimeException("Database directory " + androidDatabaseDir.getAbsolutePath() + " does not exist");
         if (! androidDatabaseDir.isDirectory())
            throw new RuntimeException("Database directory " + androidDatabaseDir.getAbsolutePath() + " is not a directory");
         if (! androidDatabaseDir.canWrite())
            throw new RuntimeException("Database directory " + androidDatabaseDir.getAbsolutePath() + " is not writable");
         databaseDir = androidDatabaseDir;
      }
      databaseName = androidDatabaseName;
      this.context = context;
      this.activeObject = activeObject;
   }

   public JdbcRequestor(Context context, ActiveObject activeObject, SQLiteDatabase database)
   //------------------------------------------------------------------------------------------------------------------
   {
      if ( (! database.isOpen()) || (database.isReadOnly()) )
         throw new RuntimeException("Closed or non-writable database");
      this.context = context;
      this.activeObject = activeObject;
      this.localDatabase = database;
      this.isDatabaseDisconnected = true;
   }

   @Override protected void finalize() throws Throwable { disconnect(null); }

   @Override
   public void setDefaultsInterceptor(IRequestorDefaultsInterceptor interceptor)
   //--------------------------------------------------------------------------------
   {
      if (interceptor == null)
         defaultsInterceptor = this;
      else
         defaultsInterceptor = interceptor;
   }

   // Start IRequestorDefaultsInterceptor =================================================================================

   @Override final public IJdbcRequestHandler onCreateRequestHandler() { return defaultHandler; }

   @Override final public String onGetDriver(DatabaseType type) { return this.onGetDriver(); }

   @Override public int onGetPort(DatabaseType type) { return this.onGetPort(); }

   @Override
   public final String onGetUrl(DatabaseType type, String host, Integer port, String database, String user, String password,
                                long connectionTimeout, long timeout, boolean isSSL)
   //-------------------------------------------------------------------------------------------
   {
      return this.onGetUrl(host, port, database, user, password, connectionTimeout, timeout, isSSL);
   }

   public final File onGetDatabaseDir() throws FileNotFoundException
   //---------------------------------------------------------------
   {
      if (databaseDir == null)
      {
         synchronized (this)
         {
            if (databaseDir == null)
            {
//             SQLiteOpenHelper dummy = new SQLiteOpenHelper(context, "db", null, 1)
//             {
//                @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
//                @Override public void onCreate(SQLiteDatabase db) {}
//             };
//             SQLiteDatabase db = dummy.getWritableDatabase();
//             db.execSQL("CREATE TEMP TABLE messages (read_status INTEGER, direction INTEGER, target TEXT)");
               if (context != null)
               {
                  databaseDir = new File(context.getExternalFilesDir(null), context.getPackageName());
                  databaseDir = new File(databaseDir, "ardata");
               } else
                  databaseDir = new File(Environment.getDataDirectory(), "ardata/database");

               if (! databaseDir.exists())
               {
                  databaseDir.mkdirs();
                  if (! databaseDir.exists())
                  {
                     Log.e(tag(), "Could not create directory on sdcard: " + databaseDir);
                     throw new FileNotFoundException("Could not create directory on sdcard: " + databaseDir);
                  }
               }
            }
         }
      }
      return databaseDir;
   }

   public SQLiteDatabase onGetAndroidDatabase(File databaseDir, StringBuilder errbuf)
   //------------------------------------------------------------------------------------
   {
      if (localDatabase == null)
      {
         synchronized (this)
         {
            if (localDatabase == null)
            {
               if (! databaseDir.exists())
                  databaseDir.mkdirs();
               if (! databaseDir.canWrite())
                  databaseDir.setWritable(true, true);
               if ( (! databaseDir.exists()) || (! databaseDir.canWrite()) )
                  return null;
               File f;
               if (databaseName != null)
                  f = new File(databaseDir, databaseName);
               else
               {
                  try
                  {
                     f = File.createTempFile("tmp", ".sqlite", databaseDir);
                  }
                  catch (Exception e)
                  {
                     f = new File(databaseDir, Long.toHexString(System.nanoTime()));
                  }
                  this.databaseName = f.getName();
               }
               localDatabase = SQLiteDatabase.openOrCreateDatabase(f, null);
               databaseFile = f;
               if ( (this.databaseDir == null) || (! databaseDir.equals(this.databaseDir)) )
                  this.databaseDir = databaseDir;

            }
         }
      }
      return localDatabase;
   }

   public void setLocalDatabase(SQLiteDatabase androidDatabase)
   //--------------------------------------------------------
   {
      if ( (! androidDatabase.isOpen()) || (androidDatabase.isReadOnly()) )
         throw new RuntimeException("Closed or non-writable database");
      if (localDatabase != null)
         closeAndroidDatabase(localDatabase);
      localDatabase = androidDatabase;
   }

   private SQLiteDatabase getDatabase(StringBuilder errbuf) throws FileNotFoundException
   //----------------------------------------------------------------------------
   {
      if (localDatabase == null)
      {
         synchronized (this)
         {
            if (localDatabase == null)
            {
               if (databaseDir == null)
               {
                  if (databaseDir == null)
                     databaseDir = defaultsInterceptor.onGetDatabaseDir();
                  if (databaseDir == null)
                     databaseDir = this.onGetDatabaseDir();
               }
               if (databaseDir == null)
               {
                  if (errbuf != null)
                     errbuf.append("Could not obtain local database directory");
                  return null;
               }
               if (! databaseDir.exists())
                  databaseDir.mkdirs();
               if (! databaseDir.canWrite())
                  databaseDir.setWritable(true, true);
               if ( (! databaseDir.exists()) || (! databaseDir.canWrite()) )
               {
                  if (errbuf != null)
                     errbuf.append("Could not create or make writable database directory " + databaseDir);
                  Log.e(TAG, "Could not create or make writable database directory " + databaseDir);
                  return null;
               }
               localDatabase = defaultsInterceptor.onGetAndroidDatabase(databaseDir, errbuf);
               if (localDatabase == null)
                  localDatabase = this.onGetAndroidDatabase(databaseDir, errbuf);
            }
         }
      }

      if (localDatabase == null)
      {
         if (errbuf != null)
            errbuf.append("Could not open local database in directory " + databaseDir);
         return null;
      }
      return localDatabase;
   }

   private void closeAndroidDatabase(SQLiteDatabase androidDatabase)
   //---------------------------------------------------------------
   {
      if (tables != null)
      {
         for (String table : tables.keySet())
         {
            TableInfo ti = tables.remove(table);
            try
            {
               if ((ti != null) && (ti.cursor != null) &&  (! ti.isCursorDisconnected) )
               {
                  if (! ti.cursor.isClosed())
                     try { ti.cursor.close(); } catch (Exception _e) {}
                  androidDatabase.execSQL("DROP TABLE " + table);
               }
            }
            catch (Exception e)
            {
               Log.e(tag(), "", e);
            }
         }
      }
      if (! isDatabaseDisconnected)
         try { androidDatabase.close(); } catch (Exception e) { Log.e("CloseThread", "", e); }
      if (androidDatabase == localDatabase)
         localDatabase = null;
   }

   protected String onGetTableName(String query)
   //-------------------------------------------
   {
      try { return "Q" + new Md5Encrypter(query).getHexHash(); } catch (NoSuchAlgorithmException e) { throw new RuntimeException("WTF: No MD5!!!"); }
   }

   // End IRequestorDefaultsInterceptor=========================================================================

   abstract protected String tag();

   abstract protected String onGetUrl(String host, Integer port, String database, String user, String password,
                                      long connectionTimeout, long timeout, boolean isSSL);

   abstract protected String onGetDriver();

   abstract protected int onGetPort();

   // Start IJdbcRequestor ===========================================================================================

   @Override
   public Connection connect(String host, String database, String user, String password, StringBuilder errbuf)
   throws SQLException
   //-------------------------------------------------------------------------------------------------------
   {
      return connect(host, -1, database, user, password, 60, TimeUnit.SECONDS, 30, TimeUnit.SECONDS,
                     false, errbuf);
   }

   @Override
   public Connection connect(String host, int port, String database, String user, String password, long connectionTimeout,
                            TimeUnit connectionTimeoutUnit, long readTimeout, TimeUnit readTimeoutUnit, boolean isSSL,
                            StringBuilder errbuf)
         throws SQLException
   //-----------------------------------------------------------------------------------------------------------------
   {
      String driver = defaultsInterceptor.onGetDriver(type());
      if (driver == null)
         driver = this.onGetDriver();
      if (port < 0)
      {
         port = defaultsInterceptor.onGetPort(type());
         if (port < 0)
            port = this.onGetPort();
      }
      String url = defaultsInterceptor.onGetUrl(type(), host, port, database, user, password,
                                                TimeUnit.SECONDS.convert(connectionTimeout, connectionTimeoutUnit),
                                                TimeUnit.SECONDS.convert(readTimeout, readTimeoutUnit), isSSL);
      if (url == null)
         url = this.onGetUrl(host, port, database, user, password,
                             TimeUnit.SECONDS.convert(connectionTimeout, connectionTimeoutUnit),
                             TimeUnit.SECONDS.convert(readTimeout, readTimeoutUnit), isSSL);
      try
      {
         IJdbcRequestHandler handler = defaultsInterceptor.onCreateRequestHandler();
         if (handler == null)
            handler = defaultHandler;
         return handler.connect(driver, url, errbuf);
      }
      catch (ClassNotFoundException e)
      {
         if (errbuf == null)
            errbuf = new StringBuilder();
         errbuf.append("Class not found exception loading JDBC driver ").append(driver).
                append(". Check if necessary jars are in classpath");
         Log.e(tag(), errbuf.toString(), e);
         throw new RuntimeException(errbuf.toString(), e);
      }
   }

   @Override
   public Future<Connection> connect(String host, String database, String user, String password,
                                  IJdbcConnectCallback callback, Anything token, StringBuilder errbuf)
   //---------------------------------------------------------------------------------------------------
   {
      return connect(host, -1, database, user, password, 30, TimeUnit.SECONDS, 15, TimeUnit.SECONDS,
                     false, callback, token, errbuf);
   }

   @Override
   public Future<Connection> connect(String host, int port, String databaseName, String user, String password,
                            long connectionTimeout, TimeUnit connectionTimeoutUnit, long readTimeout,
                            TimeUnit readTimeoutUnit, boolean isSSL, IJdbcConnectCallback callback, Anything token,
                            StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      ConnectThread t = new ConnectThread(host, port, databaseName, user, password, connectionTimeout, connectionTimeoutUnit,
                                          readTimeout, readTimeoutUnit, isSSL, callback, token);
      if (activeObject == null)
      {
         StringBuilder sb = new StringBuilder("Synchronous call to ").append(this.getClass().getName()).
               append(" with no active object scheduler");
         Log.e(tag(), sb.toString());
         throw new RuntimeException(sb.toString());
      }
      return (Future<Connection>) activeObject.scheduleWithFuture(t);
   }

   @Override public void close() throws IOException { disconnect(null); }

   @Override
   public void disconnect(Connection connection)
   //-------------------------------------------
   {
      try
      {
         if (connection != null)
            try { connection.close(); } catch (Exception e) { Log.e("CloseThread", "", e); }
         connection = null;
         if (localDatabase != null)
            closeAndroidDatabase(localDatabase);
      }
      catch (Exception E)
      {
         Log.e(tag(), "", E);
      }
   }

   @Override
   public void disconnectCursor(String query)
   //----------------------------------------
   {
      String tableName = onGetTableName(query);
      TableInfo ti = tables.get(tableName);
      ti.isCursorDisconnected = true;
   }

   @Override
   public void addInvalidationListener(String query, ICursorInvalidatedListener listener)
   //------------------------------------------------------------------------------------
   {
      String tableName = onGetTableName(query);
      TableInfo ti = tables.get(tableName);
      ti.addInvalidationListener(listener);
   }

   @Override
   public boolean isConnected(Connection connection, int timeoutSeconds)
   //-------------------------------------------------------------------
   {
      if (connection == null)
         return false;
      try
      {
         return connection.isValid(timeoutSeconds);
      }
      catch (SQLException e)
      {
         return false;
      }
   }

   @Override
   public Future<Boolean> isConnected(final Connection connection, final int timeoutSeconds,
                                      final IJdbcConnectCallback callback, final Anything token)
   //-------------------------------------------------------------------------------------------------------
   {
      if (connection == null)
      {
         callback.onError(token, "Not connected", null);
         return new NoFuture<Boolean>(false);
      }
      Callable<Boolean> thread = new Callable<Boolean>()
      //================================================
      {
         @Override
         public Boolean call() throws Exception
         //-------------------------------------
         {
            boolean b = isConnected(connection, timeoutSeconds);
            if (b)
               callback.onConnected(token, connection);
            else
               callback.onError(token, "Not connected", null);
            return b;
         }
      };
      return (Future<Boolean>) activeObject.scheduleWithFuture(thread);
   }

   @Override
   public Future<Cursor> query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                               Map<String, Object> params, Anything token, ICursorQueryCallback callback,
                               StringBuilder errbuf)
   //----------------------------------------------------------------------------------------------------
   {
      return query(connection, query, cacheAge, cacheAgeUnit, params, localDatabase, token, callback, errbuf);
   }

   @Override
   public Future<Cursor> query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                        Map<String, Object> params, SQLiteDatabase androidDatabase,
                        Anything token, ICursorQueryCallback callback,
                        StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------------
   {
      if (! query.trim().toUpperCase().startsWith("SELECT"))
      {
         if (errbuf != null)
            errbuf.append("Not a SELECT query");
         Log.e(tag(), "Not a SELECT query");
         return null;
      }
      QueryThread t = new QueryThread(connection, query, cacheAge, cacheAgeUnit, params, androidDatabase, callback, token);
      if (activeObject == null)
      {
         StringBuilder sb = new StringBuilder("Synchronous call to ").append(this.getClass().getName()).
               append(" with no active object scheduler");
         Log.e(tag(), sb.toString());
         throw new RuntimeException(sb.toString());
      }
      return (Future<Cursor>) activeObject.scheduleWithFuture(t);
   }

   @Override
   public Cursor query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                       Map<String, Object> params, Map<String, int[]> paramIndices, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------
   {
      return query(connection, query, cacheAge, cacheAgeUnit, params, paramIndices, localDatabase, errbuf);
   }

   @Override
   public Cursor query(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                       Map<String, Object> params, Map<String, int[]> paramIndices, SQLiteDatabase androidDatabase,
                       StringBuilder errbuf)
   //----------------------------------------------------------------------------------------------------
   {
      Cursor cursor = null;
      if (! query.trim().toUpperCase().startsWith("SELECT"))
      {
         if (errbuf != null)
            errbuf.append("Not a SELECT query");
         Log.e(tag(), "Not a SELECT query");
         return null;
      }
      IJdbcRequestHandler handler = defaultsInterceptor.onCreateRequestHandler();
      if (handler == null)
         handler = defaultHandler;
      try
      {
         if (androidDatabase == null)
         {
            androidDatabase = getDatabase(errbuf);
            if (androidDatabase == null)
            {
               Log.e(TAG, "Could not obtain local (Android SQLite) database for creating result cursors");
               return null;
            }
         }
         final PreparedStatement pst = handler.prepare(connection, query, params, paramIndices);
         String tableName = onGetTableName(query);
         boolean isRefresh = true;
         TableInfo ti = tables.get(tableName);
         if (cacheAge > 0)
         {
            if (ti != null)
            {
               long accessed = ti.accessTime;
               long age = System.currentTimeMillis() - accessed, ageParam;
               if (cacheAgeUnit != TimeUnit.MILLISECONDS)
                  ageParam = TimeUnit.MILLISECONDS.convert(cacheAge, cacheAgeUnit);
               else
                  ageParam = cacheAge;
               isRefresh = (ageParam <= age);
            }
         }
         cursor = handler.select(pst, androidDatabase, tableName, isRefresh, errbuf);
         if (cursor != null)
         {
            if (ti == null)
               ti = new TableInfo(query, System.currentTimeMillis(), cursor);
            else
            {
               try
               {
                  if ( (ti.cursor != null) && (! ti.cursor.isClosed()) )
                  {
                     if ( (isRefresh) && (ti.isCursorDisconnected) )
                        ti.notifyListeners(cursor);
                     ti.cursor.close();
                  }
               }
               catch (Exception ee)
               {
                  Log.e(tag(), "", ee);
               }
               ti.cursor = cursor;
               ti.accessTime = System.currentTimeMillis();
            }
            tables.put(tableName, ti);
         }
         return cursor;
      }
      catch (Exception e)
      {
         Log.e(tag(), query, e);
         return null;
      }
   }

   @Override
   public Future<Integer> modify(Connection connection, String query, Map<String, Object> params,
                                 IJdbcModifyCallback callback, Anything token, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------------
   {
      if (query.trim().toUpperCase().startsWith("SELECT"))
      {
         if (errbuf != null)
            errbuf.append("Not a INSERT/DELETE/UPDATE query");
         Log.e(tag(), "Not an INSERT/DELETE/UPDATE query");
         return null;
      }

      ModifyThread t = new ModifyThread(connection, query, params, callback, token);
      if (activeObject == null)
      {
         StringBuilder sb = new StringBuilder("Synchronous call to ").append(this.getClass().getName()).
               append(" with no active object scheduler");
         Log.e(tag(), sb.toString());
         throw new RuntimeException(sb.toString());
      }
      return (Future<Integer>) activeObject.scheduleWithFuture(t);
   }

   @Override
   public int modify(Connection connection, String query, Map<String, Object> params, Map<String, int[]> paramIndices, StringBuilder errbuf)
   //-----------------------------------------------------------------------------------------------------------------
   {
      String s = query.trim().toUpperCase();
      if ( (! s.startsWith("UPDATE")) && (! s .startsWith("INSERT")) && (! s.startsWith("DELETE")) )
      {
         if (errbuf != null)
            errbuf.append("Not a UPDATE/INSERT/DELETE query");
         Log.e(tag(), "Not a UPDATE/INSERT/DELETE query");
         return -1;
      }
      IJdbcRequestHandler handler = defaultsInterceptor.onCreateRequestHandler();
      if (handler == null)
         handler = defaultHandler;
      try
      {
         final PreparedStatement pst = handler.prepare(connection, query, params, paramIndices);
         return handler.modify(pst, errbuf);
      }
      catch (Exception e)
      {
         Log.e(tag(), query, e);
         return -1;
      }
   }

   @Override
   public void destroy(String query)
   //------------------------------
   {
      try
      {
         String tableName = onGetTableName(query);
         if (tables.containsKey(tableName))
         {
            TableInfo ti = tables.remove(tableName);
            if (ti != null)
            {
               try
               {
                  if ( (ti.cursor != null) && (! ti.isCursorDisconnected) )
                  {
                     if (! ti.cursor.isClosed())
                        try { ti.cursor.close(); } catch (Exception _e) {}
                     localDatabase.execSQL("DROP TABLE " + tableName);
                  }
               }
               catch (Exception ee)
               {
                  Log.e(tag(), "", ee);
               }
            }
         }
      }
      catch (Exception e)
      {
         Log.e(tag(), "destroy(" + query + ")", e);
      }
   }

   class ConnectThread implements Callable<Connection>
   //==================================================
   {
      private final Anything token;
      private final IJdbcConnectCallback callback;
      private String host, database, user;
      private int port;
      private String password;
      private long connectionTimeout, readTimeout;
      private TimeUnit connectionTimeoutUnit, readTimeoutUnit;
      private boolean isSSL = false;

      public ConnectThread(String host, int port, String database, String user, String password,
                           long connectionTimeout, TimeUnit connectionTimeoutUnit,
                           long readTimeout, TimeUnit readTimeoutUnit, boolean isSSL,
                           IJdbcConnectCallback callback, Anything token)
      //----------------------------------------------------------------------------------------
      {
         this.host = host;
         this.port = port;
         this.database = database;
         this.user = user;
//         this.password = ENCRYPTER.encryptHex(password);
         this.password = password;
         this.connectionTimeout = connectionTimeout;
         this.connectionTimeoutUnit = connectionTimeoutUnit;
         this.readTimeout = readTimeout;
         this.readTimeoutUnit = readTimeoutUnit;
         this.isSSL = isSSL;
         this.callback = callback;
         this.token = token;
      }

      @Override
      public Connection call() throws Exception
      //--------------------------------------
      {
         StringBuilder errbuf = new StringBuilder();
         Connection conn = null;
         try
         {
            conn = connect(host, port, database, user, password, connectionTimeout, connectionTimeoutUnit,
                           readTimeout, readTimeoutUnit, isSSL, errbuf);
            if (conn != null)
            {
               final Connection connsend = conn;
               activeObject.schedule(new Callable<Void>()
               //========================================
               {  // In case callback method calls get on the Future represented by this thread from
                  // within this thread.
                  @Override public Void call() throws Exception
                  //----------------------------------
                  {
                     callback.onConnected(token, connsend);
                     return null;
                  }
               });
            }
            else
               callback.onError(token, errbuf.toString(), null);
         }
         catch (SQLException e)
         {
            Log.e(tag(), "Connection exception: ", e);
            callback.onError(token, e.getMessage(), e);
         }
         return conn;
      }
   }

   private class QueryThread implements Callable<Cursor>
   //===================================================
   {
      private final Connection connection;
      private final String query;
      private final ICursorQueryCallback callback;
      private final Anything token;
      private final Map<String, Object> params;
      private final long cacheAge;
      private final TimeUnit cacheAgeUnit;
      private SQLiteDatabase androidDatabase;

      public QueryThread(Connection connection, String query, long cacheAge, TimeUnit cacheAgeUnit,
                         Map<String, Object> params, SQLiteDatabase localDatabase,
                         ICursorQueryCallback callback, Anything token)
      //--------------------------------------------------------------------------------------------
      {
         this.connection = connection;
         this.query = query;
         this.callback = callback;
         this.token = token;
         this.params = params;
         this.cacheAge = cacheAge;
         this.cacheAgeUnit = cacheAgeUnit;
         this.androidDatabase = localDatabase;
      }


      @Override
      public Cursor call() throws Exception
      //-----------------------------------
      {
         StringBuilder errbuf = new StringBuilder();
         IJdbcRequestHandler handler = defaultsInterceptor.onCreateRequestHandler();
         if (handler == null)
            handler = defaultHandler;
         try
         {
            final Map<String, int[]> paramIndices;
            if (params == null)
               paramIndices = null;
            else
               paramIndices = new HashMap<>(params.size());
            if (androidDatabase == null)
            {
               androidDatabase = getDatabase(errbuf);
               if (androidDatabase == null)
               {
                  Log.e(TAG, "Could not obtain local (Android SQLite) database for cursor creation");
                  callback.onError(token, -1, "Could not obtain local (Android SQLite) database for cursor creation", null);
                  return null;
               }
            }
            final Cursor cursor;
            String tableName = onGetTableName(query);
            TableInfo ti = tables.get(tableName);
            boolean isRefresh = true;
            if (cacheAge > 0)
            {
               if (ti != null)
               {
                  long accessed = ti.accessTime;
                  long age = System.currentTimeMillis() - accessed, ageParam;
                  if (cacheAgeUnit != TimeUnit.MILLISECONDS)
                     ageParam = TimeUnit.MILLISECONDS.convert(cacheAge, cacheAgeUnit);
                  else
                     ageParam = cacheAge;
                  isRefresh = (ageParam <= age);
               }
            }
            final PreparedStatement pst = handler.prepare(connection, query, params, paramIndices);
            if (pst != null)
               cursor = handler.select(pst, androidDatabase, tableName, isRefresh, errbuf);
            else
               cursor = null;

            if (cursor != null)
            {
               if (ti == null)
                  ti = new TableInfo(query, System.currentTimeMillis(), cursor);
               else
               {
                  try
                  {
                     if ( (ti.cursor != null) && (! ti.cursor.isClosed()) && (! ti.isCursorDisconnected) )
                     {
                        if ( (isRefresh) && (ti.isCursorDisconnected) )
                           ti.notifyListeners(cursor);
                        ti.cursor.close();
                     }
                  }
                  catch (Exception ee)
                  {
                     Log.e(tag(), "", ee);
                  }
                  ti.cursor = cursor;
                  ti.accessTime = System.currentTimeMillis();
               }
               tables.put(tableName, ti);
               activeObject.schedule(new Callable<Void>()
                     //========================================
               {  // In case callback method calls get on the Future represented by this thread from
                  // within this thread.
                  @Override
                  public Void call() throws Exception
                  //----------------------------------
                  {
                     callback.onJdbcQueried(connection, token, cursor, params, paramIndices);
                     return null;
                  }
               });
               return cursor;
            }
            else
            {
               final String err = "SQL Syntax error in query: " + query + " (" + errbuf.toString() + ")";
               callback.onJdbcError(connection, token, err, null);
            }
         }
         catch (Exception e)
         {
            callback.onJdbcError(connection, token, errbuf, null);
            return null;
         }
         return null;
      }
   }

   private class ModifyThread implements Callable<Integer>
   //===================================================
   {
      private final Connection connection;
      private final String query;
      private final IJdbcModifyCallback callback;
      private final Anything token;
      private final Map<String, Object> params;


      public ModifyThread(Connection connection, String query, Map<String, Object> params, IJdbcModifyCallback callback,
                          Anything token)
      //--------------------------------------------------------------------------------------------
      {
         this.connection = connection;
         this.query = query;
         this.callback = callback;
         this.token = token;
         this.params = params;
      }


      @Override
      public Integer call() throws Exception
      //-----------------------------------
      {
         StringBuilder errbuf = new StringBuilder();
         IJdbcRequestHandler handler = defaultsInterceptor.onCreateRequestHandler();
         if (handler == null)
            handler = defaultHandler;
         try
         {
            final Map<String, int[]> paramIndices;
            if (params == null)
               paramIndices = null;
            else
               paramIndices = new HashMap<>(params.size());
            final PreparedStatement pst = handler.prepare(connection, query, params, paramIndices);
            final int result = handler.modify(pst, errbuf);
            if (result >= 0)
            {
               activeObject.schedule(new Callable<Void>()
               //========================================
               {  // In case callback method calls get on the Future represented by this thread from
                  // within this thread.
                  @Override
                  public Void call() throws Exception
                  //----------------------------------
                  {
                     callback.onResponse(token, result, params, paramIndices);
                     return null;
                  }
               });
               return result;
            }
            else
               callback.onError(token, errbuf, null);
         }
         catch (Exception e)
         {
            callback.onError(token, errbuf, null);
            return -1;
         }
         return -1;
      }
   }
}
