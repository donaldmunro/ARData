package to.augmented.reality.android.ardb.jdbc;

public class QueryParameter
//=========================
{
   int type;
   Object value;

   /**
    * @param typ The SQL database column type of the parameter as specified in @link java.sql.Types
    * @param v   The value of the parameter
    */
   public QueryParameter(int typ, Object v)
   {
      this.type = typ;
      this.value = v;
   }
}
