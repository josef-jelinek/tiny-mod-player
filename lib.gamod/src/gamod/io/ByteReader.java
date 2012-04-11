package gamod.io;

public final class ByteReader {
  public static char badChar = (char)183;
  public static char nulChar = (char)215;
  
  private int pos = 0;
  private final byte[] data;

  public ByteReader(byte[] data) {
    this.data = data;
  }

  public int index() {
    return pos;
  }

  public int size() {
    return data.length;
  }

  public void seek(int pos) {
    this.pos = pos;
  }

  public void skip(int n) {
    pos += n;
  }

  public int available() {
    return Math.max(0, data.length - pos);
  }

  public int s1() {
    return data[pos++];
  }

  public int u1() {
    return s1() & 255;
  }

  public int s2() {
    return (short)u2();
  }

  public int u2() {
    int x = u1();
    return x << 8 | u1();
  }

  public int s4() {
    int x = u2();
    return x << 16 | u2();
  }

  public int w2() {
    return u2() << 1;
  }

  public String string(int length) {
    final StringBuilder s = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int c = u1();
      s.append(c == 0 ? (char)215 : c < 32 || c > 127 ? (char)183 : (char)c);
    }
    return s.toString();
  }
  
  public byte[] bytes(int length) {
    byte[] a = new byte[length];
    for (int i = 0; i < length; i++)
      a[i] = (byte)s1();
    return a;
  }
}
