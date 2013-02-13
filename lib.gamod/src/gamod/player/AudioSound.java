package gamod.player;

import gamod.*;

public final class AudioSound {
  private static final int[] sintab = {
      0, 24, 49, 74, 97, 120, 141, 161, 180, 197, 212, 224, 235, 244, 250, 253,
      255, 253, 250, 244, 235, 224, 212, 197, 180, 161, 141, 120, 97, 74, 49, 24,
      0, -24, -49, -74, -97, -120, -141, -161, -180, -197, -212, -224, -235, -244, -250, -253,
      -255, -253, -250, -244, -235, -224, -212, -197, -180, -161, -141, -120, -97, -74, -49, -24
  };
  private final long stepForPeriod;
  private final AudioSynthSound synthSound;
  private Instrument instrument;
  private byte[] data;
  private long pos;
  private long step;
  private int end;
  private int fineTune;
  private int lastKey;
  private int period;
  private int period0;
  private int leftPan;
  private int rightPan;

  public AudioSound(long stepForPeriod, int leftPan, int rightPan) {
    this.stepForPeriod = stepForPeriod;
    synthSound = new AudioSynthSound();
    this.leftPan = leftPan;
    this.rightPan = rightPan;
    instrument = null;
    resetNote();
    step = 0;
    lastKey = 0;
    period = 0;
    period0 = 0;
  }

  private void resetNote() {
    pos = 0;
    end = 0;
    fineTune = 0;
    data = null;
    synthSound.instrument(instrument);
    if (instrument != null) {
      data = instrument.data();
      end = data.length;
      fineTune = instrument.fineTune;
    }
  }

  public void play(int key, Instrument newInstrument) {
    if (newInstrument != null)
      instrument = newInstrument;
    resetNote();
    lastKey = instrument == null ? key : instrument.key(key);
    setKeyPeriod(lastKey);
  }

  public void switchTo(Instrument newInstrument) {
    if (newInstrument != null && instrument != newInstrument) {
      instrument = newInstrument;
      resetNote();
    }
  }

  public boolean synthDecay(int decay) {
    return synthSound.decay(decay);
  }

  public void synthWaveform(int pos) {
    synthSound.jumpWaveform(pos);
  }

  public void playFrom(long x) {
    pos += x << 16;
  }

  public void fineTune(int fineTune) {
    this.fineTune = Tools.crop(fineTune > 7 ? fineTune - 16 : fineTune, -8, 7);
  }

  public void instrumentVolume(int volume) {
    if (instrument != null)
      instrument.volume(volume);
  }

  private void setPeriod(int freq) {
    period0 = period = Period.cropPeriod(freq);
    step = 100 * stepForPeriod / period;
  }

  public void modPeriod(int d) {
    setPeriod(period + 100 * d);
  }

  public void toKey(int key, int speed, boolean glissando) {
    if (key > 0 && key < 128) {
      int ikey = instrument == null ? key : instrument.key(key);
      int toPeriod = Period.getPeriodForKey(ikey, fineTune);
      int sign = Tools.sign(toPeriod - period);
      modPeriod(sign * speed);
      if (glissando) // semitone slide
        setPeriod(Period.snapPeriod(period));
      if (Tools.sign(toPeriod - period) == -sign) // overadjusted
        setPeriod(toPeriod);
    }
  }

  public void setKeyPeriod(int key) {
    if (key >= 128) {
      instrument = null;
    } else if (key != 0) {
      setPeriod(Period.getPeriodForKey(key, fineTune));
    }
  }

  public void restorePeriod() {
    period = period0;
  }

  public void vibrato(int waveformType, int index, int depth) {
    modTempPeriod(modulate(waveformType, index, depth));
  }

  public static int modulate(int waveformType, int index, int depth) { // index 0..63
    return waveform(waveformType, index) * depth / 255;
  }

  public static int waveform(int type, int index) { // index 0..63
    if (type == 1) // ramp down waveform
      return 255 - index * 510 / 63;
    if (type == 2) // square waveform
      return index / 32 * 510 - 255;
    return sintab[index]; // sine (or random) waveform
  }

  public void update() { // update each tick - particularly synths
    synthSound.update();
    if (synthSound.arpeggio())
      setKeyPeriod(Tools.crop(lastKey + synthSound.keyChange(), 0, 127));
    modTempPeriod(synthSound.periodChange());
    if (synthSound.dataChange() != null) {
      data = synthSound.dataChange();
      end = data.length;
    }
  }

  private void modTempPeriod(int d) {
    period = Period.cropPeriod(period + 100 * d);
    step = 100 * stepForPeriod / period;
  }

  public void pan(int left, int right) {
    leftPan = left;
    rightPan = right;
  }

  public void mix(int[] left, int[] right, int from, int to, boolean filter, int trackVolume) {
    if (instrument == null)
      return;
    int loopStart = synthSound.synthWaveForm() ? 0 : instrument.loopStart;
    int loopLength = synthSound.synthWaveForm() ? data.length : instrument.loopLength;
    for (int i = from; loopPos(loopStart, loopLength) && i < to && (pos >>> 16) < end; i++) {
      int sampleValue = getSampleValue(filter, loopStart, loopLength);
      mixValue(left, right, i, trackVolume * synthSound.volume() * sampleValue / 4096);
      pos += step;
    }
  }

  private void mixValue(int[] left, int[] right, int i, int value) {
    left[i] += value * leftPan / 256;
    right[i] += value * rightPan / 256;
  }

  private int getSampleValue(boolean filter, int loopStart, int loopLength) {
    return filter
        ? getFilteredValue(loopStart, loopLength)
        : getInterpolatedValue(loopStart, loopLength);
  }

  private int getInterpolatedValue(int loopStart, int loopLength) {
    int posWhole = (int)(pos >>> 16);
    int d1 = sampleAt(posWhole, data, end, loopStart, loopLength) * 4;
    int d2 = sampleAt(posWhole + 1, data, end, loopStart, loopLength) * 4;
    int posFractional = (int)(pos & 0xFFFF);
    return (d1 * (0xFFFF - posFractional) + d2 * posFractional) / 0xFFFF;
  }

  private int getFilteredValue(int loopStart, int loopLength) {
    int posWhole = (int)(pos >>> 16);
    int t1 = sampleAt(posWhole, data, end, loopStart, loopLength) * 4;
    int t2 = sampleAt(posWhole + 1, data, end, loopStart, loopLength) * 4;
    int d1 = t2 + 2 * t1 + sampleAt(posWhole - 1, data, end, loopStart, loopLength) * 4;
    int d2 = t1 + 2 * t2 + sampleAt(posWhole + 2, data, end, loopStart, loopLength) * 4;
    int posFractional = (int)(pos & 0xFFFF);
    return (d1 * (0xFFFF - posFractional) + d2 * posFractional) / 0xFFFF / 4;
  }

  private boolean loopPos(int loopStart, int loopLength) {
    while ((pos >>> 16) >= end) {
      if (loopLength == 0)
        return false;
      pos = ((long)loopStart << 16) | (pos & 0xFFFF);
      end = loopStart + loopLength;
    }
    return true;
  }

  private static int sampleAt(int pos, byte[] data, int end, int loopStart, int loopLength) {
    if (pos < 0)
      return 0;
    if (pos < end)
      return data[pos];
    if (loopLength == 0 || loopStart >= end)
      return 0;
    while (pos - end >= loopLength)
      pos -= loopLength;
    return data[loopStart + pos - end];
  }
}
