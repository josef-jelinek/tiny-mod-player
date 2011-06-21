package player.tinymod.unpack;

import player.tinymod.io.BinaryData;

public final class PowerPackerLite {
  public static byte[] unpack(final byte[] pk) {
    if (pk.length < 12 || pk[0] != 'P' || pk[1] != 'P' || pk[2] != '2' || pk[3] != '0')
      return null;
    final int[] offs = new int[4];
    for (int i = 0; i < 4; i++)
      offs[i] = pk[i + 4];
    final BinaryData data = new BinaryData(pk, true);
    final int skip = data.u1();
    int outlen = data.u1();
    outlen |= data.u1() << 8;
    outlen |= data.u1() << 16;
    final byte[] out = new byte[outlen];
    data.b(skip); // skipped bits
    while (outlen > 0) {
      if (data.b1() == 0) { // copy from input
        int cnt = 1, cinc;
        while ((cinc = data.b(2)) == 3)
          // '11's + 1 + the last non '11'
          cnt += 3;
        for (cnt += cinc; cnt > 0; cnt--)
          out[--outlen] = (byte)data.b(8);
        if (outlen == 0)
          break;
      }
      int run = data.b(2); // always at least 2 bytes (2 bytes ~ 0, 3 ~ 1, 4 ~ 2, 5+ ~ 3)
      final int offBits = run == 3 && data.b1() == 0 ? 7 : offs[run];
      final int off = data.b(offBits);
      int rinc = 0;
      if (run == 3)
        while ((rinc = data.b(3)) == 7)
          // '111's + 2 + the last non '111'
          run += 7;
      for (run += 2 + rinc; run > 0; run--, outlen--)
        out[outlen - 1] = out[outlen + off];
    }
    return out;
  }

  private PowerPackerLite() {}
}
