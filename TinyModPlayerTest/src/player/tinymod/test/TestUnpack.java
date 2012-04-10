package player.tinymod.test;

import junit.framework.TestCase;
import player.tinymod.unpack.PowerPacker;

public class TestUnpack extends TestCase {
  private static byte[] short_packed = { 'P', 'P', '2', '0', 0, 0, 0, 0, 0, 0, 0 };
  private static byte[] empty_packed = { 'P', 'P', '2', '0', 0, 0, 0, 0, 0, 0, 0, 0 };
  private static byte[] one_packed = { 'P', 'P', '2', '0', 0, 0, 0, 0, 48, 0, 0, 0, 1, 5 };

  public void test_PowerPacker_unpack() {
    assertNull(PowerPacker.unpackData(short_packed));
    assertEquals(0, PowerPacker.unpackData(empty_packed).length);
    assertEquals(1, PowerPacker.unpackData(one_packed).length);
    assertEquals(12, PowerPacker.unpackData(one_packed)[0]);
  }
}
