package player.tinymod;

public final class Block {
  private final Note[][] notes;
  private final String[] lineStrings;

  public Block(final int lines, final int tracks) {
    notes = new Note[lines][tracks];
    lineStrings = new String[lines];
  }

  public Note getNote(final int line, final int track) {
    return notes[line][track];
  }

  public int getNumberOfLines() {
    return notes.length;
  }

  public int getNumberOfTracks() {
    return notes[0].length;
  }

  public void putNote(final int line, final int track, final Note note) {
    notes[line][track] = note;
  }

  public String lineString(final int line) {
    if (lineStrings[line] == null) {
      String s = "" + line / 100 % 10 + line / 10 % 10 + line % 10;
      for (int track = 0; track < getNumberOfTracks(); track++)
        s += "|" + getNote(line, track);
      lineStrings[line] = s + "|";
    }
    return lineStrings[line];
  }
}
