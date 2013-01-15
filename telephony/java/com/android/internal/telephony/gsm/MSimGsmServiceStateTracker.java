/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011 The Linux Foundation. All rights reserved.
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

import android.content.Intent;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Telephony.Intents;
import android.telephony.MSimTelephonyManager;
import android.util.Log;

import com.android.internal.telephony.DataConnectionTracker;
import com.android.internal.telephony.MSimPhoneFactory;
import com.android.internal.telephony.MSimProxyManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.Subscription;
import com.android.internal.telephony.SubscriptionManager;
import com.android.internal.telephony.UiccCardApplication;
import com.android.internal.telephony.UiccManager.AppFamily;

import static com.android.internal.telephony.MSimConstants.DEFAULT_SUBSCRIPTION;

/**
 * {@hide}
 */
final class MSimGsmServiceStateTracker extends GsmServiceStateTracker {

    protected static final int EVENT_ALL_DATA_DISCONNECTED = 1001;

    public MSimGsmServiceStateTracker(GSMPhone phone) {
        super(phone);
    }

    public void updateRecords() {
        updateIccAvailability();
    }

    @Override
    protected UiccCardApplication getUiccCardApplication() {
        Subscription subscriptionData = ((MSimGSMPhone)phone).getSubscriptionInfo();
        if(subscriptionData != null) {
            return  mUiccManager.getUiccCardApplication(subscriptionData.slotId,
                    AppFamily.APP_FAM_3GPP);
        }
        return null;
    }

    @Override
    public String getSystemProperty(String property, String defValue) {
        return MSimTelephonyManager.getTelephonyProperty(property, ((MSimGSMPhone)phone).getSubscription(), defValue);
    }

    /**
     * Clean up existing voice and data connection then turn off radio power.
     *
     * Hang up the existing voice calls to decrease call drop rate.
     */
    @Override
    public void powerOffRadioSafely(DataConnectionTracker dcTracker) {
        synchronized (this) {
            if (!mPendingRadioPowerOffAfterDataOff) {
                int dds = MSimPhoneFactory.getDataSubscription();
                // To minimize race conditions we call cleanUpAllConnections on
                // both if else paths instead of before this isDisconnected test.
                if (dcTracker.isDisconnected()
                        && (dds == phone.getSubscription()
                            || (dds != phone.getSubscription()
                                && MSimProxyManager.getInstance().isDataDisconnected(dds)))) {
                    // To minimize race conditions we do this after isDisconnected
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    if (DBG) log("Data disconnected, turn off radio right away.");
                    hangupAndPowerOff();
                } else {
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    if (dds != phone.getSubscription()
                            && !MSimProxyManager.getInstance().isDataDisconnected(dds)) {
                        if (DBG) log("Data is active on DDS.  Wait for all data disconnect");
                        // Data is not disconnected on DDS. Wait for the data disconnect complete
                        // before sending the RADIO_POWER off.
                        MSimProxyManager.getInstance().registerForAllDataDisconnected(dds, this,
                                EVENT_ALL_DATA_DISCONNECTED, null);
                        mPendingRadioPowerOffAfterDataOff = true;
                    }
                    Message msg = Message.obtain(this);
                    msg.what = EVENT_SET_RADIO_POWER_OFF;
                    msg.arg1 = ++mPendingRadioPowerOffAfterDataOffTag;
                    if (sendMessageDelayed(msg, 30000)) {
                        if (DBG) log("Wait upto 30s for data to disconnect, then turn off radio.");
                        mPendingRadioPowerOffAfterDataOff = true;
                    } else {
                        log("Cannot send delayed Msg, turn off radio right away.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOff = false;
                    }
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_ALL_DATA_DISCONNECTED:
                int dds = MSimPhoneFactory.getDataSubscription();
                MSimProxyManager.getInstance().unregisterForAllDataDisconnected(dds, this);
                synchronized(this) {
                    if (mPendingRadioPowerOffAfterDataOff) {
                        if (DBG) log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    }
                }
                break;

            case EVENT_NITZ_TIME:
                if(!(SystemProperties.getBoolean("persist.timed.enable", false))) {
                    SubscriptionManager subMgr = SubscriptionManager.getInstance();
                    log("EVENT_NITZ_TIME received phone type ::" + MSimTelephonyManager.
                            getDefault().getCurrentPhoneType(DEFAULT_SUBSCRIPTION) +
                            "is cdma sub active ::" + subMgr.isSubActive(DEFAULT_SUBSCRIPTION));
                    if (MSimTelephonyManager.PHONE_TYPE_CDMA == MSimTelephonyManager.
                            getDefault().getCurrentPhoneType(DEFAULT_SUBSCRIPTION) &&
                            subMgr.isSubActive(DEFAULT_SUBSCRIPTION)) {
                        log("EVENT_NITZ_TIME received in c + g ignore updating time");
                    }
                } else {
                    super.handleMessage(msg);
                }
                break;

            default:
                super.handleMessage(msg);
                break;
        }
    }

    @Override
    protected void log(String s) {
        Log.d(LOG_TAG, "[GsmSST] [SUB : " + ((MSimGSMPhone)phone).getSubscription() + "] " + s);
    }

    @Override
    protected void loge(String s) {
        Log.e(LOG_TAG, "[GsmSST] [SUB : " + ((MSimGSMPhone)phone).getSubscription() + "] " + s);
    }

}
