package to.augmented.reality.android.ardb;

import org.junit.Test;
import to.augmented.reality.android.ardb.concurrency.ActiveObject;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ActiveObjectTest
//===========================
{
   @Test
   public void testSchedule() throws Exception
   //------------------------------------------
   {
      final Semaphore semaphore = new Semaphore(3);
      semaphore.acquire(3);
      ActiveObject instance = new ActiveObject("testSchedule", 2);
      instance.schedule(new TestThread(semaphore, "1"));
      instance.schedule(new TestThread(semaphore,"2"));
      instance.schedule(new TestThread(semaphore,"3"));
      assertTrue(semaphore.tryAcquire(3, 10, TimeUnit.SECONDS));
   }

   @Test
   public void testScheduleWithFuture() throws Exception
   //---------------------------------------------------
   {
      class TestFuture implements Callable<String>
      //==========================================
      {
         final String name;
         final Semaphore semaphore;

         public TestFuture(Semaphore semaphore, String name) { this.semaphore = semaphore; this.name = name; }

         @Override public String call() throws Exception
         {
            semaphore.release();
            System.out.println("Thread " + name + " completed");
            return name;
         }
      }
      final Semaphore semaphore = new Semaphore(5);
      semaphore.acquire(5);
      ActiveObject instance = new ActiveObject("testSchedule", 3);
      Future<String> one = (Future<String>) instance.scheduleWithFuture(new TestFuture(semaphore, "1"));
      assertNotNull(one);
      Future<String> two = (Future<String>) instance.scheduleWithFuture(new TestFuture(semaphore, "2"));
      assertNotNull(two);
      Future<String> three = (Future<String>) instance.scheduleWithFuture(new TestFuture(semaphore, "3"));
      assertNotNull(three);
      Future<String> four = (Future<String>) instance.scheduleWithFuture(new TestFuture(semaphore, "4"));
      assertNotNull(four);
      Future<String> five = (Future<String>) instance.scheduleWithFuture(new TestFuture(semaphore, "5"));
      assertNotNull(five);
      assertTrue(semaphore.tryAcquire(5, 300, TimeUnit.SECONDS));
      assertEquals("1", one.get());
      assertEquals("2", two.get());
      assertEquals("3", three.get());
      assertEquals("4", four.get());
      assertEquals("5", five.get());
   }

   class TestThread implements Callable<Void>
   //=========================================
   {
      final String name;
      final Semaphore semaphore;

      public TestThread(Semaphore semaphore, String name) { this.semaphore = semaphore; this.name = name; }

      @Override public Void call() throws Exception
      {
         System.out.println(name + " release");
         semaphore.release();
         return null;
      }

      @Override
      public String toString() { return "TestThread " + name;  }
   }
}
