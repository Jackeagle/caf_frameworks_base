/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2012 The Linux Foundation. All rights reserved.
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
import android.net.Uri;
import android.provider.Telephony.Sms.Intents;
import android.util.Log;

import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.ImsSMSDispatcher;

final class MSimCdmaSMSDispatcher extends CdmaSMSDispatcher {

    MSimCdmaSMSDispatcher(PhoneBase phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor, ImsSMSDispatcher imsSMSDispatcher) {
        super(phone, storageMonitor, usageMonitor, imsSMSDispatcher);
        Log.d(TAG, "MSimCdmaSMSDispatcher created");
    }

    /**
     * Dispatches standard PDUs to interested applications
     *
     * @param pdus The raw PDUs making up the message
     */
    @Override
    protected void dispatchPdus(byte[][] pdus) {
        Intent intent = new Intent(Intents.SMS_RECEIVED_ACTION);
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", getFormat());
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                 mPhone.getSubscription()); //Subscription information to be passed in an intent
        dispatch(intent, RECEIVE_SMS_PERMISSION);
    }

    /**
     * Dispatches port addressed PDUs to interested applications
     *
     * @param pdus The raw PDUs making up the message
     * @param port The destination port of the messages
     */
    @Override
    protected void dispatchPortAddressedPdus(byte[][] pdus, int port) {
        Uri uri = Uri.parse("sms://localhost:" + port);
        Intent intent = new Intent(Intents.DATA_SMS_RECEIVED_ACTION, uri);
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", getFormat());
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY,
                 mPhone.getSubscription()); //Subscription information to be passed in an intent
        dispatch(intent, RECEIVE_SMS_PERMISSION);
    }
}
