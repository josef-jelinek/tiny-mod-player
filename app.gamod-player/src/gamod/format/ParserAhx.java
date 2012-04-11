package gamod.format;

import static gamod.tools.Tools.crop;
import gamod.*;
import gamod.io.ByteReader;
import gamod.player.Mod;

public final class ParserAhx implements Parser {

  public String name() {
    return "AHX";
  }

  public boolean test(byte[] data) {
    return false;
  }

  public Mod parse(byte[] data) {
    final ByteReader reader = new ByteReader(data);
    final String id = reader.string(3);
    final int ver = reader.u1();
    if (!id.equals("THX") || ver > 1)
      return null;
    final Mod mod = new Mod(4);
    reader.u2(); // offset to title and sample names
    int len = reader.u2(); // position list length + timing + track zero flag
    //mod.tps = 50 + 50 * (len >> 12 & 7);
    final boolean trackZero = (len & 0x8000) != 0;
    len &= 0xFFF;
    final int res = crop(reader.u2(), 0, len - 1); // restart position
    final int trl = crop(reader.u1(), 1, 64); // track length
    final int trk = reader.u1(); // number of tracks saved
    final int smp = crop(reader.u1(), 0, 63); // number of samples saved
    final int ss = reader.u1(); // number of subsongs
    final int[] sslist = new int[ss]; // subsong list
    for (int i = 0; i < ss; i++)
      sslist[i] = crop(reader.u2(), 0, len - 1);
    return mod;
  }
}
