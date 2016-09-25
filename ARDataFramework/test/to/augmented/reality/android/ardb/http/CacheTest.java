package to.augmented.reality.android.ardb.http;

import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;

import static org.junit.Assert.*;

public class CacheTest
{

   @Test
   public void testPurgeAged() throws Exception
   {
      File dir = new File("cache_test");
      if (dir.isDirectory())
         HttoRequestHandlerTest.recursiveDelete(dir);
      dir.mkdirs();
      Cache instance = new Cache(dir);
      File f = new File(dir, "1.prop");
      PrintWriter pw = new PrintWriter(f);
      pw.println("Cache properties");
      pw.close();
      f = new File(dir,"1.cache");
      pw = new PrintWriter(f);
      pw.println("Cache properties");
      pw.close();
      f = new File(dir,"2.prop");
      pw = new PrintWriter(f);
      pw.println("Cache 2 properties");
      pw.close();
      f = new File(dir,"2.cache");
      pw = new PrintWriter(f);
      pw.println("Cache 2 properties");
      pw.close();

      instance.purgeAged();
      assertFalse(new File("1.prop").exists());
      assertFalse(new File("1.cache").exists());
      assertFalse(new File("2.prop").exists());
      assertFalse(new File("2.cache").exists());
   }
}