/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
         copyright notice, this list of conditions and the following
         disclaimer in the documentation and/or other materials provided
         with the distribution.
 * Neither the name of The Linux Foundation, Inc. nor the names of its
         contributors may be used to endorse or promote products derived
         from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
   ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
   IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.systemui.statusbar;

import android.content.Context;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.MSimTelephonyManager;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.Log;

import com.android.internal.telephony.MSimConstants;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.MSimNetworkController;
import com.android.systemui.statusbar.policy.TelephonyIcons;

import com.android.systemui.R;

// Intimately tied to the design of res/layout/msim_signal_cluster_view.xml
public class CUMSimSignalClusterView extends MSimSignalClusterView implements
        MSimNetworkController.MSimSignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "CUMSimSignalCluster";

    MSimNetworkController mMSimNC;

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
    private boolean[] mMNoSimIconVisible;
    private boolean[] mSignalIconVisible;
    private ServiceState[] mServiceState;

    ViewGroup mWifiGroup, mMobileGroup, mMobileGroupSub2;
    ImageView mWifi, mWifiActivity, mMobile, mMobileActivity, mMobileType, mAirplane;
    ImageView mNoSimSlot, mNoSimSlotSub2;
    ImageView mMobileSub2, mMobileActivitySub2, mMobileTypeSub2;
    View mSpacer;

    public CUMSimSignalClusterView(Context context) {
        this(context, null);
    }

    public CUMSimSignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CUMSimSignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        mMobileStrengthId = new int[numPhones];
        mMobileDescription = new String[numPhones];
        mMobileTypeId = new int[numPhones];
        mMobileActivityId = new int[numPhones];
        mServiceState = new ServiceState[numPhones];
        mNoSimIconId = new int[numPhones];
        mMNoSimIconVisible = new boolean[numPhones];
        mSignalIconVisible = new boolean[numPhones];
        for (int i = 0; i < numPhones; i++) {
            mMobileStrengthId[i] = 0;
            mMobileTypeId[i] = 0;
            mMobileActivityId[i] = 0;
            mNoSimIconId[i] = 0;
            mMNoSimIconVisible[i] = false;
            mSignalIconVisible[i] = false;
            mServiceState[i] = new ServiceState();
        }
    }

    public void setNetworkController(MSimNetworkController nc) {
        if (DEBUG) {
            Slog.d(TAG, "MSimNetworkController=" + nc);
        }
        mMSimNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile = (ImageView) findViewById(R.id.mobile_signal);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType = (ImageView) findViewById(R.id.mobile_type);
        mNoSimSlot = (ImageView) findViewById(R.id.no_sim);
        mMobileGroupSub2 = (ViewGroup) findViewById(R.id.mobile_combo_sub2);
        mMobileSub2 = (ImageView) findViewById(R.id.mobile_signal_sub2);
        mMobileActivitySub2 = (ImageView) findViewById(R.id.mobile_inout_sub2);
        mMobileTypeSub2 = (ImageView) findViewById(R.id.mobile_type_sub2);
        mNoSimSlotSub2 = (ImageView) findViewById(R.id.no_sim_slot2);
        mSpacer = findViewById(R.id.spacer);
        mAirplane = (ImageView) findViewById(R.id.airplane);

        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup = null;
        mWifi = null;
        mWifiActivity = null;
        mMobileGroup = null;
        mMobile = null;
        mMobileActivity = null;
        mMobileType = null;
        mSpacer = null;
        mNoSimSlot = null;
        mMobileGroupSub2 = null;
        mMobileSub2 = null;
        mMobileActivitySub2 = null;
        mMobileTypeSub2 = null;
        mNoSimSlotSub2 = null;
        mAirplane = null;
        super.onDetachedFromWindow();
    }

    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    public void setMobileDataIndicators(boolean visible, int[] strengthIcon, int activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            int noSimIcon, int subscription, ServiceState simServiceState, boolean isRoaming,
            boolean dataConnect) {
        mMobileVisible = visible;
        mMobileStrengthId[subscription] = convertStrengthIconIdToCU(strengthIcon[0], subscription);
        mMobileActivityId[subscription] = activityIcon;
        mMobileTypeId[subscription] = typeIcon;
        mMobileDescription[subscription] = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId[subscription] = convertNoSimIconIdToCU(subscription);
        mServiceState[subscription] = simServiceState;

        if (noSimIcon != 0) {
            mMNoSimIconVisible[subscription] = true;
            mSignalIconVisible[subscription] = false;
        } else {
            mMNoSimIconVisible[subscription] = false;
            mSignalIconVisible[subscription] = true;
        }
        if (DEBUG) {
            Log.i(TAG, "SetMobileDataIndicators MNoSimIconVisible " + subscription + "="
                    + mMNoSimIconVisible[subscription]);
        }

        applySubscription(subscription);
    }

    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;
        applySubscription(MSimTelephonyManager.getDefault().getDefaultSubscription());
    }

    private void applySubscription(int subscription) {
        apply();

        if (mMobileVisible && !mIsAirplaneMode) {
            boolean useDefaultStyle =
                    PhoneStatusBar.STATUSBAR_STYLE == PhoneStatusBar.STATUSBAR_STYLE_DEFAULT;
            if (subscription == MSimConstants.SUB1) {
                mMobileGroup.setVisibility(View.VISIBLE);
                mMobile.setImageResource(mMobileStrengthId[subscription]);
                mMobile.setVisibility(mSignalIconVisible[subscription] ? View.VISIBLE : View.GONE);
                mMobileGroup.setContentDescription(mMobileTypeDescription + " "
                        + mMobileDescription[subscription]);
                mMobileActivity.setImageResource(mMobileActivityId[subscription]);
                mMobileType.setImageResource(mMobileTypeId[subscription]);
                mMobileType.setVisibility((!mWifiVisible && useDefaultStyle) ? View.VISIBLE
                        : View.GONE);
                mNoSimSlot.setImageResource(mNoSimIconId[subscription]);
                mNoSimSlot.setVisibility(mMNoSimIconVisible[subscription] ? View.VISIBLE
                        : View.GONE);
            } else {
                mMobileGroupSub2.setVisibility(View.VISIBLE);
                mMobileSub2.setImageResource(mMobileStrengthId[subscription]);
                mMobileSub2.setVisibility(mSignalIconVisible[subscription] ? View.VISIBLE
                        : View.GONE);
                mMobileGroupSub2.setContentDescription(mMobileTypeDescription + " "
                        + mMobileDescription[subscription]);
                mMobileActivitySub2.setImageResource(mMobileActivityId[subscription]);
                mMobileTypeSub2.setImageResource(mMobileTypeId[subscription]);
                mMobileTypeSub2.setVisibility((!mWifiVisible && useDefaultStyle) ? View.VISIBLE
                        : View.GONE);
                mNoSimSlotSub2.setImageResource(mNoSimIconId[subscription]);
                mNoSimSlotSub2.setVisibility(mMNoSimIconVisible[subscription] ? View.VISIBLE
                        : View.GONE);
            }
        } else {
            if (subscription == 0) {
                mMobileGroup.setVisibility(View.GONE);
            } else {
                mMobileGroupSub2.setVisibility(View.GONE);
            }
        }
        if (mIsAirplaneMode) {
            mMobileGroup.setVisibility(View.GONE);
            mMobileGroupSub2.setVisibility(View.GONE);
            mAirplane.setVisibility(View.VISIBLE);
            mAirplane.setImageResource(mAirplaneIconId);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mMobileVisible && mWifiVisible && mIsAirplaneMode) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) {
            return;
        }

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) {
            Slog.d(TAG, String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"), mWifiStrengthId, mWifiActivityId));
        }
    }

    private int convertStrengthIconIdToCU(int orignalId, int subscription) {
        int level = 0;
        int inetCondition = 0;
        switch (orignalId) {
            case R.drawable.stat_sys_signal_0:
                level = TelephonyIcons.SIGNAL_LEVEL_0;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_NOT_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_1:
                level = TelephonyIcons.SIGNAL_LEVEL_1;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_NOT_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_2:
                level = TelephonyIcons.SIGNAL_LEVEL_2;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_NOT_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_3:
                level = TelephonyIcons.SIGNAL_LEVEL_3;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_NOT_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_4:
                level = TelephonyIcons.SIGNAL_LEVEL_4;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_NOT_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_0_fully:
                level = TelephonyIcons.SIGNAL_LEVEL_0;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_1_fully:
                level = TelephonyIcons.SIGNAL_LEVEL_1;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_2_fully:
                level = TelephonyIcons.SIGNAL_LEVEL_2;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_3_fully:
                level = TelephonyIcons.SIGNAL_LEVEL_3;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_4_fully:
                level = TelephonyIcons.SIGNAL_LEVEL_4;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_null:
                return convertSignalNullIconIdToCU(subscription);
            default:
                return orignalId;
        }
        return getCUSignalStrenthIconId(subscription, inetCondition, level);
    }

    private int convertNoSimIconIdToCU(int subscription) {
        return TelephonyIcons.MULTI_NO_SIM_CU[subscription];
    }

    private int convertSignalNullIconIdToCU(int subscription) {
        return TelephonyIcons.MULTI_SIGNAL_NULL_CU[subscription];
    }

    private int getCUSignalStrenthIconId(int subscription,
            int inetCondition, int level) {
        /* find out radio technology by looking at service state */
        if (mServiceState[subscription] == null) {
            return 0;
        }

        int radioTechnology = mServiceState[subscription].getRadioTechnology();
        if (radioTechnology == 0) {
            radioTechnology = mServiceState[subscription].getRilVoiceRadioTechnology();
        }
        if (DEBUG) {
            Log.d(TAG, subscription + ":radio technology is:"
                    + mServiceState[subscription].getRadioTechnology());
        }

        switch (radioTechnology) {
            case ServiceState.RIL_RADIO_TECHNOLOGY_GPRS:
            case ServiceState.RIL_RADIO_TECHNOLOGY_EDGE:
                return TelephonyIcons.MULTI_SIGNAL_IMAGES_G[subscription][inetCondition][level];
            case ServiceState.RIL_RADIO_TECHNOLOGY_LTE:
            case ServiceState.RIL_RADIO_TECHNOLOGY_UMTS:
            case ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD:
                return TelephonyIcons.MULTI_SIGNAL_IMAGES_3G[subscription][inetCondition][level];
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP:
                return TelephonyIcons.MULTI_SIGNAL_IMAGES_H[subscription][inetCondition][level];
            default:
                return TelephonyIcons.MULTI_SIGNAL_IMAGES_G[subscription][inetCondition][level];
        }
    }
}
