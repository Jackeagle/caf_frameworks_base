/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.os.RemoteException;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

/**
 *
 * Public API for controlling the Bluetooth Sap Service.
 *
 * @hide
 */
public class BluetoothSap {

    private static final String TAG = "BluetoothSap";
    private IBluetooth mService;

    public BluetoothSap() {
        IBinder b = ServiceManager.getService(BluetoothAdapter.BLUETOOTH_SERVICE);
        if (b != null) {
            mService = IBluetooth.Stub.asInterface(b);
        } else {
            Log.i(TAG, "Failed to get the Bluetooth Interface");
        }
    }
     /**
     * Initiate the disconnection from SAP server.
     * Status of the SAP server can be determined by the signal emitted
     * from org.qcom.sap
     */
    public boolean disconnect() {
        Log.i(TAG, "->disconnect");
        try {
            mService.disconnectSap();
            return true;
        } catch(RemoteException e) {
            Log.e(TAG, "", e);
        }

        return false;
    }
}
