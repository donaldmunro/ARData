package to.augmented.reality.android.ardb.sourcefacade;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class DataPointTest
//==========================
{
   @Test
   public void testSet() throws Exception
   //-------------------------------------
   {
      DataPoint instance = new DataPoint(DataType.STRING, DataType.INT, DataType.LONG, DataType.FLOAT, DataType.DOUBLE, DataType.URI, DataType.BLOB);
      URI uri = new URI("http://localhost:8080");
      byte[] blob = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
      instance.set("Test", 42, 42L, 42.0f, 42.0, uri, blob);
      assertEquals("Test",instance.asString(0));
      assertEquals(42, instance.asInt(1));
      assertEquals(42L, instance.asLong(2));
      assertEquals(42.0f, instance.asFloat(3), 0.0001f);
      assertEquals(42.0, instance.asDouble(4), 0.0001);
      assertEquals(uri, instance.asURI(5));
      byte[] ab = instance.asBytes(6);
      assertEquals(blob.length, ab.length);
      for (int i=0; i<blob.length; i++)
         assertEquals(blob[i], ab[i]);
   }

   @Test
   public void testGet() throws Exception
   //------------------------------------
   {
      DataPoint instance = new DataPoint(DataType.STRING, DataType.INT, DataType.LONG, DataType.FLOAT, DataType.DOUBLE, DataType.URI, DataType.BLOB);
      URI uri = new URI("http://localhost:8080");
      byte[] blob = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
      instance.set("Test", 42, 42L, 42.0f, 42.0, uri, blob);
      Object[] values = instance.get();
      assertTrue(values[0] instanceof String);
      assertEquals("Test", values[0]);
      assertTrue(values[1] instanceof Integer);
      assertEquals(42, values[1]);
      assertTrue(values[2] instanceof Long);
      assertEquals(42L, values[2]);
      assertTrue(values[3] instanceof Float);
      assertEquals(42.0f, (Float) values[3], 0.0001f);
      assertTrue(values[4] instanceof Double);
      assertEquals(42.0, (Double) values[4], 0.0001);
      assertTrue(values[5] instanceof URI);
      assertEquals(uri, values[5]);
      byte[] ab = (byte[]) values[6];
      assertEquals(blob.length, ab.length);
      for (int i=0; i<blob.length; i++)
         assertEquals(blob[i], ab[i]);
   }
}
