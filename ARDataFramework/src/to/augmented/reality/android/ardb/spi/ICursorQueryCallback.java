package to.augmented.reality.android.ardb.spi;

import android.database.Cursor;
import to.augmented.reality.android.ardb.anything.Anything;

import java.sql.Connection;
import java.util.Map;

public interface ICursorQueryCallback
//=======================================
{
   void onQueried(Anything token, Cursor cursor, int retcode);

   void onError(Anything token, int code, CharSequence message, Throwable exception);

   void onJdbcQueried(Connection connection, Anything token, Cursor cursor, Map<String, Object> params,
                      Map<String, int[]> paramIndices);

   /**
    * Called when a query error occurs.
    * @param connection The JDBC connection on which the query was made. Can be null.
    * @param token The identifying token that was passed to the request
    * @param message A message describing the error
    * @param exception Contains an exception if one occurred. Can be null.
    */
   void onJdbcError(Connection connection, Anything token, CharSequence message, Throwable exception);
}
