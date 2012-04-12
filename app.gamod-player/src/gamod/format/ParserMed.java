package gamod.format;

import static gamod.tools.Tools.crop;
import gamod.*;
import gamod.io.ByteReader;
import gamod.player.*;
import android.util.Log;

public final class ParserMed implements Parser {
  
  public String name() {
    return "OctaMED";
  }

  public boolean test(byte[] data) {
    ByteReader reader = new ByteReader(data);
    if (reader.available() < 4)
      return false;
    String id = reader.string(4); // read identification
    return id.equals("MMD0") || id.equals("MMD1");
  }

  public Mod parse(byte[] data) {
    ByteReader reader = new ByteReader(data);
    if (reader.available() < 4)
      return null;
    String id = reader.string(4); // read identification
    if (!id.equals("MMD0") && !id.equals("MMD1"))
      return null;
    return parseMed(reader, id);
  }

  private static Mod parseMed(ByteReader data, String id) {
    data.s4(); // mod length
    int song = data.s4(); // MMD0Song
    data.s4(); // reserved
    int blockarr = data.s4(); // MMD0Block[]
    data.s4(); // reserved
    int smplarr = data.s4(); // InstrHdr[]
    data.s4(); // reserved
    int expdata = data.s4(); // MMD0Exp
    data.skip(15); // 4reserved 2pstate 2pblock 2pline 2pseqnum 2actplayline 1counter
    int extrasongs = data.u1();
    if (extrasongs > 0)
      Log.i("MOD", "Extrasongs " + extrasongs);
    // process song
    if (song == 0)
      return null;
    data.seek(song);
    // MMD0Song
    data.skip(63 * 8); // skip MMD0Sample - to be read later
    Pattern[] blocks = new Pattern[data.u2()]; // length of blockarr in longwords
    int songLength = data.u2();
    int[] order = new int[256];
    for (int i = 0, j = 0; i < 256; i++) { // playseq[256]
      order[j] = data.u1();
      if (j < songLength && order[j] >= blocks.length)
        songLength--; // skip an illegal position
      else
        j++;
    }
    int tempo = Math.max(1, data.u2());
    int transpose = data.s1();
    int flags = data.u1(); // 0 - filter, 4 - hex volumes, 5 - ST/NT/PT slide, 6 - 5-8 channels
    boolean filter = (flags & 1) != 0;
    boolean hexVol = (flags & 16) != 0;
    boolean doFirstLineTick = (flags & 32) == 0;
    boolean chan5to8 = (flags & 64) != 0;
    int flags2 = data.u1(); // 0-4 lines per beat 0~1,31~32, 5 - bpm mode, 7 - !mix mode
    boolean mixMode = (flags2 & 128) == 0; // sampled instruments above 3rd octave should play as in 3rd octave
    boolean bpmMode = (flags2 & 32) != 0; // all tempos given in bpm
    int linesPerBeat = bpmMode ? (flags2 & 31) + 1 : 0; // 0 - do not use lpb - use constant tpb instead
    int ticksPerLine = crop(data.u1(), 1, 60); // ticks per line
    int beatsPerMinute = bpmMode ? tempo : tempo * 125 / 33;
    if (chan5to8) { // tempo in 5-8 channel modules was determined by a size of the mix buffer (0-9)
      bpmMode = true;
      linesPerBeat = 4;
      beatsPerMinute = tempo >= 10 ? 99 : bpm1to9[tempo - 1];
    }
    if (!bpmMode && tempo < 12) { // just guessing here...
      ticksPerLine = tempo;
      beatsPerMinute = 125;
    }
    // I hope: 50 Hz ~ standard tempo 33 if not BpM mode
    final int[] trackVolumes = new int[16];
    for (int i = 0; i < trackVolumes.length; i++)
      trackVolumes[i] = crop(data.u1(), 0, 64);
    final int mainVolume = crop(data.u1(), 0, 64);
    final Instrument[] instruments = new Instrument[data.u1()]; // length of smplarr in longwords
    // process samples - must be done before processsing blocks (sample referencing)
    if (smplarr != 0)
      for (int i = 0; i < instruments.length; i++) {
        data.seek(smplarr + 4 * i);
        final int samplep = data.s4();
        if (samplep != 0) {
          data.seek(samplep);
          instruments[i] = readMedInstrument(data, i, samplep);
        }
      }
    data.seek(song);
    for (final Instrument instrument : instruments) { // MMD0Sample[63] at the start of MMD0Song
      final int rep = data.w2();
      final int replen = data.w2();
      data.skip(2); // 1midich 1midipreset
      final int svol = crop(data.u1(), 0, 64);
      final int strans = data.s1();
      if (instrument != null) {
        instrument.loop(rep, replen);
        instrument.volume(svol);
        if (!mixMode || instrument instanceof SynthInstrument &&
            !((SynthInstrument)instrument).isHybrid())
          instrument.transpose(strans);
        else {
          instrument.transpose(strans + 2 * 12);
          instrument.minKey(2 * 12 + 1);
          instrument.maxKey(5 * 12);
        }
      }
    }
    // process blocks
    int tracks = 4;
    if (blockarr != 0)
      for (int block = 0; block < blocks.length; block++) {
        data.seek(blockarr + 4 * block);
        final int blockp = data.s4();
        if (blockp != 0) {
          data.seek(blockp);
          blocks[block] = readMedBlock(data, id, instruments, hexVol, chan5to8, bpmMode);
          tracks = Math.max(tracks, blocks[block].tracks());
        }
      }
    // process expdata
    if (expdata != 0) {
      data.seek(expdata);
      data.s4(); // next mod
      final int expSmp = data.s4();
      final int expSmps = data.u2();
      final int expSmpSz = data.u2();
      // other data is unimportant to the player
      if (expSmp != 0 && expSmpSz >= 2) {
        data.seek(expSmp);
        for (int smp = 0; smp < expSmps; smp++) {
          if (smp >= instruments.length || instruments[smp] == null) {
            data.skip(expSmpSz);
            continue;
          }
          instruments[smp].hold(data.u1());
          instruments[smp].decay(data.u1());
          if (expSmpSz < 4) {
            data.skip(expSmpSz - 2);
            continue;
          }
          data.u1(); // supress midi off
          instruments[smp].fineTune(data.s1());
          data.skip(expSmpSz - 4);
        }
      }
    }
    final Mod mod = new Mod(tracks);
    mod.tracker = "MED/OctaMed (" + id + ")";
    mod.filter = filter;
    mod.patterns = blocks;
    mod.songLength = songLength;
    mod.patternOrder = order;
    mod.instruments = instruments;
    mod.transpose = transpose;
    mod.doFirstLineTick = doFirstLineTick;
    mod.linesPerBeat = linesPerBeat;
    mod.ticksPerLine = ticksPerLine;
    mod.beatsPerMinute = beatsPerMinute;
    if (linesPerBeat != 0)
      mod.ticksPerBeat = 0;
    mod.mainVolume = mainVolume;
    for (int i = 0; i < tracks; i++)
      mod.trackVolumes[i] = trackVolumes[i];
    return mod;
  }

  private static Instrument readMedInstrument(final ByteReader data, final int index,
      final int samplep) {
    final int length = data.s4();
    final int type = data.s2();
    if (type > 0)
      Log.i("MOD", "Unsupported sample type " + type);
    if (type == 0) {
      final Instrument instrument = new SampledInstrument(index + 1, length);
      for (int i = 0; i < length; i++)
        instrument.data()[i] = (byte)data.s1();
      return instrument;
    }
    if (type == -1 || type == -2) { // synth or hybrid
      data.skip(8); // not used in modules
      final int voltbllen = data.u2();
      final int wftbllen = data.u2();
      final int volspeed = data.u1();
      final int wfspeed = data.u1();
      final int wforms = data.u2();
      final SynthInstrument instrument = new SynthInstrument(index + 1, wforms, type == -2);
      instrument.volSpeed(volspeed);
      instrument.wfSpeed(wfspeed);
      instrument.volData(voltbllen);
      for (int vol = 0; vol < voltbllen; vol++) {
        final int x = data.u1();
        instrument.volData[vol] = (byte)x;
        if (x == 0xFF) {
          data.skip(voltbllen - vol - 1);
          break;
        }
      }
      instrument.waveformData(wftbllen);
      for (int wf = 0; wf < wftbllen; wf++) {
        final int x = data.u1();
        instrument.waveformData[wf] = (byte)x;
        if (x == 0xFF) {
          data.skip(wftbllen - wf - 1);
          break;
        }
      }
      final int wfpos = data.index();
      for (int wform = 0; wform < wforms; wform++) {
        data.seek(wfpos + 4 * wform);
        final int wformp = data.s4();
        if (wformp != 0) {
          data.seek(wformp + samplep);
          if (instrument.isSynthWaveform(wform)) {
            final int ln = data.w2();
            instrument.waveform(wform, ln);
            for (int i = 0; i < ln; i++)
              instrument.data(wform)[i] = (byte)data.s1();
          } else { // sampled waveform
            final int ln = data.s4();
            final int tp = data.s2();
            if (tp != 0)
              Log.i("MOD", "Illegal type of hybrid instrument waveform - " + tp);
            instrument.waveform(wform, ln);
            for (int i = 0; i < ln; i++)
              instrument.data(wform)[i] = (byte)data.s1();
          }
        }
      }
      return instrument;
    }
    return null;
  }

  private static Pattern readMedBlock(final ByteReader data, final String id,
      final Instrument[] instruments, final boolean hexVol, final boolean chan5to8,
      final boolean bpmMode) {
    if (id.equals("MMD0")) {
      final int numtracks = data.u1();
      final int lines = data.u1() + 1;
      final Pattern block = new Pattern(numtracks, lines);
      for (int line = 0; line < lines; line++)
        for (int track = 0; track < numtracks; track++)
          block.setNote(track, line, readMmd0Note(data, instruments.length, hexVol, chan5to8, bpmMode));
      return block;
    }
    if (id.equals("MMD1")) {
      final int numtracks = data.u2();
      final int lines = data.u2() + 1;
      final Pattern block = new Pattern(numtracks, lines);
      data.s4(); // BlockInfo - unimportant for the player
      for (int line = 0; line < lines; line++)
        for (int track = 0; track < numtracks; track++)
          block.setNote(track, line, readMmd1Note(data, instruments.length, hexVol, chan5to8, bpmMode));
      return block;
    }
    return null;
  }

  private static long readMmd0Note(ByteReader data, int instruments, boolean hexVol, boolean chan5to8, boolean bpmMode) {
    int b0 = data.u1();
    int b1 = data.u1();
    int b2 = data.u1();
    int smp = (b0 >> 1 & 32 | b0 >> 3 & 16 | b1 >> 4 & 15) - 1;
    if (smp >= instruments)
      smp = -1;
    return getMedNote(b0 & 63, smp, medCommand(b1 & 15, b2, hexVol, chan5to8, bpmMode));
  }

  private static long readMmd1Note(ByteReader data, int instruments, boolean hexVol, boolean chan5to8, boolean bpmMode) {
    int b0 = data.u1() & 127;
    int b1 = data.u1() & 63;
    int b2 = data.u1();
    int b3 = data.u1();
    int smp = b1 - 1;
    if (smp >= instruments)
      smp = -1;
    return getMedNote(b0, smp, medCommand(b2, b3, hexVol, chan5to8, bpmMode));
  }

  private static long getMedNote(int key, int smp, final int effxy) {
    return Note.create(key, smp, effxy >> 8, effxy & 0xFF, smp >= 0 && key == 0);
  }

  private static int[] bpm1to9 = { 179, 164, 152, 141, 131, 123, 116, 110, 104 };

  private static int medCommand(final int eff, final int efxy, final boolean hexVol,
      final boolean bpmCon, final boolean bpmMod) {
    final int efx = efxy >> 4 & 15;
    final int efy = efxy & 15;
    switch (eff) {
    case 0x00: // arpeggio
      return 0x0000 | efxy;
    case 0x01: // slide frequency up
      return 0x0100 | efxy;
    case 0x11: // increase frequency - once
      return 0xE100 | efxy;
    case 0x02: // slide frequency down
      return 0x0200 | efxy;
    case 0x12: // decrease frequency - once
      return 0xE200 | efxy;
    case 0x03: // portamento to note
      return 0x0300 | efxy;
    case 0x04: // vibrato deeper (2x - from old N.T.)
      return 0x1400 | efxy;
    case 0x14: // vibrato shallower
      return 0x0400 | efxy;
    case 0x05: // volume slide + continue portamento
      return 0x0500 | efxy;
    case 0x06: // volume slide + continue vibrato
      return 0x0600 | efxy;
    case 0x07: // tremolo
      return 0x0700 | efxy;
    case 0x08: // set hold/decay (efx=decay, efy=hold); decay is treated as JMP in voltable for synth/hybrid
      return 0x1800 | efxy;
    case 0x09: // secondary tempo - ticks per line 1-32
      return 0x0F00 | crop(efxy, 1, 32);
    case 0x0A: // volume slide
    case 0x0D: // volume slide
      return 0x0A00 | efxy;
    case 0x0B: // jump to the given block
      return 0x0B00 | efxy;
    case 0x0C: // set volume if < 128 or set instrument volume if >= 128 (mod 128  0x80-0xC0 - always hex)
      return 0x0C00 | (!hexVol && efxy < 128 ? efx * 10 + efy : efxy);
    case 0x0E: // set synth waveform sequence position - do not reset
      return 0x1E00 | efxy;
    case 0x0F: // misc/main tempo
      switch (efxy) {
      case 0x00: // pattern break (to line 0);
        return 0x0D00;
      case 0xF1: // play note now and at the half of the line (two times per line); play at ticks 0 and 3
        return 0xE903;
      case 0xF2: // delay note by a half of line (as 0x1F); play at tick 3
        return 0xED03;
      case 0xF3: // play note 3 times per line; play at ticks 0, 2, and 4
        return 0xE902;
      case 0xF4: // delay note by one third of a line (by tpl/3)
      case 0xF5: // delay note by two thirds of a line (by tpl*2/3)
        return 0;
      case 0xF6: // ?
      case 0xF7: // MIDI wait
        return 0;
      case 0xF8: // amiga filter off
        return 0xE000;
      case 0xF9: // amiga filter on
        return 0xE001;
      case 0xFA: // ?
      case 0xFB: // ?
      case 0xFC: // ?
        return 0;
      case 0xFD: // change frequency (pitch is set without replaying a note)
        return 0xFD00;
      case 0xFE: // stop playing
        return 0x0BFF;
      case 0xFF: // mute track (as 0xC00)
        return 0x0C00;
      default: // change tempo bpm for all data 0x01..0xF0 (1..240); 1-11 as command 0x09 (if primary tempo = 33)
        if (bpmCon) // bizzare conversion from 5-8ch format...
          return efxy >= 10 ? 0x0F00 | 99 : 0x0F00 | bpm1to9[efxy - 1];
        if (bpmMod)
          return efxy < 12 ? 0x0F00 | efxy : 0x0F00 | crop(efxy, 33, 255); // not good for values 12..32
        return efxy < 12 ? 0x0F00 | efxy : 0x0F00 | crop(efxy * 125 / 33, 33, 255);
      }
    case 0x13: // pulse vibrato (was 3 then 5)
      return 0;
    case 0x15: // set finetune
      return 0xE500 | efxy;
    case 0x16: // repeat loop
      return 0xE600 | efxy;
    case 0x18: // cut note
      return 0xEC00 | efxy;
    case 0x19: // sample offset (*256)
      return 0x0900 | efxy;
    case 0x1A: // volume slide up (small - only once) volume + efxy (+ 1 if efxy >= 128)
      return crop(0xEA00 | efxy & 127, 0, 64); // not sure here
    case 0x1B: // volume slide down (small - only once)
      return crop(0xEB00 | efxy & 127, 0, 64); // not sure here
    case 0x1D: // next pattern to the given line
      return 0x0D00 | efxy; // conversion hex->dec not appropriate (internally used hex)
    case 0x1E: // block delay (data + 1)
      return 0xEE00 | efxy;
    case 0x1F: // retrig/delay efx = delay, efy = retrig
      return efx == 0 ? 0xE900 | efy : efy == 0 ? 0xED00 | efx : 0x1F00 | efxy;
    case 0x20: // if data=0 sample backwards else change sample position
    case 0x21: // slide up - constant rate (freq += data * freq / 2048)
    case 0x22: // slide down - constant rate (freq -= data * freq / 2048)
    case 0x23: // filter sweep (cutoff)
    case 0x24: // filter cutoff frequency
    case 0x25: // filter resonance + type
    case 0x29: // set sample position relative to sample length
      return 0;
    case 0x2E: // panpot for the track -16..16
      return 0x0800 | ((byte)efxy + 16) * 4;
    }
    return 0; // unknown conversion
  }
}
