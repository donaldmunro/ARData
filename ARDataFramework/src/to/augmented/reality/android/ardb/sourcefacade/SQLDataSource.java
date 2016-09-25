package to.augmented.reality.android.ardb.sourcefacade;


import to.augmented.reality.android.ardb.spi.ISpatialFunctions;
import to.augmented.reality.android.ardb.util.Util;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.sourcefacade.jdbc.JoinContext;
import to.augmented.reality.android.ardb.sourcefacade.jdbc.JoinType;
import to.augmented.reality.android.ardb.sourcefacade.jdbc.SpatialFunctions;
import to.augmented.reality.android.ardb.sourcefacade.jdbc.WhereParameterProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

abstract public class SQLDataSource
//==================================
{
   protected ColumnContext[] projectionColumns = null;
   protected DataType[] projectionTypes = null;
   protected JoinContext[] joins = null;
   protected String fromTable = null;
   protected String tableAlias;
   protected String spatialWhereColumnName = null;
   protected String where = null;
   protected Map<String, WhereParameterProperties> whereParameters = null;
   protected String[] orderByColumns = null;
   protected String[] orderBy;
   protected Map<String, Object> parameters;

   abstract protected String onGetWhere(String where);
   abstract protected Future<?> executeQuery(final String query, final Map<String, Object> parameters,
                                             ISpatialQueryResult.CALLBACK_TYPE callbackType, final Anything token,
                                             final ISpatialQueryResult callback, StringBuilder errbuf);

   /**
    * Set additional named parameters to be used in PreparedStatement. The name of the parameter is specified in
    * the WHERE clause with a leading colon eg :location or :latitude
    * @param parameters The parameters as name, value pairs
    */
   public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

   public void configureSelection(ColumnContext[] projection, String table, String tableAlias, JoinContext[] joins,
                                  String where, WhereParameterProperties[] whereParams, String[] orderBy)
   //------------------------------------------------------------------------------------------------------------------
   {
      setSelection(projection, table, tableAlias, joins, where, whereParams, orderBy);
   }

   public void configureSelection(ColumnContext[] projection, String table, String tableAlias,
                                  JoinContext[] joins, String where, WhereParameterProperties[] whereParams,
                                  String[] orderBy, String spatialSearchColumn)
   //------------------------------------------------------------------------------------------------------------------
   {
      setSelection(projection, table, tableAlias, joins, where, whereParams, orderBy);
      this.spatialWhereColumnName = spatialSearchColumn;
   }

   public void configureSelection(ColumnContext[] projection, String table, String tableAlias,
                                  JoinContext[] joins, String where, WhereParameterProperties[] whereParams,
                                  String[] orderBy, String spatialSearchColumn, DataType[] projectionTypes)
   //-----------------------------------------------------------------------------------------------------------------
   {
      setSelection(projection, table, tableAlias, joins, where, whereParams, orderBy);
      this.spatialWhereColumnName = spatialSearchColumn;
      this.projectionTypes = projectionTypes;
   }

   private void setSelection(ColumnContext[] projection, String table, String tableAlias, JoinContext[] joins,
                             String where, WhereParameterProperties[] whereParams, String[] orderBy)
   //------------------------------------------------------------------------------------------------------------------
   {
      this.projectionColumns = projection;
      this.fromTable = table;
      this.tableAlias = tableAlias;
      this.joins = joins;
      this.where = where;
      this.whereParameters = new HashMap<>(whereParams.length);
      for (WhereParameterProperties prop : whereParams)
         this.whereParameters.put(prop.name, prop);
      this.orderBy = orderBy;
   }

   protected ColumnContext[] onGetProjectionColumns() { return projectionColumns; }

   protected String[] onGetOrderByColumns() { return orderByColumns; }

   protected String onGetFrom() { return fromTable; }

   protected String onGetFromAlias() { return tableAlias; }

   protected String onGetSpatialWhereColumn() { return spatialWhereColumnName; }

   protected void setWhere(String where) { this.where = where; }

   public String getWhere() { return where; }

   protected String onGetJoin()
   //--------------------------
   {
      StringBuilder joinBuf = new StringBuilder();
      for (JoinContext jc : joins)
      {
         switch (jc.getJoinType())
         {
            case INNER:
               joinBuf.append(" INNER JOIN "); break;
            case LEFT_OUTER:
               joinBuf.append(" LEFT OUTER JOIN "); break;
            case RIGHT_OUTER:
               joinBuf.append(" RIGHT OUTER JOIN "); break;
            case FULL_OUTER:
               joinBuf.append(" FULL OUTER JOIN "); break;
            case CARTESIAN:
               joinBuf.append(" CROSS JOIN "); break;
         }
         joinBuf.append(jc.getTable());
         if ( (jc.getAlias() != null) && (! jc.getAlias().trim().isEmpty()) )
            joinBuf.append(' ').append(jc.getAlias());
         if (jc.getJoinType() != JoinType.CARTESIAN)
            joinBuf.append(' ').append(" ON ").append(jc.getJoinCondition());
      }
      return joinBuf.toString();
   }

   protected String select(String where, ISpatialFunctions spatialFunctions)
   //------------------------------------------------------------------------
   {
      StringBuilder sql = new StringBuilder("SELECT ");
      ColumnContext[] columns = onGetProjectionColumns();
      for (ColumnContext column : columns)
      {
         SpatialFunctions function = column.getFunction();
         if ( (function != null) && (function != SpatialFunctions.NONE) )
         {
            switch (function)
            {
               case SPATIAL_TO_TEXT: sql.append(spatialFunctions.spatialToString(column.getName())); break;
               case TEXT_TO_SPATIAL: sql.append(spatialFunctions.stringToSpatial(column.getName())); break;
            }
         }
         else
            sql.append(column.getName());
         String alias = column.getAlias();
         if ( (alias != null) && (! alias.isEmpty()) )
            sql.append(" AS ").append(alias);
         sql.append(",");
      }
      sql.deleteCharAt(sql.length() - 1).append(" FROM ");
      String table = onGetFrom();
      String tableAlias = onGetFromAlias();
      sql.append(table);
      if ( (tableAlias != null) && (! tableAlias.trim().isEmpty()) )
         sql.append(' ').append(tableAlias);
      sql.append(' ');
      if ( (joins != null) && (joins.length > 0) )
         sql.append(onGetJoin());
      String fullWhere = onGetWhere(where);
      if (! Util.startWithWordPattern("WHERE").matcher(fullWhere).matches())
         sql.append(" WHERE ");
      else
         sql.append(' ');
      sql.append(fullWhere);
      String[] order = onGetOrderByColumns();
      if ( (order != null) && (order.length > 0) )
      {
         sql.append(" ORDER BY ");
         for (String column : order)
            sql.append(column).append(",");
         sql.deleteCharAt(sql.length() - 1);
      };
      return sql.toString();
   }
}
