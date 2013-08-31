/**
 * Copyright (C) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
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

package com.android.systemui.statusbar.phone;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.internal.telephony.MSimConstants;
import com.android.systemui.R;

class ExtQuickSettingsModel extends QuickSettingsModel {

    private final Context mContext;

    // Roaming Data
    private int mPhoneCount;
    private boolean mIsForeignState = false;
    private QuickSettingsBasicTile mRoamingTile;
    private RefreshCallback mRoamingCallback;
    private State mRoamingState = new State();

    /** ContentObserver to roaming data **/
    private class RoamingDataObserver extends ContentObserver {
        public RoamingDataObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onRoamingDataStateChanged();
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(
                    Settings.Global.CONTENT_URI, true, this);
        }
    }

    public ExtQuickSettingsModel(Context context) {
        super(context);
        mContext = context;

        if (SystemProperties.getBoolean("persist.env.phone.global", false)) {
            Handler handler = new Handler();
            RoamingDataObserver roamingDataObserver = new RoamingDataObserver(handler);
            roamingDataObserver.startObserving();
        }
    }

    public QuickSettingsBasicTile addRoamingTile() {
        mRoamingTile = new QuickSettingsBasicTile(mContext);
        mRoamingTile.setText(mContext.getResources()
                    .getString(R.string.accessibility_data_connection_roaming));
        mRoamingTile.setVisibility(View.VISIBLE);
        mRoamingTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean enable = getDataRoaming(MSimConstants.SUB1);
                setDataRoaming(!enable, MSimConstants.SUB1);
            }
        });

        mRoamingCallback = new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView unused, State state) {
                if (state.enabled) {
                    mRoamingTile.setImageResource(R.drawable.roam_on);
                } else {
                    mRoamingTile.setImageResource(R.drawable.roam_off);
                }
            }
        };

        // Get phone count
        mPhoneCount = MSimTelephonyManager.getDefault().getPhoneCount();
        // Set RoamingTile as GONE
        mRoamingTile.setVisibility(View.GONE);

        return mRoamingTile;
    }

    private boolean isValidNumeric(String numeric) {
        if (TextUtils.isEmpty(numeric)
                || numeric.equals("null") || numeric.equals("00000")) {
            return false;
        }

        return true;
    }

    public void onRoamingVisibleChanged() {
        final String CHINA_MCC = "460";
        final String MACAO_MCC = "455";

        String numeric;
        boolean isForeign;

        if (mPhoneCount > 1) {
            numeric = MSimTelephonyManager.getDefault()
                    .getNetworkOperator(MSimConstants.SUB1);
        } else {
            numeric = TelephonyManager.getDefault().getNetworkOperator();
        }

        // Return if invaild values
        if (!isValidNumeric(numeric)) {
            return;
        }

        if (numeric.startsWith(CHINA_MCC) || numeric.startsWith(MACAO_MCC)) {
            isForeign = false;
        } else {
            isForeign = true;
        }

        if (isForeign != mIsForeignState) {
            if (isForeign) {
                mRoamingTile.setVisibility(View.VISIBLE);
                mRoamingState.enabled = getDataRoaming(MSimConstants.SUB1);
                mRoamingCallback.refreshView(mRoamingTile, mRoamingState);
            } else {
                mRoamingTile.setVisibility(View.GONE);
            }
        }
        mIsForeignState = isForeign;
    }

    private void onRoamingDataStateChanged() {
        // Update Roaming Data State
        mRoamingState.enabled = getDataRoaming(MSimConstants.SUB1);
        mRoamingCallback.refreshView(mRoamingTile, mRoamingState);
    }

    // Get Data roaming flag, from DB, as per SUB.
    private boolean getDataRoaming(int sub) {
        String val = mPhoneCount > 1 ? String.valueOf(sub) : "";
        boolean enabled = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.DATA_ROAMING + val, 0) != 0;
        return enabled;
    }

    // Set Data roaming flag, in DB, as per SUB.
    private void setDataRoaming(boolean enabled, int sub) {
        if (mPhoneCount > 1) {
            // as per SUB, set the individual flag
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.DATA_ROAMING + sub, enabled ? 1 : 0);

            if (sub == MSimTelephonyManager.getDefault().getPreferredDataSubscription()) {
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.DATA_ROAMING, enabled ? 1 : 0);
            }
        } else {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.DATA_ROAMING, enabled ? 1 : 0);
        }
    }
}
