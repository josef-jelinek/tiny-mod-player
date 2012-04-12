package gamod.test;

import static org.junit.Assert.*;
import gamod.Note;
import org.junit.Test;

public class TestNote {
  @Test
  public final void test_isHolding_true_when_told_and_good_key() {
    assertTrue(Note.isHolding(Note.create(0, 0, 0, 0, true)));
    assertTrue(Note.isHolding(Note.create(1, 0, 0, 0, true)));
    assertTrue(Note.isHolding(Note.create(127, 0, 0, 0, true)));
    assertFalse(Note.isHolding(Note.create(128, 0, 0, 0, true)));
  }
  
  public final void test_isHolding_false_for_most_effects() {
    assertFalse(Note.isHolding(Note.create(0, 0, 0, 0)));
    assertFalse(Note.isHolding(Note.create(1, 0, 0, 0)));
    assertFalse(Note.isHolding(Note.create(127, 0, 0, 0)));
    assertFalse(Note.isHolding(Note.create(128, 0, 0, 0)));
  }
  
  public final void test_isHolding_true_just_for_some_key_effect_pair() {
    assertFalse(Note.isHolding(Note.create(0, 0, 5, 0)));
    assertTrue(Note.isHolding(Note.create(1, 0, 3, 0)));
    assertTrue(Note.isHolding(Note.create(127, 0, 5, 0)));
    assertFalse(Note.isHolding(Note.create(128, 0, 3, 0)));
  }

  @Test
  public final void test_getKey() {
    assertEquals(0, Note.getKey(Note.create(-1, 0, 0, 0)));
    assertEquals(0, Note.getKey(Note.create(0, 0, 0, 0)));
    assertEquals(127, Note.getKey(Note.create(127, 0, 0, 0)));
    assertEquals(128, Note.getKey(Note.create(128, 0, 0, 0)));
    assertEquals(128, Note.getKey(Note.create(129, 0, 0, 0)));
  }

  @Test
  public final void test_instrument_is_signed_12bit() {
    assertEquals(-1, Note.getInstrument(Note.create(0, -1, 0, 0)));
    assertEquals(0, Note.getInstrument(Note.create(0, 0, 0, 0)));
    assertEquals(1, Note.getInstrument(Note.create(0, 1, 0, 0)));
    assertEquals(0x7FF, Note.getInstrument(Note.create(0, 0x7FF, 0, 0)));
    assertEquals(-1, Note.getInstrument(Note.create(0, 0xFFF, 0, 0)));
    assertEquals(-1, Note.getInstrument(Note.create(0, 0xFFFF, 0, 0)));
  }

  @Test
  public final void test_effect_is_unsigned_16bit() {
    assertEquals(0xFFFF, Note.getEffect(Note.create(0, 0, -1, 0)));
    assertEquals(0, Note.getEffect(Note.create(0, 0, 0, 0)));
    assertEquals(1, Note.getEffect(Note.create(0, 0, 1, 0)));
    assertEquals(0xFFFF, Note.getEffect(Note.create(0, 0, 0xFFFF, 0)));
    assertEquals(0xFFFF, Note.getEffect(Note.create(0, 0, 0xFFFFF, 0)));
  }

  @Test
  public final void test_param_is_unsigned_16bit() {
    assertEquals(0xFFFF, Note.getParam(Note.create(0, 0, 0, -1)));
    assertEquals(0, Note.getParam(Note.create(0, 0, 0, 0)));
    assertEquals(1, Note.getParam(Note.create(0, 0, 0, 1)));
    assertEquals(0xFFFF, Note.getParam(Note.create(0, 0, 0, 0xFFFF)));
    assertEquals(0xFFFF, Note.getParam(Note.create(0, 0, 0, 0xFFFFF)));
  }
}
