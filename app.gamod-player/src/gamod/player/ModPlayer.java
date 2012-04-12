package gamod.player;

import gamod.Note;
import gamod.tools.*;
import android.util.Log;

public final class ModPlayer {
  private static final int BUFFERS_PER_SECOND = 20;
  private final short[] mixBuffer;
  private final int[] left;
  private final int[] right;
  private final AudioDevice audioDevice;
  private Mod mod;
  private AudioTrack[] track;
  private final long samplingStepForPeriod;
  private boolean active = false;
  private boolean paused = false;
  private final boolean loop = false;
  private boolean filter;
  private int ticksPerLine;
  private int ticksPerBeat;
  private int beatsPerMinute;
  private int ticksPerSecond;
  private int tick;
  private int line;
  private int blockIndex;
  private int jumpLine;
  private int jumpBlockIndex;
  private boolean isSetToJump;
  private int cycleLine;
  private int cycleTimes;
  private int blockDelay;
  private int bytesLeft;

  public ModPlayer(final AudioDevice audioDevice, final boolean pal) {
    this.audioDevice = audioDevice;
    samplingStepForPeriod = Period.getSamplingStepForPeriod(audioDevice.getSampleRateInHz(), pal);
    final int bufferSize = audioDevice.getSampleRateInHz() / BUFFERS_PER_SECOND;
    left = new int[bufferSize];
    right = new int[bufferSize];
    mixBuffer = new short[bufferSize * 2];
  }

  public synchronized void play(final Mod mod) {
    if (!active) {
      this.mod = mod;
      reset();
      audioDevice.play();
      active = true;
      paused = false;
    }
  }

  public synchronized void stop() {
    audioDevice.stop();
    active = false;
  }

  public void pause() {
    audioDevice.pause();
    paused = true;
  }

  public void resume() {
    audioDevice.play();
    paused = false;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isPaused() {
    return paused;
  }

  public void mix() {
    if (!active || paused)
      return;
    for (int i = 0; i < left.length; i++)
      left[i] = right[i] = 0;
    process(left, right, left.length);
    for (int i = 0; i < left.length; i++) {
      mixBuffer[i * 2] = (short)Tools.crop(left[i], -32768, 32767);
      mixBuffer[i * 2 + 1] = (short)Tools.crop(right[i], -32768, 32767);
    }
    audioDevice.write(mixBuffer);
  }

  private void process(final int[] left, final int[] right, final int size) {
    int p = 0;
    int todo = size;
    while (todo > 0) {
      if (bytesLeft == 0) {
        bytesLeft = audioDevice.getSampleRateInHz() / ticksPerSecond;
        if (tick == 0 && blockDelay > 0)
          blockDelay--;
        else if (tick == 0)
          doLine();
        for (int i = 0; i < mod.tracks; i++)
          track[i].doEffects(!mod.doFirstLineTick && tick == 0);
        tick = (tick + 1) % ticksPerLine;
      }
      final int amount = Math.min(bytesLeft, todo);
      for (int i = 0; i < mod.tracks; i++)
        track[i].mix(left, right, p, p + amount, filter);
      bytesLeft -= amount;
      p += amount;
      todo -= amount;
    }
  }

  private void reset() {
    filter = mod.filter;
    ticksPerLine = mod.ticksPerLine;
    ticksPerBeat = mod.linesPerBeat > 0 ? ticksPerLine * mod.linesPerBeat : mod.ticksPerBeat;
    beatsPerMinute = mod.beatsPerMinute;
    ticksPerSecond = ticksPerBeat * beatsPerMinute / 60;
    tick = 0;
    blockIndex = 0;
    line = 0;
    jumpLine = 0;
    jumpBlockIndex = 0;
    isSetToJump = false;
    cycleLine = 0;
    cycleTimes = 0;
    blockDelay = 0;
    bytesLeft = 0;
    if (track == null || track.length != mod.tracks)
      track = new AudioTrack[mod.tracks];
    for (int i = 0; i < mod.tracks; i++) {
      final boolean l = i % 4 == 0 || i % 4 == 3;
      track[i] = new AudioTrack(samplingStepForPeriod, l ? 192 : 64, l ? 64 : 192);
    }
  }

  private void doLine() {
    final Pattern block = mod.patterns[mod.patternOrder[blockIndex]];
    if (line == 0)
      Log.d("MOD", "#" + blockIndex + " (" + mod.patternOrder[blockIndex] + ")");
    Log.d("MOD", block.rowString(line));
    for (int i = 0; i < block.tracks(); i++) {
      track[i].doTrack(block.getNote(i, line), mod.instruments); // update sound (volume and pitch)
      doTrack(block.getNote(i, line)); // update control (global)
    }
    nextPos();
    for (int i = 0; i < block.tracks(); i++)
      track[i].checkHold(block.getNote(i, line), ticksPerLine);
  }

  private void doTrack(long note) {
    final int efxy = Note.getParam(note);
    switch (Note.getEffect(note)) {
    case 0x0B: // jump to block
      jump(efxy == 0 ? blockIndex + 1 : efxy, 0);
      break;
    case 0x0D: // block break (to the specified line) (originally dec - converted to hex)
      jump(blockIndex + 1, efxy);
      break;
    case 0xE0: // amiga filter
      filter = efxy != 0;
      break;
    case 0xE6: // block cycle
      cycle(efxy);
      break;
    case 0xEE: // block delay
      blockDelay = efxy + 1; // ?
      break;
    case 0x0F: // set tpl / bpm
      if (efxy <= 32) { // set ticks/line (can be < 32)
        ticksPerLine = Math.max(1, efxy); // default 6 ticks/line
        ticksPerBeat = mod.linesPerBeat > 0 ? ticksPerLine * mod.linesPerBeat : mod.ticksPerBeat;
      } else
        beatsPerMinute = efxy; // default 50 tps ~ 125 bpm
      ticksPerSecond = ticksPerBeat * beatsPerMinute / 60;
      break;
    }
  }

  private void nextPos() {
    if (isSetToJump) {
      if (!loop && cycleTimes == 0 &&
          (jumpBlockIndex < blockIndex || jumpBlockIndex == blockIndex && jumpLine <= line)) {
        reset();
        active = false;
        return;
      }
      if (jumpBlockIndex != blockIndex) {
        cycleLine = 0;
        cycleTimes = 0;
      }
      blockIndex = jumpBlockIndex;
      line = jumpLine;
      isSetToJump = false;
    } else
      line++;
    while (blockIndex < mod.songLength &&
        line >= mod.patterns[mod.patternOrder[blockIndex]].rows()) {
      line -= mod.patterns[mod.patternOrder[blockIndex]].rows();
      blockIndex++;
      cycleLine = 0;
      cycleTimes = 0;
    }
    if (blockIndex >= mod.songLength) {
      reset();
      active = loop;
    }
  }

  private void jump(final int block, final int line) {
    if (isSetToJump)
      return;
    jumpLine = line;
    jumpBlockIndex = block;
    isSetToJump = true;
  }

  private void cycle(final int counter) {
    if (cycleTimes == 0 && counter == 0)
      cycleLine = line;
    else if (cycleTimes == 0) { // first iteration (counter > 0)
      cycleTimes = counter;
      jump(blockIndex, cycleLine);
    } else if (counter > 0) { // cycle update (cycleTimes > 0)
      cycleTimes--;
      if (cycleTimes > 0)
        jump(blockIndex, cycleLine);
      else
        cycleLine = line + 1; // set loop start to prevent infinite loops S3M/IT
    }
  }

  @Override
  public String toString() {
    if (mod == null)
      return "";
    String s = "tracker: " + mod.tracker + "\r\n";
    if (!mod.packer.equals(""))
      s += "packer: " + mod.packer + "\r\n";
    s += "title: " + mod.title + "\r\n";
    s += "channels: " + mod.tracks;
    return s;
  }
}
