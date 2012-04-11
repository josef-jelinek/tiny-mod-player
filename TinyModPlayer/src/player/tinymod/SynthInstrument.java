package player.tinymod;

import gamod.Instrument;
import gamod.tools.Tools;

public final class SynthInstrument extends Instrument {
  private final boolean hybrid;
  private final byte[][] waveform;
  public byte[] volData;
  public int volSpeed = 0;
  public byte[] waveformData;
  public int waveformSpeed = 0;

  public SynthInstrument(int id, int length, boolean hybrid) {
    super(id);
    this.hybrid = hybrid;
    waveform = new byte[length][];
  }

  @Override
  public byte[] data() { // waveform to start with
    return waveform[0];
  }

  public byte[] data(int i) {
    return waveform[i];
  }

  @Override
  public void loop(int start, int length) {
    int datalen = hybrid ? waveform[0].length : 0;
    loopStart = Tools.crop(start, 0, datalen);
    loopLength = Tools.crop(length, 0, datalen - loopStart);
    if (loopLength <= 2) {
      loopStart = datalen;
      loopLength = 0;
    }
  }

  @Override
  public void trimTo(int length) {
    if (!hybrid || length >= waveform[0].length)
      return;
    byte[] data = new byte[length];
    for (int i = 0; i < length; i++)
      data[i] = waveform[0][i];
    waveform[0] = data;
    loop(loopStart, loopLength);
  }

  public boolean isHybrid() {
    return hybrid;
  }

  public boolean isSynthWaveform(int wf) {
    return !hybrid || wf != 0;
  }

  public void volSpeed(int speed) {
    volSpeed = speed;
  }

  public void wfSpeed(int speed) {
    waveformSpeed = speed;
  }

  public void volData(int length) {
    volData = new byte[length];
  }

  public void waveformData(int length) {
    waveformData = new byte[length];
  }

  public void waveform(int wf, int length) {
    waveform[wf] = new byte[length];
  }

  public int waveforms() {
    return waveform.length;
  }
}
