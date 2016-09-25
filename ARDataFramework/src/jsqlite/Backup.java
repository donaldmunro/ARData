package jsqlite;

/**
 * Class wrapping an SQLite backup object.
 */

public class Backup {

    /**
     * Internal handle for the native SQLite API.
     */

    protected long handle = 0;

    /**
     * Finish a backup.
     */

    protected void finish() throws SpatialiteException
    {
        synchronized (this) {
            _finalize();
        }
    }

    /**
     * Destructor for object.
     */

    protected void finalize() {
        synchronized (this) {
            try {
                _finalize();
            } catch (SpatialiteException e) {
            }
        }
    }

    protected native void _finalize() throws SpatialiteException;

    /**
     * Perform a backup step.
     *
     * @param n number of pages to backup
     * @return true when backup completed
     * @throws SpatialiteException  if something goes wrong.
     */

    public boolean step( int n ) throws SpatialiteException
    {
        synchronized (this) {
            return _step(n);
        }
    }

    private native boolean _step( int n ) throws SpatialiteException;

    /**
     * Perform the backup in one step.
     */
    public void backup() throws SpatialiteException
    {
        synchronized (this) {
            _step(-1);
        }
    }

    /**
     * Return number of remaining pages to be backed up.
     */

    public int remaining() throws SpatialiteException
    {
        synchronized (this) {
            return _remaining();
        }
    }

    private native int _remaining() throws SpatialiteException;

    /**
     * Return the total number of pages in the backup source database.
     */

    public int pagecount() throws SpatialiteException
    {
        synchronized (this) {
            return _pagecount();
        }
    }

    private native int _pagecount() throws SpatialiteException;

    /**
     * Internal native initializer.
     */

    private static native void internal_init();

    static {
        internal_init();
    }
}
