package player.tinymod.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

  private void playSong(final String file) {
    try {
      //Notification notification = new Notification(R.drawable.play, file, null, file, null);
      //nm.notify(NOTIFY_ID, notification);
      mp.reset();
      mp.setDataSource(file);
      mp.prepare();
      mp.start();
      mp.setOnCompletionListener(new OnCompletionListener() {
        public void onCompletion(final MediaPlayer arg0) {
          nextSong();
        }
      });
    } catch (final IOException e) {
      Log.e(getString(R.string.app_name), e.getMessage());
    }
  }

  private void nextSong() {
    // Check if last song or not
    if (++currentPosition >= songs.size()) {
      currentPosition = 0;
      nm.cancel(NOTIFY_ID);
    } else
      playSong(Start.MEDIA_PATH + songs.get(currentPosition));
  }

  private void prevSong() {
    if (mp.getCurrentPosition() < 3000 && currentPosition >= 1)
      playSong(Start.MEDIA_PATH + songs.get(--currentPosition));
    else
      playSong(Start.MEDIA_PATH + songs.get(currentPosition));
  }

  private final TinyModServiceInterface.Stub mBinder = new TinyModServiceInterface.Stub() {
    public void playFile(final int position) throws DeadObjectException {
      try {
        currentPosition = position;
        playSong(Start.MEDIA_PATH + songs.get(position));
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
