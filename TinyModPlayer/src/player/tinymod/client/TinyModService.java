package player.tinymod.client;

import java.io.*;
import java.util.*;
import player.tinymod.*;
import player.tinymod.audio.AndroidAudioDevice;
import player.tinymod.format.*;
import player.tinymod.unpack.*;
import android.app.*;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.widget.Toast;

public class TinyModService extends Service {
  static final int MSG_PLAY_FILE = 1;
  static final int MSG_PAUSE = 2;
  static final int MSG_RESUME = 3;
  static final int MSG_STOP = 4;
  static final int MSG_GET_PLAYING_STATE = 5;
  static final int MSG_SET_PLAYING_STATE = 6;
  static final int PLAYING_STATE_PLAY = 0;
  static final int PLAYING_STATE_PAUSE = 1;
  static final int PLAYING_STATE_STOP = 2;
  static final int PLAYING_STATE_END = 3;
  final ModPlayer player = new ModPlayer(new AndroidAudioDevice(44100), true);
  private static final List<Parser> parsers = new ArrayList<Parser>();
  private static final List<Unpacker> unpackers = new ArrayList<Unpacker>();
  
  private final Messenger messenger = new Messenger(new Handler() {
    @Override
    public void handleMessage(final Message message) {
      Log.d("tinymod", "service got a message " + message.what);
      if (message.what == MSG_PLAY_FILE)
        play(message.getData().getString("name"));
      else if (message.what == MSG_PAUSE)
        pause();
      else if (message.what == MSG_RESUME)
        resume();
      else if (message.what == MSG_STOP)
        stop();
      else if (message.what == MSG_GET_PLAYING_STATE)
        sendPlayingState(message.replyTo);
      else
        super.handleMessage(message);
    }
  });
  static {
    parsers.add(new ParserAhx());
    parsers.add(new ParserMed());
    parsers.add(new ParserMod());
    unpackers.add(new PowerPacker());
    unpackers.add(new XpkSqsh());
  }

  @Override
  public void onDestroy() {
    Log.d("tinymod", "service destroyed");
    stopLoop();
    super.onDestroy();
  }

  @Override
  public IBinder onBind(final Intent intent) {
    Log.d("tinymod", "service binding");
    return messenger.getBinder();
  }

  void startForeground(final String name) {
    Log.d("tinymod", "service going foreground");
    final Intent intent = new Intent(this, Start.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
    final CharSequence text = getString(R.string.service_song_playing) + " " + name;
    final long now = System.currentTimeMillis();
    final Notification notification = new Notification(R.drawable.notify_icon, text, now);
    notification.setLatestEventInfo(this, getText(R.string.app_name), text, pendingIntent);
    startForeground(R.string.service_song_playing, notification);
  }

  void stopForeground() {
    Log.d("tinymod", "service going background");
    stopForeground(true);
  }

  private void playSong(final String filePath) {
    File file = new File(filePath);
    byte[] data = new byte[(int)file.length()];
    try {
      final FileInputStream in = new FileInputStream(file);
      if (in.read(data) != data.length) {
        in.close();
        Log.e("tinymod", "Not all bytes read");
        return;
      }
      in.close();
    } catch (final Exception e) {
      Log.e("tinymod", e.getMessage(), e);
      return;
    }
    String packerName = "";
    for (Unpacker unpacker : unpackers) {
      if (unpacker.test(data)) {
        byte[] a = unpacker.unpack(data);
        if (a == null) {
          Toast.makeText(this, "Failed to unpack as " + unpacker.name(), Toast.LENGTH_SHORT).show();
        } else {
          packerName = unpacker.name();
          data = a;
        }
      }
    }
    for (Parser parser : parsers) {
      if (parser.test(data)) {
        Mod mod = parser.parse(data);
        if (mod == null) {
          Toast.makeText(this, "Failed to load as " + parser.name(), Toast.LENGTH_SHORT).show();
        } else {
          mod.packer = packerName;
          playLoop(mod, file.getName());
          return;
        }
      }
    }
    Toast.makeText(this, "Unknown format", Toast.LENGTH_SHORT).show();
  }

  private Thread playThread = null;

  private synchronized void playLoop(final Mod mod, final String name) {
    stopLoop();
    player.play(mod);
    playThread = new Thread(new Runnable() {
      public void run() {
        try {
          startForeground(name);
          broadcastPlayState();
          while (player.isActive()) {
            if (player.isPaused())
              Thread.sleep(50);
            if (Thread.interrupted())
              player.stop();
            else
              player.mix();
          }
        } catch (final InterruptedException e) {
          player.stop();
        } finally {
          stopForeground();
          broadcastStopState();
        }
      }
    });
    playThread.setPriority(Thread.MAX_PRIORITY);
    playThread.start();
  }

  private void stopLoop() {
    try {
      if (playThread != null && playThread.isAlive()) {
        playThread.interrupt();
        playThread.join();
      }
    } catch (final InterruptedException e) {}
  }

  void play(final String path) {
    player.stop();
    broadcastStopState();
    playSong(path);
  }

  void resume() {
    try {
      if (player.isActive() && player.isPaused()) {
        player.resume();
        broadcastPlayState();
      }
    } catch (final IndexOutOfBoundsException e) {
      Log.e("tinymod", e.getMessage(), e);
    }
  }

  void pause() {
    player.pause();
    broadcastPauseState();
  }

  void stop() {
    stopLoop();
  }

  void sendPlayingState(final Messenger messenger) {
    try {
      final int state =
          player.isActive() ? player.isPaused() ? PLAYING_STATE_PAUSE : PLAYING_STATE_PLAY : PLAYING_STATE_STOP;
      Log.d("tinymod", "service sent playing state message " + state + " " + messenger.getClass());
      messenger.send(createPlayingStateMessage(state));
    } catch (final RemoteException e) {
      Log.e("tinymod", e.getMessage(), e);
    }
  }

  private Message createPlayingStateMessage(final int state) {
    final Message message = Message.obtain(null, MSG_SET_PLAYING_STATE, state, 0);
    final Bundle bundle = new Bundle();
    bundle.putString("info", player.toString());
    message.setData(bundle);
    return message;
  }

  void broadcastPlayState() {
    Log.d("tinymod", "service broadcasting play");
    sendBroadcastWithInfo(R.string.intent_action_play);
  }

  private void broadcastPauseState() {
    Log.d("tinymod", "service broadcasting pause");
    sendBroadcastWithInfo(R.string.intent_action_pause);
  }

  void broadcastStopState() {
    Log.d("tinymod", "service broadcasting stop");
    sendBroadcastWithInfo(R.string.intent_action_stop);
  }

  private void sendBroadcastWithInfo(final int id) {
    final Intent intent = new Intent(getString(id));
    intent.putExtra("info", player.toString());
    sendBroadcast(intent);
  }
}
