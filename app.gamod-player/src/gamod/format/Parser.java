package gamod.format;

import gamod.player.Mod;


public interface Parser {
  String name();
  boolean test(byte[] data);
  Mod parse(byte[] data);
}
