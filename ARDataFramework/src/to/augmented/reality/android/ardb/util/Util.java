package to.augmented.reality.android.ardb.util;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class Util
//===============
{
   final static private String TAG = Util.class.getName();

   static public Pattern startWithWordPattern(String word)
   //-----------------------------------------------------
   {
      return Pattern.compile("^" + word.trim() + "\\s+.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
   }

   static public Constructor getEmptyConstructor(Class<?> C, Object o)
   //-----------------------------------------------------------------
   {
      Constructor constructor = null;
      try
      {
         constructor = C.getDeclaredConstructor();
         constructor.setAccessible(true);
         return constructor;
      }
      catch (Exception e)
      {
         Log.e(TAG, "get empty constructor", e);
         return null;
      }
   }

   static public Constructor getConstructor(Class<?> C, Object o, Class<?>... params)
   //--------------------------------------------------------------------------------
   {
      Constructor constructor = null;
      try
      {
         constructor = C.getDeclaredConstructor(params);
         constructor.setAccessible(true);
         return constructor;
      }
      catch (Exception e)
      {
         Log.e(TAG, "get empty constructor", e);
         return null;
      }
   }

   static public Method getCloneMethod(Class<?> C, Object o)
   //-------------------------------------------------------
   {
      Method cloneMethod = null;
      if (o instanceof Cloneable)
      {
         try
         {
            cloneMethod = C.getDeclaredMethod("clone");
            cloneMethod.setAccessible(true);
            return cloneMethod;
         }
         catch (NoSuchMethodException e)
         {
            try
            {
               cloneMethod = C.getSuperclass().getDeclaredMethod("clone");
               cloneMethod.setAccessible(true);
            }
            catch (Exception ee)
            {
               Log.e(TAG, "get clone method", e);
               cloneMethod = null;
            }
         }
         catch (Exception e)
         {
            Log.e(TAG, "get clone method", e);
            cloneMethod = null;
         }
      }
      return cloneMethod;
   }
}
