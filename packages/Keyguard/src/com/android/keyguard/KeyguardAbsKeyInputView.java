/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

/**
 * Base class for PIN and password unlock screens.
 */
public abstract class KeyguardAbsKeyInputView extends LinearLayout
        implements KeyguardSecurityView, OnEditorActionListener, TextWatcher {
    private static final String TAG = "KeyguardAbsKeyInputView";
    protected KeyguardSecurityCallback mCallback;
    protected TextView mPasswordEntry;
    protected LockPatternUtils mLockPatternUtils;
    protected SecurityMessageDisplay mSecurityMessageDisplay;
    protected View mEcaView;
    private Drawable mBouncerFrame;
    protected boolean mEnableHaptics;
    //flag to tell whether long press on 1 has happened to bring the symbols table
    private boolean mDisplaySymbolsTable = false;

    // To avoid accidental lockout due to events while the device in in the pocket, ignore
    // any passwords with length less than or equal to this length.
    protected static final int MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT = 3;

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
        mEnableHaptics = mLockPatternUtils.isTactileFeedbackEnabled();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (hasWindowFocus) {
            reset();
        }
    }

    public void reset() {
        // start fresh
        //Clear the content of password field only when symbols table is not shown
        if (!mDisplaySymbolsTable ) {
        mPasswordEntry.setText("");
        }
        mPasswordEntry.requestFocus();

        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline();
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }

    // Allow subclasses to override this behavior
    protected boolean shouldLockout(long deadline) {
        return deadline != 0;
    }

    protected abstract int getPasswordTextViewId();
    protected abstract void resetState();

    @Override
    protected void onFinishInflate() {
        mLockPatternUtils = new LockPatternUtils(mContext);

        mPasswordEntry = (TextView) findViewById(getPasswordTextViewId());
        mPasswordEntry.setOnEditorActionListener(this);
        mPasswordEntry.addTextChangedListener(this);

        // Set selected property on so the view can send accessibility events.
        mPasswordEntry.setSelected(true);

        // Poke the wakelock any time the text is selected or modified
        mPasswordEntry.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mCallback.userActivity(0); // TODO: customize timeout for text?
            }
        });

        mPasswordEntry.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
                if (mCallback != null) {
                    mCallback.userActivity(0);
                }
            }
        });
        mSecurityMessageDisplay = new KeyguardMessageArea.Helper(this);
        mEcaView = findViewById(R.id.keyguard_selector_fade_container);
        View bouncerFrameView = findViewById(R.id.keyguard_bouncer_frame);
        if (bouncerFrameView != null) {
            mBouncerFrame = bouncerFrameView.getBackground();
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        // send focus to the password field
        return mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    /*
     * Override this if you have a different string for "wrong password"
     *
     * Note that PIN/PUK have their own implementation of verifyPasswordAndUnlock and so don't need this
     */
    protected int getWrongPasswordStringId() {
        return R.string.kg_wrong_password;
    }

    protected void verifyPasswordAndUnlock() {
        String entry = mPasswordEntry.getText().toString();
        if (mLockPatternUtils.checkPassword(entry)) {
            mCallback.reportSuccessfulUnlockAttempt();
            mCallback.dismiss(true);
        } else if (entry.length() > MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT ) {
            // to avoid accidental lockout, only count attempts that are long enough to be a
            // real password. This may require some tweaking.
            mCallback.reportFailedUnlockAttempt();
            if (0 == (mCallback.getFailedAttempts()
                    % LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT)) {
                long deadline = mLockPatternUtils.setLockoutAttemptDeadline();
                handleAttemptLockout(deadline);
            }
            mSecurityMessageDisplay.setMessage(getWrongPasswordStringId(), true);
        }
        mPasswordEntry.setText("");
    }

    // Prevent user from using the PIN/Password entry until scheduled deadline.
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        mPasswordEntry.setEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                mSecurityMessageDisplay.setMessage(
                        R.string.kg_too_many_failed_attempts_countdown, true, secondsRemaining);
            }

            @Override
            public void onFinish() {
                mSecurityMessageDisplay.setMessage("", false);
                resetState();
            }
        }.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mCallback.userActivity(0);
        return false;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // Check if this was the result of hitting the enter key
        if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT) {
            // Modify for keypad support by start
            if (KeyguardService.isPhoneTypeTouch) {
                verifyPasswordAndUnlock();
            }
            // Modify for keypad support by end
            return true;
        }
        return false;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume(int reason) {
        reset();
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mCallback != null) {
            mCallback.userActivity(KeyguardViewManager.DIGIT_PRESS_WAKE_MILLIS);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    // Cause a VIRTUAL_KEY vibration
    public void doHapticKeyClick() {
        if (mEnableHaptics) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                    | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    @Override
    public void showBouncer(int duration) {
        KeyguardSecurityViewHelper.
                showBouncer(mSecurityMessageDisplay, mEcaView, mBouncerFrame, duration);
    }

    @Override
    public void hideBouncer(int duration) {
        KeyguardSecurityViewHelper.
                hideBouncer(mSecurityMessageDisplay, mEcaView, mBouncerFrame, duration);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
        //Set to true only when symbol table is displayed.
        //Symbol table is displayed when we do a long press on 1 key.
        if ( event.getKeyCode() == KeyEvent.KEYCODE_1 ) {
            if (isDown && event.getRepeatCount() > 0 ) {
                mDisplaySymbolsTable = true;
            }
        } else {
            mDisplaySymbolsTable = false;
        }
        //action to be taken when the DPAD center key is pressed in PIN or Password
        //lock screen
        if(findFocus() == mPasswordEntry) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_ENTER:
                    if (isDown) {
                       verifyPasswordAndUnlock();
                    }
                    // Handling Enter key event.
                    return true;
                case KeyEvent.KEYCODE_CLEAR:
                    if (isDown) {
                        KeyEvent delKey = new KeyEvent(event.getAction(), KeyEvent.KEYCODE_DEL);
                        dispatchKeyEvent(delKey);
                    }
                    return true;
                default:
            }
        }
        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean isPasswordEntryFocused = (findFocus() == mPasswordEntry);

        if ((!isPasswordEntryFocused)
                && ((keyCode >= KeyEvent.KEYCODE_0) && (keyCode <= KeyEvent.KEYCODE_9))) {
            // Bring focus to PIN/Password field whenever user press numeric key
            // Add the number to edit field
            mPasswordEntry.requestFocus();
            String keyPressed = new String("" + event.getNumber());
            mPasswordEntry.append(keyPressed);
            mPasswordEntry.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            mCallback.userActivity(0);
            return true;
        }
        return super.dispatchKeyEvent(event);
}

    // This method will show Emergency call confirmation dialog
    private void showDialog(String title, String message, final Intent aIntent) {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                final boolean bypassHandler = true;
                                KeyguardUpdateMonitor.getInstance(mContext)
                                        .reportEmergencyCallAction(
                                                bypassHandler);
                                Intent intent = new Intent(aIntent);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.setData(Uri.fromParts("tel",
                                        mPasswordEntry.getText().toString(),
                                        null));
                                getContext().startActivityAsUser(
                                        intent,
                                        new UserHandle(mLockPatternUtils
                                                .getCurrentUser()));
                            }
                        }).setNegativeButton(android.R.string.cancel, null)
                .create();
        if (!(mContext instanceof Activity)) {
            dialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
        dialog.show();
    }
}
