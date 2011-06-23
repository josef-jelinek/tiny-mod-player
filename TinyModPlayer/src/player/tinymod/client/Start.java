package player.tinymod.client;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import player.tinymod.R;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class Start extends ListActivity {
  private static final String MEDIA_PATH = "/sdcard/Mods";
  private final List<String> songs = new ArrayList<String>();
  private final static ModFilter filter = new ModFilter();
  private Button playButton;
  private Button pauseButton;
  private Button stopButton;
  Messenger serviceMessenger = null;
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(final ComponentName className, final IBinder service) {
      serviceMessenger = new Messenger(service);
      Log.d("tinymod", "activity asking service for playing state");
      sendMessage(Message.obtain(null, TinyModService.MSG_GET_PLAYING_STATE));
      updateSongList();
    }

    public void onServiceDisconnected(final ComponentName className) {
      // Called when the service is unexpectedly disconnected (its process crashed).
      serviceMessenger = null;
    }
  };
  final Messenger activityMessenger = new Messenger(new Handler() {
    @Override
    public void handleMessage(final Message message) {
      if (message.what == TinyModService.MSG_SET_PLAYING_STATE) {
        Log.d("tinymod", "activity got a playing state message " + message.what + " " +
            message.arg1);
        if (message.arg1 == TinyModService.PLAYING_STATE_PLAY)
          showButtonsForPlay();
        else if (message.arg1 == TinyModService.PLAYING_STATE_PAUSE)
          showButtonsForPause();
        else
          showButtonsForStop();
      } else {
        Log.d("tinymod", "activity got an unknown message " + message.what);
        super.handleMessage(message);
      }
    }
  });

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    Log.d("tinymod", "activity created");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.mod_list);
    playButton = (Button)findViewById(R.id.play);
    pauseButton = (Button)findViewById(R.id.pause);
    stopButton = (Button)findViewById(R.id.stop);
    setButtonListeners();
    setBroadcastReceivers();
    showButtonsForStop();
    bindService(new Intent(this, TinyModService.class), serviceConnection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onDestroy() {
    Log.d("tinymod", "activity destroyed");
    try {
      unbindService(serviceConnection);
    } catch (final Throwable t) {
      Log.e("tinymod", "Failed to unbind from the service", t);
    }
    super.onDestroy();
  }

  private void setButtonListeners() {
    playButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        playClicked();
      }
    });
    pauseButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        pauseClicked();
      }
    });
    stopButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        stopClicked();
      }
    });
  }

  private void setBroadcastReceivers() {
    final BroadcastReceiver playReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("tinymod", "activity got PLAY broadcast");
        showButtonsForPlay();
      }
    };
    final BroadcastReceiver pauseReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("tinymod", "activity got PAUSE broadcast");
        showButtonsForPause();
      }
    };
    final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("tinymod", "activity got STOP broadcast");
        showButtonsForStop();
      }
    };
    registerReceiver(playReceiver, new IntentFilter(getString(R.string.intent_action_play)));
    registerReceiver(pauseReceiver, new IntentFilter(getString(R.string.intent_action_pause)));
    registerReceiver(stopReceiver, new IntentFilter(getString(R.string.intent_action_stop)));
  }

  private void showButtonsForPlay() {
    hideButton(playButton);
    showButton(pauseButton);
    stopButton.setEnabled(true);
  }

  private void showButtonsForPause() {
    showButton(playButton);
    hideButton(pauseButton);
    stopButton.setEnabled(true);
  }

  private void showButtonsForStop() {
    showButton(playButton);
    hideButton(pauseButton);
    stopButton.setEnabled(false);
  }

  private void showButton(final Button button) {
    button.setEnabled(true);
    button.setVisibility(View.VISIBLE);
  }

  private void hideButton(final Button button) {
    button.setEnabled(false);
    button.setVisibility(View.GONE);
  }

  private void playClicked() {
    sendMessage(Message.obtain(null, TinyModService.MSG_PLAY));
  }

  private void pauseClicked() {
    sendMessage(Message.obtain(null, TinyModService.MSG_PAUSE));
  }

  private void stopClicked() {
    sendMessage(Message.obtain(null, TinyModService.MSG_STOP));
  }

  public void updateSongList() {
    final List<File> fileList = listSongFiles(new File(MEDIA_PATH));
    sendMessage(Message.obtain(null, TinyModService.MSG_CLEAR));
    for (final File file : fileList) {
      songs.add(file.getName());
      final Message message = Message.obtain(null, TinyModService.MSG_ADD);
      sendMessage(addStringParameter(message, "name", file.getAbsolutePath()));
    }
    setListAdapter(new ArrayAdapter<String>(this, R.layout.mod_item, songs));
  }

  @Override
  protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
    sendMessage(Message.obtain(null, TinyModService.MSG_PLAY_INDEX, position, 0));
  }

  private void sendMessage(final Message message) {
    if (serviceMessenger != null)
      try {
        message.replyTo = activityMessenger;
        serviceMessenger.send(message);
      } catch (final RemoteException e) {
        Log.w("tinymod", e.getMessage(), e);
      }
  }

  private Message addStringParameter(final Message message, final String name, final String value) {
    final Bundle b = new Bundle();
    b.putString(name, value);
    message.setData(b);
    return message;
  }

  private static List<File> listSongFiles(final File directory) {
    final File[] files = directory.listFiles(filter);
    return files == null ? new ArrayList<File>(0) : Arrays.asList(files);
  }

  private static class ModFilter implements FilenameFilter {
    public boolean accept(final File dir, final String name) {
      final String s = name.toLowerCase();
      return s.endsWith(".mod") || s.endsWith(".med") || s.endsWith(".mp3");
    }
  }
}
