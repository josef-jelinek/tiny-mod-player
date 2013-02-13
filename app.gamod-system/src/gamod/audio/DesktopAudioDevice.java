package gamod.audio;

import gamod.player.AudioDevice;
import javax.sound.sampled.*;

public final class DesktopAudioDevice implements AudioDevice {
  private static final int bufferMs = 100;
  private final int sampleRateInHz;
  private final SourceDataLine track;
  private final byte[] buffer;
  private boolean closed = false;

  public DesktopAudioDevice(int sampleRateInHz) {
    this.sampleRateInHz = sampleRateInHz;
    AudioFormat format = new AudioFormat(sampleRateInHz, 16, 2, true, true);
    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
    try {
      track = (SourceDataLine)AudioSystem.getLine(info);
      track.open(format, sampleRateInHz * bufferMs / 1000 * format.getFrameSize());
      buffer = new byte[track.getBufferSize()];
    } catch (LineUnavailableException e) {
      throw new RuntimeException(e);
    }
    track.start();
  }

  public int getSampleRateInHz() {
    return sampleRateInHz;
  }

  public void write(short[] samples) {
    checkClosed();
    int pos = 0;
    while (pos < samples.length) {
      int i;
      for (i = 0; i < buffer.length && pos < samples.length; i += 2, pos++) {
        buffer[i] = (byte)(samples[pos] >> 8);
        buffer[i + 1] = (byte)samples[pos];
      }
      track.write(buffer, 0, i);
    }
  }

  public void play() {
    checkClosed();
    track.start();
  }

  public void pause() {
    checkClosed();
    track.stop();
  }

  public void close() {
    checkClosed();
    track.close();
    closed = true;
  }

  private void checkClosed() {
    if (closed)
      throw new RuntimeException("Line already closed");
  }
}
