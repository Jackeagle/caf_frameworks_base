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

import com.android.internal.telephony.UiccConstants.AppState;
import com.android.internal.telephony.UiccConstants.AppType;
import com.android.internal.telephony.UiccConstants.PersoSubState;
import com.android.internal.telephony.UiccConstants.PinState;
import com.android.internal.telephony.cdma.RuimFileHandler;
import com.android.internal.telephony.cdma.RuimRecords;
import com.android.internal.telephony.gsm.SIMFileHandler;
import com.android.internal.telephony.gsm.SIMRecords;
/** This class will handle PIN, PUK, etc
 * Every user of this class will be registered for Unavailable with every
 * object it gets reference to. It is the user's responsibility to unregister
 * and remove reference to object, once UNAVAILABLE callback is received.
 */
public class UiccCardApplication {
    private String mLogTag = "RIL_UiccCardApplication";
    private UiccCard mUiccCard; //parent

    private int mSlotId; //Icc slot number of the Icc this app resides on

    private UiccApplicationRecords mUiccApplicationRecords;
    private AppState      mAppState;
    private AppType       mAppType;
    private PersoSubState mPersoSubState;
    private String        mAid;
    private String        mAppLabel;
    private boolean       mPin1Replaced;
    private PinState      mPin1State;
    private PinState      mPin2State;

    private IccFileHandler mIccFh;

    private boolean mDestroyed = false; //set to true once this App is commanded to be disposed of.

    private CommandsInterface mCi;
    private Context mContext;

    private RegistrantList mReadyRegistrants = new RegistrantList();
    private RegistrantList mUnavailableRegistrants = new RegistrantList();
    private RegistrantList mLockedRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();
    private RegistrantList mPersoSubstateRegistrants = new RegistrantList();

    UiccCardApplication(UiccCard uiccCard, UiccCardStatusResponse.CardStatus.AppStatus as, UiccRecords ur, Context c, CommandsInterface ci) {
        mSlotId = uiccCard.getSlotId();
        mUiccCard = uiccCard;
        mAppState = as.app_state;
        mAppType = as.app_type;
        mPersoSubState = as.perso_substate;
        mAid = as.aid;
        mAppLabel = as.app_label;
        mPin1Replaced = (as.pin1_replaced != 0);
        mPin1State = as.pin1;
        mPin2State = as.pin2;

        mContext = c;
        mCi = ci;

        mIccFh = createUiccFileHandler(as.app_type);
        mUiccApplicationRecords = createUiccApplicationRecords(as.app_type, ur, mContext, mCi);
    }

    void update (UiccCardStatusResponse.CardStatus.AppStatus as, UiccRecords ur, Context c, CommandsInterface ci) {
        if (mDestroyed) {
            Log.e(mLogTag, "Application updated after destroyed! Fix me!");
            return;
        }
        mContext = c;
        mCi = ci;

        if (as.app_type != mAppType) {
            mUiccApplicationRecords.dispose();
            mUiccApplicationRecords = createUiccApplicationRecords(as.app_type, ur, c, ci);
            mAppType = as.app_type;
        }

        if (mPersoSubState != as.perso_substate) {
            mPersoSubState = as.perso_substate;
            notifyNetworkLockedRegistrants();
            notifyPersoSubstateRegistrants();
        }

        mAid = as.aid;
        mAppLabel = as.app_label;
        mPin1Replaced = (as.pin1_replaced != 0);
        mPin1State = as.pin1;
        mPin2State = as.pin2;
        if (mAppState != as.app_state) {
            mAppState = as.app_state;
            notifyLockedRegistrants();
            notifyReadyRegistrants();
        }
    }

    synchronized void dispose() {
        mDestroyed = true;
        mUiccApplicationRecords.dispose();
        mUiccApplicationRecords = null;
        mIccFh = null;
        notifyUnavailableRegistrants();
    }

    private UiccApplicationRecords createUiccApplicationRecords(AppType type, UiccRecords ur, Context c, CommandsInterface ci) {
        if (type == AppType.APPTYPE_USIM || type == AppType.APPTYPE_SIM) {
            return new SIMRecords(this, ur, c, ci);
        } else {
            return new RuimRecords(this, ur, c, ci);
        }
    }

    private IccFileHandler createUiccFileHandler(AppType type) {
        switch (type) {
            case APPTYPE_SIM:
                return new SIMFileHandler(this, mSlotId, mAid, mCi);
            case APPTYPE_RUIM:
                return new RuimFileHandler(this, mSlotId, mAid, mCi);
            case APPTYPE_USIM:
                return new UsimFileHandler(this, mSlotId, mAid, mCi);
            case APPTYPE_CSIM:
                return new CsimFileHandler(this, mSlotId, mAid, mCi);
            default:
                return null;
        }
    }

    public AppType getType() {
        return mAppType;
    }

    public synchronized UiccApplicationRecords getApplicationRecords() {
        return mUiccApplicationRecords;
    }

    public synchronized IccFileHandler getIccFileHandler() {
        return mIccFh;
    }

    public synchronized UiccCard getCard() {
        return mUiccCard;
    }

    public AppState getState()
    {
        return mAppState;
    }

    public PersoSubState getPersonalizationState() {
        return mPersoSubState;
    }

    public PinState getPin1State() {
        if (mPin1Replaced) {
            return mUiccCard.getUniversalPinState();
        } else {
            return mPin1State;
        }
    }

    public PinState getPin2State() {
        return mPin2State;
    }

    public String getAid() {
        return mAid;
    }

    private synchronized void notifyAllRegistrants() {
        notifyUnavailableRegistrants();
        notifyLockedRegistrants();
        notifyReadyRegistrants();
        notifyNetworkLockedRegistrants();
    }

    /** Notifies specified registrant.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private synchronized void notifyUnavailableRegistrants(Registrant r) {
        if (mDestroyed) {
            if (r == null) {
                mUnavailableRegistrants.notifyRegistrants();
            } else {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
            return;
        }
    }

    private synchronized void notifyUnavailableRegistrants() {
        notifyUnavailableRegistrants(null);
    }


    /** Notifies specified registrant.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private synchronized void notifyLockedRegistrants(Registrant r) {
        if (mDestroyed) {
            return;
        }

        if (mAppState == AppState.APPSTATE_PIN ||
            mAppState == AppState.APPSTATE_PUK ||
            mAppState == AppState.APPSTATE_SUBSCRIPTION_PERSO) {
            if (mPin1State == PinState.PINSTATE_ENABLED_VERIFIED || mPin1State == PinState.PINSTATE_DISABLED) {
                Log.e(mLogTag, "Sanity check failed! APPSTATE is locked while PIN1 is not!!!");
            }
            if (r == null) {
                mLockedRegistrants.notifyRegistrants();
            } else {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    private synchronized void notifyPersoSubstateRegistrants() {
        notifyPersoSubstateRegistrants(null);
    }

    /** Notifies specified registrant.
    *
    * @param r Registrant to be notified. If null - all registrants will be notified
    */
   private synchronized void notifyPersoSubstateRegistrants(Registrant r) {
       if (mDestroyed) {
           return;
       }

       if (r == null) {
           mPersoSubstateRegistrants.notifyRegistrants();
       } else {
           r.notifyRegistrant(new AsyncResult(null, null, null));
       }
   }

   private synchronized void notifyLockedRegistrants() {
       notifyLockedRegistrants(null);
   }

    /** Notifies specified registrant.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private synchronized void notifyReadyRegistrants(Registrant r) {
        if (mDestroyed) {
            return;
        }
        if (mAppState == AppState.APPSTATE_READY) {
            if (mPin1State == PinState.PINSTATE_ENABLED_NOT_VERIFIED || mPin1State == PinState.PINSTATE_ENABLED_BLOCKED || mPin1State == PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                Log.e(mLogTag, "Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
            }
            if (r == null) {
                mReadyRegistrants.notifyRegistrants();
            } else {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    private synchronized void notifyReadyRegistrants() {
        notifyReadyRegistrants(null);
    }

    /** Notifies specified registrant.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private synchronized void notifyNetworkLockedRegistrants(Registrant r) {
        if (mPersoSubState == PersoSubState.PERSOSUBSTATE_SIM_NETWORK ||
                mPersoSubState == PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET ||
                mPersoSubState == PersoSubState.PERSOSUBSTATE_SIM_NETWORK_PUK ||
                mPersoSubState == PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK ||
                mPersoSubState == PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1 ||
                mPersoSubState == PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2 ||
                mPersoSubState == PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1_PUK ||
                mPersoSubState == PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2_PUK) {
            if (r == null) {
                mNetworkLockedRegistrants.notifyRegistrants();
            } else {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
            return;
        }
    }

    private synchronized void notifyNetworkLockedRegistrants() {
        notifyNetworkLockedRegistrants(null);
    }

    public synchronized void registerForReady(Handler h, int what, Object obj) {
        if (mDestroyed) {
            return;
        }

        Registrant r = new Registrant (h, what, obj);
        mReadyRegistrants.add(r);

        notifyReadyRegistrants(r);
    }
    public synchronized void unregisterForReady(Handler h) {
        mReadyRegistrants.remove(h);
    }

    public synchronized void registerForUnavailable(Handler h, int what, Object obj) {
        if (mDestroyed) {
            return;
        }

        Registrant r = new Registrant (h, what, obj);
        mUnavailableRegistrants.add(r);
    }
    public synchronized void unregisterForUnavailable(Handler h) {
        mUnavailableRegistrants.remove(h);
    }

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Original IccCard (with modifications)
    protected boolean mDbg;

    private boolean mDesiredPinLocked;
    private boolean mDesiredFdnEnabled;
    private boolean mIccPinLocked = true; // Default to locked
    private boolean mIccFdnEnabled = false; // Default to disabled.
                                            // Will be updated when SIM_READY.
    private boolean mIccFdnAvailable = true; // Default is enabled.
                                             // Will be updated when SIM_READY.
    private boolean mIccPin2Blocked = false; // Default to disabled.
                                             // Will be updated when sim status changes.
    private boolean mIccPuk2Blocked = false; // Default to disabled.
                                             // Will be updated when sim status changes.

    private int mPin1RetryCount = -1;
    private int mPin2RetryCount = -1;
    
    //TODO: Fusion - IccProxyCard to incorporate broadcasting
    /* The extra data for broacasting intent INTENT_ICC_STATE_CHANGE */
    static public final String INTENT_KEY_ICC_STATE = "ss";
    /* NOT_READY means the ICC interface is not ready (eg, radio is off or powering on) */
    static public final String INTENT_VALUE_ICC_NOT_READY = "NOT_READY";
    /* ABSENT means ICC is missing */
    static public final String INTENT_VALUE_ICC_ABSENT = "ABSENT";
    /* CARD_IO_ERROR means for three consecutive times there was SIM IO error */
    static public final String INTENT_VALUE_ICC_CARD_IO_ERROR = "CARD_IO_ERROR";
    /* LOCKED means ICC is locked by pin or by network */
    static public final String INTENT_VALUE_ICC_LOCKED = "LOCKED";
    /* READY means ICC is ready to access */
    static public final String INTENT_VALUE_ICC_READY = "READY";
    /* IMSI means ICC IMSI is ready in property */
    static public final String INTENT_VALUE_ICC_IMSI = "IMSI";
    /* LOADED means all ICC records, including IMSI, are loaded */
    static public final String INTENT_VALUE_ICC_LOADED = "LOADED";
    /* The extra data for broacasting intent INTENT_ICC_STATE_CHANGE */
    static public final String INTENT_KEY_LOCKED_REASON = "reason";
    /* PIN means ICC is locked on PIN1 */
    static public final String INTENT_VALUE_LOCKED_ON_PIN = "PIN";
    /* PUK means ICC is locked on PUK1 */
    static public final String INTENT_VALUE_LOCKED_ON_PUK = "PUK";
    /* NETWORK means ICC is locked on NETWORK PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_NETWORK = "SIM NETWORK";
    /* NETWORK SUBSET means ICC is locked on NETWORK SUBSET PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_NETWORK_SUBSET = "SIM NETWORK SUBSET";
    /* CORPORATE means ICC is locked on CORPORATE PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_CORPORATE = "SIM CORPORATE";
    /* SERVICE PROVIDER means ICC is locked on SERVICE PROVIDER PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_SERVICE_PROVIDER = "SIM SERVICE PROVIDER";
    /* SIM means ICC is locked on SIM PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_SIM = "SIM SIM";
    /* RUIM NETWORK1 means ICC is locked on RUIM NETWORK1 PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_RUIM_NETWORK1 = "RUIM NETWORK1";
    /* RUIM NETWORK2 means ICC is locked on RUIM NETWORK2 PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_RUIM_NETWORK2 = "RUIM NETWORK2";
    /* RUIM HRPD means ICC is locked on RUIM HRPD PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_RUIM_HRPD = "RUIM HRPD";
    /* RUIM CORPORATE means ICC is locked on RUIM CORPORATE PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_RUIM_CORPORATE = "RUIM CORPORATE";
    /* RUIM SERVICE PROVIDER means ICC is locked on RUIM SERVICE PROVIDER PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_RUIM_SERVICE_PROVIDER = "RUIM SERVICE PROVIDER";
    /* RUIM RUIM means ICC is locked on RUIM RUIM PERSONALIZATION */
    static public final String INTENT_VALUE_LOCKED_RUIM_RUIM = "RUIM RUIM";

    protected static final int EVENT_ICC_LOCKED_OR_ABSENT = 1;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 3;
    private static final int EVENT_PIN1PUK1_DONE = 4;
    protected static final int EVENT_ICC_READY = 6;
    private static final int EVENT_QUERY_FACILITY_LOCK_DONE = 7;
    private static final int EVENT_CHANGE_FACILITY_LOCK_DONE = 8;
    private static final int EVENT_CHANGE_ICC_PASSWORD_DONE = 9;
    private static final int EVENT_QUERY_FACILITY_FDN_DONE = 10;
    private static final int EVENT_CHANGE_FACILITY_FDN_DONE = 11;
    private static final int EVENT_PIN2PUK2_DONE = 13;

    protected void finalize() {
        if(mDbg) Log.d(mLogTag, "IccCard finalized");
    }

    /**
     * Notifies handler of any transition into State.NETWORK_LOCKED
     */
    public void registerForNetworkLocked(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);

        mNetworkLockedRegistrants.add(r);
        notifyNetworkLockedRegistrants(r);
    }

    public void unregisterForNetworkLocked(Handler h) {
        mNetworkLockedRegistrants.remove(h);
    }

    /**
     * Notifies handler of any changes to PersoSubstate
     */
    public void registerForPersoSubstate(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);

        mPersoSubstateRegistrants.add(r);
    }

    public void unregisterForPersoSubstate(Handler h) {
        mPersoSubstateRegistrants.remove(h);
    }

    /**
     * Notifies handler of any transition into State.isPinLocked()
     */
    public void registerForLocked(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);

        mLockedRegistrants.add(r);
        notifyLockedRegistrants(r);
    }

    public void unregisterForLocked(Handler h) {
        mLockedRegistrants.remove(h);
    }


    /**
     * Supply the ICC PIN to the ICC
     *
     * When the operation is complete, onComplete will be sent to it's
     * Handler.
     *
     * onComplete.obj will be an AsyncResult
     *
     * ((AsyncResult)onComplete.obj).exception == null on success
     * ((AsyncResult)onComplete.obj).exception != null on fail
     *
     * If the supplied PIN is incorrect:
     * ((AsyncResult)onComplete.obj).exception != null
     * && ((AsyncResult)onComplete.obj).exception
     *       instanceof com.android.internal.telephony.gsm.CommandException)
     * && ((CommandException)(((AsyncResult)onComplete.obj).exception))
     *          .getCommandError() == CommandException.Error.PASSWORD_INCORRECT
     *
     *
     */

    public void supplyPin (String pin, Message onComplete) {
        mCi.supplyIccPin(mSlotId, mAid, pin, mHandler.obtainMessage(EVENT_PIN1PUK1_DONE, onComplete));
    }

    public void supplyPuk (String puk, String newPin, Message onComplete) {
        mCi.supplyIccPuk(mSlotId, mAid, puk, newPin,
                mHandler.obtainMessage(EVENT_PIN1PUK1_DONE, onComplete));
    }

    public void supplyPin2 (String pin2, Message onComplete) {
        mCi.supplyIccPin2(mSlotId, mAid, pin2,
                mHandler.obtainMessage(EVENT_PIN2PUK2_DONE, onComplete));
    }

    public void supplyPuk2 (String puk2, String newPin2, Message onComplete) {
        mCi.supplyIccPuk2(mSlotId, mAid, puk2, newPin2,
                mHandler.obtainMessage(EVENT_PIN2PUK2_DONE, onComplete));
    }

    public void supplyNetworkDepersonalization (String pin, Message onComplete) {
        if(mDbg) log("Network Despersonalization: " + pin);
        mCi.supplyNetworkDepersonalization(pin,
                mHandler.obtainMessage(EVENT_PIN1PUK1_DONE, onComplete));
    }

    /**
     * Check whether fdn (fixed dialing number) service is available.
     * @return true if ICC fdn service available
     *         false if ICC fdn service not available
     */
    public boolean getIccFdnAvailable() {
        return mIccFdnAvailable;
    }

    /**
     * Check whether ICC pin lock is enabled
     * This is a sync call which returns the cached pin enabled state
     *
     * @return true for ICC locked enabled
     *         false for ICC locked disabled
     */
    public boolean getIccLockEnabled() {
        return mIccPinLocked;
     }

    /**
     * Check whether ICC fdn (fixed dialing number) is enabled
     * This is a sync call which returns the cached pin enabled state
     *
     * @return true for ICC fdn enabled
     *         false for ICC fdn disabled
     */
     public boolean getIccFdnEnabled() {
        return mIccFdnEnabled;
     }

     /**
     * @return No. of Attempts remaining to unlock PIN1/PUK1
     */
    public int getIccPin1RetryCount() {
    return mPin1RetryCount;
    }

    /**
     * @return No. of Attempts remaining to unlock PIN2/PUK2
     */
    public int getIccPin2RetryCount() {
    return mPin2RetryCount;
    }


     /**
      * Set the ICC pin lock enabled or disabled
      * When the operation is complete, onComplete will be sent to its handler
      *
      * @param enabled "true" for locked "false" for unlocked.
      * @param password needed to change the ICC pin state, aka. Pin1
      * @param onComplete
      *        onComplete.obj will be an AsyncResult
      *        ((AsyncResult)onComplete.obj).exception == null on success
      *        ((AsyncResult)onComplete.obj).exception != null on fail
      */
     public void setIccLockEnabled (boolean enabled,
             String password, Message onComplete) {
         int serviceClassX;
         serviceClassX = CommandsInterface.SERVICE_CLASS_VOICE +
                 CommandsInterface.SERVICE_CLASS_DATA +
                 CommandsInterface.SERVICE_CLASS_FAX;

         mDesiredPinLocked = enabled;

         mCi.setFacilityLock(mSlotId, mAid, CommandsInterface.CB_FACILITY_BA_SIM,
                 enabled, password, serviceClassX,
                 mHandler.obtainMessage(EVENT_CHANGE_FACILITY_LOCK_DONE, onComplete));
     }

     /**
      * Set the ICC fdn enabled or disabled
      * When the operation is complete, onComplete will be sent to its handler
      *
      * @param enabled "true" for locked "false" for unlocked.
      * @param password needed to change the ICC fdn enable, aka Pin2
      * @param onComplete
      *        onComplete.obj will be an AsyncResult
      *        ((AsyncResult)onComplete.obj).exception == null on success
      *        ((AsyncResult)onComplete.obj).exception != null on fail
      */
     public void setIccFdnEnabled (boolean enabled,
             String password, Message onComplete) {
         int serviceClassX;
         serviceClassX = CommandsInterface.SERVICE_CLASS_VOICE +
                 CommandsInterface.SERVICE_CLASS_DATA +
                 CommandsInterface.SERVICE_CLASS_FAX +
                 CommandsInterface.SERVICE_CLASS_SMS;

         mDesiredFdnEnabled = enabled;

         mCi.setFacilityLock(mSlotId, mAid, CommandsInterface.CB_FACILITY_BA_FD,
                 enabled, password, serviceClassX,
                 mHandler.obtainMessage(EVENT_CHANGE_FACILITY_FDN_DONE, onComplete));
     }

     /**
      * Change the ICC password used in ICC pin lock
      * When the operation is complete, onComplete will be sent to its handler
      *
      * @param oldPassword is the old password
      * @param newPassword is the new password
      * @param onComplete
      *        onComplete.obj will be an AsyncResult
      *        ((AsyncResult)onComplete.obj).exception == null on success
      *        ((AsyncResult)onComplete.obj).exception != null on fail
      */
     public void changeIccLockPassword(String oldPassword, String newPassword,
             Message onComplete) {
         if(mDbg) log("Change Pin1 old: " + oldPassword + " new: " + newPassword);
         mCi.changeIccPin(mSlotId, mAid, oldPassword, newPassword,
                 mHandler.obtainMessage(EVENT_CHANGE_ICC_PASSWORD_DONE, onComplete));

     }

     /**
      * Change the ICC password used in ICC fdn enable
      * When the operation is complete, onComplete will be sent to its handler
      *
      * @param oldPassword is the old password
      * @param newPassword is the new password
      * @param onComplete
      *        onComplete.obj will be an AsyncResult
      *        ((AsyncResult)onComplete.obj).exception == null on success
      *        ((AsyncResult)onComplete.obj).exception != null on fail
      */
     public void changeIccFdnPassword(String oldPassword, String newPassword,
             Message onComplete) {
         if(mDbg) log("Change Pin2 old: " + oldPassword + " new: " + newPassword);
         mCi.changeIccPin2(mSlotId, mAid, oldPassword, newPassword,
                 mHandler.obtainMessage(EVENT_CHANGE_ICC_PASSWORD_DONE, onComplete));

     }

    /**
     * Returns service provider name stored in ICC card.
     * If there is no service provider name associated or the record is not
     * yet available, null will be returned <p>
     *
     * Please use this value when display Service Provider Name in idle mode <p>
     *
     * Usage of this provider name in the UI is a common carrier requirement.
     *
     * Also available via Android property "gsm.sim.operator.alpha"
     *
     * @return Service Provider Name stored in ICC card
     *         null if no service provider name associated or the record is not
     *         yet available
     *
     */
    //TODO: Fusion - seems like no one uses this
    //public abstract String getServiceProviderName();

    //TODO: Fusion - There should be only one app or card setting its state there.
    //Should it be IccCardProxy?
    protected void updateStateProperty() {
        SystemProperties.set(TelephonyProperties.PROPERTY_SIM_STATE, getState().toString());
    }

    /**
     * Interperate EVENT_QUERY_FACILITY_LOCK_DONE
     * @param ar is asyncResult of Query_Facility_Locked
     */
    private void onQueryFdnEnabled(AsyncResult ar) {
        if(ar.exception != null) {
            if(mDbg) log("Error in querying facility lock:" + ar.exception);
            return;
        }

        int[] ints = (int[])ar.result;
        if (ints.length != 0) {
            if (ints[0] != 2) {
                mIccFdnEnabled = (0!=ints[0]);
                mIccFdnAvailable = true;
            } else {
                if(mDbg) log("Query facility lock: FDN Service Unavailable!");
                mIccFdnAvailable = false;
                mIccFdnEnabled = false;
            }
            if(mDbg) log("Query facility lock for FDN : "  + mIccFdnEnabled);
        } else {
            Log.e(mLogTag, "[IccCard] Bogus facility lock response");
        }
    }

    /**
     * Interperate EVENT_QUERY_FACILITY_LOCK_DONE
     * @param ar is asyncResult of Query_Facility_Locked
     */
    private void onQueryFacilityLock(AsyncResult ar) {
        if(ar.exception != null) {
            if (mDbg) log("Error in querying facility lock:" + ar.exception);
            return;
        }

        int[] ints = (int[])ar.result;
        if(ints.length != 0) {
            mIccPinLocked = (0!=ints[0]);
            if(mDbg) log("Query facility lock for SIM Lock : "  + mIccPinLocked);
        } else {
            Log.e(mLogTag, "[IccCard] Bogus facility lock response");
        }
    }

    /**
     * Parse the error response to obtain No of attempts remaining to unlock PIN1/PUK1
     */
    private void parsePinPukErrorResult(AsyncResult ar, boolean isPin1) {
        int[] intArray = (int[]) ar.result;
        int length = intArray.length;
        mPin1RetryCount = -1;
        mPin2RetryCount = -1;
        if (length > 0) {
            if (isPin1) {
                mPin1RetryCount = intArray[0];
            } else {
                mPin2RetryCount = intArray[0];
            }
        }
    }

    public void broadcastIccStateChangedIntent(String value, String reason) {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        //TODO: Fusion - How do I get phoneName here?
        intent.putExtra(VoicePhone.PHONE_NAME_KEY, "Phone");
        intent.putExtra(INTENT_KEY_ICC_STATE, value);
        intent.putExtra(INTENT_KEY_LOCKED_REASON, reason);
        if(mDbg) log("Broadcasting intent ACTION_SIM_STATE_CHANGED " +  value
                + " reason " + reason);
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE);
    }

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            AsyncResult ar;
            int serviceClassX;

            serviceClassX = CommandsInterface.SERVICE_CLASS_VOICE +
                            CommandsInterface.SERVICE_CLASS_DATA +
                            CommandsInterface.SERVICE_CLASS_FAX;

            //TODO: Fusion - make sure this is not a problem
            //if (!mPhone.mIsTheCurrentActivePhone) {
            //    Log.e(mLogTag, "Received message " + msg +
            //            "[" + msg.what + "] while being destroyed. Ignoring.");
            //    return;
            //}

            switch (msg.what) {
                case EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                    updateStateProperty();
                    //TODO: Fusion - Probably just need to notify unavailable registrants.
                    //TODO: Fusion - Or better yet - just let UiccManager take care of that
                    // and wait for dispose() call 
                    broadcastIccStateChangedIntent(INTENT_VALUE_ICC_NOT_READY, null);
                    break;
                case EVENT_ICC_READY:
                    //TODO: Fusion - Does comment below still apply?
                    //TODO: put facility read in SIM_READY now, maybe in REG_NW
                    mCi.queryFacilityLock (mSlotId, mAid,
                            CommandsInterface.CB_FACILITY_BA_SIM, "", serviceClassX,
                            obtainMessage(EVENT_QUERY_FACILITY_LOCK_DONE));
                    mCi.queryFacilityLock (mSlotId, mAid,
                            CommandsInterface.CB_FACILITY_BA_FD, "", serviceClassX,
                            obtainMessage(EVENT_QUERY_FACILITY_FDN_DONE));
                    break;
                case EVENT_ICC_LOCKED_OR_ABSENT:
                    mCi.queryFacilityLock (mSlotId, mAid,
                            CommandsInterface.CB_FACILITY_BA_SIM, "", serviceClassX,
                            obtainMessage(EVENT_QUERY_FACILITY_LOCK_DONE));
                    break;
                case EVENT_PIN1PUK1_DONE:
                case EVENT_PIN2PUK2_DONE:
                    // a PIN/PUK/PIN2/PUK2/Network Personalization
                    // request has completed. ar.userObj is the response Message
                    // Repoll before returning
                    ar = (AsyncResult)msg.obj;
                    // TODO should abstract these exceptions
                    AsyncResult.forMessage(((Message)ar.userObj)).exception
                    = ar.exception;
                    if ((ar.exception != null) && (ar.result != null)) {
                        if (msg.what == EVENT_PIN1PUK1_DONE) {
                            parsePinPukErrorResult(ar, true);
                        } else {
                            parsePinPukErrorResult(ar, false);
                        }
                    }
                    //TODO: Fusion - Check if repolling here really required.
                    //TODO: Fusion - Why can't we send callback message right away?
                    mUiccCard.getUiccManager().triggerIccStatusUpdate(ar.userObj);
                    break;
                case EVENT_QUERY_FACILITY_LOCK_DONE:
                    ar = (AsyncResult)msg.obj;
                    onQueryFacilityLock(ar);
                    break;
                case EVENT_QUERY_FACILITY_FDN_DONE:
                    ar = (AsyncResult)msg.obj;
                    onQueryFdnEnabled(ar);
                    break;
                case EVENT_CHANGE_FACILITY_LOCK_DONE:
                    ar = (AsyncResult)msg.obj;
                    if (ar.exception == null) {
                        mIccPinLocked = mDesiredPinLocked;
                        if (mDbg) log( "EVENT_CHANGE_FACILITY_LOCK_DONE: " +
                                "mIccPinLocked= " + mIccPinLocked);
                    } else {
                        if (ar.result != null) {
                            parsePinPukErrorResult(ar, true);
                        }
                        Log.e(mLogTag, "Error change facility lock with exception "
                            + ar.exception);
                    }
                    AsyncResult.forMessage(((Message)ar.userObj)).exception
                                                        = ar.exception;
                    ((Message)ar.userObj).sendToTarget();
                    break;
                case EVENT_CHANGE_FACILITY_FDN_DONE:
                    ar = (AsyncResult)msg.obj;

                    if (ar.exception == null) {
                        mIccFdnEnabled = mDesiredFdnEnabled;
                        if (mDbg) log("EVENT_CHANGE_FACILITY_FDN_DONE: " +
                                "mIccFdnEnabled=" + mIccFdnEnabled);
                    } else {
                        if (ar.result != null) {
                            parsePinPukErrorResult(ar, false);
                        }
                        Log.e(mLogTag, "Error change facility fdn with exception "
                                + ar.exception);
                    }
                    AsyncResult.forMessage(((Message)ar.userObj)).exception
                                                        = ar.exception;
                    ((Message)ar.userObj).sendToTarget();
                    break;
                case EVENT_CHANGE_ICC_PASSWORD_DONE:
                    ar = (AsyncResult)msg.obj;
                    if(ar.exception != null) {
                        Log.e(mLogTag, "Error in change icc app password with exception"
                            + ar.exception);
                        if (ar.result != null) {
                            parsePinPukErrorResult(ar, true);
                        }
                    }
                    AsyncResult.forMessage(((Message)ar.userObj)).exception
                                                        = ar.exception;
                    ((Message)ar.userObj).sendToTarget();
                    break;
                default:
                    Log.e(mLogTag, "[IccCard] Unknown Event " + msg.what);
            }
        }
    };

    //TODO: Fusion - work with App state instead, fix PUK1, PUK2
    //public AppState getApplicationState() {
/*        if (mIccCardStatus == null) {
            Log.e(mLogTag, "[IccCard] IccCardStatus is null");
            return UiccCard.State.ABSENT;
        }

        // this is common for all radio technologies
        // Presently all SIM card statuses except card present are treated as
        // ABSENT. Handling Card IO error case seperately.
        if (!mIccCardStatus.getCardState().isCardPresent()) {
            if (mIccCardStatus.getCardState().isCardFaulty() &&
                SystemProperties.getBoolean("persist.cust.tel.adapt",false)) {
                return UiccCard.State.CARD_IO_ERROR;
            }
            return UiccCard.State.ABSENT;
        }

        RadioState currentRadioState = mCi.getRadioState();
        // check radio technology
        if( currentRadioState == RadioState.RADIO_OFF         ||
            currentRadioState == RadioState.RADIO_UNAVAILABLE
//          TODO: fusion - Will be changed as part of Uicc framework.
//            currentRadioState == RadioState.SIM_NOT_READY     ||
//            currentRadioState == RadioState.RUIM_NOT_READY    ||
//            currentRadioState == RadioState.NV_NOT_READY      ||
//            currentRadioState == RadioState.NV_READY
            ) {
            return UiccCard.State.NOT_READY;
        }

        if( false
//              TODO: fusion - Will be changed as part of Uicc framework.
//                currentRadioState == RadioState.SIM_LOCKED_OR_ABSENT  ||
//            currentRadioState == RadioState.SIM_READY             ||
//            currentRadioState == RadioState.RUIM_LOCKED_OR_ABSENT ||
//            currentRadioState == RadioState.RUIM_READY
            ) {

            int index;

            // check for CDMA radio technology
            if (false) {
//              TODO: fusion - Will be changed as part of Uicc framework.
//                    currentRadioState == RadioState.RUIM_LOCKED_OR_ABSENT ||
//                currentRadioState == RadioState.RUIM_READY) {
                index = mIccCardStatus.getCdmaSubscriptionAppIndex();
            }
            else {
                index = mIccCardStatus.getGsmUmtsSubscriptionAppIndex();
            }
        IccCardApplication app;
        if ((index < mIccCardStatus.CARD_MAX_APPS) && (index >= 0)) {
        app = mIccCardStatus.getApplication(index);
        } else {
        Log.e(mLogTag, "[IccCard] Invalid Subscription Application index:" + index);
        return UiccCard.State.ABSENT;
        }

            if (app == null) {
                Log.e(mLogTag, "[IccCard] Subscription Application in not present");
                return UiccCard.State.ABSENT;
            }

            Log.i(mLogTag, "PIN1 Status " + app.pin1 + "PIN2 Status " + app.pin2);
            if (app.pin2.isPinBlocked()) {
                Log.i(mLogTag, "PIN2 is blocked, PUK2 required.");
                mIccPin2Blocked = true;
                mIccPuk2Blocked = false;
            } else if (app.pin2.isPukBlocked()) {
                Log.i(mLogTag, "PUK2 is permanently blocked.");
                mIccPuk2Blocked = true;
                mIccPin2Blocked = false;
            } else {
                Log.i(mLogTag, "Neither PIN2 nor PUK2 is blocked.");
                mIccPin2Blocked = false;
                mIccPuk2Blocked = false;
            }

            // check if PIN required
            if (app.app_state.isPinRequired()) {
                return UiccCard.State.PIN_REQUIRED;
            }
            if (app.app_state.isPukRequired()) {
                return UiccCard.State.PUK_REQUIRED;
            }
            if (app.app_state.isSubscriptionPersoEnabled()) {
                //Following De-Personalizations are supported
                //as specified in 3GPP TS 22.022, and 3GPP2 C.S0068-0.
                //01.PERSOSUBSTATE_SIM_NETWORK
                //02.PERSOSUBSTATE_SIM_NETWORK_SUBSET
                //03.PERSOSUBSTATE_SIM_CORPORATE
                //04.PERSOSUBSTATE_SIM_SERVICE_PROVIDER
                //05.PERSOSUBSTATE_SIM_SIM
                //06.PERSOSUBSTATE_RUIM_NETWORK1
                //07.PERSOSUBSTATE_RUIM_NETWORK2
                //08.PERSOSUBSTATE_RUIM_HRPD
                //09.PERSOSUBSTATE_RUIM_CORPORATE
                //10.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER
                //11.PERSOSUBSTATE_RUIM_RUIM
                log("ICC is Perso Locked, substate " + app.perso_substate);
                if (app.perso_substate.isPersoSubStateSimNetwork()) {
                    return UiccCard.State.NETWORK_LOCKED;
                } else if (app.perso_substate.isPersoSubStateSimNetworkSubset()) {
                    return UiccCard.State.SIM_NETWORK_SUBSET_LOCKED;
                } else if (app.perso_substate.isPersoSubStateSimCorporate()) {
                    return UiccCard.State.SIM_CORPORATE_LOCKED;
                } else if (app.perso_substate.isPersoSubStateSimServiceProvider()) {
                    return UiccCard.State.SIM_SERVICE_PROVIDER_LOCKED;
                } else if (app.perso_substate.isPersoSubStateSimSim()) {
                    return UiccCard.State.SIM_SIM_LOCKED;
                } else if (app.perso_substate.isPersoSubStateRuimNetwork1()) {
                    return UiccCard.State.RUIM_NETWORK1_LOCKED;
                } else if (app.perso_substate.isPersoSubStateRuimNetwork2()) {
                    return UiccCard.State.RUIM_NETWORK2_LOCKED;
                } else if (app.perso_substate.isPersoSubStateRuimHrpd()) {
                    return UiccCard.State.RUIM_HRPD_LOCKED;
                } else if (app.perso_substate.isPersoSubStateRuimCorporate()) {
                    return UiccCard.State.RUIM_CORPORATE_LOCKED;
                } else if (app.perso_substate.isPersoSubStateRuimServiceProvider()) {
                    return UiccCard.State.RUIM_SERVICE_PROVIDER_LOCKED;
                } else if (app.perso_substate.isPersoSubStateRuimRuim()) {
                    return UiccCard.State.RUIM_RUIM_LOCKED;
                } else {
                    Log.e(mLogTag,"[IccCard] UnSupported De-Personalization, substate "
                          + app.perso_substate + " assuming ICC_NOT_READY");
                    return UiccCard.State.NOT_READY;
                }
            }
            if (app.app_state.isAppReady()) {
                return UiccCard.State.READY;
            }
            if (app.app_state.isAppNotReady()) {
                return UiccCard.State.NOT_READY;
            }
            return UiccCard.State.NOT_READY;
        }

        return UiccCard.State.ABSENT;*/
  //      return mAppState;
  //  }

    /**
     * @return true if ICC card is PIN2 blocked
     */
    public boolean getIccPin2Blocked() {
        return mIccPin2Blocked;
    }

    /**
     * @return true if ICC card is PUK2 blocked
     */
    public boolean getIccPuk2Blocked() {
        return mIccPuk2Blocked;
    }

    private void log(String msg) {
        Log.d(mLogTag, "[IccCard] " + msg);
    }

 }