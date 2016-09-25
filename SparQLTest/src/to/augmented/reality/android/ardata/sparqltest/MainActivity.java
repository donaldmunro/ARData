package to.augmented.reality.android.ardata.sparqltest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.http.IHttpRequestorCallback;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.*;
import to.augmented.reality.android.ardb.http.sparql.SparQLDialect;
import to.augmented.reality.android.ardb.http.sparql.SparQLRequestor;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity
{
   int queryNum = 1;
   ActiveObject activeObject = new ActiveObject("SparQLTest", 5);
   Future<?> requestFuture = null;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      final EditText editQuery = (EditText) findViewById(R.id.editTextQuery);
      final Spinner spinnerMethod = (Spinner) findViewById(R.id.spinnerMethod);
      final Spinner spinnerEncoding = (Spinner) findViewById(R.id.spinnerEncoding);
      final Button executeButton = (Button) findViewById(R.id.buttonExecute);
      executeButton.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         //-------------------
         {
            String query = editQuery.getText().toString();
            if (query.trim().isEmpty())
               return;
            String s = (String) spinnerMethod.getSelectedItem();
            HTTP_METHOD method;
            switch (s)
            {
               case "GET":    method = HTTP_METHOD.GET; break;
               case "POST":   method = HTTP_METHOD.POST; break;
               default:       return;
            }
            s = (String) spinnerEncoding.getSelectedItem();
            MIME_TYPES encoding = MIME_TYPES.keyOf(s);
            SparQLRequestor requestor = new SparQLRequestor(activeObject, SparQLDialect.GEOSPARQL);
            URI uri = null;
//            try { uri = new URI("http://dbpedia.org/sparql"); } catch (Exception e) { Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show(); }
//            try { uri = new URI("http://127.0.0.1:8080/parliament/sparql"); } catch (Exception e) { Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show(); }
            try { uri = new URI("http://10.0.2.2:8080/parliament/sparql"); } catch (Exception e) { Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show(); }
            Map<String, String> header = new HashMap<>();
            Anything parameters = new Anything();
//            query = "select distinct ?Concept where {[] a ?Concept} LIMIT 100";
            query = "prefix : <http://augmented.reality.to/semantic/owl#>\n" +
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
                  "SELECT DISTINCT ?l ?wkt\n" +
                  "WHERE {\n" +
                  "   ?L a :Location .\n" +
                  "   ?L :hasWGS84Geolocation ?geom .\n" +
                  "   ?geom geo:asWKT ?wkt .\n" +
                  "   ?L rdfs:label ?l\n" +
                  "}";
            parameters.put("query", query);
            Anything defaultGraphUris = new Anything();
            defaultGraphUris.add("");
            parameters.put("defaultGraphUris", defaultGraphUris);
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ardata");
            if (! dir.isDirectory())
               dir.delete();
            if (! dir.exists())
               dir.mkdirs();
            if ( (! dir.exists()) || (! dir.isDirectory()) )
            {
               Log.e("MainActivity", "Error making storage directory " + dir.getAbsolutePath());
               Toast.makeText(MainActivity.this, "Error making storage directory " + dir.getAbsolutePath(),
                              Toast.LENGTH_LONG).show();
               return;
            }
            final File f = new File(dir, "results." + queryNum);
            FileOutputStream fw;
            try { fw = new FileOutputStream(f); } catch (Exception e) { Log.e("MainActivity", "", e); return; }
            Anything token = new Anything(queryNum++);
            IHttpRequestorCallback callback = new IHttpRequestorCallback()
            {
               @Override
               public void onResponse(Anything token, int code)
               {
                  Log.i("MainActivity", f.getAbsolutePath());
                  Toast.makeText(MainActivity.this, "Created file " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                  requestFuture = null;
               }

               @Override
               public void onError(Anything token, int code, CharSequence message, Throwable e)
               {
                  Log.e("MainActivity", code + ":" + message, e);
                  Toast.makeText(MainActivity.this, "HTTP error " + code + ": " + message, Toast.LENGTH_LONG).show();
                  requestFuture = null;
               }
            };
            StringBuilder errbuf = new StringBuilder();
            requestFuture = requestor.request(method, uri, null, encoding, header, parameters,
                                              60, TimeUnit.SECONDS, 30, TimeUnit.SECONDS, fw, callback, token, errbuf);
            if (requestFuture == null)
               Toast.makeText(MainActivity.this, errbuf.toString(), Toast.LENGTH_LONG).show();
         }
      });

   }


}
