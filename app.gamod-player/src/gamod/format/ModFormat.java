package gamod.format;

public final class ModFormat {
  public static enum Type {
    unknown, ust, nt, pt, trekker, ft_orpheus, generic
  }

  public final Type type;
  public final int tracks;
  public final int samples;
  public final String description;

  public ModFormat(Type type, int tracks, int samples, String description) {
    this.type = type;
    this.tracks = tracks;
    this.samples = samples;
    this.description = description;
  }

  public ModFormat changeType(Type type, String description) {
    if (type == this.type && description.equals(this.description))
      return this;
    return new ModFormat(type, tracks, samples, description);
  }

  public ModFormat changeDescription(String description) {
    if (description.equals(this.description))
      return this;
    return new ModFormat(type, tracks, samples, description);
  }

  public boolean isLegacy() {
    return type == Type.unknown || type == Type.ust || type == Type.nt;
  }

  public ModFormat changeTracks(int tracks) {
    if (tracks == this.tracks)
      return this;
    return new ModFormat(type, tracks, samples, description);
  }
}
