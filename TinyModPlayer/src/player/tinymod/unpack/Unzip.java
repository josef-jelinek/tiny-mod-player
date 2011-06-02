package player.tinymod.unpack;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import android.util.Log;

public final class Unzip {
  private static final byte[] Empty = new byte[0];
  private final List<byte[]> blocks = new ArrayList<byte[]>();
  private final List<String> names = new ArrayList<String>();

  public void read(final InputStream in) {
    final ZipInputStream zip = new ZipInputStream(in);
    try {
      do {} while (processNextEntry(zip));
      zip.close();
    } catch (final Exception e) {
      Log.w("tinymod Unzip", e);
    }
  }

  private boolean processNextEntry(final ZipInputStream zip) {
    try {
      final ZipEntry entry = zip.getNextEntry();
      if (entry == null)
        return false;
      if (!entry.isDirectory()) {
        names.add(entry.getName());
        blocks.add(readEntryData(zip, entry));
      }
      return true;
    } catch (final Exception e) {
      Log.w("tinymod Unzip", e);
      return false;
    }
  }

  private byte[] readEntryData(final ZipInputStream zip, final ZipEntry entry) throws IOException {
    try {
      final int size = (int)entry.getSize();
      if (size < 0)
        return Empty;
      final byte[] b = new byte[size];
      int p = 0;
      while (p < size)
        p += zip.read(b, p, size - p);
      return b;
    } catch (final Exception e) {
      Log.w("tinymod Unzip", e);
      return Empty;
    }
  }

  public int size() {
    return names.size();
  }

  public byte[] getData(final int i) {
    return blocks.get(i);
  }

  public String getName(final int i) {
    return names.get(i);
  }
}
