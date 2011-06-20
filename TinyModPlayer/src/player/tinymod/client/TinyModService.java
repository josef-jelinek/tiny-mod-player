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
import android.app.NotificationManager;
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
  private NotificationManager nm;
  private static final int NOTIFY_ID = R.layout.mod_list;
  private final ModPlayer player = new ModPlayer(new AndroidAudioDevice(44100), true);

  @Override
  public void onCreate() {
    super.onCreate();
    nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  }

  @Override
  public void onDestroy() {
    mp.stop();
    mp.release();
    nm.cancel(NOTIFY_ID);
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
        //Notification notification = new Notification(R.drawable.play, file, null, file, null);
        //nm.notify(NOTIFY_ID, notification);
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
          while (player.playing())
            if (Thread.interrupted())
              player.stop();
            else
              player.mix();
        }
      });
      playThread.start();
    } catch (final InterruptedException e) {}
  }

  private void nextSong() {
    // Check if last song or not
    if (++currentPosition >= songs.size()) {
      currentPosition = 0;
      nm.cancel(NOTIFY_ID);
    } else
      playSong(songs.get(currentPosition));
  }

  private void prevSong() {
    if (mp.getCurrentPosition() < 3000 && currentPosition >= 1)
      playSong(songs.get(--currentPosition));
    else
      playSong(songs.get(currentPosition));
  }

  private final TinyModServiceInterface.Stub mBinder = new TinyModServiceInterface.Stub() {
    public void playFile(final int position) throws DeadObjectException {
      try {
        currentPosition = position;
        playSong(songs.get(position));
      } catch (final IndexOutOfBoundsException e) {
        Log.e(getString(R.string.app_name), e.getMessage());
      }
    }

    public void addSongPlaylist(final String song) throws DeadObjectException {
      songs.add(song);
    }

    public void clearPlaylist() throws DeadObjectException {
      songs.clear();
    }

    public void skipBack() throws DeadObjectException {
      prevSong();
    }

    public void skipForward() throws DeadObjectException {
      nextSong();
    }

    public void pause() throws DeadObjectException {
      //Notification notification = new Notification(R.drawable.pause, null, null, null, null);
      //nm.notify(NOTIFY_ID, notification);
      mp.pause();
    }

    public void stop() throws DeadObjectException {
      nm.cancel(NOTIFY_ID);
      mp.stop();
    }
  };

  @Override
  public IBinder onBind(final Intent intent) {
    return mBinder;
  }
}
