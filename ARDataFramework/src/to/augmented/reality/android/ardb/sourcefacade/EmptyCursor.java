package to.augmented.reality.android.ardb.sourcefacade;

import android.database.AbstractCursor;

public class EmptyCursor extends AbstractCursor
//=============================================
{
   @Override public int getCount() { return 0; }

   @Override public String[] getColumnNames() { return new String[0]; }

   @Override public String getString(int column) { throw new IndexOutOfBoundsException(); }

   @Override public short getShort(int column) { throw new IndexOutOfBoundsException(); }

   @Override public int getInt(int column) { throw new IndexOutOfBoundsException(); }

   @Override public long getLong(int column) { throw new IndexOutOfBoundsException(); }

   @Override public float getFloat(int column) { throw new IndexOutOfBoundsException(); }

   @Override public double getDouble(int column) { throw new IndexOutOfBoundsException(); }

   @Override public boolean isNull(int column) { throw new IndexOutOfBoundsException(); }
}
