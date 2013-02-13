package gamod.test;

import static org.junit.Assert.assertEquals;
import gamod.Pattern;
import org.junit.Test;

public class TestPattern {
  @Test
  public void sizes_match() {
    Pattern p = new Pattern(3, 8);
    assertEquals(8, p.rows());
    assertEquals(3, p.tracks());
  }

  @Test
  public void isHolding_false_for_most_effects() {
    Pattern p = new Pattern(3, 8);
    long note = 0;
    for (int track = 0; track < 3; track++) {
      for (int row = 0; row < 8; row++) {
        p.setNote(track, row, note++);
      }
    }
    note = 0;
    for (int track = 0; track < 3; track++) {
      for (int row = 0; row < 8; row++) {
        assertEquals(note++, p.getNote(track, row));
      }
    }
  }
}
