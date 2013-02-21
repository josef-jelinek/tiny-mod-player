package gamod.client;

import gamod.client.ui.Controls;
import java.io.File;
import java.util.*;
import android.app.ListActivity;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.*;

public final class Start extends ListActivity {
  private static final String modPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Mods";
  private final List<String> songs = new ArrayList<String>();
  private final Map<String, String> songPaths = new HashMap<String, String>();
  private Controls guiControls = null;
  private Messenger serviceMessenger = null;
  private BroadcastReceiver playReceiver;
  private BroadcastReceiver pauseReceiver;
  private BroadcastReceiver stopReceiver;
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(final ComponentName className, final IBinder service) {
      serviceMessenger = new Messenger(service);
      Log.d("MOD", "activity sending " + PlayerService.MSG_GET_PLAYING_STATE);
      sendMessage(Message.obtain(null, PlayerService.MSG_GET_PLAYING_STATE));
    }

    public void onServiceDisconnected(final ComponentName className) {
      // Called when the service is unexpectedly disconnected (its process crashed).
      serviceMessenger = null;
    }
  };
  final Messenger activityMessenger = new Messenger(new Handler() {
    @Override
    public void handleMessage(final Message message) {
      if (message.what == PlayerService.MSG_SET_PLAYING_STATE) {
        Log.d("MOD", "activity got message " + message.what + "(" + message.arg1 + ")");
        if (message.arg1 == PlayerService.PLAYING_STATE_PLAY) {
          guiControls.showButtonsForPlay();
        } else if (message.arg1 == PlayerService.PLAYING_STATE_PAUSE) {
          guiControls.showButtonsForPause();
        } else {
          guiControls.showButtonsForStop();
        }
        guiControls.showInfo(message.getData().getString("info"));
        updateSongList();
      } else {
        Log.d("MOD", "activity got message " + message.what);
        super.handleMessage(message);
      }
    }
  });

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("MOD", "activity created " + modPath);
    initGui();
    setBroadcastReceivers();
    connectPlayerService();
  }

  private void initGui() {
    setContentView(R.layout.mod_list);
    guiControls = new Controls(this, findButton(R.id.play), findButton(R.id.pause), findButton(R.id.stop), findText(R.id.text_info));
  }

  private Button findButton(int id) {
    return (Button)findViewById(id);
  }

  private TextView findText(int id) {
    return (TextView)findViewById(id);
  }

  private void connectPlayerService() {
    Intent intent = new Intent(this, PlayerService.class);
    startService(intent);
    if (!bindService(intent, serviceConnection, 0))
      Toast.makeText(this, "Inicialization error", Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    Log.d("MOD", "activity destroyed");
    try {
      unbindService(serviceConnection);
      unregisterReceiver(playReceiver);
      unregisterReceiver(pauseReceiver);
      unregisterReceiver(stopReceiver);
    } catch (final Throwable t) {
      Log.e("MOD", "Failed to unbind from the service", t);
    }
    super.onDestroy();
  }

  private void setBroadcastReceivers() {
    playReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("MOD", "activity got PLAY broadcast");
        guiControls.showInfo(intent.getStringExtra("info"));
        guiControls.showButtonsForPlay();
      }
    };
    pauseReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("MOD", "activity got PAUSE broadcast");
        guiControls.showButtonsForPause();
      }
    };
    stopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("MOD", "activity got STOP broadcast");
        guiControls.showButtonsForStop();
      }
    };
    registerReceiver(playReceiver, new IntentFilter(getString(R.string.intent_action_play)));
    registerReceiver(pauseReceiver, new IntentFilter(getString(R.string.intent_action_pause)));
    registerReceiver(stopReceiver, new IntentFilter(getString(R.string.intent_action_stop)));
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    final Message message = Message.obtain(null, PlayerService.MSG_PLAY_FILE);
    sendMessage(addStringParameter(message, "name", songPaths.get(songs.get(position))));
  }

  void sendMessage(Message message) {
    if (serviceMessenger != null)
      try {
        message.replyTo = activityMessenger;
        serviceMessenger.send(message);
      } catch (RemoteException e) {
        Log.w("MOD", e.getMessage(), e);
      }
  }

  private Message addStringParameter(Message message, String name, String value) {
    final Bundle b = new Bundle();
    b.putString(name, value); // more files using putStringArray()
    message.setData(b);
    return message;
  }

  private static List<File> listSongFiles(File directory) {
    final File[] files = directory.listFiles();
    return files == null ? new ArrayList<File>(0) : Arrays.asList(files);
  }

  public void playClicked() {
    sendMessage(Message.obtain(null, PlayerService.MSG_RESUME));
  }

  public void pauseClicked() {
    sendMessage(Message.obtain(null, PlayerService.MSG_PAUSE));
  }

  public void stopClicked() {
    sendMessage(Message.obtain(null, PlayerService.MSG_STOP));
  }

  public void updateSongList() {
    final List<File> fileList = listSongFiles(new File(modPath));
    songs.clear();
    songPaths.clear();
    for (final File file : fileList) {
      songs.add(file.getName());
      songPaths.put(file.getName(), file.getAbsolutePath());
    }
    Collections.sort(songs, String.CASE_INSENSITIVE_ORDER);
    setListAdapter(new ArrayAdapter<String>(this, R.layout.mod_item, songs));
  }
}
