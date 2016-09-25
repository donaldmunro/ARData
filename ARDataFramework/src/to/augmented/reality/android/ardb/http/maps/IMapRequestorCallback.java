package to.augmented.reality.android.ardb.http.maps;

import to.augmented.reality.android.ardb.anything.Anything;
import to.augmented.reality.android.ardb.http.MIME_TYPES;

import java.io.File;

public interface IMapRequestorCallback
//====================================
{
   void onMapBitmap(Anything token, int code, byte[] imageData, MIME_TYPES mimeType);

   void onMapFile(Anything token, int code, File f);

   void onError(Anything token, int code, CharSequence message, Throwable exception);
}
