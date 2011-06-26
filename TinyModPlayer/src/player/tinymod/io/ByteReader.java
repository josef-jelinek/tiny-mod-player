package player.tinymod.io;

public final class ByteReader {
  private int pos = 0;
  private final byte[] data;

  public ByteReader(final byte[] data) {
    this.data = data;
  }

  public int index() {
    return pos;
  }

  public int size() {
    return data.length;
  }

  public void seek(final int pos) {
    this.pos = pos;
  }

  public void skip(final int num) {
    pos += num;
  }

  public boolean isEnd() {
    return pos >= data.length;
  }

  public byte s1() {
    return data[pos++];
  }

  public int u1() {
    return s1() & 255;
  }

  public short s2() {
    return (short)u2();
  }

  public int u2() {
    final int x = u1();
    return x << 8 | u1();
  }

  public int s4() {
    final int x = u2();
    return x << 16 | u2();
  }

  public int w2() {
    return u2() << 1;
  }

  public String string(final int length) {
    final StringBuilder s = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      final char c = (char)u1();
      if (c != 0)
        s.append(c);
    }
    return s.toString();
  }
}
