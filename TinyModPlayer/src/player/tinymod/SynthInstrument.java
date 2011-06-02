package player.tinymod;

public final class SynthInstrument extends Instrument {
  private boolean hybrid = false;
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

  public boolean hybrid() {
    return hybrid;
  }

  public boolean synthWf(final int wf) {
    return !hybrid || wf != 0;
  }

  public void hold(final int hold, final int decay) {
    this.hold = hold;
    this.decay = decay;
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

  @Override
  public String toString() {
    String s = hybrid ? "Hybrid " : "Synthetic ";
    s += super.toString();
    s += "\nVolumes: Speed=" + volSpeed + "\n";
    for (int i = 0; i < volData.length; i++) {
      final int x = volData[i] & 255;
      if (x == 0xFF) {
        s += i + ":END";
        break;
      } else if (x < 0x80)
        s += i + ":" + x;
      else
        switch (x) {
        case 0xF0: {
          s += i + ":SPD_" + (volData[++i] & 255);
        }
          break;
        case 0xF1: {
          s += i + ":WAI_" + (volData[++i] & 255);
        }
          break;
        case 0xF2: {
          s += i + ":CHD_" + (volData[++i] & 255);
        }
          break;
        case 0xF3: {
          s += i + ":CHU_" + (volData[++i] & 255);
        }
          break;
        case 0xF4: {
          s += i + ":EN1_" + (volData[++i] & 255);
        }
          break;
        case 0xF5: {
          s += i + ":EN2_" + (volData[++i] & 255);
        }
          break;
        case 0xF6: {
          s += i + ":EST";
        }
          break;
        case 0xFA: {
          s += i + ":JWS_" + (volData[++i] & 255);
        }
          break;
        case 0xFB: {
          s += i + ":HLT";
        }
          break;
        case 0xFE: {
          s += i + ":JMP_" + (volData[++i] & 255);
        }
          break;
        default: {
          s += "???";
        }
        }
      s += " ";
    }
    s += "\nWaveforms: Number=" + waveform.length + " Speed=" + wfSpeed + "\n";
    for (int i = 0; i < wfData.length; i++) {
      final int x = wfData[i] & 255;
      if (x == 0xFF) {
        s += i + ":END";
        break;
      } else if (x < 0x80)
        s += i + ":" + x;
      else
        switch (x) {
        case 0xF0: {
          s += i + ":SPD_" + (wfData[++i] & 255);
        }
          break;
        case 0xF1: {
          s += i + ":WAI_" + (wfData[++i] & 255);
        }
          break;
        case 0xF2: {
          s += i + ":CHD_" + (wfData[++i] & 255);
        }
          break;
        case 0xF3: {
          s += i + ":CHU_" + (wfData[++i] & 255);
        }
          break;
        case 0xF4: {
          s += i + ":VBD_" + (wfData[++i] & 255);
        }
          break;
        case 0xF5: {
          s += i + ":VBS_" + (wfData[++i] & 255);
        }
          break;
        case 0xF6: {
          s += i + ":RES";
        }
          break;
        case 0xF7: {
          s += i + ":VWF_" + (wfData[++i] & 255);
        }
          break;
        case 0xFA: {
          s += i + ":JVS_" + (wfData[++i] & 255);
        }
          break;
        case 0xFB: {
          s += i + ":HLT";
        }
          break;
        case 0xFC: {
          s += i + ":ARP";
          while (++i < wfData.length && (wfData[i] & 255) < 128)
            s += "_" + wfData[i];
          i--;
        }
          break;
        case 0xFD: {
          s += "ARE";
        }
          break;
        case 0xFE: {
          s += i + ":JMP_" + (wfData[++i] & 255);
        }
          break;
        default: {
          s += "???";
        }
        }
      s += " ";
    }
    return s;
  }
}
