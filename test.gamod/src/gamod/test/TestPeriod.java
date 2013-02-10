package gamod.test;

import static org.junit.Assert.*;
import gamod.Period;
import org.junit.Test;

public class TestPeriod {
  private static final int KEY_C2 = 3 * 12 + 1;
  private static final int KEY_Cs2 = 3 * 12 + 2;
  private static final int KEY_C3 = 4 * 12 + 1;
  private static final int PERIOD_C2 = 42800;

  @Test
  public void getPeriodForKey() {
    assertEquals(PERIOD_C2, Period.getPeriodForKey(KEY_C2, 0));
    assertEquals(PERIOD_C2 / 2, Period.getPeriodForKey(KEY_C3, 0));
  }

  @Test
  public void getPeriodForKey_finetune() {
    assertEquals(Period.getPeriodForKey(KEY_C2, 3), Period.getPeriodForKey(KEY_Cs2, -5));
    assertTrue(Period.getPeriodForKey(KEY_C2, 3) != Period.getPeriodForKey(KEY_C2, 0));
  }

  @Test
  public void getPeriodForKey_bounds() {
    assertEquals(Period.getPeriodForKey(0, 0), Period.getPeriodForKey(-1, 0));
    assertTrue(Period.getPeriodForKey(0, 0) != Period.getPeriodForKey(1, 0));
    assertTrue(Period.getPeriodForKey(8 * 12 + 1, 0) != Period.getPeriodForKey(8 * 12, 0));
    assertEquals(Period.getPeriodForKey(8 * 12 + 1, 0), Period.getPeriodForKey(8 * 12 + 2, 0));
  }

  @Test
  public void getKeyForPeriod() {
    assertEquals(KEY_C2, Period.getKeyForPeriod(PERIOD_C2));
    assertEquals(KEY_C3, Period.getKeyForPeriod(PERIOD_C2 / 2));
  }

  @Test
  public void getKeyForPeriod_zero() {
    assertEquals(0, Period.getKeyForPeriod(0));
  }

  @Test
  public void getKeyName() {
    assertEquals("C-4", Period.getKeyName(KEY_C2, false));
    assertEquals("C-5", Period.getKeyName(KEY_C3, true));
    assertEquals("C#4", Period.getKeyName(KEY_Cs2, false));
    assertEquals(" - ", Period.getKeyName(0, false));
    assertEquals(" * ", Period.getKeyName(0, true));
  }

  @Test
  public void snapPeriod() {
    assertTrue(PERIOD_C2 == Period.snapPeriod(Period.getPeriodForKey(KEY_C2, 3)));
    assertTrue(PERIOD_C2 != Period.snapPeriod(Period.getPeriodForKey(KEY_C2, 5)));
  }

  @Test
  public void cropPeriod() {
    assertTrue(PERIOD_C2 == Period.cropPeriod(PERIOD_C2));
    assertTrue(0 != Period.cropPeriod(0));
  }

  @Test
  public void amiga_magic_number() {
    assertEquals(PERIOD_C2 / 100, Period.getSamplingStepForPeriod(8287, true) >>> 16);
  }
}
