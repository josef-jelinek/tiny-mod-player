package player.tinymod.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import android.widget.Toast;

public class TinyModService extends Service {
  private final MediaPlayer mp = new MediaPlayer();
  private final List<String> songs = new ArrayList<String>();
  private int currentPosition;
  private final ModPlayer player = new ModPlayer(new AndroidAudioDevice(44100), true);

  @Override
  public void onDestroy() {
    stopLoop();
    mp.stop();
    mp.release();
  }

  private void startForeground(final String name) {
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
            nextSong();
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

  private void nextSong() {
    if (++currentPosition >= songs.size())
      currentPosition = 0;
    else
      playSong(songs.get(currentPosition));
  }

  private void prevSong() {
    if (mp.getCurrentPosition() < 3000 && currentPosition >= 1)
      playSong(songs.get(--currentPosition));
    else
      playSong(songs.get(currentPosition));
  }

  public void play(final int position) {
    currentPosition = position < 0 || position >= songs.size() ? 0 : position;
    player.stop();
    Log.d("tinymod", "service broadcasting stop");
    sendBroadcast(new Intent(getString(R.string.intent_action_stop)));
    play();
  }

  public void play() {
    try {
      if (player.isActive() && player.isPaused()) {
        player.resume();
        Log.d("tinymod", "service broadcasting play");
        sendBroadcast(new Intent(getString(R.string.intent_action_play)));
      } else if (!player.isActive())
        playSong(songs.get(currentPosition));
    } catch (final IndexOutOfBoundsException e) {
      Log.e("tinymod", e.getMessage(), e);
    }
  }

  public void add(final String song) {
    songs.add(song);
  }

  public void clear() {
    songs.clear();
  }

  public void backward() {
    prevSong();
  }

  public void forward() {
    nextSong();
  }

  public void pause() {
    player.pause();
    mp.pause();
    sendBroadcast(new Intent(getString(R.string.intent_action_pause)));
    Log.d("tinymod", "service broadcasting pause");
  }

  public void stop() {
    stopLoop();
    mp.stop();
  }

  static final int MSG_PLAY_INDEX = 1;
  static final int MSG_PLAY = 2;
  static final int MSG_PAUSE = 3;
  static final int MSG_STOP = 4;
  static final int MSG_ADD = 5;
  static final int MSG_CLEAR = 6;
  static final int MSG_GET_PLAYING_STATE = 7;
  static final int MSG_SET_PLAYING_STATE = 8;
  static final int PLAYING_STATE_PLAY = 0;
  static final int PLAYING_STATE_PAUSE = 1;
  static final int PLAYING_STATE_STOP = 2;

  class IncomingHandler extends Handler {
    @Override
    public void handleMessage(final Message message) {
      Log.d("tinymod", "service got a message " + message.what);
      switch (message.what) {
      case MSG_CLEAR:
        clear();
        break;
      case MSG_ADD:
        add(message.getData().getString("name"));
        break;
      case MSG_PLAY_INDEX:
        Toast.makeText(getApplicationContext(), "play " + message.arg1, Toast.LENGTH_SHORT).show();
        play(message.arg1);
        break;
      case MSG_PLAY:
        Toast.makeText(getApplicationContext(), "play", Toast.LENGTH_SHORT).show();
        play();
        break;
      case MSG_PAUSE:
        Toast.makeText(getApplicationContext(), "pause", Toast.LENGTH_SHORT).show();
        pause();
        break;
      case MSG_STOP:
        Toast.makeText(getApplicationContext(), "stop", Toast.LENGTH_SHORT).show();
        stop();
        break;
      case MSG_GET_PLAYING_STATE:
        sendPlayingState(message.replyTo);
        break;
      default:
        super.handleMessage(message);
      }
    }
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

  final Messenger messenger = new Messenger(new IncomingHandler());

  @Override
  public IBinder onBind(final Intent intent) {
    Log.d("tinymod", "binding");
    return messenger.getBinder();
  }
}
