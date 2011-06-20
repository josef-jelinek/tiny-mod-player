package player.tinymod;

import static player.tinymod.Tools.crop;

public final class Period {
  private static final int stepsPerTone = 8;
  private static final int tonesPerOctave = 12;
  private static final int totalOctaves = 8;
  private static final int totalTones = totalOctaves * tonesPerOctave + 1;
  private static final int[] periods = new int[totalTones * stepsPerTone];
  private static final int[] tonePeriods = new int[totalTones]; // C-0 C#0 D-0 ... C-8
  private static final int[] toneBorderPeriods = new int[totalTones];
  static {
    final int periodOfC2 = 42800;
    final int indexOfC2 = (3 * tonesPerOctave + 1) * stepsPerTone;
    final double stepsPerOctave = tonesPerOctave * stepsPerTone;
    for (int i = 0; i < periods.length; i++)
      periods[i] = (int)Math.round(periodOfC2 * Math.pow(2.0, (indexOfC2 - i) / stepsPerOctave));
    for (int i = 0; i < tonePeriods.length; i++)
      tonePeriods[i] = periods[i * stepsPerTone];
    for (int i = 0; i < toneBorderPeriods.length; i++)
      toneBorderPeriods[i] = periods[i * stepsPerTone + stepsPerTone / 2];
  }

  /** Returns Amiga period number for the given tone key and finetune (there are
   * 8 finetune steps between 2 adjacent tones) */
  public static int getPeriodForKey(final int key, final int finetune) {
    return periods[crop(key * stepsPerTone + finetune, 0, periods.length - 1)];
  }

  /** Finds the closest tone key for the given Amiga period number */
  public static int getKeyForPeriod(final int period) { // table lookup
    if (period == 0)
      return 0;
    int low = 0, high = toneBorderPeriods.length - 1;
    while (high > low) {
      final int middle = (low + high) / 2;
      if (toneBorderPeriods[middle] > period)
        low = middle + 1;
      else
        high = middle;
    }
    return low;
  }

  public static int snapPeriod(final int period) {
    return tonePeriods[getKeyForPeriod(period)];
  }

  public static int cropPeriod(final int period) {
    return crop(period, periods[periods.length - 1], periods[0]);
  }

  public static long getSamplingStepForPeriod(final int sampleFrequence, final boolean pal) {
    final long ratePal_x100 = 354689460l; // PAL  (50Hz): rate = 3546894.6
    final long rateNtsc_x100 = 357954525l; // NTSC (60Hz): rate = 3579545.25
    // Hz = rate / amigaPeriod,
    //                               rate / amigaPeriod   rate / sampleFrequence
    // step = Hz / sampleFrequence = ------------------ = ----------------------
    //                                sampleFrequence          amigaPeriod
    // for example on PAL, sampleFrequence for C-2 is 8287
    // amigaPeriod = rate / Hz ~ 428
    // Step will be a fixed-point value with 16b whole + 16b fractional.
    return ((pal ? ratePal_x100 : rateNtsc_x100) << 16) / (sampleFrequence * 100);
  }

  private Period() {}
}
