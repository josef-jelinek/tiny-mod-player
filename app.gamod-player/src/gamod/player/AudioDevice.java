package gamod.player;

public interface AudioDevice {
  int getSampleRateInHz();
  void write(short[] samples);
  void play();
  void pause();
  void stop();
}
