package to.augmented.reality.android.ardb.http;

import to.augmented.reality.android.ardb.anything.Anything;

public interface IHttpRequestorCallback
//========================================
{
   void onResponse(Anything token, int code);

   void onError(Anything token, int code, CharSequence message, Throwable exception);
}
