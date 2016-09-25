package to.augmented.reality.android.ardb.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import to.augmented.reality.android.ardb.spi.IQueryMetaData;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class DatabaseUtils
//========================
{
   static public String sqlLiteType(int type)
   //----------------------------------------
   {
      switch (type)
      {
         case Types.BIGINT:
         case Types.INTEGER:
         case Types.SMALLINT:
         case Types.TINYINT:
            return "INTEGER";

         case Types.VARCHAR:
         case Types.CHAR:
         case Types.LONGNVARCHAR:
         case Types.LONGVARCHAR:
         case Types.NCHAR:
         case Types.NVARCHAR:
            return "TEXT";

         case Types.DOUBLE:
         case Types.FLOAT:
         case Types.REAL:
            return "REAL";

         case Types.DECIMAL:
         case Types.NUMERIC:
         case Types.BOOLEAN:
         case Types.DATE:
         case Types.TIME:
         case Types.TIMESTAMP:
            return "NUMERIC";

         case Types.BLOB:
         case Types.CLOB:
            return "BLOB";

         default: return "TEXT";
      }
   }

   static public SimpleDateFormat SQLITE_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
   static public SimpleDateFormat SQLITE_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
   static public SimpleDateFormat SQLITE_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

   static public String toString(ResultSet rs, int col, int type, String nul) throws SQLException
   //------------------------------------------------------------------------------------------------------
   {
      return toString(rs, col, type, SQLITE_DATA_FORMAT, SQLITE_TIME_FORMAT, SQLITE_TIMESTAMP_FORMAT, nul);
   }

   static public String toString(ResultSet rs, int col, int type, SimpleDateFormat dateFormat,
                                 SimpleDateFormat timeFormat, SimpleDateFormat timestampFormat, String nul)
         throws SQLException
   //------------------------------------------------------------------------------------------------------
   {
      Object o;
      switch (type)
      {
         case Types.BIGINT:
            try
            {
               long l = rs.getLong(col);
               if (rs.wasNull())
                  return nul;
               else return Long.toString(l);
            }
            catch (Exception e)
            {
               o = rs.getBigDecimal(col);
               if ( (o == null) || (rs.wasNull()) )
                  return nul;
            }
            return o.toString();

         case Types.INTEGER:
            int i = rs.getInt(col);
            if (rs.wasNull())
               return nul;
            else
               return Integer.toString(i);

         case Types.SMALLINT:
            short sh = rs.getShort(col);
            if (rs.wasNull())
               return nul;
            else
               return Short.toString(sh);

         case Types.TINYINT:
            byte b = rs.getByte(col);
            if (rs.wasNull())
               return nul;
            else
               return Byte.toString(b);

         case Types.VARCHAR:
         case Types.CHAR:
         case Types.LONGNVARCHAR:
         case Types.LONGVARCHAR:
         case Types.NCHAR:
         case Types.NVARCHAR:
            String s = rs.getString(col);
            if ( (s == null) || (rs.wasNull()) )
               return nul;
            return s;

         case Types.DOUBLE:
         case Types.FLOAT:
            double d = rs.getDouble(col);
            if (rs.wasNull())
               return nul;
            else
               return Double.toString(d);

         case Types.REAL:
            float f = rs.getFloat(col);
            if (rs.wasNull())
               return nul;
            else
               return Float.toString(f);

         case Types.DECIMAL:
         case Types.NUMERIC:
            BigDecimal bd = rs.getBigDecimal(col);
            if ( (bd == null) || (rs.wasNull()) )
               return nul;
            return bd.toString();

         case Types.BOOLEAN:
            Boolean B = Boolean.valueOf(rs.getBoolean(col));
            return B.toString();

         case Types.DATE:
            Date dt = rs.getDate(col);
            if ( (dt == null) || (rs.wasNull()) )
               return nul;
            return dateFormat.format(dt);

         case Types.TIME:
            Time tm = rs.getTime(col);
            if ( (tm == null) || (rs.wasNull()) )
               return nul;
            return timeFormat.format(tm);

         case Types.TIMESTAMP:
            Timestamp ts = rs.getTimestamp(col);
            if ( (ts == null) || (rs.wasNull()) )
               return nul;
            return timestampFormat.format(ts);

         case Types.BLOB:
            Blob blob = rs.getBlob(col);
            if ( (blob == null) || (rs.wasNull()) )
               return nul;
            return blob.toString();

         case Types.CLOB:
            Clob clob = rs.getClob(col);
            if ( (clob == null) || (rs.wasNull()) )
               return nul;
            return clob.toString();

         default:
            o = rs.getObject(col);
            if ( (o == null) || (rs.wasNull()) )
               return nul;
            return o.toString();
      }
   }

   static public void setContentValue(ContentValues cv, ResultSet rs, String name, int col, int type) throws SQLException
   //------------------------------------------------------------------------------------------------------
   {
      setContentValue(cv, rs, name, col, type, SQLITE_DATA_FORMAT, SQLITE_TIME_FORMAT, SQLITE_TIMESTAMP_FORMAT);

   }

   static public void setContentValue(ContentValues cv, ResultSet rs, String name, int col, int type,
                                      SimpleDateFormat dateFormat, SimpleDateFormat timeFormat,
                                      SimpleDateFormat timestampFormat) throws SQLException
   //-------------------------------------------------------------------------------------------------
   {
      switch (type)
      {
         case Types.BIGINT:
            try
            {
               long l = rs.getLong(col);
               if (rs.wasNull())
                  cv.putNull(name);
               else
                  cv.put(name, l);
            }
            catch (Exception e)
            {
               BigDecimal bd = rs.getBigDecimal(col);
               if ( (bd == null) || (rs.wasNull()) )
                  cv.putNull(name);
               else
                  cv.put(name, bd.longValue());
            }
            break;

         case Types.INTEGER:
            int i = rs.getInt(col);
            if (rs.wasNull())
               cv.putNull(name);
            else
               cv.put(name, i);
            break;

         case Types.SMALLINT:
            short sh = rs.getShort(col);
            if (rs.wasNull())
               cv.putNull(name);
            else
               cv.put(name, sh);

         case Types.TINYINT:
            byte b = rs.getByte(col);
            if (rs.wasNull())
               cv.putNull(name);
            else
               cv.put(name, b);

         case Types.VARCHAR:
         case Types.CHAR:
         case Types.LONGNVARCHAR:
         case Types.LONGVARCHAR:
         case Types.NCHAR:
         case Types.NVARCHAR:
            String s = rs.getString(col);
            if ( (s == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, s);
            break;

         case Types.DOUBLE:
         case Types.FLOAT:
            double d = rs.getDouble(col);
            if (rs.wasNull())
               cv.putNull(name);
            else
               cv.put(name, d);
            break;

         case Types.REAL:
            float f = rs.getFloat(col);
            if (rs.wasNull())
               cv.putNull(name);
            else
               cv.put(name, f);
            break;

         case Types.DECIMAL:
         case Types.NUMERIC:
            BigDecimal bd = rs.getBigDecimal(col);
            if ( (bd == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, bd.longValue());
            break;

         case Types.BOOLEAN:
            Boolean B = Boolean.valueOf(rs.getBoolean(col));
            cv.put(name, B);
            break;

         case Types.DATE:
            Date dt = rs.getDate(col);
            if ( (dt == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, dateFormat.format(dt));
            break;

         case Types.TIME:
            Time tm = rs.getTime(col);
            if ( (tm == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, timeFormat.format(tm));
            break;

         case Types.TIMESTAMP:
            Timestamp ts = rs.getTimestamp(col);
            if ( (ts == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, timestampFormat.format(ts));
            break;

         case Types.BLOB:
            Blob blob = rs.getBlob(col);
            if ( (blob == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, blob.toString().getBytes());
            break;

         case Types.CLOB:
            Clob clob = rs.getClob(col);
            if ( (clob == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, clob.toString().getBytes());
            break;

         default:
            Object o = rs.getObject(col);
            if ( (o == null) || (rs.wasNull()) )
               cv.putNull(name);
            else
               cv.put(name, o.toString());
            break;
      }
   }

   static public boolean createLocalSQL(IQueryMetaData metaData, String localTableName, boolean isTemp, boolean isAutoSeq,
                                     StringBuilder create, StringBuilder insert, StringBuilder select)
   //------------------------------------------------------------------------------------------------------------------
   {
      if (create != null)
         create.setLength(0);
      else
         create = new StringBuilder(); // Local use only to avoid checking nulls
      if (insert != null)
         insert.setLength(0);
      else
         insert = new StringBuilder(); // Local use only to avoid checking nulls
      if (select != null)
         select.setLength(0);
      else
         select = new StringBuilder(); // Local use only to avoid checking nulls
      create.append("CREATE ");
      if (isTemp)
         create.append("TEMP ");
      create.append("TABLE ").append(localTableName).append(" (_seq_ INTEGER PRIMARY KEY, ");
      insert.append("INSERT INTO ").append(localTableName).append(" (");
      if (! isAutoSeq)
         insert.append("_seq_, ");
      select.append("SELECT ");
      int ccols = metaData.getColumnCount();
      if (ccols < 0)
      {
         create.setLength(0); insert.setLength(0); select.setLength(0);
         return false;
      }
      Map<String, Integer> names = new HashMap<>();
      for (int i=0; i<ccols; i++)
      {
         String name = metaData.getColumnAlias(i);
         if (name == null)
         {
            create.setLength(0); insert.setLength(0); select.setLength(0);
            return false;
         }
         Integer count = names.get(name);
         if (count != null)
         {
            count++;
            names.put(name, count);
            name = name + count;
         }
         else
            names.put(name, 0);

         int typ = metaData.getColumnType(i);
         if (typ == Integer.MIN_VALUE)
         {
            create.setLength(0); insert.setLength(0); select.setLength(0);
            return false;
         }
         create.append(name).append(' ').append(DatabaseUtils.sqlLiteType(typ)).append(',');
         insert.append(name).append(',');
         select.append(name).append(',');
      }
      create.deleteCharAt(create.length() - 1);
      insert.deleteCharAt(insert.length() - 1);
      select.deleteCharAt(select.length() - 1);
      create.append(")");
      insert.append(") VALUES (");
      for (int i=0; i<ccols; i++)
         insert.append("?,");
      insert.deleteCharAt(insert.length() - 1);
      insert.append(')');
      select.append(" FROM ").append(localTableName).append(" ORDER BY _seq_");
      return true;
   }

   static public boolean isAndroidTableExists(SQLiteDatabase database, String localTableName)
   //----------------------------------------------------------------------------------------
   {
      Cursor C = null;
      try
      {
         C = database.query(localTableName, null, "1=2", null, null, null, null);
         return true;
      }
      catch (Exception ee)
      {
         return false;
      }
      finally
      {
         if (C != null)
            try { C.close(); } catch (Exception _e) { }
      }
   }

}
