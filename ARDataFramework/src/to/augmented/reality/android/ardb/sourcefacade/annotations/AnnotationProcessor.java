package to.augmented.reality.android.ardb.sourcefacade.annotations;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract public class AnnotationProcessor
//=======================================
{
   abstract protected String tag();

   protected Context context = null;

   protected Set<Class<?>> PRIMITIVES = new HashSet<Class<?>>() {{ add(int.class); add(boolean.class); add(char.class);
      add(double.class); add(float.class); add(long.class); add(byte.class); add(short.class); }};

   protected Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<Class<?>, Class<?>>()
      { {
         put(boolean.class, Boolean.class);
         put(byte.class, Byte.class);
         put(char.class, Character.class);
         put(double.class, Double.class);
         put(float.class, Float.class);
         put(int.class, Integer.class);
         put(long.class, Long.class);
         put(short.class, Short.class);
         put(void.class, Void.class);
      } };

   public AnnotationProcessor() {}

   public AnnotationProcessor(Context context) { this.context = context; }

   protected boolean setf(Cursor cursor, int index, Field field, Class<?> typ, Object instance)
   //----------------------------------------------------------------------------------------
   {
      if (typ == String.class)
      {
         String s = cursor.getString(index);
         try { field.set(instance, cursor.getString(index)); return true; } catch (Exception e) { Log.e(tag(), "", e); return false; }
      }
      if ( (typ == int.class) || (typ == Integer.class) )
         try { field.set(instance, cursor.getInt(index)); return true; } catch (Exception e) { Log.e(tag(), "", e); return false;}
      if ( (typ == long.class) || (typ == Long.class) )
         try { field.set(instance, cursor.getLong(index)); return true; } catch (Exception e) { Log.e(tag(), "", e); return false; }
      if ( (typ == boolean.class) || (typ == Boolean.class) )
      {
         boolean b = (cursor.getInt(index) != 0);
         try { field.set(instance, b); return true; } catch (Exception e) { Log.e(tag(), "", e); return false; }
      }
      if ( (typ == double.class) || (typ == Double.class) )
         try { field.set(instance, cursor.getDouble(index)); return true; } catch (Exception e) { Log.e(tag(), "", e); return false; }
      if ( (typ == float.class) || (typ == Float.class) )
         try { field.set(instance, cursor.getFloat(index)); return true; } catch (Exception e) { Log.e(tag(), "", e); return false; }
      if ( (typ == short.class) || (typ == Short.class) )
         try { field.set(instance, cursor.getShort(index)); return true; } catch (Exception e) { Log.e(tag(), "", e); return false; }
      return false;
   }


   protected String extractMethodOrField(Class<?> annotatedClass, Object instance, Class<? extends Annotation> annotation)
//         throws InvocationTargetException, IllegalAccessException
   //------------------------------------------------------------------------------------------------------
   {
      String value = "";
      Method[] methods = annotatedClass.getDeclaredMethods();
      boolean isMethod = false;
      for (Method method : methods)
      {
         if (method.isAnnotationPresent(annotation))
         {
            if (method.getReturnType() != java.lang.String.class)
            {
               final String s = "JDBC annotated method " + method.getName() + " should return a String";
               Log.e(tag(), s);
               throw new RuntimeException(s);
            }
            if (method.getParameterTypes().length > 0)
            {
               final String s = "JDBC annotated method " + method.getName() + " should take no parameters";
               Log.e(tag(), s);
               throw new RuntimeException(s);
            }
            try
            {
               method.setAccessible(true);
               value = (String) method.invoke(instance);
            }
            catch (Exception e)
            {
               Log.e(tag(), "", e);
               return "";
            }
            isMethod = true;
            break;
         }
      }
      if (! isMethod)
      {
         Field[] fields = annotatedClass.getDeclaredFields();
         for (Field field : fields)
         {
            if (field.isAnnotationPresent(annotation))
            {
               if (field.getType() != java.lang.String.class)
               {
                  final String s = "JDBC annotated field " + field.getName() + " should be a String";
                  Log.e(tag(), s);
                  throw new RuntimeException(s);
               }
               try
               {
                  field.setAccessible(true);
                  value = (String) field.get(instance);
               }
               catch (Exception e)
               {
                  Log.e(tag(), "", e);
                  return "";
               }
               break;
            }
         }
      }
      return value;
   }

   protected String getAnnotatedField(Field field, Class<? extends Annotation> annotation, boolean isIncludeSubClasses,
                                      Object instance, Class<?>... allowedTypes)
   //----------------------------------------------------------------------------------------------------------------
   {
      Annotation anno = field.getAnnotation(annotation);
      if (anno == null)
         return null;
      Class<?> type = null;
      for (Class<?> allowedType : allowedTypes)
      {
         Class<?> fieldtype = field.getType();
         if (fieldtype == allowedType)
         {
            type = allowedType;
            break;
         }

         if (isIncludeSubClasses)// && (allowedType.isAssignableFrom(fieldtype)) )
         {
            if ( (PRIMITIVES.contains(fieldtype)) && (! PRIMITIVES.contains(allowedType)) )
               fieldtype = PRIMITIVE_WRAPPERS.get(fieldtype);
            else if ( (PRIMITIVES.contains(allowedType)) && (! PRIMITIVES.contains(fieldtype)) )
               allowedType = PRIMITIVE_WRAPPERS.get(allowedType);
            if (allowedType.isAssignableFrom(fieldtype))
            {
               type = allowedType;
               break;
            }
         }
      }
      if (type == null)
         return null;
      try
      {
         field.setAccessible(true);
         Object o = field.get(instance);
         return o.toString();
      }
      catch (Exception e)
      {
         Log.e(tag(), "", e);
         return null;
      }
   }

//   private <T extends Comparable<T>> T linearSearch(T[] array, T item)
//   {
//      for (int i=0; i<array.length; i++)
//         if (array[i].compareTo(item) == 0)
//            return item;
//      return null;
//   }
}
