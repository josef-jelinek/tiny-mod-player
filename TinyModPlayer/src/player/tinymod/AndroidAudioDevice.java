package player.tinymod;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public final class AndroidAudioDevice implements AudioDevice {
  private final int sampleRateInHz;
  private final AudioTrack track;

  public AndroidAudioDevice(final int sampleRateInHz) {
    this.sampleRateInHz = sampleRateInHz;
    final int minSize =
        AudioTrack.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT);
    track =
        new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
    track.play();
  }

  public int getSampleRateInHz() {
    return sampleRateInHz;
  }

  public void write(final short[] samples) {
    track.write(samples, 0, samples.length);
  }
}
