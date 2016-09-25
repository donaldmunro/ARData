package to.augmented.reality.android.ardb.http;


public class RequestorConfiguration
//=================================
{
   private static final class SingletonHolder
   {
      static RequestorConfiguration singleton = new RequestorConfiguration();
   }

   public static RequestorConfiguration get()
   //---------------------------------------
   {
      if (SingletonHolder.singleton == null)
         SingletonHolder.singleton = new RequestorConfiguration();
      return SingletonHolder.singleton;
   }




}
