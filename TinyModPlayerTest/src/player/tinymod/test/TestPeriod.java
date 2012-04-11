package player.tinymod.test;

import gamod.tools.Period;
import junit.framework.TestCase;

public class TestPeriod extends TestCase {
  private static final int KEY_C2 = 3 * 12 + 1;
  private static final int KEY_Cs2 = 3 * 12 + 2;
  private static final int KEY_C3 = 4 * 12 + 1;
  private static final int PERIOD_C2 = 42800;

  public final void testPeriodForKey() {
    assertEquals(PERIOD_C2, Period.getPeriodForKey(KEY_C2, 0));
    assertEquals(PERIOD_C2 / 2, Period.getPeriodForKey(KEY_C3, 0));
  }

  public final void testPeriodForKeyFinetune() {
    assertEquals(Period.getPeriodForKey(KEY_C2, 3), Period.getPeriodForKey(KEY_Cs2, -5));
    assertTrue(Period.getPeriodForKey(KEY_C2, 3) != Period.getPeriodForKey(KEY_C2, 0));
  }

  public void testPeriodForKeyBounds() {
    assertEquals(Period.getPeriodForKey(0, 0), Period.getPeriodForKey(-1, 0));
    assertTrue(Period.getPeriodForKey(0, 0) != Period.getPeriodForKey(1, 0));
    assertTrue(Period.getPeriodForKey(8 * 12 + 1, 0) != Period.getPeriodForKey(8 * 12, 0));
    assertEquals(Period.getPeriodForKey(8 * 12 + 1, 0), Period.getPeriodForKey(8 * 12 + 2, 0));
  }

  public final void testKeyForPeriod() {
    assertEquals(KEY_C2, Period.getKeyForPeriod(PERIOD_C2));
    assertEquals(KEY_C3, Period.getKeyForPeriod(PERIOD_C2 / 2));
  }

  public final void testZeroKeyForPeriod() {
    assertEquals(0, Period.getKeyForPeriod(0));
  }

  public void testKeyName() {
    assertEquals("C-4", Period.getKeyName(KEY_C2, false));
    assertEquals("C-5", Period.getKeyName(KEY_C3, true));
    assertEquals("C#4", Period.getKeyName(KEY_Cs2, false));
    assertEquals(" - ", Period.getKeyName(0, false));
    assertEquals(" * ", Period.getKeyName(0, true));
  }

  public void testSnapPeriod() {
    assertTrue(PERIOD_C2 == Period.snapPeriod(Period.getPeriodForKey(KEY_C2, 3)));
    assertTrue(PERIOD_C2 != Period.snapPeriod(Period.getPeriodForKey(KEY_C2, 5)));
  }

  public void testCropPeriod() {
    assertTrue(PERIOD_C2 == Period.cropPeriod(PERIOD_C2));
    assertTrue(0 != Period.cropPeriod(0));
  }

  public void testAmigaMagicNumber() {
    assertEquals(PERIOD_C2 / 100, Period.getSamplingStepForPeriod(8287, true) >>> 16);
  }
}
