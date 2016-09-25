package to.augmented.reality.android.ardb.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Encrypter
//=======================
{
   private byte[] hash;
   public byte[] getHash() { return hash; }

   private String hexHash;
   public String getHexHash() { return hexHash; }

   public Md5Encrypter(String s) throws NoSuchAlgorithmException { encrypt(s); }

   public void encrypt(String s) throws NoSuchAlgorithmException
   //-----------------------------------------------------------
   {
      MessageDigest m = MessageDigest.getInstance("MD5");
      m.update(s.getBytes(),0,s.length());
      hash = m.digest();
      hexHash = new BigInteger(1, hash).toString(16);
   }
}
