/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.DataPhone.DataState;
import com.android.internal.telephony.DataPhone.IPVersion;

/**
 * broadcast intents
 */
public class DefaultPhoneNotifier implements PhoneNotifier {
    static final String LOG_TAG = "Phone";

    private static final boolean DBG = true;

    private ITelephonyRegistry mRegistry;


    private ServiceState[] mVoiceServiceState;
    private ServiceState[] mDataServiceState;

    /* package */
    DefaultPhoneNotifier() {
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                    "telephony.registry"));
        int numPhones = TelephonyManager.getPhoneCount();
        mVoiceServiceState = new ServiceState[numPhones];
        mDataServiceState = new ServiceState[numPhones];
        for (int i = 0; i < numPhones; i++) {
            mVoiceServiceState[i] = new ServiceState();
            mDataServiceState[i] = new ServiceState();
        }
    }

    public void notifyPhoneState(VoicePhone sender) {
        Call ringingCall = sender.getRingingCall();
        int subscription = sender.getSubscription();
        String incomingNumber = "";
        if (ringingCall != null && ringingCall.getEarliestConnection() != null) {
            incomingNumber = ringingCall.getEarliestConnection().getAddress();
        }
        try {
            mRegistry.notifyCallStateOnSubscription(convertCallState(sender.getState()), incomingNumber, subscription);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }


    /*
     * TODO: perhaps this should function should reside else where.
     */
    static ServiceState combineVoiceDataServiceStates(ServiceState vss, ServiceState dss) {

        ServiceState ss;
        if (vss != null) {
            /*
             * Copy all the fields from voice service state as voice phone has
             * the majority fields updated if data service is also present some
             * fields will be overwritten by data service below
             */
            ss = new ServiceState(vss);
        } else if (dss != null) {
            /* Voice phone did not send service state, use data service */
            ss = new ServiceState(dss);
        } else {
            /* we should never come here ideally */
            ss = new ServiceState();
            ss.setStateOutOfService();
            return ss;
        }

        /*
         * Update combined service state with data service information for the
         * below fields
         * 1. State is STATE_IN_SERVICE if voice or data has service
         * 2. Radio technology is data radio if data is in service
         *    Radio technology is voice radio only if data is not in service
         * 3. Roaming is set if either voice or data is roaming
         */

        if ((dss != null) && (vss != null)
                && (dss.getState() == ServiceState.STATE_IN_SERVICE)) {

            /*
             * If voice was not in service it will be overwritten with data
             * service state here
             */
            ss.setState(ServiceState.STATE_IN_SERVICE);

            /* Update radio technology to reflect the data technology */
            ss.setRadioTechnology(dss.getRadioTechnology());

            /* Overwrite voice roaming state if data is on roaming */
            if (dss.getRoaming())
                ss.setRoaming(true);
        }

        return ss;
    }

    private void notifyCombinedServiceState(int subscription) {

        ServiceState ss = combineVoiceDataServiceStates(mVoiceServiceState[subscription], mDataServiceState[subscription]);

        try {
            // send combined service state to UI
            mRegistry.notifyServiceStateOnSubscription(ss, subscription);
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyVoiceServiceState(VoicePhone sender) {
        int subscription = sender.getSubscription();
        mVoiceServiceState[subscription] = new ServiceState(sender.getVoiceServiceState());
        notifyCombinedServiceState(subscription);
    }

    public void notifyDataServiceState(DataPhone sender) {
        int subscription = sender.getSubscription();
        // DataPhone may return -1 if subscription is not initialized.
        if (subscription >= 0) {
            mDataServiceState[subscription] = new ServiceState(sender.getDataServiceState());
            notifyCombinedServiceState(subscription);
        }
    }

    public void notifySignalStrength(VoicePhone sender) {
        try {
            mRegistry.notifySignalStrengthOnSubscription(sender.getSignalStrength(),
                                           sender.getSubscription());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyMessageWaitingChanged(VoicePhone sender) {
        try {
            mRegistry.notifyMessageWaitingChangedOnSubscription(sender.getMessageWaitingIndicator(),
                                                 sender.getSubscription());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyCallForwardingChanged(VoicePhone sender) {
        try {
            mRegistry.notifyCallForwardingChangedOnSubscription(sender.getCallForwardingIndicator(),
                                                 sender.getSubscription());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyDataActivity(DataPhone sender) {
        try {
            mRegistry.notifyDataActivityOnSubscription(convertDataActivityState(sender.getDataActivityState()),
                                        sender.getSubscription());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyDataConnection(DataPhone sender, String apnType, IPVersion ipv, String reason) {
        TelephonyManager telephony = TelephonyManager.getDefault();

        /*
         * Notify Data Connection is called by DCT, whenever there is a change in data connection state
         * associated with <data service type / apn type, ipv>. We then pass the following information
         * to Telephony Registry.
         * 1. data connection State of service type <type> on IP Version <ipv>
         * 2. reason why data connection state changed
         * 3. apn/data profile through which <type> is active on <ipv>
         * 4. interface through which <type> is active on <ipv>
         * 5. ipv
         */

        Log.v("DATA", "[DefaultPhoneNotifier] : "
                + apnType + ", " + ipv + ", " + sender.getDataConnectionState(apnType, ipv));

        int subscription = sender.getSubscription();
        // Sometimes DataConnectionTracker returns -1 as subscription.
        if (subscription < 0) {
           subscription = 0;
        }
        try {
            mRegistry.notifyDataConnectionOnSubscription(
                    convertDataState(sender.getDataConnectionState()),
                    apnType,
                    ipv.toString(),
                    convertDataState(sender.getDataConnectionState(apnType, ipv)),
                    sender.getActiveApn(apnType, ipv),
                    sender.getInterfaceName(apnType, ipv),
                    sender.getIpAddress(apnType, ipv),
                    sender.getGateway(apnType, ipv),
                    sender.isDataConnectivityPossible(),
                    ((telephony != null) ? telephony.getNetworkType(subscription) :
                    TelephonyManager.NETWORK_TYPE_UNKNOWN),
                    reason,
                    sender.getSubscription());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyDataConnectionFailed(DataPhone sender, String reason) {
        try {
            mRegistry.notifyDataConnectionFailedOnSubscription(reason, sender.getSubscription());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    public void notifyCellLocation(VoicePhone sender) {
        Bundle data = new Bundle();
        sender.getCellLocation().fillInNotifierBundle(data);
        try {
            mRegistry.notifyCellLocationOnSubscription(data, sender.getSubscription());
        } catch (RemoteException ex) {
            // system process is dead
        }
    }

    private void log(String s) {
        Log.d(LOG_TAG, "[PhoneNotifier] " + s);
    }

    /**
     * Convert the {@link State} enum into the TelephonyManager.CALL_STATE_*
     * constants for the public API.
     */
    public static int convertCallState(VoicePhone.State state) {
        switch (state) {
            case RINGING:
                return TelephonyManager.CALL_STATE_RINGING;
            case OFFHOOK:
                return TelephonyManager.CALL_STATE_OFFHOOK;
            default:
                return TelephonyManager.CALL_STATE_IDLE;
        }
    }

    /**
     * Convert the TelephonyManager.CALL_STATE_* constants into the
     * {@link State} enum for the public API.
     */
    public static VoicePhone.State convertCallState(int state) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                return VoicePhone.State.RINGING;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return VoicePhone.State.OFFHOOK;
            default:
                return VoicePhone.State.IDLE;
        }
    }

    /**
     * Convert the {@link DataState} enum into the TelephonyManager.DATA_* constants
     * for the public API.
     */
    public static int convertDataState(DataPhone.DataState state) {
        switch (state) {
            case CONNECTING:
                return TelephonyManager.DATA_CONNECTING;
            case CONNECTED:
                return TelephonyManager.DATA_CONNECTED;
            case SUSPENDED:
                return TelephonyManager.DATA_SUSPENDED;
            default:
                return TelephonyManager.DATA_DISCONNECTED;
        }
    }

    /**
     * Convert the TelephonyManager.DATA_* constants into {@link DataState} enum
     * for the public API.
     */
    public static DataPhone.DataState convertDataState(int state) {
        switch (state) {
            case TelephonyManager.DATA_CONNECTING:
                return DataPhone.DataState.CONNECTING;
            case TelephonyManager.DATA_CONNECTED:
                return DataPhone.DataState.CONNECTED;
            case TelephonyManager.DATA_SUSPENDED:
                return DataPhone.DataState.SUSPENDED;
            default:
                return DataPhone.DataState.DISCONNECTED;
        }
    }

    /**
     * Convert the {@link DataState} enum into the TelephonyManager.DATA_* constants
     * for the public API.
     */
    public static int convertDataActivityState(DataPhone.DataActivityState state) {
        switch (state) {
            case DATAIN:
                return TelephonyManager.DATA_ACTIVITY_IN;
            case DATAOUT:
                return TelephonyManager.DATA_ACTIVITY_OUT;
            case DATAINANDOUT:
                return TelephonyManager.DATA_ACTIVITY_INOUT;
            case DORMANT:
                return TelephonyManager.DATA_ACTIVITY_DORMANT;
            default:
                return TelephonyManager.DATA_ACTIVITY_NONE;
        }
    }

    /**
     * Convert the TelephonyManager.DATA_* constants into the {@link DataState} enum
     * for the public API.
     */
    public static DataPhone.DataActivityState convertDataActivityState(int state) {
        switch (state) {
            case TelephonyManager.DATA_ACTIVITY_IN:
                return DataPhone.DataActivityState.DATAIN;
            case TelephonyManager.DATA_ACTIVITY_OUT:
                return DataPhone.DataActivityState.DATAOUT;
            case TelephonyManager.DATA_ACTIVITY_INOUT:
                return DataPhone.DataActivityState.DATAINANDOUT;
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
                return DataPhone.DataActivityState.DORMANT;
            default:
                return DataPhone.DataActivityState.NONE;
        }
    }
}
