/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011-12 The Linux Foundation. All rights reserved.
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

import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;
import android.telephony.MSimTelephonyManager;
import android.content.Intent;
import android.provider.Settings.SettingNotFoundException;

import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_DEFAULT_SUBSCRIPTION;
import com.android.internal.telephony.uicc.UiccController;

/**
 * {@hide}
 */
public class MSimPhoneFactory extends PhoneFactory {
    //***** Class Variables
    static final String LOG_TAG = "PHONE";
    static private Phone[] sProxyPhones = null;
    static private CommandsInterface[] sCommandsInterfaces = null;

    static private boolean sMadeMultiSimDefaults = false;

    static private MSimProxyManager mMSimProxyManager;
    static private CardSubscriptionManager mCardSubscriptionManager;
    static private SubscriptionManager mSubscriptionManager;
    static private UiccController mUiccController;

    //***** Class Methods

    public static void makeMultiSimDefaultPhones(Context context) {
        makeMultiSimDefaultPhone(context);
    }

    public static void makeMultiSimDefaultPhone(Context context) {
        synchronized(Phone.class) {
            if (!sMadeMultiSimDefaults) {
                sLooper = Looper.myLooper();
                sContext = context;

                if (sLooper == null) {
                    throw new RuntimeException(
                        "MSimPhoneFactory.makeDefaultPhone must be called from Looper thread");
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
                        throw new RuntimeException("MSimPhoneFactory probably already running");
                    } else {
                        try {
                            Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                        } catch (InterruptedException er) {
                        }
                    }
                }

                sPhoneNotifier = new MSimDefaultPhoneNotifier();

                // Get preferred network mode
                int preferredNetworkMode = RILConstants.PREFERRED_NETWORK_MODE;
                if (BaseCommands.getLteOnCdmaModeStatic() == Phone.LTE_ON_CDMA_TRUE) {
                    preferredNetworkMode = Phone.NT_MODE_GLOBAL;
                }

                // Get cdmaSubscription
                // TODO: Change when the ril will provides a way to know at runtime
                //       the configuration, bug 4202572. And the ril issues the
                //       RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED, bug 4295439.
                int cdmaSubscription = Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.CDMA_SUBSCRIPTION_MODE,
                        preferredCdmaSubscription);
                Log.i(LOG_TAG, "Cdma Subscription set to " + cdmaSubscription);

                /* In case of multi SIM mode two instances of PhoneProxy, RIL are created,
                   where as in single SIM mode only instance. isMultiSimEnabled() function checks
                   whether it is single SIM or multi SIM mode */
                int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
                int[] networkModes = new int[numPhones];
                sProxyPhones = new MSimPhoneProxy[numPhones];
                sCommandsInterfaces = new RIL[numPhones];

                for (int i = 0; i < numPhones; i++) {
                    //reads the system properties and makes commandsinterface
                    try {
                        networkModes[i]  = Settings.Secure.getIntAtIndex(
                                context.getContentResolver(),
                                Settings.Secure.PREFERRED_NETWORK_MODE, i);
                    } catch (SettingNotFoundException snfe) {
                        Log.e(LOG_TAG, "Settings Exception Reading Value At Index", snfe);
                        networkModes[i] = preferredNetworkMode;
                    }
                    Log.i(LOG_TAG, "Network Mode set to " + Integer.toString(networkModes[i]));
                    sCommandsInterfaces[i] = new RIL(context, networkModes[i],
                            cdmaSubscription, i);
                }

                // Instantiate UiccController so that all other classes can just call getInstance()
                mUiccController = UiccController.make(context, sCommandsInterfaces);

                mCardSubscriptionManager = CardSubscriptionManager.getInstance(context,
                        mUiccController, sCommandsInterfaces);
                mSubscriptionManager = SubscriptionManager.getInstance(context,
                        mUiccController, sCommandsInterfaces);

                for (int i = 0; i < numPhones; i++) {
                    int phoneType = getPhoneType(networkModes[i]);
                    if (phoneType == Phone.PHONE_TYPE_GSM) {
                        Log.i(LOG_TAG, "Creating MSimGSMPhone sub = " + i);
                        sProxyPhones[i] = new MSimPhoneProxy(new MSimGSMPhone(context,
                                sCommandsInterfaces[i], sPhoneNotifier, i));
                    } else if (phoneType == Phone.PHONE_TYPE_CDMA) {
                        Log.i(LOG_TAG, "Creating MSimCDMALTEPhone sub = " + i);
                        sProxyPhones[i] = new MSimPhoneProxy(new MSimCDMALTEPhone(context,
                                sCommandsInterfaces[i], sPhoneNotifier, i));
                    }
                }
                mMSimProxyManager = MSimProxyManager.getInstance(context, sProxyPhones,
                        mUiccController, sCommandsInterfaces);

                sMadeMultiSimDefaults = true;

                // Set the default phone in base class
                sProxyPhone = sProxyPhones[MSimConstants.DEFAULT_SUBSCRIPTION];
                sCommandsInterface = sCommandsInterfaces[MSimConstants.DEFAULT_SUBSCRIPTION];
                sMadeDefaults = true;
            }
        }
    }


    public static Phone getMSimCdmaPhone(int subscription) {
        Phone phone;
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            phone = new MSimCDMALTEPhone(sContext, sCommandsInterfaces[subscription],
                    sPhoneNotifier, subscription);
        }
        return phone;
    }

    public static Phone getMSimGsmPhone(int subscription) {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            Phone phone = new MSimGSMPhone(sContext, sCommandsInterfaces[subscription],
                    sPhoneNotifier, subscription);
            return phone;
        }
    }

    public static Phone getPhone(int subscription) {
        if (sLooper != Looper.myLooper()) {
            throw new RuntimeException(
                "MSimPhoneFactory.getPhone must be called from Looper thread");
        }
        if (!sMadeMultiSimDefaults) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            } else if (subscription == MSimConstants.DEFAULT_SUBSCRIPTION) {
                return sProxyPhone;
            }
        }
        return sProxyPhones[subscription];
    }

    // TODO: move the get/set subscription APIs to SubscriptionManager

    /* Sets the default subscription. If only one phone instance is active that
     * subscription is set as default subscription. If both phone instances
     * are active the first instance "0" is set as default subscription
     */
    public static void setDefaultSubscription(int subscription) {
        SystemProperties.set(PROPERTY_DEFAULT_SUBSCRIPTION, Integer.toString(subscription));

        // Set the default phone in base class
        if (subscription >= 0 && subscription < sProxyPhones.length) {
            sProxyPhone = sProxyPhones[subscription];
            sCommandsInterface = sCommandsInterfaces[subscription];
            sMadeDefaults = true;
        }

        // Broadcast an Intent for default sub change
        Intent intent = new Intent(TelephonyIntents.ACTION_DEFAULT_SUBSCRIPTION_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(MSimConstants.SUBSCRIPTION_KEY, subscription);
        Log.d(LOG_TAG, "setDefaultSubscription : " + subscription
                + " Broadcasting Default Subscription Changed...");
        sContext.sendStickyBroadcast(intent);
    }

    /* Gets the default subscription */
    public static int getDefaultSubscription() {
        return SystemProperties.getInt(PROPERTY_DEFAULT_SUBSCRIPTION, 0);
    }

    /* Gets User preferred Voice subscription setting*/
    public static int getVoiceSubscription() {
        int subscription = 0;

        try {
            subscription = Settings.System.getInt(sContext.getContentResolver(),
                    Settings.System.MULTI_SIM_VOICE_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(LOG_TAG, "Settings Exception Reading Dual Sim Voice Call Values");
        }

        // Set subscription to 0 if current subscription is invalid.
        // Ex: multisim.config property is TSTS and subscription is 2.
        // If user is trying to set multisim.config to DSDS and reboots
        // in this case index 2 is invalid so need to set to 0.
        if (subscription < 0 || subscription >= MSimTelephonyManager.getDefault().getPhoneCount()) {
            Log.i(LOG_TAG, "Subscription is invalid..." + subscription + " Set to 0");
            subscription = 0;
            setVoiceSubscription(subscription);
        }

        return subscription;
    }

    /* Returns User Prompt property,  enabed or not */
    public static boolean isPromptEnabled() {
        boolean prompt = false;
        int value = 0;
        try {
            value = Settings.System.getInt(sContext.getContentResolver(),
                    Settings.System.MULTI_SIM_VOICE_PROMPT);
        } catch (SettingNotFoundException snfe) {
            Log.e(LOG_TAG, "Settings Exception Reading Dual Sim Voice Prompt Values");
        }
        prompt = (value == 0) ? false : true ;
        Log.d(LOG_TAG, "Prompt option:" + prompt);

       return prompt;
    }

    /*Sets User Prompt property,  enabed or not */
    public static void setPromptEnabled(boolean enabled) {
        int value = (enabled == false) ? 0 : 1;
        Settings.System.putInt(sContext.getContentResolver(),
                Settings.System.MULTI_SIM_VOICE_PROMPT, value);
        Log.d(LOG_TAG, "setVoicePromptOption to " + enabled);
    }

    /* Gets User preferred Data subscription setting*/
    public static int getDataSubscription() {
        int subscription = 0;

        try {
            subscription = Settings.System.getInt(sContext.getContentResolver(),
                    Settings.System.MULTI_SIM_DATA_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(LOG_TAG, "Settings Exception Reading Dual Sim Data Call Values");
        }

        if (subscription < 0 || subscription >= MSimTelephonyManager.getDefault().getPhoneCount()) {
            Log.i(LOG_TAG, "Subscription is invalid..." + subscription + " Set to 0");
            subscription = 0;
            setDataSubscription(subscription);
        }

        return subscription;
    }

    /* Gets User preferred SMS subscription setting*/
    public static int getSMSSubscription() {
        int subscription = 0;
        try {
            subscription = Settings.System.getInt(sContext.getContentResolver(),
                    Settings.System.MULTI_SIM_SMS_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Values");
        }

        if (subscription < 0 || subscription >= MSimTelephonyManager.getDefault().getPhoneCount()) {
            Log.i(LOG_TAG, "Subscription is invalid..." + subscription + " Set to 0");
            subscription = 0;
            setSMSSubscription(subscription);
        }

        return subscription;
    }

    static public void setVoiceSubscription(int subscription) {
        Settings.System.putInt(sContext.getContentResolver(),
                Settings.System.MULTI_SIM_VOICE_CALL_SUBSCRIPTION, subscription);
        Log.d(LOG_TAG, "setVoiceSubscription : " + subscription);
    }

    static public void setDataSubscription(int subscription) {
        Settings.System.putInt(sContext.getContentResolver(),
                Settings.System.MULTI_SIM_DATA_CALL_SUBSCRIPTION, subscription);
        Log.d(LOG_TAG, "setDataSubscription: " + subscription);
    }

    static public void setSMSSubscription(int subscription) {
        Settings.System.putInt(sContext.getContentResolver(),
                Settings.System.MULTI_SIM_SMS_SUBSCRIPTION, subscription);

        Intent intent = new Intent("com.android.mms.transaction.SEND_MESSAGE");
        sContext.sendBroadcast(intent);
        Log.d(LOG_TAG, "setSMSSubscription : " + subscription);
    }
}
