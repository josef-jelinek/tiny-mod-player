package player.tinymod;

public interface Player {
  public void play(Mod mod);
  public void stop();
  public void pause(boolean state);
  public boolean playing();
  public void loop(boolean state);
  public AudioControl audio();
  public void process(int[] left, int[] right, int size);
}
