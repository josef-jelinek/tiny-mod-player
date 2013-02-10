package gamod.player;

import gamod.*;

public final class AudioTrack {
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
  private final AudioSound sound;
  private Instrument nextInstrument;
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
  private int currentParamX;
  private int currentParamY;
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

  public AudioTrack(final long stepForPeriod, final int left, final int right) {
    sound = new AudioSound(stepForPeriod, left, right);
    nextInstrument = null;
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
    currentParamX = 0;
    currentParamY = 0;
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

  public void mix(final int[] left, final int[] right, final int from, final int to,
      final boolean filter) {
    sound.mix(left, right, from, to, filter, volume * 16);
  }

  public void doTrack(long note, Instrument[] instruments) {
    effect = NONE;
    restoreTone();
    int key = Note.getKey(note);
    int ins = Note.getInstrument(note);
    int eff = Note.getEffect(note);
    int efxy = Note.getParam(note);
    int efx = currentParamX = efxy >> 4;
    int efy = currentParamY = efxy & 0xF;
    nextVolume = -1;
    setNextInstrument(ins < 0 || ins >= instruments.length ? null : instruments[ins]);
    nextKey = 0;
    if (key == 128) // stop note
      stopNote = true;
    if (eff == 0x3 || eff == 0x5)
      switchInstrument(); // only change instrument and volume if appropriate
    else {
      nextKey = key;
      if ((eff != 0xED || efxy == 0) && eff != 0xFD)
        playNote(); // not portamente to note or delay note or pitch change -> play note
    }
    switch (eff) {
    case 0x00: // arpeggio - chord simulation
      if (efxy != 0) {
        if (key > 0)
          arpeggioNotes[0] = key;
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
      if (key > 0 && key < 128)
        portaKey = key;
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
      if (key > 0 && key < 128)
        portaKey = key;
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
      else
        setPan(Tools.crop(efxy, 0, 128) * 2);
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
      sound.synthWaveform(efxy);
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
      setPan(efxy * 256 / 15);
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
      if (key > 0 && key < 128)
        sound.setKeyPeriod(key);
      break;
    }
  }

  private void setPan(final int pan) {
    sound.pan(256 - pan, pan);
  }

  private void setNextInstrument(final Instrument instrument) {
    if (instrument != null) {
      nextInstrument = instrument;
      nextVolume = nextInstrument.volume;
      nextHold = nextInstrument.hold == 0 ? -1 : nextInstrument.hold;
      nextDecay = nextInstrument.decay;
    }
  }

  public void checkHold(long note, int ticks) { // called right after doTrack with the new note
    if (hold >= 0)
      if (Note.isHolding(note))
        hold += ticks;
    if (stopNote)
      hold = 0;
    stopNote = false;
  }

  public void doEffects(boolean newLine) { // should be called each tick (after doTrack and chechHold if new line)
    restoreTone();
    doDecay();
    if (effect == DELAY_NOTE || effect == DELAY_RETRIG_NOTE)
      delayNote();
    else if (effect == RETRIG_NOTE)
      retriggerNote();
    else if (effect == CUT_NOTE)
      cutNote();
    else if (effect == ARPEGGIO)
      updateArpeggio();
    else if (effect == TREMOLO)
      updateTremolo();
    else if (isPortamenteUpOrDown(effect) && !newLine)
      sound.modPeriod(getPortamenteDirection(effect) * (currentParamX * 16 + currentParamY));
    if (isVibrato(effect))
      updateVibrato();
    if (isVolumeSlide(effect) && !newLine)
      volume(volume + currentParamX - currentParamY);
    if (isPortamente(effect) && !newLine)
      sound.toKey(portaKey, portaSpeed, glissando);
    sound.update();
  }

  private void updateVibrato() {
    sound.vibrato(vibratoWaveform & 3, vibratoIndex, vibratoDepth);
    vibratoIndex = (vibratoIndex + vibratoSpeed) % 63;
  }

  private void updateTremolo() {
    tempVolume(volume + AudioSound.waveform(tremoloWaveform & 3, tremoloIndex) * tremoloDepth / 255);
    tremoloIndex = (tremoloIndex + tremoloSpeed) % 63;
  }

  private void updateArpeggio() {
    sound.setKeyPeriod(arpeggioNotes[effectCounter]);
    effectCounter = (effectCounter + 1) % 3;
  }

  private void delayNote() {
    if (delay > 0)
      delay--;
    else {
      effect = effect == DELAY_NOTE ? NONE : RETRIG_NOTE;
      playNote();
    }
  }

  private void cutNote() {
    if (effectCounter > 0)
      effectCounter--;
    else
      volume(0);
  }

  private void retriggerNote() {
    if (effectCounter > 0)
      effectCounter--;
    else {
      nextKey = retrigKey;
      nextVolume = retrigVolume;
      playNote();
      effectCounter = retrigSpeed;
    }
  }

  private static boolean isPortamenteUpOrDown(int effect) {
    return effect == PORTA_UP || effect == PORTA_DOWN;
  }

  private static int getPortamenteDirection(int effect) {
    return effect == PORTA_UP ? -1 : effect == PORTA_DOWN ? 1 : 0;
  }

  private static boolean isVibrato(int effect) {
    return effect == VIBRATO || effect == VIBRATO_VOLUME_SLIDE;
  }

  private static boolean isVolumeSlide(int effect) {
    return effect == VOLUME_SLIDE || effect == PORTA_VOLUME_SLIDE || effect == VIBRATO_VOLUME_SLIDE;
  }

  private static boolean isPortamente(int effect) {
    return effect == PORTA || effect == PORTA_VOLUME_SLIDE;
  }

  private void doDecay() {
    if (hold >= 0) {
      hold--;
      if (hold < 0 && !sound.synthDecay(decay)) {
        fade = decay;
        if (fade == 0)
          volume(0);
      }
    }
    volume(volume - fade);
  }

  private void playNote() {
    retrigKey = nextKey;
    retrigVolume = nextVolume;
    if (nextKey > 0 && nextKey < 128) {
      sound.play(nextKey, nextInstrument);
      nextInstrument = null;
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
    sound.switchTo(nextInstrument);
    nextInstrument = null;
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
