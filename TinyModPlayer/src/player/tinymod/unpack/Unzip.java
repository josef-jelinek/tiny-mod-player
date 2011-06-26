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
  private final List<Item> items = new ArrayList<Item>();

  public void read(final InputStream in) {
    final ZipInputStream zip = new ZipInputStream(in);
    try {
      do {} while (processNextEntry(zip));
    } catch (final Exception e) {
      Log.w("tinymod", e);
    } finally {
      try {
        zip.close();
      } catch (final IOException e) {}
    }
  }

  private boolean processNextEntry(final ZipInputStream zip) {
    try {
      final ZipEntry entry = zip.getNextEntry();
      if (entry == null)
        return false;
      if (!entry.isDirectory())
        items.add(new Item(entry.getName(), readEntryData(zip, (int)entry.getSize())));
      return true;
    } catch (final Exception e) {
      Log.w("tinymod", e);
      return false;
    }
  }

  private byte[] readEntryData(final ZipInputStream zip, final int size) throws IOException {
    try {
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
    return items.size();
  }

  public Item getItem(final int i) {
    return items.get(i);
  }

  public static class Item {
    private final String name;
    private final byte[] data;

    public Item(final String name, final byte[] data) {
      this.name = name;
      this.data = data;
    }

    public byte[] getData() {
      return data;
    }

    public String getName() {
      return name;
    }
  }
}
