package player.tinymod;

public final class Block {
  private final Note[][] notes;

  public Block(final int lines, final int tracks) {
    notes = new Note[lines][tracks];
  }

  public Note note(final int line, final int track) {
    return notes[line][track];
  }

  public int lines() {
    return notes.length;
  }

  public int tracks(final int line) {
    return notes[line].length;
  }

  public void putNote(final int line, final int track, final Note note) {
    notes[line][track] = note;
  }

  public String lineString(final int line) {
    String s = "";
    for (int track = 0; track < tracks(line); track++)
      s += (track > 0 ? "|" : "") + note(line, track);
    return s;
  }
}
