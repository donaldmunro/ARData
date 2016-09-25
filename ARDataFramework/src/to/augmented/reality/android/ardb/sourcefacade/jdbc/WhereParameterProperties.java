package to.augmented.reality.android.ardb.sourcefacade.jdbc;

public class WhereParameterProperties
//===================================
{
   public String name, column, table;
   public int sqlType;
   public boolean isTextToSpatial;

   public WhereParameterProperties(String name, String table, String column, int sqlType, boolean isTextToSpatial)
   //-------------------------------------------------------------------------------------------------------------
   {
      this.name = name;
      this.table = table;
      this.column = column;
      this.sqlType = sqlType;
      this.isTextToSpatial = isTextToSpatial;
   }
}
