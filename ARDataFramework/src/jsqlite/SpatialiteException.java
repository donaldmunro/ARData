package jsqlite;

/**
 * Class for SQLite related exceptions.
 */

public class SpatialiteException extends java.lang.Exception {

    /**
     * Construct a new SQLite exception.
     *
     * @param string error message
     */

    public SpatialiteException(String string) {
	super(string);
    }
}
