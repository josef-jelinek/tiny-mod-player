package player.tinymod.format;

import static player.tinymod.format.ModFormat.Type.*;
import static player.tinymod.io.ByteReader.nulChar;
import static player.tinymod.tools.Tools.digit;
import player.tinymod.*;
import player.tinymod.io.ByteReader;
import player.tinymod.tools.Tools;
import android.util.Log;

public final class ParserMod implements Parser {
  private static final String ustText = "Ultimate Soundtracker";
  private static final String ntText = "NoiseTracker";
  private static final String ptText = "Protracker";
  private static final String trekkerText = "StarTrekker";
  private static final String ftText = "FastTracker";
  private static final String ttText = "TakeTracker";

  public String name() {
    return "Original Mod";
  }

  public boolean test(byte[] data) {
    ByteReader reader = new ByteReader(data);
    reader.seek(1080);
    if (reader.available() < 4)
      return false;
    String id = reader.string(4); // read identification
    return checkType(id) != null || testLegacy(reader) != null;
  }

  public Mod parse(byte[] data) {
    ByteReader reader = new ByteReader(data);
    reader.seek(1080);
    if (reader.available() < 4)
      return null;
    String id = reader.string(4); // read identification
    ModFormat format = checkType(id);
    if (format == null)
      format = testLegacy(reader);
    if (format == null)
      return null;
    reader.seek(0);
    String title = reader.string(20); // read title
    Instrument[] instruments = readInstruments(reader, format);
    format = updateDescription(format, id, instruments);
    int songLength = reader.u1();
    reader.skip(1); // magic byte
    int[] order = readPatternOrder(reader);
    if (format.type == trekker && format.tracks == 8)
      format = format.changeTracks(adjustTrekker8Order(order));
    int totalPatterns = countPatterns(order, songLength);
    if (!format.isLegacy())
      reader.skip(4); // skip id
    Pattern[] patterns = readPatterns(reader, totalPatterns, format, instruments);
    if (patterns == null)
      return null;
    readInstrumentSamples(reader, instruments);
    Mod mod = new Mod(format.tracks);
    mod.tracker = format.description;
    mod.title = Tools.trimEnd(title, nulChar);
    mod.instruments = instruments;
    mod.songLength = songLength;
    mod.patternOrder = order;
    mod.patterns = patterns;
    return mod;
  }

  private ModFormat updateDescription(ModFormat format, String id, Instrument[] instruments) {
    for (int i = 0; i < instruments.length; i++)
      if (format.type == ft_orpheus && instruments[i].is16bit)
        return format.changeDescription("Imago Orpheus (" + id + ")");
    return format;
  }

  private static Pattern[] readPatterns(ByteReader reader, int n, ModFormat format, Instrument[] ins) {
    Pattern[] patterns = new Pattern[n];
    for (int i = 0; i < n; i++) {
      if (format.type == trekker && format.tracks == 8) {
        Pattern pattern1 = readPattern(reader, format.type, 4, ins);
        Pattern pattern2 = readPattern(reader, format.type, 4, ins);
        if (pattern1 == null || pattern2 == null)
          return null;
        patterns[i] = new Pattern(8, 64);
        for (int row = 0; row < 64; row++) {
          for (int track = 0; track < 4; track++) {
            patterns[i].setNote(track, row, pattern1.getNote(track, row));
            patterns[i].setNote(track + 4, row, pattern2.getNote(track, row));
          }
        }
      } else {
        patterns[i] = readPattern(reader, format.type, format.tracks, ins);
        if (patterns[i] == null)
          return null;
      }
    }
    return patterns;
  }

  private static Pattern readPattern(ByteReader reader, ModFormat.Type type, int tracks, Instrument[] ins) {
    if (reader.available() < 64 * tracks * 4)
      return null;
    Pattern block = new Pattern(tracks, 64);
    for (int row = 0; row < 64; row++) {
      for (int track = 0; track < tracks; track++) {
        int b0 = reader.u1();
        int b1 = reader.u1();
        int b2 = reader.u1();
        int b3 = reader.u1();
        int i = (b0 & 0xF0 | (b2 & 0xF0) >> 4) - 1;
        int period = (b0 << 8 | b1) & 0xFFF;
        int key = Period.getKeyForPeriod(period * 100);
        int effect = type == ust ? ustEffect(b2 & 0xF, b3) : b2 & 0xF;
        int param = type == ust ? ustEffectParam(b2 & 0xF, b3) : b3;
        int paramX = param >> 4;
        int paramY = param & 15;
        if (effect == 0x0E) { // extended commands
          effect = effect << 4 | paramX;
          paramX = 0;
        }
        if (effect == 0x0D) { // pattern break dec to hex
          int dec = paramX * 10 + paramY;
          paramX = dec >> 4;
          paramY = dec & 15;
        }
        boolean isOutOfRange = i < 0 || i >= ins.length;
        Instrument instrument = isOutOfRange ? null : ins[i];
        block.setNote(track, row, new Note(key, instrument, effect, paramX, paramY, false));
      }
    }
    return block;
  }

  private static int ustEffect(int effect, int param) {
    if (effect == 1)
      return 0;
    if (effect == 2 && (param & 0xF) != 0)
      return 1;
    if (effect == 2 && (param >> 4 & 0xF) != 0)
      return 2;
    if (effect != 0 && effect != 2 && effect != 3)
      return effect;
    return 0;
  }

  private static int ustEffectParam(int effect, int param) {
    if (effect == 1)
      return param;
    if (effect == 2 && (param & 0xF) != 0)
      return param & 0xF;
    if (effect == 2 && (param >> 4 & 0xF) != 0)
      return param >> 4 & 0xF;
    if (effect != 0 && effect != 2 && effect != 3)
      return param;
    return 0;
  }

  static ModFormat checkType(String id) {
    if (id.equals("M.K.") || id.equals("M!K!"))
      return new ModFormat(pt, 4, 31, ptText + " (" + id + ")");
    if (id.equals("FLT4") || id.equals("FLT8") || id.equals("EXO4") || id.equals("EXO8"))
      return new ModFormat(trekker, digit(id, 3), 31, trekkerText + " (" + id + ")");
    if (id.equals("OKTA"))
      return new ModFormat(generic, 8, 31, "Oktalyzer" + " (" + id + ")");
    if (id.equals("CD81"))
      return new ModFormat(generic, 8, 31, "Oktalyser" + " (" + id + ")");
    if (id.endsWith("CHN") && digit(id, 0) >= 0)
      return new ModFormat(generic, digit(id, 0), 31, ftText + " (" + id + ")");
    if (id.endsWith("CH") && digit(id, 0) >= 0 && digit(id, 1) >= 0)
      return new ModFormat(ft_orpheus, number00(id), 31, ftText + " (" + id + ")");
    if (id.endsWith("CN") && digit(id, 0) >= 0 && digit(id, 1) >= 0)
      return new ModFormat(generic, number00(id), 31, ttText + " (" + id + ")");
    return null;
  }

  private static int number00(String id) {
    return digit(id, 0) * 10 + digit(id, 1);
  }

  private ModFormat testLegacy(ByteReader reader) {
    reader.seek(0);
    if (reader.available() < 20)
      return null;
    String name = reader.string(20);
    if (!checkName(name))
      return null;
    ModFormat format = new ModFormat(unknown, 4, 15, "?");
    format = checkLegacyInstruments(reader, format);
    if (format == null || reader.available() < 130)
      return null;
    int length = reader.u1();
    int magic = reader.u1(); // usually 127 or restart position for U.S.T. or N.T.
    if (length == 0 || length > 128 || magic > 127)
      return null;
    // sometimes found 0x6A and 0x78
    if ((magic & 0xF8) != 0x78 && magic != 0x6A && magic > length)
      return null;
    int[] order = readPatternOrder(reader);
    for (int i = 0; i < 128; i++)
      if (order[i] > 63 || order[i] < 0)
        return null;
    int patterns = countPatterns(order, length);
    return checkLegacyPatterns(reader, format, patterns);
  }

  private int[] readPatternOrder(ByteReader reader) {
    final int[] order = new int[128];
    for (int i = 0; i < order.length; i++)
      order[i] = reader.u1();
    return order;
  }

  private static int adjustTrekker8Order(int[] order) {
    for (int i = 0; i < 128; i++)
      if (order[i] % 2 == 1) // maybe FLT8 is FLT4 if it refers to odd patterns
        return 4;
    for (int i = 0; i < 128; i++)
      order[i] /= 2;
    return 8;
  }

  private static int countPatterns(int[] order, int count) {
    int end = 128;
    // find if unused patterns exist after count
    for (int i = count; i < 128; i++)
      if (order[i] >= 128) // found garbage
        end = count;
    int max = 0;
    for (int i = 0; i < end; i++)
      if (order[i] > max)
        max = order[i];
    return max + 1;
  }

  private static boolean checkName(String name) {
    boolean endFound = false;
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (c == ByteReader.nulChar)
        endFound = true;
      if (endFound && c != ByteReader.nulChar || c == ByteReader.badChar)
        return false;
    }
    return true;
  }

  private static Instrument[] readInstruments(ByteReader reader, ModFormat format) {
    final Instrument[] instruments = new Instrument[format.samples];
    for (int i = 0; i < instruments.length; i++)
      instruments[i] = readInstrument(reader, i, format.type);
    return instruments;
  }

  private static Instrument readInstrument(ByteReader reader, int index, ModFormat.Type type) {
    final String name = reader.string(22);
    final int length = reader.u2() * 2;
    final int finetune = reader.u1() & 15;
    final int volume = reader.u1();
    final int loopStart = reader.u2() * (type == ust ? 1 : 2);
    final int loopLength = reader.u2() * (type == ust ? 1 : 2);
    final Instrument instrument = new SampledInstrument(index + 1, length);
    instrument.name(name);
    instrument.volume(volume);
    instrument.fineTune(finetune);
    instrument.loop(loopStart, loopLength);
    instrument.is16bit = type == ft_orpheus && (volume & 0x80) != 0;
    return instrument;
  }

  private static void readInstrumentSamples(ByteReader reader, Instrument[] instruments) {
    for (int i = 0; i < instruments.length; i++) {
      for (int j = 0, n = instruments[i].data().length; j < n; j++) {
        if (reader.available() == 0) {
          instruments[i].trimTo(j);
          break;
        }
        instruments[i].data()[j] = (byte)reader.s1();
        if (instruments[i].is16bit)
          reader.skip(1); // ignore lower part of 16bit sample
      }
    }
  }

  private static ModFormat checkLegacyInstruments(ByteReader reader, ModFormat format) {
    if (reader.available() < format.samples * 30)
      return null;
    boolean isUst = false, isNt = false;
    for (int i = 0; i < format.samples; i++) {
      String name = reader.string(22);
      int length = reader.u2(); // in words
      int fineTune = reader.u1();
      int volume = reader.u1();
      int loopStart = reader.u2(); // in bytes for U.S.T. or words
      int loopLength = reader.u2(); // in bytes for U.S.T. or words
      if (!checkName(name) || fineTune > 0 || volume > 64)
        return null;
      if (length > 32768)
        return null;
      if (length > 4999 || loopStart > 9999)
        isNt = true;
      // if the loop does not fit as words, but does as bytes, this is likely a U.S.T.
      if (loopStart + loopLength > length + 2 && loopStart + loopLength <= length * 2 + 2)
        isUst = true;
    }
    return resolveLegacyType(format, isUst, isNt);
  }

  private static ModFormat checkLegacyPatterns(ByteReader reader, ModFormat format, int patterns) {
    if (reader.available() < patterns * 64 * 4 * 4)
      return null;
    boolean isUst = false, isNt = false;
    for (int pattern = 0; pattern < patterns; pattern++) {
      for (int row = 0; row < 64; row++) {
        for (int track = 0; track < 4; track++) {
          reader.skip(2);
          int effect = reader.u1();
          int params = reader.u1();
          if ((effect == 1 || effect == 2) && params >= 0x20)
            isUst = true;
          boolean isNt1 = effect == 1 && params < 3;
          boolean isNt2 = effect == 2 && params < 0x20;
          boolean isNt3 = effect == 3 && params > 0;
          if (effect == 0 || effect > 3 || isNt1 || isNt2 || isNt3)
            isNt = true;
        }
      }
    }
    return resolveLegacyType(format, isUst, isNt);
  }

  private static ModFormat resolveLegacyType(ModFormat format, boolean isUst, boolean isNt) {
    if (isNt) {
      if (isUst)
        Log.d("MOD", "U.S.T. & N.T.");
      if (format.type == ust)
        Log.d("MOD", "U.S.T. -> N.T.");
      return format.changeType(nt, ntText);
    } else if (isUst) {
      if (format.type != nt)
        return format.changeType(ust, ustText);
      Log.d("MOD", "N.T. -> U.S.T.");
    }
    return format;
  }
}
