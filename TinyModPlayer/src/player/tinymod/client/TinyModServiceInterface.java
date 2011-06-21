package player.tinymod.client;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface TinyModServiceInterface extends IInterface {
  public void clear() throws RemoteException;

  public void add(String song) throws RemoteException;

  public void play(int position) throws RemoteException;

  public void pause() throws RemoteException;

  public void stop() throws RemoteException;

  public void forward() throws RemoteException;

  public void backward() throws RemoteException;

  public static abstract class Stub extends Binder implements TinyModServiceInterface {
    private static final String DESCRIPTOR = "player.tinymod.client.TinyModServiceInterface";
    private static final int TRANSACTION_clear = IBinder.FIRST_CALL_TRANSACTION;
    private static final int TRANSACTION_add = IBinder.FIRST_CALL_TRANSACTION + 1;
    private static final int TRANSACTION_play = IBinder.FIRST_CALL_TRANSACTION + 2;
    private static final int TRANSACTION_pause = IBinder.FIRST_CALL_TRANSACTION + 3;
    private static final int TRANSACTION_stop = IBinder.FIRST_CALL_TRANSACTION + 4;
    private static final int TRANSACTION_forward = IBinder.FIRST_CALL_TRANSACTION + 5;
    private static final int TRANSACTION_backward = IBinder.FIRST_CALL_TRANSACTION + 6;

    public Stub() {
      this.attachInterface(this, DESCRIPTOR);
    }

    public static TinyModServiceInterface asInterface(final IBinder binder) {
      if (binder == null)
        return null;
      final IInterface iinterface = binder.queryLocalInterface(DESCRIPTOR);
      final boolean isTinyModServiceInterface = iinterface instanceof TinyModServiceInterface;
      return isTinyModServiceInterface ? (TinyModServiceInterface)iinterface : new Proxy(binder);
    }

    public IBinder asBinder() {
      return this;
    }

    @Override
    public boolean
        onTransact(final int code, final Parcel data, final Parcel reply, final int flags)
            throws RemoteException {
      switch (code) {
      case INTERFACE_TRANSACTION:
        reply.writeString(DESCRIPTOR);
        return true;
      case TRANSACTION_clear:
        data.enforceInterface(DESCRIPTOR);
        clear();
        return finishReply(reply);
      case TRANSACTION_add:
        data.enforceInterface(DESCRIPTOR);
        add(data.readString());
        return finishReply(reply);
      case TRANSACTION_play:
        data.enforceInterface(DESCRIPTOR);
        play(data.readInt());
        return finishReply(reply);
      case TRANSACTION_pause:
        data.enforceInterface(DESCRIPTOR);
        pause();
        return finishReply(reply);
      case TRANSACTION_stop:
        data.enforceInterface(DESCRIPTOR);
        stop();
        return finishReply(reply);
      case TRANSACTION_forward:
        data.enforceInterface(DESCRIPTOR);
        forward();
        return finishReply(reply);
      case TRANSACTION_backward:
        data.enforceInterface(DESCRIPTOR);
        backward();
        return finishReply(reply);
      }
      return super.onTransact(code, data, reply, flags);
    }

    private boolean finishReply(final Parcel reply) {
      reply.writeNoException();
      return true;
    }

    private static class Proxy implements TinyModServiceInterface {
      private final IBinder remote;

      Proxy(final IBinder remote) {
        this.remote = remote;
      }

      public IBinder asBinder() {
        return remote;
      }

      public void clear() throws RemoteException {
        transact(TRANSACTION_clear);
      }

      public void add(final String song) throws RemoteException {
        transact(TRANSACTION_add, song);
      }

      public void play(final int position) throws RemoteException {
        transact(TRANSACTION_play, position);
      }

      public void pause() throws RemoteException {
        transact(TRANSACTION_pause);
      }

      public void stop() throws RemoteException {
        transact(TRANSACTION_stop);
      }

      public void forward() throws RemoteException {
        transact(TRANSACTION_forward);
      }

      public void backward() throws RemoteException {
        transact(TRANSACTION_backward);
      }

      private void transact(final int code) throws RemoteException {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
          data.writeInterfaceToken(DESCRIPTOR);
          transact(code, data, reply);
        } finally {
          recycle(data, reply);
        }
      }

      private void transact(final int code, final String s) throws RemoteException {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
          data.writeInterfaceToken(DESCRIPTOR);
          data.writeString(s);
          transact(code, data, reply);
        } finally {
          recycle(data, reply);
        }
      }

      private void transact(final int code, final int n) throws RemoteException {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
          data.writeInterfaceToken(DESCRIPTOR);
          data.writeInt(n);
          transact(code, data, reply);
        } finally {
          recycle(data, reply);
        }
      }

      private void transact(final int code, final Parcel data, final Parcel reply)
          throws RemoteException {
        remote.transact(code, data, reply, 0);
        reply.readException();
      }

      private void recycle(final Parcel data, final Parcel reply) {
        reply.recycle();
        data.recycle();
      }
    }
  }
}