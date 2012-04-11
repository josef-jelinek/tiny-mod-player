package player.tinymod;

import player.tinymod.tools.Tools;

public final class Note {
  public final int key;
  public final Instrument instrument;
  public final int effect;
  public final int paramX;
  public final int paramY;
  private final boolean hold;

  public Note(int key, Instrument instrument, int effect, int paramX, int paramY, boolean hold) {
    this.key = Tools.crop(key, 0, 128);
    this.instrument = instrument;
    this.effect = effect;
    this.paramX = paramX;
    this.paramY = paramY;
    this.hold = hold && this.key < 128;
  }

  public boolean isHolding() {
    return hold || key > 0 && key < 128 && (effect == 0x03 || effect == 0x05);
  }

  @Override
  public String toString() {
    return Period.getKeyName(key, hold) + " " + getInstrumentName() + " " + getEffectName();
  }

  private String getInstrumentName() {
    return instrument == null ? "··" : d2(instrument.id);
  }

  private String getEffectName() {
    return hasEffect() ? h2(effect) + h1(paramX) + h1(paramY) : "····";
  }

  private boolean hasEffect() {
    return effect != 0 || paramX != 0 || paramY != 0;
  }
  
  private static long holdMask = 1L << 63;

  // H0000000 0000IIII IIIIIIII KKKKKKKK EEEEEEEE EEEEEEEE PPPPPPPP PPPPPPPP
  public static long create(int key, int instrument, int effect, int param, boolean hold) {
    long x = hold ? holdMask : 0;
    x |= (instrument & 0xFFFL) << 40;
    x |= (key & 0xFFL) << 32;
    x |= (effect & 0xFFFFL) << 16;
    x |= param & 0xFFFFL;
    return x;
  }
  
  private static char h1(int x) {
    return "0123456789ABCDEF".charAt(x);
  }

  private static String d2(int x) {
    return (x / 10 == 0 ? " " : "" + h1(x / 10)) + h1(x % 10);
  }

  private static String h2(int x) {
    return "" + h1(x / 16) + h1(x % 16);
  }
}
