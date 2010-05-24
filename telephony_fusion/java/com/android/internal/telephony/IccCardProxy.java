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

import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.CommandsInterface.RadioTechnologyFamily;
import com.android.internal.telephony.UiccConstants.AppType;
import com.android.internal.telephony.UiccConstants.AppState;
import com.android.internal.telephony.UiccConstants.CardState;
import com.android.internal.telephony.UiccConstants.PersoSubState;
import com.android.internal.telephony.UiccManager.AppFamily;

/*
 * The Phone App UI and the external world assumes that there is only one icc card,
 * and one icc application available at a time. But the Uicc Manager can handle
 * multiple instances of icc objects. This class implements the icc interface to expose
 * the  first application on the first icc card, so that external apps wont break.
 */

public class IccCardProxy extends Handler implements IccCard {

    private static final String LOG_TAG = "IccCardProxy";

    private static final int EVENT_ICC_CHANGED = 1;
    private static final int EVENT_ICC_ABSENT = 2;
    private static final int EVENT_ICC_LOCKED = 3;
    private static final int EVENT_APP_READY = 4;
    private static final int EVENT_NETWORK_LOCKED = 5;
    private static final int EVENT_RECORDS_LOADED = 6;
    private static final int EVENT_IMSI_READY = 7;
    private static final int EVENT_PERSO_SUBSTATE_CHANGED = 8;

    private Context mContext;
    private CommandsInterface cm;

    private RegistrantList mAbsentRegistrants = new RegistrantList();
    private RegistrantList mPinLockedRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();

    private AppFamily mCurrentAppType = AppFamily.APP_FAM_3GPP; //default to 3gpp?
    private UiccManager mUiccManager = null;
    private UiccCard mUiccCard = null;
    private UiccCardApplication mApplication = null;
    private UiccApplicationRecords mAppRecords = null;

    public IccCardProxy(Context mContext, CommandsInterface cm) {
        super();
        this.mContext = mContext;
        this.cm = cm;

        mUiccManager = UiccManager.getInstance(mContext, cm);
        mUiccManager.registerForIccChanged(this, EVENT_ICC_CHANGED, null);

        //Force update
        sendMessage(obtainMessage(EVENT_ICC_CHANGED));
    }

    public void dispose() {
        //Cleanup icc references
        mUiccManager.unregisterForIccChanged(this);
        mUiccManager = null;
    }

    /*
     * The card application that the external world sees will be based on the
     * voice radio technology only!
     */
    public void setVoiceRadioTech(RadioTechnologyFamily mVoiceRadioFamily) {
        if (mVoiceRadioFamily == RadioTechnologyFamily.RADIO_TECH_3GPP2)
            mCurrentAppType = AppFamily.APP_FAM_3GPP2;
        else
            mCurrentAppType = AppFamily.APP_FAM_3GPP;

        sendMessage(obtainMessage(EVENT_ICC_CHANGED));
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_ICC_CHANGED:
                updateIccAvailability();
                break;
            case EVENT_ICC_ABSENT:
                mAbsentRegistrants.notify();
                break;
            case EVENT_ICC_LOCKED:
                mPinLockedRegistrants.notify();
                break;
            case EVENT_NETWORK_LOCKED:
                mNetworkLockedRegistrants.notify();
                break;
            case EVENT_APP_READY:
                broadcastIccStateChangedIntent(IccCard.INTENT_VALUE_ICC_READY, null);
                break;
            case EVENT_RECORDS_LOADED:
                broadcastIccStateChangedIntent(IccCard.INTENT_VALUE_ICC_LOADED, null);
                break;
            case EVENT_IMSI_READY:
                broadcastIccStateChangedIntent(IccCard.INTENT_VALUE_ICC_IMSI, null);
                break;
            case EVENT_PERSO_SUBSTATE_CHANGED:
                if (mApplication != null) {
                    broadcastPersoSubState(mApplication.getPersonalizationState());
                }
                break;
            default:
                Log.e(LOG_TAG, "Unhandled message with number: " + msg.what);
                break;
        }
    }

    void updateIccAvailability() {

        UiccCardApplication newApplication = mUiccManager.getCurrentApplication(mCurrentAppType);

        if (mApplication != newApplication) {
            if (mApplication != null) {
                mApplication.unregisterForUnavailable(this);
                unregisterUiccCardEvents();
                mApplication = null;
                mUiccCard = null;
                mAppRecords = null;
            }
            if (newApplication != null) {
                mApplication = newApplication;
                /*
                 * TODO : fusion - study this. we request the card in which the
                 * requested application is present. So, if we have a SIM
                 * inserted when we are camped on CDMA, then mUicccCard will be
                 * null as SIM will not have 3GPP2 application present.
                 */
                mUiccCard = newApplication.getCard();
                mAppRecords = newApplication.getApplicationRecords();
                registerUiccCardEvents();
                updateStateProperty();
            }
        }
    }

    private void unregisterUiccCardEvents() {
        mApplication.unregisterForReady(this);
        mApplication.unregisterForLocked(this);
        mApplication.unregisterForNetworkLocked(this);
        mApplication.unregisterForPersoSubstate(this);
        mUiccCard.unregisterForAbsent(this);
        mAppRecords.unregisterForImsiReady(this);
        mAppRecords.unregisterForRecordsLoaded(this);
    }

    private void registerUiccCardEvents() {
        mApplication.registerForReady(this, EVENT_APP_READY, null);
        mApplication.registerForLocked(this, EVENT_ICC_LOCKED, null);
        mApplication.registerForNetworkLocked(this, EVENT_NETWORK_LOCKED, null);
        mApplication.registerForPersoSubstate(this, EVENT_PERSO_SUBSTATE_CHANGED, null);
        mUiccCard.registerForAbsent(this, EVENT_ICC_ABSENT, null);
        mAppRecords.registerForImsiReady(this, EVENT_IMSI_READY, null);
        mAppRecords.registerForRecordsLoaded(this, EVENT_RECORDS_LOADED, null);
    }

    private void updateStateProperty() {
        if (mUiccCard != null) {
            SystemProperties.set(TelephonyProperties.PROPERTY_SIM_STATE, mUiccCard.getCardState().toString());
        }
    }

    /* why do external apps need to use this? */
    public void broadcastIccStateChangedIntent(String value, String reason) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        //TODO: Fusion - How do I get phoneName here?
        intent.putExtra(VoicePhone.PHONE_NAME_KEY, "Phone");
        intent.putExtra(INTENT_KEY_ICC_STATE, value);
        intent.putExtra(INTENT_KEY_LOCKED_REASON, reason);
        Log.e(LOG_TAG, "Broadcasting intent ACTION_SIM_STATE_CHANGED " +  value
                + " reason " + reason);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE);
    }

    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
        if (mApplication != null) {
            mApplication.changeIccFdnPassword(oldPassword, newPassword, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
        if (mApplication != null) {
            mApplication.changeIccLockPassword(oldPassword, newPassword, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public State getIccCardState() {
        /*
         * TODO: fusion - what is difference between getState() and
         * getIccCardState()? No one seems to be using getIccCardState();
         * How to map CARD_PRESENT?
         */
        return getState();
    }

    public State getState() {
        // old states are as follows.
        // UNKNOWN, DONE
        // ABSENT, DONE
        // PIN_REQUIRED, DONE
        // PUK_REQUIRED, DONE
        // NETWORK_LOCKED,
        // READY, DONE
        // CARD_IO_ERROR,DONE
        // SIM_NETWORK_SUBSET_LOCKED,
        // SIM_CORPORATE_LOCKED,
        // SIM_SERVICE_PROVIDER_LOCKED,
        // SIM_SIM_LOCKED,
        // RUIM_NETWORK1_LOCKED,
        // RUIM_NETWORK2_LOCKED,
        // RUIM_HRPD_LOCKED,
        // RUIM_CORPORATE_LOCKED,
        // RUIM_SERVICE_PROVIDER_LOCKED,
        // RUIM_RUIM_LOCKED,
        // NOT_READY;

        State retState = State.UNKNOWN;
        CardState cardState = CardState.ABSENT;
        AppState appState = AppState.APPSTATE_UNKNOWN;

        if (mUiccCard != null && mApplication != null) {
            appState = mApplication.getState();
            cardState = mUiccCard.getCardState();
        }

        switch (cardState) {
            case ABSENT:
                retState = State.ABSENT;
                break;
            case ERROR:
                retState = State.CARD_IO_ERROR;
                break;
            case PRESENT:
                switch (appState) {
                    case APPSTATE_ILLEGAL:
                    case APPSTATE_UNKNOWN:
                        retState = State.UNKNOWN;
                        break;
                    case APPSTATE_READY:
                        retState = State.READY;
                        break;
                    case APPSTATE_PIN:
                        retState = State.PIN_REQUIRED;
                        break;
                    case APPSTATE_PUK:
                        retState = State.PUK_REQUIRED;
                        break;
                    case APPSTATE_SUBSCRIPTION_PERSO:
                        /* TODO: fusion - get personalization substate from uicc */
                        retState = State.UNKNOWN;
                        break;
                    case APPSTATE_DETECTED:
                        /* TODO: fusion - ???? */
                        retState = State.UNKNOWN;
                        break;
                }
        }
        return retState;
    }

    public boolean getIccFdnAvailable() {
        boolean retValue = mApplication != null ? mApplication.getIccFdnAvailable() : false;
        return retValue;
    }

    public boolean getIccFdnEnabled() {
        Boolean retValue = mApplication != null ? mApplication.getIccFdnEnabled() : false;
        return retValue;
    }

    public boolean getIccLockEnabled() {
        /* defaults to true, if ICC is absent */
        Boolean retValue = mApplication != null ? mApplication.getIccLockEnabled() : true;
        return retValue;
    }

    public int getIccPin1RetryCount() {
        int retValue = mApplication != null ? mApplication.getIccPin1RetryCount() : -1;
        return retValue;
    }

    public boolean getIccPin2Blocked() {
        /* defaults to disabled */
        Boolean retValue = mApplication != null ? mApplication.getIccPin2Blocked() : false;
        return retValue;
    }

    public int getIccPin2RetryCount() {
        int retValue = mApplication != null ? mApplication.getIccPin2RetryCount() : -1;
        return retValue;
    }

    public boolean getIccPuk2Blocked() {
        /* defaults to disabled */
        Boolean retValue = mApplication != null ? mApplication.getIccPuk2Blocked() : false;
        return retValue;
    }

    public String getServiceProviderName() {
        /* TODO - fusion */
        return null;
    }

    public boolean hasIccCard() {
        if (mUiccCard != null && mUiccCard.getCardState() == CardState.PRESENT) {
            return true;
        }
        return false;
    }

    public boolean isApplicationOnIcc(AppType type) {
        Boolean retValue = mUiccCard != null ? mUiccCard.isApplicationOnIcc(type) : false;
        return retValue;
    }

    /**
     * Notifies handler of any transition into State.ABSENT
     */
    public void registerForAbsent(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);

        mAbsentRegistrants.add(r);

        if (getState() == State.ABSENT) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForAbsent(Handler h) {
        mAbsentRegistrants.remove(h);
    }

    /**
     * Notifies handler of any transition into State.NETWORK_LOCKED
     */
    public void registerForNetworkLocked(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);

        mNetworkLockedRegistrants.add(r);

        if (getState() == State.NETWORK_LOCKED) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForNetworkLocked(Handler h) {
        mNetworkLockedRegistrants.remove(h);
    }

    /**
     * Notifies handler of any transition into State.isPinLocked()
     */
    public void registerForLocked(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);

        mPinLockedRegistrants.add(r);

        if (getState().isPinLocked()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForLocked(Handler h) {
        mPinLockedRegistrants.remove(h);
    }

    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
        if (mApplication != null) {
            mApplication.setIccFdnEnabled(enabled, password, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
        if (mApplication != null) {
            mApplication.setIccLockEnabled(enabled, password, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
        if (mApplication != null) {
            mApplication.supplyNetworkDepersonalization(pin, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public void supplyPin(String pin, Message onComplete) {
        if (mApplication != null) {
            mApplication.supplyPin(pin, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public void supplyPin2(String pin2, Message onComplete) {
        if (mApplication != null) {
            mApplication.supplyPin2(pin2, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public void supplyPuk(String puk, String newPin, Message onComplete) {
        if (mApplication != null) {
            mApplication.supplyPuk(puk, newPin, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        if (mApplication != null) {
            mApplication.supplyPuk(puk2, newPin2, onComplete);
        } else if (onComplete != null) {
            Exception e = new RuntimeException("ICC card is absent.");
            AsyncResult.forMessage(onComplete).exception = e;
            onComplete.sendToTarget();
            return;
        }
    }

    private void broadcastPersoSubState(PersoSubState state) {
        switch (state) {
            case PERSOSUBSTATE_UNKNOWN:
            case PERSOSUBSTATE_IN_PROGRESS:
            case PERSOSUBSTATE_READY:
                return;
            case PERSOSUBSTATE_SIM_NETWORK:
                Log.e(LOG_TAG, "Notify SIM network locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                        INTENT_VALUE_LOCKED_NETWORK);
                break;
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET:
                Log.e(LOG_TAG, "Notify SIM network Subset locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_NETWORK_SUBSET);
                break;
            case PERSOSUBSTATE_SIM_CORPORATE:
                Log.e(LOG_TAG, "Notify SIM Corporate locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_CORPORATE);
                break;
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER:
                Log.e(LOG_TAG, "Notify SIM Service Provider locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_SERVICE_PROVIDER);
                break;
            case PERSOSUBSTATE_SIM_SIM:
                Log.e(LOG_TAG, "Notify SIM SIM locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_SIM);
                break;
            case PERSOSUBSTATE_SIM_NETWORK_PUK:
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK:
            case PERSOSUBSTATE_SIM_CORPORATE_PUK:
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK:
            case PERSOSUBSTATE_SIM_SIM_PUK:
                Log.e(LOG_TAG, "This Personalization substate is not handled: " + state);
                break;
            case PERSOSUBSTATE_RUIM_NETWORK1:
                Log.e(LOG_TAG, "Notify RUIM network1 locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_RUIM_NETWORK1);
                break;
            case PERSOSUBSTATE_RUIM_NETWORK2:
                Log.e(LOG_TAG, "Notify RUIM network2 locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_RUIM_NETWORK2);
                break;
            case PERSOSUBSTATE_RUIM_HRPD:
                Log.e(LOG_TAG, "Notify RUIM hrpd locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_RUIM_HRPD);
                break;
            case PERSOSUBSTATE_RUIM_CORPORATE:
                Log.e(LOG_TAG, "Notify RUIM Corporate locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_RUIM_CORPORATE);
                break;
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER:
                Log.e(LOG_TAG, "Notify RUIM Service Provider locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_RUIM_SERVICE_PROVIDER);
                break;
            case PERSOSUBSTATE_RUIM_RUIM:
                Log.e(LOG_TAG, "Notify RUIM RUIM locked.");
                broadcastIccStateChangedIntent(INTENT_VALUE_ICC_LOCKED,
                      INTENT_VALUE_LOCKED_RUIM_RUIM);
                break;
            case PERSOSUBSTATE_RUIM_NETWORK1_PUK:
            case PERSOSUBSTATE_RUIM_NETWORK2_PUK:
            case PERSOSUBSTATE_RUIM_HRPD_PUK:
            case PERSOSUBSTATE_RUIM_CORPORATE_PUK:
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK:
            case PERSOSUBSTATE_RUIM_RUIM_PUK:
            default:
                Log.e(LOG_TAG, "This Personalization substate is not handled: " + state);
                break;
        }
    }
}
