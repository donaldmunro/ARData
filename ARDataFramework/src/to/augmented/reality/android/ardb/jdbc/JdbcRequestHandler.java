package to.augmented.reality.android.ardb.jdbc;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import to.augmented.reality.android.ardb.util.DatabaseUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JdbcRequestHandler implements IJdbcRequestHandler
//============================================================
{
   static final private String TAG = JdbcRequestHandler.class.getSimpleName();

   @Override
   public Connection connect(String driver, String url, StringBuilder errbuf) throws SQLException, ClassNotFoundException
   //--------------------------------------------------------------------------------------------------------------------
   {
      Class.forName(driver);
      return DriverManager.getConnection(url);
   }

   @Override
   public PreparedStatement prepare(Connection connection, String sql, Map<String, Object> params,
                                    Map<String, int[]> paramIndices) throws SQLException
   //-----------------------------------------------------------------------------------------------------------------
   {
      if ( (connection == null) || (connection.isClosed()) )
      {
         Log.e(TAG, "prepare: Closed or null connection");
         throw new SQLException("Closed or null connection");
      }
      if (params == null)
         synchronized (connection) { return connection.prepareStatement(sql); }

      if (paramIndices == null)
         paramIndices = new HashMap<>();
      int[] count = new int[1];
      String preparedSql = parseParameters(sql, paramIndices, count);

      PreparedStatement pst = null;
      synchronized (connection) { pst = connection.prepareStatement(preparedSql); }
      StringBuilder errbuf = new StringBuilder();
      if (! setParameters(pst, params, paramIndices, errbuf))
      {
         String s = String.format(Locale.getDefault(), "Parse error: %s In SQL %s. Generated SQL: %s",
                                  errbuf, sql, preparedSql);
         Log.e(TAG, s);
         throw new SQLException(s);
      }

      return pst;
   }

   public boolean setParameters(PreparedStatement pst, Map<String, Object> params, Map<String, int[]> paramIndices,
                                StringBuilder errbuf) throws SQLException
   //-----------------------------------------------------------------------------------------------------------
   {
      pst.clearParameters();
      for (Map.Entry<String, Object> entry : params.entrySet())
      {
         String name = entry.getKey();
         if (name.startsWith(":"))
            name = name.substring(1);
         int[] indices = paramIndices.get(name);
         if (indices == null)
         {
            if (errbuf != null)
               errbuf.append("Parameter ").append(name).append(" not found.");
            return false;
         }
         for (int i : indices)
         {
            Object v = entry.getValue();
            if (v instanceof QueryParameter)
            {
               QueryParameter param = (QueryParameter) v;
               pst.setObject(i, param.value, param.type);
            }
            else
               pst.setObject(i, v);
         }
      }
      return true;
   }

   @Override
   public Cursor select(PreparedStatement query, SQLiteDatabase localDatabase, String localTableName, boolean isReplace,
                        StringBuilder errbuf)
   //------------------------------------------------------------------------------------------------------------------
   {
      StringBuilder create = new StringBuilder();
      StringBuilder insert = new StringBuilder();
      StringBuilder select = new StringBuilder();
      ResultSet rs = null;
      boolean isCreated = false, isInserted = false, isSelected = false;
      try
      {
         rs = query.executeQuery();
         ResultSetMetaData metaData = rs.getMetaData();
         if (DatabaseUtils.createLocalSQL(new QueryMetaData(metaData), localTableName, true, false, create, insert, select))
            return null;
         synchronized (localDatabase)
         {
            if (DatabaseUtils.isAndroidTableExists(localDatabase, localTableName))
            {
               if (isReplace)
                  localDatabase.execSQL("DROP TABLE " + localTableName);
               else
               {
                  Cursor cursor = null;
                  try { cursor = localDatabase.rawQuery(select.toString(), null); } catch (Exception _e) { cursor = null; }
                  if (cursor != null)
                     return cursor;
                  else
                     try { localDatabase.execSQL("DROP TABLE " + localTableName); } catch (Exception _e) { Log.e(TAG, "", _e); }
               }
            }
            localDatabase.execSQL(create.toString());
         }
         isCreated = true;
         ContentValues v = new ContentValues();
         int seq = 0;
         String sql = insert.toString();
         ContentValues values = new ContentValues();
         localDatabase.beginTransaction();
         try
         {
            Map<String, Integer> names = new HashMap<>();
            while (rs.next())
            {
               values.clear();
               values.put("_seq_", seq++);
               names.clear();
               for (int i = 1; i <= metaData.getColumnCount(); i++)
               {
                  //TODO: Optimize - reuse names
                  String name = metaData.getColumnName(i);
                  name = name.replace('?', '_');
                  Integer count = names.get(name);
                  if (count != null)
                  {
                     count++;
                     names.put(name, count);
                     name = name + count;
                  }
                  else
                     names.put(name, 0);

                  DatabaseUtils.setContentValue(values, rs, name, i, metaData.getColumnType(i));
               }
               localDatabase.insert(localTableName, null, values);
            }
            localDatabase.setTransactionSuccessful();
         }
         finally
         {
            localDatabase.endTransaction();
         }
         isInserted = true;

         return localDatabase.rawQuery(select.toString(), null);
      }
      catch (Exception e)
      {
         if (isCreated)
            try { localDatabase.execSQL("DROP TABLE " + localTableName); } catch (Exception _e) {}
         if (errbuf == null)
            errbuf = new StringBuilder();
         if (! isCreated)
            errbuf.append(create.toString());
         else if (! isInserted)
            errbuf.append(insert.toString());
         else if (! isSelected)
            errbuf.append(select.toString());
         Log.e(TAG, errbuf.toString(), e);
         errbuf.append(": ").append(e.getMessage());
         return null;
      }
      finally
      {
         if (rs != null)
            try { rs.close(); } catch (Exception _e) {}
      }
   }

   @Override
   public int modify(PreparedStatement pst, StringBuilder errbuf)
   //-------------------------------------------------------------
   {
      try
      {
         return pst.executeUpdate();
      }
      catch (SQLException e)
      {
         if (errbuf == null)
            errbuf = new StringBuilder();
         errbuf.append("Modify exception ").append(e.getMessage()).append(" in ").append(pst);
         Log.e(TAG, errbuf.toString(), e);
         return -1;
      }
   }


   static public String parseParameters(String sql, Map<String, int[]> paramMap, int[] count)
   //---------------------------------------------------------------
   {
      Map<String, List<Integer>> paramMapAux = new HashMap<String, List<Integer>>();
      int length = sql.length();
      StringBuffer parsedQuery = new StringBuffer(length);
      boolean inSingleQuote = false;
      boolean inDoubleQuote = false;
      int index = 1, n = 0;

      for (int i = 0; i < length; i++)
      {
         char c = sql.charAt(i);
         if (inSingleQuote)
         {
            if (c == '\'')
               inSingleQuote = false;
         } else if (inDoubleQuote)
         {
            if (c == '"')
               inDoubleQuote = false;
         } else
         {
            if (c == '\'')
               inSingleQuote = true;
            else if (c == '"')
               inDoubleQuote = true;
            else if (c == ':' && i + 1 < length &&
                  Character.isJavaIdentifierStart(sql.charAt(i + 1)))
            {
               int j = i + 2;
               while (j < length && Character.isJavaIdentifierPart(sql.charAt(j)))
                  j++;
               String name = sql.substring(i + 1, j);
               c = '?'; // replace the parameter with a question mark
               n++;
               i += name.length(); // skip past the end if the parameter

               List<Integer> indexList = paramMapAux.get(name);
               if (indexList == null)
               {
                  indexList = new LinkedList<>();
                  paramMapAux.put(name, indexList);
               }
               indexList.add(index);

               index++;
            }
         }
         parsedQuery.append(c);
      }

      // replace the lists of Integer objects with arrays of ints
      for (Map.Entry<String, List<Integer>> entry : paramMapAux.entrySet())
      {
         List<Integer> list = entry.getValue();
         int[] indexes = new int[list.size()];
         int i = 0;
         for (Integer x : list)
            indexes[i++] = x;
         paramMap.put(entry.getKey(), indexes);
      }

      if ( (count != null) && (count.length > 0) )
         count[0] = n;
      return parsedQuery.toString();
   }
}
