package gamod;

public final class Tools {
  private static final String digits = "0123456789";

  public static int digit(String s, int pos) {
    return digits.indexOf(s.charAt(pos));
  }
  
  public static int sign(int x) {
    return x > 0 ? 1 : x < 0 ? -1 : 0;
  }

  public static int crop(int x, int low, int high) {
    return x < low ? low : x > high ? high : x;
  }

  public static String trimEnd(String s, char c) {
    int last = s.length() - 1;
    while (last >= 0 && s.charAt(last) == c)
      last -= 1;
    return s.substring(0, last + 1);
  }
}
