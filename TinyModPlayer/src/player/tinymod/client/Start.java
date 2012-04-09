package player.tinymod.client;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import android.widget.TextView;
import android.widget.Toast;

public final class Start extends ListActivity {
  private static final String MEDIA_PATH = "/sdcard/Mods";
  private final List<String> songs = new ArrayList<String>();
  private final Map<String, String> songPaths = new HashMap<String, String>();
  private final static ModFilter filter = new ModFilter();
  private GuiControls guiControls = null;
  Messenger serviceMessenger = null;
  private BroadcastReceiver playReceiver;
  private BroadcastReceiver pauseReceiver;
  private BroadcastReceiver stopReceiver;
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(final ComponentName className, final IBinder service) {
      serviceMessenger = new Messenger(service);
      Log.d("tinymod", "activity sending " + TinyModService.MSG_GET_PLAYING_STATE);
      sendMessage(Message.obtain(null, TinyModService.MSG_GET_PLAYING_STATE));
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
        Log.d("tinymod", "activity got message " + message.what + "(" + message.arg1 + ")");
        if (message.arg1 == TinyModService.PLAYING_STATE_PLAY)
          guiControls.showButtonsForPlay();
        else if (message.arg1 == TinyModService.PLAYING_STATE_PAUSE)
          guiControls.showButtonsForPause();
        else
          guiControls.showButtonsForStop();
        guiControls.showInfo(message.getData().getString("info"));
        updateSongList();
      } else {
        Log.d("tinymod", "activity got message " + message.what);
        super.handleMessage(message);
      }
    }
  });

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("tinymod", "activity created");
    initGui();
    setBroadcastReceivers();
    connectPlayerService();
  }

  private void initGui() {
    setContentView(R.layout.mod_list);
    guiControls =
        new GuiControls(findButton(R.id.play), findButton(R.id.pause), findButton(R.id.stop),
            findText(R.id.text_info));
  }

  private Button findButton(final int id) {
    return (Button)findViewById(id);
  }

  private TextView findText(final int id) {
    return (TextView)findViewById(id);
  }

  private void connectPlayerService() {
    final Intent intent = new Intent(this, TinyModService.class);
    startService(intent);
    if (!bindService(intent, serviceConnection, 0))
      Toast.makeText(this, "Inicialization error", Toast.LENGTH_SHORT);
  }

  @Override
  protected void onDestroy() {
    Log.d("tinymod", "activity destroyed");
    try {
      unbindService(serviceConnection);
      unregisterReceiver(playReceiver);
      unregisterReceiver(pauseReceiver);
      unregisterReceiver(stopReceiver);
    } catch (final Throwable t) {
      Log.e("tinymod", "Failed to unbind from the service", t);
    }
    super.onDestroy();
  }

  private void setBroadcastReceivers() {
    playReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("tinymod", "activity got PLAY broadcast");
        guiControls.showInfo(intent.getStringExtra("info"));
        guiControls.showButtonsForPlay();
      }
    };
    pauseReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("tinymod", "activity got PAUSE broadcast");
        guiControls.showButtonsForPause();
      }
    };
    stopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("tinymod", "activity got STOP broadcast");
        guiControls.showButtonsForStop();
      }
    };
    registerReceiver(playReceiver, new IntentFilter(getString(R.string.intent_action_play)));
    registerReceiver(pauseReceiver, new IntentFilter(getString(R.string.intent_action_pause)));
    registerReceiver(stopReceiver, new IntentFilter(getString(R.string.intent_action_stop)));
  }

  @Override
  protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
    final Message message = Message.obtain(null, TinyModService.MSG_PLAY_FILE);
    sendMessage(addStringParameter(message, "name", songPaths.get(songs.get(position))));
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
    b.putString(name, value); // more files using putStringArray()
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
      return s.endsWith(".mod") || s.endsWith(".med");
    }
  }

  private void playClicked() {
    sendMessage(Message.obtain(null, TinyModService.MSG_RESUME));
  }

  private void pauseClicked() {
    sendMessage(Message.obtain(null, TinyModService.MSG_PAUSE));
  }

  private void stopClicked() {
    sendMessage(Message.obtain(null, TinyModService.MSG_STOP));
  }

  public void updateSongList() {
    final List<File> fileList = listSongFiles(new File(MEDIA_PATH));
    songs.clear();
    songPaths.clear();
    for (final File file : fileList) {
      songs.add(file.getName());
      songPaths.put(file.getName(), file.getAbsolutePath());
    }
    Collections.sort(songs, String.CASE_INSENSITIVE_ORDER);
    setListAdapter(new ArrayAdapter<String>(this, R.layout.mod_item, songs));
  }

  private final class GuiControls {
    private final Button playButton;
    private final Button pauseButton;
    private final Button stopButton;
    private final TextView infoText;

    public GuiControls(final Button play, final Button pause, final Button stop, final TextView info) {
      playButton = play;
      pauseButton = pause;
      stopButton = stop;
      infoText = info;
      setButtonCallbacks();
      showButtonsForStop();
    }

    public void setButtonCallbacks() {
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

    public void showButtonsForPlay() {
      hideButton(playButton);
      showButton(pauseButton);
      stopButton.setEnabled(true);
    }

    public void showButtonsForPause() {
      showButton(playButton);
      hideButton(pauseButton);
      stopButton.setEnabled(true);
    }

    public void showButtonsForStop() {
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

    public void showInfo(final String s) {
      infoText.setText(s);
    }
  }
}
