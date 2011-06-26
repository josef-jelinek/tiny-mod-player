package player.tinymod;

import static player.tinymod.Tools.crop;

public final class Period {
  private static final String[] keyNames = {
      " - ", "C-1", "C#1", "D-1", "D#1", "E-1", "F-1", "F#1", "G-1", "G#1", "A-1", "A#1", "B-1",
      "C-2", "C#2", "D-2", "D#2", "E-2", "F-2", "F#2", "G-2", "G#2", "A-2", "A#2", "B-2", "C-3",
      "C#3", "D-3", "D#3", "E-3", "F-3", "F#3", "G-3", "G#3", "A-3", "A#3", "B-3", "C-4", "C#4",
      "D-4", "D#4", "E-4", "F-4", "F#4", "G-4", "G#4", "A-4", "A#4", "B-4", "C-5", "C#5", "D-5",
      "D#5", "E-5", "F-5", "F#5", "G-5", "G#5", "A-5", "A#5", "B-5", "C-6", "C#6", "D-6", "D#6",
      "E-6", "F-6", "F#6", "G-6", "G#6", "A-6", "A#6", "B-6", "C-7", "C#7", "D-7", "D#7", "E-7",
      "F-7", "F#7", "G-7", "G#7", "A-7", "A#7", "B-7", "C-8", "C#8", "D-8", "D#8", "E-8", "F-8",
      "F#8", "G-8", "G#8", "A-8", "A#8", "B-8" };
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

  public static String getKeyName(final int key, final boolean hold) {
    return key == 0 && hold ? " * " : key < 0 || key >= keyNames.length ? " ! " : keyNames[key];
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
