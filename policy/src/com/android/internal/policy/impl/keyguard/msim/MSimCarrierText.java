/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
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

package com.android.internal.policy.impl.keyguard;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import android.util.Log;

import com.android.internal.R;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.widget.LockPatternUtils;

import android.telephony.MSimTelephonyManager;

public class MSimCarrierText extends CarrierText {
    private static final String TAG = "MSimCarrierText";
    private boolean []mShowPlmn;
    private CharSequence []mPlmn;
    private boolean []mShowSpn;
    private CharSequence []mSpn;
    private State []mSimState;

    private KeyguardUpdateMonitorCallback mMSimCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onRefreshCarrierInfo(boolean bShowPlmn, CharSequence plmn, boolean bShowSpn,
                CharSequence spn, int sub) {
            mShowPlmn[sub] = bShowPlmn;
            mPlmn[sub] = plmn;
            mShowSpn[sub] = bShowSpn;
            mSpn[sub] = spn;
            updateCarrierText(mSimState, mShowPlmn, mPlmn, mShowSpn, mSpn);
        }

        @Override
        public void onSimStateChanged(IccCardConstants.State simState, int sub) {
            mSimState[sub] = simState;
            updateCarrierText(mSimState, mShowPlmn, mPlmn, mShowSpn, mSpn);
        }

        @Override
        void onAirplaneModeChanged(boolean on) {
            mAirplaneMode = on;
            updateCarrierText(mSimState, mShowPlmn, mPlmn, mShowSpn, mSpn);
        }
    };

    private void initialize() {
        int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        mShowPlmn = new boolean[numPhones];
        mPlmn = new CharSequence[numPhones];
        mShowSpn = new boolean[numPhones];
        mSpn = new CharSequence[numPhones];
        mSimState = new State[numPhones];
    }

    public MSimCarrierText(Context context) {
        this(context, null);
    }

    public MSimCarrierText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    protected void updateCarrierText(State[] simState, boolean[] bShowPlmn, CharSequence[] plmn,
            boolean[] bshowSpn, CharSequence[] spn) {
        CharSequence text = "";

        if (mAirplaneMode) {
            text = getContext().getText(R.string.lockscreen_airplane_mode_on);
            if (KeyguardViewManager.USE_UPPER_CASE) {
                text = text.toString().toUpperCase();
            }
        } else {
            for (int i = 0; i < simState.length; i++) {
                 CharSequence displayText = getCarrierTextForSimState(simState[i], bShowPlmn[i],
                    plmn[i], bshowSpn[i], spn[i]);
                if (KeyguardViewManager.USE_UPPER_CASE) {
                    displayText = (displayText != null ? displayText.toString().toUpperCase() : "");
                }
                text = (TextUtils.isEmpty(text)
                        ? displayText : getContext().getString(R.string.msim_carrier_text_format,
                                text, displayText));
            }
        }
        Log.d(TAG, "updateCarrierText: text = " + text);
        setText(text);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mMSimCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mMSimCallback);
    }
}

