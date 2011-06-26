package player.tinymod;

public final class AudioSound {
  private static final int[] sintab = {
      0, 24, 49, 74, 97, 120, 141, 161, 180, 197, 212, 224, 235, 244, 250, 253, 255, 253, 250, 244,
      235, 224, 212, 197, 180, 161, 141, 120, 97, 74, 49, 24, 0, -24, -49, -74, -97, -120, -141,
      -161, -180, -197, -212, -224, -235, -244, -250, -253, -255, -253, -250, -244, -235, -224,
      -212, -197, -180, -161, -141, -120, -97, -74, -49, -24 };
  private final long stepForPeriod;
  private Instrument instrument;
  private byte[] data;
  private long pos;
  private long step;
  private int end;
  private int fineTune;
  private final AudioSynthSound synth;
  private int lastKey;
  private int period;
  private int period0;
  private final int lPan0;
  private final int rPan0;
  private int lPan;
  private int rPan;

  public AudioSound(final long stepForPeriod, final int left, final int right) {
    this.stepForPeriod = stepForPeriod;
    lPan0 = left;
    rPan0 = right;
    synth = new AudioSynthSound();
    lPan = lPan0;
    rPan = rPan0;
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
    synth.instrument(instrument);
    if (instrument != null) {
      data = instrument.data();
      end = data.length;
      fineTune = instrument.fineTune;
    }
  }

  public void play(final int key, final Instrument newInstrument) {
    if (newInstrument != null)
      instrument = newInstrument;
    resetNote();
    lastKey = instrument == null ? key : instrument.key(key);
    setKeyPeriod(lastKey);
  }

  public void switchTo(final Instrument newInstrument) {
    if (newInstrument != null && instrument != newInstrument) {
      instrument = newInstrument;
      resetNote();
    }
  }

  public boolean decay(final int decay) {
    return synth.decay(decay);
  }

  public void synthWf(final int pos) {
    synth.jumpWaveForm(pos);
  }

  public void playFrom(final long x) {
    pos += x << 16;
  }

  public void fineTune(final int fineTune) {
    this.fineTune = Tools.crop(fineTune > 7 ? fineTune - 16 : fineTune, -8, 7);
  }

  public void instrumentVolume(final int volume) {
    if (instrument != null)
      instrument.volume(volume);
  }

  private void setPeriod(final int freq) {
    period0 = period = Period.cropPeriod(freq);
    step = 100 * stepForPeriod / period;
  }

  public void modPeriod(final int d) {
    setPeriod(period + 100 * d);
  }

  public void toKey(final int key, final int speed, final boolean glissando) {
    if (key > 0 && key < 128) {
      final int ikey = instrument == null ? key : instrument.key(key);
      final int toPeriod = Period.getPeriodForKey(ikey, fineTune);
      final int sign = Tools.sign(toPeriod - period);
      modPeriod(sign * speed);
      if (glissando) // semitone slide
        setPeriod(Period.snapPeriod(period));
      if (Tools.sign(toPeriod - period) == -sign) // overadjusted
        setPeriod(toPeriod);
    }
  }

  public void setKeyPeriod(final int key) {
    if (key == 0)
      return;
    if (key >= 128)
      instrument = null;
    else
      setPeriod(Period.getPeriodForKey(key, fineTune));
  }

  private void modTempPeriod(final int d) {
    period = Period.cropPeriod(period + 100 * d);
    step = 100 * stepForPeriod / period;
  }

  public void restorePeriod() {
    period = period0;
  }

  public void vibrato(final int waveform, final int index, final int depth) {
    modTempPeriod(modulate(waveform, index, depth));
  }

  public static int modulate(final int waveform, final int index, final int depth) { // index 0..63
    return waveform(waveform, index) * depth / 255;
  }

  public static int waveform(final int type, final int index) { // index 0..63
    if ((type & 3) == 1) // ramp down waveform
      return 255 - index * 510 / 63;
    if ((type & 3) == 2) // square waveform
      return index / 32 * 510 - 255;
    return sintab[index]; // sine (or random) waveform
  }

  public void update() { // update each tick - particularly synths
    synth.update();
    if (synth.arpeggio())
      setKeyPeriod(Tools.crop(lastKey + synth.keyChange(), 0, 127));
    modTempPeriod(synth.periodChange());
    if (synth.dataChange() != null) {
      data = synth.dataChange();
      end = data.length;
    }
  }

  public void pan(final int left, final int right) {
    lPan = left;
    rPan = right;
  }

  public void mix(final int[] lBuf, final int[] rBuf, final int from, final int to,
      final boolean filter, final int trackVolume) {
    if (instrument == null)
      return;
    int lps = instrument.loopStart;
    int lpl = instrument.loopLength;
    if (synth.synthWaveForm()) {
      lps = 0;
      lpl = data.length;
    }
    if (!fixPos(lps, lpl))
      return;
    for (int i = from; i < to && (pos >>> 16) < end; i++) {
      final int sp = (int)(pos >>> 16);
      int d = data[sp] * 4;
      if (filter) { // simple filter
        final int t = sampleAt(sp + 1, data, end, lps, lpl) * 4;
        final int d1 = (sampleAt(sp - 1, data, end, lps, lpl) * 4 + d * 2 + t);
        final int d2 = (d + 2 * t + sampleAt(sp + 2, data, end, lps, lpl) * 4);
        final int spf = (int)(pos & 65535);
        d = (d1 * (65535 - spf) + d2 * spf) / 65535 / 4;
      } else { // linear resampling
        final int spf = (int)(pos & 65535);
        final int t = sampleAt(sp + 1, data, end, lps, lpl) * 4;
        d = (d * (65535 - spf) + t * spf) / 65535;
      }
      // trackVolume = track.volume * main.volume / main.boost
      final int s = trackVolume * synth.volume() * d / 4096;
      lBuf[i] += s * lPan / 256;
      rBuf[i] += s * rPan / 256;
      pos += step;
      if (!fixPos(lps, lpl))
        break;
    }
  }

  private boolean fixPos(final int lps, final int lpl) {
    while ((pos >>> 16) >= end) {
      if (lpl == 0)
        return false;
      pos = ((long)lps << 16) | (pos & 65535);
      end = lps + lpl;
    }
    return true;
  }

  private static int sampleAt(int pos, final byte[] data, final int end, final int lps,
      final int lpl) {
    if (pos < 0)
      return 0;
    if (pos < end)
      return data[pos];
    if (lpl == 0 || lps >= end)
      return 0;
    while (pos - end >= lpl)
      pos -= lpl;
    return data[lps + pos - end];
  }
}