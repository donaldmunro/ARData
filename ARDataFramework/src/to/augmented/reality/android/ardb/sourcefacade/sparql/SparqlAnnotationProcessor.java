package to.augmented.reality.android.ardb.sourcefacade.sparql;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.http.Cache;
import to.augmented.reality.android.ardb.http.HTTP_METHOD;
import to.augmented.reality.android.ardb.http.sparql.SparQLDialect;
import to.augmented.reality.android.ardb.sourcefacade.ColumnContext;
import to.augmented.reality.android.ardb.sourcefacade.IAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.ISpatialSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.AnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.annotations.BoundingBox;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Circle;
import to.augmented.reality.android.ardb.sourcefacade.annotations.Url;
import to.augmented.reality.android.ardb.sourcefacade.annotations.http.HttpCache;
import to.augmented.reality.android.ardb.sourcefacade.annotations.http.HttpHeader;
import to.augmented.reality.android.ardb.sourcefacade.annotations.http.HttpHeaders;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.DefaultGraphUris;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.NamedGraphUris;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLAddPrefixes;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLFrom;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLGroupBy;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLHaving;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLLimit;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLOffset;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLOrderBy;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLPrefixes;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSelect;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSelectVariable;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLWhere;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLWhereParameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SparqlAnnotationProcessor extends AnnotationProcessor implements IAnnotationProcessor, Cloneable
//============================================================================================================
{
   static final private String TAG = SparqlAnnotationProcessor.class.getName();

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

   public SparqlAnnotationProcessor() {  }

   public SparqlAnnotationProcessor(Context context) { super(context); }

   @Override
   public ISpatialSource createAnnotated(String sourceName, Context context, ActiveObject activeObject, Object instance)
   //------------------------------------------------------------------------------------------------------------------
   {
      final Class<?> C = instance.getClass();
      SparQLSource sparQlSourceAnno = C.getAnnotation(SparQLSource.class);
      if (sparQlSourceAnno == null)
         throw new RuntimeException("SparQL source annotation class must be annotated with the SparQlSource annotation");
      final String name = sparQlSourceAnno.name();
      final SparQLDialect dialect = sparQlSourceAnno.dialect();
      final HTTP_METHOD httpMethod = sparQlSourceAnno.method();
      final String userAgent = sparQlSourceAnno.agent();
      final long connectionTimeout = sparQlSourceAnno.connectionTimeout();
      final TimeUnit connectionTimeoutUnit = sparQlSourceAnno.connectionTimeoutUnit();
      final long readTimeout = sparQlSourceAnno.readTimeout();
      final TimeUnit readTimeoutUnit = sparQlSourceAnno.readTimeoutUnit();
      String endpt = sparQlSourceAnno.endpoint();
      if ( (endpt == null) || endpt.trim().isEmpty() )
      {
         endpt = extractMethodOrField(C, instance, Url.class);
         if ( (endpt == null) || (endpt.trim().isEmpty()) )
         {
            String s = "SParQL endpoint must be specified either in the @SparQlSource annotation or using an @Url " +
                       "annotation on a method or field.";
            Log.e(TAG, s);
            throw new RuntimeException(s);
         }
      }
      final URI endpoint;
      try
      {
         endpoint = new URI(endpt);
      }
      catch (Exception e)
      {
         Log.e(TAG, "Cannot parse SparQL endpoint URI: " + endpt);
         throw new RuntimeException("Cannot parse SparQL endpoint URI: " + endpt);
      }

      HttpCache cacheAnno = C.getAnnotation(HttpCache.class);
      Cache cache = null;
      if (cacheAnno != null)
      {
         File dir;
         if ( (cacheAnno.directory() != null) && (! cacheAnno.directory().trim().isEmpty()) )
            dir = new File(cacheAnno.directory());
         else
            dir = null;
         try
         {
            cache = new Cache(cacheAnno.age(), cacheAnno.ageUnit(), dir, cacheAnno.overideNoCache(), cacheAnno.overideAge());
         }
         catch (FileNotFoundException e)
         {
            throw new RuntimeException("Could not create specified cache directory (" + dir + "). " + e.getMessage());
         }
      }

      final String[] prefixes;
      SparQLPrefixes prefixesAnno = C.getAnnotation(SparQLPrefixes.class);
      if (prefixesAnno != null)
         prefixes = prefixesAnno.value();
      else
         prefixes = null;

      final String[] addPrefixes;
      SparQLAddPrefixes addPrefixesAnno = C.getAnnotation(SparQLAddPrefixes.class);
      if (addPrefixesAnno != null)
         addPrefixes = addPrefixesAnno.value();
      else
         addPrefixes = null;

      final String[] defaultUris;
      DefaultGraphUris defaultGraphUrisAnno = C.getAnnotation(DefaultGraphUris.class);
      if (defaultGraphUrisAnno != null)
         defaultUris = defaultGraphUrisAnno.value();
      else
         defaultUris = null;

      final String[] namedUris;
      NamedGraphUris namedGraphUrisAnno = C.getAnnotation(NamedGraphUris.class);
      if (namedGraphUrisAnno != null)
         namedUris = namedGraphUrisAnno.value();
      else
         namedUris = null;

      SparQLSelect selectAnno = C.getAnnotation(SparQLSelect.class);
      if (selectAnno == null)
      {
         String s = "Annotated SparQL class requires @Select (" + C.getName() + ")";
         Log.e(TAG, s);
         throw new RuntimeException(s);
      }
      final String select = selectAnno.value();

      final String[] from;
      SparQLFrom fromAnno = C.getAnnotation(SparQLFrom.class);
      if (fromAnno != null)
         from = fromAnno.value();
      else
         from = null;

      final String where = getWhere(C);

      final String orderBy;
      SparQLOrderBy orderByAnno = C.getAnnotation(SparQLOrderBy.class);
      if (orderByAnno != null)
         orderBy = orderByAnno.value();
      else
         orderBy = null;

      final String groupBy;
      SparQLGroupBy groupByAnno = C.getAnnotation(SparQLGroupBy.class);
      if (groupByAnno != null)
         groupBy = groupByAnno.value();
      else
         groupBy = null;

      final String having;
      SparQLHaving havingAnno = C.getAnnotation(SparQLHaving.class);
      if (havingAnno != null)
         having = havingAnno.value();
      else
         having = null;

      final String offset;
      SparQLOffset offsetAnno = C.getAnnotation(SparQLOffset.class);
      if (offsetAnno != null)
         offset = Integer.toString(offsetAnno.value());
      else
         offset = null;

      final String limit;
      SparQLLimit limitAnno = C.getAnnotation(SparQLLimit.class);
      if (limitAnno != null)
         limit = Integer.toString(limitAnno.value());
      else
         limit = null;

      SparQLDataSource source = new SparQLDataSource(name, context, dialect, activeObject, this, instance);
      if (cache != null)
         source.setHttpCache(cache);
      source.setEndpoint(endpoint);
      source.setConnectionTimeout(connectionTimeout, connectionTimeoutUnit);
      source.setReadTimeout(readTimeout, readTimeoutUnit);
      source.setMethod(httpMethod);
      if ( (userAgent != null) && (! userAgent.trim().isEmpty()) )
         source.setUserAgent(userAgent);
      if (prefixes != null)
         source.setPrefixes(prefixes);
      if (addPrefixes != null)
         source.addPrefix(addPrefixes);
      if ( (defaultUris != null) && (defaultUris.length > 0) )
         source.setDefaultGraphUris(defaultUris);
      if ( (namedUris != null) && (namedUris.length > 0) )
         source.setNamedGraphUris(namedUris);
      source.setSelect(select);
      source.setFrom(from);
      source.setWhere(where);
      source.setGroupBy(groupBy);
      source.setOrderBy(orderBy);
      source.setHaving(having);
      source.setOffset(offset);
      source.setLimit(limit);
      HttpHeaders headersAnno = C.getAnnotation(HttpHeaders.class);
      if (headersAnno != null)
      {
         HttpHeader[] headers = headersAnno.value();
         for (HttpHeader header : headers)
            source.addHeader(header.key(), header.value());
      }
      else
      {
         HttpHeader headerAnno = C.getAnnotation(HttpHeader.class);
         if (headerAnno != null)
            source.addHeader(headerAnno.key(), headerAnno.value());
      }

      Field[] fields = C.getDeclaredFields();
      for (Field field : fields)
      {
         SparQLSelectVariable varAnno = field.getAnnotation(SparQLSelectVariable.class);
         if (varAnno != null)
         {
            String varname = varAnno.name();
            if ( (varname == null) || (varname.trim().isEmpty()) )
               varname = field.getName();
            String alias = varAnno.alias();
            source.addAnnotationColumn(varname, alias);
         }
      }
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
      source.setSpatialWhereSubjectName(spatialSearchColumn);
      return source;
   }

   private String getWhere(Class<?> C)
   //--------------------------------
   {
      SparQLWhere whereAnno = C.getAnnotation(SparQLWhere.class);
      String where;
      if (whereAnno == null)
         where = "WHERE { }";
      else
         where = whereAnno.value();
      int openCount = where.length() - where.replace("{", "").length();
      int closeCount = where.length() - where.replace("}", "").length();
      if ( (openCount != closeCount) || (openCount == 0) )
      {
         String s = "Mismatched braces ( { } ) or no graph in SparQL WHERE graph: " + where + " (" + C.getName() + ")";
         Log.e(TAG, s);
         throw new RuntimeException(s);
      }
      if (! where.trim().endsWith("}"))
      {
         String s = "No terminating } in SparQL WHERE graph: " + where + " (" + C.getName() + ")";
         Log.e(TAG, s);
         throw new RuntimeException(s);
      }
      return where;
   }


   @Override
   public Map<String, Object> processParameters(Object instance, Map<String, Object> parameters)
   //-------------------------------------------------------------------------------------------
   {
      Class<?> C = instance.getClass();
      if (parameters == null)
         parameters = new HashMap<>();
      final String where = getWhere(C);
      Field[] fields = C.getDeclaredFields();
      for (Field field : fields)
      {
         SparQLWhereParameter whereParamAnno = field.getAnnotation(SparQLWhereParameter.class);
         if (whereParamAnno != null)
         {
            String paramColumn = whereParamAnno.variable();
            if ( (paramColumn == null) || (paramColumn.trim().isEmpty()) )
               paramColumn = field.getName();
            String paramName = whereParamAnno.name();
            if ( (paramName == null) || (paramName.trim().isEmpty()) )
               paramName = paramColumn;
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
            parameters.put(paramName, v);
         }
      }
      return parameters;
   }

   @Override
   public void processCursor(Object instance, Cursor cursor, ColumnContext[] projectionColumns)
         throws IllegalAccessException
   //-----------------------------------------------------------------------------------------
   {
      Class<?> C = instance.getClass();
      Field[] fields = C.getDeclaredFields();
      Map<String, Field> projectionMap = new HashMap<>(projectionColumns.length);
      Map<String, Field> aliasMap = new HashMap<>(projectionColumns.length);
      for (Field field : fields)
      {
         SparQLSelectVariable varAnno = field.getAnnotation(SparQLSelectVariable.class);
         if (varAnno != null)
         {
            String columnName = varAnno.name();
            if ( (columnName == null) || (columnName.trim().isEmpty()) )
               columnName = field.getName();
            field.setAccessible(true);
            projectionMap.put(columnName, field);
            if ( (varAnno.alias() != null) && (! varAnno.alias().trim().isEmpty()) )
               aliasMap.put(varAnno.alias(), field);
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
                  " in SELECT columns");
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
