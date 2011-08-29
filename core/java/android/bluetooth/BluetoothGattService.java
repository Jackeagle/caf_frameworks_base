/*
 * Copyright (c) 2011, Code Aurora Forum. All rights reserved.
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

import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Public API for controlling the Bluetooth GATT based services.
 *
 * @hide
 */

public class BluetoothGattService {
    private static final String TAG = "BluetoothGattService";
    private ParcelUuid mUuid;
    private String mObjPath;
    private BluetoothDevice mDevice;
    private String mName = null;
    private boolean watcherRegistered = false;
    private IBluetoothGattProfile profileCallback = null;

    private final HashMap<String, Map<String, String>> mCharacteristicProperties;
    private String[] characteristicPaths = null;
    private boolean discoveryDone = false;

    private boolean mClosed;
    private final ReentrantReadWriteLock mLock;

    private final IBluetooth mService;

    private final ServiceHelper mHelper;

    public BluetoothGattService(BluetoothDevice device, ParcelUuid uuid, String path,
                                IBluetoothGattProfile callback) {
        mDevice = device;
        mUuid = uuid;
        mObjPath = path;
        profileCallback = callback;
        mClosed = false;
        mLock = new ReentrantReadWriteLock();

        mCharacteristicProperties = new HashMap<String, Map<String, String>>();

        mHelper = new ServiceHelper();
        mService = BluetoothDevice.getService();
        mHelper.startRemoteGattService();
    }

    public ParcelUuid getServiceUuid() {
        return mUuid;
    }

    public String getServiceName() throws Exception {

        if (mName != null)
            return  mName;

        mLock.readLock().lock();
        try {
            if (mClosed) throw new IOException("GATT service closed");
            return mService.getGattServiceName(mObjPath);
        } finally {
            mLock.readLock().unlock();
        }
    }

    public String[] getCharacteristics() {
        if (!discoveryDone)
            mHelper.waitDiscoveryDone();
        return characteristicPaths;
    }

    public boolean isCharacteristicDiscoveryDone() {
        return discoveryDone;
    }

    public ParcelUuid[] getCharacteristicUuids() {

        ArrayList<ParcelUuid>  uuidList = new ArrayList<ParcelUuid>();

        if (!discoveryDone)
            mHelper.waitDiscoveryDone();

        if (characteristicPaths == null)
            return null;

        int count  = characteristicPaths.length;

        for(int i = 0; i< count; i++) {

            String value = getCharacteristicProperty(characteristicPaths[i], "UUID");

            if (value != null)
                uuidList.add(ParcelUuid.fromString(value));

            Log.d (TAG, "Characteristic UUID: " + value);

        }

        ParcelUuid[] uuids = new ParcelUuid[count];

        uuidList.toArray(uuids);

        return uuids;

    }

    public ParcelUuid getCharacteristicUuid(String path) {

        ParcelUuid uuid = null;

        if (!discoveryDone)
            mHelper.waitDiscoveryDone();

        String value = getCharacteristicProperty(path, "UUID");

        if (value != null) {
                uuid = ParcelUuid.fromString(value);

                Log.d (TAG, "Characteristic UUID: " + value);
        }
        return uuid;
    }

    public String getCharacteristicDescription(String path) {
        if (!discoveryDone)
            mHelper.waitDiscoveryDone();
        return getCharacteristicProperty(path, "Description");

    }

    public byte[] readCharacteristicRaw(String path)
    {
        Log.d (TAG, "readCharacteristicValue for " + path);

        if (!discoveryDone)
            mHelper.waitDiscoveryDone();

        if (characteristicPaths == null)
            return null;

        String value = getCharacteristicProperty(path, "Value");

        if (value == null) {
            return null;
        }
        byte[] ret = value.getBytes();
        return ret;
    }

    public boolean updateCharacteristicValue(String path) throws Exception {
        Log.d (TAG, "updateCharacteristicValue for " + path);

        if (!discoveryDone)
            mHelper.waitDiscoveryDone();

        if (characteristicPaths == null)
            return false;

        mLock.readLock().lock();
        try {
            if (mClosed) throw new Exception ("GATT service closed");
            return mHelper.fetchCharValue(path);
        } finally {
            mLock.readLock().unlock();
        }
    }

    public String getCharacteristicClientConf(String path)
    {
        if (!discoveryDone)
            mHelper.waitDiscoveryDone();

        if (characteristicPaths == null)
            return null;

        String value = (String) getCharacteristicProperty(path, "ClientConfiguration");

        if (value == null) {
            return null;
        }

        return value;
    }

    public boolean writeCharacteristicRaw(String path, byte[] value) throws Exception {

        if (!discoveryDone)
            mHelper.waitDiscoveryDone();

        if (characteristicPaths == null)
            return false;

        mLock.readLock().lock();
        try {
            if (mClosed) throw new Exception ("GATT service closed");
            return mHelper.setCharacteristicProperty(path, "Value", value);
        }  finally {
            mLock.readLock().unlock();
        }
    }

    public boolean setCharacteristicClientConf(String path, int config) throws Exception {

        if (!discoveryDone)
            mHelper.waitDiscoveryDone();

        if (characteristicPaths == null)
            return false;

        // Client Conf is 2 bytes
        byte[] value = new byte[2];
        value[1] = (byte)(config & 0xFF);
        value[0] = (byte)((config >> 8) & 0xFF);

        mLock.readLock().lock();
        try {
            if (mClosed) throw new Exception ("GATT service closed");
            return mHelper.setCharacteristicProperty(path, "ClientConfiguration", value);
        }  finally {
            mLock.readLock().unlock();
        }
    }

    public boolean registerWatcher() throws Exception {
        if (watcherRegistered == false) {
            mLock.readLock().lock();
            try {
                if (mClosed) throw new Exception ("GATT service closed");

                watcherRegistered = mHelper.registerCharacteristicsWatcher();
                return watcherRegistered;
            }  finally {
                mLock.readLock().unlock();
            }
       } else {
            return true;
        }
    }

    public boolean deregisterWatcher()  throws Exception {
        if (watcherRegistered == true) {
            watcherRegistered = false;

            mLock.readLock().lock();
            try {
                if (mClosed) throw new Exception ("GATT service closed");
                return mHelper.deregisterCharacteristicsWatcher();
            }  finally {
                mLock.readLock().unlock();
            }
        }
        return true;
    }

    public void close() throws Exception{

        mLock.writeLock().lock();
        if (mClosed) {
            mLock.writeLock().unlock();
            return;
        }

        deregisterWatcher();

        try {
            mClosed = true;
            mService.closeRemoteGattService(mObjPath);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    /** @hide */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private String getCharacteristicProperty(String path, String property) {

        Map<String, String> properties = mCharacteristicProperties.get(path);

        if (properties != null)
            return properties.get(property);

        return null;
    }

    private void addCharacteristicProperties(String path, String[] properties) {
        Map<String, String> propertyValues = mCharacteristicProperties.get(path);
        if (propertyValues == null) {
            propertyValues = new HashMap<String, String>();
        }

        for (int i = 0; i < properties.length; i++) {
            String name = properties[i];
            String newValue = null;

            if (name == null) {
                Log.e(TAG, "Error: Gatt Characterisitc Property at index" + i + "is null");
                continue;
            }

            newValue = properties[++i];

            propertyValues.put(name, newValue);
        }

        mCharacteristicProperties.put(path, propertyValues);
    }

    private void updateCharacteristicPropertyCache(String path) {
        String[] properties = null;

        try {
            properties = mService.getCharacteristicProperties(path);
        } catch (Exception e) {Log.e(TAG, "", e);}

        if (properties != null) {
            addCharacteristicProperties(path, properties);
        }
    }

    /**
     * Helper to perform Service Characteristic discovery
     */
    private class ServiceHelper extends IBluetoothGattService.Stub {

        /**
         * Throws IOException on failure.
         */
        public boolean doDiscovery() {

            Log.d(TAG, "doDiscovery " + mObjPath);

            try {
                return mService.discoverCharacteristics(mObjPath);
            } catch (RemoteException e) {Log.e(TAG, "", e);}

            return false;

        }

        public void startRemoteGattService() {
            try {
                mService.startRemoteGattService(mObjPath, this);
            } catch (RemoteException e) {Log.e(TAG, "", e);}

            doDiscovery();
        }

        public synchronized void onCharacteristicsDiscovered(String[] paths)
        {
            Log.d(TAG, "onCharacteristicsDiscovered: " + paths);
            boolean result = false;

            if (paths !=null) {

                int count = paths.length;

                Log.d(TAG, "Discovered  " + count + " characteristics for service " + mObjPath + " ( " + mName + " )");

                characteristicPaths = paths;

                for (int i = 0; i < count; i++) {

                    String[] properties = null;

                    try {
                        properties = mService.getCharacteristicProperties(paths[i]);
                    } catch (RemoteException e) {Log.e(TAG, "", e);}

                    if (properties != null) {
                        addCharacteristicProperties(paths[i], properties);
                    }
                }
                result = true;

            }

            discoveryDone = true;

            if (profileCallback != null) {
                try {
                    profileCallback.onDiscoverCharacteristicsResult(mObjPath, result);
                } catch (Exception e) {Log.e(TAG, "", e);}
            }

            this.notify();
        }

        public synchronized void onSetCharacteristicProperty(String path, String property, boolean result)
        {
            Log.d(TAG, "onSetCharacteristicProperty: " + path + " property " + property + " result " + result);
            if ((path == null) || (property == null)) {
                return;
            }
            if (property.equals("Value")) {
                try {
                    if(result) {
                        updateCharacteristicPropertyCache(path);
                    }
                    if (profileCallback != null)
                        profileCallback.onSetCharacteristicValueResult(path, result);
                } catch (RemoteException e) {Log.e(TAG, "", e);}
            }
            if (property.equals("ClientConfiguration")) {
                try {
                    if(result) {
                        updateCharacteristicPropertyCache(path);
                    }
                    if (profileCallback != null)
                        profileCallback.onSetCharacteristicCliConfResult(path, result);
                } catch (RemoteException e) {Log.e(TAG, "", e);}

            }
        }

        public synchronized void onValueChanged(String path, String value)
        {
            if (path == null) {
                return;
            }
            Log.d(TAG, "WatcherValueChanged = " + path + value);

            if (profileCallback == null) {
                deregisterCharacteristicsWatcher();
                return;
            }
            try {
                profileCallback.onValueChanged(path, value);
            } catch (RemoteException e) {Log.e(TAG, "", e);}
        }

        public synchronized boolean setCharacteristicProperty(String path, String key, byte[] value) {
            Log.d(TAG, "setCharacteristicProperty");
            try {
                return mService.setCharacteristicProperty(path, key, value);
            } catch (RemoteException e) {Log.e(TAG, "", e);}

            return false;
        }

        public synchronized void onCharacteristicValueUpdated(String path, boolean result)
        {

            if (result) {
                updateCharacteristicPropertyCache(path);
            }

            if (profileCallback != null) {
                try {
                    profileCallback.onUpdateCharacteristicValueResult(path, result);
                } catch (RemoteException e) {Log.e(TAG, "", e);}
            }
        }

        public synchronized boolean fetchCharValue(String path) {
            try {
                return mService.updateCharacteristicValue(path);
            } catch (RemoteException e) {Log.e(TAG, "", e);}

            return false;
        }

        public synchronized boolean registerCharacteristicsWatcher() {
            Log.d(TAG, "registerCharacteristicsWatcher: ");

            try {
                if (mService.registerCharacteristicsWatcher(mObjPath, this) == true) {
                    return true;
                }
            } catch (RemoteException e) {Log.e(TAG, "", e);}

            return false;
        }

        public synchronized boolean deregisterCharacteristicsWatcher() {
            Log.d(TAG, "deregisterCharacteristicsWatcher: ");
            try {
               return mService.deregisterCharacteristicsWatcher(mObjPath);
             } catch (RemoteException e) {Log.e(TAG, "", e);}
            return false;
        }

        public synchronized void waitDiscoveryDone()
        {
            try {
                this.wait(60000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Characteristics discovery takes too long");
            }
        }
    }
}