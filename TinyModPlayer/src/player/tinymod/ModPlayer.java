package player.tinymod;

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
  private int tpl;
  private int tpb;
  private int bpm;
  private int tps;
  private int tick;
  private int line;
  private int pos;
  private int jumpLine;
  private int jumpPos;
  private boolean jump;
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
      active = true;
      paused = false;
    }
  }

  public synchronized void stop() {
    active = false;
  }

  public void pause() {
    paused = true;
  }

  public void resume() {
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
        bytesLeft = audioDevice.getSampleRateInHz() / tps;
        if (tick == 0 && blockDelay > 0)
          blockDelay--;
        else if (tick == 0)
          doLine();
        for (int i = 0; i < mod.tracks; i++)
          track[i].doEffects(!mod.doFirstLineTick && tick == 0);
        tick = (tick + 1) % tpl;
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
    tpl = mod.ticksPerLine;
    tpb = mod.linesPerBeat > 0 ? tpl * mod.linesPerBeat : mod.ticksPerBeat;
    bpm = mod.beatsPerMinute;
    tps = tpb * bpm / 60;
    tick = 0;
    pos = 0;
    line = 0;
    jumpLine = 0;
    jumpPos = 0;
    jump = false;
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
    final Block block = mod.blocks[mod.order[pos]];
    if (line == 0)
      Log.d("tinymod", ">> " + pos + "(" + mod.order[pos] + ")");
    Log.d("tinymod", "" + line / 100 % 10 + line / 10 % 10 + line % 10 + block.lineString(line));
    for (int i = 0; i < block.tracks(line); i++)
      track[i].doTrack(block.note(line, i)); // update sound (volume and pitch)
    for (int i = 0; i < block.tracks(line); i++)
      doTrack(block.note(line, i)); // update control (global)
    nextPos();
    for (int i = 0; i < block.tracks(line); i++)
      track[i].checkHold(block.note(line, i), tpl);
  }

  private void doTrack(final Note note) {
    final int efxy = note.paramX * 16 + note.paramY;
    switch (note.effect) {
    case 0x0B: // jump to block
      jump(efxy == 0 ? pos + 1 : efxy, 0);
      break;
    case 0x0D: // block break (to the specified line) (originally dec - converted to hex)
      jump(pos + 1, efxy);
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
        tpl = Math.max(1, efxy); // default 6 ticks/line
        tpb = mod.linesPerBeat > 0 ? tpl * mod.linesPerBeat : mod.ticksPerBeat;
      } else
        bpm = efxy; // default 50 tps ~ 125 bpm
      tps = tpb * bpm / 60;
      break;
    }
  }

  private void nextPos() {
    if (jump) {
      if (!loop && cycleTimes == 0 && (jumpPos < pos || jumpPos == pos && jumpLine <= line)) {
        reset();
        active = false;
        return;
      }
      if (jumpPos != pos) {
        cycleLine = 0;
        cycleTimes = 0;
      }
      pos = jumpPos;
      line = jumpLine;
      jump = false;
    } else
      line++;
    while (pos < mod.songLength && line >= mod.blocks[mod.order[pos]].lines()) {
      line -= mod.blocks[mod.order[pos]].lines();
      pos++;
      cycleLine = 0;
      cycleTimes = 0;
    }
    if (pos >= mod.songLength) {
      reset();
      active = loop;
    }
  }

  private void jump(final int jPos, final int jLine) {
    if (jump)
      return;
    jumpLine = jLine;
    jumpPos = jPos;
    jump = true;
  }

  private void cycle(final int counter) {
    if (cycleTimes == 0 && counter == 0)
      cycleLine = line;
    else if (cycleTimes == 0) { // first iteration (counter > 0)
      cycleTimes = counter;
      jump(pos, cycleLine);
    } else if (counter > 0) { // cycle update (cycleTimes > 0)
      cycleTimes--;
      if (cycleTimes > 0)
        jump(pos, cycleLine);
      else
        cycleLine = line + 1; // set loop start to prevent infinite loops S3M/IT
    }
  }
}
