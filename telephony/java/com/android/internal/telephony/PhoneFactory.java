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

import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.UiccManager;
import com.android.internal.telephony.ProxyManager;
import android.provider.Settings.SettingNotFoundException;

/**
 * {@hide}
 */
public class PhoneFactory {
    static final String LOG_TAG = "PHONE";
    static final int SOCKET_OPEN_RETRY_MILLIS = 2 * 1000;
    static final int SOCKET_OPEN_MAX_RETRY = 3;
    //***** Class Variables

    static private Phone sProxyPhone[] = null;
    static private CommandsInterface sCommandsInterface[] = null;
    static private UiccManager mUiccManager = null;
    static private ProxyManager mProxyManager = null;

    static private boolean sMadeDefaults = false;
    static private PhoneNotifier sPhoneNotifier;
    static private Looper sLooper;
    static private Context sContext;
    static private int numPhones = 1;
    static private int defaultSubscription = 0;
    static private int voiceSubscription = 0;
    static private int dataSubscription = 0;
    static private int smsSubscription = 0;


    static final int NORMAL_PHONE = 1;
    static final int DSDS_PHONE = 2;


    static final int preferredNetworkMode = RILConstants.PREFERRED_NETWORK_MODE;

    static final int preferredCdmaSubscription = RILConstants.PREFERRED_CDMA_SUBSCRIPTION;

    //***** Class Methods
    public static void makeDefaultPhones(Context context) {
        makeDefaultPhone(context);
    }

    /**
     * FIXME replace this with some other way of making these
     * instances
     */
    public static void makeDefaultPhone(Context context) {
        Log.i(LOG_TAG, "In makeDefaultPhone function");
        synchronized(Phone.class) {
            if (!sMadeDefaults) {
                sLooper = Looper.myLooper();
                sContext = context;

                if (sLooper == null) {
                    throw new RuntimeException(
                        "PhoneFactory.makeDefaultPhone must be called from Looper thread");
                }

                int retryCount = 0;
                for(;;) {
                    boolean hasException = false;
                    retryCount ++;

                    try {
                        // use UNIX domain socket to
                        // prevent subsequent initialization
                        new LocalServerSocket("com.android.internal.telephony");
                    } catch (java.io.IOException ex) {
                        hasException = true;
                    }

                    if ( !hasException ) {
                        break;
                    } else if (retryCount > SOCKET_OPEN_MAX_RETRY) {
                        throw new RuntimeException("PhoneFactory probably already running");
                    } else {
                        try {
                            Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                        } catch (InterruptedException er) {
                        }
                    }
                }
                /* In case of DSDS two instances of PhoneProxy,RIL are created.
                   where as in non-DSDS only instance. isDsdsEnabled() function checks
                   whether it is DSDS or non-DSDS */

                //DSDS num of phone instance2, non-DSDS it is 1
                numPhones = TelephonyManager.getPhoneCount();
                sPhoneNotifier = new DefaultPhoneNotifier();
                //Get preferredNetworkMode from Settings.System
                int networkMode = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
                Log.i(LOG_TAG, "Network Mode set to " + Integer.toString(networkMode));

                //Get preferredNetworkMode from Settings.System
                int cdmaSubscription = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.PREFERRED_CDMA_SUBSCRIPTION, preferredCdmaSubscription);
                Log.i(LOG_TAG, "Cdma Subscription set to " + Integer.toString(cdmaSubscription));
                sProxyPhone = new PhoneProxy[numPhones];
                sCommandsInterface = new RIL[numPhones];

                for(int i = 0; i < numPhones; i++) {
                    sCommandsInterface[i] = new RIL(context, networkMode, cdmaSubscription, i);
                }

                mUiccManager = UiccManager.getInstance(context, sCommandsInterface);
                for( int i = 0; i < numPhones; i++) {
                    int phoneType = getPhoneType(networkMode);
                    Log.i(LOG_TAG, "get Phone Type:"+ phoneType);
                    DataPhone dct = new MMDataConnectionTracker(context, sPhoneNotifier,
                                                                    sCommandsInterface[i]);

                    if (phoneType == VoicePhone.PHONE_TYPE_GSM) {
                        sProxyPhone[i] = new PhoneProxy(new GSMPhone(context,
                                                             sCommandsInterface[i], sPhoneNotifier), dct);
                        Log.i(LOG_TAG, "Creating GSMPhone");
                    } else if (phoneType == VoicePhone.PHONE_TYPE_CDMA) {
                        sProxyPhone[i] = new PhoneProxy(new CDMAPhone(context,
                                     sCommandsInterface[i], sPhoneNotifier), dct);
                        Log.i(LOG_TAG, "Creating CDMAPhone");
                    }
                }
                sMadeDefaults = true;
                mProxyManager = ProxyManager.getInstance(context, sProxyPhone, mUiccManager, sCommandsInterface);
            }

        }
    }

    /*
     * This function returns the type of the phone, depending
     * on the network mode.
     *
     * @param network mode
     * @return Phone Type
     */
    public static int getPhoneType(int networkMode) {
        switch(networkMode) {
        case RILConstants.NETWORK_MODE_CDMA:
        case RILConstants.NETWORK_MODE_CDMA_NO_EVDO:
        case RILConstants.NETWORK_MODE_EVDO_NO_CDMA:
        case RILConstants.NETWORK_MODE_CDMA_AND_LTE_EVDO:
            return Phone.PHONE_TYPE_CDMA;

        case RILConstants.NETWORK_MODE_WCDMA_PREF:
        case RILConstants.NETWORK_MODE_GSM_ONLY:
        case RILConstants.NETWORK_MODE_WCDMA_ONLY:
        case RILConstants.NETWORK_MODE_GSM_UMTS:
        case RILConstants.NETWORK_MODE_GSM_WCDMA_LTE:
            return Phone.PHONE_TYPE_GSM;

        case RILConstants.NETWORK_MODE_GLOBAL:
        case RILConstants.NETWORK_MODE_GLOBAL_LTE:
        case RILConstants.NETWORK_MODE_LTE_ONLY:
            return Phone.PHONE_TYPE_CDMA;
        default:
            return Phone.PHONE_TYPE_GSM;
        }
    }

    /* Sets the default subscription. If only one phone instance is active that
     * that subscription is set as default subscription. If both phone isntances
     * are active the first instance "0" is set as default subscription
     */
    public static void setDefaultSubscription(int subscription) {
       defaultSubscription = subscription;
    }

    public static Phone getDefaultPhone() {
        if (sLooper != Looper.myLooper()) {
            throw new RuntimeException(
                "PhoneFactory.getDefaultPhone must be called from Looper thread");
        }
        if (!sMadeDefaults) {
            throw new IllegalStateException("Default phones haven't been made yet!");
        }
       return sProxyPhone[defaultSubscription];
    }

    public static Phone getPhone(int subscription) {
        if (sLooper != Looper.myLooper()) {
            throw new RuntimeException(
                "PhoneFactory.getPhone must be called from Looper thread");
        }
        if (!sMadeDefaults) {
            throw new IllegalStateException("Default phones haven't been made yet!");
        }
       return sProxyPhone[subscription];
    }

    public static Phone[] getPhones() {
        if (sLooper != Looper.myLooper()) {
            throw new RuntimeException(
                "PhoneFactory.getDefaultPhone must be called from Looper thread");
        }
        if (!sMadeDefaults) {
            throw new IllegalStateException("Default phones haven't been made yet!");
        }
       return sProxyPhone;
    }

    public static VoicePhone getCdmaPhone() {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            VoicePhone phone = new CDMAPhone(sContext, sCommandsInterface[0], sPhoneNotifier);
            return phone;
        }
    }

    /* Gets CDMA phone attached with proper CommandInterface */
    public static VoicePhone getCdmaPhone(int subscription) {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            VoicePhone phone;
            phone = new CDMAPhone(sContext, sCommandsInterface[subscription], sPhoneNotifier);
            return phone;
        }
    }

    public static VoicePhone getGsmPhone() {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            VoicePhone phone = new GSMPhone(sContext, sCommandsInterface[0], sPhoneNotifier);
            return phone;
        }
    }

    /* Gets GSM phone attached with proper CommandInterface */
    public static VoicePhone getGsmPhone(int subscription) {
        Log.d(LOG_TAG,"getGsmPhone on sub :" + subscription);
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            VoicePhone phone = new GSMPhone(sContext, sCommandsInterface[subscription], sPhoneNotifier);
            return phone;
        }
    }

    /* Gets the default subscription */
    public static int getDefaultSubscription() {
        return defaultSubscription;
    }

     static public void setVoiceSubscription(int subscription) {
        Log.d(LOG_TAG, "setVoiceSubscription setting subscription to: " + subscription);
        Settings.System.putInt(sContext.getContentResolver(),Settings.System.DUAL_SIM_VOICE_CALL, subscription);
        voiceSubscription = subscription;
        Log.d(LOG_TAG, "setVoiceSubscription voiceSubscription: " + voiceSubscription);
    }

    static public void setDataSubscription(int subscription) {
        Settings.System.putInt(sContext.getContentResolver(),Settings.System.DUAL_SIM_DATA_CALL, subscription);
        dataSubscription = subscription;
        Log.d(LOG_TAG, "-----: setDataSubscription: " + dataSubscription);
    }

    static public void setSMSSubscription(int subscription) {
        Log.d(LOG_TAG, "setSMSSubscription setting subscription to: " + subscription);
        Settings.System.putInt(sContext.getContentResolver(),Settings.System.DUAL_SIM_SMS, subscription);
        smsSubscription = subscription;
        Log.d(LOG_TAG, "setSMSSubscription set subscription value: " + smsSubscription);
    }

    /* Gets User preferred Voice subscription setting*/
    public static int getVoiceSubscription(Context context) {
        try {
            voiceSubscription = Settings.System.getInt(context.getContentResolver(),Settings.System.DUAL_SIM_VOICE_CALL);
            Log.d(LOG_TAG, "getVoiceSubscription voiceSubscription: " + voiceSubscription);
        } catch (SettingNotFoundException snfe) {
            Log.e(LOG_TAG, "Settings Exception Reading Dual Sim Voice Call Values", snfe);
        }
        return voiceSubscription;
    }

    /* Gets User preferred Data subscription setting*/
    public static int getDataSubscription(Context context) {
        try {
            dataSubscription = Settings.System.getInt(context.getContentResolver(),Settings.System.DUAL_SIM_DATA_CALL);
        } catch (SettingNotFoundException snfe) {
            Log.e(LOG_TAG, "Settings Exception Reading Dual Sim Data Call Values", snfe);
        }
        return dataSubscription;
    }

    /* Gets User preferred SMS subscription setting*/
    public static int getSMSSubscription(Context context) {
        try {
            smsSubscription = Settings.System.getInt(context.getContentResolver(),Settings.System.DUAL_SIM_SMS);
            Log.d(LOG_TAG, "getSMSSubscription 1: " + smsSubscription);
            } catch (SettingNotFoundException snfe) {
                Log.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Values", snfe);
            }
        return smsSubscription;
    }
}
