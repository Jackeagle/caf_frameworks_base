/*
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

import android.app.ActivityManagerNative;
import android.content.Context;
import android.provider.Settings;
import android.util.Config;
import android.util.Log;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.UiccManager;
import com.android.internal.telephony.UiccCard;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.UiccConstants.CardState;
import com.android.internal.telephony.IccPhoneBookInterfaceManagerProxy;
import com.android.internal.telephony.IccSmsInterfaceManager;
import com.android.internal.telephony.PhoneSubInfoProxy;
import com.android.internal.telephony.UiccConstants.AppType;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.TelephonyIntents;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.os.Registrant;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import java.util.regex.PatternSyntaxException;
import java.lang.Exception;

public class ProxyManager extends Handler {
    static final String LOG_TAG = "PROXY";

    // Subscription activation status
    public static final int SUB_ACTIVATE = 0;
    public static final int SUB_DEACTIVATE = 1;
    public static final int SUB_ACTIVATING = 2;
    public static final int SUB_DEACTIVATING = 3;
    public static final int SUB_ACTIVATED = 4;
    public static final int SUB_DEACTIVATED = 5;
    public static final int SUB_INVALID = 6;

    static final int NUM_SUBSCRIPTIONS = 2;
    static final int EVENT_SET_UICC_SUBSCRIPTION_DONE = 1;
    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 2;
    static final int EVENT_ICC_CHANGED = 3;
    static final int EVENT_SUBSCRIPTION_SET_DONE = 4;
    static final int EVENT_GET_ICC_STATUS_DONE = 5;
    static final int EVENT_SET_SUBSCRIPTION_MODE_DONE = 6;
    static final int EVENT_DISABLE_DATA_CONNECTION_DONE = 7;
    static final int EVENT_GET_ICCID_DONE = 8;

    static public final String INTENT_VALUE_SUBSCR_INFO_1 = "SUBSCR INFO 01";
    static public final String INTENT_VALUE_SUBSCR_INFO_2 = "SUBSCR INFO 02";


    // Class Variables
    static private Phone sProxyPhone[] = null;      //phoneproxy instances
    String [] userDefaultSubs = {"USIM", "USIM"}; //user default subscriptions
    String [] userPrefSubs = null;                //user chosen subscriptions

    private static CommandsInterface[] mCi;
    private IccSmsInterfaceManager mIccSmsInterfaceManager; //IccSmsInterfaceManager which arbitrates the SMS dispatch
    private IccPhoneBookInterfaceManagerProxy mIccPhoneBookInterfaceManagerProxy; //IccPhoneBookInterfaceManagerProxy to handle
                                                                                  //"simphonebook"
    private PhoneSubInfoProxy mPhoneSubInfoProxy;
    private UiccManager mUiccManager;
    private UiccCard[] mUiccCards;
    private static Context  mContext;
    static ProxyManager mProxyManager;
    private boolean mUiccSubSet = false;
    private boolean mDdsSet = false;
    SupplySubscription supplySubscription;
    PhoneAppBroadcastReceiver mReceiver;

    static int queuedDds;
    static int currentDds;
    static boolean disableDdsInProgress = false;
    static Message mSetDdsCompleteMsg;
    static private int defaultSubscription = 0;
    static private int voiceSubscription = 0;
    static private int dataSubscription = 0;
    static private int smsSubscription = 0;

    private int mPendingIccidRequest = 0;
    private boolean setSubscriptionMode = true;
    private boolean mReadIccid = true;
    private String[] mIccIds;
    // The subscription information of all the cards
    SubscriptionData[] mCardSubData = null;
    // The User prefered subscription information
    SubscriptionData mUserPrefSubs = null;


    //***** Class Methods
    public static ProxyManager getInstance(Context context, Phone[] phoneProxy, UiccManager uiccmgr, CommandsInterface[] ci)
    {
        Log.d(LOG_TAG, "In ProxyManager getInstance");
        if (mProxyManager == null) {
            mProxyManager = new ProxyManager(context, phoneProxy, uiccmgr, ci);
        }
        return mProxyManager;
    }
    static public ProxyManager getInstance() {

        return mProxyManager;
    }

    private ProxyManager() {
    }

    private ProxyManager(Context context, Phone[] phoneProxy, UiccManager uiccManager, CommandsInterface[] ci) {

        Log.d(LOG_TAG, "Creating ProxyManager");
        mContext = context;
        sProxyPhone = phoneProxy;
        getDefaultProperties(context);

        if (TelephonyManager.isDsdsEnabled()) {
            mIccSmsInterfaceManager = new IccSmsInterfaceManager(sProxyPhone,ci);
            mIccPhoneBookInterfaceManagerProxy = new IccPhoneBookInterfaceManagerProxy(sProxyPhone);
            mPhoneSubInfoProxy = new PhoneSubInfoProxy(sProxyPhone);
            mUiccManager = uiccManager;
            mUiccCards = new UiccCard[UiccConstants.RIL_MAX_CARDS];
            mCi = ci;

            mUiccManager.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
            getUserPreferredSubs();
            supplySubscription = this.new SupplySubscription(mContext);

            // Register for intent broadcasts.
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            mReceiver = new PhoneAppBroadcastReceiver();
            mContext.registerReceiver(mReceiver, intentFilter);
            intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            mContext.registerReceiver(mReceiver, intentFilter);

            // get the current active dds
            currentDds = PhoneFactory.getDataSubscription(context);
            Log.d(LOG_TAG, "In ProxyManager constructor current active dds is:" + currentDds);
        }
    }

    /*
     *  This function will read from the User Prefered Subscription from the
     *  system property, parse and populate the member variable mUserPrefSubs.
     *  User Prefered Subscription is stored in the system property string as
     *    iccId+appType+appId+activationStatus+subIndex
     *  If the the property is not set already, then set it to the default values
     *  for appType to USIM and activationStatus to ACTIVATED.
     */
    public void getUserPreferredSubs() {
        boolean errorOnParsing = false;

        mUserPrefSubs = new SubscriptionData(NUM_SUBSCRIPTIONS);

        for(int i = 0; i < NUM_SUBSCRIPTIONS; i++) {

            String strUserSub = Settings.System.getString(mContext.getContentResolver(),
                                                   Settings.System.USER_PREFERRED_SUBS[i]);
            if (strUserSub != null) {
                Log.d(LOG_TAG, "getUserPreferredSubs: strUserSub = " + strUserSub);

                try {
                    String splitUserSub[] = strUserSub.split(",");

                    // There should be 5 fields in the user prefered settings.
                    if (splitUserSub.length == 5) {
                        mUserPrefSubs.subscription[i].iccId = getStringFrom(splitUserSub[0]);
                        mUserPrefSubs.subscription[i].appType = getStringFrom(splitUserSub[1]);
                        mUserPrefSubs.subscription[i].appId = getStringFrom(splitUserSub[2]);

                        try {
                            mUserPrefSubs.subscription[i].subStatus = Integer.parseInt(splitUserSub[3]);
                        } catch (NumberFormatException ex) {
                            Log.e(LOG_TAG, "getUserPreferredSubs: NumberFormatException: " + ex);
                            mUserPrefSubs.subscription[i].subStatus = SUB_INVALID;
                        }

                        try {
                            mUserPrefSubs.subscription[i].subIndex = Integer.parseInt(splitUserSub[4]);
                        } catch (NumberFormatException ex) {
                            Log.e(LOG_TAG, "getUserPreferredSubs: NumberFormatException: " + ex);
                            mUserPrefSubs.subscription[i].subIndex = -1;
                        }
                    } else {
                        Log.e(LOG_TAG, "getUserPreferredSubs: splitUserSub.length != 5");
                        errorOnParsing = true;
                    }
                } catch (PatternSyntaxException pe) {
                    Log.e(LOG_TAG, "getUserPreferredSubs: PatternSyntaxException while split : " + pe);
                    errorOnParsing = true;

                }
            }

            if (strUserSub == null || errorOnParsing) {
                String defaultUserSub = " " + "," +                        // iccId
                                        userDefaultSubs[i] + "," +         // app type
                                        " " + "," +                        // app id
                                        Integer.toString(SUB_ACTIVATED)+   // activate state
                                        ",-1";                             // sub index in the card

                Settings.System.putString(mContext.getContentResolver(),
                            Settings.System.USER_PREFERRED_SUBS[i], defaultUserSub);

                mUserPrefSubs.subscription[i].iccId = null;
                mUserPrefSubs.subscription[i].appType = userDefaultSubs[i];
                mUserPrefSubs.subscription[i].appId = null;
                mUserPrefSubs.subscription[i].subStatus = SUB_ACTIVATED;
                mUserPrefSubs.subscription[i].subIndex = -1;
            }

            mUserPrefSubs.subscription[i].subNum = i;

            Log.d(LOG_TAG, "getUserPreferredSubs: mUserPrefSubs.subscription[" + i + "] = "
                           + mUserPrefSubs.subscription[i].toString());
        }
    }

    public void saveUserPreferedSubscription(SubscriptionData userPrefSubData) {
        Subscription userPrefSub;
        String userSub;

        // Update the user prefered sub
        mUserPrefSubs.copyFrom(userPrefSubData);

        for (int index = 0; index < userPrefSubData.numSubscriptions; index++) {
            userPrefSub = userPrefSubData.subscription[index];

            userSub = ((userPrefSub.iccId != null) ? userPrefSub.iccId : " ") + "," +
                      ((userPrefSub.appType != null) ? userPrefSub.appType : " ") + "," +
                      ((userPrefSub.appId != null) ? userPrefSub.appId : " ") + "," +
                      Integer.toString(userPrefSub.subStatus) + "," +
                      Integer.toString(userPrefSub.subIndex);

            Log.d(LOG_TAG, "saveUserPreferedSubscription: userPrefSub = " + userPrefSub.toString());
            Log.d(LOG_TAG, "saveUserPreferedSubscription: userSub = " + userSub);

            // Construct the string and store in Settings data base at index.
            //update the user pref settings so that next time user is not prompted of the subscriptions
            Settings.System.putString(mContext.getContentResolver(),Settings.System.USER_PREFERRED_SUBS[index],
                                  userSub);
        }
    }

    String getStringFrom(String str) {
        if ((str == null) || (str != null && str.equals(" "))) {
            return null;
        }
        return str;
    }


    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        Message     onComplete;
        int size;
        int cardIndex = 0;
        String strCardIndex;

        switch(msg.what) {
            case EVENT_ICC_CHANGED: {
                Log.d(LOG_TAG, "ProxyManager EVENT_ICC_CHANGED");
                for (int i = 0; i < UiccConstants.RIL_MAX_CARDS; i++) {
                    mUiccCards[i] = mUiccManager.getCard(i);
                }
                if (!mUiccSubSet) {
                    checkCardStatus();
                }
                break;
            }

            case EVENT_GET_ICCID_DONE:
                Log.d(LOG_TAG, "ProxyManager EVENT_READ_ICCID_DONE");
                ar = (AsyncResult)msg.obj;
                byte []data = (byte[])ar.result;
                cardIndex = 0;
                strCardIndex = (String) ar.userObj;
                if (strCardIndex != null) {
                    cardIndex = Integer.parseInt(strCardIndex);
                    Log.d(LOG_TAG, "parsed cardIndex: " + cardIndex);
                }

                if (ar.exception != null) {
                    Log.d(LOG_TAG, "Exception in GET ICCID");
                    mIccIds[cardIndex] = null;
                } else {
                    mIccIds[cardIndex] = IccUtils.bcdToString(data, 0, data.length);
                }
                Log.d(LOG_TAG, "GET ICCID DONE.. mIccIds[" + cardIndex + "] = "
                               + mIccIds[cardIndex]);
                mPendingIccidRequest--;
                if (mPendingIccidRequest <= 0) {
                    processCardStatus();
                }
                break;

            case EVENT_DISABLE_DATA_CONNECTION_DONE:
                Log.d(LOG_TAG, "EVENT_DISABLE_DATA_CONNECTION_DONE, disableDdsInProgress = "
                               + disableDdsInProgress);
                if (disableDdsInProgress) {
                    // Set the DDS in cmd interface
                    String str = Integer.toString(queuedDds);
                    Message callback = Message.obtain(this, EVENT_SET_DATA_SUBSCRIPTION_DONE, str);
                    Log.d(LOG_TAG, "Set DDS to " + queuedDds
                          + " Calling cmd interface setDataSubscription");
                    try {
                        Thread.sleep(15000);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception occured in sleep!");
                    }
                    mCi[queuedDds].setDataSubscription(callback);
                }
                break;

            case EVENT_SET_DATA_SUBSCRIPTION_DONE:
                Log.d(LOG_TAG, "EVENT_SET_DATA_SUBSCRIPTION_DONE");

                ar = (AsyncResult) msg.obj;

                SetDdsResult result = SetDdsResult.ERR_GENERIC_FAILURE;

                //if SUCCESS
                if (ar.exception == null) {
                    // Mark this as the current dds
                    PhoneFactory.setDataSubscription(queuedDds);
                    currentDds = queuedDds;

                    if (disableDdsInProgress) {
                        // Enable the data connectivity on new dds.
                        Log.d(LOG_TAG, "  Enable Data Connectivity on Subscription " + currentDds);
                        sProxyPhone[currentDds]
                               .enableDataConnectivity();
                    }
                    result = SetDdsResult.SUCCESS;
                    Log.d(LOG_TAG, "  setDataSubscriptionSource is Successful");

                    //Subscription is changed to this new sub, need to update the DB to mark
                    //the respective profiles as "current".
                    sProxyPhone[currentDds].updateCurrentCarrierInProvider();

                } else {
                    // error
                    if (ar.exception instanceof CommandException
                            && ((CommandException) (ar.exception)).getCommandError()
                            == CommandException.Error.RADIO_NOT_AVAILABLE) {
                        result = SetDdsResult.ERR_RADIO_NOT_AVAILABLE;
                    } else if (ar.exception instanceof CommandException
                            && ((CommandException) (ar.exception)).getCommandError()
                            == CommandException.Error.GENERIC_FAILURE) {
                        result = SetDdsResult.ERR_GENERIC_FAILURE;
                    } else if (ar.exception instanceof CommandException
                            && ((CommandException) (ar.exception)).getCommandError()
                            == CommandException.Error.SUBSCRIPTION_NOT_AVAILABLE) {
                        result = SetDdsResult.ERR_SUBSCRIPTION_NOT_AVAILABLE;
                    }
                    Log.d(LOG_TAG, "setDataSubscriptionSource Failed : " + result);
                }

                // Reset the flag.
                disableDdsInProgress = false;

                // Send the message back to callee with result.
                if (mSetDdsCompleteMsg != null) {
                    AsyncResult.forMessage(mSetDdsCompleteMsg, result, null);
                    Log.d(LOG_TAG, "Enable Data Connectivity Done!! Sending the cnf back!");
                    mSetDdsCompleteMsg.sendToTarget();
                    mSetDdsCompleteMsg = null;
                }
                break;
        }
    }

    void getCardIccids() {
        // get the iccid from the cards present.
        mIccIds = new String[UiccConstants.RIL_MAX_CARDS];

        for (int cardIndex = 0; cardIndex < UiccConstants.RIL_MAX_CARDS; cardIndex++) {
            mIccIds[cardIndex] = null;
            if (mUiccCards[cardIndex].getCardState() == CardState.PRESENT) {
                Log.d(LOG_TAG, "get ICCID for card : " + cardIndex);
                String strCardIndex = Integer.toString(cardIndex);

                Message response = obtainMessage(EVENT_GET_ICCID_DONE, strCardIndex);

                UiccCardApplication cardApp = mUiccCards[cardIndex].getUiccCardApplication(0);
                if (cardApp != null) {
                    IccFileHandler fileHandler = cardApp.getIccFileHandler();
                    if (fileHandler != null) {
                        fileHandler.loadEFTransparent(IccConstants.EF_ICCID, response);
                        mPendingIccidRequest++;
                    }
                }
            }
        }
    }

    public void checkCardStatus() {
        UiccCard card1 = mUiccCards[0];
        UiccCard card2 = mUiccCards[1];

        if (card1 != null && card2 != null) {
            Log.d(LOG_TAG, ":  card 1 state: "+card1.getCardState());
            Log.d(LOG_TAG, ":  card 2 state: "+card2.getCardState());

            /* Card status to be processed if
              card1 status is present and card2 status is present
              card1 status is present and card2 status is absent
              card1 status is absent and card2 status is present
            */
            if ((!(card1.getCardState() == CardState.ABSENT &&
                   card2.getCardState() == CardState.ABSENT)) &&
                (!(card1.getCardState() == CardState.ERROR &&
                   card2.getCardState() == CardState.ERROR))) {
                // Get the Iccid and then process the cards
                if (mReadIccid) {
                    mReadIccid = false;
                    getCardIccids();
                }
            }
        }
    }

    void processCardStatus() {
        int numApps = 0;

        mCardSubData = new SubscriptionData[UiccConstants.RIL_MAX_CARDS];

        // Loop through list of cards and list of applications and store it in the mCardSubData
        for (int cardIndex = 0; cardIndex < UiccConstants.RIL_MAX_CARDS; cardIndex++) {
            CardState cardstate = mUiccCards[cardIndex].getCardState();
            Log.d(LOG_TAG, "cardIndex = " + cardIndex + " cardstate = " + cardstate);

            if (cardstate == CardState.PRESENT) {
                numApps = mUiccCards[cardIndex].getNumApplications();
                Log.d(LOG_TAG, "num of apps : " + numApps);

                mCardSubData[cardIndex] = new SubscriptionData(numApps);

                for (int appIndex = 0; appIndex < numApps ; appIndex++ ) {
                    Log.d(LOG_TAG, ":  appIndex : "+ appIndex);

                    Subscription cardSub = mCardSubData[cardIndex].subscription[appIndex];

                    UiccCardApplication uiccCardApplication = mUiccCards[cardIndex]
                                                                .getUiccCardApplication(appIndex);

                    cardSub.slotId = cardIndex;
                    cardSub.subIndex = appIndex;
                    cardSub.subNum = -1;               // Not set the sub id
                    cardSub.subStatus = SUB_INVALID;
                    cardSub.appId = uiccCardApplication.getAid();
                    cardSub.appLabel = uiccCardApplication.getAppLabel();
                    cardSub.iccId = mIccIds[cardIndex];

                    AppType type = uiccCardApplication.getType();
                    String subAppType = toString(type);
                    //Apps like ISIM etc are treated as UNKNOWN apps, to be discarded
                    if (!subAppType.equals("UNKNOWN")) {
                        Log.d(LOG_TAG, "app type: "+ subAppType);
                        cardSub.appType = subAppType;
                    } else {
                        cardSub.appType = null;
                        Log.d(LOG_TAG, "UNKNOWN APP");
                    }

                    Log.d(LOG_TAG, "mCardSubData[" + cardIndex + "].subscription[" + appIndex +
                                   "] = " + cardSub.toString());
                }
            }
        }

        matchSubscriptions();
    }

    public String toString(AppType p) {
        switch(p) {
            case APPTYPE_UNKNOWN:
                    {return "UNKNOWN";}
            case APPTYPE_SIM:
                    {return "SIM"; }
            case APPTYPE_USIM:
                    {return "USIM";}
            case APPTYPE_RUIM:
                    {return "RUIM";}
            case APPTYPE_CSIM:
                    {return "CSIM";}
            default:
                    {return "UNKNOWN";}
        }
    }

    int getLength(String [] string) {
        int subLength = 0;
        for(int i = 0; i < string.length; i++) {
            if (string[i] != null) {
                subLength++;
            }
        }
        Log.d(LOG_TAG, ":  string length: "+subLength);
        return subLength;
    }

    /*
     * Compare each of the user pref sub with the Subscriptions in each of the card.
     * If all of the user pref subscriptions, which were activated on the last session,
     * matches the subscriptions(applications) in the card then automatically set the
     * subscription.  Otherwise prompt the User to select subscriptions.
     */
    public void matchSubscriptions() {
        int cardIndex = 0;
        int num_cards = 0;
        SubscriptionData matchedSub = new SubscriptionData(NUM_SUBSCRIPTIONS);

        Log.d(LOG_TAG, "matchSubscriptions");

        // For each subscription in mUserPrefSubs
        for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
            Subscription userSub = mUserPrefSubs.subscription[i];
            Log.d(LOG_TAG, "subNum: " + i);

            // For each cards in mCardSubData
            for (cardIndex = 0; cardIndex < UiccConstants.RIL_MAX_CARDS; cardIndex++) {
                Log.d(LOG_TAG, "cardIndex: " + cardIndex + " userSub.subIndex: "
                               + userSub.subIndex);

                if ((userSub.subIndex != -1) &&
                    (mCardSubData[cardIndex] != null) &&
                    (userSub.subIndex < mCardSubData[cardIndex].numSubscriptions)) {
                    Subscription cardSub = mCardSubData[cardIndex].subscription[userSub.subIndex];

                    // Check for the iccid, app id, app name
                    if (((userSub.iccId == null && cardSub.iccId == null) ||
                            (userSub.iccId != null && userSub.iccId.equals(cardSub.iccId))) &&
                        ((userSub.appId == null && cardSub.appId == null) ||
                            (userSub.appId != null && userSub.appId.equals(cardSub.appId))) &&
                        ((userSub.appType == null && cardSub.appType == null) ||
                            (userSub.appType != null && userSub.appType.equals(cardSub.appType)))) {

                        // Update the matched subscription
                        matchedSub.subscription[i].copyFrom(userSub);
                        // Set the activate state
                        matchedSub.subscription[i].subStatus = SUB_ACTIVATE;
                        matchedSub.subscription[i].subNum = i;
                        // Set the slot id, sub index from mCardSubData
                        matchedSub.subscription[i].slotId = cardSub.slotId;
                        matchedSub.subscription[i].subIndex = cardSub.subIndex;

                        Log.d(LOG_TAG, "Subscription is matched for UserPrefSub subNum = " + i +
                                       " cardIndex = " + cardIndex + " subIndex = " + userSub.subIndex);
                        break;
                    }
                }
                Log.d(LOG_TAG, "Not matched for UserPrefSub subNum: " + i +
                     " userSub.subIndex : " + userSub.subIndex + " cardIndex : " + cardIndex);
            }
            Log.d(LOG_TAG, "matchedSub.subscription[" + i + "] = " + matchedSub.subscription[i].toString());
        }

        // If the user pref sub is not matched, then propmt the user to select the subs.
        if ((mUserPrefSubs.subscription[0].subStatus == SUB_ACTIVATED &&
             matchedSub.subscription[0].subNum == -1) ||
            (mUserPrefSubs.subscription[1].subStatus == SUB_ACTIVATED &&
             matchedSub.subscription[1].subNum == -1)) {

            //Subscription settings do not match with the card applications
            mUiccSubSet = true;  //card status is processed only the first time
            promptUserSubscription();
        } else {
            // No need to wait for the set subscription to be done in case of
            // auto set subscription.
            setSubscription(matchedSub, true);
            mUiccSubSet = true;
        }
    }

    public String[] setSubscription(SubscriptionData subData, boolean auto) {

        Log.d(LOG_TAG, "setSubscription");

        for (int i = 0; i < subData.numSubscriptions ; i++) {
            Log.d(LOG_TAG, "subData.subscription[" + i + "] = " + subData.subscription[i].toString());

            if ((subData.subscription[i].slotId != -1) &&
                (subData.subscription[i].subIndex != -1)) {

                SubscriptionData cardSubData = mCardSubData[subData.subscription[i].slotId];
                Subscription cardSub = cardSubData.subscription[subData.subscription[i].subIndex];

                if (((cardSub.appType.equals("SIM")) ||
                    (cardSub.appType.equals("USIM"))) &&
                    (!sProxyPhone[i].getPhoneName().equals("GSM"))) {

                    Log.d(LOG_TAG, "gets New GSM phone" );
                    sProxyPhone[i].updatePhoneProxy(PhoneFactory.getGsmPhone(i));
                    mIccSmsInterfaceManager.updatePhoneObject(sProxyPhone[i].getVoicePhone(),i);
                    //when the phone object is re-created broadcast an intent so that PhoneApp is aware of it
                    Intent intent = new Intent(TelephonyIntents.ACTION_PHONE_CHANGED);
                    intent.putExtra(Phone.PHONE_SUBSCRIPTION, i);
                    ActivityManagerNative.broadcastStickyIntent(intent, null);

                } else if (((cardSub.appType.equals("RUIM")) ||
                          (cardSub.appType.equals("CSIM"))) &&
                          (!sProxyPhone[i].getPhoneName().equals("CDMA")) ) {

                     Log.d(LOG_TAG, "gets New CDMA phone" );
                     sProxyPhone[i].updatePhoneProxy(PhoneFactory.getCdmaPhone(i));
                     mIccSmsInterfaceManager.updatePhoneObject(sProxyPhone[i].getVoicePhone(),i);
                     //when the phone object is re-created broadcast an intent so that PhoneApp is aware of it
                     Intent intent = new Intent(TelephonyIntents.ACTION_PHONE_CHANGED);
                     intent.putExtra(Phone.PHONE_SUBSCRIPTION, i);
                     ActivityManagerNative.broadcastStickyIntent(intent, null);

                }
            }
        }

        mUiccSubSet = true;

        // Setting the subscription at RIL/Modem is handled through a thread. Start the thread
        // if it is not already started.
        if (!supplySubscription.isAlive()) {
            supplySubscription.start();
        }

        return supplySubscription.setSubscription(subData, auto);
    }

    public void promptUserSubscription() {
        // Send an intent to start SetSubscription.java activity to show the list of apps
        // on each card for user selection
        Log.d(LOG_TAG, "promptUserSubscription" );
        Intent setSubscriptionIntent = new Intent(Intent.ACTION_MAIN);
        setSubscriptionIntent.setClassName("com.android.phone",
                "com.android.phone.SetSubscription");
        setSubscriptionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        setSubscriptionIntent.putExtra("CONFIG_SUB", false);

        mContext.startActivity(setSubscriptionIntent);
    }

    public class SupplySubscription extends Thread {

        private boolean mDone = false;
        public Handler mHandler;
        private int eventsPending = 0;
        private Context  mContext;
        private String [] subResult;
        boolean isStarted = false;

        private static final int SUBSCRIPTION_SET_SUCCESS = 0;
        private static final int SUBSCRIPTION_SET_FAILED = 1;

        SubscriptionData subscriptionData;
        SubscriptionData prevSubscriptionData;

        public SupplySubscription(Context context) {

            mContext = context;
            eventsPending = 0;
            subResult = new String[NUM_SUBSCRIPTIONS];

            subscriptionData = new SubscriptionData(NUM_SUBSCRIPTIONS);
            prevSubscriptionData = new SubscriptionData(NUM_SUBSCRIPTIONS);
        }


        @Override
        public void run() {
            Looper.prepare();
            synchronized (SupplySubscription.this) {
                mHandler = new Handler() {

                    @Override
                    public void handleMessage(Message msg) {

                        int phoneIndex = 0;
                        AsyncResult ar = (AsyncResult) msg.obj;
                        String string = (String) ar.userObj;
                        if (string != null) {
                            phoneIndex = Integer.parseInt(string);
                            Log.d(LOG_TAG, ": parsed i:"+phoneIndex);
                        }

                        switch (msg.what) {
                            case EVENT_SET_SUBSCRIPTION_MODE_DONE:{
                                // Event received when SUBSCRIPTION_MODE is set at Modem SingleStandBy/DualStandBy
                                Log.d(LOG_TAG, "EVENT_SET_SUBSCRIPTION_MODE_DONE:");

                                for (int index = 0; index < subscriptionData.numSubscriptions; index++) {
                                    if (subscriptionData.subscription[index].slotId != -1 &&
                                        subscriptionData.subscription[index].subIndex != -1) {
                                        String str = Integer.toString(index);
                                        Message callback = Message.obtain(mHandler, EVENT_SET_UICC_SUBSCRIPTION_DONE, str);
                                        Log.d(LOG_TAG, ":  calling cmd interface setSubscription.....");
                                        eventsPending++;
                                        mCi[index].setUiccSubscription(subscriptionData.subscription[index], callback );
                                    }
                                 }
                                 break;
                            }

                            case EVENT_SET_UICC_SUBSCRIPTION_DONE: {
                                // Event received when SET_SUBSCRIPTION is set at RIL
                                Log.d(LOG_TAG, "EVENT_SET_UICC_SUBSCRIPTION_DONE: on sub: "+phoneIndex);
                                synchronized (SupplySubscription.this) {
                                    eventsPending--;
                                    if ( ar.exception != null ) {
                                        if (ar.exception instanceof CommandException ) {
                                            CommandException.Error error =  ((CommandException) (ar.exception)).getCommandError();
                                            if (error == null)
                                                subResult[phoneIndex] = "FAILED";
                                            else {
                                                if (error == CommandException.Error.RADIO_NOT_AVAILABLE ||
                                                    error == CommandException.Error.GENERIC_FAILURE ||
                                                    error ==  CommandException.Error.SUBSCRIPTION_NOT_AVAILABLE) {
                                                    subResult[phoneIndex] = "FAILED";
                                                } else if ( error ==  CommandException.Error.SUBSCRIPTION_NOT_SUPPORTED ) {
                                                    subResult[phoneIndex] = "Not Supported";
                                                }
                                            }

                                       }
                                    } else {
                                        subResult[phoneIndex] = "SUCCESS";
                                        subscriptionData.subscription[phoneIndex].subStatus = SUB_ACTIVATED;
                                        //set subscription success, update subscription info in phone objects
                                        sProxyPhone[phoneIndex].setSubscriptionInfo(subscriptionData.subscription[phoneIndex]);
                                        Log.d(LOG_TAG, "EVENT_SET_UICC_SUBSCRIPTION_DONE success, phone index=" + phoneIndex) ;
                                    }

                                    if (eventsPending == 0) {
                                        // All the pending subscription_set responses are received now we can unblock the thread
                                        // so that control can go to UI to finish the activity.
                                        mDone = true;

                                        // Store the User prefered Subscription once all
                                        // the set uicc subscription is done.
                                        saveUserPreferedSubscription(subscriptionData);
                                        prevSubscriptionData.copyFrom(subscriptionData);

                                        SupplySubscription.this.notifyAll();
                                    }

                                    // Disable the data connectivity if the phone is not the Designated Data Subscription
                                    // Data should be active only on DDS.
                                    Phone currentPhone = sProxyPhone[phoneIndex];
                                    currentDds = PhoneFactory.getDataSubscription(mContext);
                                    Log.d(LOG_TAG, " SET_UICC_SUBSCRIPTION_DONE : currentDds = " + currentDds +
                                          " currentPhone.getSubscription() = " + currentPhone.getSubscription());
                                    if (currentPhone.getSubscription() != currentDds) {
                                        Log.d(LOG_TAG, "Disabling the Data Connectivity on " +
                                             currentPhone.getSubscription() + ". This is not the DDS");
                                        currentPhone.disableDataConnectivity();
                                    } else {
                                        Log.d(LOG_TAG, "Active DDS : " + currentPhone.getSubscription());
                                    }
                                }
                                break;
                            }

                            case EVENT_SET_DATA_SUBSCRIPTION_DONE: {
                                //Data subscription preference is set at RIL
                                Log.d(LOG_TAG, "EVENT_SET_DATA_SUBSCRIPTION_DONE: on sub:"+phoneIndex);
                                break;
                            }
                        }
                    }
                };
                SupplySubscription.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized String[] setSubscription(SubscriptionData userSubData, boolean auto) {

            String [] string = null;
            subResult[0] = null;
            subResult[1] = null;

            Log.d(LOG_TAG, "In setSubscription");
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            Log.d(LOG_TAG, "Copying the subscriptionData from the userSubData");
            subscriptionData.copyFrom(userSubData);
            Log.d(LOG_TAG, "subscriptionData.numSubscriptions :"+subscriptionData.numSubscriptions);

            if (!setSubscriptionMode) {
                // Workarround for activate/deactivate subscription from Dual SIM Settings menu.
                // When user try to change the subscription from menu, return with success and
                // store it in the user prefered subscription property and will be used in the
                // next power cycle to set the subscriptions.
                // Once QCRIL supports the activate/deactivate subscriptions, need to remove
                // this and call the set uicc subscription accordingly.
                // User need to *reboot manually* to reflect the changes made.
                for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
                    subResult[i] = "SUCCESS";
                    // Set the activation status as ACTIVATED, to activate this
                    // subscription when user *reboots*.
                    if (subscriptionData.subscription[i].slotId != -1 &&
                        subscriptionData.subscription[i].subIndex != -1) {
                        subscriptionData.subscription[i].subStatus = SUB_ACTIVATED;
                    } else if (subscriptionData.subscription[i].slotId == -1 &&
                        subscriptionData.subscription[i].subIndex == -1) {
                        subscriptionData.subscription[i].subStatus = SUB_DEACTIVATED;
                    }
                }
                mDone = true;
                // Store the User Pref Subscriptions
                saveUserPreferedSubscription(subscriptionData);
                prevSubscriptionData.copyFrom(subscriptionData);
            } else {
                Message callback = Message.obtain(mHandler, EVENT_SET_SUBSCRIPTION_MODE_DONE, null);
                mCi[0].setSubscriptionMode(subscriptionData.numSubscriptions, callback);
                setSubscriptionMode = false;
                mDone = false;
            }

            if (!auto) {
                while (!mDone) {
                    try {
                        wait();
                        Log.d(LOG_TAG, "waiting subscription done");
                    } catch (InterruptedException e) {
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                    }
                }
            }

            Log.d(LOG_TAG, "setSubscription DONE!!");
            return subResult;
        }
    }

   private class PhoneAppBroadcastReceiver extends BroadcastReceiver {

       @Override
       public void onReceive(Context context, Intent intent) {
           String action = intent.getAction();
            Log.v(LOG_TAG,"Action intent recieved");
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                //When airplane mode is enabled/disabled from settings
                boolean enabled = intent.getBooleanExtra("state",false);
                if (enabled) {
                    //unregistering for card status indication when moved to airplane mode
                    mUiccManager.unregisterForIccChanged(mProxyManager);
                    mUiccSubSet = true; //flag which takes care of processing/Handling of card status
                                                  //card status is processed only the first time
                    setSubscriptionMode = true;
                } else {
                    //registering for card status indication when moved out of airplane mode to get card status once again
                    mUiccManager.registerForIccChanged(mProxyManager, EVENT_ICC_CHANGED, null);
                    mUiccSubSet = false;
                    mReadIccid = true;
                    mDdsSet = false;
                }
            } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                if (!mDdsSet) {
                    int subscription = intent.getIntExtra(IccCard.INTENT_KEY_SUBSCRIPTION, 0);
                    String state = intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE);
                    // Set Data Subscription Source only when App state becomes READY.
                    if (IccCard.INTENT_VALUE_ICC_READY.equals(state)) {
                        if (subscription == currentDds) {
                            String str = Integer.toString(currentDds);
                            Message callback = Message.obtain(supplySubscription.mHandler, EVENT_SET_DATA_SUBSCRIPTION_DONE, str);
                            // Set Data Subscription preference at RIL
                            Log.d(LOG_TAG, "setDataSubscription on " + currentDds);
                            mCi[currentDds].setDataSubscription(callback);
                            mDdsSet = true;
                        }
                    }
                }
            }
        }
   }

   public void setDataSubscription(int subscription, Message onCompleteMsg) {
       Log.d(LOG_TAG, " setDataSubscription: currentDds = "
             + currentDds + " new subscription = " + subscription);

        mSetDdsCompleteMsg = onCompleteMsg;

        // If there is no set dds in progress disable the current
        // active dds. Once all data connections is teared down, the data
        // connections on queuedDds will be enabled.
        // Call the PhoneFactory setDataSubscription API only after disconnecting
        // the current dds.
        queuedDds = subscription;
        if (!disableDdsInProgress) {
            Message allDataDisabledMsg = obtainMessage(EVENT_DISABLE_DATA_CONNECTION_DONE);
            sProxyPhone[currentDds].disableDataConnectivity(allDataDisabledMsg);
            disableDdsInProgress = true;
        }
    }

    public SubscriptionData[] getCardSubscriptions() {
        return mCardSubData;
    }

    public SubscriptionData getCurrentSubscriptions() {
        return supplySubscription.subscriptionData;
    }

    /* Result of the setDataSubscription */
    public enum SetDdsResult {
        ERR_RADIO_NOT_AVAILABLE,
        ERR_GENERIC_FAILURE,
        ERR_SUBSCRIPTION_NOT_AVAILABLE,
        SUCCESS;

        @Override
        public String toString() {
            switch (this) {
                case ERR_RADIO_NOT_AVAILABLE: return "Radio Not Available";
                case ERR_GENERIC_FAILURE: return "Generic Failure";
                case ERR_SUBSCRIPTION_NOT_AVAILABLE: return "Subscription Not Available";
                case SUCCESS: return "SUCCESS";
                default: return "unknown";
            }
        }
    }

    /* Gets the default subscriptions for VOICE/SMS/DATA */
    public static void getDefaultProperties(Context context) {
        try{
            voiceSubscription = Settings.System.getInt(context.getContentResolver(),Settings.System.DUAL_SIM_VOICE_CALL);
            dataSubscription = Settings.System.getInt(context.getContentResolver(),Settings.System.DUAL_SIM_DATA_CALL);
            smsSubscription = Settings.System.getInt(context.getContentResolver(),Settings.System.DUAL_SIM_SMS);

            Log.d(LOG_TAG,"Dual Sim Settings from Settings Provider -----");
            Log.d(LOG_TAG,"Voice_val = " + voiceSubscription + " Data_val = " + dataSubscription + " Sms_val = " + smsSubscription);

        } catch (SettingNotFoundException snfe) {
            Log.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Call Values", snfe);
            /* Incase of exception setting default values to 0 */
            Settings.System.putInt(context.getContentResolver(),Settings.System.DUAL_SIM_VOICE_CALL, 0);
            Settings.System.putInt(context.getContentResolver(),Settings.System.DUAL_SIM_DATA_CALL, 0);
            Settings.System.putInt(context.getContentResolver(),Settings.System.DUAL_SIM_SMS, 0);
        }
    }

    public class SubscriptionData {
        public int numSubscriptions;
        public Subscription [] subscription;

        public SubscriptionData(int numSub) {
            numSubscriptions = numSub;
            subscription = new Subscription[numSub];
            for (int i = 0; i < numSub; i++) {
                subscription[i] = new Subscription();
            }
        }

        public SubscriptionData copyFrom(SubscriptionData from) {
            if (from != null) {
                numSubscriptions = from.numSubscriptions;
                subscription = new Subscription[numSubscriptions];
                for (int i = 0; i < numSubscriptions; i++) {
                    subscription[i] = new Subscription();
                    subscription[i].copyFrom(from.subscription[i]);
                }
            }
            return this;
        }
    }

    public class Subscription {
        public int slotId;         // Slot id
        public int subIndex;       // Subscription index in the card
        public int subNum;         // SUB 0 or SUB 1
        public int subStatus;      // ACTIVATE = 0, DEACTIVATE = 1, ACTIVATING = 2,
                                   // DEACTIVATING = 3, ACTIVATED = 4, DEACTIVATED = 5, INVALID = 6;
        public String appId;
        public String appLabel;
        public String appType;
        public String iccId;

        public Subscription() {
            slotId = -1;
            subIndex = -1;
            subNum = -1;
            subStatus = SUB_INVALID;
            appId = null;
            appLabel = null;
            appType = null;
            iccId = null;
        }

        public String toString() {
            return "Subscription = { "
                   + "slotId = " + slotId
                   + ", subIndex = " + subIndex
                   + ", subNum = " + subNum
                   + ", subStatus = " + subStatus
                   + ", appId = " + appId
                   + ", appLabel = " + appLabel
                   + ", appType = " + appType
                   + ", iccId = " + iccId + " }";
        }

        public boolean equals(Subscription sub) {
            if (sub != null) {
                if ((slotId == sub.slotId) && (subIndex == sub.subIndex) &&
                    (subNum == sub.subNum) && (subStatus == sub.subStatus) &&
                    ((appId == null && sub.appId == null) ||
                        (appId != null && appId.equals(sub.appId))) &&
                    ((appLabel == null && sub.appLabel == null) ||
                        (appLabel != null && appLabel.equals(sub.appLabel))) &&
                    ((appType == null && sub.appType == null) ||
                        (appType != null && appType.equals(sub.appType))) &&
                    ((iccId == null && sub.iccId == null) ||
                        (iccId != null && iccId.equals(sub.iccId)))) {
                    return true;
                }
            } else {
                Log.d(LOG_TAG, "Subscription.equals: sub == null");
            }
            return false;
        }
        public Subscription copyFrom(Subscription from) {
            if (from != null) {
                slotId = from.slotId;
                subIndex = from.subIndex;
                subNum = from.subNum;
                subStatus = from.subStatus;
                if (from.appId != null) {
                    appId = new String(from.appId);
                }
                if (from.appLabel != null) {
                    appLabel = new String(from.appLabel);
                }
                if (from.appType != null) {
                    appType = new String(from.appType);
                }
                if (from.iccId != null) {
                    iccId = new String(from.iccId);
                }
            }

            return this;
        }
    }
}//end of proxy manager



