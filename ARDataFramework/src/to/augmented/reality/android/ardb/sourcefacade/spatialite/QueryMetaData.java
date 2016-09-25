package to.augmented.reality.android.ardb.sourcefacade.spatialite;

import android.util.Log;
import jsqlite.Stmt;
import jsqlite.Constants;
import to.augmented.reality.android.ardb.spi.IQueryMetaData;

import java.sql.Types;

public class QueryMetaData implements IQueryMetaData
//==================================================
{
   final Stmt statement;

   public QueryMetaData(Stmt stmt) { statement = stmt; }

   @Override public int getColumnCount() { try { return statement.column_count(); } catch (Exception e) { return Integer.MIN_VALUE; } }

   @Override
   public String getColumnName(int col)
   //----------------------------------
   {
      String name;
      try { name = statement.column_origin_name(col); } catch (Exception e) { name = null; }
      if ( (name == null) || (name.trim().isEmpty()) )
         try { name = statement.column_name(col); } catch (Exception e) { name = null; }
      return name;
   }

   @Override
   public String getColumnAlias(int col)
   //-----------------------------------
   {
      String name;
      try { name = statement.column_name(col); } catch (Exception e) { name = null; }
      if ( (name == null) || (name.trim().isEmpty()) )
         try { name = statement.column_origin_name(col); } catch (Exception e) { name = null; }
      return name;
   }

   @Override
   public int getColumnType(int col)
   //------------------------------
   {
      int type;
      try
      {
         type = statement.column_type(col);
      }
      catch (Exception e)
      {
         Log.e("QueryMetaData", "getColumnType", e);
         type = Integer.MIN_VALUE;
         String typ;
         try { typ = statement.column_decltype(col); } catch (Exception ee) { typ = null; }
         if (typ != null)
         {
            switch (typ)
            {
               case "TEXT":
               case "VARIANT":   type = Constants.SQLITE_TEXT; break;
               case "INTEGER":   type = Constants.SQLITE_INTEGER; break;
               case "REAL":      type = Constants.SQLITE_FLOAT; break;
               case "BLOB":      type = Constants.SQLITE_BLOB; break;
               case "NULL":      type = Constants.SQLITE_NULL; break;
            }
         }
      }
      switch (type)
      {
         case Constants.SQLITE_TEXT:      return Types.VARCHAR;
         case Constants.SQLITE_INTEGER:   return Types.BIGINT;
         case Constants.SQLITE_BLOB:      return Types.BLOB;
         case Constants.SQLITE_FLOAT:     return Types.DOUBLE;
         case Constants.SQLITE_NULL:      return Types.NULL;
      }
      return Integer.MIN_VALUE;
   }
}
