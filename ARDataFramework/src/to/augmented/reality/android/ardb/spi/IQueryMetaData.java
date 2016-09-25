package to.augmented.reality.android.ardb.spi;

public interface IQueryMetaData
//=============================
{
   /**
    * @return The number of columns in the query or Integer.MIN_VALUE if an error occurs.
    */
   int getColumnCount ();

   /**
    * @param column The column number. Can be 1 based (JDBC) or zero based depending on the implementation.
    * @return The query column name or null if an error occurred. For some implementations if there is a query alias
    * then the query alias is returned (eg JDBC), while for others if a valid original table name exists (ie not
    * an expression) then the original table column name is returned.
    */
   String getColumnName(int column);

   /**
    * @param column The column number. Can be 1 based (JDBC) or zero based depending on the implementation.
    * @return The query column alias or null if an error occurred. For some implementations if there is a query alias
    * then the query alias is returned from both getColumnAlias and getColumnName (eg JDBC). For others if a
    * alias exists it is returned else the result of getColumnName is returned.
    */
   String getColumnAlias(int column);

   /**
    * @param column The column number. Can be 1 based (JDBC) or zero based depending on the implementation.
    * @return The column type as a {@link java.sql.Types} or Integer.MIN_VALUE if an error occurs.
    */
   int getColumnType(int column);
}
