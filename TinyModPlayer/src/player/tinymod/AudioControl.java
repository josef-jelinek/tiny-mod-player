package player.tinymod;

public final class AudioControl {
  public boolean linear;
  public boolean filter;
  public int volume;
  public int boost;

  public AudioControl() {
    reset();
  }

  public void reset() {
    this.volume = 64;
    this.boost = 4;
    this.linear = true;
    this.filter = false;
  }
}
