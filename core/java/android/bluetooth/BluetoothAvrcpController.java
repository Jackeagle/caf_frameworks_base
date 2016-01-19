/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the public APIs to control the Bluetooth AVRCP Controller
 * profile.
 *
 *<p>BluetoothAvrcpController is a proxy object for controlling the Bluetooth AVRCP
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothAvrcpController proxy object.
 *
 * {@hide}
 */
public final class BluetoothAvrcpController implements BluetoothProfile {
    private static final String TAG = "BluetoothAvrcpController";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    /**
     * Intent used to broadcast the change in connection state of the AVRCP Controller
     * profile.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     *   <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
     *   <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     *   <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
     * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to
     * receive.
     */
    public static final String ACTION_CONNECTION_STATE_CHANGED =
        "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the change in metadata state of playing track on the AVRCP
     * AG.
     *
     * <p>This intent will have the two extras:
     * <ul>
     *    <li> {@link #EXTRA_METADATA} - {@link MediaMetadata} containing the current metadata.</li>
     *    <li> {@link #EXTRA_PLAYBACK} - {@link PlaybackState} containing the current playback
     *    state. </li>
     * </ul>
     */
    public static final String ACTION_TRACK_EVENT =
        "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";


    /**
     * Intent used to broadcast the change in player application setting state on AVRCP AG.
     *
     * <p>This intent will have the following extras:
     * <ul>
     *    <li> {@link #EXTRA_PLAYER_SETTING} - {@link BluetoothAvrcpPlayerSettings} containing the
     *    most recent player setting. </li>
     * </ul>
     */
    public static final String ACTION_PLAYER_SETTING =
        "android.bluetooth.avrcp-controller.profile.action.PLAYER_SETTING";

    public static final String EXTRA_METADATA =
            "android.bluetooth.avrcp-controller.profile.extra.METADATA";

    public static final String EXTRA_PLAYBACK =
            "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";

    public static final String EXTRA_PLAYER_SETTING =
            "android.bluetooth.avrcp-controller.profile.extra.PLAYER_SETTING";


    public static final String AVAILABLE_MEDIA_PLAYERS_UPDATE =
            "android.bluetooth.avrcp-controller.profile.action.REMOTE_PLAYERS_UPDATE";

    public static final String BROWSED_PLAYER_CHANGED =
            "android.bluetooth.avrcp-controller.profile.action.BROWSED_PLAYER_CHANGED";

    public static final String ADDRESSED_PLAYER_CHANGED =
            "android.bluetooth.avrcp-controller.profile.action.ADDRESSED_PLAYER_CHANGED";

    public static final String EXTRA_REMOTE_PLAYERS =
            "android.bluetooth.avrcp-controller.profile.extra.PLAYERS";

    public static final String EXTRA_PLAYER_UPDATE_STATUS =
            "android.bluetooth.avrcp-controller.profile.extra.STATUS";

    public static final String BROWSE_COMMAND_GET_NOW_PLAYING_LIST =
            "android.bluetooth.avrcp-controller.browse.GET_NPL";

    public static final String BROWSE_COMMAND_ADD_TO_NOW_PLAYING_LIST =
            "android.bluetooth.avrcp-controller.browse.ADD_TO_NPL";

    public static final String EXTRA_ADD_TO_NOW_PLAYING_LIST =
            "id";

    public static final String SESSION_EVENT_REFRESH_LIST =
            "android.bluetooth.avrcp-controller.event.REFRESH_LIST";

    public static final String AVRCP_BROWSE_THUMBNAILS_UPDATE =
            "android.bluetooth.avrcp-controller.profile.action.THUMBNAIL_UPDATE";
    public static final String EXTRA_MEDIA_IDS =
            "android.bluetooth.avrcp-controller.profile.extra.MEDIA_IDS";
    public static final String EXTRA_THUMBNAILS =
            "android.bluetooth.avrcp-controller.profile.extra.THUMBNAILS";

    public static final String BROWSE_COMMAND_BROWSE_FOLDER_UP = "UP";
    public static final String BROWSE_COMMAND_BROWSE_FOLDER_DOWN = "DOWN";

    /*
     * KeyCoded for Pass Through Commands
     */
    public static final int PASS_THRU_CMD_ID_PLAY = 0x44;
    public static final int PASS_THRU_CMD_ID_PAUSE = 0x46;
    public static final int PASS_THRU_CMD_ID_VOL_UP = 0x41;
    public static final int PASS_THRU_CMD_ID_VOL_DOWN = 0x42;
    public static final int PASS_THRU_CMD_ID_STOP = 0x45;
    public static final int PASS_THRU_CMD_ID_FF = 0x49;
    public static final int PASS_THRU_CMD_ID_REWIND = 0x48;
    public static final int PASS_THRU_CMD_ID_FORWARD = 0x4B;
    public static final int PASS_THRU_CMD_ID_BACKWARD = 0x4C;
    /* Key State Variables */
    public static final int KEY_STATE_PRESSED = 0;
    public static final int KEY_STATE_RELEASED = 1;
    /* Group Navigation Key Codes */
    public static final int PASS_THRU_CMD_ID_NEXT_GRP = 0x00;
    public static final int PASS_THRU_CMD_ID_PREV_GRP = 0x01;
    /* Remote supported Features */
    public static final int BTRC_FEAT_NONE = 0x00;
    public static final int BTRC_FEAT_METADATA = 0x01;
    public static final int BTRC_FEAT_ABSOLUTE_VOLUME = 0x02;
    public static final int BTRC_FEAT_BROWSE = 0x04;
    public static final int  BTRC_FEAT_COVER_ART = 0x08;

    /* Browsing constants */
    public static final int PLAYER_FEATURE_MASK_SIZE = 16;
    public static final int PLAYER_FEATURE_BITMASK_BROWSING_BIT = 59;
    public static final int PLAYER_FEATURE_BITMASK_ONLY_BROWSABLE_WHEN_ADDRESSED = 63;
    public static final int PLAYER_FEATURE_BITMASK_SEARCH_BIT = 60;
    public static final int PLAYER_FEATURE_BITMASK_ONLY_SEARCHABLE_WHEN_ADDRESSED = 64;
    public static final int PLAYER_FEATURE_BITMASK_NOW_PLAYING_BIT = 65;
    public static final int PLAYER_FEATURE_BITMASK_ADD_TO_NOW_PLAYING_BIT = 61;

    public static final int AVRCP_SCOPE_MEDIA_PLAYLIST = 0;
    public static final int AVRCP_SCOPE_VFS = 1;
    public static final int AVRCP_SCOPE_SEARCH = 2;
    public static final int AVRCP_SCOPE_NOW_PLAYING = 3;
    public static final int AVRCP_SCOPE_NONE = 4;

    private Context mContext;
    private ServiceListener mServiceListener;
    private IBluetoothAvrcpController mService;
    private BluetoothAdapter mAdapter;

    final private IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
        new IBluetoothStateChangeCallback.Stub() {
            public void onBluetoothStateChange(boolean up) {
                if (DBG) Log.d(TAG, "onBluetoothStateChange: up=" + up);
                if (!up) {
                    if (VDBG) Log.d(TAG,"Unbinding service...");
                    synchronized (mConnection) {
                        try {
                            mService = null;
                            mContext.unbindService(mConnection);
                        } catch (Exception re) {
                            Log.e(TAG,"",re);
                        }
                    }
                } else {
                    synchronized (mConnection) {
                        try {
                            if (mService == null) {
                                if (VDBG) Log.d(TAG,"Binding service...");
                                doBind();
                            }
                        } catch (Exception re) {
                            Log.e(TAG,"",re);
                        }
                    }
                }
            }
      };

    /**
     * Create a BluetoothAvrcpController proxy object for interacting with the local
     * Bluetooth AVRCP service.
     *
     */
    /*package*/ BluetoothAvrcpController(Context context, ServiceListener l) {
        mContext = context;
        mServiceListener = l;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG,"",e);
            }
        }

        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothAvrcpController.class.getName());
        ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !mContext.bindServiceAsUser(intent, mConnection, 0,
                android.os.Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth AVRCP Controller Service with " + intent);
            return false;
        }
        return true;
    }

    /*package*/ void close() {
        mServiceListener = null;
        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG,"",e);
            }
        }

        synchronized (mConnection) {
            if (mService != null) {
                try {
                    mService = null;
                    mContext.unbindService(mConnection);
                } catch (Exception re) {
                    Log.e(TAG,"",re);
                }
            }
        }
    }

    public void finalize() {
        close();
    }

    /**
     * {@inheritDoc}
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) log("getConnectedDevices()");
        if (mService != null && isEnabled()) {
            try {
                return mService.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (VDBG) log("getDevicesMatchingStates()");
        if (mService != null && isEnabled()) {
            try {
                return mService.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    public int getConnectionState(BluetoothDevice device) {
        if (VDBG) log("getState(" + device + ")");
        if (mService != null && isEnabled()
            && isValidDevice(device)) {
            try {
                return mService.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public void sendPassThroughCmd(BluetoothDevice device, int keyCode, int keyState) {
        if (DBG) Log.d(TAG, "sendPassThroughCmd dev = " + device + " key " + keyCode + " State = " + keyState);
        if (mService != null && isEnabled()) {
            try {
                mService.sendPassThroughCmd(device, keyCode, keyState);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in sendPassThroughCmd()", e);
                return;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
    }

    /**
     * Gets the player application settings.
     *
     * @return the {@link BluetoothAvrcpPlayerSettings} or {@link null} if there is an error.
     */
    public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getPlayerSettings");
        BluetoothAvrcpPlayerSettings settings = null;
        if (mService != null && isEnabled()) {
            try {
                settings = mService.getPlayerSettings(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in getPlayerSettings() " + e);
                return null;
            }
        }
        return settings;
    }

    /**
     * Gets the metadata for the current track.
     *
     * This should be usually called when application UI needs to be updated, eg. when the track
     * changes or immediately after connecting and getting the current state.
     * @return the {@link MediaMetadata} or {@link null} if there is an error.
     */
    public MediaMetadata getMetadata(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getMetadata");
        MediaMetadata metadata = null;
        if (mService != null && isEnabled()) {
            try {
                metadata = mService.getMetadata(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in getMetadata() " + e);
                return null;
            }
        }
        return metadata;
    }

    /**
     * Gets the playback state for current track.
     *
     * When the application is first connecting it can use current track state to get playback info.
     * For all further updates it should listen to notifications.
     * @return the {@link PlaybackState} or {@link null} if there is an error.
     */
    public PlaybackState getPlaybackState(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getPlaybackState");
        PlaybackState playbackState = null;
        if (mService != null && isEnabled()) {
            try {
                playbackState = mService.getPlaybackState(device);
            } catch (RemoteException e) {
                Log.e(TAG,
                    "Error talking to BT service in getPlaybackState() " + e);
                return null;
            }
        }
        return playbackState;
    }

    /**
     * Sets the player app setting for current player.
     * returns true in case setting is supported by remote, false otherwise
     */
    public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings plAppSetting) {
        if (DBG) Log.d(TAG, "setPlayerApplicationSetting");
        if (mService != null && isEnabled()) {
            try {
                return mService.setPlayerApplicationSetting(plAppSetting);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in setPlayerApplicationSetting() " + e);
                return false;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Informs AvrcpControllerService to start fetching Album Art.
     * Fetching will start only after this api is called.
     * input parameters are preferred values from app.
     * if input parameters are null, 0, 0, 0: image in native encoding will be fetched.
     */
    public void startFetchingAlbumArt(String mimeType, int height, int width, long maxSize) {
        if (DBG) Log.d(TAG, "startFetchingAlbumArt");
        if (mService != null && isEnabled()) {
            try {
                mService.startFetchingAlbumArt(mimeType, height, width, maxSize);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in startFetchingAlbumArt() " + e);
                return;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return ;
    }
    /*
     * Send Group Navigation Command to Remote.
     */
    public void sendGroupNavigationCmd(BluetoothDevice device, int keyCode, int keyState) {
        Log.d(TAG, "sendGroupNavigationCmd dev = " + device + " key " + keyCode + " State = " + keyState);
        if (mService != null && isEnabled()) {
            try {
                mService.sendGroupNavigationCmd(device, keyCode, keyState);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in sendGroupNavigationCmd()", e);
                return;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
    }
    /*
     * Sets player with given ID as browsed player, returns false if player id is not in playerList
     * or is not browsable.
     */
    public boolean SetBrowsedPlayer(int playerId) {
        Log.d(TAG, "setBrowsePlayer playerId = " + playerId);
        if (mService != null && isEnabled()) {
            try {
                return mService.SetBrowsedPlayer(playerId);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in setBrowsePlayer()", e);
                return false;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }
    /*
     * Sets player with given ID as browsed player, returns false if player id is not in playerList
     */
    public boolean SetAddressedPlayer(int playerId) {
        Log.d(TAG, "SetAddressedPlayer playerId = " + playerId);
        if (mService != null && isEnabled()) {
            try {
                return mService.SetAddressedPlayer(playerId);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in SetAddressedPlayer()", e);
                return false;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }
    /*
     * To get current list of players, Will return an object of type BluetoothAvrcpRemoteMediaPlayers
     * null otherwise
     */
    public BluetoothAvrcpRemoteMediaPlayers GetRemoteAvailableMediaPlayer() {
        Log.d(TAG, "GetRemoteAvailableMediaPlayer ");
        if (mService != null && isEnabled()) {
            try {
                return mService.GetRemoteAvailableMediaPlayer();
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in GetRemoteAvailableMediaPlayer()", e);
                return null;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return null;
    }
    public BluetoothAvrcpRemoteMediaPlayers GetAddressedPlayer() {
        Log.d(TAG, "GetAddressedPlayer ");
        if (mService != null && isEnabled()) {
            try {
                return mService.GetAddressedPlayer();
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in GetAddressedPlayer()", e);
                return null;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return null;
    }
    public BluetoothAvrcpRemoteMediaPlayers GetBrowsedPlayer() {
        Log.d(TAG, "GetBrowsedPlayer ");
        if (mService != null && isEnabled()) {
            try {
                return mService.GetBrowsedPlayer();
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in GetBrowsedPlayer()", e);
                return null;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return null;
    }
    public int getSupportedFeatures(BluetoothDevice device) {
        Log.d(TAG, "getSupportedFeatures dev = " + device);
        if (mService != null && isEnabled()) {
            try {
                return mService.getSupportedFeatures(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in getSupportedFeatures()", e);
                return 0;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return 0;
    }
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) Log.d(TAG, "Proxy object connected");
            mService = IBluetoothAvrcpController.Stub.asInterface(service);

            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.AVRCP_CONTROLLER,
                        BluetoothAvrcpController.this);
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) Log.d(TAG, "Proxy object disconnected");
            mService = null;
            if (mServiceListener != null) {
                mServiceListener.onServiceDisconnected(BluetoothProfile.AVRCP_CONTROLLER);
            }
        }
    };

    private boolean isEnabled() {
       if (mAdapter.getState() == BluetoothAdapter.STATE_ON) return true;
       return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
       if (device == null) return false;

       if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) return true;
       return false;
    }

    private static void log(String msg) {
      Log.d(TAG, msg);
    }
}
