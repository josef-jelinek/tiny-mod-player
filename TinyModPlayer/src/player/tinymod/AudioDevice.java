package player.tinymod;

public interface AudioDevice {
  public int getSampleRateInHz();

  public void write(final short[] samples);
}
