package gamod.player;

import gamod.Instrument;
import gamod.tools.Tools;

public final class SampledInstrument extends Instrument {
  private byte[] data;

  public SampledInstrument(int id, int length) {
    super(id);
    data = new byte[length];
  }

  @Override
  public byte[] data() {
    return data;
  }

  @Override
  public void loop(int start, int length) {
    loopStart = Tools.crop(start, 0, data.length);
    loopLength = Tools.crop(length, 0, data.length - loopStart);
    if (loopLength <= 2) {
      loopStart = data.length;
      loopLength = 0;
    }
  }

  @Override
  public void trimTo(int length) {
    if (length < data.length) {
      byte[] a = new byte[length];
      System.arraycopy(data, 0, a, 0, length);
      data = a;
      loop(loopStart, loopLength);
    }
  }
}
