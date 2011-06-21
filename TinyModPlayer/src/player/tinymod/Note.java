package player.tinymod;

public final class Note {
  private static final String[] names = {
      "C-1", "C#1", "D-1", "D#1", "E-1", "F-1", "F#1", "G-1", "G#1", "A-1", "A#1", "B-1", "C-2",
      "C#2", "D-2", "D#2", "E-2", "F-2", "F#2", "G-2", "G#2", "A-2", "A#2", "B-2", "C-3", "C#3",
      "D-3", "D#3", "E-3", "F-3", "F#3", "G-3", "G#3", "A-3", "A#3", "B-3", "C-4", "C#4", "D-4",
      "D#4", "E-4", "F-4", "F#4", "G-4", "G#4", "A-4", "A#4", "B-4", "C-5", "C#5", "D-5", "D#5",
      "E-5", "F-5", "F#5", "G-5", "G#5", "A-5", "A#5", "B-5", "C-6", "C#6", "D-6", "D#6", "E-6",
      "F-6", "F#6", "G-6", "G#6", "A-6", "A#6", "B-6", "C-7", "C#7", "D-7", "D#7", "E-7", "F-7",
      "F#7", "G-7", "G#7", "A-7", "A#7", "B-7", "C-8", "C#8", "D-8", "D#8", "E-8", "F-8", "F#8",
      "G-8", "G#8", "A-8", "A#8", "B-8" };
  public final int key;
  public final Instrument sample;
  public final int effect;
  public final int paramX;
  public final int paramY;
  public final boolean hold;
  public String string;

  public Note(final int key, final Instrument sample, final int effect, final int paramX,
      final int paramY, final boolean hold) {
    this.key = Tools.crop(key, 0, 128);
    this.sample = sample;
    this.effect = effect;
    this.paramX = paramX;
    this.paramY = paramY;
    this.hold = hold && this.key < 128;
  }

  @Override
  public String toString() {
    if (string == null) {
      string = key == 0 ? (hold ? " | " : " - ") : names[key + 1];
      string += sample == null ? "  -" : " " + d2(sample.id);
      if (effect == 0 && paramX == 0 && paramY == 0)
        string += "  -  ";
      else
        string += " " + h2(effect) + h1(paramX) + h1(paramY);
    }
    return string;
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