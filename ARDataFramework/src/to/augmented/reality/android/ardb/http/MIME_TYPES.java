package to.augmented.reality.android.ardb.http;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MIME_TYPES
//=========================
{
   APP_XML("application/xml"),

   TEXT_XML("text/xml"),

   SPARQL_XML("application/sparql-results+xml"),

   JSON("application/json"),

   JSONP("application/javascript"),

   SPARQL_JSON("application/sparql-results+json"),

   CSV("text/csv"),

   TSV("text/tab-separated-values"),

   SPARQL_Turtle("text/turtle"),

   SPARQL_RDFXML("application/rdf+xml"),

   TEXT("text/plain"),

   SPARQL_NTRIPLES("text/plain"),

   PNG("image/png"),

   JPG("image/jpeg"),

   GIF("image/gif"),

   ANY("ANY");

   private static final Map<String, MIME_TYPES> lookup = new HashMap<String, MIME_TYPES>();

   static
   {
      for (MIME_TYPES nm : EnumSet.allOf(MIME_TYPES.class))
         lookup.put(nm.toString(), nm);
   }

   private String mime;

   MIME_TYPES(String mime) { this.mime = mime; }

   @Override
   public String toString() { return mime; }

   static public MIME_TYPES keyOf(String s) { return lookup.get(s); }
}
