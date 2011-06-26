package player.tinymod;

public final class SynthInstrument extends Instrument {
  private final boolean hybrid;
  private final byte[][] waveform;
  public byte[] volData;
  public int volSpeed = 0;
  public byte[] wfData;
  public int wfSpeed = 0;

  public SynthInstrument(final int id, final int length, final boolean hybrid) {
    super(id);
    this.hybrid = hybrid;
    waveform = new byte[length][];
  }

  @Override
  public byte[] data() { // waveform to start with
    return waveform[0];
  }

  public byte[] data(final int i) {
    return waveform[i];
  }

  @Override
  public void loop(final int start, final int length) {
    final int datalen = hybrid ? waveform[0].length : 0;
    loopStart = Tools.crop(start, 0, datalen);
    loopLength = Tools.crop(length, 0, datalen - loopStart);
    if (loopLength <= 2) {
      loopStart = datalen;
      loopLength = 0;
    }
  }

  @Override
  public void trimTo(final int length) {
    if (!hybrid || length >= waveform[0].length)
      return;
    final byte[] data = new byte[length];
    for (int i = 0; i < length; i++)
      data[i] = waveform[0][i];
    waveform[0] = data;
    loop(loopStart, loopLength);
  }

  public boolean isHybrid() {
    return hybrid;
  }

  public boolean synthWf(final int wf) {
    return !hybrid || wf != 0;
  }

  public void volSpeed(final int speed) {
    volSpeed = speed;
  }

  public void wfSpeed(final int speed) {
    wfSpeed = speed;
  }

  public void volData(final int length) {
    volData = new byte[length];
  }

  public void wfData(final int length) {
    wfData = new byte[length];
  }

  public void waveform(final int wf, final int length) {
    waveform[wf] = new byte[length];
  }

  public int waveforms() {
    return waveform.length;
  }
}
