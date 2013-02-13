package gamod.system;

import gamod.Mod;
import gamod.audio.DesktopAudioDevice;
import gamod.format.*;
import gamod.player.ModPlayer;
import gamod.unpack.*;
import java.io.*;
import java.util.*;

public final class Start {
  final ModPlayer player = new ModPlayer(new DesktopAudioDevice(44100), true);
  private static final List<Parser> parsers = new ArrayList<Parser>();
  private static final List<Unpacker> unpackers = new ArrayList<Unpacker>();
  static {
    parsers.add(new ParserAhx());
    parsers.add(new ParserMed());
    parsers.add(new ParserMod());
    unpackers.add(new PowerPacker());
    unpackers.add(new XpkSqsh());
  }

  private void playSong(String filePath) {
    File file = new File(filePath);
    byte[] data = new byte[(int)file.length()];
    try {
      FileInputStream in = new FileInputStream(file);
      if (in.read(data) != data.length) {
        in.close();
        System.out.println("Not all bytes read");
        return;
      }
      in.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }
    String packerName = "";
    for (Unpacker unpacker : unpackers) {
      if (unpacker.test(data)) {
        byte[] a = unpacker.unpack(data);
        if (a == null) {
          System.out.println("Failed to unpack as " + unpacker.name());
        } else {
          packerName = unpacker.name();
          data = a;
        }
      }
    }
    for (Parser parser : parsers) {
      if (parser.test(data)) {
        Mod mod = parser.parse(data);
        if (mod == null) {
          System.out.println("Failed to load as " + parser.name());
        } else {
          mod.packer = packerName;
          playLoop(mod, file.getName());
          return;
        }
      }
    }
    System.out.println("Unknown format");
  }

  private Thread playThread = null;

  private synchronized void playLoop(Mod mod, String name) {
    player.play(mod);
    playThread = new Thread(new Runnable() {
      public void run() {
        try {
          while (player.isActive()) {
            if (player.isPaused())
              Thread.sleep(50);
            if (Thread.interrupted())
              player.stop();
            else
              player.mix();
          }
        } catch (final InterruptedException e) {
          player.stop();
        }
      }
    });
    playThread.setPriority(Thread.MAX_PRIORITY);
    playThread.start();
  }

  public static void main(String[] args) throws InterruptedException {
    Start p = new Start();
    p.playSong("c:\\Users\\Lemur\\Documents\\josef-jelinek\\Amiga Mods\\GOLDRUNNER.MOD");
    if (p.playThread != null)
      p.playThread.join();
  }
}
