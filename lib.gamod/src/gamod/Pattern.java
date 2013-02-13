package gamod;

public final class Pattern {
  private final long[][] notes;
  private final String[] lineStrings;

  public Pattern(int tracks, int rows) {
    notes = new long[tracks][rows];
    lineStrings = new String[rows];
  }

  public long getNote(int track, int row) {
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

  public void setNote(int track, int row, long note) {
    notes[track][row] = note;
  }

  public String rowString(int row) {
    if (lineStrings[row] == null) {
      String s = "" + row / 100 % 10 + row / 10 % 10 + row % 10;
      for (int track = 0; track < tracks(); track++)
        s += "|" + Note.toString(getNote(track, row));
      lineStrings[row] = s + "|";
    }
    return lineStrings[row];
  }
}
