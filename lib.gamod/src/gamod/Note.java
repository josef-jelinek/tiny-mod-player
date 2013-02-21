package gamod;

public final class Note {
  private static long holdMask = 1L << 63;
  private static int paramMask = 0xFFFF;
  private static int effectShift = 16;
  private static int effectMask = 0xFFFF;
  private static int keyShift = 32;
  private static int keyMask = 0xFF;
  private static int instrumentShift = 40;
  private static int instrumentMask = 0xFFF;
  private static int instrumentMaskLength = 12;

  // 63    56 55    48 47    40 39    32 31    24 23    16 15     8 7      0
  // H0000000 0000IIII IIIIIIII KKKKKKKK EEEEEEEE EEEEEEEE PPPPPPPP PPPPPPPP
  public static long create(int key, int instrument, int effect, int param, boolean hold) {
    key = Tools.crop(key, 0, 128);
    long x = hold && key < 128 ? holdMask : 0;
    x |= (long)(instrument & instrumentMask) << instrumentShift;
    x |= (long)(key & keyMask) << keyShift;
    x |= (long)(effect & effectMask) << effectShift;
    x |= (long)(param & paramMask);
    return x;
  }

  public static long create(int key, int instrument, int effect, int param) {
    return create(key, instrument, effect, param, false);
  }

  public static boolean isHolding(long note) {
    if (getHold(note))
      return true;
    int key = getKey(note);
    int effect = getEffect(note);
    return key > 0 && key < 128 && (effect == 0x03 || effect == 0x05);
  }

  private static boolean getHold(long note) {
    return (note & holdMask) != 0;
  }

  public static int getKey(long note) {
    return (int)(note >> keyShift & keyMask);
  }

  public static int getInstrument(long note) {
    int shift = 32 - instrumentMaskLength;
    return (int)(note >> instrumentShift & instrumentMask) << shift >> shift;
  }

  public static int getEffect(long note) {
    return (int)(note >> effectShift & effectMask);
  }

  public static int getParam(long note) {
    return (int)(note & paramMask);
  }

  public static boolean hasEffect(long note) {
    return getEffect(note) != 0 || getParam(note) != 0;
  }

  public static String toString(long note) {
    String keyPart = Period.getKeyName(getKey(note), getHold(note));
    return keyPart + " " + getInstrumentName(note) + " " + getEffectName(note);
  }

  private static String getInstrumentName(long note) {
    int instrument = getInstrument(note);
    return instrument < 0 ? "···" : d3(instrument);
  }

  private static String getEffectName(long note) {
    return hasEffect(note) ? h4(getEffect(note)) + ":" + h4(getParam(note)) : "····:····";
  }

  private static char h1(int x) {
    return "0123456789ABCDEF".charAt(x);
  }

  private static String d3(int x) {
    String s = x / 100 % 10 == 0 ? " " : "" + h1(x / 100 % 10);
    s += x / 10 % 10 == 0 ? " " : h1(x / 10 % 10);
    return s + h1(x % 10);
  }

  private static String h4(int x) {
    return "" + h1(x >> 12 & 0xF) + h1(x >> 8 & 0xF) + h1(x >> 4 & 0xF) + h1(x & 0xF);
  }
}
