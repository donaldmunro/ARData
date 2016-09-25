package to.augmented.reality.android.ardb.jdbc;

import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.anything.ImmutableAnything;

public interface IJdbcRequestorCallback
//=======================================
{
   void onQueried(Anything token, ImmutableAnything result);

   void onError(Anything token, int code, CharSequence message, Throwable exception);
}
