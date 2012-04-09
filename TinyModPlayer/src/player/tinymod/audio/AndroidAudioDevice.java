package player.tinymod.audio;

import player.tinymod.AudioDevice;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public final class AndroidAudioDevice implements AudioDevice {
  private static final int MUSIC = AudioManager.STREAM_MUSIC;
  private static final int STREAM = AudioTrack.MODE_STREAM;
  private static final int STEREO = AudioFormat.CHANNEL_OUT_STEREO;
  private static final int PCM16BIT = AudioFormat.ENCODING_PCM_16BIT;
  private final int sampleRateInHz;
  private final AudioTrack track;

  public AndroidAudioDevice(final int sampleRateInHz) {
    this.sampleRateInHz = sampleRateInHz;
    final int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, STEREO, PCM16BIT);
    track = new AudioTrack(MUSIC, sampleRateInHz, STEREO, PCM16BIT, bufferSize, STREAM);
    track.play();
  }

  public int getSampleRateInHz() {
    return sampleRateInHz;
  }

  public void write(final short[] samples) {
    track.write(samples, 0, samples.length);
  }
}
