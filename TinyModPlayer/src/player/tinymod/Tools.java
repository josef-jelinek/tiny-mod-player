package player.tinymod;

import android.util.Log;

public final class Tools {
  public static void log(final String message) {
    Log.i("tinymod", message);
  }

  public static void warn(final String message) {
    Log.w("tinymod", message);
  }

  public static int sign(final int x) {
    return x > 0 ? 1 : x < 0 ? -1 : 0;
  }

  public static int crop(final int x, final int low, final int high) {
    return Math.max(low, Math.min(high, x));
  }

  private Tools() {}
}
