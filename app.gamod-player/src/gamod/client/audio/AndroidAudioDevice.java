package gamod.client.audio;

import gamod.player.AudioDevice;
import android.media.*;

public final class AndroidAudioDevice implements AudioDevice {
  private static final int music = AudioManager.STREAM_MUSIC;
  private static final int stream = AudioTrack.MODE_STREAM;
  private static final int stereo = AudioFormat.CHANNEL_OUT_STEREO;
  private static final int pcm16b = AudioFormat.ENCODING_PCM_16BIT;
  private final int sampleRateInHz;
  private final AudioTrack track;

  public AndroidAudioDevice(int sampleRateInHz) {
    this.sampleRateInHz = sampleRateInHz;
    int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, stereo, pcm16b);
    track = new AudioTrack(music, sampleRateInHz, stereo, pcm16b, bufferSize, stream);
  }

  public int getSampleRateInHz() {
    return sampleRateInHz;
  }

  public void write(short[] samples) {
    track.write(samples, 0, samples.length);
  }

  public void play() {
    track.play();
  }

  public void pause() {
    track.pause();
  }

  public void stop() {
    track.stop();
  }
}
