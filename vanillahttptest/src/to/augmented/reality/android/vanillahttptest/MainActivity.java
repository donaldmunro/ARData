package to.augmented.reality.android.vanillahttptest;

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
   ActiveObject activeObject = new ActiveObject("VanillaHttpTest", 5);
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
            HttpRequestor requestor = SingletonHttpRequestorFactory.get().createHttpRequestor(activeObject);
            URI uri = null;
//               try { uri = new URI("http://developer.android.com/tools/devices/emulator.html"); } catch (Exception e) { Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show(); }
//            try { uri = new URI("http://128.30.52.100//Protocols/rfc2616/rfc2616-sec14.html"); } catch (Exception e) { Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show(); }
            try { uri = new URI("http://game4fun.square7.ch"); } catch (Exception e) { Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show(); }
            Map<String, String> header = new HashMap<>();
            Anything parameters = new Anything();
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
               }

               @Override
               public void onError(Anything token, int code, String message)
               {
                  Log.e("MainActivity", code + ":" + message);
                  Toast.makeText(MainActivity.this, "HTTP error " + code + ": " + message, Toast.LENGTH_LONG).show();
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
