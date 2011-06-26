package player.tinymod.unpack;

import player.tinymod.io.BitReader;

public final class PowerPacker {
  public static byte[] unpack(final byte[] pk) {
    if (pk.length < 12 || pk[0] != 'P' || pk[1] != 'P' || pk[2] != '2' || pk[3] != '0')
      return null;
    final int[] offsetBitLengths = getOffsetBitLengths(pk);
    final BitReader data = new BitReader(pk, true);
    final int skip = data.readByte();
    int outPos = getDecodedDataSize(data);
    final byte[] out = new byte[outPos];
    data.readBits(skip); // skipped bits
    while (outPos > 0)
      outPos = decodeSegment(data, out, outPos, offsetBitLengths);
    return out;
  }

  private static int[] getOffsetBitLengths(final byte[] pk) {
    final int[] offs = new int[4];
    for (int i = 0; i < 4; i++)
      offs[i] = pk[i + 4];
    return offs;
  }

  private static int getDecodedDataSize(final BitReader data) {
    int outPos = data.readByte();
    outPos |= data.readByte() << 8;
    outPos |= data.readByte() << 16;
    return outPos;
  }

  private static int decodeSegment(final BitReader in, final byte[] out, int outPos,
      final int[] offsetBitLengths) {
    if (in.readBit() == 0)
      outPos = copyFromInput(in, out, outPos);
    if (outPos > 0)
      outPos = copyFromDecoded(in, out, outPos, offsetBitLengths);
    return outPos;
  }

  private static int copyFromInput(final BitReader in, final byte[] out, int outPos) {
    int cnt = 1, cinc;
    while ((cinc = in.readBits(2)) == 3)
      // '11's + 1 + the last non '11'
      cnt += 3;
    for (cnt += cinc; cnt > 0; cnt--)
      out[--outPos] = (byte)in.readBits(8);
    return outPos;
  }

  private static int copyFromDecoded(final BitReader in, final byte[] out, int outPos,
      final int[] offsetBitLengths) {
    int run = in.readBits(2); // always at least 2 bytes (2 bytes ~ 0, 3 ~ 1, 4 ~ 2, 5+ ~ 3)
    final int offBits = run == 3 && in.readBit() == 0 ? 7 : offsetBitLengths[run];
    final int off = in.readBits(offBits);
    int rinc = 0;
    if (run == 3)
      while ((rinc = in.readBits(3)) == 7)
        // '111's + 2 + the last non '111'
        run += 7;
    for (run += 2 + rinc; run > 0; run--, outPos--)
      out[outPos - 1] = out[outPos + off];
    return outPos;
  }

  private PowerPacker() {}
}
