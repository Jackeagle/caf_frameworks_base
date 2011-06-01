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

/* This class is responsible for keeping all knowledge about
 * ICCs in the system. It is also used as API to get appropriate
 * applications to pass them to phone and service trackers.
 */
public class UiccManager extends Handler{
    private final static String LOG_TAG = "RIL_UiccManager";
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
    private CatService[] mCatService;

    CommandsInterface[] mCi;
    Context mContext;
    UiccCard[] mUiccCards;
    private boolean[] mRadioOn = {false, false};

    private RegistrantList mIccChangedRegistrants = new RegistrantList();

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
        Log.d(LOG_TAG, "Creating UiccManager");
        mUiccCards = new UiccCard[UiccConstants.RIL_MAX_CARDS];
        int phoneCount = TelephonyManager.getPhoneCount();
        mCi = new CommandsInterface[phoneCount];
        mCatService = new CatService[phoneCount];

        mContext = c;
        for (int i = 0; i < phoneCount; i++) {
            Integer index = new Integer(i);
            mCi[i] = ci[i];
            mCi[i].registerForOn(this,EVENT_RADIO_ON, index);
            mCi[i].registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, index);
            mCi[i].registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_UNAVAILABLE, index);
            mCatService[i] = new CatService(mCi[i], null, mContext, null, i);
        }
    }

    @Override
    public void handleMessage (Message msg) {
        AsyncResult ar;
        Integer index = getCiIndex(msg);

        switch (msg.what) {
            case EVENT_RADIO_ON:
                if (index < 0 || index >= mRadioOn.length) {
                    Log.d(LOG_TAG, "EVENT_RADIO_ON: Invalid index - " + index);
                    break;
                }
                mRadioOn[index] = true;
                Log.d(LOG_TAG, "Radio on -> Forcing sim status update on index : " + index);
                sendMessage(obtainMessage(EVENT_ICC_STATUS_CHANGED, index));
                break;
            case EVENT_ICC_STATUS_CHANGED:
                if (index < mCi.length && mRadioOn[index]) {
                    Log.d(LOG_TAG, "Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus on index"
                            + index);
                    mCi[index].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE, index));
                } else {
                    Log.d(LOG_TAG, "Received EVENT_ICC_STATUS_CHANGED while radio is not ON or index is invalid. Ignoring");
                }
                break;
            case EVENT_GET_ICC_STATUS_DONE:
                Log.d(LOG_TAG, "Received EVENT_GET_ICC_STATUS_DONE on index : " + index);
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
                if (index < 0 || index >= mRadioOn.length) {
                    Log.d(LOG_TAG, "EVENT_RADIO_OFF_OR_UNAVAILABLE: Invalid index - " + index);
                    break;
                }
                Log.d(LOG_TAG, "EVENT_RADIO_OFF_OR_UNAVAILABLE: index = " + index);
                mRadioOn[index] = false;
                disposeCard(index);
                break;
            default:
                Log.e(LOG_TAG, " Unknown Event " + msg.what);
        }

    }


    private Integer getCiIndex(Message msg) {
        AsyncResult ar;
        Integer index = new Integer(DEFAULT_INDEX);

        /*
         * The events can be come in two ways. By explicitly sending it using
         * sendMessage, in this case the user object passed is msg.obj and from
         * the CommandsInterface, in this case the user object is msg.obj.userObj
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
            Log.e(LOG_TAG,"Error getting ICC status. "
                    + "RIL_REQUEST_GET_ICC_STATUS should "
                    + "never return an error", ar.exception);
            return;
        }

        UiccCardStatusResponse status = (UiccCardStatusResponse)ar.result;

        //Update already existing card
        if (mUiccCards[index] != null && status.card != null) {
            mUiccCards[index].update(status.card, mContext, mCi[index]);
        }

        //Dispose of removed card
        if (mUiccCards[index] != null && status.card == null) {
            mUiccCards[index].dispose();
            mUiccCards[index] = null;
        }

        //Create new card
        if (mUiccCards[index] == null && status.card != null) {
            mUiccCards[index] = new UiccCard(this, status.card, mContext, mCi[index], mCatService[index]);
        }

        Log.d(LOG_TAG, "Notifying IccChangedRegistrants");
        mIccChangedRegistrants.notifyRegistrants();
    }

    private synchronized void disposeCard(int index) {
        if ((index < mUiccCards.length) &&
                (mUiccCards[index] != null)) {
             Log.d(LOG_TAG, "Disposing card " + index);
            mUiccCards[index].dispose();
            mUiccCards[index] = null;
        }
    }

    public void triggerIccStatusUpdate(Object onComplete) {
        sendMessage(obtainMessage(EVENT_ICC_STATUS_CHANGED, onComplete));
    }

    public boolean isCardFaulty(int slotId) {
        if (slotId < 0 || slotId >= mUiccCards.length) {
            return false;
        }

        if ((mUiccCards[slotId] != null) && (mUiccCards[slotId].getCardState() == CardState.ERROR)) {
            Log.d(LOG_TAG, "Card is faulty");
            return true;
        }

        return false;
    }

    public synchronized UiccCard getIccCard(int index) {
        return mUiccCards[index];
    }

    public synchronized UiccCard[] getIccCards() {
        // Return cloned array since we don't want to give out reference
        // to internal data structure.
        return mUiccCards.clone();
    }

    //Gets application based on slotId and appId
    public synchronized UiccCardApplication getApplication(int slotId, int appId) {
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

    //Gets CatService for the slotId specified.
    public CatService getCatService(int slotId) {
        return mCatService[slotId];
    }

    //Notifies when any of the cards' STATE changes (or card gets added or removed)
    public void registerForIccChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);
        synchronized (mIccChangedRegistrants) {
            mIccChangedRegistrants.add(r);
        }
        //Notify registrants right after registering, so that it will get the latest ICC status,
        //otherwise which may not happen until there is an actual change in ICC status.
        r.notifyRegistrant();
    }
    public void unregisterForIccChanged(Handler h) {
        synchronized (mIccChangedRegistrants) {
            mIccChangedRegistrants.remove(h);
        }
    }
}
