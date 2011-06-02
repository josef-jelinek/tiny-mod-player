/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Projects\\josef-jelinek\\tiny-mod-player\\TinyModPlayer\\src\\player\\tinymod\\client\\TinyModServiceInterface.aidl
 */
package player.tinymod.client;
public interface TinyModServiceInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements player.tinymod.client.TinyModServiceInterface
{
private static final java.lang.String DESCRIPTOR = "player.tinymod.client.TinyModServiceInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an player.tinymod.client.TinyModServiceInterface interface,
 * generating a proxy if needed.
 */
public static player.tinymod.client.TinyModServiceInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof player.tinymod.client.TinyModServiceInterface))) {
return ((player.tinymod.client.TinyModServiceInterface)iin);
}
return new player.tinymod.client.TinyModServiceInterface.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_clearPlaylist:
{
data.enforceInterface(DESCRIPTOR);
this.clearPlaylist();
reply.writeNoException();
return true;
}
case TRANSACTION_addSongPlaylist:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.addSongPlaylist(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_playFile:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.playFile(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_pause:
{
data.enforceInterface(DESCRIPTOR);
this.pause();
reply.writeNoException();
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
this.stop();
reply.writeNoException();
return true;
}
case TRANSACTION_skipForward:
{
data.enforceInterface(DESCRIPTOR);
this.skipForward();
reply.writeNoException();
return true;
}
case TRANSACTION_skipBack:
{
data.enforceInterface(DESCRIPTOR);
this.skipBack();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements player.tinymod.client.TinyModServiceInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void clearPlaylist() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_clearPlaylist, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void addSongPlaylist(java.lang.String song) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(song);
mRemote.transact(Stub.TRANSACTION_addSongPlaylist, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void playFile(int position) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(position);
mRemote.transact(Stub.TRANSACTION_playFile, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void pause() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_pause, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void stop() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void skipForward() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_skipForward, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void skipBack() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_skipBack, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_clearPlaylist = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_addSongPlaylist = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_playFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_pause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_skipForward = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_skipBack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
}
public void clearPlaylist() throws android.os.RemoteException;
public void addSongPlaylist(java.lang.String song) throws android.os.RemoteException;
public void playFile(int position) throws android.os.RemoteException;
public void pause() throws android.os.RemoteException;
public void stop() throws android.os.RemoteException;
public void skipForward() throws android.os.RemoteException;
public void skipBack() throws android.os.RemoteException;
}
