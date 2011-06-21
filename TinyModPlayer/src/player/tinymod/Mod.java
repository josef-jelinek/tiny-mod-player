package player.tinymod;

import static player.tinymod.Tools.crop;
import player.tinymod.io.BinaryData;
import android.util.Log;

public final class Mod {
  public String title = "";
  public final String id;
  public Instrument[] sample;
  public int songLength;
  public int[] order = new int[256];
  public Block[] blocks;
  public int tracks;
  public int mainVolume = 64;
  public int[] trackVolume = new int[64];
  public boolean filter = false;
  public int bpm = 125; // beats per minute
  public int lpb = 0; // lines per beat (= 0 if tpb != 0)
  public int tpb = 24; // ticks per beat (= 0 if lpb != 0)
  public int tpl = 6; // ticks per line
  public int transpose = 0;
  public boolean doFirstLineTick = false;

  public Mod(final String id) {
    this.id = id;
    for (int i = 0; i < trackVolume.length; i++)
      trackVolume[i] = 64;
  }

  public static Mod parseMod(final byte[] file) {
    final BinaryData data = new BinaryData(file);
    data.seek(1080);
    final String id = data.string(4); // read identification
    int samples = 31;
    final Mod mod = new Mod(id);
    mod.tracks = 4;
    if (id.endsWith("CHN") && id.charAt(0) > '0' && id.charAt(0) <= '9')
      mod.tracks = id.charAt(0) - '0';
    else if (id.endsWith("CH") && id.charAt(0) > '0' && id.charAt(0) <= '9' &&
        id.charAt(1) >= '0' && id.charAt(1) <= '9')
      mod.tracks = (id.charAt(0) - '0') * 10 + id.charAt(1) - '0';
    // FLT8 uses 2 patterns as one; not one 8 track pattern... (numbers in playing sequence should be divided by 2)
    else if (!id.equals("M.K.") && !id.equals("FLT4") && !id.equals("M!K!")) {
      Log.i("tinymod", "Unknown mod id: " + id);
      samples = 15; // assume old-style .MOD (N.T.)
    }
    data.seek(0);
    final String title = data.string(20); // read title
    mod.title = title;
    mod.sample = new Instrument[samples];
    for (int smp = 0; smp < mod.sample.length; smp++) { // read all samples (30*sample bytes)
      final String sname = data.string(22); // read sample name
      final int len = data.w2();
      final int finetune = data.u1() & 15;
      final int vol = Math.min(64, data.u1());
      final int rep = data.w2(); // should be probbably in bytes for N.T. or S.T.? (u2()) - Amiga chip requirement??
      final int replen = data.w2();
      mod.sample[smp] = new SampledInstrument(smp + 1, len);
      mod.sample[smp].name(sname);
      mod.sample[smp].volume(vol);
      mod.sample[smp].fineTune(finetune - (finetune & 8) * 2);
      mod.sample[smp].loop(rep, replen);
    }
    mod.songLength = data.u1(); // read block length
    data.u1(); // skip block repeat or 128 etc.
    int numBlocks = 0;
    for (int i = 0; i < 128; i++)
      // read block order
      numBlocks = Math.max(numBlocks, mod.order[i] = data.u1());
    if (samples == 31)
      data.s4(); // skip id
    numBlocks++;
    mod.blocks = new Block[numBlocks];
    for (int block = 0; block < numBlocks; block++) { // read blocks
      mod.blocks[block] = new Block(64, mod.tracks);
      for (int line = 0; line < 64; line++)
        for (int trk = 0; trk < mod.tracks; trk++) {
          final int b0 = data.u1();
          final int b1 = data.u1();
          final int b2 = data.u1();
          final int b3 = data.u1();
          final int smp = (b0 & 0xF0 | (b2 & 0xF0) >> 4) - 1;
          final int period = (b0 << 8 | b1) & 0xFFF;
          final int key = Period.getKeyForPeriod(period * 100);
          int eff = b2 & 0xF;
          int efx = b3 >> 4;
          int efy = b3 & 15;
          if (eff == 0x0E) { // extended commands
            eff = eff << 4 | efx;
            efx = 0;
          }
          if (eff == 0x0D) { // pattern break dec to hex
            final int dec = efx * 10 + efy;
            efx = dec >> 4;
            efy = dec & 15;
          }
          final Instrument instr = smp < 0 || smp >= mod.sample.length ? null : mod.sample[smp];
          mod.blocks[block].putNote(line, trk, new Note(key, instr, eff, efx, efy, false));
        }
    }
    for (int i = 0; i < mod.sample.length; i++)
      for (int j = 0; j < mod.sample[i].data().length; j++) {
        if (data.isEnd()) {
          mod.sample[i].trimTo(j);
          break;
        }
        mod.sample[i].data()[j] = data.s1();
      }
    return mod;
  }

  public static Mod parseMed(final byte[] file) {
    final BinaryData data = new BinaryData(file);
    // MMD0 structure
    final String id = data.string(4);
    if (!id.equals("MMD0") && !id.equals("MMD1"))
      return null;
    final Mod mod = new Mod(id);
    data.s4(); // mod length
    final int song = data.s4(); // MMD0Song
    data.s4(); // reserved
    final int blockarr = data.s4(); // MMD0Block[]
    data.s4(); // reserved
    final int smplarr = data.s4(); // InstrHdr[]
    data.s4(); // reserved
    final int expdata = data.s4(); // MMD0Exp
    data.skip(15); // 4reserved 2pstate 2pblock 2pline 2pseqnum 2actplayline 1counter
    final int extrasongs = data.u1();
    if (extrasongs > 0)
      Log.i("tinymod", "Extrasongs " + extrasongs);
    // process song
    if (song == 0)
      return null;
    data.seek(song);
    // MMD0Song
    data.skip(63 * 8); // skip MMD0Sample - to be read later
    mod.blocks = new Block[data.u2()]; // length of blockarr in longwords
    mod.songLength = data.u2();
    for (int i = 0, j = 0; i < 256; i++) { // playseq[256]
      mod.order[j] = data.u1();
      if (j < mod.songLength && mod.order[j] >= mod.blocks.length)
        mod.songLength--; // skip an illegal position
      else
        j++;
    }
    final int tempo = Math.max(1, data.u2());
    mod.transpose = data.s1();
    final int flags = data.u1(); // 0 - filter, 4 - hex volumes, 5 - ST/NT/PT slide, 6 - 5-8 channels
    mod.filter = (flags & 1) != 0;
    final boolean hexVol = (flags & 16) != 0;
    mod.doFirstLineTick = (flags & 32) == 0;
    final boolean chan5to8 = (flags & 64) != 0;
    final int flags2 = data.u1(); // 0-4 lines per beat 0~1,31~32, 5 - bpm mode, 7 - !mix mode
    final boolean mixMode = (flags2 & 128) == 0; // sampled instruments above 3rd octave should play as in 3rd octave
    boolean bpmMode = (flags2 & 32) != 0; // all tempos given in bpm
    mod.lpb = bpmMode ? (flags2 & 31) + 1 : 0; // 0 - do not use lpb - use constant tpb instead
    mod.tpl = crop(data.u1(), 1, 60); // ticks per line
    mod.bpm = bpmMode ? tempo : tempo * 125 / 33;
    if (chan5to8) { // tempo in 5-8 channel modules was determined by a size of the mix buffer (0-9)
      bpmMode = true;
      mod.lpb = 4;
      mod.bpm = tempo >= 10 ? 99 : bpm1to9[tempo - 1];
    }
    if (mod.lpb != 0)
      mod.tpb = 0;
    if (!bpmMode && tempo < 12) { // just guessing here...
      mod.tpl = tempo;
      mod.bpm = 125;
    }
    // I hope: 50 Hz ~ standard tempo 33 if not BpM mode
    for (int trk = 0; trk < 16; trk++)
      // trkvol[16]
      mod.trackVolume[trk] = crop(data.u1(), 0, 64);
    mod.mainVolume = crop(data.u1(), 0, 64);
    mod.sample = new Instrument[data.u1()]; // length of smplarr in longwords
    // process samples - must be done before processsing blocks (sample referencing)
    if (smplarr != 0)
      for (int smp = 0; smp < mod.sample.length; smp++) {
        data.seek(smplarr + 4 * smp);
        final int samplep = data.s4();
        if (samplep != 0) {
          data.seek(samplep);
          final int length = data.s4();
          final int type = data.s2();
          if (type > 0)
            Log.i("tinymod MED", "Unsupported sample type " + type);
          if (type == 0) {
            mod.sample[smp] = new SampledInstrument(smp + 1, length);
            for (int i = 0; i < length; i++)
              mod.sample[smp].data()[i] = data.s1();
          } else if (type == -1 || type == -2) { // synth or hybrid
            data.skip(8); // not used in modules
            final int voltbllen = data.u2();
            final int wftbllen = data.u2();
            final int volspeed = data.u1();
            final int wfspeed = data.u1();
            final int wforms = data.u2();
            final SynthInstrument instr = new SynthInstrument(smp + 1, wforms, type == -2);
            mod.sample[smp] = instr;
            instr.volSpeed(volspeed);
            instr.wfSpeed(wfspeed);
            instr.volData(voltbllen);
            for (int vol = 0; vol < voltbllen; vol++) {
              final int x = data.u1();
              instr.volData[vol] = (byte)x;
              if (x == 0xFF) {
                data.skip(voltbllen - vol - 1);
                break;
              }
            }
            instr.wfData(wftbllen);
            for (int wf = 0; wf < wftbllen; wf++) {
              final int x = data.u1();
              instr.wfData[wf] = (byte)x;
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
                if (instr.synthWf(wform)) {
                  final int ln = data.w2();
                  instr.waveform(wform, ln);
                  for (int i = 0; i < ln; i++)
                    instr.data(wform)[i] = data.s1();
                } else { // sampled waveform
                  final int ln = data.s4();
                  final int tp = data.s2();
                  if (tp != 0)
                    Log.i("tinymod MED", "Illegal type of hybrid instrument waveform - " + tp);
                  instr.waveform(wform, ln);
                  for (int i = 0; i < ln; i++)
                    instr.data(wform)[i] = data.s1();
                }
              }
            }
          }
        }
      }
    data.seek(song);
    for (final Instrument element : mod.sample) { // MMD0Sample[63] at the start of MMD0Song
      final int rep = data.w2();
      final int replen = data.w2();
      data.skip(2); // 1midich 1midipreset
      final int svol = crop(data.u1(), 0, 64);
      final int strans = data.s1();
      final Instrument instr = element;
      if (instr != null) {
        instr.loop(rep, replen);
        instr.volume(svol);
        if (!mixMode || instr instanceof SynthInstrument && !((SynthInstrument)instr).hybrid())
          instr.transpose(strans);
        else {
          instr.transpose(strans + 2 * 12);
          instr.minKey(2 * 12 + 1);
          instr.maxKey(5 * 12);
        }
      }
    }
    // process blocks
    mod.tracks = 4;
    if (blockarr != 0)
      for (int block = 0; block < mod.blocks.length; block++) {
        data.seek(blockarr + 4 * block);
        final int blockp = data.s4();
        if (blockp != 0) {
          data.seek(blockp);
          if (id.equals("MMD0")) {
            final int numtracks = data.u1();
            mod.tracks = Math.max(mod.tracks, numtracks);
            final int lines = data.u1() + 1;
            mod.blocks[block] = new Block(lines, numtracks);
            for (int line = 0; line < lines; line++)
              for (int track = 0; track < numtracks; track++) {
                final int b0 = data.u1();
                final int b1 = data.u1();
                final int b2 = data.u1();
                final int key = b0 & 63;
                final int smp = (b0 >> 1 & 32 | b0 >> 3 & 16 | b1 >> 4 & 15) - 1;
                final int effxy = medCommand(b1 & 15, b2, hexVol, chan5to8, bpmMode);
                final Instrument instr =
                    smp < 0 || smp >= mod.sample.length ? null : mod.sample[smp];
                final Note note =
                    new Note(key, instr, effxy >> 8, effxy >> 4 & 15, effxy & 15, instr != null &&
                        key == 0);
                mod.blocks[block].putNote(line, track, note);
              }
          } else if (id.equals("MMD1")) {
            final int numtracks = data.u2();
            mod.tracks = Math.max(mod.tracks, numtracks);
            final int lines = data.u2() + 1;
            mod.blocks[block] = new Block(lines, numtracks);
            data.s4(); // BlockInfo - unimportant for the player
            for (int line = 0; line < lines; line++)
              for (int track = 0; track < numtracks; track++) {
                final int b0 = data.u1() & 127;
                final int b1 = data.u1() & 63;
                final int b2 = data.u1();
                final int b3 = data.u1();
                final int key = b0;
                final int smp = b1 - 1;
                final int effxy = medCommand(b2, b3, hexVol, chan5to8, bpmMode);
                final Instrument instr =
                    smp < 0 || smp >= mod.sample.length ? null : mod.sample[smp];
                final Note note =
                    new Note(key, instr, effxy >> 8, effxy >> 4 & 15, effxy & 15, instr != null &&
                        key == 0);
                mod.blocks[block].putNote(line, track, note);
              }
          }
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
          if (smp >= mod.sample.length || mod.sample[smp] == null) {
            data.skip(expSmpSz);
            continue;
          }
          mod.sample[smp].hold(data.u1());
          mod.sample[smp].decay(data.u1());
          if (expSmpSz < 4) {
            data.skip(expSmpSz - 2);
            continue;
          }
          data.u1(); // supress midi off
          mod.sample[smp].fineTune(data.s1());
          data.skip(expSmpSz - 4);
        }
      }
    }
    return mod;
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

  // TODO remove after completing
  @SuppressWarnings("unused")
  public static Mod parseAhx(final byte[] file) {
    final BinaryData data = new BinaryData(file);
    final String id = data.string(3);
    final int ver = data.u1();
    if (!id.equals("THX") || ver > 1)
      return null;
    final Mod mod = new Mod(id);
    data.u2(); // offset to title and sample names
    int len = data.u2(); // position list length + timing + track zero flag
    //mod.tps = 50 + 50 * (len >> 12 & 7);
    final boolean trackZero = (len & 0x8000) != 0;
    len &= 0xFFF;
    final int res = crop(data.u2(), 0, len - 1); // restart position
    final int trl = crop(data.u1(), 1, 64); // track length
    final int trk = data.u1(); // number of tracks saved
    final int smp = crop(data.u1(), 0, 63); // number of samples saved
    final int ss = data.u1(); // number of subsongs
    final int[] sslist = new int[ss]; // subsong list
    for (int i = 0; i < ss; i++)
      sslist[i] = crop(data.u2(), 0, len - 1);
    return mod;
  }
}