/*
 * Copyright (C) 2006 The Android Open Source Project
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

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;

public class PhoneSubInfoProxy extends IPhoneSubInfo.Stub {
    private Phone[] mPhone;
    private PhoneSubInfo mPhoneSubInfo;

    public PhoneSubInfoProxy(Phone Phone,PhoneSubInfo phoneSubInfo) {
        mPhoneSubInfo = phoneSubInfo;

        int numPhones = TelephonyManager.getPhoneCount();
        mPhone = new Phone[numPhones];
        mPhone[0] = Phone;

        if(ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
    }

    public PhoneSubInfoProxy(Phone[] phone) {
        mPhone = phone;
        if(ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
    }

    public void setmPhoneSubInfo(PhoneSubInfo phoneSubInfo) {
        this.mPhoneSubInfo = phoneSubInfo;
    }

    public String getDeviceId() {
        return getDeviceIdOnSubscription(getDefaultSubscription());
    }

    public String getDeviceIdOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getDeviceId();
    }

    public String getDeviceSvn() {
        return getDeviceSvnOnSubscription(getDefaultSubscription());
    }

    public String getDeviceSvnOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getDeviceSvn();
    }

    /**
     * Retrieves the unique sbuscriber ID, e.g., IMSI for GSM phones.
     */
    public String getSubscriberId() {
        return getSubscriberIdOnSubscription(getDefaultSubscription());
    }

    /**
     * Retrieves the unique sbuscriber ID, e.g., IMSI for GSM phones
     * for a subscription
     */
    public String getSubscriberIdOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getSubscriberId();
    }

    /**
     * Retrieves the serial number of the ICC, if applicable.
     */
    public String getIccSerialNumber() {
        return getIccSerialNumberOnSubscription(getDefaultSubscription());
    }

    /**
     * Retrieves the serial number of the ICC, if applicable
     * for a subscription.
     */
    public String getIccSerialNumberOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getIccSerialNumber();
    }

    /**
     * Retrieves the phone number string for line 1.
     */
    public String getLine1Number() {
        return getLine1NumberOnSubscription(getDefaultSubscription());
    }

    /**
     * Retrieves the phone number string for line 1
     * for a subscription.
     */
    public String getLine1NumberOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getLine1Number();
    }

    /**
     * Retrieves the alpha identifier for line 1.
     */
    public String getLine1AlphaTag() {
        return getLine1AlphaTagOnSubscription(getDefaultSubscription());
    }

    /**
     * Retrieves the alpha identifier for line 1
     * for a subscription.
     */
    public String getLine1AlphaTagOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getLine1AlphaTag();
    }

    /**
     * Retrieves the voice mail number.
     */
    public String getVoiceMailNumber() {
        return getVoiceMailNumberOnSubscription(getDefaultSubscription());
    }

    /**
     * Retrieves the voice mail number
     * for a subscription.
     */
    public String getVoiceMailNumberOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getVoiceMailNumber();
    }

    /**
     * Retrieves the complete voice mail number.
     */
    public String getCompleteVoiceMailNumber() {
        return mPhoneSubInfo.getCompleteVoiceMailNumber();
    }

    /**
     * Retrieves the complete voice mail number on the specified subscription
     */
    public String getCompleteVoiceMailNumberOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getCompleteVoiceMailNumber();
    }

    /**
     * Retrieves the alpha identifier associated with the voice mail number.
     */
    public String getVoiceMailAlphaTag() {
        return getVoiceMailAlphaTagOnSubscription(getDefaultSubscription());
    }

    /**
     * Retrieves the alpha identifier associated with the voice mail number
     * for a subscription.
     */
    public String getVoiceMailAlphaTagOnSubscription(int subscription) {
        return getPhoneSubInfo(subscription).getVoiceMailAlphaTag();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        mPhoneSubInfo.dump(fd, pw, args);
    }

    private PhoneSubInfo getPhoneSubInfo(int subscription) {
        return mPhone[subscription].getPhoneSubInfo();
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }
}
