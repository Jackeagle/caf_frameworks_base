
/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of Linux Foundation nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDeviceProfileState;
import android.bluetooth.BluetoothHogpDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfileState;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This handles all the operations on the HID profile.
 * All functions are called by BluetoothService, as Bluetooth Service
 * is the Service handler for the HID profile.
 */
final class BluetoothHogpProfileHandler {
    private static final String TAG = "BluetoothHogpProfileHandler";
    private static final boolean DBG = true;
    final static String ACTION_HOGP_CONNECTION_STATE_CHANGED = "android.bluetooth.hogp.profile.action.low.CONNECTION_STATE_CHANGED";
    final static String EXTRA_CONNECTED = "CONNECTED";

    public static BluetoothHogpProfileHandler sInstance;
    private Context mContext;
    private BluetoothService mBluetoothService;
    private final HashMap<BluetoothDevice, Integer> mInputDevices;
    private final BluetoothProfileState mInputProfileState;
    private IHogpDevice mService;

    private BluetoothHogpProfileHandler(Context context, BluetoothService service) {
        mContext = context;
        mBluetoothService = service;
        mInputDevices = new HashMap<BluetoothDevice, Integer>();
        mInputProfileState = new BluetoothProfileState(mContext, BluetoothProfileState.HOGP);
        mInputProfileState.start();
        if (!context.bindService(new Intent(IHogpDevice.class.getName()), mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Could not bind to Bluetooth Hogp Service");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HOGP_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
    }

    static synchronized BluetoothHogpProfileHandler getInstance(Context context,
            BluetoothService service) {
        if (sInstance == null) sInstance = new BluetoothHogpProfileHandler(context, service);
        return sInstance;
    }

    boolean connectInputDevice(BluetoothDevice device,
                                            BluetoothDeviceProfileState state) {
        String objectPath = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        if (objectPath == null ||
            getInputDeviceConnectionState(device) != BluetoothHogpDevice.STATE_DISCONNECTED ||
            getInputDevicePriority(device) == BluetoothHogpDevice.PRIORITY_OFF) {
            return false;
        }
        if (state != null) {
            Message msg = new Message();
            msg.arg1 = BluetoothDeviceProfileState.CONNECT_HOGP_OUTGOING;
            msg.obj = state;
            mInputProfileState.sendMessage(msg);
            return true;
        }
        return false;
    }

    boolean connectInputDeviceInternal(BluetoothDevice device) {
        if(mService == null) {
            return false;
        }
        handleInputDeviceStateChange(device, BluetoothHogpDevice.STATE_CONNECTING);
        try {
            if (!mService.connect(device)) {
                handleInputDeviceStateChange(device, BluetoothHogpDevice.STATE_DISCONNECTED);
                return false;
            }
        } catch (RemoteException e) {
            Log.e(TAG,  Log.getStackTraceString(new Throwable()));
            handleInputDeviceStateChange(device, BluetoothHogpDevice.STATE_DISCONNECTED);
            return false;
        }

        /*if(DBG) Log.d(TAG, "trying to connect to HOGP device");
        Intent intent = new Intent(ACTION_HOGP_CONNECTION_REQUEST);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra("CONNECTED", true);
        mContext.sendBroadcast(intent, BluetoothService.BLUETOOTH_PERM);
    */
        return true;
    }

    boolean disconnectInputDevice(BluetoothDevice device,
                                               BluetoothDeviceProfileState state) {
        String objectPath = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        if (objectPath == null ||
                getInputDeviceConnectionState(device) == BluetoothHogpDevice.STATE_DISCONNECTED) {
            try {
                mService.disconnect(device);
            } catch(Exception e) {
                Log.d(TAG, "Exception"+e);
            }
            return false;
        }
        if (state != null) {
            Message msg = new Message();
            msg.arg1 = BluetoothDeviceProfileState.DISCONNECT_HOGP_OUTGOING;
            msg.obj = state;
            mInputProfileState.sendMessage(msg);
            return true;
        }
        return false;
    }

    boolean disconnectInputDeviceInternal(BluetoothDevice device) {
        if(mService == null) {
            return false;
        }

        handleInputDeviceStateChange(device, BluetoothHogpDevice.STATE_DISCONNECTING);
        try {
            if (!mService.disconnect(device)) {
                handleInputDeviceStateChange(device, BluetoothHogpDevice.STATE_CONNECTED);
                return false;
            }
        }catch (RemoteException e) {
            handleInputDeviceStateChange(device, BluetoothHogpDevice.STATE_CONNECTED);
        return false;
    }

        /*if(DBG) Log.d(TAG, "trying to disconnect to HOGP device");
        Intent intent = new Intent(ACTION_HOGP_CONNECTION_REQUEST);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra("CONNECTED", false);
        mContext.sendBroadcast(intent, BluetoothService.BLUETOOTH_PERM);
        */
        return true;
    }

    int getInputDeviceConnectionState(BluetoothDevice device) {
        if (mInputDevices.get(device) == null) {
            return BluetoothHogpDevice.STATE_DISCONNECTED;
        }
        return mInputDevices.get(device);
    }

    List<BluetoothDevice> getConnectedInputDevices() {
        List<BluetoothDevice> devices = lookupInputDevicesMatchingStates(
            new int[] {BluetoothHogpDevice.STATE_CONNECTED});
        return devices;
    }

    List<BluetoothDevice> getInputDevicesMatchingConnectionStates(int[] states) {
        List<BluetoothDevice> devices = lookupInputDevicesMatchingStates(states);
        return devices;
    }
    //MR1 Change
    int getInputDevicePriority(BluetoothDevice device) {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.getBluetoothHogpDevicePriorityKey(device.getAddress()),
                BluetoothHogpDevice.PRIORITY_UNDEFINED);
    }
    //MR1 change
    boolean setInputDevicePriority(BluetoothDevice device, int priority) {
        if (!BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
            return false;
        }
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.getBluetoothHogpDevicePriorityKey(device.getAddress()),
                priority);
    }

    List<BluetoothDevice> lookupInputDevicesMatchingStates(int[] states) {
        List<BluetoothDevice> inputDevices = new ArrayList<BluetoothDevice>();

        for (BluetoothDevice device: mInputDevices.keySet()) {
            int inputDeviceState = getInputDeviceConnectionState(device);
            for (int state : states) {
                if (state == inputDeviceState) {
                    inputDevices.add(device);
                    break;
                }
            }
        }
        return inputDevices;
    }

    private void handleInputDeviceStateChange(BluetoothDevice device, int state) {
        int prevState;
        if (mInputDevices.get(device) == null) {
            prevState = BluetoothHogpDevice.STATE_DISCONNECTED;
        } else {
            prevState = mInputDevices.get(device);
        }
        if (prevState == state) return;

        mInputDevices.put(device, state);

        if (getInputDevicePriority(device) >
              BluetoothHogpDevice.PRIORITY_OFF &&
            state == BluetoothHogpDevice.STATE_CONNECTING ||
            state == BluetoothHogpDevice.STATE_CONNECTED) {
            // We have connected or attempting to connect.
            // Bump priority
            setInputDevicePriority(device, BluetoothHogpDevice.PRIORITY_AUTO_CONNECT);
        }

        Intent intent = new Intent(BluetoothHogpDevice.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothHogpDevice.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothHogpDevice.EXTRA_STATE, state);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcast(intent, BluetoothService.BLUETOOTH_PERM);

        debugLog("InputDevice state : device: " + device + " State:" + prevState + "->" + state);
    }

    // this should be triggered by some broadcast event receiver for HOGP connection state.
    // to do ...
    void handleInputDevicePropertyChange(String address, boolean connected) {
        int state = connected ? BluetoothHogpDevice.STATE_CONNECTED :
            BluetoothHogpDevice.STATE_DISCONNECTED;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = adapter.getRemoteDevice(address);
        handleInputDeviceStateChange(device, state);
    }

    void setInitialInputDevicePriority(BluetoothDevice device, int state) {
        switch (state) {
            case BluetoothDevice.BOND_BONDED:
                if (getInputDevicePriority(device) == BluetoothHogpDevice.PRIORITY_UNDEFINED) {
                    setInputDevicePriority(device, BluetoothHogpDevice.PRIORITY_ON);
                }
                break;
            case BluetoothDevice.BOND_NONE:
                setInputDevicePriority(device, BluetoothHogpDevice.PRIORITY_UNDEFINED);
                break;
        }
    }

    private static void debugLog(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private static void errorLog(String msg) {
        Log.e(TAG, msg);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) Log.d(TAG, "Hogp Service Proxy object connected");
            mService = IHogpDevice.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            if (DBG) Log.d(TAG, "Hogp Service Proxy object disconnected");
            mService = null;
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String action = intent.getAction();
            if (action.equals(ACTION_HOGP_CONNECTION_STATE_CHANGED)) {
                boolean connected = intent.getBooleanExtra(EXTRA_CONNECTED, false);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(DBG) Log.d(TAG, "address: " + device.getAddress() +", connected: " + connected);
                handleInputDevicePropertyChange(device.getAddress(), connected);
            }
        }
    };
}
