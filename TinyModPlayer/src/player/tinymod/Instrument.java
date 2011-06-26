package player.tinymod;

public abstract class Instrument {
  public static final int SAMPLED = 0;
  public static final int SYNTHETIC = 1;
  public static final int HYBRID = 2;
  public final int id;
  public String name = "";
  public int fineTune = 0;
  public int transpose = 0;
  public int volume = 64;
  public int loopStart = 0;
  public int loopLength = 0;
  private int maxKey = 127;
  private int minKey = 1;
  public int hold = 0;
  public int decay = 0;

  public Instrument(final int id) {
    this.id = id;
  }

  public abstract byte[] data();

  public abstract void loop(int start, int length);

  public abstract void trimTo(int length);

  public final int key(final int key) {
    int ikey = key + transpose;
    while (ikey > maxKey)
      ikey -= 12;
    while (ikey < minKey)
      ikey += 12;
    return ikey;
  }

  public final void maxKey(final int key) {
    maxKey = key;
  }

  public final void minKey(final int key) {
    minKey = key;
  }

  public final void name(final String name) {
    this.name = name;
  }

  public final void volume(final int volume) {
    this.volume = Tools.crop(volume, 0, 64);
  }

  public final void fineTune(final int fineTune) {
    this.fineTune = Tools.crop(fineTune < 8 ? fineTune : fineTune - 16, -8, 7);
  }

  public final void transpose(final int transpose) {
    this.transpose = transpose;
  }

  public final void hold(final int hold) {
    this.hold = hold;
  }

  public final void decay(final int decay) {
    this.decay = decay;
  }
}
