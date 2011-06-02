package player.tinymod;

public final class SynthSound {
  private SynthInstrument instrument;
  private byte[] data;
  private int volume;
  private int volSpeed;
  private int volPos;
  private int volTick;
  private int volWait;
  private int volDif;
  private int envWf;
  private boolean envLoop;
  private int envPos;
  private int waveform;
  private int wfSpeed;
  private int wfPos;
  private int wfTick;
  private int wfWait;
  private int wfPeriod;
  private int wfDif;
  private int keyDelta;
  private boolean arpeggio;
  private int arpStart;
  private int arpPos;
  private int periodDelta;
  private int vibWf;
  private int vibSpeed;
  private int vibDepth;
  private int vibPos;

  public void reset() {
    instrument = null;
    data = null;
    volume = 64;
    volSpeed = 0;
    volPos = 0;
    volTick = 0;
    volWait = 0;
    volDif = 0;
    envWf = -1;
    envLoop = false;
    envPos = 0;
    waveform = 0;
    wfSpeed = 0;
    wfPos = 0;
    wfTick = 0;
    wfWait = 0;
    wfPeriod = 0;
    wfDif = 0;
    keyDelta = 0;
    arpeggio = false;
    arpStart = 0;
    arpPos = 0;
    periodDelta = 0;
    vibWf = -1;
    vibSpeed = 0;
    vibDepth = 0;
    vibPos = 0;
  }

  public void instrument(final Instrument instrument) {
    reset();
    if (instrument instanceof SynthInstrument) {
      this.instrument = (SynthInstrument)instrument;
      volSpeed = Math.max(1, this.instrument.volSpeed);
      wfSpeed = Math.max(1, this.instrument.wfSpeed);
    }
  }

  public int volume() {
    return volume;
  }

  public boolean arpeggio() {
    return arpeggio;
  }

  public int keyChange() {
    return keyDelta;
  }

  public int periodChange() {
    return periodDelta;
  }

  public byte[] dataChange() {
    return data;
  }

  public boolean synthWf() {
    return instrument != null && instrument.synthWf(waveform);
  }

  public boolean decay(final int decay) {
    if (instrument == null)
      return false;
    volPos = decay;
    volWait = 0;
    System.err.println("vol jump to " + volPos);
    return true;
  }

  public void jumpWf(final int pos) {
    if (instrument != null) {
      wfPos = pos;
      wfWait = 0;
      System.err.println("wf jump to " + pos);
    }
  }

  public void update() {
    data = null;
    keyDelta = 0;
    periodDelta = 0;
    if (instrument == null)
      return;
    volUpdate();
    wfUpdate();
    if (arpeggio) {
      if (arpPos > instrument.wfData.length || (instrument.wfData[arpPos] & 255) >= 128)
        arpPos = arpStart;
      keyDelta = instrument.wfData[arpPos++] & 255;
    }
    if (vibDepth > 0) {
      if (vibWf >= 0) {
        vibPos %= instrument.data(vibWf).length * 16;
        periodDelta = (instrument.data(vibWf)[vibPos / 16] + 128) * 2 * vibDepth / 255 - vibDepth;
      } else {
        vibPos %= 32 * 16;
        periodDelta = Sound.modulate(0, vibPos / 8, vibDepth * 2);
      }
      vibPos += vibSpeed;
    }
    periodDelta += wfPeriod;
  }

  private void volUpdate() {
    if (volTick == 0) {
      volume = Tools.crop(volume + volDif, 0, 64);
      if (envWf >= 0) { // waveform envelope
        // probably for !hybrid only
        if (envPos >= instrument.data(envWf).length || envPos >= 128) {
          envPos = 0;
          if (!envLoop)
            envWf = -1;
        }
        if (envWf >= 0)
          volume = (instrument.data(envWf)[envPos] + 128) * 64 / 255;
      }
      if (volWait > 0)
        volWait--;
      else {
        boolean get = true;
        int cnt = 0;
        while (get && volPos < instrument.volData.length && cnt < instrument.volData.length) {
          final int cmd = instrument.volData[volPos++] & 255;
          if (cmd < 128) {
            volume = Tools.crop(cmd, 0, 64);
            break;
          }
          switch (cmd) {
          case 0xF0: { // SPD
            volSpeed = instrument.volData[volPos++] & 255;
          }
            break;
          case 0xF1: { // WAI
            volWait = instrument.volData[volPos++] & 255;
            get = false;
          }
            break;
          case 0xF2: { // CHD
            volDif = -(instrument.volData[volPos++] & 255);
          }
            break;
          case 0xF3: { // CHU
            volDif = instrument.volData[volPos++] & 255;
          }
            break;
          case 0xF4: { // EN1 - take a waveform as an envelop once
            envWf = instrument.volData[volPos++] & 255;
            envPos = 0;
            envLoop = false;
          }
            break;
          case 0xF5: { // EN2 - take a looped waveform as an envelope
            envWf = instrument.volData[volPos++] & 255;
            envPos = 0;
            envLoop = true;
          }
            break;
          case 0xF6: { // EST (RES) - reset envelope (no waveform)
            envWf = -1;
          }
            break;
          case 0xFA: { // JWS - jump in the waveform table
            wfPos = instrument.volData[volPos++] & 255;
            wfWait = 0;
          }
            break;
          case 0xFE: { // JMP
            volPos = instrument.volData[volPos] & 255;
          }
            break;
          case 0xFB: // HLT
          case 0xFF: // END
            volPos--;
          default: {
            get = false;
          }
          }
          cnt++;
        }
      }
    }
    volTick = (volTick + 1) % volSpeed;
  }

  private void wfUpdate() {
    if (wfTick == 0) {
      wfPeriod += wfDif;
      if (wfWait > 0)
        wfWait--;
      else {
        boolean get = true;
        int cnt = 0;
        while (get && wfPos < instrument.wfData.length && cnt < instrument.wfData.length) {
          final int cmd = instrument.wfData[wfPos++] & 255;
          if (cmd < 128) {
            waveform = cmd;
            data = instrument.data(waveform);
            break;
          }
          switch (cmd) {
          case 0xF0: { // SPD
            wfSpeed = instrument.wfData[wfPos++] & 255;
          }
            break;
          case 0xF1: { // WAI
            wfWait = instrument.wfData[wfPos++] & 255;
            get = false;
          }
            break;
          case 0xF2: { // CHD
            wfDif = instrument.wfData[wfPos++] & 255;
          }
            break;
          case 0xF3: { // CHU
            wfDif = -(instrument.wfData[wfPos++] & 255);
          }
            break;
          case 0xF4: { // VBD
            vibDepth = instrument.wfData[wfPos++] & 255;
          }
            break;
          case 0xF5: { // VBS
            vibSpeed = instrument.wfData[wfPos++] & 255;
          }
            break;
          case 0xF6: { // RES
            wfPeriod = 0;
          }
            break;
          case 0xF7: { // VWF
            vibWf = instrument.wfData[wfPos++] & 255;
          }
            break;
          case 0xFA: { // JVS
            volPos = instrument.wfData[wfPos++] & 255;
            volWait = 0;
          }
            break;
          case 0xFC: { // ARP
            arpeggio = wfPos < instrument.wfData.length && (instrument.wfData[wfPos] & 255) < 128;
            if (arpeggio) {
              arpPos = arpStart = wfPos;
              while (wfPos < instrument.wfData.length && (instrument.wfData[wfPos] & 255) < 128)
                wfPos++;
            }
          }
            break;
          case 0xFD: { // ARE
          }
            break;
          case 0xFE: { // JMP
            wfPos = instrument.wfData[wfPos] & 255;
          }
            break;
          case 0xFB: // HLT
          case 0xFF: { // END
            wfPos--;
            get = false;
          }
            break;
          default: {
            get = false;
          }
            break;
          }
          cnt++;
        }
      }
    }
    wfTick = (wfTick + 1) % wfSpeed;
  }
}
