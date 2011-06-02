package player.tinymod;

import static player.tinymod.Tools.crop;

public final class Freq {
  private static final int stepsPerToneCnt = 8;
  private static final int tonesPerOctaveCnt = 12;
  private static final int octaveCnt = 8;
  private static final int toneCnt = octaveCnt * tonesPerOctaveCnt + 1;
  private static final int[] freqs = new int[toneCnt * stepsPerToneCnt];
  private static final int[] toneFreqs = new int[toneCnt]; // C-0 C#0 D-0 ... C-8
  private static final int[] divFreqs = new int[toneCnt - 1];
  static {
    // frequency of C-2 = 428 - is there a more accurate note?
    // index of C-2 in the table = (1 + 12 * 3) * 8 = 296;
    // 2^(deltaFrom("C-2") / tonesPerOctaveCnt / stepsPerToneCnt)
    // gives the multiplication factor
    for (int i = 0; i < freqs.length; i++)
      freqs[i] = (int)Math.round(42800.0 * Math.pow(2.0, (296.0 - i) / 96.0));
    for (int i = 0; i < toneFreqs.length; i++)
      toneFreqs[i] = freqs[i * stepsPerToneCnt];
    for (int i = 0; i < divFreqs.length; i++)
      divFreqs[i] = freqs[i * stepsPerToneCnt + stepsPerToneCnt / 2];
  }

  public static int getFreqForKey(final int key, final int finetune) {
    return freqs[crop(key * stepsPerToneCnt + finetune, 0, freqs.length - 1)];
  }

  public static int getKeyForFreq(final int freq) { // table lookup
    for (int i = 0; i < divFreqs.length; i++)
      if (freq > divFreqs[i])
        return i;
    return divFreqs.length;
  }

  public static int snapFreq(final int freq) {
    return toneFreqs[getKeyForFreq(freq)];
  }

  public static int cropFreq(final int freq) {
    return crop(freq, freqs[freqs.length - 1], freqs[0]);
  }

  public static long magic(final int sampleFreq, final boolean pal) {
    // PAL  (50Hz): rate = 3546894.6
    // NTSC (60Hz): rate = 3579545.25
    // Hz = rate / amigaPeriod,
    //                          rate / amigaPeriod   rate / sampleFreq
    // step = Hz / sampleFreq = ------------------ = -----------------
    //                              sampleFreq          amigaPeriod
    // for example on PAL, sampleFreq for C-2 is 8287
    // amigaPeriod = rate / Hz = 428 (slightly rounded)
    final long magicPal = (354689460l << 16) / (sampleFreq * 100);
    final long magicNtsc = (357954525l << 16) / (sampleFreq * 100);
    // Now, since step will be a fixed-point value, we'll multiply it
    // by 65536, thus moving the integral part into the high-word,
    // and the fractional part into the low-word.
    return pal ? magicPal : magicNtsc;
  }

  private Freq() {}
}
