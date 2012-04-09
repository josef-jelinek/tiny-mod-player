package player.tinymod;

public interface Parser {
  String name();
  boolean test(byte[] data);
  Mod parse(byte[] data);
}
