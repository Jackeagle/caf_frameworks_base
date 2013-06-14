/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (c) 2013 The Linux Foundation. All rights reserved
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
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

package com.android.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.server.BluetoothA2dpService;
import android.server.BluetoothService;
import android.util.Log;
import android.util.Slog;

class BluetoothManagerService extends IBluetoothManager.Stub {
    private static final String TAG = "BluetoothManagerService";
    private static final boolean DBG = true;
    BluetoothService bluetooth = null;
    BluetoothA2dpService bluetoothA2dp = null;
    private static final int MESSAGE_USER_SWITCHED = 300;
    private HandlerThread mThread;
    private final BluetoothHandler mHandler;

    //private final Context mContext;
    private final ContentResolver mContentResolver;
    private static boolean mBluetoothState;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) Log.d(TAG, "received Intent " + action);
            if (Intent.ACTION_USER_SWITCHED.equals(action)) {
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_USER_SWITCHED,
                       intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 0), 0));
            }
        }
    };

   /* private final IBluetoothCallback mBluetoothCallback =  new IBluetoothCallback.Stub() {
        @Override
        public void onBluetoothStateChange(int prevState, int newState) throws RemoteException  {
            Message msg = mHandler.obtainMessage(MESSAGE_BLUETOOTH_STATE_CHANGE,prevState,newState);
            mHandler.sendMessage(msg);
        }
    };*/

    BluetoothManagerService(Context context) {
        mThread = new HandlerThread("BluetoothManager");
        mThread.start();
        mHandler = new BluetoothHandler(mThread.getLooper());
        Slog.i(TAG, "BluetoothManagerService started.Start Bluetooth Service");
        mContentResolver = context.getContentResolver();
        bluetooth = new BluetoothService(context);
        ServiceManager.addService(BluetoothAdapter.BLUETOOTH_SERVICE, bluetooth);
        bluetooth.initAfterRegistration();
        bluetoothA2dp = new BluetoothA2dpService(context, bluetooth);
        ServiceManager.addService(BluetoothA2dpService.BLUETOOTH_A2DP_SERVICE, bluetoothA2dp);
        bluetooth.initAfterA2dpRegistration();
        int airplaneModeOn = Settings.System.getInt(mContentResolver,
               Settings.System.AIRPLANE_MODE_ON, 0);
        int bluetoothOn = Settings.Secure.getInt(mContentResolver,
            Settings.Secure.BLUETOOTH_ON, 0);
        if (airplaneModeOn == 0 && bluetoothOn != 0) {
            bluetooth.enable();
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
        context.registerReceiver(mReceiver, filter);
    }

    public boolean isEnabled() {
        return bluetooth.isEnabled();
    }

    public boolean disable(boolean saveSetting) {
        return bluetooth.disable(saveSetting);
    }

    public boolean enableNoAutoConnect() {
        return false;
    }

    public String getAddress() {
        return null;
    }

    public String getName() {
        return null;
    }

    private class BluetoothHandler extends Handler {
        public BluetoothHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DBG) Log.d (TAG, "Message: " + msg.what);
            switch (msg.what) {
            case MESSAGE_USER_SWITCHED:
                if (isEnabled()) {
                    // disable
                    bluetooth.disable();
                    waitForOnOff(false, true);
                    // wait for some time before re-enabling
                    SystemClock.sleep(500);
                    // enable
                    bluetooth.enable();
                }
                break;
            }
        }
    }

    /**
    *  if on is true, wait for state become ON
    *  if off is true, wait for state become OFF
    *  if both on and off are false, wait for state not ON
    */
     private boolean waitForOnOff(boolean on, boolean off) {
         int i = 0;
         while (i < 50) {
             if (on) {
                 if (bluetooth.getBluetoothState() == BluetoothAdapter.STATE_ON) return true;
             } else if (off) {
                 if (bluetooth.getBluetoothState() == BluetoothAdapter.STATE_OFF) return true;
             } else {
                 if (bluetooth.getBluetoothState() != BluetoothAdapter.STATE_ON) return true;
             }
             if (on || off) {
                 SystemClock.sleep(300);
             } else {
                 SystemClock.sleep(50);
             }
             i++;
         }
         Log.w(TAG,"waitForOnOff time out");
         return false;
    }
}
