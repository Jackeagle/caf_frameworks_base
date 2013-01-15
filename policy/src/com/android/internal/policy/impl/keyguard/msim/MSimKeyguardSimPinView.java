/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
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

package com.android.internal.policy.impl.keyguard;

import com.android.internal.telephony.msim.ITelephonyMSim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;

import android.util.Log;

import com.android.internal.R;

/**
 * Displays a PIN pad for unlocking.
 */
public class MSimKeyguardSimPinView extends KeyguardSimPinView {
    private final static boolean DEBUG = KeyguardViewMediator.DEBUG;
    private static String TAG = "MSimKeyguardSimPinView";

    public MSimKeyguardSimPinView(Context context) {
        this(context, null);
    }

    public MSimKeyguardSimPinView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void resetState() {
        mSecurityMessageDisplay.setMessage(
                getSecurityMessageDisplay(R.string.kg_sim_pin_instructions), true);
        mPasswordEntry.setEnabled(true);
    }

    /**
     * Since the IPC can block, we want to run the request in a separate thread
     * with a callback.
     */
    private abstract class MSimCheckSimPin extends Thread {
        private final String mPin;
        protected final int mSubscription;

        protected MSimCheckSimPin(String pin, int sub) {
            mPin = pin;
            mSubscription = sub;
        }

        abstract void onSimCheckResponse(boolean success);

        @Override
        public void run() {
            try {
                if (DEBUG) Log.d(TAG, "MSimCheckSimPin:run(), mPin = " + mPin
                        + " mSubscription = " + mSubscription);
                final boolean result = ITelephonyMSim.Stub.asInterface(ServiceManager
                        .checkService("phone_msim")).supplyPin(mPin, mSubscription);
                post(new Runnable() {
                    public void run() {
                        onSimCheckResponse(result);
                    }
                });
            } catch (RemoteException e) {
                post(new Runnable() {
                    public void run() {
                        onSimCheckResponse(false);
                    }
                });
            }
        }
    }

    @Override
    protected void verifyPasswordAndUnlock() {
        String entry = mPasswordEntry.getText().toString();
        if (DEBUG) Log.d(TAG, "verifyPasswordAndUnlock(): entry = " + entry);

        if (entry.length() < 4) {
            // otherwise, display a message to the user, and don't submit.
            mSecurityMessageDisplay.setMessage(getSecurityMessageDisplay(R.string.kg_invalid_sim_pin_hint), true);
            mPasswordEntry.setText("");
            mCallback.userActivity(0);
            return;
        }

        getSimUnlockProgressDialog().show();

        if (!mSimCheckInProgress) {
            mSimCheckInProgress = true; // there should be only one

            if (DEBUG) Log.d(TAG, "startCheckSimPin(), Multi SIM enabled");
            new MSimCheckSimPin(mPasswordEntry.getText().toString(),
                    KeyguardUpdateMonitor.getInstance(mContext).getPinLockedSubscription()) {
                void onSimCheckResponse(final boolean success) {
                    post(new Runnable() {
                        public void run() {
                            if (mSimUnlockProgressDialog != null) {
                                mSimUnlockProgressDialog.hide();
                            }
                            if (success) {
                                // before closing the keyguard, report back that the sim is unlocked
                                // so it knows right away.
                                KeyguardUpdateMonitor.getInstance(getContext()).reportSimUnlocked(mSubscription);
                                mCallback.dismiss(true);
                            } else {
                                mSecurityMessageDisplay.setMessage
                                    (getSecurityMessageDisplay(R.string.kg_password_wrong_pin_code), true);
                                mPasswordEntry.setText("");
                            }
                            mCallback.userActivity(0);
                            mSimCheckInProgress = false;
                        }
                    });
                }
            }.start();
        }
    }

    protected CharSequence getSecurityMessageDisplay(int resId) {
        // Returns the String in the format
        // "SUB:%d : %s", sub, msg
        return getContext().getString(R.string.msim_kg_sim_pin_msg_format,
                KeyguardUpdateMonitor.getInstance(mContext).getPinLockedSubscription()+1,
                getContext().getResources().getText(resId));
    }
}

