package player.tinymod.io;

public final class BitReader {
  private int pos = 0;
  private int bitPos = 7;
  private int dir = 1;
  private final byte[] data;

  public BitReader(final byte[] data, final boolean reverseByBits) {
    this.data = data;
    if (reverseByBits) {
      pos = data.length - 1;
      bitPos = 0;
      dir = -1;
    }
  }

  public int readByte() {
    final int x = data[pos] & 255;
    pos += dir;
    bitPos = dir < 0 ? 0 : 7;
    return x;
  }

  public int readBit() {
    final int x = data[pos] >> bitPos & 1;
    if (bitPos == (dir < 0 ? 7 : 0)) {
      bitPos = dir < 0 ? 0 : 7;
      pos += dir;
    } else
      bitPos -= dir;
    return x;
  }

  public int readBits(final int n) {
    int x = 0;
    for (int i = 0; i < n; i++)
      x = x << 1 | readBit();
    return x;
  }
}
