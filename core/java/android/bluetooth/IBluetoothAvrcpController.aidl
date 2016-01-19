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

import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothAvrcpRemoteMediaPlayers;
import android.bluetooth.BluetoothDevice;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;

/**
 * APIs for Bluetooth AVRCP controller service
 *
 * @hide
 */
interface IBluetoothAvrcpController {
    List<BluetoothDevice> getConnectedDevices();
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states);
    int getConnectionState(in BluetoothDevice device);
    void sendPassThroughCmd(in BluetoothDevice device, int keyCode, int keyState);
    BluetoothAvrcpPlayerSettings getPlayerSettings(in BluetoothDevice device);
    MediaMetadata getMetadata(in BluetoothDevice device);
    PlaybackState getPlaybackState(in BluetoothDevice device);
    boolean setPlayerApplicationSetting(in BluetoothAvrcpPlayerSettings plAppSetting);
    void sendGroupNavigationCmd(in BluetoothDevice device, int keyCode, int keyState);
    void startFetchingAlbumArt(in String mimeType, int height, int width, long maxSize);
    boolean SetBrowsedPlayer(in int playerId);
    boolean SetAddressedPlayer(in int playerId);
    BluetoothAvrcpRemoteMediaPlayers GetRemoteAvailableMediaPlayer();
    BluetoothAvrcpRemoteMediaPlayers GetAddressedPlayer();
    BluetoothAvrcpRemoteMediaPlayers GetBrowsedPlayer();
    int getSupportedFeatures(in BluetoothDevice device);
}
