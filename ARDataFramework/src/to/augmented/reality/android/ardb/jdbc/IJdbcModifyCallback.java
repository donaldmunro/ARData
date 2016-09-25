package to.augmented.reality.android.ardb.jdbc;

import to.augmented.reality.android.ardb.anything.Anything;

import java.util.Map;

public interface IJdbcModifyCallback
//=======================================
{
   void onResponse(Anything token, int no, Map<String, Object> params, Map<String, int[]> paramIndices);

   void onError(Anything token, CharSequence message, Throwable exception);
}
