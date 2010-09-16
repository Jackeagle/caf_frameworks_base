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
import com.android.internal.telephony.UiccConstants.CardState;
import com.android.internal.telephony.UiccConstants.AppType;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandException;
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

public class ProxyManager extends Handler {
    static final String LOG_TAG = "PROXY";
    static final int SUB_ACTIVATE = 0;
    static final int SUB_DEACTIVATE = 1;
    static final int NUM_SUBSCRIPTIONS = 2;
    static final int EVENT_SET_UICC_SUBSCRIPTION_DONE = 1;
    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 2;
    static final int EVENT_ICC_CHANGED = 3;
    static final int EVENT_SUBSCRIPTION_SET_DONE = 4;
    static final int EVENT_GET_ICC_STATUS_DONE = 5;
    static final int EVENT_SET_SUBSCRIPTION_MODE_DONE = 6;
    static final int EVENT_DISABLE_DATA_CONNECTION_DONE = 7;
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
    private static String[][] cardSubscriptions = null;
    private boolean uiccSubSet = false;
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
            mCi = ci;

            mUiccManager.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
            getUserPreferredSubs();
            supplySubscription = this.new SupplySubscription(mContext);

            // Register for intent broadcasts.
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            mReceiver = new PhoneAppBroadcastReceiver();
            mContext.registerReceiver(mReceiver, intentFilter);

            // get the current active dds
            currentDds = PhoneFactory.getDataSubscription(context);
            Log.d(LOG_TAG, "In ProxyManager constructor current active dds is:" + currentDds);
        }
    }

    public void getUserPreferredSubs() {

        userPrefSubs = new String [NUM_SUBSCRIPTIONS];
        for(int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
            userPrefSubs[i] = Settings.System.getString(mContext.getContentResolver(),Settings.System.USER_PREFERRED_SUBS[i]);
            Log.d(LOG_TAG, "userPrefSubs:"+ userPrefSubs[i]);
            if (userPrefSubs[i] == null) {
                Settings.System.putString(mContext.getContentResolver(),Settings.System.USER_PREFERRED_SUBS[i],userDefaultSubs[i] );
                userPrefSubs[i] = userDefaultSubs[i];
                Log.d(LOG_TAG, "userPrefSubs:"+ userPrefSubs[i]);
            }
        }
    }


    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        Message     onComplete;

        switch(msg.what) {
            case EVENT_ICC_CHANGED: {
                        Log.d(LOG_TAG, "ProxyManager EVENT_ICC_CHANGED");
                if (!uiccSubSet)
                    checkCardStatus();
                    break;
            }

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

    public void checkCardStatus() {

        mUiccCards = mUiccManager.getIccCards();
        if(mUiccCards.length == UiccConstants.RIL_MAX_CARDS) {

            if (mUiccCards[0] != null && mUiccCards[1] != null) {
                Log.d(LOG_TAG, ":  card 1 state: "+mUiccCards[0].getCardState());
                Log.d(LOG_TAG, ":  card 2 state: "+mUiccCards[1].getCardState());

                /* Card status to be processed if
                  card0 status is present and card1 status is present
                  card0 status is present and card1 status is absent
                  card0 status is absent and card1 status is present
                */

                if (mUiccCards[0].getCardState() == CardState.PRESENT && mUiccCards[1].getCardState() == CardState.PRESENT) {
                    Log.d(LOG_TAG, "Both cards present");
                    ProcessCardStatus();

                } else if ((mUiccCards[0].getCardState() == CardState.PRESENT &&
                          mUiccCards[1].getCardState() == CardState.ABSENT) ||
                         (mUiccCards[1].getCardState() == CardState.PRESENT &&
                         mUiccCards[0].getCardState() == CardState.ABSENT)) {
                    Log.d(LOG_TAG, "one card present and one card absent");
                    ProcessCardStatus();

                } else if ((mUiccCards[0].getCardState() == CardState.PRESENT &&
                          mUiccCards[1].getCardState() == CardState.ERROR) ||
                         (mUiccCards[1].getCardState() == CardState.PRESENT &&
                          mUiccCards[0].getCardState() == CardState.ERROR)) {
                   //currently there is a limitation from UIM that with ffa if card status is absent it is sent as error
                    Log.d(LOG_TAG, "one card present and one card error");
                    ProcessCardStatus();

                } else {
                   Log.d(LOG_TAG, "Not Valid Card status");
                }
            }
        }
    }

    void ProcessCardStatus() {
        cardSubscriptions = new String [UiccConstants.RIL_MAX_CARDS][UiccConstants.RIL_CARD_MAX_APPS];
        int numApps = 0;

        /* Loop through list of cards and list of applications and store it in a two dimensional array */
        for (int cardIndex = 0; cardIndex < UiccConstants.RIL_MAX_CARDS; cardIndex++) {
            CardState cardstate = mUiccCards[cardIndex].getCardState();
            if (cardstate == CardState.PRESENT) {
                numApps = mUiccCards[cardIndex].getNumApplications();
                Log.d(LOG_TAG, "num of apps"+ numApps);

                for (int appIndex = 0; appIndex < numApps ; appIndex++ ) {
                    Log.d(LOG_TAG, ":  appIndex "+ appIndex);
                    UiccCardApplication mUiccCardApplication = mUiccCards[cardIndex].getUiccCardApplication(appIndex);
                    AppType type = mUiccCardApplication.getType();
                    String s = toString(type);
                    //Apps like ISIM etc are treated as UNKNOWN apps, to be discarded
                    if (! s.equals("UNKNOWN")) {
                        Log.d(LOG_TAG, "app type:"+ s);
                        cardSubscriptions[cardIndex][appIndex] =  s;
                    } else {
                      Log.d(LOG_TAG, "UNKNOWN APP");
                    }
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

    public void matchSubscriptions() {
        int cardIndex = 0;
        int matchedIndex = -1;
        int matchedCard = -1;
        int[] subIndexMatched ={-1, -1};
        int[] subSlotMatched = {-1, -1};

        /* For each user preferred subscription loop through all the cards and all the application in each card
         * to find a match. Once a match is found store the slot id, application id where the match occured.
         */
        for(int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
           Log.d(LOG_TAG, "Index:"+i);
           for ( cardIndex = 0; cardIndex < UiccConstants.RIL_MAX_CARDS; cardIndex++) {
               Log.d(LOG_TAG, "cardIndex: "+cardIndex);

               if ( mUiccCards[cardIndex] != null ) {
                   CardState cardstate = mUiccCards[cardIndex].getCardState();

                   if ( cardstate == CardState.PRESENT) {
                       int subIndex = 0;

                       for (subIndex = 0; subIndex < UiccConstants.RIL_CARD_MAX_APPS; subIndex++) {
                           Log.d(LOG_TAG, "subIndex:" +subIndex);

                           if ( cardSubscriptions[cardIndex][subIndex] != null ) {
                               Log.d(LOG_TAG, "card sub :"+cardSubscriptions[cardIndex][subIndex]+
                                                            ", userPrefSub[i]="+userPrefSubs[i] );

                               //get user preferred subscription
                               if ((cardSubscriptions[cardIndex][subIndex].equals( userPrefSubs[i])) &&
                                  (matchedCard != cardIndex || matchedIndex != subIndex)) {

                                  /* matchedCard != cardIndex, matchedIndex != subIndex check is
                                   * introduced to avoid the same application gets matched both times (for both subscriptions)
                                   */
                                   matchedCard = cardIndex;
                                   matchedIndex = subIndex;
                                   subIndexMatched[i] = subIndex;   //matched app index
                                   subSlotMatched[i] = cardIndex;   //matched slot id
                                   Log.d(LOG_TAG, "matched, cardIndex"+cardIndex+"subIndex:"+subIndex+"userPrefSub:"+userPrefSubs[i]);
                                   Log.d(LOG_TAG, "matchedCard"+matchedCard+",matchedIndex"+subIndex);
                                   break;

                               } else {
                                   Log.d(LOG_TAG, "not matched: subIndex, cardIndex:"+subIndex+","+cardIndex );
                               }

                           }
                       }

                       // user preferred subscription is matched with from one of the cards
                       if (subIndex < UiccConstants.RIL_CARD_MAX_APPS) {
                           Log.d(LOG_TAG,"subscription is matched for cardIndex:"+cardIndex);
                           break;
                       }
                    }

                }
            }
        }

        Log.d(LOG_TAG, "subIndexMatched[0]"+subIndexMatched[0]+"subIndexMatched[1]:"+subIndexMatched[1]);
        Log.d(LOG_TAG, "subSlotMatched[0]"+subSlotMatched[0]+"subSlotMatched[1]:"+subSlotMatched[1]);


        if (subIndexMatched[0] == -1 && subIndexMatched[1] == -1) {

            //Subscription settings do not match with the card applications
            Log.d(LOG_TAG, "PromptUserSubscription" );
            //send intent to app showing two user two cards subscriptions

            uiccSubSet = true;  //card status is processed only the first time
            promptUserSubscription(cardSubscriptions);

        } else if ((subIndexMatched[0] != -1 && subIndexMatched[1] == -1) ||
                   (subIndexMatched[0] == -1 && subIndexMatched[1] != -1)) {

            //one subscription mathced another subscription did not match then also prompt user for selection
            Log.d(LOG_TAG, "PromptUserSubscription" );
            //send intent to app showing two card subscriptions
            uiccSubSet = true;
            promptUserSubscription(cardSubscriptions);

        } else {

            Log.d(LOG_TAG, "setSubscription calling" );
            setSubscription(NUM_SUBSCRIPTIONS, subSlotMatched, subIndexMatched);
            uiccSubSet = true;

        }

    }

    public String[] setSubscription(int num, int[] slotId, int []subIndex) {

        for(int i = 0; i < num ; i++) {
            Log.d(LOG_TAG, "num"+num+", slotId="+slotId[i]+",subIndex="+subIndex[i]);

            /* if slotId[i] != -1 and subIndex[i] != -1 then that slotId,subIndex for a subscription is valid
             * and the app type of subscription should align with the phone object created.
             * USIM/SIM should have a GSM phone object created.
             * RUIM/CSIM should have a CDMA phone object created.
             * If not re-create the voice phone object accordingly.
             */

            if (slotId[i] != -1 && subIndex[i] != -1) {

                if (((cardSubscriptions[slotId[i]][subIndex[i]].equals("SIM")) ||
                    (cardSubscriptions[slotId[i]][subIndex[i]].equals("USIM"))) &&
                    (!sProxyPhone[i].getPhoneName().equals("GSM"))) {

                    Log.d(LOG_TAG, "gets New GSM phone" );
                    sProxyPhone[i].updatePhoneProxy(PhoneFactory.getGsmPhone(i));
                    mIccSmsInterfaceManager.updatePhoneObject(sProxyPhone[i].getVoicePhone(),i);
                    //when the phone object is re-created broadcast an intent so that PhoneApp is aware of it
                    Intent intent = new Intent(TelephonyIntents.ACTION_PHONE_CHANGED);
                    intent.putExtra(Phone.PHONE_SUBSCRIPTION, i);
                    ActivityManagerNative.broadcastStickyIntent(intent, null);

                } else if (((cardSubscriptions[slotId[i]][subIndex[i]].equals("RUIM")) ||
                          (cardSubscriptions[slotId[i]][subIndex[i]].equals("CSIM"))) &&
                          (!sProxyPhone[i].getPhoneName().equals("CDMA")) ) {

                     Log.d(LOG_TAG, "gets New CDMA phone" );
                     sProxyPhone[i].updatePhoneProxy(PhoneFactory.getCdmaPhone(i));
                     mIccSmsInterfaceManager.updatePhoneObject(sProxyPhone[i].getVoicePhone(),i);
                    //when the phone object is re-created broadcast an intent so that PhoneApp is aware of it
                     Intent intent = new Intent(TelephonyIntents.ACTION_PHONE_CHANGED);
                     intent.putExtra(Phone.PHONE_SUBSCRIPTION, i);
                     ActivityManagerNative.broadcastStickyIntent(intent, null);

                }
                //update the user pref settings so that next time user is not prompted of the subscriptions
                Settings.System.putString( mContext.getContentResolver(),Settings.System.USER_PREFERRED_SUBS[i],
                                                                   cardSubscriptions[slotId[i]][subIndex[i]] );
                userPrefSubs[i] = cardSubscriptions[slotId[i]][subIndex[i]];
            }
        }
        uiccSubSet = true;

        // Setting the subscription at RIL/Modem is handled through a thread. Start the thread
        // if it is not already started.
        if (!supplySubscription.isAlive()) {
            supplySubscription.start();
        }

        return supplySubscription.setSubscription(slotId, subIndex);
   }

   public void promptUserSubscription(String[][] subInfo) {

       // Send an intent to start SetSubscription.java activity to show the list of apps
       // on each card for user selection
       Log.d(LOG_TAG, "Prompt user subscriptin" );
       Intent setSubscriptionIntent = new Intent(Intent.ACTION_MAIN);
       setSubscriptionIntent.setClassName("com.android.phone",
                "com.android.phone.SetSubscription");
        setSubscriptionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle subscrInfo = new Bundle();
        if (subInfo[0] != null)
        subscrInfo.putStringArray(INTENT_VALUE_SUBSCR_INFO_1, subInfo[0]);
        if (subInfo[1] != null)
        subscrInfo.putStringArray(INTENT_VALUE_SUBSCR_INFO_2, subInfo[1]);
        setSubscriptionIntent.putExtras(subscrInfo);
        mContext.startActivity(setSubscriptionIntent);

    }

    public class SupplySubscription extends Thread {

        private boolean mDone = false;
        private Handler mHandler;
        private int eventsPending = 0;
        private Context  mContext;
        private String [] subResult;
        boolean isStarted = false;

        private static final int SUBSCRIPTION_SET_SUCCESS = 0;
        private static final int SUBSCRIPTION_SET_FAILED = 1;


        public class SubscriptionData {
            public class Subscription {
                public int slotId;       //slotId indicates the card slot
                public int subIndex;     //subIndex indicates the app in the card
                public int subNum;       //subNum indicates whether SUB0, SUB1
                public int subStatus;    //subStatus indicates active/de-active
            };
            int numSubscriptions;
            Subscription [] subscription;
            public SubscriptionData() {
            }
        };

        SubscriptionData subscriptionData;
        SubscriptionData prevSubscriptionData;

        public SupplySubscription(Context context) {

            mContext = context;
            subscriptionData = new SubscriptionData();
            subscriptionData.numSubscriptions = NUM_SUBSCRIPTIONS;
            subscriptionData.subscription = new SubscriptionData.Subscription[NUM_SUBSCRIPTIONS];
            eventsPending = 0;
            subResult = new String[NUM_SUBSCRIPTIONS];
            prevSubscriptionData = new SubscriptionData();
            prevSubscriptionData.numSubscriptions = NUM_SUBSCRIPTIONS;
            prevSubscriptionData.subscription = new SubscriptionData.Subscription[NUM_SUBSCRIPTIONS];

            // TODO DSDS
            // SupplySubscription logic will be enhanced as part of handling swapping of sim cards use case handled
            // At that time all use cases are taken care.
            for ( int i = 0; i < NUM_SUBSCRIPTIONS; i++) {
                subscriptionData.subscription[i] = subscriptionData. new Subscription();
                subscriptionData.subscription[i].slotId = -1;
                subscriptionData.subscription[i].subIndex = -1;
                subscriptionData.subscription[i].subNum = -1;
                subscriptionData.subscription[i].subStatus = SUB_DEACTIVATE;
                prevSubscriptionData.subscription[i] = subscriptionData. new Subscription();
                prevSubscriptionData.subscription[i].slotId = 0;
                prevSubscriptionData.subscription[i].subIndex = 0;
                prevSubscriptionData.subscription[i].subNum = 0;
                prevSubscriptionData.subscription[i].subStatus = SUB_ACTIVATE;
            }
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
                                Log.d(LOG_TAG, ":  EVENT_SET_UICC_SUBSCRIPTION_DONE: on sub"+phoneIndex);
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
                                        //set subscription success, update subscription info in phone objects
                                        sProxyPhone[phoneIndex].setSubscriptionInfo(subscriptionData.subscription[phoneIndex]);
                                        Log.d(LOG_TAG, "EVENT_SET_UICC_SUBSCRIPTION_DONE success, phone index=" + phoneIndex) ;
                                    }

                                    if (eventsPending == 0) {
                                        // All the pending subscription_set responses are received now we can unblock the thread                                            // so that control can go to UI to finish the activity.
                                        mDone = true;
                                        SupplySubscription.this.notifyAll();
                                        int dataSub = PhoneFactory.getDataSubscription(mContext);
                                        Log.d(LOG_TAG, "dataSub :"+dataSub);
                                        String str = Integer.toString(dataSub);
                                        Message callback = Message.obtain(mHandler, EVENT_SET_DATA_SUBSCRIPTION_DONE,str);
                                        // Set Data Subscription preference at RIL
                                        Log.d(LOG_TAG, "cmd interface setDataSubscription.....");
                                        mCi[dataSub].setDataSubscription(callback );
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

        synchronized String[] setSubscription( int[] slotId, int[] subIndex ) {

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
            // TODO DSDS
            // Currently De-activation mechanism logic is not present, as part of that implemenation all use cases are taken care.

            for (int i = 0; i < NUM_SUBSCRIPTIONS; i++) {

                Log.d(LOG_TAG, "supply subscriptin, slotId[i]:"+ slotId[i]+",subIndex[i]:"
                                                           +subIndex[i]+",status:"+SUB_ACTIVATE );
                if (slotId[i] != -1 && subIndex[i] != -1) {

                    if (subscriptionData.subscription[i].slotId != prevSubscriptionData.subscription[i].slotId &&
                        subscriptionData.subscription[i].subIndex != prevSubscriptionData.subscription[i].subIndex &&
                        subscriptionData.subscription[i].subNum != prevSubscriptionData.subscription[i].subNum &&
                        subscriptionData.subscription[i].subStatus != prevSubscriptionData.subscription[i].subStatus ) {

                        subscriptionData.subscription[i].slotId = slotId[i];
                        subscriptionData.subscription[i].subIndex = subIndex[i];
                        subscriptionData.subscription[i].subNum = i;
                        subscriptionData.subscription[i].subStatus = SUB_ACTIVATE;
                    }
                }
            }
            prevSubscriptionData = subscriptionData;
            Log.d(LOG_TAG, "subscriptionData.numSubscriptions:"+subscriptionData.numSubscriptions);
            Message callback = Message.obtain(mHandler, EVENT_SET_SUBSCRIPTION_MODE_DONE, null);
            mCi[0].setSubscriptionMode(subscriptionData.numSubscriptions, callback);
            mDone = false;

            while (!mDone) {
                try {
                    wait();
                    Log.d(LOG_TAG, "waiting subscription done");
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
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
                    uiccSubSet = true; //flag which takes care of processing/Handling of card status
                                                  //card status is processed only the first time
                } else {
                    //registering for card status indication when moved out of airplane mode to get card status once again
                    mUiccManager.registerForIccChanged(mProxyManager, EVENT_ICC_CHANGED, null);
                    uiccSubSet = false;
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
}//end of proxy manager



