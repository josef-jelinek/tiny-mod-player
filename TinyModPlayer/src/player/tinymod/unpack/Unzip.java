package player.tinymod.unpack;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import android.util.Log;

public final class Unzip {
  private static final byte[] Empty = new byte[0];
  private final List<Item> items = new ArrayList<Item>();

  public void read(InputStream in) {
    ZipInputStream zip = new ZipInputStream(in);
    try {
      while (processNextEntry(zip))
        ;
    } catch (Exception e) {
      Log.w("tinymod", e);
    } finally {
      try {
        zip.close();
      } catch (IOException e) {}
    }
  }

  private boolean processNextEntry(ZipInputStream zip) {
    try {
      ZipEntry entry = zip.getNextEntry();
      if (entry == null)
        return false;
      if (!entry.isDirectory())
        items.add(new Item(entry.getName(), readEntryData(zip, (int)entry.getSize())));
      return true;
    } catch (Exception e) {
      Log.w("tinymod", e);
      return false;
    }
  }

  private byte[] readEntryData(ZipInputStream zip, int size) {
    try {
      if (size < 0)
        return Empty;
      byte[] b = new byte[size];
      int p = 0;
      while (p < size)
        p += zip.read(b, p, size - p);
      return b;
    } catch (Exception e) {
      Log.w("tinymod Unzip", e);
      return Empty;
    }
  }

  public int size() {
    return items.size();
  }

  public Item getItem(int i) {
    return items.get(i);
  }

  public static class Item {
    private final String name;
    private final byte[] data;

    public Item(String name, byte[] data) {
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
