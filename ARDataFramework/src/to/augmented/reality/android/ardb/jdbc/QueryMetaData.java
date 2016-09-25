package to.augmented.reality.android.ardb.jdbc;

import to.augmented.reality.android.ardb.spi.IQueryMetaData;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class QueryMetaData implements IQueryMetaData
//===================================================
{
   final ResultSetMetaData metaData;

   public QueryMetaData(ResultSet rs) throws SQLException { metaData = rs.getMetaData(); }

   public QueryMetaData(ResultSetMetaData metaData) {  this.metaData = metaData; }

   @Override public int getColumnCount() { try { return metaData.getColumnCount(); } catch (SQLException e) { return Integer.MIN_VALUE; } }

   @Override public String getColumnName(int col) { try { return metaData.getColumnName(col); } catch (SQLException e) { return null; } }

   @Override
   public String getColumnAlias(int col)
   //-----------------------------------
   {
      String name = getColumnName(col);
      if (name != null)
         return name.replace('?', '_');
      else
         return null;
   }

   @Override
   public int getColumnType(int col)
   {
      try { return metaData.getColumnType(col); } catch (Exception e) { return Integer.MIN_VALUE; }
   }
}
