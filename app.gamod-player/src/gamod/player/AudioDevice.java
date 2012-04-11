package gamod.player;

public interface AudioDevice {
  public int getSampleRateInHz();
  public void write(short[] samples);
}
