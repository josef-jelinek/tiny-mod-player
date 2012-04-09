package player.tinymod;

public final class Tools {
  private static final String digits = "0123456789";

  public static int digit(String s, int pos) {
    return digits.indexOf(s.charAt(pos));
  }
  
  public static int sign(int x) {
    return x > 0 ? 1 : x < 0 ? -1 : 0;
  }

  public static int crop(int x, int low, int high) {
    return Math.max(low, Math.min(high, x));
  }

  public static String escape(String s) {
    for (int i = s.length() - 1; i >= 0; i--)
      if (s.charAt(i) < 32 || s.charAt(i) > 127 || s.charAt(i) == '[')
        s = s.substring(0, i) + "[" + (int)s.charAt(i) + "]" + s.substring(i + 1);
    return s;
  }

  public static String trimEnd(String s, char c) {
    int last = s.length() - 1;
    while (last >= 0 && s.charAt(last) == c)
      last -= 1;
    return s.substring(0, last + 1);
  }
}
