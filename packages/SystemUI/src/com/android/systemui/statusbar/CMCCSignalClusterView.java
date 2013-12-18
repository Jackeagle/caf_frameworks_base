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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.TelephonyIcons;

import com.android.systemui.R;

// Intimately tied to the design of res/layout/signal_cluster_view_cmcc.xml
public class CMCCSignalClusterView
        extends SignalClusterView
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = true;
    static final String TAG = "CMCCSignalClusterView";

    NetworkController mNC;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private boolean mMobileVisible = false;
    private int mMobileStrengthId;
    private int mMobileActivityId;
    private int mMobileTypeId;
    private int mNoSimIconId;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileTypeDescription;
    private String mMobileDescription;
    private boolean mMNoSimIconVisiable = false;
    private boolean mSignalIconVisiable = false;
    private ServiceState mServiceState;
    private boolean isSimRoam;
    private int mTypeIcon = 0;
    ViewGroup mWifiGroup, mMobileGroup;
    ImageView mWifi, mWifiActivity, mMobile, mMobileActivity, mMobileType, mAirplane;
    ImageView mNoSimSlot;

    View mSpacer;

    public CMCCSignalClusterView(Context context) {
        this(context, null);
    }

    public CMCCSignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CMCCSignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mServiceState = new ServiceState();
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG)
            Slog.d(TAG, "NetworkController=" + nc);
        mNC = nc;
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

    public void setMobileDataIndicators(boolean visible, int strengthIcon, int activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            int noSimIcon, ServiceState simServiceState, boolean isRoam) {

        mServiceState = simServiceState;
        isSimRoam = isRoam;
        mMobileVisible = visible;
        mMobileStrengthId = convertStrengthIconIdToCMCC(strengthIcon);
        mMobileActivityId = convertMobileActivityIconIdToCMCC(typeIcon, activityIcon);
        mMobileTypeId = typeIcon;
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId = TelephonyIcons.MULTI_NO_SIM_CMCC[0];

        if (noSimIcon != 0) {
            mMNoSimIconVisiable = true;
            mSignalIconVisiable = false;
        } else {
            mMNoSimIconVisiable = false;
            mSignalIconVisiable = true;
        }
        if (DEBUG)
            Log.i(TAG, "SetMobileDataIndicators MNoSimIconVisiable " + "=" + mMNoSimIconVisiable);

        applySubscription();
    }

    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;
        applySubscription();
    }

    private void applySubscription() {
        if (mWifiGroup == null)
            return;

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }
        if (DEBUG)
            Slog.d(TAG,
                    String.format("wifi: %s sig=%d act=%d",
                            (mWifiVisible ? "VISIBLE" : "GONE"),
                            mWifiStrengthId, mWifiActivityId));

        if (mMobileVisible && !mIsAirplaneMode) {
            mMobileGroup.setVisibility(View.VISIBLE);
            mMobile.setImageResource(mMobileStrengthId);
            mMobile.setVisibility(mSignalIconVisiable ? View.VISIBLE : View.GONE);
            mMobileGroup.setContentDescription(mMobileTypeDescription + " "
                        + mMobileDescription);
            mMobileActivity.setImageResource(mMobileActivityId);
            mMobileType.setImageResource(mMobileTypeId);
            mMobileType.setVisibility((!mWifiVisible &&
                SystemProperties.getInt(
                "persist.env.c.sb.style", PhoneStatusBar.STATUSBAR_STYLE_DEFAULT)
                == PhoneStatusBar.STATUSBAR_STYLE_DEFAULT)
                ? View.VISIBLE: View.GONE);
            mNoSimSlot.setImageResource(mNoSimIconId);
            mNoSimSlot.setVisibility(mMNoSimIconVisiable ? View.VISIBLE : View.GONE);
        } else {
            mMobileGroup.setVisibility(View.GONE);
        }
        if (mIsAirplaneMode) {
            mMobileGroup.setVisibility(View.GONE);
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
        if (mWifiGroup == null)
            return;

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (mMobileVisible && !mIsAirplaneMode) {
            mMobileActivity.setVisibility(View.VISIBLE);
        } else {
            mMobileActivity.setVisibility(View.GONE);
        }
        if (DEBUG)
            Slog.d(TAG,
                    String.format("wifi: %s sig=%d act=%d",
                            (mWifiVisible ? "VISIBLE" : "GONE"),
                            mWifiStrengthId, mWifiActivityId));
    }

    private int convertStrengthIconIdToCMCC(int orignalId) {
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
            case R.drawable.stat_sys_signal_5:
                level = TelephonyIcons.SIGNAL_LEVEL_5;
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
            case R.drawable.stat_sys_signal_5_fully:
                level = TelephonyIcons.SIGNAL_LEVEL_5;
                inetCondition = TelephonyIcons.DATA_CONNECTIVITY_CONNECTED;
                break;
            case R.drawable.stat_sys_signal_null:
                return TelephonyIcons.MULTI_SIGNAL_NULL_CMCC[0];
            default:
                return orignalId;
        }
        return getCMCCSignalStrenthIconId(inetCondition, level);
    }

    private int getCMCCSignalStrenthIconId(int inetCondition, int level) {
        /* find out radio technology by looking at service state */
        if (mServiceState == null) {
            return 0;
        }
        int radioTechnology = mServiceState.getRadioTechnology();
        if (radioTechnology == 0)
            radioTechnology = mServiceState.getRilVoiceRadioTechnology();// getRilRadioTechnology
        switch (radioTechnology) {
            case ServiceState.RIL_RADIO_TECHNOLOGY_IS95A:
            case ServiceState.RIL_RADIO_TECHNOLOGY_IS95B:
            case ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT:
            case ServiceState.RIL_RADIO_TECHNOLOGY_GPRS:
                mTypeIcon = R.drawable.stat_sys_data_connected_g;
                if (isSimRoam) {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_G[inetCondition][level];
                } else {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_G[inetCondition][level];
                }
            case ServiceState.RIL_RADIO_TECHNOLOGY_LTE:
                mTypeIcon = R.drawable.stat_sys_data_connected_4g;
                if (isSimRoam) {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_4G[inetCondition][level];
                } else {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_4G[inetCondition][level];
                }
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP:
            case ServiceState.RIL_RADIO_TECHNOLOGY_TD_SCDMA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_UMTS:
            case ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD:
                mTypeIcon = R.drawable.stat_sys_data_connected_3g;
                if (isSimRoam) {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_3G[inetCondition][level];
                } else {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_3G[inetCondition][level];
                }
            case ServiceState.RIL_RADIO_TECHNOLOGY_EDGE:
                mTypeIcon = R.drawable.stat_sys_data_connected_e;
                if (isSimRoam) {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_E[inetCondition][level];
                } else {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_E[inetCondition][level];
                }
            default:
                mTypeIcon = R.drawable.stat_sys_data_connected_g;
                if (isSimRoam) {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_G[inetCondition][level];
                } else {
                    return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_G[inetCondition][level];
                }
        }
    }

    private int convertMobileActivityIconIdToCMCC(int typeicon, int activityicon) {
        if (!isDataConnect()) {
            return 0;
        }
        int cmccMobileTypeID = 0;
        int cmccMobileActivityID = 0;
        Slog.d(TAG, "convertMobileTypeIconIdToCMCC typeicon=" + typeicon + " activityicon="
                + activityicon + " mTypeIcon=" + mTypeIcon);
        switch (activityicon) {
            case R.drawable.stat_sys_signal_in:
                cmccMobileActivityID = TelephonyIcons.MOBILE_DATA_CONNECT_ACTIVITY_IN;
                break;
            case R.drawable.stat_sys_signal_out:
                cmccMobileActivityID = TelephonyIcons.MOBILE_DATA_CONNECT_ACTIVITY_OUT;
                break;
            case R.drawable.stat_sys_signal_inout:
                cmccMobileActivityID = TelephonyIcons.MOBILE_DATA_CONNECT_ACTIVITY_INOUT;
                break;
            default:
                cmccMobileActivityID = TelephonyIcons.MOBILE_DATA_CONNECT_ACTIVITY_IDLE;
                break;
        }
        switch (mTypeIcon) {
            case R.drawable.stat_sys_data_connected_1x:
                cmccMobileTypeID = TelephonyIcons.MOBILE_DATA_CONNECT_TYPE_1X;
                break;
            case R.drawable.stat_sys_data_connected_g:
                cmccMobileTypeID = TelephonyIcons.MOBILE_DATA_CONNECT_TYPE_G;
                break;
            case R.drawable.stat_sys_data_connected_3g:
                cmccMobileTypeID = TelephonyIcons.MOBILE_DATA_CONNECT_TYPE_3G;
                break;
            case R.drawable.stat_sys_data_connected_4g:
                cmccMobileTypeID = TelephonyIcons.MOBILE_DATA_CONNECT_TYPE_4G;
                break;
            case R.drawable.stat_sys_data_connected_e:
                cmccMobileTypeID = TelephonyIcons.MOBILE_DATA_CONNECT_TYPE_E;
                break;
            case R.drawable.stat_sys_data_connected_h:
            case R.drawable.stat_sys_data_connected_roam:
                cmccMobileTypeID = TelephonyIcons.MOBILE_DATA_CONNECT_TYPE_H;
                break;
            default:
                cmccMobileTypeID = TelephonyIcons.MOBILE_DATA_CONNECT_TYPE_3G;
                break;
        }
        Slog.d(TAG, "subscription=" + " convertMobileTypeIconIdToCMCC cmccMobileActivityID="
                + cmccMobileActivityID + " cmccMobileTypeID=" + cmccMobileTypeID);
        int dataIcon = TelephonyIcons.
            MOBILE_DATA_CONNECT_ICON_CMCC[cmccMobileActivityID][cmccMobileTypeID];
        return dataIcon;
    }

    private boolean isDataConnect() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == cm) {
            Slog.d(TAG, "failed to get ConnectivityManager.");
            return false;
        }
        NetworkInfo.State mobileState = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .getState();
        NetworkInfo.State mmsState = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS)
                .getState();
        boolean isDataOn = (mobileState == NetworkInfo.State.CONNECTED);
        boolean isMMSOn = (mmsState == NetworkInfo.State.CONNECTED);
        boolean isDataConnectOn = cm.getMobileDataEnabled();
        Slog.d(TAG, " isDataOn=" + isDataOn + " isDataConnectOn=" + isDataConnectOn + " isMMSOn="
                + isMMSOn + " NetworkInfo.State.CONNECTED=" + NetworkInfo.State.CONNECTED);

        boolean result = false;

        if (isMMSOn || (isDataConnectOn && isDataOn)) {
            result = true;
        }
        return result;
    }
}
