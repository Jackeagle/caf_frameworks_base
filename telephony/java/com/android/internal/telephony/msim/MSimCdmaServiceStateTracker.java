/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.provider.Telephony.Intents;
import android.telephony.MSimTelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.cdma.CdmaServiceStateTracker;
import com.android.internal.telephony.msim.Subscription;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccCardApplication;

import android.util.Log;

/**
 * {@hide}
 */
final class MSimCdmaServiceStateTracker extends CdmaServiceStateTracker {
    static final String LOG_TAG = "CDMA";
    public MSimCdmaServiceStateTracker(CDMAPhone phone) {
        super(phone);
    }

    @Override
    protected UiccCardApplication getUiccCardApplication() {
        Subscription subscriptionData = ((MSimCDMAPhone)phone).getSubscriptionInfo();
        if(subscriptionData != null) {
            return  mUiccController.getUiccCardApplication(
                    subscriptionData.slotId, UiccController.APP_FAM_3GPP2);
        }
        return null;
    }

    @Override
    protected void updateSpnDisplay() {
        // mOperatorAlphaLong contains the ERI text
        String plmn = ss.getOperatorAlphaLong();
        if (!TextUtils.equals(plmn, mCurPlmn)) {
            // Allow A blank plmn, "" to set showPlmn to true. Previously, we
            // would set showPlmn to true only if plmn was not empty, i.e. was not
            // null and not blank. But this would cause us to incorrectly display
            // "No Service". Now showPlmn is set to true for any non null string.
            boolean showPlmn = plmn != null;
            if (DBG) {
                log(String.format("updateSpnDisplay: changed sending intent" +
                            " showPlmn='%b' plmn='%s'", showPlmn, plmn));
            }
            Intent intent = new Intent(Intents.SPN_STRINGS_UPDATED_ACTION);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(Intents.EXTRA_SHOW_SPN, false);
            intent.putExtra(Intents.EXTRA_SPN, "");
            intent.putExtra(Intents.EXTRA_SHOW_PLMN, showPlmn);
            intent.putExtra(Intents.EXTRA_PLMN, plmn);
            intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, phone.getSubscription());
            phone.getContext().sendStickyBroadcast(intent);
        }

        mCurPlmn = plmn;
    }

    protected void updateCdmaSubscription() {
        cm.getCDMASubscription(obtainMessage(EVENT_POLL_STATE_CDMA_SUBSCRIPTION));
    }

    @Override
    protected String getSystemProperty(String property, String defValue) {
        return MSimTelephonyManager.getTelephonyProperty(
                property, phone.getSubscription(), defValue);
    }

    @Override
    protected void log(String s) {
        Log.d(LOG_TAG, "[MSimCdmaSST] [SUB : " + phone.getSubscription() + "] " + s);
    }

    @Override
    protected void loge(String s) {
        Log.e(LOG_TAG, "[MSimCdmaSST] [SUB : " + phone.getSubscription() + "] " + s);
    }
}
