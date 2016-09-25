package to.augmented.reality.android.ardb.concurrency;

import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ActiveObject
//=======================
{
   private static final String TAG = ActiveObject.class.getSimpleName();

   private AtomicLong idGenerator = new AtomicLong(0);

   final private ExecutorService schedulerExecutor = Executors.newSingleThreadExecutor();
   private Future<?> scheduler;

   private ExecutorService threadExecutor;

   volatile private boolean isRunning = true;

   final private BlockingQueue<Callable<?>> queue = new LinkedBlockingQueue<>();
   final private Servant servant = new Servant();

   public ActiveObject(final String name, final int noConcurrentThreads)
   //------------------------------------------------------------------
   {
      final AtomicLong count = new AtomicLong(1);
      ThreadFactory tf = new ThreadFactory()
      //====================================
      {
         @Override
         public Thread newThread(Runnable r)
         //---------------------------------
         {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(String.format("%s-%d", name, count.getAndIncrement()));
            return t;
         }
      };
      threadExecutor = Executors.newFixedThreadPool(noConcurrentThreads, tf);
      scheduler = schedulerExecutor.submit(servant);
   }

   public void schedule(Callable<Void> r) { queue.offer(r); }

   public Future<?> scheduleWithFuture(Callable<?> r)
   //---------------------------------------------
   {
      IdableThread t;
      try { t = new IdableThread(r); } catch (InterruptedException e) { return null; }
      queue.offer(t);
      try { t.getMutex().acquire(1); } catch (InterruptedException e) { return null; }
      return servant.removeFuture(t.getId());
   }

   class Servant implements Runnable
   //==================================
   {
//      ConcurrentHashMap<SoftReference<Long>, SoftReference<Future<?>>> futures = new ConcurrentHashMap<>();
//      public Future<?> removeFuture(long id)
//      //---------------------------------
//      {
//         Future<?> future = null;
//         SoftReference<Long> sid = new SoftReference<Long>(id);
//         SoftReference<Future<?>> v = futures.get(sid);
//         if (v != null)
//            future = v.get();
//         if (future == null)
//            futures.remove(sid);
//         return future;
//      }

      Map<Long, Future<?>> futures = Collections.synchronizedMap(new HashMap<Long, Future<?>>());
      public Future<?> removeFuture(long id) { return futures.remove(id); }

      @Override
      public void run()
      //---------------
      {
         isRunning = true;
         Callable<?> r;
         Future<?> f = null;
         long id;
         while (isRunning)
         {
            try
            {
               try { r = queue.poll(200, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { return; }
               if (! isRunning)
                  break;
               if (r == null)
                  continue;
               if (r instanceof IdableThread)
               {
                  IdableThread t = (IdableThread) r;
                  id = t.getId();
                  f = threadExecutor.submit(t.getCallable());
//                  SoftReference<Long> k = new SoftReference<Long>(id);
//                  SoftReference<Future<?>> v = new SoftReference<Future<?>>(f);
                  futures.put(id, f);
                  t.getMutex().release(1);
               }
               else
                  threadExecutor.submit(r);
            }
            catch (Exception ee)
            {
               Log.e(TAG, "", ee);
            }

         }
      }
   }

   class IdableThread implements Callable
   //====================================
   {
      long id = -1;
      public long getId() { return id; }

      Callable<?> r = null;
      public Callable<?> getCallable() { return r; }

      final Semaphore mutex;
      public Semaphore getMutex() { return mutex; }

      public IdableThread(Callable<?> r) throws InterruptedException
      //------------------------------------------------------------
      {
         mutex = new Semaphore(1, true);
         mutex.acquire(1);
         id = idGenerator.getAndIncrement();
         this.r = r;
      }

      @Override
      public Object call() throws Exception
      //-----------------------------------
      {
         if (r == null)
         {
            Log.e(TAG, "Runnable was null");
            throw new RuntimeException("Runnable was null");
         }
         return (Object) r.call();
      }

   }

   public void stop()
   //----------------
   {
      List<Runnable> threads = schedulerExecutor.shutdownNow();
      int cstoppable = 0;
      for (Runnable t : threads)
      {
         if (t instanceof StoppableRunnable)
         {
            ((StoppableRunnable) t).stop();
            cstoppable++;
         }
      }
      if (cstoppable > 0)
         try { Thread.sleep(200); } catch (Exception e) {}
      isRunning = false;
      try { Thread.sleep(200); } catch (Exception e) {}
      try { scheduler.get(500, TimeUnit.MILLISECONDS); } catch (Exception e) {}
      if (! scheduler.isDone())
      {
         scheduler.cancel(true);
         try { Thread.sleep(200); } catch (Exception e) {}
         if (! scheduler.isDone())
            schedulerExecutor.shutdownNow();
      }
   }
}
