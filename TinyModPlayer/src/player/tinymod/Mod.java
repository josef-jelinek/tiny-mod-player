package player.tinymod;

import static player.tinymod.Tools.crop;
import java.util.Arrays;
import player.tinymod.io.ByteReader;

public final class Mod {
  public String title = "";
  public String tracker = "";
  public Instrument[] instruments;
  public int songLength;
  public int[] blockOrder = new int[256];
  public Block[] blocks;
  public int mainVolume = 64;
  public final int tracks;
  public final int[] trackVolumes;
  public boolean filter = false;
  public int beatsPerMinute = 125;
  public int linesPerBeat = 0; // = 0 if ticksPerBeat != 0
  public int ticksPerBeat = 24; // = 0 if linesPerBeat != 0
  public int ticksPerLine = 6;
  public int transpose = 0;
  public boolean doFirstLineTick = false;

  public Mod(int tracks) {
    this.tracks = tracks;
    trackVolumes = new int[tracks];
    Arrays.fill(trackVolumes, 64);
  }

  // TODO remove after completing
  @SuppressWarnings("unused")
  public static Mod parseAhx(final byte[] file) {
    final ByteReader data = new ByteReader(file);
    final String id = data.string(3);
    final int ver = data.u1();
    if (!id.equals("THX") || ver > 1)
      return null;
    final Mod mod = new Mod(4);
    data.u2(); // offset to title and sample names
    int len = data.u2(); // position list length + timing + track zero flag
    //mod.tps = 50 + 50 * (len >> 12 & 7);
    final boolean trackZero = (len & 0x8000) != 0;
    len &= 0xFFF;
    final int res = crop(data.u2(), 0, len - 1); // restart position
    final int trl = crop(data.u1(), 1, 64); // track length
    final int trk = data.u1(); // number of tracks saved
    final int smp = crop(data.u1(), 0, 63); // number of samples saved
    final int ss = data.u1(); // number of subsongs
    final int[] sslist = new int[ss]; // subsong list
    for (int i = 0; i < ss; i++)
      sslist[i] = crop(data.u2(), 0, len - 1);
    return mod;
  }
}
