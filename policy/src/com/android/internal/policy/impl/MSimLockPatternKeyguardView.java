/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) 2012, The Linux Foundation. All rights reserved
 *
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
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

package com.android.internal.policy.impl;

import com.android.internal.R;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.IccCard;
import com.android.internal.widget.LockPatternUtils;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.View;

/**
 * The host view for all of the screens of the pattern unlock screen.  There are
 * two {@link Mode}s of operation, lock and unlock.  This will show the appropriate
 * screen, and listen for callbacks via
 * {@link com.android.internal.policy.impl.KeyguardScreenCallback}
 * from the current screen.
 *
 * This view, in turn, communicates back to
 * {@link com.android.internal.policy.impl.KeyguardViewManager}
 * via its {@link com.android.internal.policy.impl.KeyguardViewCallback}, as appropriate.
 */
public class MSimLockPatternKeyguardView extends LockPatternKeyguardView {
    private static final boolean DEBUG = false;
    private static final String TAG = "MSimLockPatternKeyguardView";
    private int mNumPhones =  MSimTelephonyManager.getDefault().getPhoneCount();

    private boolean[] mIsPinUnlockCancelled = {false, false, false};
    private boolean[] mIsPukUnlockCancelled = {false, false, false};

    /**
     * @return Whether we are stuck on the lock screen because the sim is
     *   missing.
     */
    @Override
    protected boolean stuckOnLockScreenBecauseSimMissing() {
        boolean result = mRequiresSim && (!mUpdateMonitor.isDeviceProvisioned());
        for (int i = 0; i < mNumPhones; i++) {
            //In case of dual subscription, stuck on lock screen
            //only when SIM is absent on both subscriptions
            result = result && (getSimState(i) == IccCard.State.ABSENT ||
            getSimState(i) == IccCard.State.PERM_DISABLED);
            if (!result) break;
        }
        return result;
    }

    /**
     * @param context Used to inflate, and create views.
     * @param callback Keyguard callback object for pokewakelock(), etc.
     * @param updateMonitor Knows the state of the world, and passed along to each
     *   screen so they can use the knowledge, and also register for callbacks
     *   on dynamic information.
     * @param lockPatternUtils Used to look up state of lock pattern.
     */
    public MSimLockPatternKeyguardView(
            Context context, KeyguardViewCallback callback,
            KeyguardUpdateMonitor updateMonitor,
            LockPatternUtils lockPatternUtils,
            KeyguardWindowController controller) {
        super(context, callback, updateMonitor, lockPatternUtils, controller);

        updateScreen(getInitialMode(), false);
    }

    @Override
    protected void keyguardScreenCallback() {
        mKeyguardScreenCallback = new KeyguardScreenCallback() {
            public void goToLockScreen() {
                mForgotPattern = false;
                if (mIsVerifyUnlockOnly) {
                    // navigating away from unlock screen during verify mode means
                    // we are done and the user failed to authenticate.
                    mIsVerifyUnlockOnly = false;
                    getCallback().keyguardDone(false);
                } else {
                    updateScreen(Mode.LockScreen, false);
                }
            }

            public void goToUnlockScreen() {
                boolean isPukRequired = true;
                for (int i = 0; i < mNumPhones; i++) {
                    isPukRequired = isPukRequired && isSimPukLocked(i);
                    if (!isPukRequired) break;
                }

                if (stuckOnLockScreenBecauseSimMissing()
                         || (isPukRequired
                             && !mLockPatternUtils.isPukUnlockScreenEnable())){
                    // stuck on lock screen when sim missing or
                    // puk'd but puk unlock screen is disabled
                    return;
                }
                if (!isSecure()) {
                    getCallback().keyguardDone(true);
                } else {
                    updateScreen(Mode.UnlockScreen, false);
                }
            }

            /**
             * SimUnlockScreen invokes this method when user dismisses the
             * PIN dialog for any of the subscription.PIN Dialog will not be
             * prompted again for that subscription if the other subscription
             * is in ready state.
             * In case, the other subscriptions is PUK-Locked, user is not
             * allowed to dismiss the PIN dialog.
            */
            public void updatePinUnlockCancel(int subscription) {
                int otherSub;
                for (int i = 0; i < mNumPhones-1; i++) {
                    otherSub = getNextSubscription(subscription+i);
                    if (!isPukCancelDialog(otherSub)) {
                        mIsPinUnlockCancelled[subscription] = true;
                    } else {
                         Log.i(TAG, "Cannot cancel PIN dialog");
                        mIsPinUnlockCancelled[subscription] = false;
                    }
                }

                if (isAllPinUnlockCancel() &&
                        !MSimLockPatternKeyguardView.this.isAnySubUnlocked()) {
                    for (int i = 0; i < mNumPhones; i++) {
                        mIsPinUnlockCancelled[i] = false;
                    }
                }
            }

           /**
            *Gives status of dimisses PIN Dialog for PIN Locke Subscriptions
            * return true if all PIN dialgos dismissed by user for
            * SIM PIN Locked Subscriptions.
            * otherwise retun false
           */
            private boolean isAllPinUnlockCancel() {
                boolean pinUnlockCancel = true;
                for (int i = 0; i < mNumPhones; i++) {
                    if (isSimPinLocked (i)) {
                        pinUnlockCancel = pinUnlockCancel  && mIsPinUnlockCancelled[i];
                    }
                }
                return pinUnlockCancel;
            }

           /**
            *Gives status of dimisses PUK Dialog for PUK Locke Subscriptions
            * return true if all PUK dialgos dismissed by user for
            * SIM PUK Locked Subscriptions.
            * otherwise retun false
           */
            private boolean isPukCancelDialog(int sub) {
                return ((isSimPukLocked(sub) &&
                        mIsPukUnlockCancelled[sub]) ||
                        (getSimState(sub) == IccCard.State.ABSENT));
            }

            /**
             * SimPukUnlockScreen invokes this method when user dismisses the
             * PUK dialog for any of the subscription.PUK Dialog will not be
             * prompted again for that subscription if the other subscription
             * also is not in PUK-Locked state.
             * In case the the sim state of other subscriptions is ABSENT
             * user is not allowed to dismiss the PUK dialog.
             */
            public void updatePukUnlockCancel(int subscription) {
                int otherSub;
                for (int i = 0; i < mNumPhones-1; i++) {
                    otherSub = getNextSubscription(subscription+i);
                    if (!isCardAbsent(otherSub)) {
                        mIsPukUnlockCancelled[subscription] = true;
                    } else {
                        mIsPukUnlockCancelled[subscription] = false;
                    }
                }

                if (isAllPukUnlockCancel() &&
                        !MSimLockPatternKeyguardView.this.isAnySubUnlocked()) {
                    for (int i = 0; i < mNumPhones; i++) {
                    mIsPukUnlockCancelled[i] = false;
                    }
                }
            }

            private boolean isAllPukUnlockCancel() {
                boolean pukUnlockCancel = true;
                for (int i = 0; i < mNumPhones; i++) {
                    if (isSimPukLocked (i)) {
                        pukUnlockCancel = pukUnlockCancel && mIsPukUnlockCancelled[i];
                    }
                }
                return pukUnlockCancel;
            }

            private boolean isCardAbsent(int sub) {
                return (getSimState(sub) == IccCard.State.ABSENT);
            }

            public void forgotPattern(boolean isForgotten) {
                if (mEnableFallback) {
                    mForgotPattern = isForgotten;
                    updateScreen(Mode.UnlockScreen, false);
                }
            }
            public boolean isSecure() {
                return MSimLockPatternKeyguardView.this.isSecure();
            }

            public boolean isVerifyUnlockOnly() {
                return mIsVerifyUnlockOnly;
            }

            public void recreateMe(Configuration config) {
                if (DEBUG) Log.v(TAG, "recreateMe()");
                removeCallbacks(mRecreateRunnable);
                post(mRecreateRunnable);
            }

            public void takeEmergencyCallAction() {
                mSuppressBiometricUnlock = true;

                if (mBiometricUnlock != null) {
                    if (mBiometricUnlock.isRunning()) {
                        // Continue covering backup lock until dialer comes up or call is resumed
                        mBiometricUnlock.show(BIOMETRIC_AREA_EMERGENCY_DIALER_TIMEOUT);
                    }

                    // We must ensure the biometric unlock is stopped when emergency call is pressed
                    mBiometricUnlock.stop();
                }

                pokeWakelock(EMERGENCY_CALL_TIMEOUT);
                if (TelephonyManager.getDefault().getCallState()
                        == TelephonyManager.CALL_STATE_OFFHOOK) {
                    mLockPatternUtils.resumeCall();
                } else {
                    Intent intent = new Intent(ACTION_EMERGENCY_DIAL);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    getContext().startActivity(intent);
                }
            }

            public void pokeWakelock() {
                getCallback().pokeWakelock();
            }

            public void pokeWakelock(int millis) {
                getCallback().pokeWakelock(millis);
            }

            public void keyguardDone(boolean authenticated) {
                getCallback().keyguardDone(authenticated);
                mSavedState = null; // clear state so we re-establish when locked again
            }

            public void keyguardDoneDrawing() {
                // irrelevant to keyguard screen, they shouldn't be calling this
            }

            public void reportFailedUnlockAttempt() {
                mUpdateMonitor.reportFailedAttempt();
                final int failedAttempts = mUpdateMonitor.getFailedAttempts();
                if (DEBUG) Log.d(TAG, "reportFailedPatternAttempt: #" + failedAttempts +
                    " (enableFallback=" + mEnableFallback + ")");

                final boolean usingPattern = mLockPatternUtils.getKeyguardStoredPasswordQuality()
                        == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

                final int failedAttemptsBeforeWipe = mLockPatternUtils.getDevicePolicyManager()
                        .getMaximumFailedPasswordsForWipe(null);

                final int failedAttemptWarning = LockPatternUtils.FAILED_ATTEMPTS_BEFORE_RESET
                        - LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT;

                final int remainingBeforeWipe = failedAttemptsBeforeWipe > 0 ?
                        (failedAttemptsBeforeWipe - failedAttempts)
                        : Integer.MAX_VALUE; // because DPM returns 0 if no restriction

                if (remainingBeforeWipe < LockPatternUtils.FAILED_ATTEMPTS_BEFORE_WIPE_GRACE) {
                    // If we reach this code, it means the user has installed a DevicePolicyManager
                    // that requests device wipe after N attempts.  Once we get below the grace
                    // period, we'll post this dialog every time as a clear warning until the
                    // bombshell hits and the device is wiped.
                    if (remainingBeforeWipe > 0) {
                        showAlmostAtWipeDialog(failedAttempts, remainingBeforeWipe);
                    } else {
                        // Too many attempts. The device will be wiped shortly.
                        Slog.i(TAG, "Too many unlock attempts; device will be wiped!");
                        showWipeDialog(failedAttempts);
                    }
                } else {
                    boolean showTimeout =
                        (failedAttempts % LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT) == 0;
                    if (usingPattern && mEnableFallback) {
                        if (failedAttempts == failedAttemptWarning) {
                            showAlmostAtAccountLoginDialog();
                            showTimeout = false; // don't show both dialogs
                        } else if (failedAttempts >=
                                LockPatternUtils.FAILED_ATTEMPTS_BEFORE_RESET) {
                            mLockPatternUtils.setPermanentlyLocked(true);
                            updateScreen(mMode, false);
                            // don't show timeout dialog because we show account unlock screen next
                            showTimeout = false;
                        }
                    }
                    if (showTimeout) {
                        showTimeoutDialog();
                    }
                }
                mLockPatternUtils.reportFailedPasswordAttempt();
            }

            public boolean doesFallbackUnlockScreenExist() {
                return mEnableFallback;
            }

            public void reportSuccessfulUnlockAttempt() {
                mLockPatternUtils.reportSuccessfulPasswordAttempt();
            }
        };
    }

   /**
   *returns true if any subscription is not PIN and PUK locked
   */
    public boolean isAnySubUnlocked() {
        boolean isSubUnlocked = false;
        for (int i = 0; i < mNumPhones; i++) {
            isSubUnlocked = isSubUnlocked ||
                    (!isSimPinLocked(i) && !isSimPukLocked(i));
        }
        return isSubUnlocked;
    }


    /**
     * Get the subscription that is PIN-Locked.
     * Return '0' if SUB1 is PIN-Locked and,
     * PIN dialog for SUB1 was not dismissed by user.
     * Return '1' if SUB2 is PIN-Locked and,
     * PIN dialog for SUB2 was not dismissed by user.
     * Return '2' if SUB3 is PIN-Locked and,
     * PIN dialog for SUB3 was not dismissed by user.
     */
    private int getPinLockedSubscription() {
        int subscription = MSimConstants.SUB2;
        for (int i = 0; i < mNumPhones; i++) {
            if (isSimPinLocked(i) && !mIsPinUnlockCancelled[i]) {
                subscription = i;
                break;
            }
        }
        for (int i = 0; i < mNumPhones; i++) {
            if (isPukLockedOtherSub(i) && isSimPinLocked(i)) {
                return i;
            }
        }
        return subscription;
    }

    /**
    * retun true if all other subscriptions are PUK locked
    */
    private boolean isPukLockedOtherSub(int subscription) {
        boolean isSimPukLocked = true;
        for (int i = 0; i < mNumPhones-1; i++) {
             isSimPukLocked = isSimPukLocked && isSimPukLocked(getNextSubscription(subscription+i));
        }
        return isSimPukLocked;
    }

   /**
   *return Subscription next to current subscription
   * if maximum subscription index reached reset to 0
   */
    private int getNextSubscription(int subscription) {
        subscription = subscription % mNumPhones;
        subscription = subscription + 1;
        if (subscription == mNumPhones) subscription = 0;
        return subscription;
    }

    /**
     * Get the subscription that is PUK-Locked.
     * Return '0' if SUB1 is PUK-Locked and,
     * PUK dialog for SUB1 was not dismissed by user.
     * Return '1' if SUB2 is PUK-Locked and,
     * PUK dialog for SUB2 was not dismissed by user.
     * Return '2' if SUB3 is PUK-Locked and,
     * PUK dialog for SUB3 was not dismissed by user.
     */
    private int getPukLockedSubscription() {
        int subscription = MSimConstants.SUB1;

        for (int i = 0; i < mNumPhones; i++) {
            if (isSimPukLocked(i) && !mIsPukUnlockCancelled[i]) {
                subscription = i;
                break;
            }
        }
        return subscription;
    }

    private IccCard.State getSimState(int subscription) {
        return mUpdateMonitor.getSimState(subscription);
    }

    private boolean isSimPinLocked(int subscription) {
        return (getSimState(subscription) == IccCard.State.PIN_REQUIRED);
    }

    private boolean isSimPukLocked(int subscription) {
        return (getSimState(subscription) == IccCard.State.PUK_REQUIRED);
    }

    @Override
    public void wakeWhenReadyTq(int keyCode) {
        if (DEBUG) Log.d(TAG, "onWakeKey");
        boolean isPukRequired = true;
        for (int i = 0; i < MSimTelephonyManager.getDefault().getPhoneCount(); i++) {
            isPukRequired = isPukRequired && isSimPukLocked(i);
            if (!isPukRequired) break;
        }

        if (keyCode == KeyEvent.KEYCODE_MENU && isSecure() && (mMode == Mode.LockScreen)
                && !isPukRequired) {
            if (DEBUG) Log.d(TAG, "switching screens to unlock screen because wake key was MENU");
            updateScreen(Mode.UnlockScreen, false);
            getCallback().pokeWakelock();
        } else {
            if (DEBUG) Log.d(TAG, "poking wake lock immediately");
            getCallback().pokeWakelock();
        }
    }

    @Override
    protected boolean isSecure() {
        UnlockMode unlockMode = getUnlockMode();
        boolean secure = false;
        switch (unlockMode) {
            case Pattern:
                secure = mLockPatternUtils.isLockPatternEnabled();
                break;
            case SimPin:
            case SimPuk:
                for (int i = 0; i < MSimTelephonyManager.getDefault().getPhoneCount(); i++) {
                    // Check if subscription is PIN/PUK locked.
                    // isPinLocked returns true if the state is PIN_REQUIRED/PUK_REQUIRED
                    secure = secure || getSimState(i).isPinLocked();
                    if (secure) break;
                }
                break;
            case Account:
                secure = true;
                break;
            case Password:
                secure = mLockPatternUtils.isLockPasswordEnabled();
                break;
            case Unknown:
                // This means no security is set up
                break;
            default:
                throw new IllegalStateException("unknown unlock mode " + unlockMode);
        }
        return secure;
    }

    @Override
    View createUnlockScreenFor(UnlockMode unlockMode) {
        View unlockView = null;

        Log.d(TAG,
                "createUnlockScreenFor(" + unlockMode + "): mEnableFallback=" + mEnableFallback);

        if (unlockMode == UnlockMode.Pattern) {
            PatternUnlockScreen view = new PatternUnlockScreen(
                    mContext,
                    mConfiguration,
                    mLockPatternUtils,
                    mUpdateMonitor,
                    mKeyguardScreenCallback,
                    mUpdateMonitor.getFailedAttempts());
            view.setEnableFallback(mEnableFallback);
            unlockView = view;
        } else if (unlockMode == UnlockMode.SimPuk) {
            int subscription = getPukLockedSubscription();
            Log.d(TAG, "Display SimPukUnlockScreen for sub :" + subscription);
            unlockView = new SimPukUnlockScreen(
                    mContext,
                    mConfiguration,
                    mUpdateMonitor,
                    mKeyguardScreenCallback,
                    mLockPatternUtils, subscription);
        } else if (unlockMode == UnlockMode.SimPin) {
            int subscription = getPinLockedSubscription();
            Log.d(TAG, "Display SimUnlockScreen for sub :" + subscription);
            unlockView = new MSimSimUnlockScreen(
                    mContext,
                    mConfiguration,
                    mUpdateMonitor,
                    mKeyguardScreenCallback,
                    mLockPatternUtils, subscription);
        } else if (unlockMode == UnlockMode.Account) {
            try {
                unlockView = new AccountUnlockScreen(
                        mContext,
                        mConfiguration,
                        mUpdateMonitor,
                        mKeyguardScreenCallback,
                        mLockPatternUtils);
            } catch (IllegalStateException e) {
                Log.i(TAG, "Couldn't instantiate AccountUnlockScreen"
                      + " (IAccountsService isn't available)");
                // TODO: Need a more general way to provide a
                // platform-specific fallback UI here.
                // For now, if we can't display the account login
                // unlock UI, just bring back the regular "Pattern" unlock mode.

                // (We do this by simply returning a regular UnlockScreen
                // here.  This means that the user will still see the
                // regular pattern unlock UI, regardless of the value of
                // mUnlockScreenMode or whether or not we're in the
                // "permanently locked" state.)
                return createUnlockScreenFor(UnlockMode.Pattern);
            }
        } else if (unlockMode == UnlockMode.Password) {
            unlockView = new PasswordUnlockScreen(
                    mContext,
                    mConfiguration,
                    mLockPatternUtils,
                    mUpdateMonitor,
                    mKeyguardScreenCallback);
        } else {
            throw new IllegalArgumentException("unknown unlock mode " + unlockMode);
        }
        initializeTransportControlView(unlockView);
        initializeBiometricUnlockView(unlockView);

        mUnlockScreenMode = unlockMode;
        return unlockView;
    }

    /**
     * Given the current state of things, what should be the initial mode of
     * the lock screen (lock or unlock).
     */
    private Mode getInitialMode() {
        boolean isPukRequired = false;
        for (int i = 0; i < MSimTelephonyManager.getDefault().getPhoneCount(); i++) {
            isPukRequired = isPukRequired || isSimPukLocked(i);
            if (isPukRequired) break;
        }
        if (stuckOnLockScreenBecauseSimMissing() || (isPukRequired
                        || !mLockPatternUtils.isPukUnlockScreenEnable())) {
            return Mode.LockScreen;
        } else {
            if (!isSecure() || mShowLockBeforeUnlock) {
                return Mode.LockScreen;
            } else {
                return Mode.UnlockScreen;
            }
        }
    }

    /**
     * Given the current state of things, what should the unlock screen be?
     */
     @Override
     protected UnlockMode getUnlockMode() {
        boolean isPinLocked = false, isPukLocked = false;
        boolean isPinRequired = false, isPukRequired = false;
        // In case of multi SIM mode,
        // Set the unlock mode to "SimPin" if any of the sub is PIN-Locked.
        // Set the unlock mode to "SimPuk" if any of the sub is PUK-Locked.
        for (int i = 0; i < mNumPhones; i++) {
            isPinLocked = isPinLocked || (isSimPinLocked(i) && (!mIsPinUnlockCancelled[i]));
            isPukLocked = isPukLocked || (isSimPukLocked(i) && (!mIsPukUnlockCancelled[i]));
            isPinRequired = isPinRequired || (isSimPinLocked(i));
            isPukRequired = isPukRequired || (isSimPukLocked(i));
        }
        UnlockMode currentMode;
        if (isPinLocked) {
             currentMode = UnlockMode.SimPin;
        } else if (isPukLocked) {
            currentMode = UnlockMode.SimPuk;
        } else if (isPinRequired && isPukRequired && !isAnySubUnlocked()) {
            currentMode = UnlockMode.SimPin;
        } else {
            final int mode = mLockPatternUtils.getKeyguardStoredPasswordQuality();
            switch (mode) {
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    currentMode = UnlockMode.Password;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                    if (mLockPatternUtils.isLockPatternEnabled()) {
                        // "forgot pattern" button is only available in the pattern mode...
                        if (mForgotPattern || mLockPatternUtils.isPermanentlyLocked()) {
                            currentMode = UnlockMode.Account;
                        } else {
                            currentMode = UnlockMode.Pattern;
                        }
                    } else {
                        currentMode = UnlockMode.Unknown;
                    }
                    break;
                default:
                   throw new IllegalStateException("Unknown unlock mode:" + mode);
            }
        }
        return currentMode;
    }

}
