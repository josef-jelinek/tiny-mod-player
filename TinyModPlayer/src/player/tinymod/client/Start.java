package player.tinymod.client;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import player.tinymod.R;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
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
  private TinyModServiceInterface serviceInterface;
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(final ComponentName className, final IBinder service) {
      serviceInterface = TinyModServiceInterface.Stub.asInterface(service);
      updateSongList();
    }

    public void onServiceDisconnected(final ComponentName className) {
      serviceInterface = null;
    }
  };

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.mod_list);
    bindService(new Intent(Start.this, TinyModService.class), serviceConnection,
        Context.BIND_AUTO_CREATE);
    ((Button)findViewById(R.id.play)).setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        if (serviceInterface != null)
          try {
            serviceInterface.play();
          } catch (final RemoteException e) {
            Log.w("tinymod", e.getMessage(), e);
          }
      }
    });
    ((Button)findViewById(R.id.stop)).setOnClickListener(new View.OnClickListener() {
      public void onClick(final View v) {
        if (serviceInterface != null)
          try {
            serviceInterface.stop();
          } catch (final RemoteException e) {
            Log.w("tinymod", e.getMessage(), e);
          }
      }
    });
  }

  public void updateSongList() {
    try {
      final List<File> fileList = listSongFiles(new File(MEDIA_PATH));
      serviceInterface.clear();
      for (final File file : fileList) {
        songs.add(file.getName());
        serviceInterface.add(file.getAbsolutePath());
      }
      setListAdapter(new ArrayAdapter<String>(this, R.layout.mod_item, songs));
    } catch (final DeadObjectException e) {
      Log.e("tinymod", e.getMessage(), e);
    } catch (final RemoteException e) {
      Log.e("tinymod", e.getMessage(), e);
    }
  }

  private List<File> listSongFiles(final File directory) {
    final File[] files = directory.listFiles(filter);
    return files == null ? new ArrayList<File>(0) : Arrays.asList(files);
  }

  @Override
  protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
    try {
      serviceInterface.play(position);
    } catch (final DeadObjectException e) {
      Log.e(getString(R.string.app_name), e.getMessage());
    } catch (final RemoteException e) {
      Log.e(getString(R.string.app_name), e.getMessage());
    }
  }

  private static class ModFilter implements FilenameFilter {
    public boolean accept(final File dir, final String name) {
      final String s = name.toLowerCase();
      return s.endsWith(".mod") || s.endsWith(".med") || s.endsWith(".mp3");
    }
  }
}
