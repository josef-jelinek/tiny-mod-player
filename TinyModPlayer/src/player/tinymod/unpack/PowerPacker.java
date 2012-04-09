package player.tinymod.unpack;

import player.tinymod.io.BitReader;

public final class PowerPacker {
  public static byte[] unpack(byte[] data) {
    if (data.length < 12 || data[0] != 'P' || data[1] != 'P' || data[2] != '2' || data[3] != '0')
      return null;
    int[] offsetBitLengths = getOffsetBitLengths(data);
    int skip = data[data.length - 1] & 255;
    byte[] out = new byte[getDecodedDataSize(data)];
    int outPos = out.length;
    BitReader in = new BitReader(data, data.length - 5, true);
    in.readBits(skip); // skipped bits
    while (outPos > 0)
      outPos = decodeSegment(in, out, outPos, offsetBitLengths);
    return out;
  }

  private static int[] getOffsetBitLengths(byte[] data) {
    int[] a = new int[4];
    for (int i = 0; i < 4; i++)
      a[i] = data[i + 4];
    return a;
  }

  private static int getDecodedDataSize(byte[] data) {
    int i = data.length - 2;
    return (data[i - 2] & 255) << 16 | (data[i - 1] & 255) << 8 | data[i] & 255;
  }

  private static int decodeSegment(BitReader in, byte[] out, int outPos, int[] offsetBitLengths) {
    if (in.readBit() == 0)
      outPos = copyFromInput(in, out, outPos);
    if (outPos > 0)
      outPos = copyFromDecoded(in, out, outPos, offsetBitLengths);
    return outPos;
  }

  private static int copyFromInput(BitReader in, byte[] out, int outPos) {
    int cnt = 1, cinc;
    while ((cinc = in.readBits(2)) == 3) // '11's + 1 + the last non '11'
      cnt += 3;
    for (cnt += cinc; cnt > 0; cnt--)
      out[--outPos] = (byte)in.readBits(8);
    return outPos;
  }

  private static int copyFromDecoded(BitReader in, byte[] out, int outPos, int[] offsetBitLengths) {
    int run = in.readBits(2); // always at least 2 bytes (2 bytes ~ 0, 3 ~ 1, 4 ~ 2, 5+ ~ 3)
    int offBits = run == 3 && in.readBit() == 0 ? 7 : offsetBitLengths[run];
    int off = in.readBits(offBits);
    int rinc = 0;
    if (run == 3)
      while ((rinc = in.readBits(3)) == 7) // '111's + 2 + the last non '111'
        run += 7;
    for (run += 2 + rinc; run > 0; run--, outPos--)
      out[outPos - 1] = out[outPos + off];
    return outPos;
  }
}
