/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.keyguard;

import com.android.internal.telephony.ConfigResourceUtil;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import java.util.regex.Pattern;

/**
 * A Pin based Keyguard input view
 */
public abstract class KeyguardPinBasedInputView extends KeyguardAbsKeyInputView
        implements View.OnKeyListener {

    // value for subsidy lock restricted state
    private static final int SUBSIDYLOCK_LOCKED = 102;
    private static final int SUBSIDYLOCK_RESTRICTED = 103;
    private static final String SUBSIDY_STATUS = "subsidy_status";
    private static final String SUBSIDY_LOCK_SYSTEM_PROPERY
            = "persist.radio.subsidylock";

    protected PasswordTextView mPasswordEntry;
    private View mOkButton;
    private View mDeleteButton;
    private View mButton0;
    private View mButton1;
    private View mButton2;
    private View mButton3;
    private View mButton4;
    private View mButton5;
    private View mButton6;
    private View mButton7;
    private View mButton8;
    private View mButton9;

    public KeyguardPinBasedInputView(Context context) {
        this(context, null);
    }

    public KeyguardPinBasedInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void reset() {
        mPasswordEntry.requestFocus();
        super.reset();
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        // send focus to the password field
        return mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void resetState() {
        setPasswordEntryEnabled(true);
    }

    @Override
    protected void setPasswordEntryEnabled(boolean enabled) {
        mPasswordEntry.setEnabled(enabled);
        mOkButton.setEnabled(enabled);
    }

    @Override
    protected void setPasswordEntryInputEnabled(boolean enabled) {
        mPasswordEntry.setEnabled(enabled);
        mOkButton.setEnabled(enabled);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.isConfirmKey(keyCode)) {
            performClick(mOkButton);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            performClick(mDeleteButton);
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            int number = keyCode - KeyEvent.KEYCODE_0 ;
            performNumberClick(number);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected int getPromtReasonStringRes(int reason) {
        switch (reason) {
            case PROMPT_REASON_RESTART:
                return R.string.kg_prompt_reason_restart_pin;
            case PROMPT_REASON_TIMEOUT:
                return R.string.kg_prompt_reason_timeout_pin;
            default:
                return 0;
        }
    }

    private void performClick(View view) {
        view.performClick();
    }

    private void performNumberClick(int number) {
        switch (number) {
            case 0:
                performClick(mButton0);
                break;
            case 1:
                performClick(mButton1);
                break;
            case 2:
                performClick(mButton2);
                break;
            case 3:
                performClick(mButton3);
                break;
            case 4:
                performClick(mButton4);
                break;
            case 5:
                performClick(mButton5);
                break;
            case 6:
                performClick(mButton6);
                break;
            case 7:
                performClick(mButton7);
                break;
            case 8:
                performClick(mButton8);
                break;
            case 9:
                performClick(mButton9);
                break;
        }
    }

    @Override
    protected void resetPasswordText(boolean animate) {
        mPasswordEntry.reset(animate);
    }

    @Override
    protected String getPasswordText() {
        return mPasswordEntry.getText();
    }

    @Override
    protected void onFinishInflate() {
        mPasswordEntry = (PasswordTextView) findViewById(getPasswordTextViewId());
        mPasswordEntry.setOnKeyListener(this);

        // Set selected property on so the view can send accessibility events.
        mPasswordEntry.setSelected(true);

        mPasswordEntry.setUserActivityListener(new PasswordTextView.UserActivityListener() {
            @Override
            public void onUserActivity() {
                onUserInput();
            }
        });

        mOkButton = findViewById(R.id.key_enter);
        if (mOkButton != null) {
            mOkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doHapticKeyClick();
                    if (mPasswordEntry.isEnabled()) {
                        verifyPasswordAndUnlock();
                    }
                }
            });
            mOkButton.setOnHoverListener(new LiftToActivateListener(getContext()));
        }

        mDeleteButton = findViewById(R.id.delete_button);
        mDeleteButton.setVisibility(View.VISIBLE);
        mDeleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // check for time-based lockouts
                if (mPasswordEntry.isEnabled()) {
                    mPasswordEntry.deleteLastChar();
                }
                doHapticKeyClick();
            }
        });
        mDeleteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // check for time-based lockouts
                if (mPasswordEntry.isEnabled()) {
                    resetPasswordText(true /* animate */);
                }
                doHapticKeyClick();
                return true;
            }
        });

        mButton0 = findViewById(R.id.key0);
        mButton1 = findViewById(R.id.key1);
        mButton2 = findViewById(R.id.key2);
        mButton3 = findViewById(R.id.key3);
        mButton4 = findViewById(R.id.key4);
        mButton5 = findViewById(R.id.key5);
        mButton6 = findViewById(R.id.key6);
        mButton7 = findViewById(R.id.key7);
        mButton8 = findViewById(R.id.key8);
        mButton9 = findViewById(R.id.key9);

        mPasswordEntry.requestFocus();
        super.onFinishInflate();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            onKeyDown(keyCode, event);
            return true;
        }
        return false;
    }

    protected boolean isSubsidyRestricted(int subId) {
        int subsidyStatus  = Settings.Secure.getInt(
                getContext().getContentResolver(),
                SUBSIDY_STATUS, SUBSIDYLOCK_LOCKED);
        boolean subsidyLocked = (subsidyStatus == SUBSIDYLOCK_LOCKED)
                || (subsidyStatus == SUBSIDYLOCK_RESTRICTED);
        SubscriptionInfo sir = SubscriptionManager.from(getContext())
                .getActiveSubscriptionInfo(subId);
        if (sir == null) {
            return false;
        }
        boolean isWhiteListed = isWhiteListed(
                String.valueOf(sir.getMcc()), String.valueOf(sir.getMnc()));
        return isSubSidyLockFeatureEnabled() && subsidyLocked && !isWhiteListed;
    }

    private static boolean isSubSidyLockFeatureEnabled() {
        int prop = SystemProperties.getInt(SUBSIDY_LOCK_SYSTEM_PROPERY, 0);
        return (prop == 1) || (prop == 2);
    }

    private boolean isWhiteListed(String mcc, String mnc) {
        boolean mccAllowed = false;
        boolean mncAllowed = false;
        String[] mccWhiteList = ConfigResourceUtil.getStringArray(
                getContext(), "mccs_white_listed");
        String[] mncsWhiteList = ConfigResourceUtil.getStringArray(
                getContext(), "mncs_white_listed");
        for (String mccRegEx : mccWhiteList) {
            mccAllowed |= Pattern.compile(mccRegEx).matcher(mcc).matches();
            if (mccAllowed) {
                break;
            }
        }
        for (String mncRegEx : mncsWhiteList) {
            mncAllowed |= Pattern.compile(mncRegEx).matcher(mnc).matches();
            if (mncAllowed) {
                break;
            }
        }
        return mccAllowed && mncAllowed;
    }

}
