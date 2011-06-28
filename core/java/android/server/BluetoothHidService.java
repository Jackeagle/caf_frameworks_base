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

/**
 * TODO: Move this to services.jar
 * and make the contructor package private again.
 * @hide
 */
package android.server;

import android.bluetooth.BluetoothHid;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothHid;
import android.os.ParcelUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BluetoothHidService extends IBluetoothHid.Stub {
    private static final String TAG = "BluetoothHidService";
    private static final boolean DBG = true;

    public static final String BLUETOOTH_HID_SERVICE = "bluetooth_Hid";

    private static final String BLUETOOTH_ADMIN_PERM = android.Manifest.permission.BLUETOOTH_ADMIN;
    private static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;

    private static final String BLUETOOTH_ENABLED = "bluetooth_enabled";

    private static final int MESSAGE_CONNECT_TO = 1;

    private static final String PROPERTY_STATE = "Connected";

    private static final int INPUT_STATE_DISCONNECTED = 0;
    private static final int INPUT_STATE_CONNECTING = 1;
    private static final int INPUT_STATE_CONNECTED = 2;

    private static int mInputCount;

    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private HashMap<BluetoothDevice, Integer> mInputDevices;
    private final BluetoothService mBluetoothService;
    private final BluetoothAdapter mAdapter;
    private int   mTargetInputState;
    private class InputDeviceState {
        public String Connected;
        public Boolean state;
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);
                switch (state) {
                case BluetoothAdapter.STATE_ON:
                    onBluetoothEnable();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    onBluetoothDisable();
                    break;
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                   BluetoothDevice.ERROR);
                switch(bondState) {
                case BluetoothDevice.BOND_BONDED:
                    //setSinkPriority(device, BluetoothA2dp.PRIORITY_AUTO);
                    break;
                case BluetoothDevice.BOND_BONDING:
                case BluetoothDevice.BOND_NONE:
                    //setSinkPriority(device, BluetoothA2dp.PRIORITY_OFF);
                    break;
                }
            } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                if (isInputDevice(device)) {
                    //The device is  a valid input device Hence allow it to connect
                    Message msg = Message.obtain(mHandler, MESSAGE_CONNECT_TO, device);
                    mHandler.sendMessageDelayed(msg, 6000);
                }
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                synchronized (this) {
                    if (mInputDevices.containsKey(device)) {
                        int state = mInputDevices.get(device);
                        handleInputStateChange(device, state, BluetoothHid.STATE_DISCONNECTED);
                    }
                }
            }
        }
    };

    public BluetoothHidService(Context context, BluetoothService bluetoothService) {
        mContext = context;

        mBluetoothService = bluetoothService;
        if (mBluetoothService == null) {
            throw new RuntimeException("Platform does not support Bluetooth");
        }

        if (!initNative()) {
            throw new RuntimeException("Could not init BluetoothHidService");
        }

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(mReceiver, mIntentFilter);

        mInputDevices = new HashMap<BluetoothDevice, Integer>();

        if (mBluetoothService.isEnabled())
            onBluetoothEnable();
        mTargetInputState = -1;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanupNative();
        } finally {
            super.finalize();
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_CONNECT_TO:
                BluetoothDevice device = (BluetoothDevice) msg.obj;
                // check bluetooth is still on, device is still preferred, and
                // nothing is currently connected
                if (mBluetoothService.isEnabled()
                       && lookupInputsMatchingStates(new int[] {
                            BluetoothHid.STATE_CONNECTING,
                            BluetoothHid.STATE_CONNECTED,
                            BluetoothHid.STATE_DISCONNECTING}).size() == 0) {
                    log("Auto-connecting HID to input " + device);
                    connectInput(device);
                }
                break;
            }
        }
    };

    private int convertBluezInputStringtoState(String value) {
        if (value.equals("false"))
            return BluetoothHid.STATE_DISCONNECTED;
		if (value.equals("true"))
            return BluetoothHid.STATE_CONNECTED;
        return -1;
    }

    private boolean isInputDevice(BluetoothDevice device) {
        ParcelUuid[] uuids = mBluetoothService.getRemoteUuids(device.getAddress());
        if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HID)) {
            return true;
        }
        return false;
    }

    private synchronized boolean addInput (BluetoothDevice device) {
        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        String propValues[] = (String []) getInputPropertiesNative(path);
        if (propValues == null) {
            Log.e(TAG, "Error while getting AudioSink properties for device: " + device);
            return false;
        }
        Integer state = null;
        // Properties are name-value pairs
        for (int i = 0; i < propValues.length; i+=2) {
            if (propValues[i].equals(PROPERTY_STATE)) {
                state = new Integer(convertBluezInputStringtoState(propValues[i+1]));
                break;
            }
	}
	mInputDevices.put(device, state);
	handleInputStateChange(device, BluetoothHid.STATE_DISCONNECTED, state);
        return true;
    }

    private synchronized void onBluetoothEnable() {
        String devices = mBluetoothService.getProperty("Devices");
        mInputCount = 0;
        if (devices != null) {
            String [] paths = devices.split(",");
            for (String path: paths) {
                String address = mBluetoothService.getAddressFromObjectPath(path);
                BluetoothDevice device = mAdapter.getRemoteDevice(address);
                ParcelUuid[] remoteUuids = mBluetoothService.getRemoteUuids(address);
                if (remoteUuids != null)
                    if (BluetoothUuid.containsAnyUuid(remoteUuids,
                            new ParcelUuid[] {BluetoothUuid.HID})) {
                        addInput(device);
                    }
                }
	} 
    }

    private synchronized void onBluetoothDisable() {
        if (!mInputDevices.isEmpty()) {
            BluetoothDevice[] devices = new BluetoothDevice[mInputDevices.size()];
            devices = mInputDevices.keySet().toArray(devices);
            for (BluetoothDevice device : devices) {
                int state = getInputState(device);
                switch (state) {
                    case BluetoothHid.STATE_CONNECTING:
                    case BluetoothHid.STATE_CONNECTED:
                        disconnectInputNative(mBluetoothService.getObjectPathFromAddress(
                                device.getAddress()));
                        handleInputStateChange(device, state, BluetoothHid.STATE_DISCONNECTED);
                        break;
                    case BluetoothHid.STATE_DISCONNECTING:
                        handleInputStateChange(device, BluetoothHid.STATE_DISCONNECTING,
                                              BluetoothHid.STATE_DISCONNECTED);
                        break;
                }
            }
            mInputDevices.clear();
        }

      }

    public synchronized boolean connectInput(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                                "Need BLUETOOTH_ADMIN permission");
	if (DBG) log("connectInput(" + device + ")");
        // ignore if there are any active sinks
        if (lookupInputsMatchingStates(new int[] {
                BluetoothHid.STATE_CONNECTING,
                BluetoothHid.STATE_CONNECTED,
                BluetoothHid.STATE_DISCONNECTING}).size() != 0) {
            return false;
        }

        if (mInputDevices.get(device) == null && !addInput(device))
            return false;

        int state = mInputDevices.get(device);

        switch (state) {
        case BluetoothHid.STATE_CONNECTED:
        case BluetoothHid.STATE_DISCONNECTING:
            return false;
        case BluetoothHid.STATE_CONNECTING:
            return true;
        }
        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        if (path == null)
            return false;
	// State is DISCONNECTED
        mInputDevices.put(device, BluetoothHid.STATE_CONNECTING);
        if (!connectInputNative(path)) {
            return false;
        }
        return true;
    }

    public synchronized boolean disconnectInput(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                                "Need BLUETOOTH_ADMIN permission");
        if (DBG) log("disconnectInput(" + device + ")");
        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        if (path == null) {
            return false;
        }
        switch (getInputState(device)) {
        case BluetoothHid.STATE_DISCONNECTED:
            return false;
        case BluetoothHid.STATE_DISCONNECTING:
            return true;
        }
	// State is CONNECTING or CONNECTED or PLAYING
        mInputDevices.put(device, BluetoothHid.STATE_DISCONNECTING);
        if (!disconnectInputNative(path)) {
            return false;
        } else {
            return true;
        }
    }

    public synchronized BluetoothDevice[] getConnectedInputs() {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Set<BluetoothDevice> inputs = lookupInputsMatchingStates(
                new int[] {BluetoothHid.STATE_CONNECTED});
        return inputs.toArray(new BluetoothDevice[inputs.size()]);
    }

    public synchronized int getInputState(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
		Integer state = mInputDevices.get(device);
        if (state == null)
            return BluetoothHid.STATE_DISCONNECTED;
        return state;
    }

    private synchronized void onInputPropertyChanged(String path, String []propValues) {
        if (!mBluetoothService.isEnabled()) {
            return;
        }

        String name = propValues[0];
        String address = mBluetoothService.getAddressFromObjectPath(path);

        if (address == null) {
            Log.e(TAG, "onInputPropertyChanged: Address of the remote device in null");
            return;
        }

        BluetoothDevice device = mAdapter.getRemoteDevice(address);

        if (name.equals(PROPERTY_STATE)) {
            int state = convertBluezInputStringtoState(propValues[1]);
            if (mInputDevices.get(device) == null) {
                // This is for an incoming connection for a device not known to us.
                // We have authorized it and bluez state has changed.
                addInput(device);
            } else {
                int prevState = mInputDevices.get(device);
                handleInputStateChange(device, prevState, state);
            }
        }
    }

    private void handleInputStateChange(BluetoothDevice device, int prevState, int state) {
		if (state != prevState) {
            if (state == BluetoothHid.STATE_DISCONNECTED ||
                    state == BluetoothHid.STATE_DISCONNECTING) {
                mInputCount--;
            } else if (state == BluetoothHid.STATE_CONNECTED) {
                mInputCount ++;
            }
	    mInputDevices.put(device, state);
            mTargetInputState = -1;
            if (DBG) log("HID input state : device: " + device + " State:" + prevState + "->" + state);
        }
    }

    private synchronized Set<BluetoothDevice> lookupInputsMatchingStates(int[] states) {
        Set<BluetoothDevice> inputs = new HashSet<BluetoothDevice>();
        if (mInputDevices.isEmpty()) {
            return inputs;
        }
        for (BluetoothDevice device: mInputDevices.keySet()) {
            int inputState = getInputState(device);
            for (int state : states) {
                if (state == inputState) {
                    inputs.add(device);
                    break;
                }
            }
        }
        return inputs;
    }
   
    @Override
    protected synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mInputDevices.isEmpty()) return;
        pw.println("Cached audio devices:");
        for (BluetoothDevice device : mInputDevices.keySet()) {
            int state = mInputDevices.get(device);
            pw.println(device + " " + BluetoothHid.stateToString(state));
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private native boolean initNative();
    private native void cleanupNative();
    private synchronized native boolean connectInputNative(String path);
    private synchronized native boolean disconnectInputNative(String path);
    private synchronized native Object []getInputPropertiesNative(String path);
}
