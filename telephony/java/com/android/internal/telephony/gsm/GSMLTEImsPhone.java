/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2012, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

package com.android.internal.telephony.gsm;

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallDetails;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCardApplicationStatus;
import com.android.internal.telephony.ImsCallTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.ims.RilImsPhone;
import com.android.internal.telephony.Connection.DisconnectCause;

import android.content.Context;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class GSMLTEImsPhone extends GSMPhone {
    private static final String LOG_TAG = "GSMLTEImsPhone";
    private RilImsPhone imsPhone;

    public
    GSMLTEImsPhone (Context context, CommandsInterface ci, PhoneNotifier notifier) {
        this(context,ci,notifier, false);
    }

    public GSMLTEImsPhone(Context context, CommandsInterface ci, PhoneNotifier notifier,
            boolean unitTestMode) {
        super(context, ci, notifier);
        if (PhoneFactory.isCallOnImsEnabled()) {
            /*
             * Default phone is not registered yet. Post event to buy
             * time and register imsphone, after PhoneApp onCreate completes
             * registering default phone.
             */
            sendMessage(obtainMessage(EVENT_INIT_COMPLETE));
        }
    }

    @Override
    protected void setCallTracker() {
        mCT = new ImsCallTracker(this);
    }

    @Override
    public State getState() {
        return mCT.state;
    }

    @Override
    public void setState(Phone.State newState) {
        mCT.state = newState;
    }

    @Override
    public void handleMessage (Message msg) {
        Log.d(LOG_TAG, "Received event:" + msg.what);
        switch (msg.what) {
            case EVENT_INIT_COMPLETE:
                /*
                 * Register imsPhone after CDMALTEIMSPhone is registered as defaultphone
                 * through PhoneApp OnCreate.
                 */
                createImsPhone();
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public String getPhoneName() {
        return "GSMLTEIms";
    }

    public int getMaxConnectionsPerCall() {
        return ImsCallTracker.MAX_CONNECTIONS_PER_CALL;
    }

    public int getMaxConnections() {
        return ImsCallTracker.MAX_CONNECTIONS;
    }

    public Connection
    dial (String dialString) throws CallStateException {
        String newDialString = PhoneNumberUtils.stripSeparators(dialString);
        Log.d(LOG_TAG, "dialString=" + newDialString);
        CallDetails calldetails = new CallDetails();
        calldetails.call_domain = CallDetails.RIL_CALL_DOMAIN_CS;
        return mCT.dial(newDialString, calldetails);
    }

    private void createImsPhone() {
        Log.d(LOG_TAG, "Creating RilImsPhone");
        if (imsPhone == null) {
            if (getCallTracker() != null) {
                imsPhone = new RilImsPhone(getContext(), mNotifier, getCallTracker(),
                        mCM);
                CallManager.getInstance().registerPhone(imsPhone);
            } else {
                Log.e(LOG_TAG, "Null call tracker!!! Unable to create RilImsPhone");
            }
        } else {
            Log.e(LOG_TAG, "ImsPhone present already");
        }
    }

    private void destroyImsPhone() {
        if (imsPhone != null) {
            CallManager.getInstance().unregisterPhone(imsPhone);
            imsPhone.dispose();
        }
        imsPhone = null;
    }

    @Override
    public void dispose() {
        mCM.unregisterForImsNetworkStateChanged(this);
        destroyImsPhone();
        super.dispose();
    }

    public DisconnectCause
    disconnectCauseFromCode(int causeCode) {
        /**
         * See 22.001 Annex F.4 for mapping of cause codes to local tones
         */
        if (getUiccApplication() == null ||
                getUiccApplication().getState() !=
                IccCardApplicationStatus.AppState.APPSTATE_READY) {
            return DisconnectCause.ICC_ERROR;
        } else if (causeCode == CallFailCause.ERROR_UNSPECIFIED) {
            if (mSST.mRestrictedState.isCsRestricted()) {
                return DisconnectCause.CS_RESTRICTED;
            } else if (mSST.mRestrictedState.isCsEmergencyRestricted()) {
                return DisconnectCause.CS_RESTRICTED_EMERGENCY;
            } else if (mSST.mRestrictedState.isCsNormalRestricted()) {
                return DisconnectCause.CS_RESTRICTED_NORMAL;
            } else {
                return DisconnectCause.ERROR_UNSPECIFIED;
            }
        } else if (causeCode == CallFailCause.NORMAL_CLEARING) {
            return DisconnectCause.NORMAL;
        } else {
            // If nothing else matches, report unknown call drop reason
            // to app, not NORMAL call end.
            return DisconnectCause.ERROR_UNSPECIFIED;
        }
    }

    public void
    switchHoldingAndActive() throws CallStateException {
        mCT.switchWaitingOrHoldingAndActive(this);
    }

    public boolean canConference() {
        return mCT.canConference(this);
    }

    public void conference() throws CallStateException {
        mCT.conference(this);
    }
}