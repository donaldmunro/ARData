package to.augmented.reality.android.ardb.sourcefacade;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class DataPoint
//====================
{
   final static public int FLOAT_BYTESIZE = Float.SIZE/Byte.SIZE;
   final static public int DOUBLE_BYTESIZE = Double.SIZE/Byte.SIZE;
   final static public int LONG_BYTESIZE = Long.SIZE/Byte.SIZE;
   final static public int INT_BYTESIZE = Integer.SIZE/Byte.SIZE;

   private int dimension;
   private int[] sizes;
   private DataType[] types;
   private ByteBuffer buffer = null;

   public DataPoint(DataType... types)
   //-------------------------------
   {
      dimension = types.length;
      this.types = new DataType[dimension];
      sizes = new int[dimension];
      int i = 0;
      for (DataType type : types)
         this.types[i++] = type;
   }

   public DataPoint(DataType[] types, Object[] values)
   //-------------------------------
   {
      dimension = types.length;
      this.types = new DataType[dimension];
      sizes = new int[dimension];
      int i = 0;
      for (DataType type : types)
         this.types[i++] = type;
      set(values);
   }

   public void set(Object... values)
   //------------------------------
   {
      if (buffer != null)
         buffer = null;
      int size = 0;
      for (int i=0; i<types.length; i++)
      {
         switch (types[i])
         {
            case WKT:
               if (values[i] instanceof Geometry)
               {
                  Geometry geom = (Geometry) values[i];
                  values[i] = geom.toString();
               }
            case STRING:   sizes[i] = ((String) values[i]).length(); break;
            case INT:      sizes[i] = INT_BYTESIZE; break;
            case LONG:     sizes[i] = LONG_BYTESIZE; break;
            case FLOAT:    sizes[i] = FLOAT_BYTESIZE; break;
            case DOUBLE:   sizes[i] =DOUBLE_BYTESIZE; break;
            case IMAGE:
            case BLOB:     sizes[i] = ((byte[]) values[i]).length; break;
            case URI:
               try
               {
                  URI uri = (URI) values[i];
                  sizes[i] = uri.toString().getBytes().length;
               }
               catch (ClassCastException e)
               {
                  Uri auri = (Uri) values[i];
                  sizes[i] = auri.toString().getBytes().length;
               }
               break;
         }
         size += sizes[i];
      }
      buffer = ByteBuffer.allocate(size);
      buffer.rewind();
      for (int i=0; i<types.length; i++)
      {
         switch (types[i])
         {
            case WKT:
            case STRING:   buffer.put(((String) values[i]).getBytes()); break;
            case INT:      buffer.putInt((Integer) values[i]); break;
            case LONG:     buffer.putLong((Long) values[i]); break;
            case FLOAT:    buffer.putFloat((Float) values[i]); break;
            case DOUBLE:   buffer.putDouble((Double) values[i]); break;
            case IMAGE:
            case BLOB:     buffer.put((byte[]) values[i]); break;
            case URI:
               try
               {
                  URI uri = (URI) values[i];
                  buffer.put(uri.toString().getBytes());
               }
               catch (ClassCastException e)
               {
                  Uri auri = (Uri) values[i];
                  buffer.put(auri.toString().getBytes());
               }
               break;
         }
      }
      buffer.rewind();
   }

   protected int position(int dim)
   //-----------------------------
   {
      int pos = 0;
      for (int i=0; i<dim; i++)
         pos += sizes[i];
      buffer.position(pos);
      return pos;
   }

   public byte[] asBytes(int dim)
   //----------------------------
   {
      int bits;
      long lbits;
      switch (types[dim])
      {
         case BLOB:
         case IMAGE:
            position(dim);
            byte[] bytes = new byte[sizes[dim]];
            buffer.get(bytes);
            return bytes;
         case WKT:
         case STRING:
            return asString(dim).getBytes();
         case FLOAT:
            bits = Float.floatToIntBits(Float.valueOf(asFloat(dim)));
            return ByteBuffer.allocate(FLOAT_BYTESIZE).putInt(bits).array();
         case DOUBLE:
            lbits = Double.doubleToLongBits(Double.valueOf(asDouble(dim)));
            return ByteBuffer.allocate(DOUBLE_BYTESIZE).putLong(lbits).array();
         case INT:
            return ByteBuffer.allocate(INT_BYTESIZE).putInt(asInt(dim)).array();
         case LONG:
            return ByteBuffer.allocate(LONG_BYTESIZE).putLong(asLong(dim)).array();
         case URI:
            URI uri;
            try
            {
               uri = asURI(dim);
               return uri.toString().getBytes();
            }
            catch (URISyntaxException e)
            {
               return asUri(dim).toString().getBytes();
            }
      }
      throw new RuntimeException("asBytes: Unknown type");
   }

   public String asString(int dim)
   //-----------------------------
   {
      switch (types[dim])
      {
         case WKT:
         case STRING:   return new String(asBytes(dim));
         case INT:      return Integer.toString(asInt(dim));
         case LONG:     return Long.toString(asLong(dim));
         case FLOAT:    return Float.toString(asFloat(dim));
         case DOUBLE:   return Double.toString(asDouble(dim));
         case URI:
            try { return asURI(dim).toString(); } catch (URISyntaxException e) { return asUri(dim).toString(); }
         case BLOB:
         case IMAGE:
            return bytesToHex(asBytes(dim));
      }
      return null;
   }

   public Geometry asGeometry(int dim) throws ParseException
   //-------------------------------------------------------
   {
      if ( (types[dim] == DataType.WKT) || (types[dim] == DataType.STRING) )
      {
         WKTReader wktReader = new WKTReader();
         return wktReader.read(asString(dim));
      }
      return null;
   }

   public int asInt(int dim) { position(dim); return buffer.getInt(); }

   public long asLong(int dim) { position(dim); return buffer.getLong(); }

   public float asFloat(int dim) { position(dim); return buffer.getFloat(); }

   public double asDouble(int dim) { position(dim); return buffer.getDouble(); }

   public byte[] asBlob(int dim) { return asBytes(dim); }

   public URI asURI(int dim) throws URISyntaxException { return new URI(asString(dim)); }

   public Uri asUri(int dim) { return Uri.parse(asString(dim)); }

   public Bitmap asImage(int dim) { return BitmapFactory.decodeByteArray(asBytes(dim), 0, 0); }

   public Object[] get()
   //------------------
   {
      Object[] values = new Object[dimension];
      byte[] vb;
      buffer.rewind();
      for (int i=0; i<types.length; i++)
      {
         switch (types[i])
         {
            case WKT:
               try { values[i] = asGeometry(i); break; } catch (ParseException e) {}
            case STRING:
               vb = new byte[sizes[i]];
               buffer.get(vb);
               values[i] = new String(vb);
               break;
            case INT:      values[i] = buffer.getInt(); break;
            case LONG:     values[i] = buffer.getLong(); break;
            case FLOAT:    values[i] = buffer.getFloat(); break;
            case DOUBLE:   values[i] = buffer.getDouble(); break;
            case IMAGE:
            case BLOB:
               vb = new byte[sizes[i]];
               buffer.get(vb);
               values[i] = vb;
               break;
            case URI:
               vb = new byte[sizes[i]];
               buffer.get(vb);
               String s = new String(vb);
               try
               {
                  values[i] = new URI(s);
               }
               catch (URISyntaxException e)
               {
                  values[i] = Uri.parse(s);
               }
               break;
         }
      }
      return values;
   }

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

   public static String bytesToHex(byte[] bytes)
   //-------------------------------------------
   {
      char[] hexChars = new char[bytes.length * 2];
      for (int i = 0; i < bytes.length; i++)
      {
         int v = bytes[i] & 0xFF;
         hexChars[i * 2] = hexArray[v >>> 4];
         hexChars[i * 2 + 1] = hexArray[v & 0x0F];
      }
      return new String(hexChars);
   }
}
