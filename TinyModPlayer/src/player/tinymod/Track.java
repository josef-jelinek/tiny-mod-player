package player.tinymod;

public final class Track {
  private static final int NONE = -1;
  private static final int ARPEGGIO = 0;
  private static final int PORTA_UP = 1;
  private static final int PORTA_DOWN = 2;
  private static final int PORTA = 3;
  private static final int VIBRATO = 4;
  private static final int PORTA_VOLUME_SLIDE = 5;
  private static final int VIBRATO_VOLUME_SLIDE = 6;
  private static final int TREMOLO = 7;
  private static final int VOLUME_SLIDE = 8;
  private static final int RETRIG_NOTE = 9;
  private static final int CUT_NOTE = 10;
  private static final int DELAY_NOTE = 11;
  private static final int DELAY_RETRIG_NOTE = 12;
  private final Sound sound;
  private Instrument nextSample;
  private int nextVolume;
  private boolean stopNote;
  private int volume;
  private int volumeSave;
  private int delay;
  private int nextHold;
  private int nextDecay;
  private int hold;
  private int decay;
  private int fade;
  private int nextKey;
  private int retrigKey;
  private int retrigVolume;
  private int effect;
  private int paramX;
  private int paramY;
  private int effectCounter;
  private final int[] arpeggioNotes = new int[3];
  private int portaSpeed;
  private int portaKey;
  private boolean glissando;
  private int vibratoWaveform;
  private int vibratoSpeed;
  private int vibratoDepth;
  private int vibratoIndex;
  private int tremoloWaveform;
  private int tremoloSpeed;
  private int tremoloDepth;
  private int tremoloIndex;
  private int retrigSpeed;

  public Track(final long stepForPeriod, final int left, final int right) {
    sound = new Sound(stepForPeriod, left, right);
    nextSample = null;
    nextVolume = -1;
    stopNote = false;
    volume = 0;
    volumeSave = 0;
    delay = 0;
    nextHold = -1;
    nextDecay = 0;
    hold = -1;
    decay = 0;
    fade = 0;
    nextKey = 0;
    retrigKey = 0;
    retrigVolume = -1;
    effect = NONE;
    paramX = 0;
    paramY = 0;
    effectCounter = 0;
    arpeggioNotes[0] = -1;
    portaSpeed = 0;
    portaKey = 0;
    glissando = false;
    vibratoWaveform = 0;
    vibratoSpeed = 0;
    vibratoDepth = 0;
    vibratoIndex = 0;
    tremoloWaveform = 0;
    tremoloSpeed = 0;
    tremoloDepth = 0;
    tremoloIndex = 0;
    retrigSpeed = 0;
  }

  public void mix(final int[] lBuf, final int[] rBuf, final int from, final int to,
      final boolean filter) {
    sound.mix(lBuf, rBuf, from, to, filter, volume * 8);
  }

  public void doTrack(final Note note) {
    effect = NONE;
    restoreTone();
    final int eff = note.effect;
    final int efx = paramX = note.paramX;
    final int efy = paramY = note.paramY;
    final int efxy = efx * 16 + efy;
    nextVolume = -1;
    if (note.sample != null) {
      nextSample = note.sample;
      nextVolume = nextSample.volume;
      nextHold = nextSample.hold == 0 ? -1 : nextSample.hold;
      nextDecay = nextSample.decay;
    }
    nextKey = 0;
    if (note.key == 128) // stop note
      stopNote = true;
    if (eff == 0x3 || eff == 0x5)
      switchInstrument(); // only change instrument and volume if appropriate
    else {
      nextKey = note.key;
      if ((eff != 0xED || efxy == 0) && eff != 0xFD)
        playNote(); // not portamente to note or delay note or pitch change -> play note
    }
    switch (eff) {
    case 0x00: // arpeggio - chord simulation
      if (efxy != 0) {
        if (note.key > 0)
          arpeggioNotes[0] = note.key;
        if (arpeggioNotes[0] >= 0) {
          effect = ARPEGGIO;
          effectCounter = 0;
          arpeggioNotes[1] = arpeggioNotes[0] + efx;
          arpeggioNotes[2] = arpeggioNotes[0] + efy;
        }
      }
      break;
    case 0x01: // portamente up
      effect = PORTA_UP;
      break;
    case 0x02: // portamente down
      effect = PORTA_DOWN;
      break;
    case 0x03: // portamente to note
      effect = PORTA;
      if (efxy != 0)
        portaSpeed = efxy;
      if (note.key > 0 && note.key < 128)
        portaKey = note.key;
      break;
    case 0x04: // shallower vibrato - oscilate pitch (half change)
    case 0x14: // deeper vibrato (from MED and old N.T.)
      effect = VIBRATO;
      if (efx != 0)
        vibratoSpeed = efx;
      if (efy != 0)
        vibratoDepth = efy * (eff == 0x4 ? 2 : 4);
      break;
    case 0x05: // portamente to note + volume slide (param. is for volume)
      effect = PORTA_VOLUME_SLIDE;
      if (note.key > 0 && note.key < 128)
        portaKey = note.key;
      break;
    case 0x06: // vibrato + volume slide (param. is for volume)
      effect = VIBRATO_VOLUME_SLIDE;
      break;
    case 0x07: // tremolo - oscilate volume
      effect = TREMOLO;
      if (efx != 0)
        tremoloSpeed = efx;
      if (efy != 0)
        tremoloDepth = 4 * efy;
      break;
    case 0x08: // pan
      if (efxy == 0xA4) // surround
        sound.pan(-128, 128);
      else {
        final int pan = Tools.crop(efxy, 0, 128) * 2;
        sound.pan(256 - pan, pan);
      }
      break;
    case 0x09: // sample offset - start skip
      sound.playFrom(efxy * 256);
      break;
    case 0x0A: // volume slide
      effect = VOLUME_SLIDE;
      break;
    // G 0x0B - jump to order
    case 0x0C: // set volume
      if (efxy >= 128) // changing instrument default volume (from MED)
        sound.instrumentVolume(efxy - 128);
      volume(efxy);
      break;
    // G 0x0D - pattern break (to specified row)
    // G 0x0F - set speed / bpm
    case 0x18: // decay + hold (from MED)
      decay = nextDecay = efx;
      hold = nextHold = efy;
      fade = 0;
      break;
    case 0x1E: // set synth waveform position (from MED)
      sound.synthWf(efxy);
      break;
    case 0x1F: // delay + retrigger note (from MED)
      if (efxy != 0) {
        if (efx == 0)
          effect = RETRIG_NOTE;
        else if (efy == 0)
          effect = DELAY_NOTE;
        else
          effect = DELAY_RETRIG_NOTE;
        delay = efx;
        effectCounter = retrigSpeed = efy;
      }
      break;
    // G 0xE0 - amiga filter
    case 0xE1: // fine portamente up
      sound.modPeriod(-efxy);
      break;
    case 0xE2: // fine portamente down
      sound.modPeriod(efxy);
      break;
    case 0xE3: // glissando - portamente in semitones / smooth
      glissando = efxy != 0;
      break;
    case 0xE4: // vibrato waveform
      vibratoWaveform = efy;
      if (efxy < 4) // retrigger
        vibratoIndex = 0;
      break;
    case 0xE5: // set finetune
      sound.fineTune(efxy);
      break;
    // G 0xE6: // pattern loop
    case 0xE7: // tremolo waveform
      tremoloWaveform = efxy;
      if (efxy < 4) // retrigger
        tremoloIndex = 0;
      break;
    case 0xE8: // 16 position panning
      final int pan = efxy * 256 / 15;
      sound.pan(256 - pan, pan);
    case 0xE9: // retrigger note
      if (efxy != 0) {
        effect = RETRIG_NOTE;
        effectCounter = retrigSpeed = efxy;
        hold = nextHold;
        fade = 0;
      }
      break;
    case 0xEA: // fine volume slide up
      volume(volume + efxy);
      break;
    case 0xEB: // fine volume slide down
      volume(volume - efxy);
      break;
    case 0xEC: // cut note
      effect = CUT_NOTE;
      effectCounter = efxy;
      break;
    case 0xED: // delay note
      if (efxy != 0) {
        effect = DELAY_NOTE;
        delay = efxy;
      }
      break;
    // G 0xEE - pattern delay
    // X 0xEF - invert loop - only a fuzzy idea about this
    case 0xFD: // change frequency (from MED)
      if (note.key > 0 && note.key < 128)
        sound.setKeyPeriod(note.key);
      break;
    }
  }

  public void checkHold(final Note note, final int ticks) { // called right after doTrack with the new note
    if (hold >= 0)
      if (note.hold || note.key > 0 && note.key < 128 &&
          (note.effect == 0x03 || note.effect == 0x05))
        hold += ticks;
    if (stopNote)
      hold = 0;
    stopNote = false;
  }

  public void doEffects(final boolean newLine) { // should be called each tick (after doTrack and chechHold if new line)
    restoreTone();
    doDecay();
    if (effect == DELAY_NOTE || effect == DELAY_RETRIG_NOTE) { // delay note
      if (delay > 0)
        delay--;
      else {
        effect = effect == DELAY_NOTE ? NONE : RETRIG_NOTE;
        playNote();
      }
    } else if (effect == RETRIG_NOTE) { // retrigger note
      if (effectCounter > 0)
        effectCounter--;
      else {
        nextKey = retrigKey;
        nextVolume = retrigVolume;
        playNote();
        effectCounter = retrigSpeed;
      }
    } else if (effect == CUT_NOTE) { // cut note
      if (effectCounter > 0)
        effectCounter--;
      else
        volume(0);
    } else if (effect == ARPEGGIO) { // arpeggio
      sound.setKeyPeriod(arpeggioNotes[effectCounter]);
      effectCounter = (effectCounter + 1) % 3;
    } else if (effect == TREMOLO) { // tremolo
      tempVolume(volume + Sound.waveform(tremoloWaveform & 3, tremoloIndex) * tremoloDepth / 255);
      tremoloIndex = (tremoloIndex + tremoloSpeed) % 63;
    } else if (effect == PORTA_UP) { // portamente up
      if (!newLine)
        sound.modPeriod(-(paramX * 16 + paramY));
    } else if (effect == PORTA_DOWN)
      if (!newLine)
        sound.modPeriod(paramX * 16 + paramY);
    if (effect == VIBRATO || effect == VIBRATO_VOLUME_SLIDE) { // vibrato (+ volume slide)
      sound.vibrato(vibratoWaveform & 3, vibratoIndex, vibratoDepth);
      vibratoIndex = (vibratoIndex + vibratoSpeed) % 63;
    }
    if (effect == VOLUME_SLIDE || effect == PORTA_VOLUME_SLIDE || effect == VIBRATO_VOLUME_SLIDE)
      if (!newLine)
        volume(volume + paramX - paramY);
    if (effect == PORTA || effect == PORTA_VOLUME_SLIDE)
      if (!newLine)
        sound.toKey(portaKey, portaSpeed, glissando);
    sound.update();
  }

  private void doDecay() {
    if (hold >= 0 && --hold < 0)
      if (!sound.decay(decay))
        if ((fade = decay) == 0)
          volume(0);
    volume(volume - fade);
    //fade = Math.min(fade, volume);
  }

  private void playNote() {
    retrigKey = nextKey;
    retrigVolume = nextVolume;
    if (nextKey > 0 && nextKey < 128) {
      sound.play(nextKey, nextSample);
      nextSample = null;
      nextKey = 0;
      hold = nextHold;
      decay = nextDecay;
      fade = 0;
      if (nextVolume >= 0)
        volume(nextVolume);
    } else if (nextVolume >= 0 && nextHold < 0)
      volume(nextVolume);
    nextVolume = -1;
  }

  private void switchInstrument() {
    if (nextVolume >= 0 && nextHold < 0)
      volume(nextVolume);
    nextVolume = -1;
    sound.switchTo(nextSample);
    nextSample = null;
  }

  private void volume(final int vol) {
    volume = volumeSave = Tools.crop(vol, 0, 64);
  }

  private void tempVolume(final int vol) {
    volume = Tools.crop(vol, 0, 64);
  }

  private void restoreTone() {
    volume = volumeSave;
    sound.restorePeriod();
  }
}
