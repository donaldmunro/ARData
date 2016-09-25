package to.augmented.reality.android.ardb.util;

public class TeaEncrypter
//========================
{
	private final int _key[];	// The 128 bit key.
	private byte _keyBytes[];	// original key as found
	private int _padding;		// amount of padding added in byte --> integer conversion.


	/**
	* Accepts key for enciphering/deciphering.
	*
	* @throws ArrayIndexOutOfBoundsException if the key isn't the correct length.
	*
	* @param key 128 bit (16 byte) key.
	*/
	public TeaEncrypter(final byte key[])
	{
		final int klen = key.length;
		_key = new int[4];

		// Incorrect key length throws exception.
		if (klen != 16)
			throw new ArrayIndexOutOfBoundsException(this.getClass().getName() + ": Key is not 16 bytes: " + klen);

		int j, i;
		for (i = 0, j = 0; j < klen; j += 4, i++)
			_key[i] = (key[j] << 24 ) | (((key[j+1])&0xff) << 16) | (((key[j+2])&0xff) << 8) | ((key[j+3])&0xff);

		_keyBytes = key;	// save for toString.
	}

	public TeaEncrypter(final int key[])
	{
		_key = key;
	}

	/**
	* Encipher two <code>int</code>s.
	* Replaces the original contents of the parameters with the results.
	* The integers are usually created from 8 bytes.
	* The usual way to collect bytes to the int array is:
	* <PRE>
	* byte ba[] = { .... };
	* int v[] = new int[2];
	* v[0] = (ba[j] << 24 ) | (((ba[j+1])&0xff) << 16) | (((ba[j+2])&0xff) << 8) | ((ba[j+3])&0xff);
	* v[1] = (ba[j+4] << 24 ) | (((ba[j+5])&0xff) << 16) | (((ba[j+6])&0xff) << 8) | ((ba[j+7])&0xff);
	* v = encipher(v);
	* </PRE>
	*
	* @param v two <code>int</code> array as input.
	*
	* @return array of two <code>int</code>s, enciphered.
	*/
	public int [] encipher(final int v[])
	{
		int y=v[0];
		int z=v[1];
		int sum=0;
		final int delta=0x9E3779B9;
		final int a=_key[0];
		final int b=_key[1];
		final int c=_key[2];
		final int d=_key[3];
		int n=32;

		while(n-- > 0)
		{
			sum += delta;
			y += (z << 4)+a ^ z + sum ^ (z >>> 5) + b;
			z += (y << 4)+c ^ y+ sum ^ (y >>> 5) + d;
		}

		v[0] = y;
		v[1] = z;

		return v;
	}

	/**
	* Decipher two <code>int</code>s.
	* Replaces the original contents of the parameters with the results.
	* The integers are usually decocted to 8 bytes.
	* The decoction of the <code>int</code>s to bytes can be done
	* this way.
	* <PRE>
	* int x[] = decipher(ins);
	* outb[j]   = (byte)(x[0] >>> 24);
	* outb[j+1] = (byte)(x[0] >>> 16);
	* outb[j+2] = (byte)(x[0] >>> 8);
	* outb[j+3] = (byte)(x[0]);
	* outb[j+4] = (byte)(x[1] >>> 24);
	* outb[j+5] = (byte)(x[1] >>> 16);
	* outb[j+6] = (byte)(x[1] >>> 8);
	* outb[j+7] = (byte)(x[1]);
	* </PRE>
	*
	* @param v <code>int</code> array of 2
	*
	* @return deciphered <code>int</code> array of 2
	*/
	public int [] decipher(final int v[])
	{
		int y=v[0];
		int z=v[1];
		int sum=0xC6EF3720;
		final int delta=0x9E3779B9;
		final int a=_key[0];
		final int b=_key[1];
		final int c=_key[2];
		final int d=_key[3];
		int n=32;

	   // sum = delta<<5, in general sum = delta * n

		while(n-- > 0)
		{
			z -= (y << 4)+c ^ y+sum ^ (y >>> 5) + d;
			y -= (z << 4)+a ^ z+sum ^ (z >>> 5) + b;
			sum -= delta;
		}

		v[0] = y;
		v[1] = z;

		return v;
	}


	/**
	* Byte wrapper for encoding.
	* Converts bytes to ints.
	* Padding will be added if required.
	*
	* @param b incoming <code>byte</code> array
	*
	* @param count
	*
	* @return integer conversion array, possibly with padding.
	*
	* @see #padding
	*/
	int [] encode(final byte b[], final int count)
	{
		int j ,i;
		int bLen = count;
		byte bp[] = b;

		_padding = bLen % 8;
		if (_padding != 0)	// Add some padding, if necessary.
		{
			_padding = 8 - (bLen % 8);
			bp = new byte[bLen + _padding];
			System.arraycopy(b, 0, bp, 0, bLen);
			bLen = bp.length;
		}

		final int intCount = bLen / 4;
		final int r[] = new int[2];
		final int out[] = new int[intCount];

		for (i = 0, j = 0; j < bLen; j += 8, i += 2)
		{
			// Java's unforgivable lack of unsigneds causes more bit
			// twiddling than this language really needs.
			r[0] = (bp[j] << 24 ) | (((bp[j+1])&0xff) << 16) | (((bp[j+2])&0xff) << 8) | ((bp[j+3])&0xff);
			r[1] = (bp[j+4] << 24 ) | (((bp[j+5])&0xff) << 16) | (((bp[j+6])&0xff) << 8) | ((bp[j+7])&0xff);
			encipher(r);
			out[i] = r[0];
			out[i+1] = r[1];
		}

		return out;
	}

	/**
	* Report how much padding was done in the last encode.
	*
	* @return bytes of padding added
	*/
	public int padding()
	{
		return _padding;
	}

	/**
	* Convert a byte array to ints and then decode.
	* There may be some padding at the end of the byte array from
	* the previous encode operation.
	*
	* @param b bytes to decode
	* @param count number of bytes in the array to decode
	*
	* @return <code>byte</code> array of decoded bytes.
	*/
	public byte [] decode(final byte b[], final int count)
	{
		int i, j;

		final int intCount = count / 4;
		final int ini[] = new int[intCount];
		for (i = 0, j = 0; i < intCount; i += 2, j += 8)
		{
			ini[i] = (b[j] << 24 ) | (((b[j+1])&0xff) << 16) | (((b[j+2])&0xff) << 8) | ((b[j+3])&0xff);
			ini[i+1] = (b[j+4] << 24 ) | (((b[j+5])&0xff) << 16) | (((b[j+6])&0xff) << 8) | ((b[j+7])&0xff);
		}
		return decode(ini);
	}

	/**
	* Decode an integer array.
	* There may be some padding at the end of the byte array from
	* the previous encode operation.
	*
	* @param b bytes to decode
	*
	* @return <code>byte</code> array of decoded bytes.
	*/
	public byte [] decode(final int b[])
	{
		// create the large number and start stripping ints out, two at a time.
		final int intCount = b.length;

		final byte outb[] = new byte[intCount * 4];
		final int tmp[] = new int[2];

		// decipher all the ints.
		int i, j;
		for (j = 0, i = 0; i < intCount; i += 2, j += 8)
		{
			tmp[0] = b[i];
			tmp[1] = b[i+1];
			decipher(tmp);
			outb[j]   = (byte)(tmp[0] >>> 24);
			outb[j+1] = (byte)(tmp[0] >>> 16);
			outb[j+2] = (byte)(tmp[0] >>> 8);
			outb[j+3] = (byte)(tmp[0]);
			outb[j+4] = (byte)(tmp[1] >>> 24);
			outb[j+5] = (byte)(tmp[1] >>> 16);
			outb[j+6] = (byte)(tmp[1] >>> 8);
			outb[j+7] = (byte)(tmp[1]);
		}

		return outb;
	}

	/**
	* Convert an array of ints into a hex string.
	*
	* @param enc Array of integers.
	* @return String hexadecimal representation of the integer array.
	* @throws ArrayIndexOutOfBoundsException if the array doesn't contain pairs of integers.
	*/
	public String binToHex(final int enc[]) throws ArrayIndexOutOfBoundsException
	{
		// The number of ints should always be a multiple of two as required by TEA (64 bits).
		if ((enc.length % 2)	== 1)
			throw new ArrayIndexOutOfBoundsException("Odd number of ints found: " + enc.length);

		final StringBuffer sb = new StringBuffer();
		final byte outb[] = new byte[8];
		final int tmp[] = new int[2];
		final int counter = enc.length / 2;

		for (int i = 0; i < enc.length; i += 2)
		{
			outb[0]   = (byte)(enc[i] >>> 24);
			outb[1] = (byte)(enc[i] >>> 16);
			outb[2] = (byte)(enc[i] >>> 8);
			outb[3] = (byte)(enc[i]);
			outb[4] = (byte)(enc[i+1] >>> 24);
			outb[5] = (byte)(enc[i+1] >>> 16);
			outb[6] = (byte)(enc[i+1] >>> 8);
			outb[7] = (byte)(enc[i+1]);

			sb.append(getHex(outb));
		}

		return sb.toString();
	}

   public static int binHexToBytes
           (final String sBinHex,
           final byte[] data,
           int nSrcOfs,
           int nDstOfs,
           int nLen)
   {
      // check for correct ranges
      final int nStrLen = sBinHex.length();

      final int nAvailBytes = (nStrLen - nSrcOfs) >> 1;
      if (nAvailBytes < nLen)
      {
         nLen = nAvailBytes;
      }

      final int nOutputCapacity = data.length - nDstOfs;
      if (nLen > nOutputCapacity)
      {
         nLen = nOutputCapacity;
      }

      // convert now

      final int nDstOfsBak = nDstOfs;

      for (int nI = 0; nI < nLen; nI++)
      {
         byte bActByte = 0;
         boolean blConvertOK = true;
         for (int nJ = 0; nJ < 2; nJ++)
         {
            bActByte <<= 4;
            final char cActChar = sBinHex.charAt(nSrcOfs++);

            if ((cActChar >= 'a') && (cActChar <= 'f'))
            {
               bActByte |= (byte)(cActChar - 'a') + 10;
            }
            else
            {
               if ((cActChar >= '0') && (cActChar <= '9'))
               {
                  bActByte |= (byte)(cActChar - '0');
               }
               else
               {
                  blConvertOK = false;
               }
            }
         }
         if (blConvertOK)
         {
            data[nDstOfs++] = bActByte;
         }
      }

      return (nDstOfs - nDstOfsBak);
   }

   public static int binHexToInt(final String sBinHex, final int data[],
                                 int nSrcOfs, int nDstOfs, int nLen)
  {
    // check for correct ranges
    final int nStrLen = sBinHex.length();

    final int nAvailBytes = (nStrLen - nSrcOfs) >> 1;
    if (nAvailBytes < nLen)
    {
      nLen = nAvailBytes;
    }

    final int nOutputCapacity = data.length - nDstOfs;
    if (nLen > nOutputCapacity)
    {
      nLen = nOutputCapacity;
    }

    // convert now

    final int nDstOfsBak = nDstOfs;

    for (int nI = 0; nI < nLen; nI++)
    {
      byte bActByte = 0;
      boolean blConvertOK = true;
      for (int nJ = 0; nJ < 2; nJ++)
      {
        bActByte <<= 4;
        final char cActChar = sBinHex.charAt(nSrcOfs++);

        if ((cActChar >= 'a') && (cActChar <= 'f'))
        {
          bActByte |= (byte)(cActChar - 'a') + 10;
        }
        else
        {
          if ((cActChar >= '0') && (cActChar <= '9'))
          {
            bActByte |= (byte)(cActChar - '0');
          }
          else
          {
            blConvertOK = false;
          }
        }
      }
      if (blConvertOK)
      {
        if (data != null)
         data[nDstOfs++] = bActByte;
      }
    }

    return (nDstOfs - nDstOfsBak);
  }


	/**
	* Display bytes in HEX.
	* @param b bytes to display.
	* @return string representation of the bytes.
	*/
	public String getHex(final byte b[])
	{
		final StringBuffer r = new StringBuffer();
		final char hex[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

		for (int i = 0; i < b.length; i++)
		{
			int c = ((b[i]) >>> 4) & 0xf;
			r.append(hex[c]);
			c = (b[i] & 0xf);
			r.append(hex[c]);
		}

		return r.toString();
	}

	/**
	* Pad a string out to the proper length with the given character.
	*
	* @param str Plain text string.
	* @param pc Padding character.
	*/
	public String padPlaintext(final String str, final char pc)
	{
		final StringBuffer sb = new StringBuffer(str);
		final int padding = sb.length() % 8;
		for (int i = 0; i < padding; i++)
			sb.append(pc);

		return sb.toString();
	}

	/**
	* Pad a string out to the proper length with spaces.
	*
	* @param str Plain text string.
	*/
	public String padPlaintext(final String str)
	{
		return padPlaintext(str, ' ');
	}

   public String encryptHex(String plainText)
   //----------------------------------------
   {
      plainText = padPlaintext(plainText);
      final byte plainTextBytes[] = plainText.getBytes();
      final int enc[] = encode(plainTextBytes, plainTextBytes.length);
      return binToHex(enc);
   }

   public String decryptHex(final String hexEncrypted)
   //-------------------------------------------
   {
      final byte[] bytes = new byte[1024];
      final int len = binHexToBytes(hexEncrypted, bytes, 0, 0, hexEncrypted.length());
      final byte[] plainText = decode(bytes, len);
      return new String(plainText);
   }

//   public static void main(String[] args)
//   {
//      byte TEAKEY[] = new BigInteger("367953f865f2a10a38cd9ca8393dfe9", 16).toByteArray();
//      Tea tea = new Tea(TEAKEY);
//      String encrypted = tea.encryptHex("!Dat@1234");
//      System.out.println(encrypted + " " +  tea.decryptHex(encrypted));
//   }
}
