package gamod.test;

import static org.junit.Assert.*;
import gamod.unpack.PowerPacker;
import org.junit.Test;

public class TestUnpack {
  private static byte[] short_packed = { 'P', 'P', '2', '0', 0, 0, 0, 0, 0, 0, 0 };
  private static byte[] empty_packed = { 'P', 'P', '2', '0', 0, 0, 0, 0, 0, 0, 0, 0 };
  private static byte[] one_packed = { 'P', 'P', '2', '0', 0, 0, 0, 0, 48, 0, 0, 0, 1, 5 };

  @Test
  public void powerPacker_unpack_short_data() {
    assertNull(PowerPacker.unpackData(short_packed));
    assertEquals(0, PowerPacker.unpackData(empty_packed).length);
    assertEquals(1, PowerPacker.unpackData(one_packed).length);
    assertEquals(12, PowerPacker.unpackData(one_packed)[0]);
  }
}
