package player.tinymod;

import gamod.Instrument;
import java.util.Arrays;

public final class Mod {
  public String title = "";
  public String tracker = "";
  public Instrument[] instruments;
  public int songLength;
  public int[] patternOrder = new int[256];
  public Pattern[] patterns;
  public int mainVolume = 64;
  public final int tracks;
  public final int[] trackVolumes;
  public boolean filter = false;
  public int beatsPerMinute = 125;
  public int linesPerBeat = 0; // = 0 if ticksPerBeat != 0
  public int ticksPerBeat = 24; // = 0 if linesPerBeat != 0
  public int ticksPerLine = 6;
  public int transpose = 0;
  public boolean doFirstLineTick = false;
  public String packer = "";

  public Mod(int tracks) {
    this.tracks = tracks;
    trackVolumes = new int[tracks];
    Arrays.fill(trackVolumes, 64);
  }
}
