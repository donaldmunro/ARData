package to.augmented.reality.android.ardb.http.sparql;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.HttpRequestorThread;
import to.augmented.reality.android.ardb.http.sparql.parsers.Cell;
import to.augmented.reality.android.ardb.http.sparql.parsers.Parseable;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

class SparQLCursorThread implements Callable<Cursor>
//===================================================
{
   static final private String TAG = SparQLCursorThread.class.getName();

   final HttpRequestorThread requestorThread;
   final PipedInputStream pis;
   final PipedOutputStream pos;
   final Parseable parser;
   final ICursorQueryCallback callback;
   final Anything token;
   final AtomicBoolean mustAbort;
   final StringBuilder errbuf;
   private final SQLiteDatabase database;
   private final String tableName;
   private final Future<Integer> requestFuture;

   public SparQLCursorThread(HttpRequestorThread t, PipedInputStream pis, PipedOutputStream pos, Parseable parser,
                             SQLiteDatabase localDatabase, String tableName, ICursorQueryCallback callback,
                             Anything token, AtomicBoolean mustAbort, Future<Integer> f, StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------
   {
      requestorThread = t;
      this.pis = pis;
      this.pos = pos;
      this.parser = parser;
      this.database = localDatabase;
      this.tableName = tableName;
      this.callback = callback;
      this.token = token;
      this.mustAbort = mustAbort;
      this.requestFuture = f;
      this.errbuf = errbuf;
   }

   @Override
   public Cursor call() throws Exception
   //------------------------------------
   {
      try
      {
         parser.parse(pis);
         String[] columns = parser.projectionNames();
         if (columns == null)
         {
            Log.e(TAG, "Result parser returned no header");
            if (mustAbort != null)
               mustAbort.set(true);
            if (errbuf != null)
               errbuf.append("ERROR: Could not read columns heading");
            if (callback != null)
               callback.onError(token, 400, "ERROR: Could not read columns heading", null);
            return null;
         }
         if (database != null)
            createTable(database, tableName, columns);
         ContentValues databaseRowValues = new ContentValues();
         long seq = 0;
         for (Iterator<Map<String, Cell>> it = parser.iterator(); it.hasNext(); )
         {
            Map<String, Cell> m = it.next();
            if (m == null)
               break;
            boolean isEmpty = true;
            for (Cell ri : m.values())
            {
               if (ri != null)
               {
                  isEmpty = false;
                  break;
               }
            }
            if (! isEmpty)
            {
               if (database != null)
               {
                  Set<Map.Entry<String, Cell>> es = m.entrySet();
                  databaseRowValues.clear();
                  databaseRowValues.put("__ID__", seq++);
                  for (Map.Entry<String, Cell> e : es)
                  {
                     String column = e.getKey();
                     Cell ri = e.getValue();
                     databaseRowValues.put(column, ri.getStringValue());
                  }
                  database.insert(tableName, null, databaseRowValues);
               }
            }
         }
         Cursor cursor = database.query(tableName, null, null, null, null, null, "__ID__");
         if (callback != null)
         {
            int retcode = requestFuture.get();
            if ((retcode / 100) == 2)
               callback.onQueried(token, cursor, retcode);
            else
               callback.onError(token, retcode, errbuf.toString(), null);
         }
         return cursor;
      }
      catch (Exception e)
      {
         if (mustAbort != null)
            mustAbort.set(true);
         try { Thread.sleep(200); } catch (Exception _e) {}
         if (! requestFuture.isDone())
            requestFuture.cancel(true);
         return null;
      }
      finally
      {
         try { pis.close(); } catch (Exception _e) {}
         try { pos.close(); } catch (Exception _e) {}
      }
   }

   private void createTable(SQLiteDatabase database, String tableName, String[] columns) throws SQLException
   //--------------------------------------------------------------------------------------------------------
   {
      StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS ").append(tableName);
      database.execSQL(sql.toString());
      sql.setLength(0);
      sql.append("CREATE TABLE ").append(tableName).append(" ( __ID__ INTEGER PRIMARY KEY,");
      for (String column : columns)
      {
         column = column.replace('?', '_');
         sql.append(column).append(" TEXT,");
      }
      sql.deleteCharAt(sql.length() - 1);
      sql.append(')');
      database.execSQL(sql.toString());
   }
}
