/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import java.util.ArrayList;
import java.io.FileDescriptor;
import java.io.PrintWriter;

import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IPhoneStateListener;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.DataPhone.IPVersion;
import com.android.server.am.BatteryStatsService;

/**
 * Since phone process can be restarted, this class provides a centralized place
 * that applications can register and be called back from.
 */
class TelephonyRegistry extends ITelephonyRegistry.Stub {
    private static final String TAG = "TelephonyRegistry";

    private static class Record {
        String pkgForDebug;

        IBinder binder;

        IPhoneStateListener callback;

        int events;

        int subscription;
    }

    private final String SUBSCRIPTION = "SUBSCRIPTION";

    private final Context mContext;

    private final ArrayList<Record> mRecords = new ArrayList();

    private final IBatteryStats mBatteryStats;

    private int[] mCallState;

    private String[] mCallIncomingNumber;

    private ServiceState[] mServiceState;

    private SignalStrength[] mSignalStrength;

    private boolean[] mMessageWaiting;

    private boolean[] mCallForwarding;

    private int[] mDataActivity;

    private int[] mDataConnectionState;

    private boolean[] mDataConnectionPossible;

    private String[] mDataConnectionReason;

    private Bundle[] mCellLocation;

    private int[] mDataConnectionNetworkType;

    private int mDefaultSubscription;

    static final int PHONE_STATE_PERMISSION_MASK =
                PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR |
                PhoneStateListener.LISTEN_CALL_STATE |
                PhoneStateListener.LISTEN_DATA_ACTIVITY |
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR;

    // we keep a copy of all of the state so we can send it out when folks
    // register for it
    //
    // In these calls we call with the lock held. This is safe becasuse remote
    // calls go through a oneway interface and local calls going through a
    // handler before they get to app code.

    TelephonyRegistry(Context context) {
        CellLocation  location = CellLocation.getEmpty();

        mContext = context;
        mBatteryStats = BatteryStatsService.getService();
        // Initialize default subscription to be used for single standby.
        mDefaultSubscription = TelephonyManager.getDefaultSubscription();

        int numPhones = TelephonyManager.getPhoneCount();
        mCallState = new int[numPhones];
        mCallIncomingNumber = new String[numPhones];
        mServiceState = new ServiceState[numPhones];
        mSignalStrength = new SignalStrength[numPhones];
        mMessageWaiting = new boolean[numPhones];
        mCallForwarding = new boolean[numPhones];
        mDataActivity = new int[numPhones];
        mDataConnectionState = new int[numPhones];
        mDataConnectionPossible = new boolean[numPhones];
        mDataConnectionReason = new String[numPhones];
        mCellLocation = new Bundle[numPhones];
        mDataConnectionNetworkType =  new int[numPhones];
        for (int i = 0; i < TelephonyManager.getPhoneCount(); i++) {
            mCallState[i] =  TelephonyManager.CALL_STATE_IDLE;
            mCallIncomingNumber[i] =  "";
            mServiceState[i] =  new ServiceState();
            mSignalStrength[i] =  new SignalStrength();
            mMessageWaiting[i] =  false;
            mCallForwarding[i] =  false;
            mDataActivity[i] = TelephonyManager.DATA_ACTIVITY_NONE;
            mDataConnectionState[i] = TelephonyManager.DATA_DISCONNECTED;
            mDataConnectionPossible[i] = false;
            mDataConnectionReason[i] = "";
            mCellLocation[i] = new Bundle();
            mDataConnectionNetworkType[i] = 0;
        }

        // Note that location can be null for non-phone builds like
        // like the generic one.
        if (location != null) {
            location.fillInNotifierBundle(mCellLocation[0]);
            if (TelephonyManager.isDsdsEnabled()) {
                location.fillInNotifierBundle(mCellLocation[1]);
            }
        }
    }

    public void listen(String pkgForDebug, IPhoneStateListener callback, int events,
            boolean notifyNow) {
        listenOnSubscription(pkgForDebug, callback, events, notifyNow, mDefaultSubscription);
    }

    public void listenOnSubscription(String pkgForDebug, IPhoneStateListener callback, int events,
            boolean notifyNow, int subscription) {
        // Slog.d(TAG, "listen pkg=" + pkgForDebug + " events=0x" +
        // Integer.toHexString(events));
        if (events != 0) {
            /* Checks permission and throws Security exception */
            checkListenerPermission(events);

            synchronized (mRecords) {
                // register
                Record r = null;
                find_and_add: {
                    IBinder b = callback.asBinder();
                    final int N = mRecords.size();
                    for (int i = 0; i < N; i++) {
                        r = mRecords.get(i);
                        if (b == r.binder) {
                            break find_and_add;
                        }
                    }
                    r = new Record();
                    r.binder = b;
                    r.callback = callback;
                    r.pkgForDebug = pkgForDebug;
                    r.subscription = subscription;
                    mRecords.add(r);
                }
                int send = events & (events ^ r.events);
                r.events = events;
                if (notifyNow) {
                    if ((events & PhoneStateListener.LISTEN_SERVICE_STATE) != 0) {
                        sendServiceState(r, mServiceState[subscription]);
                    }
                    if ((events & PhoneStateListener.LISTEN_SIGNAL_STRENGTH) != 0) {
                        try {
                            int gsmSignalStrength;
                            // Call the overloaded callback function in case of valid subscription, else
                            // call the default function.
                            gsmSignalStrength = mSignalStrength[subscription].getGsmSignalStrength();
                            r.callback.onSignalStrengthChanged((gsmSignalStrength == 99 ? -1 : gsmSignalStrength));
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                    if ((events & PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR) != 0) {
                        try {
                            r.callback.onMessageWaitingIndicatorChanged(mMessageWaiting[subscription]);
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                    if ((events & PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR) != 0) {
                        try {
                            r.callback.onCallForwardingIndicatorChanged(mCallForwarding[subscription]);
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                    if ((events & PhoneStateListener.LISTEN_CELL_LOCATION) != 0) {
                        sendCellLocation(r, mCellLocation[subscription]);
                    }
                    if ((events & PhoneStateListener.LISTEN_CALL_STATE) != 0) {
                        try {
                            r.callback.onCallStateChanged(mCallState[subscription], mCallIncomingNumber[subscription]);
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                    if ((events & PhoneStateListener.LISTEN_DATA_CONNECTION_STATE) != 0) {
                        try {
                            r.callback.onDataConnectionStateChanged(mDataConnectionState[subscription],
                                mDataConnectionNetworkType[subscription]);
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                    if ((events & PhoneStateListener.LISTEN_DATA_ACTIVITY) != 0) {
                        try {
                            r.callback.onDataActivity(mDataActivity[subscription]);
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                    if ((events & PhoneStateListener.LISTEN_SIGNAL_STRENGTHS) != 0) {
                        try {
                            r.callback.onSignalStrengthsChanged(mSignalStrength[subscription]);
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                }
            }
        } else {
            remove(callback.asBinder());
        }
    }

    private void remove(IBinder binder) {
        synchronized (mRecords) {
            final int recordCount = mRecords.size();
            for (int i = 0; i < recordCount; i++) {
                if (mRecords.get(i).binder == binder) {
                    mRecords.remove(i);
                    return;
                }
            }
        }
    }

    public void notifyCallState(int state, String incomingNumber) {
        // Call the overloaded function with -1 as subscription value.
        notifyCallStateOnSubscription(state, incomingNumber, mDefaultSubscription);
    }

    public void notifyCallStateOnSubscription(int state, String incomingNumber, int subscription) {
        if (!checkNotifyPermission("notifyCallState()")) {
            return;
        }
        synchronized (mRecords) {
            if (subscription >= 0) {
                // Initialize the members only if "subscription" is valid.
                mCallState[subscription] = state;
                mCallIncomingNumber[subscription] = incomingNumber;
            }
            for (int i = mRecords.size() - 1; i >= 0; i--) {
                Record r = mRecords.get(i);
                if (((r.events & PhoneStateListener.LISTEN_CALL_STATE) != 0) &&
                    (r.subscription == subscription)) {
                    try {
                        r.callback.onCallStateChanged(state, incomingNumber);
                    } catch (RemoteException ex) {
                        remove(r.binder);
                    }
                }
            }
        }
        broadcastCallStateChanged(state, incomingNumber, subscription);
    }

    public void notifyServiceState(ServiceState state) {
        notifyServiceStateOnSubscription(state, mDefaultSubscription);
    }

    public void notifyServiceStateOnSubscription(ServiceState state, int subscription) {
        if (!checkNotifyPermission("notifyServiceState()")){
            return;
        }
        synchronized (mRecords) {
            mServiceState[subscription] = state;
            for (int i = mRecords.size() - 1; i >= 0; i--) {
                Record r = mRecords.get(i);
                if (((r.events & PhoneStateListener.LISTEN_SERVICE_STATE) != 0) &&
                    (r.subscription == subscription)) {
                    sendServiceState(r, state);
                }
            }
        }
        broadcastServiceStateChanged(state, subscription);
    }

    public void notifySignalStrength(SignalStrength signalStrength) {
        notifySignalStrengthOnSubscription(signalStrength, mDefaultSubscription);
    }

    public void notifySignalStrengthOnSubscription(SignalStrength signalStrength, int subscription) {
        if (!checkNotifyPermission("notifySignalStrength()")) {
            return;
        }
        synchronized (mRecords) {
            mSignalStrength[subscription] = signalStrength;
            for (int i = mRecords.size() - 1; i >= 0; i--) {
                Record r = mRecords.get(i);
                if (((r.events & PhoneStateListener.LISTEN_SIGNAL_STRENGTHS) != 0) &&
                    (r.subscription == subscription)){
                    sendSignalStrength(r, signalStrength);
                }
                if (((r.events & PhoneStateListener.LISTEN_SIGNAL_STRENGTH) != 0) &&
                    (r.subscription == subscription)) {
                    try {
                        int gsmSignalStrength = signalStrength.getGsmSignalStrength();
                        r.callback.onSignalStrengthChanged((gsmSignalStrength == 99 ? -1
                                : gsmSignalStrength));
                    } catch (RemoteException ex) {
                        remove(r.binder);
                    }
                }
            }
        }
        broadcastSignalStrengthChanged(signalStrength, subscription);
    }

    public void notifyMessageWaitingChanged(boolean mwi) {
        notifyMessageWaitingChangedOnSubscription(mwi, mDefaultSubscription);
    }

    public void notifyMessageWaitingChangedOnSubscription(boolean mwi, int subscription) {
        if (!checkNotifyPermission("notifyMessageWaitingChanged()")) {
            return;
        }
        synchronized (mRecords) {
            mMessageWaiting[subscription] = mwi;
            for (int i = mRecords.size() - 1; i >= 0; i--) {
                Record r = mRecords.get(i);
                if (((r.events & PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR) != 0) &&
                    (r.subscription == subscription)) {
                    try {
                        r.callback.onMessageWaitingIndicatorChanged(mwi);
                    } catch (RemoteException ex) {
                        remove(r.binder);
                    }
                }
            }
        }
    }

    public void notifyCallForwardingChanged(boolean cfi) {
        notifyCallForwardingChangedOnSubscription(cfi, mDefaultSubscription);
    }

    public void notifyCallForwardingChangedOnSubscription(boolean cfi, int subscription) {
        if (!checkNotifyPermission("notifyCallForwardingChanged()")) {
            return;
        }
        synchronized (mRecords) {
            mCallForwarding[subscription] = cfi;
            for (int i = mRecords.size() - 1; i >= 0; i--) {
                Record r = mRecords.get(i);
                if (((r.events & PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR) != 0) &&
                    (r.subscription == subscription)) {
                    try {
                        r.callback.onCallForwardingIndicatorChanged(cfi);
                    } catch (RemoteException ex) {
                        remove(r.binder);
                    }
                }
            }
        }
    }

    public void notifyDataActivity(int state) {
        notifyDataActivityOnSubscription(state, mDefaultSubscription);
    }

    public void notifyDataActivityOnSubscription(int state, int subscription) {
        if (!checkNotifyPermission("notifyDataActivity()" )) {
            return;
        }
        synchronized (mRecords) {
            if (subscription >= 0) {
                mDataActivity[subscription] = state;
            }
            for (int i = mRecords.size() - 1; i >= 0; i--) {
                Record r = mRecords.get(i);
                if (((r.events & PhoneStateListener.LISTEN_DATA_ACTIVITY) != 0) &&
                    (r.subscription == subscription)) {
                    try {
                        r.callback.onDataActivity(state);
                    } catch (RemoteException ex) {
                        remove(r.binder);
                    }
                }
            }
        }
    }

    /*
     * anyDataConnectionState - CONNECTED if at least one data connection is CONNECTED
     * apnType, ipVersion - <apnType, ipVersion> tuple whose connection state has changed
     * state - state of apnType on ipVersion
     * apn - if state is CONNECTED, apn used for establishing apnType on ipVersion
     * interfaceName - if state is CONNECTED, interfaceName used for apnType on ipVersion
     * reason - reason that triggered
     * isDataConnectivityPossible - if nw has camped, sim ready, etc.
     * networkType - radio technology used
     */
    public void notifyDataConnection(
            int anyDataConnectionState,
            String apnType, String ipVersion,
            int state, String apn, String interfaceName,
            String ipAddress, String gwAddress,
            boolean isDataConnectivityPossible, int networkType, String reason) {

        notifyDataConnectionOnSubscription(anyDataConnectionState, apnType, ipVersion,
            state, apn, interfaceName, ipAddress, gwAddress, isDataConnectivityPossible, networkType, reason,
            mDefaultSubscription);
    }

    public void notifyDataConnectionOnSubscription(
            int anyDataConnectionState,
            String apnType, String ipVersion,
            int state, String apn, String interfaceName,
            String ipAddress, String gwAddress,
            boolean isDataConnectivityPossible, int networkType,
            String reason, int subscription) {
        if (!checkNotifyPermission("notifyDataConnection()" )) {
            return;
        }
        synchronized (mRecords) {
            /* notify clients only if there is a real change in data connection state */
            if ((mDataConnectionState[subscription] != anyDataConnectionState)
                    || (mDataConnectionNetworkType[subscription] != networkType)) {
                /* cache last notification for LISTEN_DATA_CONNECTION_STATE clients. */
                mDataConnectionState[subscription] = anyDataConnectionState;
                mDataConnectionPossible[subscription] = isDataConnectivityPossible;
                mDataConnectionReason[subscription] = reason;
                mDataConnectionNetworkType[subscription] = networkType;

                for (int i = mRecords.size() - 1; i >= 0; i--) {
                    Record r = mRecords.get(i);
                    if (((r.events & PhoneStateListener.LISTEN_DATA_CONNECTION_STATE) != 0) &&
                        (r.subscription == subscription)) {
                        try {
                            r.callback.onDataConnectionStateChanged(anyDataConnectionState, networkType);
                        } catch (RemoteException ex) {
                            remove(r.binder);
                        }
                    }
                }
            }
        }

        broadcastDataConnectionStateChanged(anyDataConnectionState, apnType, ipVersion, state, apn,
                interfaceName, ipAddress, gwAddress, isDataConnectivityPossible, reason, subscription);

    }

    public void notifyDataConnectionFailed(String reason) {
        notifyDataConnectionFailedOnSubscription(reason, mDefaultSubscription);
    }

    public void notifyDataConnectionFailedOnSubscription(String reason, int subscription) {
        if (!checkNotifyPermission("notifyDataConnectionFailed()")) {
            return;
        }
        /*
         * This is commented out because there is on onDataConnectionFailed callback
         * on PhoneStateListener. There should be
        synchronized (mRecords) {
            mDataConnectionFailedReason = reason;
            final int N = mRecords.size();
            for (int i=N-1; i>=0; i--) {
                Record r = mRecords.get(i);
                if ((r.events & PhoneStateListener.LISTEN_DATA_CONNECTION_FAILED) != 0) {
                    // XXX
                }
            }
        }
        */
        broadcastDataConnectionFailed(reason, subscription);
    }

    public void notifyCellLocation(Bundle cellLocation) {
        notifyCellLocationOnSubscription(cellLocation, mDefaultSubscription);
    }

    public void notifyCellLocationOnSubscription(Bundle cellLocation, int subscription) {
        if (!checkNotifyPermission("notifyCellLocation()")) {
            return;
        }
        synchronized (mRecords) {
            mCellLocation[subscription] = cellLocation;
            for (int i = mRecords.size() - 1; i >= 0; i--) {
                Record r = mRecords.get(i);
                if (((r.events & PhoneStateListener.LISTEN_CELL_LOCATION) != 0) &&
                    (r.subscription == subscription)) {
                    sendCellLocation(r, cellLocation);
                }
            }
        }
    }

    /**
     * Copy the service state object so they can't mess it up in the local calls
     */
    public void sendServiceState(Record r, ServiceState state) {
        try {
            r.callback.onServiceStateChanged(new ServiceState(state));
        } catch (RemoteException ex) {
            remove(r.binder);
        }
    }

    private void sendCellLocation(Record r, Bundle cellLocation) {
        try {
            r.callback.onCellLocationChanged(new Bundle(cellLocation));
        } catch (RemoteException ex) {
            remove(r.binder);
        }
    }

    private void sendSignalStrength(Record r, SignalStrength signalStrength) {
        try {
            r.callback.onSignalStrengthsChanged(new SignalStrength(signalStrength));
        } catch (RemoteException ex) {
            remove(r.binder);
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump telephony.registry from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (mRecords) {
            final int recordCount = mRecords.size();
            pw.println("last known state:");
            for (int i = 0; i < TelephonyManager.getPhoneCount(); i++) {
                pw.println("  mCallState=" + mCallState[i]);
                pw.println("  mCallIncomingNumber=" + mCallIncomingNumber[i]);
                pw.println("  mServiceState=" + mServiceState[i]);
                pw.println("  mSignalStrength=" + mSignalStrength[i]);
                pw.println("  mMessageWaiting=" + mMessageWaiting[i]);
                pw.println("  mCallForwarding=" + mCallForwarding[i]);
                pw.println("  mDataActivity=" + mDataActivity[i]);
                pw.println("  mDataConnectionState=" + mDataConnectionState[i]);
                pw.println("  mDataConnectionPossible=" + mDataConnectionPossible[i]);
                pw.println("  mDataConnectionReason=" + mDataConnectionReason[i]);
                pw.println("  mCellLocation=" + mCellLocation[i]);
            }
            pw.println("registrations: count=" + recordCount);
            for (int i = 0; i < recordCount; i++) {
                Record r = mRecords.get(i);
                pw.println("  " + r.pkgForDebug + " 0x" + Integer.toHexString(r.events));
            }
        }
    }

    //
    // the legacy intent broadcasting
    //

    private void broadcastServiceStateChanged(ServiceState state, int subscription) {
        long ident = Binder.clearCallingIdentity();
        try {
            mBatteryStats.notePhoneState(state.getState());
        } catch (RemoteException re) {
            // Can't do much
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        Intent intent = new Intent(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        Bundle data = new Bundle();
        state.fillInNotifierBundle(data);
        intent.putExtras(data);
        // Pass the subscription along with the intent.
        intent.putExtra(SUBSCRIPTION, subscription);
        mContext.sendStickyBroadcast(intent);
    }

    private void broadcastSignalStrengthChanged(SignalStrength signalStrength, int subscription) {
        long ident = Binder.clearCallingIdentity();
        try {
            mBatteryStats.notePhoneSignalStrength(signalStrength);
        } catch (RemoteException e) {
            /* The remote entity disappeared, we can safely ignore the exception. */
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        Intent intent = new Intent(TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        Bundle data = new Bundle();
        signalStrength.fillInNotifierBundle(data);
        intent.putExtras(data);
        intent.putExtra(SUBSCRIPTION, subscription);
        mContext.sendStickyBroadcast(intent);
    }

    private void broadcastCallStateChanged(int state, String incomingNumber, int subscription) {
        long ident = Binder.clearCallingIdentity();
        try {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                mBatteryStats.notePhoneOff();
            } else {
                mBatteryStats.notePhoneOn();
            }
        } catch (RemoteException e) {
            /* The remote entity disappeared, we can safely ignore the exception. */
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        Intent intent = new Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(Phone.STATE_KEY, DefaultPhoneNotifier.convertCallState(state).toString());
        if (!TextUtils.isEmpty(incomingNumber)) {
            intent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, incomingNumber);
        }
        intent.putExtra(SUBSCRIPTION, subscription);
        mContext.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
    }

    private void broadcastDataConnectionStateChanged(int anyDataConnectionState,
            String apnType, String ipVersion,
            int state, String apn, String interfaceName,
            String ipAddress, String gwAddress,
            boolean isDataConnectivityPossible, String reason, int subscription) {
        // Note: not reporting to the battery stats service here, because the
        // status bar takes care of that after taking into account all of the
        // required info.
        Intent intent = new Intent(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(Phone.STATE_KEY, DefaultPhoneNotifier.convertDataState(anyDataConnectionState).toString());
        if (!isDataConnectivityPossible) {
            intent.putExtra(Phone.NETWORK_UNAVAILABLE_KEY, true);
        }
        if (reason != null) {
            intent.putExtra(Phone.STATE_CHANGE_REASON_KEY, reason);
        }
        intent.putExtra(Phone.DATA_APN_TYPES_KEY, apnType);
        intent.putExtra(Phone.DATA_IPVERSION_KEY, ipVersion);
        intent.putExtra(Phone.DATA_APN_TYPE_STATE,
                DefaultPhoneNotifier.convertDataState(state).toString());
        intent.putExtra(Phone.DATA_IFACE_NAME_KEY, interfaceName);
        intent.putExtra(Phone.DATA_APN_KEY, apn);
        intent.putExtra(Phone.DATA_IP_ADDRESS_KEY, ipAddress);
        intent.putExtra(Phone.DATA_GW_ADDRESS_KEY, gwAddress);
        intent.putExtra(SUBSCRIPTION, subscription);
        //TODO: perhaps sticky is not a good idea, as we broadcast for each <apn type/ip version>
        //and last broadcast may not be very relevant.
        mContext.sendStickyBroadcast(intent);
    }

    private void broadcastDataConnectionFailed(String reason, int subscription) {
        Intent intent = new Intent(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(Phone.FAILURE_REASON_KEY, reason);
        intent.putExtra(SUBSCRIPTION, subscription);
        mContext.sendStickyBroadcast(intent);
    }

    private boolean checkNotifyPermission(String method) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        String msg = "Modify Phone State Permission Denial: " + method + " from pid="
                + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
        Slog.w(TAG, msg);
        return false;
    }

    private void checkListenerPermission(int events) {
        if ((events & PhoneStateListener.LISTEN_CELL_LOCATION) != 0) {
            mContext.enforceCallingOrSelfPermission(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION, null);

        }

        if ((events & PHONE_STATE_PERMISSION_MASK) != 0) {
            mContext.enforceCallingOrSelfPermission(
                    android.Manifest.permission.READ_PHONE_STATE, null);
        }
    }
}
