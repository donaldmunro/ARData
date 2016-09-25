package to.augmented.reality.android.ardb.concurrency;

abstract public interface StoppableRunnable extends Runnable
//================================================
{
   public void stop();

   public boolean isStopped();
}
