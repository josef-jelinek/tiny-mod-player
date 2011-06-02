package player.tinymod;

import static player.tinymod.Tools.crop;

public final class Sample {
  public byte[] data;
  public int leftVolume = 32;
  public int rightVolume = 32;
  public int loopStart;
  public int loopLength;

  public Sample(final int length) {
    data = new byte[length];
  }

  public Sample(final byte[] data) {
    this.data = data;
  }

  public void loop(final int start, final int length) {
    loopStart = crop(start, 0, data.length);
    loopLength = crop(start, 0, data.length - loopStart);
  }

  public void leftVolume(final int volume) {
    leftVolume = crop(volume, 0, 64);
  }

  public void rightVolume(final int volume) {
    rightVolume = crop(volume, 0, 64);
  }

  //  public static Sample loadRaw(String name, boolean signed) {
  //    byte[] data = Tools.loadBytes(name);
  //    if (data == null)
  //      return null;
  //    if (signed)
  //      for (int i = 0; i < data.length; i++)
  //        data[i] ^= 0x80;
  //    return new Sample(data);
  //  }
  public Sample resample(final Sample sample, final int from, final int to) {
    final int length = data.length * (to / 100) / (from / 100);
    final Sample smp = new Sample(length);
    final int ratio = (from << 10) / to;
    if (to > from)
      for (int i = 0; i < smp.data.length; i++) {
        int j = i * ratio >> 10;
        int delta = i * ratio & 1023;
        if (j > data.length - 2) {
          j = data.length - 2;
          delta = 1023;
        }
        smp.data[i] = (byte)(data[j] * (1023 - delta) + data[j + 1] * delta >> 10);
      }
    else
      for (int i = 0; i < smp.data.length; i++) {
        int j = i * ratio >> 10;
        if (j > data.length - 1)
          j = data.length - 1;
        smp.data[i] = data[j];
      }
    smp.leftVolume(leftVolume);
    smp.rightVolume(rightVolume);
    smp.loop(loopStart * ratio >> 10, loopLength * ratio >> 10);
    return smp;
  }
}
