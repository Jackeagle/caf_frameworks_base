/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the APIs to control the Bluetooth MAP MCE Profile.
 *
 * @hide
 */
public final class BluetoothMapClient implements BluetoothProfile {

    private static final String TAG = "BluetoothMapClient";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);
    private static final boolean VDBG = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_MESSAGE_RECEIVED =
            "android.bluetooth.mapmce.profile.action.MESSAGE_RECEIVED";
    /* Actions to be used for pending intents */
    public static final String ACTION_MESSAGE_SENT_SUCCESSFULLY =
            "android.bluetooth.mapmce.profile.action.MESSAGE_SENT_SUCCESSFULLY";
    public static final String ACTION_MESSAGE_DELIVERED_SUCCESSFULLY =
            "android.bluetooth.mapmce.profile.action.MESSAGE_DELIVERED_SUCCESSFULLY";

    /**
     * Action to be used for notifying read status changed
     * Always contains the extra fields EXTRA_MESSAGE_HANDLE
     */
    public static final String ACTION_EXT_MESSAGE_READ_STATUS_CHANGED =
            "android.bluetooth.mapmce.profile.action.ext.MESSAGE_READ_STATUS_CHANGED";

    /**
     * Extended action to notify the message has been deleted
     * Always contains the extra fields EXTRA_MESSAGE_HANDLE
     */
    public static final String ACTION_EXT_MESSAGE_DELETED_STATUS_CHANGED =
            "android.bluetooth.mapmce.profile.action.ext.MESSAGE_DELETED_STATUS_CHANGED";

    /**
     * Extended action to retrieve MAS instance information
     */
    public static final String ACTION_EXT_INSTANCE_INFORMATION =
            "android.bluetooth.mapmce.profile.action.ext.INSTANCE_INFORMATION";

    /* Extras used in ACTION_MESSAGE_RECEIVED intent.
     * NOTE: HANDLE is only valid for a single session with the device. */
    public static final String EXTRA_MESSAGE_HANDLE =
            "android.bluetooth.mapmce.profile.extra.MESSAGE_HANDLE";
    /* If EXTRA_TYPE is SMS_GSM or SMS_CDMA, EXTRA_SENDER_CONTACT_URI is the phone number
     * If EXTRA_TYPE is EMAIL, EXTRA_SENDER_CONTACT_URI is the email address */
    public static final String EXTRA_SENDER_CONTACT_URI =
            "android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_URI";
    public static final String EXTRA_SENDER_CONTACT_NAME =
            "android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_NAME";
    public static final String EXTRA_RECIPIENT_CONTACT_URI =
            "android.bluetooth.mapmce.profile.extra.RECIPIENT_CONTACT_URI";
    public static final String EXTRA_RECIPIENT_CONTACT_NAME =
            "android.bluetooth.mapmce.profile.extra.RECIPIENT_CONTACT_NAME";

    /**
     * Used as a String extra field in ACTION_MESSAGE_RECEIVED
     * It contains MAP message type
     * Possible values are:
     * "EMAIL"
     * "SMS_GSM"
     * "SMS_CDMA"
     * "MMS"
     */
    public static final String EXTRA_TYPE =
            "android.bluetooth.mapmce.profile.extra.TYPE";

    /**
     * Used as a String extra field in
     * ACTION_MESSAGE_RECEIVED
     * ACTION_EXT_MESSAGE_READ_STATUS_CHANGED
     * It contains MAP message read status
     * Possible values are:
     * "READ"
     * "UNREAD"
     */
    public static final String EXTRA_READ_STATUS =
            "android.bluetooth.mapmce.profile.extra.READ_STATUS";

    /**
     * Used as a String extra field in
     * ACTION_MESSAGE_RECEIVED
     * ACTION_EXT_MESSAGE_READ_STATUS_CHANGED
     * ACTION_EXT_MESSAGE_DELETED_STATUS_CHANGED
     * It contains the folder where the message status changed
     */
    public static final String EXTRA_FOLDER =
            "android.bluetooth.mapmce.profile.extra.FOLDER";

    /**
     * Used as a int extra field in
     * ACTION_MESSAGE_RECEIVED
     * ACTION_EXT_MESSAGE_READ_STATUS_CHANGED
     * ACTION_EXT_MESSAGE_DELETED_STATUS_CHANGED
     * ACTION_EXT_INSTANCE_INFORMATION
     * It contains MAP message instance id
     * Possible value is from 0 to 255
     */
    public static final String EXTRA_INSTANCE_ID =
            "android.bluetooth.mapmce.profile.extra.INSTANCE_ID";

    /**
     * Used as a String extra field in ACTION_EXT_INSTANCE_INFORMATION
     * It contains the owner UCI(Unique Client/Caller Identifier) of the MAS-instance
     * for which the information is requested
     */
    public static final String EXTRA_OWNER_UCI =
            "android.bluetooth.mapmce.profile.extra.OWNER_UCI";

    /**
     * Used as a String extra field in ACTION_EXT_INSTANCE_INFORMATION
     * It contains MAP message instance name
     */
    public static final String EXTRA_INSTANCE_NAME =
            "android.bluetooth.mapmce.profile.extra.INSTANCE_NAME";

    /**
     * Used as a int extra field in ACTION_EXT_INSTANCE_INFORMATION
     * It contains supported MAP message type mask
     * Possible values are defined in SdpMasRecord.MessageType
     */
    public static final String EXTRA_SUPPORTED_TYPE =
            "android.bluetooth.mapmce.profile.extra.SUPPORTED_TYPE";

    /** Set message status to read */
    public static final int READ = 0;
    /** Set message status to deleted */
    public static final int DELETED = 1;

    private volatile IBluetoothMapClient mService;
    private final Context mContext;
    private ServiceListener mServiceListener;
    private BluetoothAdapter mAdapter;

    /** There was an error trying to obtain the state */
    public static final int STATE_ERROR = -1;

    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    /** Connection canceled before completion. */
    public static final int RESULT_CANCELED = 2;

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
                public void onBluetoothStateChange(boolean up) {
                    if (DBG) Log.d(TAG, "onBluetoothStateChange: up=" + up);
                    if (!up) {
                        if (VDBG) Log.d(TAG, "Unbinding service...");
                        synchronized (mConnection) {
                            try {
                                mService = null;
                                mContext.unbindService(mConnection);
                            } catch (Exception re) {
                                Log.e(TAG, "", re);
                            }
                        }
                    } else {
                        synchronized (mConnection) {
                            try {
                                if (mService == null) {
                                    if (VDBG) Log.d(TAG, "Binding service...");
                                    doBind();
                                }
                            } catch (Exception re) {
                                Log.e(TAG, "", re);
                            }
                        }
                    }
                }
            };

    /**
     * Create a BluetoothMapClient proxy object.
     */
    /*package*/ BluetoothMapClient(Context context, ServiceListener l) {
        if (DBG) Log.d(TAG, "Create BluetoothMapClient proxy object");
        mContext = context;
        mServiceListener = l;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothMapClient.class.getName());
        ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !mContext.bindServiceAsUser(intent, mConnection, 0,
                mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth MAP MCE Service with " + intent);
            return false;
        }
        return true;
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Close the connection to the backing service.
     * Other public functions of BluetoothMap will return default error
     * results once close() has been called. Multiple invocations of close()
     * are ok.
     */
    public void close() {
        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }

        synchronized (mConnection) {
            if (mService != null) {
                try {
                    mService = null;
                    mContext.unbindService(mConnection);
                } catch (Exception re) {
                    Log.e(TAG, "", re);
                }
            }
        }
        mServiceListener = null;
    }

    /**
     * Returns true if the specified Bluetooth device is connected.
     * Returns false if not connected, or if this proxy object is not
     * currently connected to the Map service.
     */
    public boolean isConnected(BluetoothDevice device) {
        if (VDBG) Log.d(TAG, "isConnected(" + device + ")");
        final IBluetoothMapClient service = mService;
        if (service != null) {
            try {
                return service.isConnected(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Initiate connection. Initiation of outgoing connections is not
     * supported for MAP server.
     */
    public boolean connect(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "connect(" + device + ")" + "for MAPS MCE");
        final IBluetoothMapClient service = mService;
        if (service != null) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Initiate disconnect.
     *
     * @param device Remote Bluetooth Device
     * @return false on error, true otherwise
     */
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "disconnect(" + device + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Get the list of connected devices. Currently at most one.
     *
     * @return list of connected devices
     */
    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) Log.d(TAG, "getConnectedDevices()");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled()) {
            try {
                return service.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList<>();
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<>();
    }

    /**
     * Get the list of devices matching specified states. Currently at most one.
     *
     * @return list of matching devices
     */
    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (DBG) Log.d(TAG, "getDevicesMatchingStates()");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled()) {
            try {
                return service.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList<>();
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<>();
    }

    /**
     * Get connection state of device
     *
     * @return device connection state
     */
    @Override
    public int getConnectionState(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getConnectionState(" + device + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Set priority of the profile
     *
     * <p> The device should already be paired.  Priority can be one of {@link #PRIORITY_ON} or
     * {@link #PRIORITY_OFF},
     *
     * @param device Paired bluetooth device
     * @return true if priority is set, false on error
     */
    public boolean setPriority(BluetoothDevice device, int priority) {
        if (DBG) Log.d(TAG, "setPriority(" + device + ", " + priority + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            if (priority != BluetoothProfile.PRIORITY_OFF
                    && priority != BluetoothProfile.PRIORITY_ON) {
                return false;
            }
            try {
                return service.setPriority(device, priority);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Get the priority of the profile.
     *
     * <p> The priority can be any of:
     * {@link #PRIORITY_AUTO_CONNECT}, {@link #PRIORITY_OFF},
     * {@link #PRIORITY_ON}, {@link #PRIORITY_UNDEFINED}
     *
     * @param device Bluetooth device
     * @return priority of the device
     */
    public int getPriority(BluetoothDevice device) {
        if (VDBG) Log.d(TAG, "getPriority(" + device + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getPriority(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return PRIORITY_OFF;
            }
        }
        if (service == null) Log.w(TAG, "Proxy not attached to service");
        return PRIORITY_OFF;
    }

    /**
     * Send a message.
     *
     * Send an SMS message to either the contacts primary number or the telephone number specified.
     *
     * @param device Bluetooth device
     * @param contacts Uri[] of the contacts. Uri scheme "tel" is for SMS message. Uri scheme "email" is for email
     * @param message Message to be sent
     * @param sentIntent intent issued when message is sent
     * @param deliveredIntent intent issued when message is delivered
     * @return true if the message is enqueued, false on error
     */
    public boolean sendMessage(BluetoothDevice device, Uri[] contacts, String message,
            PendingIntent sentIntent, PendingIntent deliveredIntent) {
        if (DBG) Log.d(TAG, "sendMessage(" + device + ", " + contacts + ", " + message);
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.sendMessage(device, contacts, message, sentIntent, deliveredIntent);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        return false;
    }

    /**
     * Get unread messages.  Unread messages will be published via {@link #ACTION_MESSAGE_RECEIVED}.
     *
     * @param device Bluetooth device
     * @return true if the message is enqueued, false on error
     */
    public boolean getUnreadMessages(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "getUnreadMessages(" + device + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getUnreadMessages(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        return false;
    }

    /**
     * Set message status of message on MSE
     * <p>
     * When read status changed, the result will be published via
     * {@link #ACTION_EXT_MESSAGE_READ_STATUS_CHANGED}
     * When deleted status changed, the result will be published via
     * {@link #ACTION_MESSAGE_DELETED_STATUS_CHANGED}
     *
     * @param device Bluetooth device
     * @param handle handle of message
     * @param read <code>READ</code> for "read", <code>DELETED</code> for
     *            "deleted", otherwise the status will not be set
     *
     * @return <code>true</code> if request has been sent, <code>false</code> on error
     *
     */
    public boolean setMessageStatus(BluetoothDevice device, String handle, int status) {
        if (DBG) Log.d(TAG, "setMessageStatus(" + device + ", " + handle + ", " + status + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device) && handle != null &&
            (status == READ || status == DELETED)) {
            try {
                return service.setMessageStatus(device, handle, status);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        return false;
    }

    /**
     * Abort current obex operation
     *
     * @param device Bluetooth device
     * @return <code>true</code> if request has been sent, <code>false</code> on error
     */
    public boolean abort(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "abort(" + device + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.abort(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        return false;
    }

    /**
     * When connected with more than 1 instance, before sendMessage, getUnreadMessages, setMessageStatus,
     * set the active instance id first for which instance will be worked on.
     *
     * @param device Bluetooth device
     * @param instance instance id. Invalid value is from 0 to 255 for one instance,
     * @return <code>true</code> if request has been sent, <code>false</code> on error
     */
    public boolean setActiveInstance(BluetoothDevice device, byte instance) {
        if (DBG) Log.d(TAG, "setActiveInstance(" + device + ", " + instance + ")");
        final IBluetoothMapClient service = mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.setActiveInstance(device, instance);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        return false;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) Log.d(TAG, "Proxy object connected");
            mService = IBluetoothMapClient.Stub.asInterface(service);
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.MAP_CLIENT,
                        BluetoothMapClient.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (DBG) Log.d(TAG, "Proxy object disconnected");
            mService = null;
            if (mServiceListener != null) {
                mServiceListener.onServiceDisconnected(BluetoothProfile.MAP_CLIENT);
            }
        }
    };

    private boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.getState() == BluetoothAdapter.STATE_ON) return true;
        if (DBG) Log.d(TAG, "Bluetooth is Not enabled");
        return false;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

}
