package to.augmented.reality.android.ardb.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class NoFuture<T> implements Future<T>
//==========================================
{
   T t;

   public NoFuture(T t) { this.t = t; }

   @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }

   @Override public boolean isCancelled() { return false; }

   @Override public boolean isDone() { return true; }

   @Override
   public T get() throws InterruptedException, ExecutionException { return t; }

   @Override
   public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
   {
      return t;
   }
}
