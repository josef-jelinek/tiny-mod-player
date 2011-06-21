package player.tinymod.io;

public final class BinaryData {
  private int pos = 0;
  private int bitPos = 7;
  private int dir = 1;
  private final byte[] data;

  public BinaryData(final byte[] data) {
    this(data, false);
  }

  public BinaryData(final byte[] data, final boolean reverseByBits) {
    this.data = data;
    if (reverseByBits) {
      pos = data.length - 1;
      bitPos = 0;
      dir = -1;
    }
  }

  public int index() {
    return pos;
  }

  public int size() {
    return data.length;
  }

  public void seek(final int pos) {
    this.pos = pos;
    bitPos = dir < 0 ? 0 : 7;
  }

  public void skip(final int num) {
    pos += dir * num;
    bitPos = dir < 0 ? 0 : 7;
  }

  public boolean isEnd() {
    return dir < 0 ? pos < 0 : pos >= data.length;
  }

  public byte s1() {
    final byte x = data[pos];
    pos += dir;
    bitPos = dir < 0 ? 0 : 7;
    return x;
  }

  public int u1() {
    return s1() & 255;
  }

  public short s2() {
    return (short)u2();
  }

  public int u2() {
    final int x = u1();
    return dir < 0 ? u1() << 8 | x : x << 8 | u1();
  }

  public int s4() {
    final int x = u2();
    return dir < 0 ? u2() << 16 | x : x << 16 | u2();
  }

  public int w2() {
    return u2() << 1;
  }

  public int b1() {
    final int x = data[pos] >> bitPos & 1;
    if (bitPos == (dir < 0 ? 7 : 0)) {
      bitPos = dir < 0 ? 0 : 7;
      pos += dir;
    } else
      bitPos -= dir;
    return x;
  }

  public int b(final int n) {
    int x = 0;
    for (int i = 0; i < n; i++)
      x = x << 1 | b1();
    return x;
  }

  public String string(final int len) {
    String s = "";
    for (int i = 0; i < len; i++) {
      final char c = (char)u1();
      if (c != 0)
        s += c;
    }
    return s;
  }
}
