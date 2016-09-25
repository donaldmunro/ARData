package to.augmented.reality.android.ardb.sourcefacade;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.JdbcSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.maps.MapsSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSource;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.SpatiaLiteSource;
import to.augmented.reality.android.ardb.sourcefacade.jdbc.JdbcAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.maps.MapsAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.sparql.SparqlAnnotationProcessor;
import to.augmented.reality.android.ardb.sourcefacade.spatialite.SpatiaLiteAnnotationProcessor;
import to.augmented.reality.android.ardb.util.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class ARDataSourceAggregator implements LocationListener
//====================================================
{
   static final private String TAG = ARDataSourceAggregator.class.getName();

   private final String name;
   final Map<Class<? extends Annotation>, IAnnotationProcessor> mainAnnotationTypes = new HashMap<>();

   final Map<String, SpatialSourceContext> sources = new HashMap<>();
   private final Context context;
   private final ActiveObject activeObject;
   private final Anything token;
   private final ISpatialQueryResult callback;
   private SQLiteDatabase androidDatabase = null;

   public ARDataSourceAggregator(String name, Context context, ActiveObject activeObject, SQLiteDatabase localDatabase,
                                 Anything callbackToken, ISpatialQueryResult callback)
   //---------------------------------------------------------------------------------------------------
   {
      if ( (localDatabase == null) || (! localDatabase.isOpen()) || (localDatabase.isReadOnly()) )
         throw new RuntimeException("Local (Android SQLite) database must be not null, open and writable");
      this.name = name;
      this.context = context;
      this.activeObject = activeObject;
      androidDatabase = localDatabase;
//      mainAnnotationTypes.put(JdbcSource.class, new JdbcAnnotationProcessor());
//      mainAnnotationTypes.put(SparQLSource.class, new SparqlAnnotationProcessor());
      addMainAnnotationType(JdbcSource.class, new JdbcAnnotationProcessor());
      addMainAnnotationType(SparQLSource.class, new SparqlAnnotationProcessor(context));
      addMainAnnotationType(MapsSource.class, new MapsAnnotationProcessor(context));
      addMainAnnotationType(SpatiaLiteSource.class, new SpatiaLiteAnnotationProcessor(context));
      this.token = callbackToken;
      this.callback = new DelegatingSpatialQueryResult(callback);
   }

   public void addMainAnnotationType(Class<? extends Annotation> annotationClass, IAnnotationProcessor processor)
   //-----------------------------------------------------------------------------------------------------------
   {
      mainAnnotationTypes.put(annotationClass, processor);
   }

   public void addSpatialSource(ISpatialSource source)
   //-------------------------------------------------
   {
      if (androidDatabase != null)
         source.setLocalDatabase(androidDatabase);
      sources.put(source.getName(), new SpatialSourceContext(source));
   }

   public void addSpatialSource(ISpatialSource source, double radius)
   //----------------------------------------------------------------
   {
      if (androidDatabase != null)
         source.setLocalDatabase(androidDatabase);
      SpatialSourceContext sourceContext = new SpatialSourceContext(source, radius);
      sources.put(source.getName(), sourceContext);
   }

   public void addSpatialSource(ISpatialSource source, double width, double height)
   //---------------------------------------------------------------------------------------------------------
   {
      if (androidDatabase != null)
         source.setLocalDatabase(androidDatabase);
      SpatialSourceContext sourceContext = new SpatialSourceContext(source, width, height);
      sources.put(source.getName(), sourceContext);
   }

   public void setLocalDatabase(ISpatialSource source, SQLiteDatabase androidDatabase)
   //----------------------------------------------------------
   {
      SpatialSourceContext context = sources.get(source.getName());
      if (context != null)
         context.source.setLocalDatabase(androidDatabase);
      else
         throw new RuntimeException("Unregistered ISpatialSource " + source.getName());
   }

   public void setRadius(ISpatialSource source, double radius)
   //---------------------------------------------------------
   {
      SpatialSourceContext context = sources.get(source.getName());
      if (context != null)
      {
         context.currentOperation = ISpatialSource.SPATIAL_OPERATION.RADIUS;
         context.circleRadius = radius;
      }
      else
         addSpatialSource(source, radius);
   }

   public void setSensitivity(ISpatialSource source, double sensitivity)
   //-----------------------------------------------------------------------------
   {
      SpatialSourceContext context = sources.get(source.getName());
      if (context != null)
         context.sensitivity = sensitivity;
   }

   public void setBoundingBox(ISpatialSource source, double width, double height)
   //-----------------------------------------------------------------------------
   {
      SpatialSourceContext context = sources.get(source.getName());
      if (context != null)
      {
         context.currentOperation = ISpatialSource.SPATIAL_OPERATION.BOUNDING_BOX;
         context.bboxWidth = width;
         context.bboxHeight = height;
      }
      else
         addSpatialSource(source, width, height);
   }

   public void setAnnotationProcessor(ISpatialSource source, IAnnotationProcessor processor)
   //--------------------------------------------------------------------------------------
   {
      SpatialSourceContext context = sources.get(source.getName());
      if (context != null)
         context.annotationProcessor = processor;
      else
         throw new RuntimeException("Spatial source " + source.toString() + " not defined");
   }

   public void setCallbackType(ISpatialSource source, ISpatialQueryResult.CALLBACK_TYPE callbackType)
   //------------------------------------------------------------------------------------------------
   {
      SpatialSourceContext context = sources.get(source.getName());
      if (context != null)
         context.callbackType = callbackType;
      else
         throw new RuntimeException("Spatial source " + source.toString() + " not defined");
   }

   public void setProjectionTypes(ISpatialSource source, DataType[] projectionTypes)
   //------------------------------------------------------------------------------------------------
   {
      SpatialSourceContext context = sources.get(source.getName());
      if (context != null)
         context.source.setProjectionTypes(projectionTypes);
      else
         throw new RuntimeException("Spatial source " + source.toString() + " not defined");
   }

   public boolean remove(ISpatialSource source) { return sources.remove(source.getName()) != null; }

   public boolean remove(String name) { return sources.remove(name) != null; }

   public ISpatialSource[] registerAnnotated(Object... pojos) throws InvocationTargetException, IllegalAccessException
   //-------------------------------------------------------------------------------------------------
   {
      ISpatialSource[] srcs = new ISpatialSource[pojos.length];
      int i = 0;
      for (Object pojo : pojos)
      {
         Class<?> klass = pojo.getClass();
         IAnnotationProcessor processor = null;
         for (Class<? extends Annotation> C : mainAnnotationTypes.keySet())
         {
            if (klass.isAnnotationPresent(C))
            {
               IAnnotationProcessor proc = mainAnnotationTypes.get(C);
               if (proc == null)
                  throw new RuntimeException("No annotation class registered for " + C.getName());
               Constructor c = Util.getConstructor(proc.getClass(), proc, Context.class);
               if (c != null)
                  try { processor = (IAnnotationProcessor) c.newInstance(context); } catch (Exception e) { processor = null; }
               if (processor == null)
               {
                  Method m = Util.getCloneMethod(proc.getClass(), proc);
                  try { processor = (IAnnotationProcessor) m.invoke(proc); } catch (Exception _e) { processor = null; }
                  if (processor == null)
                  {
                     c = Util.getEmptyConstructor(proc.getClass(), proc);
                     if (c != null)
                        try { processor = (IAnnotationProcessor) c.newInstance(); } catch (Exception e) { processor = null; }
                  }
               }
               break;
            }
         }
         if (processor == null)
         {
            StringBuilder requiredAnnos = new StringBuilder();
            for (Class<? extends Annotation> C : mainAnnotationTypes.keySet())
               requiredAnnos.append('@').append(C.getSimpleName()).append(' ');
            throw new RuntimeException("Datasource " + name + ": Class " + klass.getName() +
                                             " is not annotated as a datasource (requires one of " +
                                             requiredAnnos.toString() + ")");
         }
         ISpatialSource source = processor.createAnnotated(name, context, activeObject, pojo);
         if (source == null)
            throw new RuntimeException("Error creating spatial source using annotation class " + klass.getName());
         remove(source);
         switch (processor.operation())
         {
            case RADIUS:         addSpatialSource(source, processor.circleRadius()); break;
            case BOUNDING_BOX:   addSpatialSource(source, processor.bboxWidth(), processor.bboxHeight()); break;
            default:             addSpatialSource(source);
         }
         setAnnotationProcessor(source, processor);
         setCallbackType(source, ISpatialQueryResult.CALLBACK_TYPE.ANNOTATED_OBJECT);
         srcs[i++] = source;
      }
      return srcs;
   }

   static class InProgress
   //=====================
   {
      Future<?> future = null;
      Location location = null, nextLocation = null;
   }

   Map<String, InProgress> inProgressMap = new HashMap<>();

   @Override
   public void onLocationChanged(final Location location)
   //----------------------------------------------------
   {
      for (final SpatialSourceContext context : sources.values())
         onSourceLocationChange(context, location);
   }

   protected void onSourceLocationChange(SpatialSourceContext context, Location location)
   //------------------------------------------------------------------------------------
   {
      Future<?> f;
      final ISpatialSource source = context.source;
      InProgress inProgress = inProgressMap.get(source.getName());
      if (inProgress == null)
      {
         inProgress = new InProgress();
         f = null;
      }
      else
      {
         if ( (inProgress.location != null) && (location.distanceTo(inProgress.location) <= context.sensitivity) )
            return;
         f = inProgress.future;
      }
      if ( (f == null) || (f.isDone()) || (f.isCancelled()) )
      {
         f = null;
         switch (context.currentOperation)
         {
            case RADIUS:
               f = source.radius(location.getLatitude(), location.getLongitude(), context.circleRadius,
                                 context.callbackType, token, callback);
               break;
            case BOUNDING_BOX:
               f = source.boundingBox(location.getLatitude(), location.getLongitude(), context.bboxWidth,
                                      context.bboxHeight, context.callbackType, token, callback);
               break;
            case USER_DEFINED:
               f = source.spatialQuery(location.getLatitude(), location.getLongitude(), null, context.callbackType, token,
                                       callback);
               break;
         }
         if (f != null)
         {
            inProgress.location = location;
            inProgress.nextLocation = null;
            inProgress.future = f;
            inProgressMap.put(source.getName(), inProgress);
         }
         else
            Log.e(TAG, "source " + source.getName() + " operation " + context.currentOperation + " returned null");
      }
      else
         inProgress.nextLocation = location;
   }

   class DelegatingSpatialQueryResult implements ISpatialQueryResult
   //================================================================
   {
      final ISpatialQueryResult delegate;

      public DelegatingSpatialQueryResult(ISpatialQueryResult delegate) { this.delegate = delegate;  }


      @Override public void onDatasetStart(String sourceName, Anything token) { delegate.onDatasetStart(sourceName, token); }

      @Override
      public void onImageAvailable(String sourceName, Anything token, Bitmap image)
      //---------------------------------------------------------------------------
      {
         delegate.onImageAvailable(sourceName, token, image);
      }

      @Override
      public void onCursorAvailable(String sourceName, Anything token, Cursor cursor)
      //-----------------------------------------------------------------------------
      {
         delegate.onCursorAvailable(sourceName, token, cursor);
         updateLastLocation(sourceName);
      }

      @Override
      public void onDataPointAvailable(String sourceName, Anything token, DataPoint data)
      //---------------------------------------------------------------------------------
      {
         delegate.onDataPointAvailable(sourceName, token, data);
         updateLastLocation(sourceName);
      }

      @Override
      public void onAnnotationAvailable(String sourceName, Anything token, Object annotated)
      //------------------------------------------------------------------------------------
      {
         delegate.onAnnotationAvailable(sourceName, token, annotated);
         updateLastLocation(sourceName);
      }

      @Override
      public void onError(String sourceName, Anything token, CharSequence message, Throwable exception)
      //----------------------------------------------------------------------------
      {
         delegate.onError(sourceName, token, message, exception);
      }

      @Override public void onDatasetEnd(String sourceName, Anything token) { delegate.onDatasetEnd(sourceName, token); }

      private void updateLastLocation(final String sourceName)
      //------------------------------------------------------
      {
         InProgress inProgress = inProgressMap.get(sourceName);
         if ( (inProgress != null) && (inProgress.nextLocation != null) )
         {
            SpatialSourceContext context = sources.get(sourceName);
            Location location = inProgress.nextLocation;
            inProgress.nextLocation = null;
            inProgress.future = null;
            onSourceLocationChange(context, location);
         }
      }
   }

   public void stop()
   //----------------
   {
      for (InProgress inProgress: inProgressMap.values())
      {
         Future<?> f = inProgress.future;
         if ( (f != null) && (! f.isDone()) && (! f.isCancelled()) )
            f.cancel(true);
      }
      activeObject.stop();
   }

   @Override
   public void onStatusChanged(String provider, int status, Bundle extras)
   //----------------------------------------------------------------------
   {
      for (SpatialSourceContext context : sources.values())
      {
         ISpatialSource source = context.source;
         if (source instanceof LocationListener)
            ((LocationListener)source).onStatusChanged(provider, status, extras);
      }
   }

   @Override
   public void onProviderEnabled(String provider)
   //--------------------------------------------
   {
      for (SpatialSourceContext context : sources.values())
      {
         ISpatialSource source = context.source;
         if (source instanceof LocationListener)
            ((LocationListener)source).onProviderEnabled(provider);
      }
   }

   @Override
   public void onProviderDisabled(String provider)
   //---------------------------------------------
   {
      for (SpatialSourceContext context : sources.values())
      {
         ISpatialSource source = context.source;
         if (source instanceof LocationListener)
            ((LocationListener)source).onProviderDisabled(provider);
      }
   }

   class SpatialSourceContext
   //========================
   {
      ISpatialSource source = null;

      ISpatialSource.SPATIAL_OPERATION currentOperation = null;

      double circleRadius, bboxWidth, bboxHeight, sensitivity = 10;

      ISpatialQueryResult.CALLBACK_TYPE callbackType = ISpatialQueryResult.CALLBACK_TYPE.RAW_CURSOR;

      IAnnotationProcessor annotationProcessor;

      public SpatialSourceContext(ISpatialSource source)
      //------------------------------------------------
      {
         this.source = source;
         this.currentOperation = ISpatialSource.SPATIAL_OPERATION.USER_DEFINED;
      }

      public SpatialSourceContext(ISpatialSource source, double circleRadius)
      //---------------------------------------------------------------------
      {
         this.source = source;
         this.currentOperation = ISpatialSource.SPATIAL_OPERATION.RADIUS;
         this.circleRadius = circleRadius;
      }

      public SpatialSourceContext(ISpatialSource source, double bboxWidth, double bboxHeight)
      //-------------------------------------------------------------------------------------
      {
         this.source = source;
         this.currentOperation = ISpatialSource.SPATIAL_OPERATION.BOUNDING_BOX;
         this.bboxWidth = bboxWidth;
         this.bboxHeight = bboxHeight;
      }

      public void setSensitivity(double sensitivity) { this.sensitivity = sensitivity; }

      @Override
      public boolean equals(Object o)
      //----------------------------
      {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         SpatialSourceContext that = (SpatialSourceContext) o;

         return source.getName().equals(that.source.getName());

      }

      @Override public int hashCode() { return source.getName().hashCode(); }
   }
}
