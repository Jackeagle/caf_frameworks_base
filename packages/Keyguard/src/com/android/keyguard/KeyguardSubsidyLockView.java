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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

/**
 * Displays screen to unlock
 */
public class KeyguardSubsidyLockView extends LinearLayout implements
    KeyguardSecurityView {
    KeyguardSecurityCallback mCallBack;
    private static final String TAG = "KeyguardSubsidyLockView";
    private static final boolean DEBUG = SubsidyUtility.DEBUG;
    private View mContentView;
    private View mProgressView;
    private View mEmergencyView;
    private TextView mProgressTitleView;
    private TextView mProgressContentView;
    private Button mUnlockBtn;
    private Button mWifiBtn;
    private Context mContext;
    private WifiManager mWifiManager;

    public KeyguardSubsidyLockView(Context context) {
        super(context);
        mContext = context;
    }

    public KeyguardSubsidyLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContentView = findViewById(R.id.content_view);
        mProgressView = findViewById(R.id.keyguard_subsidy_progress);

        mEmergencyView = findViewById(R.id.emergency_view);
        mProgressTitleView = (TextView) findViewById(R.id.kg_progress_title);
        mProgressTitleView.setVisibility(View.GONE);
        mProgressContentView =
            (TextView) findViewById(R.id.kg_progress_content);
        mProgressContentView.setText(R.string.kg_subsidy_content_activating);
        mUnlockBtn = (Button) findViewById(R.id.unlock);

        mUnlockBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) {
                    Log.d(TAG, " Unlock Button Pressed ");
                }
                mContentView.setVisibility(View.GONE);
                mEmergencyView.setVisibility(View.GONE);
                mProgressView.setVisibility(View.VISIBLE);
                Intent intent = new Intent(SubsidyUtility.ACTION_USER_REQUEST);
                intent.setPackage(getResources().getString(
                        R.string.config_slc_package_name));
                intent.putExtra(SubsidyController
                    .getInstance(mContext).getCurrentSubsidyState()
                    .getLaunchIntent(0), true);
                mContext.sendBroadcast(intent,
                    SubsidyUtility.BROADCAST_PERMISSION);
            }
        });
        mContext.getApplicationContext().registerReceiver(
                mWifiStateChangedReceiver,
                new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        mWifiBtn = (Button) findViewById(R.id.wifi_icon);
        mWifiManager =
            (WifiManager) mContext
            .getApplicationContext().getSystemService(
                    Context.WIFI_SERVICE);
        int wiFiState = WifiManager.WIFI_STATE_UNKNOWN;;
        if (mWifiManager != null) {
            wiFiState = mWifiManager.getWifiState();
        }
        setWiFiDrawable(wiFiState);
        mWifiBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent wifiIntent = new Intent(SubsidyUtility.WIFI_SETUP_SCREEN_INTENT);
                wifiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivity(wifiIntent);
            }
        });
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

    public void setWiFiDrawable(int wifiState) {
        if (WifiManager.WIFI_STATE_DISABLED == wifiState) {
            mWifiBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                    R.drawable.kg_subsidy_wifi_off, 0, 0);
        } else if (WifiManager.WIFI_STATE_ENABLED == wifiState) {
            mWifiBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                    R.drawable.kg_subsidy_wifi, 0, 0);
        } else {
            mWifiBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                    R.drawable.kg_subsidy_wifi_off, 0, 0);
        }
    }

    /*
     * Get current WiFi state
     */
    public int getWiFiState() {
        int wiFiState = WifiManager.WIFI_STATE_UNKNOWN;
        if (mWifiManager != null) {
            wiFiState = mWifiManager.getWifiState();
        }
        return wiFiState;
    }

    private BroadcastReceiver mWifiStateChangedReceiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int wiFiState = WifiManager.WIFI_STATE_UNKNOWN;
                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent
                            .getAction())) {
                    wiFiState = getWiFiState();
                }
                setWiFiDrawable(wiFiState);
            }
        };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(
                mInfoCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(
                mInfoCallback);
    }

    KeyguardUpdateMonitorCallback mInfoCallback =
        new KeyguardUpdateMonitorCallback() {
            @Override
            public void onSubsidyLockStateChanged(boolean isLocked) {
                if (mProgressView.getVisibility() == View.VISIBLE) {
                    mContentView.setVisibility(View.VISIBLE);
                    mEmergencyView.setVisibility(View.VISIBLE);
                    mProgressView.setVisibility(View.GONE);
                }
            }
        };
}
