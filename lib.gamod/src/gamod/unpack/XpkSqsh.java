package gamod.unpack;

public final class XpkSqsh implements Unpacker {
  public String name() {
    return "Xpk Squash (XPKF SQSH)";
  }

  public boolean test(byte[] data) {
    return testData(data);
  }

  public byte[] unpack(byte[] data) {
    return unpackData(data);
  }

  private static final int HEADER_LENGTH = 36;
  private static final byte[] octets = {
    2, 3, 4, 5, 6, 7, 8, 0,
    3, 2, 4, 5, 6, 7, 8, 0,
    4, 3, 5, 2, 6, 7, 8, 0,
    5, 4, 6, 2, 3, 7, 8, 0,
    6, 5, 7, 2, 3, 4, 8, 0,
    7, 6, 8, 2, 3, 4, 5, 0,
    8, 7, 6, 2, 3, 4, 5, 0,
  };

  private static boolean testData(byte[] a) {
    if (a.length < 12)
      return false;
    if ('X' != a[0] || 'P' != a[1] || 'K' != a[2] || 'F' != a[3])
      return false;
    if ('S' != a[8] || 'Q' != a[9] || 'S' != a[10] || 'H' != a[11])
      return false;
    return true;
  }

  public static byte[] unpackData(byte[] data) {
    if (testData(data) && getLength(data, 4) == data.length - 8) {
      byte[] dst = new byte[getLength(data, 12)];
      if (unpack(data, dst, HEADER_LENGTH))
        return dst;
    }
    return null;
  }

  private static int getLength(byte[] a, int i) {
    return (a[i] & 127) << 24 | (a[i + 1] & 255) << 16 | (a[i + 2] & 255) << 8 | a[i + 3] & 255;
  }

  private static boolean unpack(byte[] src, byte[] dst, int srcPos) {
    int dstPos = 0;
    while (dstPos < dst.length) {
      int chunkType = src[srcPos] & 255;
      int headerChecksum = src[srcPos + 1] & 255;
      int dataChecksum = (src[srcPos + 2] & 255) << 8 | src[srcPos + 3] & 255;
      int packedLength = (src[srcPos + 4] & 255) << 8 | src[srcPos + 5] & 255;
      int unpackedLength = (src[srcPos + 6] & 255) << 8 | src[srcPos + 7] & 255;
      srcPos += 8;
      headerChecksum ^= chunkType;
      headerChecksum ^= dataChecksum >> 8 ^ dataChecksum & 255;
      headerChecksum ^= packedLength >> 8 ^ packedLength & 255;
      headerChecksum ^= unpackedLength >> 8 ^ unpackedLength & 255;
      if (headerChecksum != 0)
        return false;
      packedLength = packedLength + 3 & 0xFFFC;
      if (srcPos + packedLength + 1 > src.length)
        return false;
      for (int i = 0; i < packedLength; i += 2)
        dataChecksum ^= (src[srcPos + i] & 255) << 8 | src[srcPos + i + 1] & 255;
      if (dataChecksum != 0)
        return false;
      if (dstPos + unpackedLength > dst.length)
        return false;
      if (chunkType == 0) {
        System.arraycopy(src, srcPos, dst, dstPos, unpackedLength);
      } else if (chunkType == 1) {
        if (!unsqsh(src, srcPos, dst, dstPos, dstPos + unpackedLength))
          return false;
      } else {
        return false;
      }
      srcPos += packedLength;
      dstPos += unpackedLength;
    }
    return true;
  }

  private static boolean unsqsh(byte[] src, int srcPos, byte[] dst, int dstPos, int dstEnd) {
    if (dstEnd - dstPos != ((src[srcPos] & 255) << 8 | src[srcPos + 1] & 255))
      return false;
    int expandCounter = 0, expandCount = 0, bitCount = 0;
    if (dstPos >= dst.length)
      return false;
    int last = dst[dstPos++] = src[srcPos + 2];
    BitData data = new BitData(src, srcPos + 3);
    while (dstPos < dstEnd) {
      boolean b1 = data.getBit() == 1;
      if (b1 && expandCounter < 8 || !b1 && expandCounter >= 8 && data.getBit() == 0) {
        int count = getCopyCount(data);
        dstPos = copyBlock(data, dst, dstPos, count);
        last = dst[dstPos - 1];
        expandCounter -= count < 3 || expandCounter == 0 ? 0 : count == 3 || expandCounter == 1 ? 1 : 2;
      } else {
        bitCount = expandCounter < 8 ? 8 : b1 ? bitCount : getExpandBitCount(data, bitCount);
        if (expandCounter >= 8 && (bitCount != 8 || expandCount >= 20)) {
          if (bitCount != 8) {
            if (dstPos < dst.length)
              last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
            if (dstPos < dst.length)
              last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
            if (dstPos < dst.length)
              last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
          }
          if (dstPos < dst.length)
            last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
          expandCount += 8;
        }
        if (dstPos < dst.length)
          last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
        expandCounter += expandCounter < 31 ? 1 : 0;
      }
      expandCount -= expandCount / 8;
    }
    return true;
  }

  private static int copyBlock(BitData data, byte[] dst, int dstPos, int count) {
    int bitCount = getCopyOffsetBitCount(data);
    int offsetBase = getCopyOffsetBase(bitCount);
    int winPos = dstPos - 1 - offsetBase - data.getBits(bitCount);
    for (int i = 0; i < count; i++)
      if (dstPos + i < dst.length)
        dst[dstPos + i] = dst[winPos + i];
    return Math.min(dst.length, dstPos + count);
  }

  private static int getCopyCount(BitData data) {
    if (data.getBit() == 0)
      return 2 + data.getBit();
    if (data.getBit() == 0)
      return 4 + data.getBit();
    if (data.getBit() == 0)
      return 6 + data.getBit();
    if (data.getBit() == 0)
      return 8 + data.getBits(3);
    return 16 + data.getBits(5);
  }

  private static int getExpandBitCount(BitData data, int bitCount) {
    if (data.getBit() == 0)
      return octets[8 * bitCount - 15];
    if (data.getBit() == 0)
      return octets[8 * bitCount - 14];
    return octets[8 * bitCount + data.getBits(2) - 13];
  }

  private static int getCopyOffsetBitCount(BitData data) {
    if (data.getBit() == 1)
      return 12;
    if (data.getBit() == 1)
      return 14;
    return 8;
  }

  private static int getCopyOffsetBase(int bitCount) {
    if (bitCount == 12)
      return 0x100;
    if (bitCount == 14)
      return 0x1100;
    return 0;
  }

  private static class BitData {
    private final byte[] p;
    private final int base;
    private int i = 0;

    public BitData(byte[] data, int index) {
      p = data;
      base = index;
    }

    public int getBit() {
      int r = (p[base + i / 8] >> 7 - i % 8) & 1;
      i++;
      return r;
    }

    public int getBits(int count) {
      int b = 0;
      for (int k = 0; k < count; k++)
        b = b << 1 | getBit();
      return b;
    }

    public int getSignBits(int count) {
      return getBits(count) << (32 - count) >> (32 - count);
    }
  }
}
