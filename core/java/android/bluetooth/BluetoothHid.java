/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2010 Texas Instruments - created for HID
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

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.server.BluetoothHidService;
import android.content.Context;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * Public API for controlling the Bluetooth HID Profile Service.
 *
 * BluetoothHid is a proxy object for controlling the Bluetooth HID
 * Service via IPC.
 *
 * Creating a BluetoothHid object will initiate a binding with the
 * BluetoothHid service. Users of this object should call close() when they
 * are finished, so that this proxy object can unbind from the service.
 *
 * Currently the BluetoothHid service runs in the system server and this
 * proxy object will be immediately bound to the service on construction.
 *
 * Currently this class provides methods to connect to HID input devices.
 *
 * @hide
 */
public final class BluetoothHid {
    private static final String TAG = "BluetoothHid";
    private static final boolean DBG = false;

    /** int extra for ACTION_INPUT_STATE_CHANGED */
    public static final String EXTRA_INPUT_STATE =
        "android.bluetooth.hid.extra.INPUT_STATE";
    /** int extra for ACTION_INPUT_STATE_CHANGED */
    public static final String EXTRA_PREVIOUS_INPUT_STATE =
        "android.bluetooth.hid.extra.PREVIOUS_INPUT_STATE";

    /** Indicates the state of an HID input device has changed.
     * This intent will always contain EXTRA_INPUT_STATE,
     * EXTRA_PREVIOUS_INPUT_STATE and BluetoothDevice.EXTRA_DEVICE
     * extras.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_INPUT_STATE_CHANGED =
        "android.bluetooth.hid.action.INPUT_STATE_CHANGED";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING   = 1;
    public static final int STATE_CONNECTED    = 2;
    public static final int STATE_DISCONNECTING = 3;

    private final IBluetoothHid mService;
    private final Context mContext;

   /**
     * Create a BluetoothHid proxy object for interacting with the local
     * Bluetooth HID service.
     * @param c Context
     */
    public BluetoothHid(Context c) {
        mContext = c;

        IBinder b = ServiceManager.getService(BluetoothHidService.BLUETOOTH_HID_SERVICE);
        if (b != null) {
            mService = IBluetoothHid.Stub.asInterface(b);
        } else {
            Log.w(TAG, "Bluetooth HID service not available!");

            // Instead of throwing an exception which prevents people from going
            // into Wireless settings in the emulator. Let it crash later when it is actually used.
            mService = null;
        }
    }

   /** Initiate a connection to an HID input device.
     *  Listen for INPUT_STATE_CHANGED_ACTION to find out when the
     *  connection is completed.
     *  @param device Remote BT device.
     *  @return false on immediate error, true otherwise
     *  @hide
     */
    public boolean connectInput(BluetoothDevice device) {
        if (DBG) log("connectInput(" + device + ")");
        try {
            return mService.connectInput(device);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    /** Initiate disconnect from an HID input device.
     *  Listen for INPUT_STATE_CHANGED_ACTION to find out when
     *  disconnect is completed.
     *  @param device Remote BT device.
     *  @return false on immediate error, true otherwise
     *  @hide
     */
    public boolean disconnectInput(BluetoothDevice device) {
        if (DBG) log("disconnectInput(" + device + ")");
        try {
            return mService.disconnectInput(device);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

   /** Check if a specified HID Input device is connected.
     *  @param device Remote BT device.
     *  @return True if connected (or playing), false otherwise and on error.
     *  @hide
     */
    public boolean isInputConnected(BluetoothDevice device) {
        if (DBG) log("isInputConnected(" + device + ")");
        int state = getInputState(device);
        return state == STATE_CONNECTED;
    }

    /** Check if any HID input device is connected.
     * @return a unmodifiable set of connected HID device, or null on error.
     * @hide
     */
    public Set<BluetoothDevice> getConnectedInputs() {
        if (DBG) log("getConnectedSinks()");
        try {
            return Collections.unmodifiableSet(
                    new HashSet<BluetoothDevice>(Arrays.asList(mService.getConnectedInputs())));
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return null;
        }
    }

    /** Get the state of an HID input device
     *  @param device Remote BT device.
     *  @return State code, one of STATE_
     *  @hide
     */
    public int getInputState(BluetoothDevice device) {
        if (DBG) log("getInputState(" + device + ")");
        try {
            return mService.getInputState(device);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return BluetoothHid.STATE_DISCONNECTED;
        }
    }

    /** Helper for converting a state to a string.
     * For debug use only - strings are not internationalized.
     * @hide
     */
    public static String stateToString(int state) {
        switch (state) {
        case STATE_DISCONNECTED:
            return "disconnected";
        case STATE_CONNECTING:
            return "connecting";
        case STATE_CONNECTED:
            return "connected";
        case STATE_DISCONNECTING:
            return "disconnecting";
        default:
            return "<unknown state " + state + ">";
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
