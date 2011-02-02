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

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.util.Log;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.UiccConstants.CardState;
import com.android.internal.telephony.cat.CatService;

/* This class will be responsible for keeping all knowledge about
 * ICCs in the system. It will also be used as API to get appropriate
 * applications to pass them to phone and service trackers.
 */
public class UiccManager extends Handler{
    public enum AppFamily {
        APP_FAM_3GPP,
        APP_FAM_3GPP2;
    }

    private static UiccManager mInstance;

    private final int DEFAULT_INDEX = 0;
    private static final int EVENT_RADIO_ON = 1;
    private static final int EVENT_ICC_STATUS_CHANGED = 2;
    private static final int EVENT_GET_ICC_STATUS_DONE = 3;
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 4;
    public static final int SUBSCRIPTION_INDEX_INVALID = 99999;

    private String mLogTag = "RIL_UiccManager";
    CommandsInterface[] mCi;
    Context mContext;
    UiccCard[] mUiccCards;
    private boolean mRadioOn = false;

    private RegistrantList mIccChangedRegistrants = new RegistrantList();
    private CatService mCatService;

    public static UiccManager getInstance(Context c, CommandsInterface[] ci) {
        if (mInstance == null) {
            mInstance = new UiccManager(c, ci);
        } else {
            mInstance.mCi = ci;
            mInstance.mContext = c;
        }
        return mInstance;
    }

    public static UiccManager getInstance() {
        if (mInstance == null) {
            return null;
        } else {
            return mInstance;
        }
    }

    private UiccManager(Context c, CommandsInterface[] ci) {
        Log.d(mLogTag, "Creating UiccManager");
        mUiccCards = new UiccCard[UiccConstants.RIL_MAX_CARDS];
        int phoneCount = TelephonyManager.getPhoneCount();
        mCi = new CommandsInterface[phoneCount];

        mContext = c;
        for (int i = 0; i < phoneCount; i++) {
            Integer index = new Integer(i);
            mCi[i] = ci[i];
            mCi[i].registerForOn(this,EVENT_RADIO_ON, index);
            mCi[i].registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, index);
            mCi[i].registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_UNAVAILABLE, index);

            mCatService = CatService.getInstance(mCi[i], null, mContext, null, null);
        }
    }

    @Override
    public void handleMessage (Message msg) {
        AsyncResult ar;
        Integer index = getCiIndex(msg);

        switch (msg.what) {
            case EVENT_RADIO_ON:
                mRadioOn = true;
                Log.d(mLogTag, "Radio on -> Forcing sim status update on index : " + index);
                sendMessage(obtainMessage(EVENT_ICC_STATUS_CHANGED, index));
                break;
            case EVENT_ICC_STATUS_CHANGED:
                if (index < mCi.length && mRadioOn) {
                    Log.d(mLogTag, "Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus on index"
                            + index);
                    mCi[index].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE, index));
                } else {
                    Log.d(mLogTag, "Received EVENT_ICC_STATUS_CHANGED while radio is not ON or index is invalid. Ignoring");
                }
                break;
            case EVENT_GET_ICC_STATUS_DONE:
                Log.d(mLogTag, "Received EVENT_GET_ICC_STATUS_DONE on index : " + index);
                ar = (AsyncResult)msg.obj;

                onGetIccCardStatusDone(ar, index);

                //If UiccManager was provided with a callback when icc status update
                //was triggered - now is the time to call it.
                if (ar.userObj != null && ar.userObj instanceof AsyncResult) {
                    AsyncResult internalAr = (AsyncResult)ar.userObj;
                    if (internalAr.userObj != null &&
                            internalAr.userObj instanceof Message) {
                        Message onComplete = (Message)internalAr.userObj;
                        if (onComplete != null) {
                            onComplete.sendToTarget();
                        }
                    }
                } else if (ar.userObj != null && ar.userObj instanceof Message) {
                    Message onComplete = (Message)ar.userObj;
                    onComplete.sendToTarget();
                }
                break;
            case EVENT_RADIO_OFF_OR_UNAVAILABLE:
                Log.d(mLogTag, "EVENT_RADIO_OFF_OR_UNAVAILABLE: index = " + index);
                mRadioOn = false;
                disposeCard(index);
                break;
            default:
                Log.e(mLogTag, " Unknown Event " + msg.what);
        }

    }


    private Integer getCiIndex(Message msg) {
        AsyncResult ar;
        Integer index = new Integer(DEFAULT_INDEX);

        /*
         * The events can be come in two ways. By explicitly sending it using
         * sendMessage, in this case the user object passed is msg.obj and from
         * the CommandsInterface, in this case the user object is msg.onj.userObj
         */
        if (msg != null) {
            if (msg.obj != null && msg.obj instanceof Integer) {
                index = (Integer)msg.obj;
            } else if(msg.obj != null && msg.obj instanceof AsyncResult) {
                ar = (AsyncResult)msg.obj;
                if (ar.userObj != null && ar.userObj instanceof Integer) {
                    index = (Integer)ar.userObj;
                }
            }
        }

        return index;
    }




    private synchronized void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Log.e(mLogTag,"Error getting ICC status. "
                    + "RIL_REQUEST_GET_ICC_STATUS should "
                    + "never return an error", ar.exception);
            return;
        }

        UiccCardStatusResponse status = (UiccCardStatusResponse)ar.result;
        boolean cardStatusChanged = false;

        if (mUiccCards[index] != null && status.card != null) {
        //Update already existing card
            if (mUiccCards[index].getCardState() != status.card.card_state) {
                cardStatusChanged = true;
            }
            mUiccCards[index].update(status.card, mContext, mCi[index]);
        } else if (mUiccCards[index] != null && status.card == null) {
            //Dispose of removed card
            mUiccCards[index].dispose();
            mUiccCards[index] = null;
            cardStatusChanged = true;
        } else if (mUiccCards[index] == null && status.card != null) {
            //Create new card
            mUiccCards[index] = new UiccCard(this, status.card, mContext, mCi[index]);
            cardStatusChanged = true;
        }

        if (cardStatusChanged) {
            Log.d(mLogTag, "Notifying IccChangedRegistrants");
            mIccChangedRegistrants.notifyRegistrants();
        }
    }

    private synchronized void disposeCard(int index) {
        if ((index < mUiccCards.length) &&
                (mUiccCards[index] != null)) {
             Log.d(mLogTag, "Disposing card " + index);
            mUiccCards[index].dispose();
            mUiccCards[index] = null;
        }
    }

    public void triggerIccStatusUpdate(Object onComplete) {
        sendMessage(obtainMessage(EVENT_ICC_STATUS_CHANGED, onComplete));
    }

    public synchronized UiccCard getIccCard(int index) {
        return mUiccCards[index];
    }

    public synchronized UiccCard[] getIccCards() {
        ArrayList<UiccCard> cards = new ArrayList<UiccCard>();
        for (UiccCard c: mUiccCards) {
            //present and absent both cards are returned.
            if (c != null && (c.getCardState() == CardState.PRESENT
                        || c.getCardState() == CardState.ABSENT)) {
                cards.add(c);
            }
        }
        Log.d(mLogTag, "Number of cards = " + cards.size());
        UiccCard arrayCards[] = new UiccCard[cards.size()];
        arrayCards = (UiccCard[])cards.toArray(arrayCards);
        return arrayCards;
    }

    /*
     * This Function gets the UiccCard at the index in case of
     * the card is present and it has any applications or the
     * card is absent.  Otherwise retrun null.
     */
    public synchronized UiccCard getCard(int index) {
        UiccCard card = mUiccCards[index];
        if (card != null &&
            ((card.getCardState() == CardState.PRESENT &&
              card.getNumApplications() > 0) ||
             card.getCardState() == CardState.ABSENT)) {
            return card;
        }
        return null;
    }

    //Gets first 3gpp Application Index
    public int getFirst3gppAppIndex(int slotId) {
        if (slotId >= 0 && slotId < mUiccCards.length) {
            UiccCard c = mUiccCards[slotId];
            if (c != null && (c.getCardState() == CardState.PRESENT)) {
                int[] subscriptions;
                subscriptions = c.getSubscription3gppAppIndex();
                if (subscriptions != null && subscriptions.length > 0) {
                    return subscriptions[0];
                } else {
                    return SUBSCRIPTION_INDEX_INVALID;
                }
            }
        }
        return SUBSCRIPTION_INDEX_INVALID;
    }

    //Gets first 3gpp2 Application Index
    public int getFirst3gpp2AppIndex(int slotId) {
        if (slotId >= 0 && slotId < mUiccCards.length) {
            UiccCard c = mUiccCards[slotId];
            if (c != null && (c.getCardState() == CardState.PRESENT)) {
                int[] subscriptions;
                subscriptions = c.getSubscription3gpp2AppIndex();
                if (subscriptions != null && subscriptions.length > 0) {
                    return subscriptions[0];
                } else {
                    return SUBSCRIPTION_INDEX_INVALID;
                }
            }
        }
        return SUBSCRIPTION_INDEX_INVALID;
    }

    //Gets current application based on slotId and appId
    public synchronized UiccCardApplication getApplication(int slotId, int appId) {

        Log.d(mLogTag, "getapplication slot id = "+ slotId + "appid = "+ appId);
        if (slotId >= 0 && slotId < mUiccCards.length) {
            UiccCard c = mUiccCards[slotId];
            if (c != null && (c.getCardState() == CardState.PRESENT) &&
                (appId >= 0 && appId < c.getNumApplications())) {
                UiccCardApplication app = c.getUiccCardApplication(appId);
                return app;
            }
        }
        return null;
    }

    //Notifies when any of the cards' STATE changes (or card gets added or removed)
    public void registerForIccChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);
        synchronized (mIccChangedRegistrants) {
            mIccChangedRegistrants.add(r);
        }
        //Notify registrants soon after registering, so that it will get the latest ICC status,		
        //otherwise which may not happen until there is an actual change in ICC status.		
        r.notifyRegistrant();
    }
    public void unregisterForIccChanged(Handler h) {
        synchronized (mIccChangedRegistrants) {
            mIccChangedRegistrants.remove(h);
        }
    }
}
