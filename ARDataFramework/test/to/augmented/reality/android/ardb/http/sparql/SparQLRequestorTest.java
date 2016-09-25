package to.augmented.reality.android.ardb.http.sparql;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.HTTP_METHOD;
import to.augmented.reality.android.ardb.http.MIME_TYPES;
import to.augmented.reality.android.test.BuildConfig;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Config(constants = BuildConfig.class, sdk = 19, manifest = "/ssd/Android/ARData/ARDataFramework/AndroidManifest.xml")
@RunWith(RobolectricGradleTestRunner.class)
public class SparQLRequestorTest
//==============================
{
   @Before
   public void init() { ShadowLog.stream = System.out; }
   @Test
   public void testSyncRequest() throws Exception
   //----------------------------------------------
   {
      SQLiteDatabase database = null;
      File databasePath = null;
      ShadowApplication app = ShadowApplication.getInstance();
      assertNotNull(app);
      Context cont = app.getApplicationContext();
      assertNotNull(cont);
      database = cont.openOrCreateDatabase("SparQLTest", 0, null);
      databasePath = new File(database.getPath());
      try
      {
         SparQLRequestor requestor = new SparQLRequestor(new ActiveObject("SparQLRequestorTest", 3), SparQLDialect.GEOSPARQL);
//         URI uri = new URI("http://10.0.2.2:8080/parliament/sparql");
         URI uri = new URI("http://127.0.0.1:8080/parliament/sparql");
         Anything parameters = new Anything();
         //            query = "select distinct ?Concept where {[] a ?Concept} LIMIT 100";
         String query =
               "prefix : <http://augmented.reality.to/semantic/owl#>\n" +
                     "PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>\n" +
                     "PREFIX fn: <http://www.w3.org/2005/xpath-functions#>\n" +
                     "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                     "PREFIX par: <http://parliament.semwebcentral.org/parliament#>\n" +
                     "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                     "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                     "PREFIX time: <http://www.w3.org/2006/time#>\n" +
                     "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                     "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                     "prefix geo: <http://www.opengis.net/ont/geosparql#>\n" +
                     "prefix geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                     "prefix gml: <http://www.opengis.net/ont/gml#>\n" +
                     "prefix units: <http://www.opengis.net/ont/sf#>\n" +
                     "\n" +
                     "SELECT DISTINCT ?name ?position\n" +
                     "WHERE {\n" +
                     "   ?L a :Location .\n" +
                     "   ?L :hasWGS84Geolocation ?geom .\n" +
                     "   ?geom geo:asWKT ?position .\n" +
                     "   ?L rdfs:label ?name\n" +
                     "}" +
                     "ORDER BY ?name";
         parameters.put("query", query);
         Anything defaultGraphUris = new Anything();
         defaultGraphUris.add("");
         parameters.put("defaultGraphUris", defaultGraphUris);
         StringBuilder errbuf = new StringBuilder();
         Cursor cursor = requestor.request(HTTP_METHOD.GET, uri, null, MIME_TYPES.SPARQL_XML, null, parameters, 30,
                                           TimeUnit.SECONDS, 10, TimeUnit.SECONDS, null, database, "asynctest",
                                           null, errbuf);
         assertNotNull(errbuf.toString(), cursor);
         int c = 0;
         assertTrue(cursor.moveToNext());
         String name = cursor.getString(1);
         assertEquals("A-BLOCK TERRAIN LIGHTS", name);
         String position = cursor.getString(2);
         assertEquals("POINT(-34.00931 25.6685)", position);
      }
      finally
      {
         if (database != null)
            try { database.close(); } catch (Exception _e) { _e.printStackTrace(); }
         if (databasePath != null)
            databasePath.delete();
      }

   }
}
