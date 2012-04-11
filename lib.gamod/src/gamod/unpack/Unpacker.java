package gamod.unpack;

public interface Unpacker {
  String name();
  boolean test(byte[] data);
  byte[] unpack(byte[] data);
}
