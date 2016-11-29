/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.keyguard;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.internal.telephony.IExtTelephony;

/**
 * Displays a PIN pad for unlocking.
 */
public class KeyguardSubsidyPinView extends KeyguardPinBasedInputView {
    private static final String TAG = "KeyguardSubsidyPinView";
    private static final boolean DEBUG = SubsidyUtility.DEBUG;
    private TextView mKeyguardMessageView;
    private ImageButton mEnterKey;
    private Context mContext;
    private CheckUnlockPin mCheckUnlockPinThread;
    private ProgressDialog mUnlockProgressDialog = null;
    private int mRetryAttemptRemaining;
    private CountDownTimer mCountDownTimer;
    private long mDeadLineTime;

    public KeyguardSubsidyPinView(Context context) {
        this(context, null);
    }

    public KeyguardSubsidyPinView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mRetryAttemptRemaining = getTotalRetryAttempts();
    }

    public void resetState() {
        super.resetState();
        showDefaultMessage();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resetState();
    }

    @Override
    protected int getPromtReasonStringRes(int reason) {
        // No message on Device Pin
        return 0;
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.subsidy_pinEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mKeyguardMessageView =
            (TextView) findViewById(R.id.keyguard_message_area);
        mKeyguardMessageView.setSingleLine(false);
        mKeyguardMessageView.setEllipsize(null);

        mSecurityMessageDisplay.setTimeout(0); // don't show ownerinfo/charging
        // status by default
        if (mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) mEcaView).setCarrierTextVisible(false);
        }
    }

    private int getTotalRetryAttempts() {
        return mContext.getResources().getInteger(
                R.integer.config_max_enter_code_attempt);
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public void onPause() {
        // dismiss the dialog.
        if (mUnlockProgressDialog != null) {
            mUnlockProgressDialog.dismiss();
            mUnlockProgressDialog = null;
        }
    }

    private Dialog getUnlockProgressDialog() {
        if (mUnlockProgressDialog == null) {
            mUnlockProgressDialog = new ProgressDialog(mContext);
            mUnlockProgressDialog.setMessage(
                    mContext.getString(R.string.kg_unlock_progress_dialog_message));
            mUnlockProgressDialog.setIndeterminate(true);
            mUnlockProgressDialog.setCancelable(false);
            mUnlockProgressDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
        return mUnlockProgressDialog;
    }

    @Override
    protected void verifyPasswordAndUnlock() {
        String entry = mPasswordEntry.getText();

        if (entry.length() < 16) {
            // otherwise, display a message to the user, and don't
            // submit.
            handleErrorCase();
            return;
        }
        SubsidyController.getInstance(mContext).stopStateTransitions(true);
        getUnlockProgressDialog().show();

        if (mCheckUnlockPinThread == null) {
            mCheckUnlockPinThread =
                new CheckUnlockPin(mPasswordEntry.getText()) {
                    void onUnlockResponse(final boolean isSuccess) {
                        post(new Runnable() {
                            public void run() {
                                SubsidyController.getInstance(mContext)
                                                 .stopStateTransitions(false);
                                if (mUnlockProgressDialog != null) {
                                    mUnlockProgressDialog.hide();
                                }
                                if (isSuccess) {
                                    if (DEBUG) {
                                        Log.d(TAG, "Local Unlock code is correct and verified");
                                    }
                                    Intent intent = new Intent(
                                        SubsidyUtility.ACTION_USER_REQUEST);

                                    intent.setPackage(mContext.getResources()
                                        .getString(R.string.config_slc_package_name));
                                    intent.putExtra(SubsidyController
                                        .getInstance(mContext)
                                        .getCurrentSubsidyState()
                                        .getLaunchIntent(
                                            R.string.kg_button_activate),
                                        true);
                                    mContext.sendBroadcast(intent,
                                        SubsidyUtility.BROADCAST_PERMISSION);

                                    SubsidyController.getInstance(mContext)
                                                         .setDeviceUnlocked();
                                } else {
                                    handleErrorCase();
                                }
                                mCheckUnlockPinThread = null;
                            }
                        });
                    }
                };
            mCheckUnlockPinThread.start();
        }
    }

    @Override
    public void startAppearAnimation() {
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    private void showDefaultMessage() {
        mSecurityMessageDisplay.setMessage(
                R.string.kg_subsidy_content_pin_locked, true);
    }

    @Override
    public int getWrongPasswordStringId() {
        return R.plurals.kg_subsidy_wrong_pin;
    }

    @Override
    public void reset() {
        if (DEBUG) {
            Log.v(TAG, "Reset the state based based on the current state");
        }
        resetPasswordText(false /* animate */);
        // if the user is currently locked out, enforce it.
        long deadline = getDeadlineTime();
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }

    public void handleErrorCase() {
        if (DEBUG) {
            Log.v(TAG, "Handle error case when user attemp with wrong pin");
        }
        mRetryAttemptRemaining--;

        if (mRetryAttemptRemaining > 0) {
            mSecurityMessageDisplay.setMessage(
                    mContext.getResources().getQuantityString(
                            R.plurals.kg_subsidy_wrong_pin,
                            mRetryAttemptRemaining, mRetryAttemptRemaining),
                    true);
        } else if (mRetryAttemptRemaining == 0) {
            int attemptTimeOut =
                    mContext.getResources().getInteger(
                            R.integer.config_timeout_after_max_attempt_milli);
            mDeadLineTime = SystemClock.elapsedRealtime() + attemptTimeOut;
            handleAttemptLockout(mDeadLineTime);
        }
        resetPasswordText(true);
        mCallback.userActivity();
    }

    @Override
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        setPasswordEntryEnabled(false);

        final long elapsedRealtime = SystemClock.elapsedRealtime();

        mCountDownTimer =
                new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime,
                        1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        // Condition to distinguish min/sec to display.
                        // If more than 1 minutes remaining it will be displayed
                        // in minutes, if less than 1 minutes is displayed in
                        // seconds.
                        if (millisUntilFinished > 60000) {
                            int minutesRemaining =
                                    (int) (millisUntilFinished / 60000);
                            minutesRemaining++;
                            mSecurityMessageDisplay.setMessage(
                                    R.string.kg_subsidy_too_many_failed_attempts_countdown,
                                    true, minutesRemaining);
                        } else {
                            int secondsRemaining =
                                    (int) (millisUntilFinished / 1000);
                            secondsRemaining++;
                            mSecurityMessageDisplay.setMessage(
                                    R.string.kg_subsidy_too_many_failed_attempts_countdown_sec,
                                    true, secondsRemaining);
                        }
                    }

                    @Override
                    public void onFinish() {
                        if (DEBUG) {
                            Log.v(TAG, "CountDownTimer onFinish called");
                        }
                        mRetryAttemptRemaining = getTotalRetryAttempts();
                        setPasswordEntryEnabled(true);
                        showDefaultMessage();
                    }

                }.start();
    }

    private abstract class CheckUnlockPin extends Thread {
        private final String mPin;

        protected CheckUnlockPin(String pin) {
            mPin = pin;
        }

        abstract void onUnlockResponse(final boolean result);

        @Override
        public void run() {
            try {
                IExtTelephony extTelephony =
                    IExtTelephony.Stub.asInterface(ServiceManager
                            .getService("extphone"));
                int slotId = extTelephony.getCurrentPrimaryCardSlotId();
                if (DEBUG) {
                    Log.v(TAG, "call supplyNetworkDepersonalization SlotId ="
                            + slotId);
                }
                final boolean result = extTelephony
                    .supplyNetworkDepersonalization(mPin, "", slotId);
                if (DEBUG) {
                    Log.v(TAG, "supplyNetworkDepersonalization returned: "
                            + result);
                }
                post(new Runnable() {
                    public void run() {
                        onUnlockResponse(result);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG,
                        "Exception for supplyNetworkDepersonalization:", e);
                post(new Runnable() {
                    public void run() {
                        onUnlockResponse(false);
                    }
                });
            }
        }
    }

    public long getDeadlineTime() {
        final long now = SystemClock.elapsedRealtime();
        if (mDeadLineTime < now && mDeadLineTime != 0) {
            return 0L;
        }
        return mDeadLineTime;
    }
}
