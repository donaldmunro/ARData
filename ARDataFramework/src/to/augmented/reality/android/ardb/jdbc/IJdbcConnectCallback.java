package to.augmented.reality.android.ardb.jdbc;

import to.augmented.reality.android.ardb.anything.Anything;

import java.sql.Connection;

public interface IJdbcConnectCallback
//=======================================
{
   void onConnected(Anything token, Connection connection);

   void onError(Anything token, CharSequence message, Throwable exception);
}
