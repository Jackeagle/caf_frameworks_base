/*
 * Copyright (c) 2010-2011, Code Aurora Forum. All rights reserved.
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
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioTechnologyFamily;
import com.android.internal.telephony.MSimIccPhoneBookInterfaceManagerProxy;
import com.android.internal.telephony.MSimIccSmsInterfaceManager;
import com.android.internal.telephony.MSimPhoneSubInfoProxy;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.SimRefreshResponse;
import com.android.internal.telephony.UiccCard;
import com.android.internal.telephony.UiccConstants.AppType;
import com.android.internal.telephony.UiccConstants.CardState;
import com.android.internal.telephony.UiccManager;

import java.util.regex.PatternSyntaxException;
import java.lang.Exception;

public class ProxyManager extends Handler {
    static final String LOG_TAG = "PROXY";

    //***** Constants

    // Subscription activation status
    public static final int SUB_ACTIVATE = 0;
    public static final int SUB_DEACTIVATE = 1;
    public static final int SUB_ACTIVATED = 2;
    public static final int SUB_DEACTIVATED = 3;
    public static final int SUB_INVALID = 4;

    public static final int SUBSCRIPTION_INDEX_INVALID = 99999;

    private static final int NUM_SUBSCRIPTIONS = 2;

    // Set Subscription Return status
    public static final String SUB_ACTIVATE_SUCCESS = "ACTIVATE SUCCESS";
    public static final String SUB_ACTIVATE_FAILED = "ACTIVATE FAILED";
    public static final String SUB_ACTIVATE_NOT_SUPPORTED = "ACTIVATE NOT SUPPORTED";
    public static final String SUB_DEACTIVATE_SUCCESS = "DEACTIVATE SUCCESS";
    public static final String SUB_DEACTIVATE_FAILED = "DEACTIVATE FAILED";
    public static final String SUB_DEACTIVATE_NOT_SUPPORTED = "DEACTIVATE NOT SUPPORTED";
    public static final String SUB_NOT_CHANGED = "NO CHANGE IN SUBSCRIPTION";

    //***** Events
    static final int EVENT_SET_UICC_SUBSCRIPTION_DONE = 1;
    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 2;
    static final int EVENT_ICC_CHANGED = 3;
    static final int EVENT_SET_SUBSCRIPTION_MODE_DONE = 4;
    static final int EVENT_DISABLE_DATA_CONNECTION_DONE = 5;
    static final int EVENT_GET_ICCID_DONE = 6;
    static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 7;
    static final int EVENT_RADIO_ON = 8;
    static final int EVENT_SUBSCRIPTION_READY = 9;
    static final int EVENT_SIM_REFRESH = 10;
    static final int EVENT_CLEANUP_DATA_CONNECTION_DONE = 11;

    //***** Class Variables
    private static ProxyManager sProxyManager;

    //***** Instance Variables

    // PhoneProxy instances
    private Phone mProxyPhones[] = null;
    // User default subscriptions
    private String[] mUserDefaultSubs = {"USIM", "USIM"};
    private CommandsInterface[] mCi;
    //MSimIccSmsInterfaceManager to use proper IccSmsInterfaceManager object
    private MSimIccSmsInterfaceManager mMSimIccSmsInterfaceManager;
    //MSimIccPhoneBookInterfaceManager    Proxy to use proper IccPhoneBookInterfaceManagerProxy object
    private MSimIccPhoneBookInterfaceManagerProxy mMSimIccPhoneBookInterfaceManagerProxy;
    //MSimPhoneSubInfoProxy to use proper PhoneSubInfoProxy object
    private MSimPhoneSubInfoProxy mMSimPhoneSubInfoProxy;
    private UiccManager mUiccManager;
    private UiccCard[] mUiccCards;
    private Context  mContext;
    private boolean mUiccSubSet = false;
    private boolean mDdsSet = false;
    private SupplySubscription mSupplySubscription;
    private int mQueuedDds;
    private int mCurrentDds;
    private boolean mDisableDdsInProgress = false;
    private Message mSetDdsCompleteMsg;
    private int mPendingIccidRequest = 0;
    private boolean mSetSubscriptionMode = true;
    private boolean mReadIccid = true;
    private boolean mSetSubOnSimRefresh = false;
    private String[] mIccIds;
    private boolean[] mSubscriptionReady = {false, false};
    // The subscription information of all the cards
    private SubscriptionData[] mCardSubData = null;
    // The User prefered subscription information
    private SubscriptionData mUserPrefSubs = null;


    //***** Class Methods
    public static ProxyManager getInstance(Context context, Phone[] phoneProxy,
            UiccManager uiccMgr, CommandsInterface[] ci)
    {
        Log.d(LOG_TAG, "In ProxyManager getInstance");
        if (sProxyManager == null) {
            sProxyManager = new ProxyManager(context, phoneProxy, uiccMgr, ci);
        }
        return sProxyManager;
    }

    static public ProxyManager getInstance() {
        return sProxyManager;
    }

    private ProxyManager(Context context, Phone[] phoneProxy, UiccManager uiccManager,
            CommandsInterface[] ci) {
        Log.d(LOG_TAG, "Creating ProxyManager");

        mContext = context;
        mProxyPhones = phoneProxy;
        getDefaultProperties(context);

        mMSimIccPhoneBookInterfaceManagerProxy = new MSimIccPhoneBookInterfaceManagerProxy(mProxyPhones);
        mMSimPhoneSubInfoProxy = new MSimPhoneSubInfoProxy(mProxyPhones);
        mMSimIccSmsInterfaceManager = new MSimIccSmsInterfaceManager(mProxyPhones);
        mUiccManager = uiccManager;
        mUiccManager.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        mCi = ci;

        if (TelephonyManager.isMultiSimEnabled()) {
            mUiccCards = new UiccCard[UiccConstants.RIL_MAX_CARDS];

            mCi[0].registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
            mCi[0].registerForOn(this, EVENT_RADIO_ON, null);

            getUserPreferredSubs();
            mSupplySubscription = this.new SupplySubscription(mContext);

            for (int i = 0; i < mCi.length; i++) {
                // Register for Subscription ready event for both the subscriptions.
                Integer sub = new Integer(i);
                mCi[i].registerForSubscriptionReady(this, EVENT_SUBSCRIPTION_READY, sub);

                // Register for SIM Refresh events
                mCi[i].registerForIccRefresh(this, EVENT_SIM_REFRESH, null);
            }

            // Get the current active dds
            mCurrentDds = PhoneFactory.getDataSubscription();
            Log.d(LOG_TAG, "In ProxyManager constructor current active dds is:" + mCurrentDds);
        }
    }

    /*
     *  This function will read from the User Preferred Subscription from the
     *  system property, parse and populate the member variable mUserPrefSubs.
     *  User Prefered Subscription is stored in the system property string as
     *    iccId,appType,appId,activationStatus,3gppIndex,3gpp2Index
     *  If the the property is not set already, then set it to the default values
     *  for appType to USIM and activationStatus to ACTIVATED.
     */
    private void getUserPreferredSubs() {
        boolean errorOnParsing = false;

        mUserPrefSubs = new SubscriptionData(NUM_SUBSCRIPTIONS);

        for(int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
            String strUserSub = Settings.System.getString(mContext.getContentResolver(),
                    Settings.System.USER_PREFERRED_SUBS[i]);
            if (strUserSub != null) {
                Log.d(LOG_TAG, "getUserPreferredSubs: strUserSub = " + strUserSub);

                try {
                    String splitUserSub[] = strUserSub.split(",");

                    // There should be 6 fields in the user prefered settings.
                    if (splitUserSub.length == 6) {
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
                            mUserPrefSubs.subscription[i].m3gppIndex = Integer.parseInt(splitUserSub[4]);
                        } catch (NumberFormatException ex) {
                            Log.e(LOG_TAG, "getUserPreferredSubs:m3gppIndex: NumberFormatException: " + ex);
                            mUserPrefSubs.subscription[i].m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
                        }

                        try {
                            mUserPrefSubs.subscription[i].m3gpp2Index = Integer.parseInt(splitUserSub[5]);
                        } catch (NumberFormatException ex) {
                            Log.e(LOG_TAG, "getUserPreferredSubs:m3gpp2Index: NumberFormatException: " + ex);
                            mUserPrefSubs.subscription[i].m3gpp2Index = SUBSCRIPTION_INDEX_INVALID;
                        }

                    } else {
                        Log.e(LOG_TAG, "getUserPreferredSubs: splitUserSub.length != 6");
                        errorOnParsing = true;
                    }
                } catch (PatternSyntaxException pe) {
                    Log.e(LOG_TAG, "getUserPreferredSubs: PatternSyntaxException while split : " + pe);
                    errorOnParsing = true;

                }
            }

            if (strUserSub == null || errorOnParsing) {
                String defaultUserSub = " " + ","        // iccId
                    + mUserDefaultSubs[i] + ","          // app type
                    + " " + ","                          // app id
                    + Integer.toString(SUB_ACTIVATED)    // activate state
                    + "," + SUBSCRIPTION_INDEX_INVALID   // 3gppIndex in the card
                    + "," + SUBSCRIPTION_INDEX_INVALID;  // 3gpp2Index in the card

                Settings.System.putString(mContext.getContentResolver(),
                        Settings.System.USER_PREFERRED_SUBS[i], defaultUserSub);

                mUserPrefSubs.subscription[i].iccId = null;
                mUserPrefSubs.subscription[i].appType = mUserDefaultSubs[i];
                mUserPrefSubs.subscription[i].appId = null;
                mUserPrefSubs.subscription[i].subStatus = SUB_ACTIVATED;
                mUserPrefSubs.subscription[i].m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
                mUserPrefSubs.subscription[i].m3gpp2Index = SUBSCRIPTION_INDEX_INVALID;
            }

            mUserPrefSubs.subscription[i].subId = i;

            Log.d(LOG_TAG, "getUserPreferredSubs: mUserPrefSubs.subscription[" + i + "] = "
                    + mUserPrefSubs.subscription[i]);
        }
    }

    private void saveUserPreferredSubscription(SubscriptionData userPrefSubData) {
        Subscription userPrefSub;
        String userSub;

        // Update the user prefered sub
        mUserPrefSubs.copyFrom(userPrefSubData);

        for (int index = 0; index < userPrefSubData.getLength(); index++) {
            userPrefSub = userPrefSubData.subscription[index];

            userSub = ((userPrefSub.iccId != null) ? userPrefSub.iccId : " ") + ","
                + ((userPrefSub.appType != null) ? userPrefSub.appType : " ") + ","
                + ((userPrefSub.appId != null) ? userPrefSub.appId : " ") + ","
                + Integer.toString(userPrefSub.subStatus) + ","
                + Integer.toString(userPrefSub.m3gppIndex) + ","
                + Integer.toString(userPrefSub.m3gpp2Index);

            Log.d(LOG_TAG, "saveUserPreferredSubscription: userPrefSub = " + userPrefSub);
            Log.d(LOG_TAG, "saveUserPreferredSubscription: userSub = " + userSub);

            // Construct the string and store in Settings data base at index.
            // update the user pref settings so that next time user is
            // not prompted of the subscriptions
            Settings.System.putString(mContext.getContentResolver(),
                    Settings.System.USER_PREFERRED_SUBS[index], userSub);
        }
    }

    private String getStringFrom(String str) {
        if ((str == null) || (str != null && str.equals(" "))) {
            return null;
        }
        return str;
    }

    private void updateSubPreferences(SubscriptionData subData) {
        int activSubCount = 0;
        Subscription activeSub = null;

        for(Subscription sub : subData.subscription) {
            if (sub != null && sub.subStatus == SUB_ACTIVATED) {
                activSubCount++;
                activeSub = sub;
            }
        }

        // If there is only one active subscription, set user prefered settings
        // for voice/sms/data subscription to this subscription.
        if (activSubCount == 1) {
            Log.d(LOG_TAG, "updateSubPreferences: only SUB:" + activeSub.subId
                    + " is Active.  Update the default/voice/sms and data subscriptions");
            PhoneFactory.setVoiceSubscription(activeSub.subId);
            PhoneFactory.setSMSSubscription(activeSub.subId);

            Log.d(LOG_TAG, "updateSubPreferences: current defaultSub = "
                    + PhoneFactory.getDefaultSubscription());
            Log.d(LOG_TAG, "updateSubPreferences: current mCurrentDds = " + mCurrentDds);
            if (PhoneFactory.getDefaultSubscription() != activeSub.subId) {
                PhoneFactory.setDefaultSubscription(activeSub.subId);
            }

            if (mCurrentDds != activeSub.subId) {
                // Currently selected DDS subscription is not in activated state.
                // So set the DDS to the only active subscription available now.
                // Directly set the Data Subscription Source to the only activeSub if it
                // is READY. If the SUBSCRIPTION_READY event is not yet received on this
                // subscription, wait for the event to set the Data Subscription Source.
                if (mSubscriptionReady[activeSub.subId]) {
                    mQueuedDds = activeSub.subId;
                    Message callback = Message.obtain(this, EVENT_SET_DATA_SUBSCRIPTION_DONE,
                            Integer.toString(activeSub.subId));
                    Log.d(LOG_TAG, "update setDataSubscription to " + activeSub.subId);
                    mCi[activeSub.subId].setDataSubscription(callback);
                } else {
                    // Set the flag and update the mCurrentDds, so that when subscription
                    // ready event receives, it will set the dds properly.
                    mDdsSet = false;
                    mCurrentDds = activeSub.subId;
                    PhoneFactory.setDataSubscription(mCurrentDds);
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        int cardIndex = 0;
        String strCardIndex;

        switch(msg.what) {
            case EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                // Unregister for card status indication when radio state becomes OFF or UNAVAILABLE.
                mUiccManager.unregisterForIccChanged(sProxyManager);
                // Reset the flag, which takes care of processing/Handling of card status
                // card status is processed only the first time
                mUiccSubSet = true;
                break;

            case EVENT_RADIO_ON:
                // Register for card status indication when radio state becomes ON.
                mUiccManager.registerForIccChanged(sProxyManager, EVENT_ICC_CHANGED, null);
                mSetSubscriptionMode = true;
                mUiccSubSet = false;
                mReadIccid = true;
                mDdsSet = false;
                break;

            case EVENT_ICC_CHANGED:
                if (TelephonyManager.isMultiSimEnabled()) {
                    Log.d(LOG_TAG, "ProxyManager EVENT_ICC_CHANGED for DSDS");
                    for (int i = 0; i < UiccConstants.RIL_MAX_CARDS; i++) {
                        mUiccCards[i] = mUiccManager.getCard(i);
                    }
                    if (!mUiccSubSet) {
                        checkCardStatus();
                    }
                } else {
                    SubscriptionData cardSubData = new SubscriptionData(1);
                    cardSubData.subscription[0].slotId = 0;
                    cardSubData.subscription[0].subId = 0;
                    cardSubData.subscription[0].m3gppIndex = mUiccManager.getFirst3gppAppIndex(0);
                    cardSubData.subscription[0].m3gpp2Index = mUiccManager.getFirst3gpp2AppIndex(0);
                    Log.d(LOG_TAG, "ProxyManager EVENT_ICC_CHANGED for non DSDS m3gppIndex::"
                            + cardSubData.subscription[0].m3gppIndex + " m3gpp2Index::"
                            + cardSubData.subscription[0].m3gpp2Index);
                    mProxyPhones[0].setSubscriptionInfo(cardSubData.subscription[0]);
                }
                break;

            case EVENT_SUBSCRIPTION_READY:
                ar = (AsyncResult)msg.obj;
                Integer subscription = (Integer)ar.userObj;
                int sub = subscription.intValue();
                Log.d(LOG_TAG, "SUBSCRIPTION READY event on SUB:" + sub);
                mSubscriptionReady[sub] = true;

                if (!mDdsSet) {
                    // Set Data Subscription Source only when subscription becomes READY.
                    if (sub == mCurrentDds) {
                        // Set data sub only if the sub is activated.
                        if (getCurrentSubscriptions()
                                .subscription[mCurrentDds].subStatus == SUB_ACTIVATED) {
                            String str = Integer.toString(mCurrentDds);
                            Message callback = Message.obtain(this,
                                    EVENT_SET_DATA_SUBSCRIPTION_DONE, str);
                            // Set Data Subscription preference at RIL
                            Log.d(LOG_TAG, "setDataSubscription on " + mCurrentDds);
                            mCi[mCurrentDds].setDataSubscription(callback);
                            mDdsSet = true;
                            // Set mQueuedDds so that when the set data sub src is done, it will
                            // update the system property and enable the data connectivity.
                            mQueuedDds = mCurrentDds;
                        } else {
                            Log.d(LOG_TAG, "User prefered data subsciption " + mCurrentDds
                                    + " is not ACTIVATED");
                        }
                    }
                }
                break;

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
                Log.d(LOG_TAG, "EVENT_DISABLE_DATA_CONNECTION_DONE, mDisableDdsInProgress = "
                        + mDisableDdsInProgress);
                if (mDisableDdsInProgress) {
                    // Set the DDS in cmd interface
                    String str = Integer.toString(mQueuedDds);
                    Message callback = Message.obtain(this, EVENT_SET_DATA_SUBSCRIPTION_DONE, str);
                    Log.d(LOG_TAG, "Set DDS to " + mQueuedDds
                            + " Calling cmd interface setDataSubscription");
                    mCi[mQueuedDds].setDataSubscription(callback);
                }
                break;

            case EVENT_SIM_REFRESH:
                Log.d(LOG_TAG, "SIM refresh EVENT_SIM_REFRESH");
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    processSimRefresh(ar);
                }
                break;

            case EVENT_SET_DATA_SUBSCRIPTION_DONE:
                Log.d(LOG_TAG, "EVENT_SET_DATA_SUBSCRIPTION_DONE");

                ar = (AsyncResult) msg.obj;

                SetDdsResult result = SetDdsResult.ERR_GENERIC_FAILURE;
                //if SUCCESS
                if (ar.exception == null) {
                    // Mark this as the current dds
                    PhoneFactory.setDataSubscription(mQueuedDds);
                    mCurrentDds = mQueuedDds;

                    // Enable the data phone for the new dds
                    ((PhoneProxy) mProxyPhones[mCurrentDds]).updateDataConnectionTracker();

                    // Enable the data connectivity on new dds.
                    Log.d(LOG_TAG, "setDataSubscriptionSource is Successful"
                            + "  Enable Data Connectivity on Subscription " + mCurrentDds);
                    mProxyPhones[mCurrentDds].enableDataConnectivity();

                    result = SetDdsResult.SUCCESS;

                    //Subscription is changed to this new sub, need to update the DB to mark
                    //the respective profiles as "current".
                    ((PhoneProxy)mProxyPhones[mCurrentDds]).updateCurrentCarrierInProvider();
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
                mDisableDdsInProgress = false;

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

    private void getCardIccids() {
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

    private void checkCardStatus() {
        UiccCard card1 = mUiccCards[0];
        UiccCard card2 = mUiccCards[1];

        if (card1 != null && card2 != null) {
            Log.d(LOG_TAG, ":  card 1 state: "+card1.getCardState());
            Log.d(LOG_TAG, ":  card 2 state: "+card2.getCardState());

            // Card status to be processed if
            // card1 status is present and card2 status is present
            // card1 status is present and card2 status is absent
            // card1 status is absent and card2 status is present
            if ((!(card1.getCardState() == CardState.ABSENT
                        && card2.getCardState() == CardState.ABSENT))
                    && (!(card1.getCardState() == CardState.ERROR
                        && card2.getCardState() == CardState.ERROR))) {
                // Get the Iccid and then process the cards
                if (mReadIccid) {
                    mReadIccid = false;
                    getCardIccids();
                }
            }
        }
    }

    private void processCardStatus() {
        int numApps = 0;

        mCardSubData = new SubscriptionData[UiccConstants.RIL_MAX_CARDS];

        // Loop through list of cards and list of applications and store it in the mCardSubData
        for (int cardIndex = 0; cardIndex < UiccConstants.RIL_MAX_CARDS; cardIndex++) {
            CardState cardstate = mUiccCards[cardIndex].getCardState();
            Log.d(LOG_TAG, "cardIndex = " + cardIndex + " cardstate = " + cardstate);

            if (cardstate == CardState.PRESENT) {
                numApps = mUiccCards[cardIndex].getNumApplications();
                Log.d(LOG_TAG, "Number of apps : " + numApps);

                mCardSubData[cardIndex] = new SubscriptionData(numApps);

                for (int appIndex = 0; appIndex < numApps ; appIndex++ ) {
                    Log.d(LOG_TAG, "appIndex : "+ appIndex);

                    Subscription cardSub = mCardSubData[cardIndex].subscription[appIndex];

                    UiccCardApplication uiccCardApplication = mUiccCards[cardIndex]
                        .getUiccCardApplication(appIndex);

                    cardSub.slotId = cardIndex;
                    cardSub.subId = SUBSCRIPTION_INDEX_INVALID;  // Not set the sub id
                    cardSub.subStatus = SUB_INVALID;
                    cardSub.appId = uiccCardApplication.getAid();
                    cardSub.appLabel = uiccCardApplication.getAppLabel();
                    cardSub.iccId = mIccIds[cardIndex];

                    AppType type = uiccCardApplication.getType();
                    String subAppType = appTypetoString(type);
                    //Apps like ISIM etc are treated as UNKNOWN apps, to be discarded
                    if (!subAppType.equals("UNKNOWN")) {
                        Log.d(LOG_TAG, "appType: "+ subAppType);
                        cardSub.appType = subAppType;
                    } else {
                        cardSub.appType = null;
                        Log.d(LOG_TAG, "UNKNOWN APP");
                    }

                    fillAppIndex(cardSub, appIndex);
                    Log.d(LOG_TAG, "mCardSubData[" + cardIndex + "].subscription[" + appIndex +
                            "] = " + cardSub.toString());
                }
            }
        }

        matchSubscriptions();
    }

    private void fillAppIndex(Subscription cardSub, int appIndex) {
        if (cardSub.appType.equals("SIM") || cardSub.appType.equals("USIM")) {
            cardSub.m3gppIndex = appIndex;
            cardSub.m3gpp2Index = SUBSCRIPTION_INDEX_INVALID;
        } else if (cardSub.appType.equals("RUIM") || cardSub.appType.equals("CSIM")) {
            cardSub.m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
            cardSub.m3gpp2Index = appIndex;
        }
    }

    private String appTypetoString(AppType p) {
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

    /*
     * Compare each of the user pref sub with the Subscriptions in each of the card.
     * If all of the user pref subscriptions, which were activated on the last session,
     * matches the subscriptions(applications) in the card then automatically set the
     * subscription.  Otherwise prompt the User to select subscriptions.
     */
    private void matchSubscriptions() {
        int cardIndex = 0;
        int num_cards = 0;
        SubscriptionData matchedSub = new SubscriptionData(NUM_SUBSCRIPTIONS);

        Log.d(LOG_TAG, "matchSubscriptions");

        // For each subscription in mUserPrefSubs
        for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
            Subscription userSub = mUserPrefSubs.subscription[i];
            Log.d(LOG_TAG, "subId: " + i);

            // For each cards in mCardSubData
            for (cardIndex = 0; cardIndex < UiccConstants.RIL_MAX_CARDS; cardIndex++) {
                Log.d(LOG_TAG, "cardIndex: " + cardIndex + " userSub.m3gppIndex: "
                        + userSub.m3gppIndex + " userSub.m3gpp2Index: "
                        + userSub.m3gpp2Index);

                if (((userSub.m3gppIndex != SUBSCRIPTION_INDEX_INVALID)
                            || (userSub.m3gpp2Index != SUBSCRIPTION_INDEX_INVALID))
                        && (mCardSubData[cardIndex] != null)
                        && (userSub.getAppIndex() < mCardSubData[cardIndex].getLength())) {
                    Subscription cardSub = mCardSubData[cardIndex].subscription[userSub.getAppIndex()];

                    // Check for the iccid, app id, app name
                    if (((userSub.iccId == null && cardSub.iccId == null)
                                || (userSub.iccId != null && userSub.iccId.equals(cardSub.iccId)))
                            && ((userSub.appId == null && cardSub.appId == null)
                                || (userSub.appId != null && userSub.appId.equals(cardSub.appId)))
                            && ((userSub.appType == null && cardSub.appType == null)
                                || (userSub.appType != null && userSub.appType.equals(cardSub.appType)))) {

                        // Update the matched subscription
                        matchedSub.subscription[i].copyFrom(userSub);
                        // Set the activate state
                        matchedSub.subscription[i].subStatus = SUB_ACTIVATE;

                        if (mSetSubOnSimRefresh) {
                            //In case of sim refresh, only affected subscriptions are activated wrt slotID.
                            if (mSupplySubscription.prevSubscriptionData.subscription[i].subStatus
                                    == SUB_ACTIVATED) {
                                matchedSub.subscription[i].subStatus = SUB_ACTIVATED;
                                Log.d(LOG_TAG,"current subStatus is set to SUB_ACTIVATED");
                            }
                        }
                        matchedSub.subscription[i].subId = i;
                        // Set the slot id, sub index and appLabel from mCardSubData
                        matchedSub.subscription[i].slotId = cardSub.slotId;
                        matchedSub.subscription[i].m3gppIndex = cardSub.m3gppIndex;
                        matchedSub.subscription[i].m3gpp2Index = cardSub.m3gpp2Index;
                        if (cardSub.appLabel != null) {
                            matchedSub.subscription[i].appLabel = new String(cardSub.appLabel);
                        }

                        Log.d(LOG_TAG, "Subscription is matched for UserPrefSub subId = " + i
                                + " cardIndex = " + cardIndex + " 3gppIndex = "
                                + userSub.m3gppIndex + " 3gpp2Index = " + userSub.m3gpp2Index);
                        break;
                    }
                }
                Log.d(LOG_TAG, "Not matched for UserPrefSub subId: " + i
                        + " userSub.3gppIndex : " + userSub.m3gppIndex + " cardIndex : "
                        + cardIndex + " userSub.3gpp2Index : " + userSub.m3gpp2Index);
            }
            Log.d(LOG_TAG, "matchedSub.subscription[" + i + "] = " + matchedSub.subscription[i]);
        }

        // If the user pref sub is not matched, then propmt the user to select the subs.
        if ((mUserPrefSubs.subscription[0].subStatus == SUB_ACTIVATED
                    && matchedSub.subscription[0].subId == SUBSCRIPTION_INDEX_INVALID)
                || (mUserPrefSubs.subscription[1].subStatus == SUB_ACTIVATED
                    && matchedSub.subscription[1].subId == SUBSCRIPTION_INDEX_INVALID)) {
            //Subscription settings do not match with the card applications
            mUiccSubSet = true;  //card status is processed only the first time
            promptUserSubscription();
        } else {
            setSubscription(matchedSub, null);
            mUiccSubSet = true;
        }
    }

    private void processSimRefresh(AsyncResult ar) {

        SimRefreshResponse state = (SimRefreshResponse)ar.result;
        if (state == null) {
            Log.e(LOG_TAG, "processSimRefresh received without input");
            return;
        }

        if (state.refreshResult == SimRefreshResponse.Result.SIM_RESET) {

            Log.d(LOG_TAG, "processSimRefresh: refreshResult" + state.refreshResult);
            int currentSlotId = state.slot;
            Log.d(LOG_TAG, "processSimRefresh: currentSlotId" + currentSlotId);
            //subscription in mUserPrefSubs
            for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
                if (mUserPrefSubs.subscription[i].slotId == currentSlotId) {
                    Log.d(LOG_TAG, "processSimRefresh: mUserPrefSubs.slotId"
                            + mUserPrefSubs.subscription[i].slotId);
                    //By changing the status of prevSubscriptionData,
                    //we can activate individual subscriptions.
                    mSupplySubscription.prevSubscriptionData.subscription[i].subStatus = SUB_DEACTIVATED;
                    mUiccSubSet = false;
                    mReadIccid = true;
                    if (mUserPrefSubs.subscription[i].getAppIndex() == mCurrentDds) {
                        mDdsSet = false;
                    }
                }
            }
            mSetSubOnSimRefresh = true;
        }
    }

    /** Sets the subscriptions */
    public void setSubscription(SubscriptionData subData, Message onCompleteMsg) {

        Log.d(LOG_TAG, "setSubscription");

        for (int i = 0; i < subData.getLength() ; i++) {
            Log.d(LOG_TAG, "subData.subscription[" + i + "] = " + subData.subscription[i]);

            if ((subData.subscription[i].slotId != SUBSCRIPTION_INDEX_INVALID)
                    && (subData.subscription[i].m3gppIndex != SUBSCRIPTION_INDEX_INVALID)
                    || (subData.subscription[i].m3gpp2Index != SUBSCRIPTION_INDEX_INVALID)) {

                SubscriptionData cardSubData = mCardSubData[subData.subscription[i].slotId];
                Subscription cardSub = cardSubData.subscription[subData.subscription[i].getAppIndex()];

                if (((cardSub.appType.equals("SIM"))
                            || (cardSub.appType.equals("USIM")))
                        && (!mProxyPhones[i].getPhoneName().equals("GSM"))) {
                    Log.d(LOG_TAG, "gets New GSM phone" );
                    ((PhoneProxy) mProxyPhones[i]).updatePhoneObject(RadioTechnologyFamily.RADIO_TECH_3GPP, i);
                } else if (((cardSub.appType.equals("RUIM"))
                            || (cardSub.appType.equals("CSIM")))
                        && (!mProxyPhones[i].getPhoneName().equals("CDMA")) ) {
                    Log.d(LOG_TAG, "gets New CDMA phone" );
                    ((PhoneProxy) mProxyPhones[i]).updatePhoneObject(RadioTechnologyFamily.RADIO_TECH_3GPP2, i);
                }
            }
        }

        mUiccSubSet = true;

        // Setting the subscription at RIL/Modem is handled through a thread. Start the thread
        // if it is not already started.
        if (!mSupplySubscription.isAlive()) {
            mSupplySubscription.start();
        }

        mSupplySubscription.setSubscription(subData, onCompleteMsg);
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

        private Handler mHandler;
        private Context  mContext;
        private String [] mSubResult;

        private SubscriptionData subscriptionData;
        private SubscriptionData prevSubscriptionData;

        private Message mSetSubCompleteMsg;
        private int mPendingDeactivateEvents;
        private int mPendingActivateEvents;

        public SupplySubscription(Context context) {
            mContext = context;
            mSubResult = new String[NUM_SUBSCRIPTIONS];

            subscriptionData = new SubscriptionData(NUM_SUBSCRIPTIONS);
            prevSubscriptionData = new SubscriptionData(NUM_SUBSCRIPTIONS);

            mSetSubCompleteMsg = null;
        }


        @Override
        public void run() {
            Looper.prepare();
            synchronized (SupplySubscription.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        int phoneIndex = 0;
                        AsyncResult ar = null;
                        String string = null;

                        if (msg.obj instanceof AsyncResult) {
                            ar = (AsyncResult) msg.obj;
                            string = (String) ar.userObj;
                        } else if (msg.obj instanceof String) {
                            // In case of callback msg from disableDataConnectivity.
                            string = (String) msg.obj;
                        }

                        if (string != null) {
                            phoneIndex = Integer.parseInt(string);
                            Log.d(LOG_TAG, "phoneIndex: " + phoneIndex);
                        }

                        Log.d(LOG_TAG, "Received " + msg.what + " on Subscription : " + phoneIndex);

                        switch (msg.what) {
                            case EVENT_SET_SUBSCRIPTION_MODE_DONE:
                                // Event received when SUBSCRIPTION_MODE is set at
                                // Modem SingleStandBy/DualStandBy
                                Log.d(LOG_TAG, "EVENT_SET_SUBSCRIPTION_MODE_DONE:");
                                for (int index = 0; index < subscriptionData.getLength(); index++) {
                                    Subscription sub = subscriptionData.subscription[index];

                                    if (sub.slotId != SUBSCRIPTION_INDEX_INVALID &&
                                            (sub.m3gppIndex != SUBSCRIPTION_INDEX_INVALID ||
                                             sub.m3gpp2Index != SUBSCRIPTION_INDEX_INVALID)) {
                                        String subId = Integer.toString(index);
                                        Message msgSetUiccSubDone = Message.obtain(mHandler,
                                                EVENT_SET_UICC_SUBSCRIPTION_DONE, subId);
                                        Log.d(LOG_TAG, "Calling setSubscription on CommandsInterface: "
                                                + index);
                                        mPendingActivateEvents++;
                                        mCi[index].setUiccSubscription(sub.slotId, sub.getAppIndex(),
                                                sub.subId, sub.subStatus, msgSetUiccSubDone);
                                    } else {
                                        // This subscription is not in use.  Mark as INVALID.
                                        subscriptionData.subscription[index].subStatus = SUB_INVALID;
                                    }
                                }
                                break;

                            case EVENT_SET_UICC_SUBSCRIPTION_DONE:
                                // Event received when SET_SUBSCRIPTION is set at RIL
                                processSetUiccSubscriptionDone(phoneIndex, ar);
                                break;

                            case EVENT_CLEANUP_DATA_CONNECTION_DONE:
                                // This callback message will be received when user initiated
                                // a deactivate subscription.
                                Log.d(LOG_TAG, "EVENT_CLEANUP_DATA_CONNECTION_DONE: on sub: "
                                        + phoneIndex + " Deactivate now");

                                // Need to deactivate prev sub
                                prevSubscriptionData.subscription[phoneIndex].subStatus =
                                        SUB_DEACTIVATE;

                                Message setUiccSubCompleteMsg = Message.obtain(mHandler,
                                        EVENT_SET_UICC_SUBSCRIPTION_DONE, string);

                                mCi[phoneIndex].setUiccSubscription(
                                        prevSubscriptionData.subscription[phoneIndex].slotId,
                                        prevSubscriptionData.subscription[phoneIndex].getAppIndex(),
                                        prevSubscriptionData.subscription[phoneIndex].subId,
                                        prevSubscriptionData.subscription[phoneIndex].subStatus,
                                        setUiccSubCompleteMsg);
                                break;
                        }
                    }
                };
                SupplySubscription.this.notifyAll();
            }
            Looper.loop();
        }

        private void processSetUiccSubscriptionDone(int phoneIndex, AsyncResult ar) {
            Log.d(LOG_TAG, "processSetUiccSubscriptionDone()");

            synchronized (SupplySubscription.this) {
                if (ar.exception != null) {
                    // SET_UICC_SUBSCRIPTION failed

                    Log.d(LOG_TAG, "EVENT_SET_UICC_SUBSCRIPTION_DONE failed, phone index = "
                            + phoneIndex) ;

                    boolean notSupported = false;
                    if (ar.exception instanceof CommandException ) {
                        CommandException.Error error = ((CommandException) (ar.exception))
                            .getCommandError();
                        if (error != null &&
                                error ==  CommandException.Error.SUBSCRIPTION_NOT_SUPPORTED) {
                            notSupported = true;
                        }
                    }

                    if (prevSubscriptionData.subscription[phoneIndex].subStatus
                            == SUB_DEACTIVATE) {
                        // Set uicc subscription failed for deactivating the prev sub.
                        // Fall back to prev sub.
                        Log.d(LOG_TAG, "prevSubscription of SUB:" + phoneIndex
                                + " Deactivate Failed");
                        mPendingDeactivateEvents--;
                        if (notSupported) {
                            mSubResult[phoneIndex] = SUB_DEACTIVATE_NOT_SUPPORTED;
                        } else {
                            mSubResult[phoneIndex] = SUB_DEACTIVATE_FAILED;
                        }
                        if (subscriptionData.subscription[phoneIndex].subStatus == SUB_ACTIVATE) {
                            mSubResult[phoneIndex] = SUB_ACTIVATE_FAILED;
                        }

                        // Not deactivated., so set as activated.
                        prevSubscriptionData.subscription[phoneIndex].subStatus = SUB_ACTIVATED;
                        subscriptionData.subscription[phoneIndex].copyFrom(
                                prevSubscriptionData.subscription[phoneIndex]);

                        if (mPendingDeactivateEvents == 0) {
                            processPendingActivateRequests();
                        }
                    } else {
                        // Set uicc subscription failed for activating the sub.
                        Log.d(LOG_TAG, "subscription of SUB:" + phoneIndex + " Activate Failed");
                        mPendingActivateEvents--;
                        if (notSupported) {
                            mSubResult[phoneIndex] = SUB_ACTIVATE_NOT_SUPPORTED;
                        } else {
                            mSubResult[phoneIndex] = SUB_ACTIVATE_FAILED;
                        }
                        subscriptionData.subscription[phoneIndex].subStatus = SUB_DEACTIVATED;
                    }
                } else {
                    // SET_UICC_SUBSCRIPTION success

                    Log.d(LOG_TAG, "EVENT_SET_UICC_SUBSCRIPTION_DONE success, phone index = "
                            + phoneIndex) ;
                    if (prevSubscriptionData.subscription[phoneIndex].subStatus
                            == SUB_DEACTIVATE) {
                        Log.d(LOG_TAG, "prevSubscription of SUB:" + phoneIndex + " Deactivated");
                        mPendingDeactivateEvents--;
                        mSubResult[phoneIndex] = SUB_DEACTIVATE_SUCCESS;
                        prevSubscriptionData.subscription[phoneIndex].subStatus = SUB_DEACTIVATED;
                        mSubscriptionReady[phoneIndex] = false;
                        if (subscriptionData.subscription[phoneIndex].subStatus == SUB_DEACTIVATE) {
                            subscriptionData.subscription[phoneIndex].subStatus = SUB_DEACTIVATED;
                        }

                        if (mPendingDeactivateEvents == 0) {
                            processPendingActivateRequests();
                        }
                    } else {
                        Log.d(LOG_TAG, "subscription of SUB:" + phoneIndex + " Activated");
                        mPendingActivateEvents--;
                        mSubResult[phoneIndex] = SUB_ACTIVATE_SUCCESS;
                        subscriptionData.subscription[phoneIndex].subStatus = SUB_ACTIVATED;

                        Phone currentPhone = mProxyPhones[phoneIndex];

                        //set subscription success, update subscription info in phone objects
                        currentPhone.setSubscriptionInfo(subscriptionData.subscription[phoneIndex]);

                        mCurrentDds = PhoneFactory.getDataSubscription();
                        if (currentPhone.getSubscription() == mCurrentDds) {
                            Log.d(LOG_TAG, "Active DDS : " + currentPhone.getSubscription());
                            currentPhone.enableDataConnectivity();
                        }
                    }
                }

                if (mPendingActivateEvents == 0 && mPendingDeactivateEvents == 0) {
                    Log.d(LOG_TAG, "Set UICC Subscriptions Completed!!!");

                    sendSetSubscriptionCallback();

                    // Store the User prefered Subscription once all
                    // the set uicc subscription is done.
                    saveUserPreferredSubscription(subscriptionData);
                    prevSubscriptionData.copyFrom(subscriptionData);

                    updateSubPreferences(subscriptionData);

                    SupplySubscription.this.notifyAll();
                }
            }
        }

        /*
         * Sends SET_UICC_SUBSCRIPTION to RIL to activate the new subscriptions
         * that user has selected.  The activate request will send only of the
         * new subscription is not in use.
         */
        private void processPendingActivateRequests() {
            Log.d(LOG_TAG, "processPendingActivateRequests()");

            for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
                if (subscriptionData.subscription[i].subStatus == SUB_ACTIVATE &&
                        (prevSubscriptionData.subscription[i].subStatus == SUB_INVALID ||
                         prevSubscriptionData.subscription[i].subStatus == SUB_DEACTIVATED)) {
                    if (!isSubscriptionInUse(subscriptionData.subscription[i])) {
                        Log.d(LOG_TAG, "Activating subscriptionData on SUB:" + i);

                        String str = Integer.toString(i);
                        Message callback = Message.obtain(mHandler,
                                EVENT_SET_UICC_SUBSCRIPTION_DONE, str);
                        Log.d(LOG_TAG, "Calling setSubscription on CommandsInterface: " + i);
                        mPendingActivateEvents++;
                        mCi[i].setUiccSubscription(subscriptionData.subscription[i].slotId,
                                subscriptionData.subscription[i].getAppIndex(),
                                subscriptionData.subscription[i].subId,
                                subscriptionData.subscription[i].subStatus,
                                callback);
                        mSetSubOnSimRefresh = false;
                    } else {
                        mSubResult[i] = "ACTIVATE FAILED";
                        // Fall back to the previous subscription.
                        subscriptionData.subscription[i].copyFrom(prevSubscriptionData.subscription[i]);
                    }
                }
            }
        }

        /*
         * Returns true if the Subscription 'sub' is already in use
         */
        private boolean isSubscriptionInUse(Subscription sub) {
            for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
                Subscription prev = prevSubscriptionData.subscription[i];

                if ((prev.slotId == sub.slotId) &&
                        (prev.m3gppIndex == sub.m3gppIndex) &&
                        (prev.m3gpp2Index == sub.m3gpp2Index) &&
                        //(prev.subId == sub.subId) &&  // no need to compare subId
                        ((prev.appId == null && sub.appId == null) ||
                         (prev.appId != null && prev.appId.equals(sub.appId))) &&
                        ((prev.appLabel == null && sub.appLabel == null) ||
                         (prev.appLabel != null && prev.appLabel.equals(sub.appLabel))) &&
                        ((prev.appType == null && sub.appType == null) ||
                         (prev.appType != null && prev.appType.equals(sub.appType))) &&
                        ((prev.iccId == null && sub.iccId == null) ||
                         (prev.iccId != null && prev.iccId.equals(sub.iccId)))) {
                    // If the sub status is other than deactvated/invalid return true
                    if (!(prev.subStatus == SUB_DEACTIVATED ||
                                prev.subStatus == SUB_INVALID)) {
                        return true;
                    }
                }
            }
            return false;
        }


        synchronized void setSubscription(SubscriptionData userSubData, Message onCompleteMsg) {
            String [] string = null;
            boolean done = true;

            mPendingDeactivateEvents = 0;
            mPendingActivateEvents = 0;
            mSubResult[0] = SUB_NOT_CHANGED;
            mSubResult[1] = SUB_NOT_CHANGED;

            Log.d(LOG_TAG, "In setSubscription");
            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            mSetSubCompleteMsg = onCompleteMsg;

            Log.d(LOG_TAG, "Copying the subscriptionData from the userSubData");
            subscriptionData.copyFrom(userSubData);
            Log.d(LOG_TAG, "subscriptionData.getLength() : "
                    + subscriptionData.getLength());

            if (!mSetSubscriptionMode) {
                for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
                    Log.d(LOG_TAG, "prevSubscriptionData.subscription[" + i + "] = "
                            + prevSubscriptionData.subscription[i]);
                    Log.d(LOG_TAG, "subscriptionData.subscription[" + i + "] = "
                            + subscriptionData.subscription[i]);

                    // If the previous subscription is not equal to the current
                    // subscription (ie., the user must have marked this subscription
                    // as deactivate or selected a new sim app for this subscription),
                    // then deactivate the previous subscription.
                    if (!prevSubscriptionData.subscription[i]
                            .equals(subscriptionData.subscription[i])) {
                        Log.d(LOG_TAG, "prevSubscriptionData.subscription[" + i
                                + "] != subscriptionData.subscription[" + i + "]");

                        if (prevSubscriptionData.subscription[i].subStatus == SUB_ACTIVATED) {
                            // Need to deactivate prev sub
                            int subId = prevSubscriptionData.subscription[i].subId;
                            String sub = Integer.toString(subId);

                            Log.d(LOG_TAG, "Need to deactivate prevSubscription on SUB:" + subId);

                            if (mCurrentDds == subId) {
                                // Tear down all the data calls on this subscription. Once the
                                // clean up completed, the set uicc subscription request with
                                // deactivate will be sent to deactivate this subscription.
                                Log.d(LOG_TAG, "Deactivate all the data calls if there is any");
                                Message allDataCleanedUpMsg = Message.obtain(mHandler,
                                        EVENT_CLEANUP_DATA_CONNECTION_DONE, sub);
                                mProxyPhones[subId].disableDataConnectivity(allDataCleanedUpMsg);
                            } else {
                                prevSubscriptionData.subscription[i].subStatus = SUB_DEACTIVATE;
                                Message setUiccSubCompleteMsg = Message.obtain(mHandler,
                                        EVENT_SET_UICC_SUBSCRIPTION_DONE, sub);
                                mCi[i].setUiccSubscription(prevSubscriptionData.subscription[i].slotId,
                                        prevSubscriptionData.subscription[i].getAppIndex(),
                                        prevSubscriptionData.subscription[i].subId,
                                        prevSubscriptionData.subscription[i].subStatus,
                                        setUiccSubCompleteMsg);
                            }

                            done = false;
                            mPendingDeactivateEvents++;
                        } else if (prevSubscriptionData.subscription[i].subStatus == SUB_DEACTIVATED
                                && subscriptionData.subscription[i].subStatus == SUB_DEACTIVATE) {
                            // This subscription is already in deactivated state.
                            // Update the status properly.
                            subscriptionData.subscription[i].subStatus = SUB_DEACTIVATED;
                        }
                    }
                }

                // If there is no deactivate request in progress, then send activate request for
                // the subscriptions that user have selected newly.
                if (mPendingDeactivateEvents == 0) {
                    for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
                        // If subscription i is not activated currently, and user tries to activate it
                        if (subscriptionData.subscription[i].subStatus == SUB_ACTIVATE) {
                            Log.d(LOG_TAG, "Activating subscription on SUB:"
                                    + subscriptionData.subscription[i].subId);
                            mPendingActivateEvents++;
                            String str = Integer.toString(subscriptionData.subscription[i].subId);
                            Message callback = Message.obtain(mHandler,
                                    EVENT_SET_UICC_SUBSCRIPTION_DONE, str);
                            mCi[i].setUiccSubscription(subscriptionData.subscription[i].slotId,
                                    subscriptionData.subscription[i].getAppIndex(),
                                    subscriptionData.subscription[i].subId,
                                    subscriptionData.subscription[i].subStatus,
                                    callback);

                            done = false;
                        }
                    }
                }
            } else {
                // If subscription mode is not set
                int numSubsciptions = 0;
                for (Subscription sub : subscriptionData.subscription) {
                    if (sub.slotId != SUBSCRIPTION_INDEX_INVALID &&
                            (sub.m3gppIndex != SUBSCRIPTION_INDEX_INVALID ||
                             sub.m3gpp2Index != SUBSCRIPTION_INDEX_INVALID)) {
                        numSubsciptions++;
                    }
                }

                Log.d(LOG_TAG, "Calling setSubscriptionMode with numSubsciptions = " +
                        numSubsciptions);

                Message callback = Message.obtain(mHandler, EVENT_SET_SUBSCRIPTION_MODE_DONE, null);
                mCi[0].setSubscriptionMode(numSubsciptions, callback);
                mSetSubscriptionMode = false;
                done = false;
            }

            if(done) {
                sendSetSubscriptionCallback();
            }
        }

        private void sendSetSubscriptionCallback() {
            // Send the message back to callee with result.
            if (mSetSubCompleteMsg != null) {
                Log.d(LOG_TAG, "sendSetSubscriptionCallback");
                AsyncResult.forMessage(mSetSubCompleteMsg, mSubResult, null);
                mSetSubCompleteMsg.sendToTarget();
                mSetSubCompleteMsg = null;
            }
        }
    }

    /**
     * Sets the designated data subscription source(DDS).
     */
    public void setDataSubscription(int subscription, Message onCompleteMsg) {
        Log.d(LOG_TAG, " setDataSubscription: mCurrentDds = "
                + mCurrentDds + " new subscription = " + subscription);

        mSetDdsCompleteMsg = onCompleteMsg;

        // If there is no set dds in progress disable the current
        // active dds. Once all data connections is teared down, the data
        // connections on mQueuedDds will be enabled.
        // Call the PhoneFactory setDataSubscription API only after disconnecting
        // the current dds.
        mQueuedDds = subscription;
        if (!mDisableDdsInProgress) {
            Message allDataDisabledMsg = obtainMessage(EVENT_DISABLE_DATA_CONNECTION_DONE);
            mProxyPhones[mCurrentDds].disableDataConnectivity(allDataDisabledMsg);
            mDisableDdsInProgress = true;
        }
    }

    /** Returns the card subscriptions */
    public SubscriptionData[] getCardSubscriptions() {
        return mCardSubData;
    }

    /** Returns the current subscriptions in use.  */
    public SubscriptionData getCurrentSubscriptions() {
        return mSupplySubscription.subscriptionData;
    }

    /* Gets the default subscriptions for VOICE/SMS/DATA */
    private void getDefaultProperties(Context context) {
        boolean resetToDefault = true;

        if (TelephonyManager.isMultiSimEnabled()) {
            try {
                int voiceSubscription = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.MULTI_SIM_VOICE_CALL);
                int dataSubscription = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.MULTI_SIM_DATA_CALL);
                int smsSubscription = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.MULTI_SIM_SMS);
                int defaultSubscription = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.DEFAULT_SUBSCRIPTION);
                Log.d(LOG_TAG,"Dual Sim Settings from Settings Provider :");
                Log.d(LOG_TAG,"voiceSubscription = " + voiceSubscription
                        + " dataSubscription = " + dataSubscription
                        + " smsSubscription = " + smsSubscription
                        + " defaultSubscription = " + defaultSubscription);
                resetToDefault = false;
            } catch (SettingNotFoundException snfe) {
                Log.e(LOG_TAG, "Settings Exception Reading Voice/Sms/Data/Default Subscriptions", snfe);
            }
        }

        // Reset the Voice/Sms/Data/Default Subscriptions to default values
        // if the dsds is not enabled or if there is any exception occured
        // while reading the system properties.
        if (resetToDefault) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.MULTI_SIM_VOICE_CALL, TelephonyManager.DEFAULT_SUB);
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.MULTI_SIM_DATA_CALL, TelephonyManager.DEFAULT_SUB);
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.MULTI_SIM_SMS, TelephonyManager.DEFAULT_SUB);
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.DEFAULT_SUBSCRIPTION, TelephonyManager.DEFAULT_SUB);
        }
    }

    /** Result of the setDataSubscription */
    public enum SetDdsResult {
        ERR_RADIO_NOT_AVAILABLE,
            ERR_GENERIC_FAILURE,
            ERR_SUBSCRIPTION_NOT_AVAILABLE,
            SUCCESS;
    }

    /** Subscription Data, contains a list of subscriptions */
    public class SubscriptionData {
        public Subscription [] subscription;

        public SubscriptionData(int numSub) {
            subscription = new Subscription[numSub];
            for (int i = 0; i < numSub; i++) {
                subscription[i] = new Subscription();
            }
        }

        public int getLength() {
            if (subscription != null) {
                return subscription.length;
            }
            return 0;
        }

        public SubscriptionData copyFrom(SubscriptionData from) {
            if (from != null) {
                subscription = new Subscription[from.getLength()];
                for (int i = 0; i < from.getLength(); i++) {
                    subscription[i] = new Subscription();
                    subscription[i].copyFrom(from.subscription[i]);
                }
            }
            return this;
        }
    }

    /** Subscription, contains a information of the subscription */
    public class Subscription {
        public int slotId;         // Slot id
        public int m3gppIndex;     // Subscription index in the card for GSM
        public int m3gpp2Index;    // Subscription index in the card for CDMA
        public int subId;          // SUB 0 or SUB 1
        public int subStatus;      // ACTIVATE = 0, DEACTIVATE = 1,
                                   // ACTIVATED = 4, DEACTIVATED = 5, INVALID = 6;
        public String appId;
        public String appLabel;
        public String appType;
        public String iccId;

        public Subscription() {
            slotId = SUBSCRIPTION_INDEX_INVALID;
            m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
            m3gpp2Index = SUBSCRIPTION_INDEX_INVALID;
            subId = SUBSCRIPTION_INDEX_INVALID;
            subStatus = SUB_INVALID;
            appId = null;
            appLabel = null;
            appType = null;
            iccId = null;
        }

        public String toString() {
            return "Subscription = { "
                + "slotId = " + slotId
                + ", 3gppIndex = " + m3gppIndex
                + ", 3gpp2Index = " + m3gpp2Index
                + ", subId = " + subId
                + ", subStatus = " + subStatus
                + ", appId = " + appId
                + ", appLabel = " + appLabel
                + ", appType = " + appType
                + ", iccId = " + iccId + " }";
        }

        public boolean equals(Subscription sub) {
            if (sub != null) {
                if ((slotId == sub.slotId) && (m3gppIndex == sub.m3gppIndex) &&
                        (m3gpp2Index == sub.m3gpp2Index) && (subId == sub.subId) &&
                        (subStatus == sub.subStatus) &&
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
                m3gppIndex = from.m3gppIndex;
                m3gpp2Index = from.m3gpp2Index;
                subId = from.subId;
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

        public int getAppIndex() {
            if (this.m3gppIndex != SUBSCRIPTION_INDEX_INVALID) {
                return this.m3gppIndex;
            } else {
                return this.m3gpp2Index;
            }
        }
    }
} // End of proxy manager



