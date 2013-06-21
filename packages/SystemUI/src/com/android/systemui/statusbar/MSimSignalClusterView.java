/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
 *
 * Not a Contribution.
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.telephony.MSimTelephonyManager;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.MSimConstants;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.MSimNetworkController;

import com.android.systemui.R;

// Intimately tied to the design of res/layout/msim_signal_cluster_view.xml
public class MSimSignalClusterView
        extends LinearLayout
        implements MSimNetworkController.MSimSignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "MSimSignalClusterView";

    protected MSimNetworkController mMSimNC;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private boolean mMobileVisible = false;
    private int[] mMobileStrengthId;
    private int[] mMobileActivityId;
    private int[] mMobileTypeId;
    private int[] mNoSimIconId;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileTypeDescription;
    private String[] mMobileDescription;
    private boolean[] mNoSimIconVisiable;
    private boolean[] mSignalIconVisiable;

    ViewGroup mWifiGroup, mMobileGroup, mMobileGroupSub2;
    ImageView mWifi, mWifiActivity, mMobile, mMobileActivity, mMobileType, mAirplane;
    ImageView mNoSimSlot, mNoSimSlotSub2;
    ImageView mMobileSub2, mMobileActivitySub2, mMobileTypeSub2;
    View mSpacer;

    public MSimSignalClusterView(Context context) {
        this(context, null);
    }

    public MSimSignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MSimSignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        mMobileStrengthId = new int[numPhones];
        mMobileDescription = new String[numPhones];
        mMobileTypeId = new int[numPhones];
        mMobileActivityId = new int[numPhones];
        mNoSimIconId = new int[numPhones];
        mNoSimIconVisiable = new boolean[numPhones];
        mSignalIconVisiable = new boolean[numPhones];
        for(int i=0; i < numPhones; i++) {
            mMobileStrengthId[i] = 0;
            mMobileTypeId[i] = 0;
            mMobileActivityId[i] = 0;
            mNoSimIconId[i] = 0;
            mNoSimIconVisiable[i] = false;
            mSignalIconVisiable[i] = false;
        }
    }

    public void setNetworkController(MSimNetworkController nc) {
        if (DEBUG) Slog.d(TAG, "MSimNetworkController=" + nc);
        mMSimNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // If the defined status bar style is not the default style, do nothing.
        if (PhoneStatusBar.STATUSBAR_STYLE != PhoneStatusBar.STATUSBAR_STYLE_DEFAULT) {
            return;
        }

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup    = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile         = (ImageView) findViewById(R.id.mobile_signal);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType     = (ImageView) findViewById(R.id.mobile_type);
        mNoSimSlot      = (ImageView) findViewById(R.id.no_sim);
        mMobileGroupSub2    = (ViewGroup) findViewById(R.id.mobile_combo_sub2);
        mMobileSub2     = (ImageView) findViewById(R.id.mobile_signal_sub2);
        mMobileActivitySub2 = (ImageView) findViewById(R.id.mobile_inout_sub2);
        mMobileTypeSub2     = (ImageView) findViewById(R.id.mobile_type_sub2);
        mNoSimSlotSub2      = (ImageView) findViewById(R.id.no_sim_slot2);
        mSpacer         =             findViewById(R.id.spacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);

        applySubscription(MSimTelephonyManager.getDefault().getDefaultSubscription());
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup      = null;
        mWifi           = null;
        mWifiActivity   = null;
        mMobileGroup    = null;
        mMobile         = null;
        mMobileActivity = null;
        mMobileType     = null;
        mSpacer         = null;
        mNoSimSlot      = null;
        mMobileGroupSub2 = null;
        mMobileSub2     = null;
        mMobileActivitySub2 = null;
        mMobileTypeSub2 = null;
        mNoSimSlotSub2  = null;
        mAirplane       = null;

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        applySubscription(MSimTelephonyManager.getDefault().getDefaultSubscription());
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int[] strengthIcon, int activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            int noSimIcon, int subscription, ServiceState simServiceState, boolean isRoaming,
            boolean dataConnected) {

        mMobileVisible = visible;
        mMobileStrengthId[subscription] = strengthIcon[0];
        mMobileActivityId[subscription] = activityIcon;
        mMobileTypeId[subscription] = typeIcon;
        mMobileDescription[subscription] = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId[subscription] = noSimIcon;

        if (noSimIcon != 0) {
            mNoSimIconVisiable[subscription] = true;
            mSignalIconVisiable[subscription] = false;
        } else {
            mNoSimIconVisiable[subscription] = false;
            mSignalIconVisiable[subscription] = true;
        }
        if (DEBUG) {
            Slog.d(TAG, "setMobileDataIndicators MNoSimIconVisiable "
                    + subscription + " = " + mNoSimIconVisiable[subscription]);
        }

        applySubscription(subscription);
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        applySubscription(MSimTelephonyManager.getDefault().getDefaultSubscription());
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        if (mMobileVisible && mMobileGroup.getContentDescription() != null)
            event.getText().add(mMobileGroup.getContentDescription());
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    // Run after each indicator change.
    private void applySubscription(int subscription) {
        if (mWifiGroup == null || mMobileGroup == null || mMobileGroupSub2 == null) {
            return;
        }

        apply();

        if (mMobileVisible && !mIsAirplaneMode) {
            boolean useDefaultStyle =
                    PhoneStatusBar.STATUSBAR_STYLE == PhoneStatusBar.STATUSBAR_STYLE_DEFAULT;
            if (subscription == MSimConstants.SUB1) {
                mMobileGroup.setVisibility(View.VISIBLE);
                mMobile.setImageResource(mMobileStrengthId[subscription]);
                mMobile.setVisibility(
                        mSignalIconVisiable[subscription] ? View.VISIBLE : View.INVISIBLE);
                mMobileGroup.setContentDescription(mMobileTypeDescription + " "
                    + mMobileDescription[subscription]);
                mMobileActivity.setImageResource(mMobileActivityId[subscription]);
                mMobileType.setImageResource(mMobileTypeId[subscription]);
                mMobileType.setVisibility(
                    (!mWifiVisible && useDefaultStyle) ? View.VISIBLE : View.GONE);
                mNoSimSlot.setImageResource(mNoSimIconId[subscription]);
                mNoSimSlot.setVisibility(
                        mNoSimIconVisiable[subscription] ? View.VISIBLE : View.GONE);
            } else {
                mMobileGroupSub2.setVisibility(View.VISIBLE);
                mMobileSub2.setImageResource(mMobileStrengthId[subscription]);
                mMobileSub2.setVisibility(
                        mSignalIconVisiable[subscription] ? View.VISIBLE : View.GONE);
                mMobileGroupSub2.setContentDescription(mMobileTypeDescription + " "
                    + mMobileDescription[subscription]);
                mMobileActivitySub2.setImageResource(mMobileActivityId[subscription]);
                mMobileTypeSub2.setImageResource(mMobileTypeId[subscription]);
                mMobileTypeSub2.setVisibility(
                    (!mWifiVisible && useDefaultStyle) ? View.VISIBLE : View.GONE);
                mNoSimSlotSub2.setImageResource(mNoSimIconId[subscription]);
                mNoSimSlotSub2.setVisibility(
                        mNoSimIconVisiable[subscription] ? View.VISIBLE : View.GONE);
            }
        } else {
            if (subscription == 0) {
                mMobileGroup.setVisibility(View.GONE);
            } else {
                mMobileGroupSub2.setVisibility(View.GONE);
            }
        }

        if (mIsAirplaneMode) {
            mAirplane.setVisibility(View.VISIBLE);
            mAirplane.setImageResource(mAirplaneIconId);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (subscription != 0) {
            if (mMobileVisible && mWifiVisible && ((mIsAirplaneMode) ||
                    (mNoSimIconId[subscription] != 0))) {
                mSpacer.setVisibility(View.INVISIBLE);
            } else {
                mSpacer.setVisibility(View.GONE);
            }
        }

    }

    // Apply the change after each indicator change.
    protected void apply() {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) {
            Slog.d(TAG, String.format("wifi: %s sig=%d act=%d", (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));
        }
    }
}

