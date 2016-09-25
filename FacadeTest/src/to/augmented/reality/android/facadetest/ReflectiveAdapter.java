package to.augmented.reality.android.facadetest;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sql.SelectColumn;
import to.augmented.reality.android.ardb.sourcefacade.annotations.sparql.SparQLSelectVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ReflectiveAdapter extends ArrayAdapter<Object> implements ListAdapter
//================================================================================
{
   public ReflectiveAdapter(Context context, int resource)
   {
      super(context, resource);
   }

   @Override
   public View getView(int position, View convertView, ViewGroup parent)
   //------------------------------------------------------------------
   {
      Object o = getItem(position);
      ViewHolder holder;
      View boundView = convertView;
      if (boundView == null)
         holder = new ViewHolder(boundView);
      else
         holder = (ViewHolder) boundView.getTag();
      if (holder == null)
         holder = new ViewHolder(boundView);
      holder.bind(getItem(position));
      if (boundView == null)
         return holder.tv;
      return boundView;
   }

   class ViewHolder
   {
      final private TextView tv;

      public ViewHolder(final View view)
      //-------------------------------
      {
         tv = new TextView(ReflectiveAdapter.this.getContext());
         tv.setTypeface(Typeface.MONOSPACE);
         tv.setTextSize(10);
         if (view != null)
            view.setTag(this);
         else
            tv.setTag(this);
      }

      public void bind(final Object instance)
      //------------------------------
      {
         Class<? extends Object> C = instance.getClass();
         Field[] fields = C.getDeclaredFields();
         StringBuilder sb = new StringBuilder();
         int rpad = 15;
         for (Field field : fields)
         {
//            JdbcSelectColumn selectAnno = field.getAnnotation(JdbcSelectColumn.class);
//            if (selectAnno != null)
            if (hasSelectAnno(field, SelectColumn.class, SparQLSelectVariable.class))
            {
               String s = getValue(field, instance);
               if (s == null)
                  s = "null";
               if (s.length() <= rpad)
                  sb.append(String.format("%1$-" + rpad + "s", s));
               else
                  sb.append(s.substring(0, rpad));
               sb.append(' ');
            }

         }
         tv.setText(sb.toString());
      }

      private boolean hasSelectAnno(Field field, Class<? extends Annotation>... annotations)
      {
         for (Class<? extends Annotation> annotation : annotations)
            if (field.getAnnotation(annotation) != null)
               return true;
         return false;
      }

      private String getValue(Field field, Object instance)
      //---------------------------------------------------
      {
         Class<?> typ = field.getType();
         field.setAccessible(true);
         if (typ == String.class)
            try { return (String) field.get(instance); } catch (Exception e) { return "*ERROR*"; }
         if ( (typ == int.class) || (typ == Integer.class) )
            try { return Integer.toString(field.getInt(instance)); } catch (Exception e) { return "*ERROR*";}
         if ( (typ == long.class) || (typ == Long.class) )
            try { return Long.toString(field.getLong(instance)); } catch (Exception e) { return "*ERROR*"; }
         if ( (typ == boolean.class) || (typ == Boolean.class) )
            try { return Boolean.toString(field.getBoolean(instance)); } catch (Exception e) { return "*ERROR*"; }
         if ( (typ == double.class) || (typ == Double.class) )
            try { return Double.toString(field.getDouble(instance)); } catch (Exception e) { return "*ERROR*"; }
         if ( (typ == float.class) || (typ == Float.class) )
            try { return Float.toString(field.getFloat(instance)); } catch (Exception e) { return "*ERROR*"; }
         if ( (typ == short.class) || (typ == Short.class) )
            try { return Short.toString(field.getShort(instance)); } catch (Exception e) { return "*ERROR*"; }
         return "*" + typ.getSimpleName() + " UNKNOWN*";
      }
   }


}
