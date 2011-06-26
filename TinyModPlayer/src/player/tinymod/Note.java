package player.tinymod;

public final class Note {
  public final int key;
  public final Instrument instrument;
  public final int effect;
  public final int paramX;
  public final int paramY;
  public final boolean hold;
  public String string;

  public Note(final int key, final Instrument instrument, final int effect, final int paramX,
      final int paramY, final boolean hold) {
    this.key = Tools.crop(key, 0, 128);
    this.instrument = instrument;
    this.effect = effect;
    this.paramX = paramX;
    this.paramY = paramY;
    this.hold = hold && this.key < 128;
  }

  @Override
  public String toString() {
    if (string == null)
      string = Period.getKeyName(key, hold) + " " + getInstrumentName() + " " + getEffectName();
    return string;
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

  private static char h1(final int x) {
    return "0123456789ABCDEF".charAt(x);
  }

  private static String d2(final int x) {
    return (x / 10 == 0 ? " " : "" + h1(x / 10)) + h1(x % 10);
  }

  private static String h2(final int x) {
    return "" + h1(x / 16) + h1(x % 16);
  }
}
