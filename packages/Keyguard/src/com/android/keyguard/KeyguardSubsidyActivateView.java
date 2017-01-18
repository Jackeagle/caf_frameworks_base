/*
 * Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.
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

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.IExtTelephony;
import com.android.internal.widget.LockPatternUtils;

import java.util.List;

/**
 * Displays screen to activate and unlock
 */
public class KeyguardSubsidyActivateView extends LinearLayout implements
                                                 KeyguardSecurityView  {
    KeyguardSecurityCallback mCallBack;
    private static final String TAG = "KeyguardSubsidyActivateView";
    private static final boolean DEBUG = SubsidyUtility.DEBUG;
    private View mContentView;
    private View mProgressView;
    private View mEmergencyView;
    private TextView mProgressTitleView;
    private TextView mProgressContentView;
    private Button mUnlockBtn;
    private Button mActivateBtn;
    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private boolean mActivationCallInitiated;
    private LinearLayout mSubsidySetupContainer;

    public KeyguardSubsidyActivateView(Context context) {
        super(context);
        mContext = context;
    }

    public KeyguardSubsidyActivateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContentView = findViewById(R.id.content_view);

        mTelephonyManager =
            (TelephonyManager) mContext
            .getSystemService(Context.TELEPHONY_SERVICE);

        mProgressView = findViewById(R.id.keyguard_subsidy_progress);

        mEmergencyView = findViewById(R.id.emergency_view);
        mProgressTitleView = (TextView) findViewById(R.id.kg_progress_title);
        mProgressContentView = (TextView)
            findViewById(R.id.kg_progress_content);
        mProgressContentView.setText(R.string.kg_subsidy_content_progress_server);
        mUnlockBtn = (Button) findViewById(R.id.unlock);
        mUnlockBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.d(TAG, " Unlock Button Pressed ");
                }
                mContentView.setVisibility(View.GONE);
                mEmergencyView.setVisibility(View.GONE);
                mProgressTitleView.setText(R.string.kg_subsidy_title_unlock_progress_dialog);
                mProgressView.setVisibility(View.VISIBLE);
                setSubsidySetupContainerVisibility(View.GONE);
                Intent intent = new Intent(SubsidyUtility.ACTION_USER_REQUEST);
                intent.setPackage(getResources().getString(
                        R.string.config_slc_package_name));
                intent.putExtra(SubsidyController
                    .getInstance(mContext).getCurrentSubsidyState()
                    .getLaunchIntent(R.string.kg_button_unlock), true);
                mContext.sendBroadcast(intent,
                    SubsidyUtility.BROADCAST_PERMISSION);
            }
        });

        mActivateBtn = (Button) findViewById(R.id.activate);
        mActivateBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.d(TAG, " Activate Button Pressed ");
                }
                mActivationCallInitiated = true;
                mProgressTitleView.setText(R.string.kg_subsidy_title_progress_activating);
                getContext().startActivity(getSimActivationCallIntent());
            }
        });
    }

    /*
     * Disable navigation bar when activation/emergency call is
     * in progress.
     * @param disable
     */
    private void disableNavigationBar(boolean disable) {
        StatusBarManager statusBarManager =
            (StatusBarManager) mContext
            .getSystemService(Context.STATUS_BAR_SERVICE);
        int flags = StatusBarManager.DISABLE_NONE;
        if (statusBarManager == null) {
            Log.w(TAG, "Could not get status bar manager");
        } else if (disable) {
            flags |= StatusBarManager.DISABLE_HOME;
            flags |= StatusBarManager.DISABLE_BACK;
            flags |= StatusBarManager.DISABLE_RECENT;
            flags |= StatusBarManager.DISABLE_SEARCH;
            flags |= StatusBarManager.DISABLE_EXPAND;
        } else {
            flags |= StatusBarManager.DISABLE_RECENT;
            flags |= StatusBarManager.DISABLE_SEARCH;
        }
        if (!(mContext instanceof Activity)) {
            statusBarManager.disable(flags);
        }
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public void startAppearAnimation() {
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallBack = callback;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
    }

    @Override
    public void reset() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume(int reason) {
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return null;
    }

    @Override
    public void showPromptReason(int reason) {
    }

    @Override
    public void showMessage(String message, int color) {
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(
                mInfoCallback);
        mSubsidySetupContainer = (LinearLayout) getRootView()
                .findViewById(R.id.subsidy_setup_container);
        setSubsidySetupContainerVisibility(View.VISIBLE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(
                mInfoCallback);
        mSubsidySetupContainer = null;
    }

    private Intent getSimActivationCallIntent() {
        // Prepare intent to make a tele verification call
        // inorder to activate the sim.
        Intent intent =
            new Intent(Intent.ACTION_CALL, Uri.fromParts(
                        PhoneAccount.SCHEME_TEL,
                        getResources().getString(
                            R.string.config_televerification_number), null))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final TelecomManager tm =
            (TelecomManager) mContext
            .getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccountHandle> accounts = tm.getCallCapablePhoneAccounts();
        IExtTelephony extTelephony =
                   IExtTelephony.Stub.asInterface(
                           ServiceManager.getService("extphone"));
        try {
            if (extTelephony != null) {
                for (PhoneAccountHandle account : accounts) {
                    if (account != null && account
                            .getId()
                            .trim()
                            .equalsIgnoreCase(
                                String.valueOf(extTelephony
                                    .getCurrentPrimaryCardSlotId()))) {
                        intent.putExtra(
                                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                                account);
                        break;
                    }
                }
            }
        } catch (RemoteException ex) {
            Log.e(TAG,
                    "Exception for getCurrentPrimaryCardSlotId: ", ex);
        }

        return intent;
    }

    KeyguardUpdateMonitorCallback mInfoCallback =
        new KeyguardUpdateMonitorCallback() {
            boolean wasOffHook = false;

            @Override
            public void onPhoneStateChanged(int phoneState) {
                if (TelephonyManager.CALL_STATE_IDLE == phoneState) {
                    if (wasOffHook && mActivationCallInitiated) {
                        Log.d(TAG, "Tele-Verification is done ");

                        Intent intent = new
                            Intent(SubsidyUtility.ACTION_USER_REQUEST);

                        intent.setPackage(getResources().getString(
                                    R.string.config_slc_package_name));
                        intent.putExtra(SubsidyController
                                .getInstance(mContext).getCurrentSubsidyState()
                                .getLaunchIntent(R.string.kg_button_activate),
                                       true);
                        mContext.sendBroadcast(intent,
                                SubsidyUtility.BROADCAST_PERMISSION);
                        wasOffHook = false;
                        mActivationCallInitiated = false;
                        mContentView.setVisibility(View.GONE);
                        mEmergencyView.setVisibility(View.GONE);
                        mProgressView.setVisibility(View.VISIBLE);
                        setSubsidySetupContainerVisibility(View.GONE);
                    }
                    disableNavigationBar(false);
                } else if (TelephonyManager.CALL_STATE_OFFHOOK
                        == phoneState) {
                    wasOffHook = true;
                    disableNavigationBar(true);
                }
            }

            @Override
            public void onSubsidyLockStateChanged(boolean isLocked) {
                if (mProgressView.getVisibility() == View.VISIBLE) {
                    mContentView.setVisibility(View.VISIBLE);
                    mEmergencyView.setVisibility(View.VISIBLE);
                    mProgressView.setVisibility(View.GONE);
                }
                setSubsidySetupContainerVisibility(View.VISIBLE);
            }
        };

        public void setSubsidySetupContainerVisibility(int isVisible) {
            if (mSubsidySetupContainer != null) {
                mSubsidySetupContainer.setVisibility(isVisible);
            }
        }
}
