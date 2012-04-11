package player.tinymod;

import gamod.Note;

public final class Pattern {
  private final Note[][] notes;
  private final String[] lineStrings;

  public Pattern(int tracks, int rows) {
    notes = new Note[tracks][rows];
    lineStrings = new String[rows];
  }

  public Note getNote(int track, int row) {
    return notes[track][row];
  }

  public int rows() {
    int max = 0;
    for (int i = 0; i < notes.length; i++)
      max = Math.max(max, notes[i].length);
    return max;
  }

  public int tracks() {
    return notes.length;
  }

  public void setNote(int track, int row, Note note) {
    notes[track][row] = note;
  }

  public String rowString(int row) {
    if (lineStrings[row] == null) {
      String s = "" + row / 100 % 10 + row / 10 % 10 + row % 10;
      for (int track = 0; track < tracks(); track++)
        s += "|" + getNote(track, row);
      lineStrings[row] = s + "|";
    }
    return lineStrings[row];
  }
}
