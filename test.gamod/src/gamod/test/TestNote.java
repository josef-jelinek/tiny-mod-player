package gamod.test;

import static org.junit.Assert.*;
import gamod.Note;
import org.junit.Test;

public class TestNote {
  @Test
  public void isHolding_true_when_told_and_good_key() {
    assertTrue(Note.isHolding(Note.create(0, 0, 0, 0, true)));
    assertTrue(Note.isHolding(Note.create(1, 0, 0, 0, true)));
    assertTrue(Note.isHolding(Note.create(127, 0, 0, 0, true)));
    assertFalse(Note.isHolding(Note.create(128, 0, 0, 0, true)));
  }

  @Test
  public void isHolding_false_for_most_effects() {
    assertFalse(Note.isHolding(Note.create(0, 0, 0, 0)));
    assertFalse(Note.isHolding(Note.create(1, 0, 0, 0)));
    assertFalse(Note.isHolding(Note.create(127, 0, 0, 0)));
    assertFalse(Note.isHolding(Note.create(128, 0, 0, 0)));
  }

  @Test
  public void isHolding_true_just_for_some_key_effect_pair() {
    assertFalse(Note.isHolding(Note.create(0, 0, 5, 0)));
    assertTrue(Note.isHolding(Note.create(1, 0, 3, 0)));
    assertTrue(Note.isHolding(Note.create(127, 0, 5, 0)));
    assertFalse(Note.isHolding(Note.create(128, 0, 3, 0)));
  }

  @Test
  public void getKey() {
    assertEquals(0, Note.getKey(Note.create(-1, 0, 0, 0)));
    assertEquals(0, Note.getKey(Note.create(0, 0, 0, 0)));
    assertEquals(127, Note.getKey(Note.create(127, 0, 0, 0)));
    assertEquals(128, Note.getKey(Note.create(128, 0, 0, 0)));
    assertEquals(128, Note.getKey(Note.create(129, 0, 0, 0)));
  }

  @Test
  public void instrument_is_signed_12bit() {
    assertEquals(-1, Note.getInstrument(Note.create(0, -1, 0, 0)));
    assertEquals(0, Note.getInstrument(Note.create(0, 0, 0, 0)));
    assertEquals(1, Note.getInstrument(Note.create(0, 1, 0, 0)));
    assertEquals(0x7FF, Note.getInstrument(Note.create(0, 0x7FF, 0, 0)));
    assertEquals(-1, Note.getInstrument(Note.create(0, 0xFFF, 0, 0)));
    assertEquals(-1, Note.getInstrument(Note.create(0, 0xFFFF, 0, 0)));
  }

  @Test
  public void effect_is_unsigned_16bit() {
    assertEquals(0xFFFF, Note.getEffect(Note.create(0, 0, -1, 0)));
    assertEquals(0, Note.getEffect(Note.create(0, 0, 0, 0)));
    assertEquals(1, Note.getEffect(Note.create(0, 0, 1, 0)));
    assertEquals(0xFFFF, Note.getEffect(Note.create(0, 0, 0xFFFF, 0)));
    assertEquals(0xFFFF, Note.getEffect(Note.create(0, 0, 0xFFFFF, 0)));
  }

  @Test
  public void param_is_unsigned_16bit() {
    assertEquals(0xFFFF, Note.getParam(Note.create(0, 0, 0, -1)));
    assertEquals(0, Note.getParam(Note.create(0, 0, 0, 0)));
    assertEquals(1, Note.getParam(Note.create(0, 0, 0, 1)));
    assertEquals(0xFFFF, Note.getParam(Note.create(0, 0, 0, 0xFFFF)));
    assertEquals(0xFFFF, Note.getParam(Note.create(0, 0, 0, 0xFFFFF)));
  }

  @Test
  public void note_effect_detection() {
    assertFalse(Note.hasEffect(Note.create(0, 0, 0, 0)));
    assertFalse(Note.hasEffect(Note.create(1, 0, 0, 0)));
    assertFalse(Note.hasEffect(Note.create(0, 1, 0, 0)));
    assertTrue(Note.hasEffect(Note.create(0, 0, 1, 0)));
    assertTrue(Note.hasEffect(Note.create(0, 0, 0, 1)));
  }
  
  @Test
  public void conversion_to_string() {
    assertEquals(" -  ··· ····:····", Note.toString(Note.create(-1, -1, 0, 0)));
    assertEquals(" -    0 ····:····", Note.toString(Note.create(0, 0, 0, 0)));
    assertEquals(" *    0 ····:····", Note.toString(Note.create(0, 0, 0, 0, true)));
    assertEquals("C-1   1 0001:0001", Note.toString(Note.create(1, 1, 1, 1)));
    assertEquals("C-1   1 0001:0001", Note.toString(Note.create(1, 1, 1, 1, true)));
    assertEquals("C#1  16 0010:00FF", Note.toString(Note.create(2, 16, 16, 255)));
    assertEquals("D-1 999 0100:0FFF", Note.toString(Note.create(3, 999, 256, 4095)));
    assertEquals("B-8   1 0100:0FFF", Note.toString(Note.create(96, 1001, 256, 4095)));
    assertEquals(" !    0 FFFF:0000", Note.toString(Note.create(128, 1000, 65535, 65536)));
  }
}
