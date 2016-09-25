package to.augmented.reality.android.ardb.sourcefacade;

import to.augmented.reality.android.ardb.sourcefacade.jdbc.SpatialFunctions;

public class ColumnContext
//=========================
{
   /**
    * Column name
    */
   String name;
   public String getName() { return name; }

   /**
    * Column alias
    */
   String alias;
   public String getAlias() { return alias; }

   /**
    * The SQL function or SqlFunctions.NONE or null if N/A
    */
   SpatialFunctions function;
   public SpatialFunctions getFunction() { return function; }

   public ColumnContext(String name, String alias, SpatialFunctions function)
   //-------------------------------------------------------------------
   {
      this.name = name;
      this.alias = alias;
      this.function = function;
   }

   public ColumnContext(String name, String alias)
   //---------------------------------------------
   {
      this.name = name;
      this.alias = alias;
      this.function = SpatialFunctions.NONE;
   }
}
