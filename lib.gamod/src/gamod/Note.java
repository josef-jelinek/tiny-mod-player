package gamod;

import gamod.tools.*;

public final class Note {
  private static long holdMask = 1L << 63;

  // H0000000 0000IIII IIIIIIII KKKKKKKK EEEEEEEE EEEEEEEE PPPPPPPP PPPPPPPP
  public static long create(int key, int instrument, int effect, int param, boolean hold) {
    key = Tools.crop(key, 0, 128);
    long x = hold && key < 128 ? holdMask : 0;
    x |= (instrument & 0xFFFL) << 40;
    x |= (key & 0xFFL) << 32;
    x |= (effect & 0xFFFFL) << 16;
    x |= param & 0xFFFFL;
    return x;
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
    return (int)(note >> 32 & 0xFF);
  }
  
  public static int getInstrument(long note) {
    return (int)(note >> 40 & 0xFFF) << 20 >> 20;
  }

  public static int getEffect(long note) {
    return (int)(note >> 16 & 0xFFFF);
  }
  
  public static int getParam(long note) {
    return (int)(note & 0xFFFF);
  }

  private static boolean hasEffect(long note) {
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
    String s = x / 100 == 0 ? " " : "" + h1(x / 100);
    s += x / 10 % 10 == 0 ? " " : h1(x / 10 % 10);
    return s + h1(x % 10);
  }

  private static String h4(int x) {
    return "" + h1(x >> 12 & 0xF) + h1(x >> 8 & 0xF) + h1(x >> 4 & 0xF) + h1(x & 0xF);
  }
}
