/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011-12 Code Aurora Forum. All rights reserved.
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

package com.android.internal.telephony.msim;

import android.content.Intent;
import android.content.res.Resources;
import android.provider.Telephony.Intents;
import android.telephony.MSimTelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.gsm.GsmServiceStateTracker;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.msim.Subscription;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;

/**
 * {@hide}
 */
public final class MSimGsmServiceStateTracker extends GsmServiceStateTracker {

    static final String LOG_TAG = "GSM";
    static final boolean DBG = true;

    public MSimGsmServiceStateTracker(GSMPhone phone) {
        super(phone);
    }

    @Override
    protected UiccCardApplication getUiccCardApplication() {
        Subscription subscriptionData = ((MSimGSMPhone)phone).getSubscriptionInfo();
        if(subscriptionData != null) {
            return  mUiccController.getUiccCardApplication(subscriptionData.slotId,
                    UiccController.APP_FAM_3GPP);
        }
        return null;
    }

    @Override
    protected void updateSpnDisplay() {
        if (mIccRecords == null) {
            return;
        }
        int rule = mIccRecords.getDisplayRule(ss.getOperatorNumeric());
        String spn = mIccRecords.getServiceProviderName();
        String plmn = ss.getOperatorAlphaLong();

        // For emergency calls only, pass the EmergencyCallsOnly string via EXTRA_PLMN
        if (mEmergencyOnly && cm.getRadioState().isOn()) {
            plmn = Resources.getSystem().
                getText(com.android.internal.R.string.emergency_calls_only).toString();
            if (DBG) log("updateSpnDisplay: emergency only and radio is on plmn='" + plmn + "'");
        }

        if (rule != curSpnRule
                || !TextUtils.equals(spn, curSpn)
                || !TextUtils.equals(plmn, curPlmn)) {
            boolean showSpn = !mEmergencyOnly && !TextUtils.isEmpty(spn)
                && (rule & SIMRecords.SPN_RULE_SHOW_SPN) == SIMRecords.SPN_RULE_SHOW_SPN;
            boolean showPlmn = !TextUtils.isEmpty(plmn) &&
                (rule & SIMRecords.SPN_RULE_SHOW_PLMN) == SIMRecords.SPN_RULE_SHOW_PLMN;

            if (DBG) {
                log(String.format("updateSpnDisplay: changed sending intent" + " rule=" + rule +
                            " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'",
                            showPlmn, plmn, showSpn, spn));
            }
            Intent intent = new Intent(Intents.SPN_STRINGS_UPDATED_ACTION);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(Intents.EXTRA_SHOW_SPN, showSpn);
            intent.putExtra(Intents.EXTRA_SPN, spn);
            intent.putExtra(Intents.EXTRA_SHOW_PLMN, showPlmn);
            intent.putExtra(Intents.EXTRA_PLMN, plmn);
            intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, phone.getSubscription());
            phone.getContext().sendStickyBroadcast(intent);
        }

        curSpnRule = rule;
        curSpn = spn;
        curPlmn = plmn;
    }

    @Override
    public String getSystemProperty(String property, String defValue) {
        return MSimTelephonyManager.getTelephonyProperty(
                property, phone.getSubscription(), defValue);
    }

    @Override
    protected void log(String s) {
        Log.d(LOG_TAG, "[MSimGsmSST] [SUB : " + phone.getSubscription() + "] " + s);
    }

    @Override
    protected void loge(String s) {
        Log.e(LOG_TAG, "[MSimGsmSST] [SUB : " + phone.getSubscription() + "] " + s);
    }
}
