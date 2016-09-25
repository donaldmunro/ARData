package to.augmented.reality.android.ardb.sourcefacade.jdbc;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.jdbc.DatabaseType;
import to.augmented.reality.android.ardb.jdbc.QueryParameter;
import to.augmented.reality.android.ardb.sourcefacade.ColumnContext;
import to.augmented.reality.android.ardb.sourcefacade.IAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.AnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Host;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Password;
import to.augmented.reality.android.ardb.sourcefacade.annotations.BoundingBox;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Circle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.From;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.JdbcSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.Join;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.Joins;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.OrderBy;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.SelectColumn;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.Where;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.WhereParameter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JdbcAnnotationProcessor extends AnnotationProcessor implements IAnnotationProcessor, Cloneable
//=========================================================================================================
{
   static final private String TAG = JdbcAnnotationProcessor.class.getName();

   @Override protected String tag() { return TAG; }

   @Override public Object clone() throws CloneNotSupportedException { return super.clone(); }

   ISpatialSource.SPATIAL_OPERATION operation;
   @Override public ISpatialSource.SPATIAL_OPERATION operation() { return operation; }

   double radius =-1;
   @Override public double circleRadius() { return radius; }

   double width =-1;
   @Override public double bboxWidth() { return width; }

   double height =-1;
   @Override public double bboxHeight() { return height; }

   @Override public Object[] userDefOpParameters() { return null; }

   public JdbcAnnotationProcessor() {  }

   public JdbcAnnotationProcessor(Context context) { super(context); }

   @Override
   public ISpatialSource createAnnotated(String name, Context context, ActiveObject activeObject,
                                         Object instance)
   //--------------------------------------------------------------------------------------------------------
   {
      final Class<?> C = instance.getClass();
      String annotatedName = " (" + C.getName() + ")";
      JdbcSource jdbcSource = C.getAnnotation(JdbcSource.class);
      From fromAnno = C.getAnnotation(From.class);
      if ( (fromAnno == null) || (fromAnno.value() == null) || (fromAnno.value().trim().isEmpty()) )
         throw new RuntimeException("Valid @From annotation must be specified for class " + C.getName());
      String sourceName = jdbcSource.name();
      DatabaseType type = jdbcSource.type();
      JdbcDataSource dataSource = new JdbcDataSource(sourceName, context, activeObject, type);

      String host = jdbcSource.host();
      if ( (host == null) || (host.trim().isEmpty()) )
      {
         host = extractMethodOrField(C, instance, Host.class);
         if ( (host == null) || (host.trim().isEmpty()) )
            host = "localhost";
      }
      int port = jdbcSource.port();
      String database = jdbcSource.database();
      String user = jdbcSource.user();
      String password = jdbcSource.password();
      if (password.length() == 0)
      {
         password = extractMethodOrField(C, instance, Password.class);
         if (password.length() == 0)
            Log.w(TAG, "Datasource " + name + " " + annotatedName + ": JDBC password empty");
      }
      long connTimeout = TimeUnit.MILLISECONDS.convert(jdbcSource.connectionTimeout(),
                                                       jdbcSource.connectionTimeoutUnit());
      long readTimeout = TimeUnit.MILLISECONDS.convert(jdbcSource.readTimeout(), jdbcSource.readTimeoutUnit());
      dataSource.configureConnection(host, port, user, password, database, connTimeout, readTimeout);

      final String table = fromAnno.value();
      final String alias = (fromAnno.alias().trim().isEmpty()) ? table : fromAnno.alias();
      Set<String> tables = new HashSet<>();
      tables.add(table);
      Set<String> aliases = new HashSet<>();
      aliases.add(alias);
      Join[] joinAnnos = null;
      Joins joinsAnno = C.getAnnotation(Joins.class);
      if (joinsAnno != null)
         joinAnnos = joinsAnno.value();
      else
      {
         Join joinAnno = C.getAnnotation(Join.class);
         if (joinAnno != null)
         {
            joinAnnos = new Join[1];
            joinAnnos[0] = joinAnno;
         }
      }
      if (joinAnnos != null)
      {
         for (Join join : joinAnnos)
         {
            String joinTable = join.table();
            tables.add(joinTable);
            String joinAlias = join.alias();
            if (joinAlias.trim().isEmpty())
               joinAlias = joinTable;
            aliases.add(joinAlias);
         }
      }
      JoinContext[] joins = null;
      if (joinAnnos != null)
      {
         joins = new JoinContext[joinAnnos.length];
         int i = 0;
         for (Join join : joinAnnos)
            joins[i++] = new JoinContext(join.table(), join.alias(), join.joinType(), join.joinCondition());
      }

      Field[] fields = C.getDeclaredFields();
      int projectionCount = 0, whereParamCount = 0;
      for (Field field : fields)
      {
         SelectColumn selectAnno = field.getAnnotation(SelectColumn.class);
         if (selectAnno != null)
            projectionCount++;
         WhereParameter whereParamAnno = field.getAnnotation(WhereParameter.class);
         if (whereParamAnno != null)
            whereParamCount++;
      }
      Where whereAnno = C.getAnnotation(Where.class);
      String where = null;
      if (whereAnno != null)
         where = whereAnno.value();
      if ( (where == null) && (whereParamCount > 0) )
         throw new RuntimeException("@WhereParameter(s) specified without an @Where");
      ColumnContext[] projection = new ColumnContext[projectionCount];
      List<WhereParameterProperties> whereParamList = new ArrayList<>(whereParamCount);
      Map<Integer, String> usedSeq = new HashMap<>(projectionCount);
      for (Field field : fields)
      {
         SelectColumn selectAnno = field.getAnnotation(SelectColumn.class);
         if (selectAnno != null)
         {
            String columnName = selectAnno.column();
            if ( (columnName == null) || (columnName.trim().isEmpty()) )
               columnName = field.getName();
            int seq = selectAnno.sequence();
            if (seq >= projectionCount)
               throw new RuntimeException("@Select seq " + seq + " is greater than the count of projection columns (" +
                                          projectionCount + "). Note seq is zero based.");
            if (usedSeq.containsKey(seq))
               throw new RuntimeException("@Select seq " + seq + " used more than once in projection columns. " +
                                          "Previous use was for " + usedSeq.get(seq));
            usedSeq.put(seq, columnName);
            String tableName = selectAnno.table();
            if ( (tableName != null) && (! tableName.trim().isEmpty()) )
            {
               if ( (! tables.contains(tableName)) && (! aliases.contains(tableName)) )
                  throw new RuntimeException("@Select table " + tableName + " does not match any specified tables or aliases");
               columnName = tableName + "." + columnName;
            }
            String columnAlias = ( (selectAnno.alias() == null) || (selectAnno.alias().trim().isEmpty()) )
                                   ? "col" + seq
                                   : selectAnno.alias();
            SpatialFunctions function = SpatialFunctions.NONE;
            if (selectAnno.spatialToText())
               function = SpatialFunctions.SPATIAL_TO_TEXT;
            else if (selectAnno.textToSpatial())
               function = SpatialFunctions.SPATIAL_TO_TEXT;
            projection[seq] = new ColumnContext(columnName, columnAlias, function);
         }
         WhereParameter whereParamAnno = field.getAnnotation(WhereParameter.class);
         if (whereParamAnno != null)
         {
            String paramTable = whereParamAnno.table();
            if (paramTable.trim().isEmpty())
               paramTable = null;
            String paramColumn = whereParamAnno.column();
            if ( (paramColumn == null) || (paramColumn.trim().isEmpty()) )
               paramColumn = field.getName();
            String paramName = whereParamAnno.name();
            if ( (paramName == null) || (paramName.trim().isEmpty()) )
               paramName = paramColumn;
            int paramType = whereParamAnno.SQLType();
            boolean isTextToSpatial = whereParamAnno.textToSpatial();
            whereParamList.add(new WhereParameterProperties(paramName, paramTable, paramColumn, paramType, isTextToSpatial));
         }
      }
      usedSeq.clear(); usedSeq = null;
      WhereParameterProperties[] whereParams = (whereParamList == null) ? null : whereParamList.toArray(new WhereParameterProperties[whereParamCount]);

      OrderBy orderAnno = C.getAnnotation(OrderBy.class);
      String[] orderBy = null;
      if (orderAnno != null)
         orderBy = orderAnno.value();

      Circle circleAnno = C.getAnnotation(Circle.class);
      BoundingBox bboxAnno = C.getAnnotation(BoundingBox.class);
      String spatialSearchColumn = null;
      double[] bbox = null;
      if (circleAnno != null)
      {
         operation = ISpatialSource.SPATIAL_OPERATION.RADIUS;
         spatialSearchColumn = circleAnno.column();
         if ( (spatialSearchColumn == null) && (spatialSearchColumn.trim().isEmpty()) )
            throw new RuntimeException("Spatial search column not specified");
         operation = ISpatialSource.SPATIAL_OPERATION.RADIUS;
         radius = circleAnno.radius();
      }
      else if (bboxAnno != null)
      {
         if (spatialSearchColumn != null)
            throw new RuntimeException("Cannot specify both @Circle and @BoundingBox");
         spatialSearchColumn = bboxAnno.column();
         operation = ISpatialSource.SPATIAL_OPERATION.BOUNDING_BOX;
         width = bboxAnno.width();
         height = bboxAnno.height();
      }
      else
         operation = ISpatialSource.SPATIAL_OPERATION.USER_DEFINED;

      dataSource.configureSelection(projection, table, alias, joins, where, whereParams, orderBy,
                                    spatialSearchColumn);
      dataSource.configureAnnotationProcessor(this, instance);
      return dataSource;
   }

   @Override
   public Map<String, Object> processParameters(Object instance, Map<String, Object> parameters)
   //-------------------------------------------------------------------------------------------------------
   {
      Class<?> C = instance.getClass();
      if (parameters == null)
         parameters = new HashMap<>();
      Field[] fields = C.getDeclaredFields();
      for (Field field : fields)
      {
         WhereParameter whereParamAnno = field.getAnnotation(WhereParameter.class);
         if (whereParamAnno != null)
         {
            String paramTable = whereParamAnno.table();
            if (paramTable.trim().isEmpty())
               paramTable = null;
            String paramColumn = whereParamAnno.column();
            if ( (paramColumn == null) || (paramColumn.trim().isEmpty()) )
               paramColumn = field.getName();
            String paramName = whereParamAnno.name();
            if ( (paramName == null) || (paramName.trim().isEmpty()) )
               paramName = paramColumn;
            int paramType = whereParamAnno.SQLType();
            Object v;
            try
            {
               field.setAccessible(true);
               v = field.get(instance);
            }
            catch (IllegalAccessException e)
            {
               v = null;
               Log.e(TAG, "Error obtaining parameter " + paramName + " value.", e);
            }
            if (paramType != Integer.MAX_VALUE)
               parameters.put(paramName, new QueryParameter(paramType, v));
            else
               parameters.put(paramName, v);
         }
      }
      return parameters;
   }

   public void processCursor(Object instance, Cursor cursor, ColumnContext[] projectionColumns)
         throws IllegalAccessException
   //------------------------------------------------------------------------------------------
   {
      Class<?> C = instance.getClass();
      Field[] fields = C.getDeclaredFields();
      Map<String, Field> projectionMap = new HashMap<>(projectionColumns.length);
      Map<String, Field> aliasMap = new HashMap<>(projectionColumns.length);
      for (Field field : fields)
      {
         SelectColumn selectAnno = field.getAnnotation(SelectColumn.class);
         if (selectAnno != null)
         {
            String columnName = selectAnno.column();
            if ( (columnName == null) || (columnName.trim().isEmpty()) )
               columnName = field.getName();
            field.setAccessible(true);
            projectionMap.put(columnName, field);
            if ( (selectAnno.alias() != null) && (! selectAnno.alias().trim().isEmpty()) )
               aliasMap.put(selectAnno.alias(), field);
         }
      }

      for (int i=0; i<projectionColumns.length; i++)
      {
         String column = projectionColumns[i].getName();
         String alias = projectionColumns[i].getAlias();
         String name = column;
         int index = cursor.getColumnIndex(column);
         if ( (index < 0) && (alias != null) )
         {
            index = cursor.getColumnIndex(alias);
            name = alias;
         }
         if (index < 0)
         {
            Log.w(TAG, "WARNING: Could not find " + column + ( (alias != null) ? " alias " + alias : "") +
                  " in projection columns");
            continue;
         }
         Field field = projectionMap.get(column);
         if (field == null)
            field = aliasMap.get(alias);
         if (field == null)
            throw new RuntimeException("Could not find annotated field for column " + column + " (alias " + alias + ")");
         Class<?> typ = field.getType();
         switch (cursor.getType(index))
         {
            case Cursor.FIELD_TYPE_NULL:
               if (PRIMITIVES.contains(typ))
                  throw new RuntimeException("ERROR: Field " + field.getName() + " of class " + C.getName() +
                                             " is a primitive and cannot be set to null (column " + name + " == null)");
               field.set(instance, null);
               break;
            case Cursor.FIELD_TYPE_BLOB:
               if (typ == byte[].class)
                  field.set(instance, cursor.getBlob(index));
               else if (typ == String.class)
                  field.set(instance, new String(cursor.getBlob(index)));
               else
                  throw new RuntimeException("Cannot set " + field.getName() + " of class " + C.getName() +
                                             " as a blob (requires byte[] or String)");
               break;

            default:
               boolean isSet = setf(cursor, index, field, typ, instance);
               break;
         }
      }
   }
}
