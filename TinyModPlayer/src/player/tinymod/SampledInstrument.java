package player.tinymod;

public final class SampledInstrument extends Instrument {
  private byte[] data;

  public SampledInstrument(final int id, final int length) {
    super(id);
    data = new byte[length];
  }

  @Override
  public byte[] data() {
    return data;
  }

  @Override
  public void loop(final int start, final int length) {
    loopStart = Tools.crop(start, 0, data.length);
    loopLength = Tools.crop(length, 0, data.length - loopStart);
    if (loopLength <= 2) {
      loopStart = data.length;
      loopLength = 0;
    }
  }

  @Override
  public void trimTo(final int length) {
    if (length >= data.length)
      return;
    final byte[] newdata = new byte[length];
    for (int i = 0; i < length; i++)
      newdata[i] = data[i];
    data = newdata;
    loop(loopStart, loopLength);
  }

  @Override
  public String toString() {
    return "Sampled " + super.toString();
  }
}
