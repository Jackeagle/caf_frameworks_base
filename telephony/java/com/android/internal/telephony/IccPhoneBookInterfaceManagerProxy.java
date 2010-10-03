/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.internal.telephony;

import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;


/**
 * SimPhoneBookInterfaceManager to provide an inter-process communication to
 * access ADN-like SIM records.
 */
public class IccPhoneBookInterfaceManagerProxy extends IIccPhoneBook.Stub {
    private IccPhoneBookInterfaceManager mIccPhoneBookInterfaceManager;
    private Phone[] mPhone;

    /* In DSDS only one IccPhonBookInterfaceManagerProxy exists */
    public IccPhoneBookInterfaceManagerProxy(Phone[] phone) {
        if(ServiceManager.getService("simphonebook") == null) {
               ServiceManager.addService("simphonebook", this);
        }
        mPhone = phone;
    }

    public IccPhoneBookInterfaceManagerProxy(Phone Phone,IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager) {
        mIccPhoneBookInterfaceManager = iccPhoneBookInterfaceManager;
        if(ServiceManager.getService("simphonebook") == null) {
            ServiceManager.addService("simphonebook", this);
        }
        int numPhones = TelephonyManager.getPhoneCount();
        mPhone = new Phone[numPhones];

        mPhone[0] = Phone;
    }

    public void setmIccPhoneBookInterfaceManager(
            IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager) {
        this.mIccPhoneBookInterfaceManager = iccPhoneBookInterfaceManager;
    }

    /* Non DSDS function is routed through DSDS function with default phone object */
    public boolean
        updateAdnRecordsInEfBySearch (int efid,
            String oldTag, String oldPhoneNumber,
            String newTag, String newPhoneNumber,
            String pin2) throws android.os.RemoteException {
        return updateAdnRecordsInEfBySearchOnSubscription(getDefaultSubscription(),
                efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
    }

    public boolean
    updateAdnRecordsInEfBySearchOnSubscription(int subscription, int efid,
            String oldTag, String oldPhoneNumber,
            String newTag, String newPhoneNumber,
            String pin2) throws android.os.RemoteException {
        return getIccPhoneBookInterfaceManager(subscription).updateAdnRecordsInEfBySearch(
                efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
    }

    /* Non DSDS function is routed through DSDS function with default phone object */
    public boolean
    updateAdnRecordsInEfByIndex(int efid, String newTag,
            String newPhoneNumber, int index, String pin2) throws android.os.RemoteException {
        return updateAdnRecordsInEfByIndexOnSubscription(getDefaultSubscription(), efid,
                newTag, newPhoneNumber, index, pin2);
    }

    public boolean
    updateAdnRecordsInEfByIndexOnSubscription(int subscription, int efid, String newTag,
            String newPhoneNumber, int index, String pin2) throws android.os.RemoteException {
        return getIccPhoneBookInterfaceManager(subscription).updateAdnRecordsInEfByIndex(efid,
                newTag, newPhoneNumber, index, pin2);
    }

    /* Non DSDS function is routed through DSDS function with default phone object */
    public int[] getAdnRecordsSize(int efid) throws android.os.RemoteException {
        return getAdnRecordsSizeOnSubscription(getDefaultSubscription(), efid);
    }

    public int[] getAdnRecordsSizeOnSubscription(int subscription, int efid) throws android.os.RemoteException {
        return getIccPhoneBookInterfaceManager(subscription).getAdnRecordsSize(efid);
    }

    /* Non DSDS function is routed through DSDS function with default phone object */
    public List<AdnRecord> getAdnRecordsInEf(int efid) throws android.os.RemoteException {
        return getAdnRecordsInEfOnSubscription(getDefaultSubscription(), efid);
    }

    public List<AdnRecord> getAdnRecordsInEfOnSubscription(int subscription, int efid) throws android.os.RemoteException {
        return getIccPhoneBookInterfaceManager(subscription).getAdnRecordsInEf(efid);
    }

    private IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager(int subscription) {
        return mPhone[subscription].getIccPhoneBookInterfaceManager();
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }

}
