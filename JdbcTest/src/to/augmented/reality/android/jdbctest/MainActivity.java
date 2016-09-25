package to.augmented.reality.android.jdbctest;

import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;
import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.jdbc.IJdbcConnectCallback;
import to.augmented.reality.android.ardb.spi.ICursorQueryCallback;
import to.augmented.reality.android.ardb.jdbc.IJdbcRequestor;
import to.augmented.reality.android.ardb.jdbc.PostgresRequestor;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity
//=================================================
{
   ActiveObject activeObject = null;
   Future<Connection> connectionThread = null;
   Connection connection = null;
   IJdbcRequestor requestor = null;
   private EditText editQuery;
   private TextView textResults;

   IJdbcConnectCallback callback = new IJdbcConnectCallback()
   {
      @Override
      public void onConnected(Anything token, Connection conn)
      //--------------------------------------------------------------
      {
         String type = token.getAsString("type", "");
         connection = conn;
         if (connection != null)
         {
            MainActivity.this.runOnUiThread(new Runnable()
            {
               public void run() { Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show(); }
            });

            String sql = editQuery.getText().toString();
            if (! sql.trim().isEmpty())
               requestor.query(conn, sql, 1, TimeUnit.MINUTES, null, null, queryCallback, null);
         }
      }

      @Override
      public void onError(Anything token, CharSequence message, Throwable exception)
      {
         String type = token.getAsString("type", "");
         switch (type)
         {
            case "connection":
               MainActivity.this.runOnUiThread(new Runnable()
               {
                  public void run() { Toast.makeText(MainActivity.this, "Connection Error", Toast.LENGTH_LONG).show(); }
               });
               break;
         }
      }
   };

   ICursorQueryCallback queryCallback = new ICursorQueryCallback()
   {
      @Override
      public void onJdbcQueried(Connection conn, Anything token, Cursor cursor, Map<String, Object> params,
                                Map<String, int[]> paramIndices)
      //---------------------------------------------------------------------------------------------------------------
      {
         final StringBuilder results = new StringBuilder();
         int ccols = cursor.getColumnCount();
         String[] headings = cursor.getColumnNames();
         for (String heading : headings)
            results.append(heading).append(getString(R.string.tab));
         results.append("\n");
         int row = 0;
         while (cursor.moveToNext())
         {
            if (row++ > 20) break;
            for (int col=0; col<cursor.getColumnCount(); col++)
            {
               switch (cursor.getType(col))
               {
                  case Cursor.FIELD_TYPE_STRING:
                     results.append(cursor.getString(col)).append(getString(R.string.tab));
                     break;
                  case Cursor.FIELD_TYPE_INTEGER:
                     results.append(cursor.getInt(col)).append(getString(R.string.tab));
                     break;
                  case Cursor.FIELD_TYPE_FLOAT:
                     double d = cursor.getDouble(col);
                     results.append(String.format("%8.2f", d)).append(getString(R.string.tab));
                     break;
                  case Cursor.FIELD_TYPE_NULL:
                     results.append("NULL").append(getString(R.string.tab));
                     break;
                  case Cursor.FIELD_TYPE_BLOB:
                     byte[] ab = cursor.getBlob(col);
                     byte[] abc = Arrays.copyOf(ab, Math.min(ab.length, 10));
                     results.append(new String(abc)).append(getString(R.string.tab));
                     break;
               }
            }
            results.append("\n");
         }

         MainActivity.this.runOnUiThread(new Runnable()
         //============================================
         {
            public void run()
            //---------------
            {
               textResults.setText("");
               textResults.setText(results.toString());
            }
         });
      }

      @Override
      public void onError(Connection conn, final Anything token, final CharSequence message, final Throwable exception)
      //----------------------------------------------------------------------------------------------
      {
         MainActivity.this.runOnUiThread(new Runnable()
         {
            public void run() { Toast.makeText(MainActivity.this, "Query error: " + message, Toast.LENGTH_LONG).show(); }
         });
      }
   };

   @Override
   protected void onCreate(Bundle savedInstanceState)
   //------------------------------------------------
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      final Spinner spinnerDatabase = (Spinner) findViewById(R.id.spinnerDatabase);
      final EditText editHost = (EditText) findViewById(R.id.editHost);
      final EditText editName = (EditText) findViewById(R.id.editName);
      final EditText editUser = (EditText) findViewById(R.id.editUser);
      final EditText editPassword = (EditText) findViewById(R.id.editPassword);
      final EditText editConnTimeout = (EditText) findViewById(R.id.editConnTimeout);
      final EditText editReadTimeout = (EditText) findViewById(R.id.editReadTimeout);
      editQuery = (EditText) findViewById(R.id.editQuery);
      textResults = (TextView) findViewById(R.id.textResults);
      final Button buttonQuery = (Button) findViewById(R.id.buttonExecute);

      if (isEmulator())
         editHost.setText("10.0.2.2");
      buttonQuery.setOnClickListener(new View.OnClickListener()
      //=======================================================
      {
         @Override public void onClick(View v)
         //-----------------------------------
         {
            String database = (String) spinnerDatabase.getSelectedItem();
            String host = editHost.getText().toString();
            if (host.trim().isEmpty())
            {
               if (isEmulator())
                  host = "10.0.2.2";
               else
               {
                  Toast.makeText(MainActivity.this, "Enter host", Toast.LENGTH_LONG).show();
                  return;
               }
            }
            String name = editName.getText().toString();
            String user = editUser.getText().toString();
            String password = editPassword.getText().toString();
            int connectTimeout = 30, readTimeout = 15;
            String s = editConnTimeout.getText().toString().trim();
            if (! s.isEmpty())
               try { connectTimeout = Integer.parseInt(s); } catch (Exception e) { connectTimeout = 30; }
            s = editReadTimeout.getText().toString().trim();
            if (! s.isEmpty())
               try { readTimeout = Integer.parseInt(s); } catch (Exception e) { readTimeout = 15; }
            final int port;
            switch (database)
            {
               case "PostGres":
                  port = IJdbcRequestor.POSTGRES_TCP_PORT;
                  if (activeObject == null)
                     activeObject = new ActiveObject("JDBC Test", 5);
                  requestor = new PostgresRequestor(MainActivity.this, activeObject);
                  break;
               case "SQL Server":
                  port = IJdbcRequestor.SQLSERVER_TCP_PORT;
                  if (activeObject == null)
                     activeObject = new ActiveObject("JDBC Test", 5);
                  requestor = null;
                  break;
               default: throw new RuntimeException("Unsupported databaseName");
            }
            Anything connectToken = new Anything(); connectToken.put("type", "connection");
            if (requestor != null)
               connectionThread = requestor.connect(host, port, name, user, password,
                                                    connectTimeout, TimeUnit.SECONDS,
                                                    readTimeout, TimeUnit.SECONDS, false,
                                                    callback, connectToken, null);
         }
      });

   }


   public void onQuery(View view)
   //----------------------------
   {
   }

   public static boolean isEmulator()
   //--------------------------------
   {
      int rating = 0;

      if (Build.PRODUCT.equals("sdk") ||
            Build.PRODUCT.equals("google_sdk") ||
            Build.PRODUCT.equals("sdk_x86") ||
            Build.PRODUCT.equals("vbox86p"))
         rating++;

      if (Build.MANUFACTURER.equals("unknown") ||
            Build.MANUFACTURER.equals("Genymotion"))
         rating++;

      if (Build.BRAND.equals("generic") ||
            Build.BRAND.equals("generic_x86"))
         rating++;

      if (Build.DEVICE.equals("generic") ||
            Build.DEVICE.equals("generic_x86") ||
            Build.DEVICE.equals("vbox86p"))
         rating++;

      if (Build.MODEL.equals("sdk") ||
            Build.MODEL.equals("google_sdk") ||
            Build.MODEL.equals("Android SDK built for x86"))
         rating++;

      if (Build.HARDWARE.equals("goldfish") ||
            Build.HARDWARE.equals("vbox86"))
         rating++;

      if (Build.FINGERPRINT.contains("generic/sdk/generic") ||
            Build.FINGERPRINT.contains("generic_x86/sdk_x86/generic_x86") ||
            Build.FINGERPRINT.contains("generic/google_sdk/generic") ||
            Build.FINGERPRINT.contains("generic/vbox86p/vbox86p"))
         rating++;

      return rating > 4;
   }
}
