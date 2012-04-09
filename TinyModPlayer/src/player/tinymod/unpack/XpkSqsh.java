package player.tinymod.unpack;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class XpkSqsh {

  private static final int HEADER_LENGTH = 36;

  private static String errorMessage = null;

  private static final byte[] octets = {
    2, 3, 4, 5, 6, 7, 8, 0, 3, 2, 4, 5, 6, 7, 8, 0, 4, 3, 5, 2, 6, 7, 8, 0, 5, 4, 6, 2,
    3, 7, 8, 0, 6, 5, 7, 2, 3, 4, 8, 0, 7, 6, 8, 2, 3, 4, 5, 0, 8, 7, 6, 2, 3, 4, 5, 0,
  };

  private static class BitData {
    
    private final byte[] p;
    private final int base;
    private int i = 0;

    public BitData(byte[] data, int index) {
      p = data;
      base = index;
    }

    public int getBit() {
      int r = p[base + i / 8] >> 7 - (i % 8) & 1;
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
      return getBits(count) << 32 - count >> 32 - count;
    }
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

  private static int copyBlock(BitData data, byte[] dst, int dstPos, int count) {
    int bitCount = getCopyOffsetBitCount(data);
    int offsetBase = getCopyOffsetBase(bitCount);
    int winPos = dstPos - 1 - offsetBase - data.getBits(bitCount);
    for (int i = 0; i < count; i++)
      dst[dstPos + i] = dst[winPos + i];
    return dstPos + count;
  }

  private static int getExpandBitCount(BitData data, int bitCount) {
    if (data.getBit() == 0)
      return octets[8 * bitCount - 15];
    if (data.getBit() == 0)
      return octets[8 * bitCount - 14];
    return octets[8 * bitCount + data.getBits(2) - 13];
  }

  private static boolean sqsh(byte[] src, int srcPos, byte[] dst, int dstPos, int dstEnd) {
    if (dstEnd - dstPos != ((src[srcPos] & 255) << 8 | src[srcPos + 1] & 255)) {
      errorMessage = "SQSH chunk length does not match chunk length";
      return false;
    }
    int expandCounter = 0, expandCount = 0, bitCount = 0;
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
            last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
            last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
            last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
          }
          last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
          expandCount += 8;
        }
        last = dst[dstPos++] = (byte)(last - data.getSignBits(bitCount));
        expandCounter += expandCounter < 31 ? 1 : 0;
      }
      expandCount -= expandCount / 8;
    }
    return true;
  }

  public static boolean xpkUnpack(byte[] src, byte[] dst) {
    int srcPos = 0, dstPos = 0;
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
      if (headerChecksum != 0) {
        errorMessage = "header checksum error";
        return false;
      }
      packedLength = packedLength + 3 & 0xFFFC;
      for (int i = 0; i < packedLength; i += 2)
        dataChecksum ^= (src[srcPos + i] & 255) << 8 | src[srcPos + i + 1] & 255;
      if (dataChecksum != 0) {
        errorMessage = "data checksum error";
        return false;
      }
      if (chunkType == 0) {
        System.arraycopy(src, srcPos, dst, dstPos, unpackedLength);
      } else if (chunkType == 1) {
        if (!sqsh(src, srcPos, dst, dstPos, dstPos + unpackedLength))
          return false;
      } else {
        errorMessage = "unknown chunk type";
        return false;
      }
      srcPos += packedLength;
      dstPos += unpackedLength;
    }
    return true;
  }

  private static boolean xpkCheck(byte[] p) {
    if ('X' != p[0] || 'P' != p[1] || 'K' != p[2] || 'F' != p[3]) {
      errorMessage = "not an XPK file";
      return false;
    }
    if ('S' != p[8] || 'Q' != p[9] || 'S' != p[10] || 'H' != p[11]) {
      errorMessage = "not an XPK SQSH file";
      return false;
    }
    return true;
  }

  private static int getLength(byte[] p, int i) {
    return (p[i] << 24 | p[i + 1] << 16 | p[i + 2] << 8 | p[i + 3]) & 0xFFFFFF;
  }

  private static void cleanUp(String message) {
    System.err.println(message);
    System.exit(1);
  }

  public static void main(String[] args) throws java.io.IOException {
    if (args.length != 2)
      cleanUp("usage: xpksqsh <input-file> <output-file>");
    InputStream file = new FileInputStream(args[0]);
    byte[] header = new byte[HEADER_LENGTH];
    if (header.length != file.read(header))
      cleanUp("error reading input file header");
    if (!xpkCheck(header))
      cleanUp(errorMessage);
    byte[] src = new byte[getLength(header, 4) - HEADER_LENGTH];
    byte[] dst = new byte[getLength(header, 12)];
    if (src.length < file.read(src))
      cleanUp("error reading input file");
    if (!xpkUnpack(src, dst))
      cleanUp(errorMessage);
    OutputStream out = new FileOutputStream(args[1]);
    out.write(dst);
  }
}
