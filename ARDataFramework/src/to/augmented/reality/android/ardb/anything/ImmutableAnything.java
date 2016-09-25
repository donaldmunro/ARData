package to.augmented.reality.android.ardb.anything;


import to.augmented.reality.android.ardb.http.MIME_TYPES;

import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

public interface ImmutableAnything
//--------------------------------
{
   public enum Type { NOTHING, MAP, LIST, STRING, INT, LONG, BOOLEAN, URI, URL, MIME, OBJECT };

   public Type what();

   public String asString(String def);

   public long asLong(long def);

   public int asInt(int def);

   public boolean asBoolean();

   public boolean asBoolean(Boolean def);

   public URI asURI(URI def);

   public URL asURL(URL def);

   public MIME_TYPES asMime(MIME_TYPES def);

   public Object[] asArray(Object defentry);

   public Map<Object, Object> asMap(Map<Object, Object> def);

   public Iterator<Map.Entry<String, Anything>> mapIterator();

   public ImmutableAnything getImmutable(String name);

   public ImmutableAnything getImmutable(String name, ImmutableAnything def);

   public ImmutableAnything getImmutable(int index);

   public ImmutableAnything getImmutable(int index, ImmutableAnything def);

   public boolean isList();

   public boolean isMap();

   public boolean isEmpty();

   public String getAsString(String k, String def);
}
