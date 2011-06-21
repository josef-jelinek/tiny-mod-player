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
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;

public class TinyModService extends Service {
  private final MediaPlayer mp = new MediaPlayer();
  private final List<String> songs = new ArrayList<String>();
  private int currentPosition;
  private final ModPlayer player = new ModPlayer(new AndroidAudioDevice(44100), true);

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    stopForeground();
    mp.stop();
    mp.release();
  }

  private void startForeground() {
    final CharSequence text = getText(R.string.foreground_service_started);
    final Notification notification =
        new Notification(R.drawable.notify_icon, text, System.currentTimeMillis());
    final PendingIntent contentIntent =
        PendingIntent.getActivity(this, 0, new Intent(this, Start.class), 0);
    notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
    startForeground(R.string.foreground_service_started, notification);
  }

  private void stopForeground() {
    stopForeground(true);
  }

  private void playSong(final String filePath) {
    final File file = new java.io.File(filePath);
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
      Log.e("tinymod", "", e);
      return;
    }
    try {
      if (file.getName().toLowerCase().endsWith(".med"))
        playLoop(Mod.parseMed(data));
      else if (file.getName().toLowerCase().endsWith(".mod"))
        playLoop(Mod.parseMod(data));
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
      Log.e(getString(R.string.app_name), e.getMessage());
    }
  }

  private Thread playThread = null;

  private synchronized void playLoop(final Mod mod) {
    try {
      if (playThread != null && playThread.isAlive()) {
        playThread.interrupt();
        playThread.join();
      }
      player.play(mod);
      playThread = new Thread(new Runnable() {
        public void run() {
          Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
          while (player.playing())
            if (Thread.interrupted())
              player.stop();
            else
              player.mix();
          stopForeground();
        }
      });
      startForeground();
      playThread.start();
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

  private final TinyModServiceInterface.Stub mBinder = new TinyModServiceInterface.Stub() {
    public void play(final int position) throws DeadObjectException {
      try {
        currentPosition = position;
        playSong(songs.get(position));
      } catch (final IndexOutOfBoundsException e) {
        Log.e(getString(R.string.app_name), e.getMessage());
      }
    }

    public void add(final String song) throws DeadObjectException {
      songs.add(song);
    }

    public void clear() throws DeadObjectException {
      songs.clear();
    }

    public void backward() throws DeadObjectException {
      prevSong();
    }

    public void forward() throws DeadObjectException {
      nextSong();
    }

    public void pause() throws DeadObjectException {
      mp.pause();
    }

    public void stop() throws DeadObjectException {
      mp.stop();
      stopForeground();
    }
  };

  @Override
  public IBinder onBind(final Intent intent) {
    return mBinder;
  }
}