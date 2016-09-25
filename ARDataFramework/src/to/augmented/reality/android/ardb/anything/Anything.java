package to.augmented.reality.android.ardb.anything;

import to.augmented.reality.android.ardb.http.MIME_TYPES;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Anything implements ImmutableAnything
//================================================
{
   final public static Nothing NOTHING = new Nothing();

   private Object contents = NOTHING;

   public boolean isEmpty() { return ( (contents == NOTHING) || (contents == null) ); }

   private Map<String, Anything> map = null;

   public boolean isMap() { return (map != null); }

   private List<Anything> list = null;

   public boolean isList() { return (list != null); }

   Type typ = Type.NOTHING;

   public Anything() { }

   public Anything(Object o) { contents = o;  typ = Type.OBJECT; }

   public Anything(String s) { contents = s;  typ = Type.STRING; }

   public Anything(long l) { contents = Long.valueOf(l); typ = Type.LONG; }

   public Anything(int i) { contents = Integer.valueOf(i); typ = Type.INT; }

   public Anything(boolean b) { contents = Boolean.valueOf(b); typ = Type.BOOLEAN; }

   public Anything(URI uri) { contents = uri; typ = Type.URI; }

   public Anything(URL url) { contents = url; typ = Type.URL; }

   public Anything(MIME_TYPES mimeType) { contents = mimeType; typ = Type.MIME; }

   @Override public Type what() { return typ; }

   @Override
   public String asString(String def)
   //--------------------------------
   {
      switch (typ)
      {
         case STRING: return (String) contents;
         case LONG:
         case URI:
         case URL:
         case MIME:
            return contents.toString();
         default:
            return def;
      }
   }

   @Override
   public long asLong(long def)
   //--------------------------
   {
      switch (typ)
      {
         case LONG: return (Long) contents;
         case INT: return ((Integer) contents).longValue();
         case STRING:
            long l;
            try { l = Long.parseLong(((String) contents).trim()); } catch (NumberFormatException e) { l = def; }
            return l;
         default:
            return def;
      }
   }

   @Override
   public int asInt(int def)
   //--------------------------
   {
      switch (typ)
      {
         case INT: return (Integer) contents;
         case LONG: return ((Long) contents).intValue();
         case STRING:
            int i;
            try { i = Integer.parseInt(((String) contents).trim()); } catch (NumberFormatException e) { i = def; }
            return i;
         default:
            return def;
      }
   }

   @Override public boolean asBoolean() { return false; }

   @Override
   public boolean asBoolean(Boolean def)
   //-----------------------------------
   {
      switch (typ)
      {
         case BOOLEAN: return (Boolean) contents;
         case STRING:
            return Boolean.parseBoolean(((String) contents).trim());
         case LONG:
            long l = (Long) contents;
            return (l == 0) ? false : true;
         case INT:
            int i = (Integer) contents;
            return (i == 0) ? false : true;
         default:
            return def;
      }
   }

   @Override
   public URI asURI(URI def)
   //-----------------------
   {
      switch (typ)
      {
         case URI: return (URI) contents;
         case URL:
            try { return ((URL) contents).toURI(); } catch (URISyntaxException e) { return def; }
      }
      return def;
   }

   @Override
   public URL asURL(URL def)
   //-----------------------
   {
      switch (typ)
      {
         case URL: return (URL) contents;
         case URI:
            try { return ((URI) contents).toURL(); } catch (MalformedURLException e) { return def; }
      }
      return def;
   }

   @Override
   public MIME_TYPES asMime(MIME_TYPES def)
   //--------------------------------------
   {
      switch (typ)
      {
         case MIME: return (MIME_TYPES) contents;
         case STRING:
            MIME_TYPES v = MIME_TYPES.keyOf((String) contents);
            if (v != null)
               return v;
            break;
      }
      return def;
   }

   protected Object asObject(Object def) { return ((isEmpty() || isList() || isMap()) ? def : contents); }

   @Override
   public Object[] asArray(Object defentry)
   //-----------------------
   {
      int i = 0;
      if (list != null)
      {
         Object[] ao = new Object[list.size()];
         for (Anything a : list)
            ao[i++] = a.asObject(defentry);
         return ao;
      }
      else if (map != null)
      {
         Object[] ao = new Object[map.size()];
         for (Anything a : map.values())
            ao[i++] = a.asObject(defentry);
         return map.values().toArray(new Object[0]);
      }
      else if (! isEmpty())
      {
         final String s = asString(null);
         if (s != null)
         {
            String[] as = new String[1];
            as[0] = s;
            return as;
         }
      }
      return new Object[0];
   }

   @Override
   public Map<Object, Object> asMap(Map<Object, Object> def)
   //-------------------------------------------------------
   {
      if ( (! isMap()) || (map == null) )
         return def;
      Map<Object, Object> m = new HashMap<>(map.size());
      Set<Map.Entry<String, Anything>> es = map.entrySet();
      for (Map.Entry<String, Anything> e : es)
         m.put(e.getKey(), e.getValue());
      return m;
   }

   @Override
   public Iterator<Map.Entry<String, Anything>> mapIterator()
   //--------------------------------------------------------
   {
      if ( (! isMap()) || (map == null) )
         return EmptyIterator.INSTANCE;;
      return map.entrySet().iterator();
   }

   @Override public ImmutableAnything getImmutable(String name) { return getImmutable(name, NOTHING); }

   @Override
   public ImmutableAnything getImmutable(String name, ImmutableAnything def)
   //-----------------------------------------------------------------------
   {
      if (map != null)
      {
         Anything anything = map.get(name);
         return (anything == null) ? NOTHING :anything;
      }
      return def;
   }

   @Override public ImmutableAnything getImmutable(int index) { return getImmutable(index, NOTHING); }

   @Override
   public ImmutableAnything getImmutable(int index, ImmutableAnything def)
   //---------------------------------------------------------------------
   {
      if (list != null)
         return list.get(index);
      return def;
   }

   public void put(String k, Anything v)
   //----------------------------------
   {
      if (list != null)
         throw new RuntimeException("Anything cannot be both a Map and List");
      if (map == null)
      {
         contents = map = new HashMap<String, Anything>();
         typ = Type.MAP;
      }
      map.put(k, v);
   }

   public void put(String name, String value) { put(name, new Anything(value)); }

   public void put(String name, URI uri) { put(name, new Anything(uri)); }

   public void put(String name, URL url) { put(name, new Anything(url)); }

   public void put(String name, int v) { put(name, new Anything(v)); }

   public void put(String name, long v) { put(name, new Anything(v)); }

   public void put(String name, Boolean v) { put(name, new Anything(v)); }

   public void add(Anything v)
   //-------------------------
   {
      if (map != null)
         throw new RuntimeException("Anything cannot be both a Map and List");
      if (list == null)
      {
         contents = list = new ArrayList<>();
         typ = Type.LIST;
      }
      list.add(v);
   }

   public void addStrings(List<? extends CharSequence> list)
   //-------------------------------------------------------
   {
      if (map != null)
         throw new RuntimeException("Anything cannot be both a Map and List");
      if (this.list == null)
      {
         contents = this.list = new ArrayList<>();
         typ = Type.LIST;
      }
      for (CharSequence cs : list)
         this.list.add(new Anything(cs.toString()));
   }

   public void add(Object o) { add(new Anything(o)); }

   public void add(String v) { add(new Anything(v)); }

   public void add(URI v) { add(new Anything(v)); }

   public void add(URL v) { add(new Anything(v)); }

   public void add(long v) { add(new Anything(v)); }

   public void put(int i, Anything v)
   //----------------------------------
   {
      if (map != null)
         throw new RuntimeException("Anything cannot be both a Map and List");
      if (list == null)
         add(v);
      else
      {
         if (i >= list.size())
         {
            for (int j=list.size(); j<=i; j++)
               add(new Anything());
         }
         list.set(i, v);
      }
   }

   public void put(int i, String v) { put(i, new Anything(v)); }

   public void put(int i, URI v) { put(i, new Anything(v)); }

   public void put(int i, URL v) { put(i, new Anything(v)); }

   public void put(int i, long v) { put(i, new Anything(v)); }

   public Anything get(String k, Anything def)
   //-----------------------------------------
   {
      if (map == null)
         return def;
      Anything v = map.get(k);
      return (v != null) ? v : def;
   }

   public String getAsString(String k, String def)
   //-------------------------------------
   {
      Anything v = get(k, null);
      return (v != null) ? v.asString(def) : def;
   }

   public Anything get(String k)
   //--------------------------
   {
      Anything v = get(k, null);
      if (v == null)
         return NOTHING;
      else
         return v;
   }


   static class Nothing extends Anything implements ImmutableAnything
   //=================================================================
   {
      @Override public Type what() { return Type.NOTHING; }

      @Override
      public String asString(String def) { return def; }

      @Override
      public long asLong(long def) {  return def;  }

      @Override public int asInt(int def) { return def; }

      @Override public URI asURI(URI def) {  return def; }

      @Override public URL asURL(URL def) { return def; }

      @Override public MIME_TYPES asMime(MIME_TYPES def) { return def; }

      @Override public Object[] asArray(Object defentry) { return new Object[0]; }

      @Override public boolean asBoolean() { return false; }

      @Override public boolean asBoolean(Boolean def) { return def; }

      @Override public Map<Object, Object> asMap(Map<Object, Object> def) { return def; }

      @Override public Iterator<Map.Entry<String, Anything>> mapIterator() { return EmptyIterator.INSTANCE; }

      @Override public ImmutableAnything getImmutable(String name) { return NOTHING; }

      @Override
      public ImmutableAnything getImmutable(String name, ImmutableAnything def)
      {
         return def;
      }

      @Override public ImmutableAnything getImmutable(int index) { return NOTHING; }

      @Override
      public ImmutableAnything getImmutable(int index, ImmutableAnything def)
      {
         return def;
      }

      @Override public boolean isList() { return false; }

      @Override public boolean isMap() { return false; }

      @Override public boolean isEmpty() { return true; }
   }

   static final class EmptyIterator implements Iterator
   {

      public static final Iterator INSTANCE = new EmptyIterator();

      public boolean hasNext() {
         return false;
      }

      public Object next() {
         throw new UnsupportedOperationException();
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      private EmptyIterator() {}

      public String getAsString(String k, String def) { return def; }
   }
}
