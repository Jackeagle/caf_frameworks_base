/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTileView;

/** Quick settings tile: Dds switch **/
public class DdsTile extends QSTile<QSTile.State> {
    private final boolean DEBUG = false;
    private final String TAG = "DdsTile";

    private boolean mListening;

    private QSTileView mQSTileView = null;
    private final DdsObserver mDdsObserver;

    public DdsTile(Host host) {
        super(host);
        mDdsObserver = new DdsObserver(mHandler);
    }

    @Override
    protected State newTileState() {
        return new State();
    }

    @Override
    public QSTileView createTileView(Context context) {
        mQSTileView = new QSTileView(context);
        return mQSTileView;
    }

    @Override
    public void handleClick() {
        switchDdsToNext();
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        state.visible = true;

        int dataPhoneId = PhoneConstants.PHONE_ID1;
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            dataPhoneId = SubscriptionManager.getPhoneId(
                    SubscriptionManager.getDefaultDataSubId());
        }

        if (isDefaultDataEnabled() && !isAirplaneModeOn()) {
            if (DEBUG) Log.d(TAG, "mobile data is on.");
            switch(dataPhoneId) {
                case PhoneConstants.SUB1:
                    state.iconId = R.drawable.ic_qs_data_on_1;
                    break;
                case PhoneConstants.SUB2:
                    state.iconId = R.drawable.ic_qs_data_on_2;
                    break;
                case PhoneConstants.SUB3:
                default:
                    state.iconId = 0;
                    break;
            }
            state.label = mContext.getString(R.string.quick_settings_dds_sub, dataPhoneId + 1);
            state.contentDescription =
                    mContext.getString(R.string.quick_settings_dds_sub, dataPhoneId + 1);
        } else {
            if (DEBUG) Log.d(TAG, "mobile data is off.");
            switch(dataPhoneId) {
                case PhoneConstants.SUB1:
                    state.iconId = R.drawable.ic_qs_data_off_1;
                    break;
                case PhoneConstants.SUB2:
                    state.iconId = R.drawable.ic_qs_data_off_2;
                    break;
                case PhoneConstants.SUB3:
                default:
                    state.iconId = 0;
                    break;
            }
            state.label = mContext.getString(R.string.quick_settings_data_off);
            state.contentDescription = mContext.getString(R.string.quick_settings_data_off);
        }
    }

    private boolean isDefaultDataEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.MOBILE_DATA +
                SubscriptionManager.from(mContext).getDefaultDataPhoneId(), 0) != 0;
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            mDdsObserver.startObserving();
        } else {
            mDdsObserver.endObserving();
        }
    }

    void switchDdsToNext() {
        if (DEBUG) Log.d(TAG, "switchDdsToNext");

        if (!TelephonyManager.getDefault().isMultiSimEnabled()) {
            return;
        }

        TelephonyManager tm = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        int dataPhoneId = SubscriptionManager.getPhoneId(
                SubscriptionManager.getDefaultDataSubId());
        int phoneCount = tm.getPhoneCount();
        int[] subIds = SubscriptionManager.getSubId((dataPhoneId + 1) % phoneCount);
        SubscriptionManager.from(mContext).setDefaultDataSubId(subIds[0]);
    }

    private class DdsObserver extends ContentObserver {
        public DdsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) Log.d(TAG, "mobile data or data sub is changed.");
            refreshState();
        }

        public void startObserving() {
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            for (int i = 0; i < phoneCount; i++) {
                mContext.getContentResolver().registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + i),
                        false, this);
            }
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(
                    Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION),
                    false, this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
                    false, this);
        }

        public void endObserving() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public boolean isAirplaneModeOn() {
        return (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0);
    }
}
