package player.tinymod.format;

import player.tinymod.Mod;


public interface Parser {
  String name();
  boolean test(byte[] data);
  Mod parse(byte[] data);
}
