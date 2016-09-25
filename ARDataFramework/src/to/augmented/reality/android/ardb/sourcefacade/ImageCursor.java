package to.augmented.reality.android.ardb.sourcefacade;

import android.database.AbstractCursor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ImageCursor extends AbstractCursor
//=============================================
{
   String columnName;
   byte[] data;
   File dataFile;

   public ImageCursor(String columnName, byte[] data) { this.columnName = columnName; this.data = data; }

   public ImageCursor(String columnName, File dataFile) throws FileNotFoundException
   //--------------------------------------------------------------------------------
   {
      if ( (! dataFile.exists()) || (! dataFile.isFile()) || (! dataFile.canRead()) )
         throw new FileNotFoundException();
      this.columnName = columnName;
      this.dataFile = dataFile;
   }

   @Override public byte[] getBlob(int column)
   //-----------------------------------------
   {
      if (column != 0)
         return null;
      if (data != null)
         return data;
      else if ( (dataFile != null) && (dataFile.exists()) )
      {
         long len = dataFile.length();
         BufferedInputStream bis = null;
         try
         {
            bis = new BufferedInputStream(new FileInputStream(dataFile), 32768);
            byte[] data = new byte[(int) len];
            bis.read(data);
            return data;
         }
         catch (Exception e)
         {
            return null;
         }
         finally
         {
            if (bis != null)
               try { bis.close(); } catch (Exception _e) {}
         }
      }
      return null;
   }



   @Override
   public int getType(int column)
   //---------------------------
   {
      if (data != null)
         return FIELD_TYPE_BLOB;
      else if (dataFile != null)
         return FIELD_TYPE_STRING;
      else
         return FIELD_TYPE_NULL;
   }

   public InputStream getBlobStream(int column)
   //------------------------------------------
   {
      if (column != 0)
         return null;
      if (data != null)
         return new ByteArrayInputStream(data);
      else if ( (dataFile != null) && (dataFile.exists()) )
      {
         try { return new BufferedInputStream(new FileInputStream(dataFile), 32768); } catch (FileNotFoundException e) { return null; }
      }
      return null;
   }

   @Override public int getCount() { return 1; }

   @Override public String[] getColumnNames() { return new String[] { columnName }; }

   @Override public String getString(int column) { return dataFile.getAbsolutePath(); }

   @Override public short getShort(int column) { return 0; }

   @Override public int getInt(int column) { return 0; }

   @Override public long getLong(int column) { return 0; }

   @Override public float getFloat(int column) { return 0; }

   @Override public double getDouble(int column) { return 0; }

   @Override public boolean isNull(int column)
   //-----------------------------------------
   {
      if (column == 0)
         return false;
      else
         return true;
   }

}
