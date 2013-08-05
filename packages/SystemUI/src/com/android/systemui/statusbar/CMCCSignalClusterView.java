/*
 * Copyright (c) 2012, The Linux Foundation. All rights reserved.
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;



import com.android.systemui.R;
import com.qrd.plugin.feature_query.DefaultQuery;

// Intimately tied to the design of res/layout/msim_signal_cluster_view.xml
public class CMCCSignalClusterView
        extends SignalClusterView
        implements NetworkController.CMCCSignalCluster {

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
    private boolean mMNoSimIconVisiable=false;
    private boolean mSignalIconVisiable=false;
    private ServiceState mServiceState;
    private boolean isSimRoam;
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

    public void setNetworkController(MSimNetworkController nc) {
        if (DEBUG) Slog.d(TAG, "MSimNetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);

        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup    = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile         = (ImageView) findViewById(R.id.mobile_signal);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType     = (ImageView) findViewById(R.id.mobile_type);
        mNoSimSlot      = (ImageView) findViewById(R.id.no_sim);
        mSpacer         =             findViewById(R.id.spacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        apply();
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
        mSpacer  =  null;
        mNoSimSlot      = null;
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
            int noSimIcon,  ServiceState simServiceState,boolean isRoam,boolean dataConnect) {
        
       Log.i(TAG, "setMobileDataIndicators=simServiceState111=" +simServiceState );
       Log.i(TAG, "setMobileDataIndicators=mServiceState111=" +mServiceState );
	 mServiceState = simServiceState;
        isSimRoam = isRoam;
	 Log.i(TAG, "setMobileDataIndicators=mServiceState222=" +mServiceState );
        mMobileVisible = visible;
        mMobileStrengthId = convertStrengthIconIdToCMCC(strengthIcon, 0);
        mMobileActivityId = convertMobileActivityIconIdToCMCC(typeIcon,activityIcon);
        mMobileTypeId = typeIcon;
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId= convertNoSimIconIdToCMCC(0);


        if (noSimIcon != 0) {
            mMNoSimIconVisiable = true;
            mSignalIconVisiable = false;	
        } else {
            mMNoSimIconVisiable = false;
            mSignalIconVisiable = true;
        }
        if (DEBUG)
        Log.i(TAG,"SetMobileDataIndicators MNoSimIconVisiable "+"="+mMNoSimIconVisiable);

        apply();
        applySubscription(0);
    }

    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;
        applySubscription(0);
    }
    private void applySubscription(int subscription) {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }
        if (DEBUG) Slog.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));

        if (mMobileVisible && !mIsAirplaneMode) {
            if (subscription == 0) {
                mMobileGroup.setVisibility(View.VISIBLE);
                mMobile.setImageResource(mMobileStrengthId);
                mMobile.setVisibility(mSignalIconVisiable ? View.VISIBLE : View.GONE);
                mMobileGroup.setContentDescription(mMobileTypeDescription + " "
                    + mMobileDescription);
                mMobileActivity.setImageResource(mMobileActivityId);
                mMobileType.setImageResource(mMobileTypeId);
                mMobileType.setVisibility(
                    (!mWifiVisible && DefaultQuery.STATUSBAR_STYLE == PhoneStatusBar.STATUSBAR_STYLE_DEFAULT)
                    ? View.VISIBLE : View.GONE);
                mNoSimSlot.setImageResource(mNoSimIconId );
                mNoSimSlot.setVisibility(mMNoSimIconVisiable ? View.VISIBLE : View.GONE);
            } 
        } else {
            if (subscription == 0) {
                mMobileGroup.setVisibility(View.GONE);
            }
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
        if (mWifiGroup == null) return;

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
          } else
	   {
             mMobileActivity.setVisibility(View.GONE);
		}
        if (DEBUG) Slog.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));
    }

    private int convertStrengthIconIdToCMCC(int orignalId, int subscription) {
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
                return convertSignalNullIconIdToCMCC(subscription);
            default:
                return orignalId;
        }
        return getCMCCSignalStrenthIconId(subscription,inetCondition,level);
    }

    private int convertNoSimIconIdToCMCC(int subscription) {
        return TelephonyIcons.MULTI_NO_SIM_CMCC[subscription];
    }

    private int convertSignalNullIconIdToCMCC(int subscription) {
        return TelephonyIcons.MULTI_SIGNAL_NULL_CMCC[subscription];
    }

    private int getCMCCSignalStrenthIconId(int subscription,
            int inetCondition, int level) {
        /* find out radio technology by looking at service state */
        if (mServiceState == null) {
            return 0;
        }
	Log.i(TAG,":radio technology ismServiceState:"+mServiceState);
        int radioTechnology = mServiceState.getRadioTechnology();
        Log.i(TAG,":radio technology is:"+mServiceState.getRadioTechnology());
        if(radioTechnology == 0)
        radioTechnology = mServiceState.getVoiceRadioTechnology();// getRilRadioTechnology
        switch (radioTechnology) {
        case ServiceState.RIL_RADIO_TECHNOLOGY_IS95A:
        case ServiceState.RIL_RADIO_TECHNOLOGY_IS95B:
        case ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT:
        case ServiceState.RIL_RADIO_TECHNOLOGY_GPRS:
         if(isSimRoam){
            return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_G[inetCondition][level];}
         else{
            return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_G[inetCondition][level];}
        case ServiceState.RIL_RADIO_TECHNOLOGY_LTE:
	 case ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA:
        case ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA:
        case ServiceState.RIL_RADIO_TECHNOLOGY_HSPA:
        case ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP:
	 case ServiceState.RIL_RADIO_TECHNOLOGY_TD_SCDMA:
	 case ServiceState.RIL_RADIO_TECHNOLOGY_UMTS:
        if(isSimRoam){
          return  TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_3G[inetCondition][level];}
         else{
            return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_3G[inetCondition][level];}
	 case ServiceState.RIL_RADIO_TECHNOLOGY_EDGE:
	 case ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD:
         if(isSimRoam){
            return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_E[inetCondition][level];}
         else{
           return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_E[inetCondition][level];}
        default:
            if(isSimRoam){
            return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_R_G[inetCondition][level];}
            else{
            return TelephonyIcons.CMCC_DATA_SIGNAL_STRENGTH_G[inetCondition][level];}
        }
    }
   private int convertMobileActivityIconIdToCMCC(int typeicon, int activityicon) {
		int cuMobileTypeID = 0;
		int cuMobileActivityID = 0;
		Slog.d(TAG,"convertMobileTypeIconIdToCU typeicon="+typeicon+" activityicon="+activityicon);
	        switch (activityicon) {
	            case R.drawable.stat_sys_signal_in:
			cuMobileActivityID=MOBILE_DATA_CONNECT_ACTIVITY_IN;
			break;
	            case R.drawable.stat_sys_signal_out:
			cuMobileActivityID=MOBILE_DATA_CONNECT_ACTIVITY_OUT;
			break;
	            case R.drawable.stat_sys_signal_inout:
			cuMobileActivityID=MOBILE_DATA_CONNECT_ACTIVITY_INOUT;
			break;
	            default:
			cuMobileActivityID=MOBILE_DATA_CONNECT_ACTIVITY_IDLE;
			break;
	        }
	        switch (typeicon) {
	            case R.drawable.stat_sys_data_connected_1x:
			cuMobileTypeID=MOBILE_DATA_CONNECT_TYPE_1X;
			break;
	            case R.drawable.stat_sys_data_connected_g:
			cuMobileTypeID=MOBILE_DATA_CONNECT_TYPE_G;
			break;
	            case R.drawable.stat_sys_data_connected_3g:
			cuMobileTypeID=MOBILE_DATA_CONNECT_TYPE_3G;
			break;
	            case R.drawable.stat_sys_data_connected_4g:
			cuMobileTypeID=MOBILE_DATA_CONNECT_TYPE_4G;
			break;
	            case R.drawable.stat_sys_data_connected_e:
			cuMobileTypeID=MOBILE_DATA_CONNECT_TYPE_E;
			break;
	            case R.drawable.stat_sys_data_connected_h:
	            case R.drawable.stat_sys_data_connected_roam:
			cuMobileTypeID=MOBILE_DATA_CONNECT_TYPE_H;
			break;
	            default:
			cuMobileTypeID=MOBILE_DATA_CONNECT_TYPE_E;
			break;
	        }
		Slog.d(TAG,"subscription="+" convertMobileTypeIconIdToCU cuMobileActivityID="+cuMobileActivityID+" cuMobileTypeID="+cuMobileTypeID);
		int dataIcon = MOBILE_DATA_CONNECT_ICON_CU[cuMobileActivityID][cuMobileTypeID];
		if(!isSubDataConnect()){
			return 0;
		}
		return dataIcon;
	}

	private static int MOBILE_DATA_CONNECT_ACTIVITY_IN = 0;
	private static int MOBILE_DATA_CONNECT_ACTIVITY_OUT = 1;
	private static int MOBILE_DATA_CONNECT_ACTIVITY_INOUT = 2;
	private static int MOBILE_DATA_CONNECT_ACTIVITY_IDLE = 3;
	private static int MOBILE_DATA_CONNECT_ACTIVITY_MAX = 4;
	
	private static int MOBILE_DATA_CONNECT_TYPE_1X = 0;
	private static int MOBILE_DATA_CONNECT_TYPE_G = 1;
	private static int MOBILE_DATA_CONNECT_TYPE_3G = 2;
	private static int MOBILE_DATA_CONNECT_TYPE_4G = 3;
	private static int MOBILE_DATA_CONNECT_TYPE_E = 4;
	private static int MOBILE_DATA_CONNECT_TYPE_H = 5;
	private static int MOBILE_DATA_CONNECT_TYPE_R = 6;
	private static int MOBILE_DATA_CONNECT_TYPE_MAX = 7;
	
	private static final int[][] MOBILE_DATA_CONNECT_ICON_CU = {
	        { R.drawable.stat_sys_data_in_1x,
	          R.drawable.stat_sys_data_in_g,
	          R.drawable.stat_sys_data_in_3g,
	          R.drawable.stat_sys_data_in_4g,
	          R.drawable.stat_sys_data_in_e,
	          R.drawable.stat_sys_data_in_h,
	          R.drawable.stat_sys_data_in_e },
	        { R.drawable.stat_sys_data_out_1x,
	          R.drawable.stat_sys_data_out_g,
	          R.drawable.stat_sys_data_out_3g,
	          R.drawable.stat_sys_data_out_4g,
	          R.drawable.stat_sys_data_out_e,
	          R.drawable.stat_sys_data_out_h,
	          R.drawable.stat_sys_data_out_e },
	        { R.drawable.stat_sys_data_inout_1x,
	          R.drawable.stat_sys_data_inout_g,
	          R.drawable.stat_sys_data_inout_3g,
	          R.drawable.stat_sys_data_inandout_4g,
	          R.drawable.stat_sys_data_inandout_e,
	          R.drawable.stat_sys_data_inandout_h,
	          R.drawable.stat_sys_data_inandout_e },
	        { R.drawable.stat_sys_data_idle_1x,
	          R.drawable.stat_sys_data_idle_g,
	          R.drawable.stat_sys_data_idle_3g,
	          R.drawable.stat_sys_data_idle_4g,
	          R.drawable.stat_sys_data_idle_e,
	          R.drawable.stat_sys_data_idle_h,
	          R.drawable.stat_sys_data_idle_e  },
	};

	private boolean isSubDataConnect() {
	
		boolean Data_connect_on = false;
		
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		Data_connect_on = cm.getMobileDataEnabled();
		
		boolean data_on=(cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState()==NetworkInfo.State.CONNECTED);
		boolean mms_data_on = (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS).getState()==NetworkInfo.State.CONNECTED);
		Slog.d(TAG,  " Data_connect_on=" + Data_connect_on+" data_on="+data_on+" mms_data_on="+mms_data_on+" NetworkInfo.State.CONNECTED="+NetworkInfo.State.CONNECTED);
		if ((data_on && (Data_connect_on == true))||mms_data_on) {
			return true;
		} else {
			return false;
		}

	}
}


