package player.tinymod.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import player.tinymod.AndroidAudioDevice;
import player.tinymod.Mod;
import player.tinymod.ModPlayer;
import player.tinymod.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class TinyModService extends Service {
  static final int MSG_PLAY_FILE = 1;
  static final int MSG_PLAY = 2;
  static final int MSG_PAUSE = 3;
  static final int MSG_STOP = 4;
  static final int MSG_GET_PLAYING_STATE = 5;
  static final int MSG_SET_PLAYING_STATE = 6;
  static final int PLAYING_STATE_PLAY = 0;
  static final int PLAYING_STATE_PAUSE = 1;
  static final int PLAYING_STATE_STOP = 2;
  static final int PLAYING_STATE_END = 3;
  private final MediaPlayer mp = new MediaPlayer();
  private final ModPlayer player = new ModPlayer(new AndroidAudioDevice(44100), true);
  private final Messenger messenger = new Messenger(new Handler() {
    @Override
    public void handleMessage(final Message message) {
      Log.d("tinymod", "service got a message " + message.what);
      switch (message.what) {
      case MSG_PLAY_FILE:
        play(message.getData().getString("name"));
        break;
      case MSG_PLAY:
        play();
        break;
      case MSG_PAUSE:
        pause();
        break;
      case MSG_STOP:
        stop();
        break;
      case MSG_GET_PLAYING_STATE:
        sendPlayingState(message.replyTo);
        break;
      default:
        super.handleMessage(message);
      }
    }
  });

  @Override
  public void onDestroy() {
    Log.d("tinymod", "service destroyed");
    stopLoop();
    mp.stop();
    mp.release();
    super.onDestroy();
  }

  private void startForeground(final String name) {
    Log.d("tinymod", "service going foreground");
    final CharSequence text = getString(R.string.service_song_playing) + " " + name;
    final Notification notification =
        new Notification(R.drawable.notify_icon, text, System.currentTimeMillis());
    final PendingIntent contentIntent =
        PendingIntent.getActivity(this, 0, new Intent(this, Start.class), 0);
    notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
    startForeground(R.string.service_song_playing, notification);
    Log.d("tinymod", "service broadcasting play");
    sendBroadcast(new Intent(getString(R.string.intent_action_play)));
  }

  private void stopForeground() {
    Log.d("tinymod", "service going background");
    stopForeground(true);
    Log.d("tinymod", "service broadcasting stop");
    sendBroadcast(new Intent(getString(R.string.intent_action_stop)));
  }

  private void playSong(final String filePath) {
    final File file = new File(filePath);
    final byte[] data = new byte[(int)file.length()];
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
    try {
      if (file.getName().toLowerCase().endsWith(".med"))
        playLoop(Mod.parseMed(data), file.getName());
      else if (file.getName().toLowerCase().endsWith(".mod"))
        playLoop(Mod.parseMod(data), file.getName());
      else if (file.getName().toLowerCase().endsWith(".mp3")) {
        mp.reset();
        mp.setDataSource(filePath);
        mp.prepare();
        mp.start();
        mp.setOnCompletionListener(new OnCompletionListener() {
          public void onCompletion(final MediaPlayer arg0) {
            Log.d("tinymod", "service broadcasting stop");
            sendBroadcast(new Intent(getString(R.string.intent_action_stop)));
          }
        });
      }
    } catch (final IOException e) {
      Log.e("tinymod", e.getMessage(), e);
    }
  }

  private Thread playThread = null;

  private synchronized void playLoop(final Mod mod, final String name) {
    stopLoop();
    player.play(mod);
    playThread = new Thread(new Runnable() {
      public void run() {
        try {
          startForeground(name);
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

  private void play(final String path) {
    player.stop();
    Log.d("tinymod", "service broadcasting stop");
    sendBroadcast(new Intent(getString(R.string.intent_action_stop)));
    playSong(path);
  }

  private void play() {
    try {
      if (player.isActive() && player.isPaused()) {
        player.resume();
        Log.d("tinymod", "service broadcasting play");
        sendBroadcast(new Intent(getString(R.string.intent_action_play)));
      }
    } catch (final IndexOutOfBoundsException e) {
      Log.e("tinymod", e.getMessage(), e);
    }
  }

  private void pause() {
    player.pause();
    mp.pause();
    sendBroadcast(new Intent(getString(R.string.intent_action_pause)));
    Log.d("tinymod", "service broadcasting pause");
  }

  private void stop() {
    stopLoop();
    mp.stop();
  }

  private void sendPlayingState(final Messenger messenger) {
    try {
      final int state =
          player.isActive() ? player.isPaused() ? PLAYING_STATE_PAUSE : PLAYING_STATE_PLAY
              : PLAYING_STATE_STOP;
      Log.d("tinymod", "service sent playing state message " + state + " " + messenger.getClass());
      messenger.send(Message.obtain(null, MSG_SET_PLAYING_STATE, state, 0));
    } catch (final RemoteException e) {
      Log.e("tinymod", e.getMessage(), e);
    }
  }

  @Override
  public IBinder onBind(final Intent intent) {
    Log.d("tinymod", "service binding");
    return messenger.getBinder();
  }
}
