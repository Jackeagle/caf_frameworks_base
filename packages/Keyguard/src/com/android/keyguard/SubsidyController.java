/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
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

package com.android.keyguard;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.SubsidyUtility.SubsidyLockState;
import com.android.settingslib.TetherUtil;

public class SubsidyController {
    private static SubsidyController sSubsidyController;
    private static String TAG = "SubsidyController";
    private static final boolean DEBUG = SubsidyUtility.DEBUG;
    private SubsidyState mCurrentSubsidyState;
    private SubsidyState mPreviousSubsidyState;
    private Context mContext;
    private boolean mStopStateTransitions = false;

    private SubsidyController(Context context) {
        mContext = context;

        final IntentFilter subsidyLockFilter = new IntentFilter();
        subsidyLockFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        subsidyLockFilter.addAction(SubsidyUtility.ACTION_SUBSIDY_LOCK_CLIENT);

        mContext.registerReceiver(mSubsidyLockReceiver, subsidyLockFilter,
                    SubsidyUtility.BROADCAST_PERMISSION, null);
        setDefaultSubsidyState(context);
    }

    public static SubsidyController getInstance(Context context) {
        if (null == sSubsidyController) {
            sSubsidyController = new SubsidyController(context);
        }
        return sSubsidyController;
    }

    private void setDefaultSubsidyState(Context context) {
        int state = SubsidyUtility.getSubsidyLockStatus(context);
        if (state == SubsidyLockState.AP_LOCKED
                || state == SubsidyLockState.DEVICE_LOCKED
                || state == SubsidyLockState.SUBSIDY_STATUS_UNKNOWN) {
            mCurrentSubsidyState = new UnlockScreenState();
        }
        if (state == SubsidyLockState.AP_UNLOCKED) {
            mCurrentSubsidyState = new ApUnlockedState();
        }
        if (mCurrentSubsidyState != null) {
            mCurrentSubsidyState.init(mContext);
        }
    }

    private final BroadcastReceiver mSubsidyLockReceiver =
        new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (DEBUG) {
                    Log.d(TAG, "Received intent for SubsidyLock feature " +  intent);
                }
                if (!mStopStateTransitions) {
                    boolean isLocked = processIntent(intent);
                    KeyguardUpdateMonitor.getInstance(mContext)
                        .dispatchSubsidyLockStateChanged(isLocked);

                    if (isDeviceUnLocked()) {
                        if (DEBUG) {
                            Log.d(TAG, " UnRegistered From  SLC");
                        }
                        mContext.unregisterReceiver(mSubsidyLockReceiver);
                    }
                }
            }
        };

    public SubsidyState getCurrentSubsidyState() {
        return mCurrentSubsidyState;
    }

    private boolean processIntent(Intent intent) {
        mPreviousSubsidyState = mCurrentSubsidyState;

        if (intent.getBooleanExtra(
                    SubsidyUtility.EXTRA_INTENT_KEY_LOCK_SCREEN, false)) {
            mCurrentSubsidyState = new UnlockScreenState();
        } else if (intent.getBooleanExtra(
                    SubsidyUtility.EXTRA_INTENT_KEY_ACTIVATION_SCREEN, false)) {
            mCurrentSubsidyState = new ActivateScreenState();
        } else if (intent.getBooleanExtra(
                    SubsidyUtility.EXTRA_INTENT_KEY_UNLOCK_SCREEN, false)) {
            mCurrentSubsidyState = new ApUnlockedState();
        } else if (intent.getBooleanExtra(
                    SubsidyUtility.EXTRA_INTENT_KEY_ENTER_CODE_SCREEN, false)) {
            mCurrentSubsidyState = new DeviceLockedState();
        } else if (intent.getBooleanExtra(
                    SubsidyUtility.EXTRA_INTENT_KEY_UNLOCK_PERMANENT, false)) {
            mCurrentSubsidyState = new DeviceUnlockedState();
        } else {
            return false;
        }
        mCurrentSubsidyState.init(mContext);

        return mCurrentSubsidyState.isLocked();
    }

    public void setDeviceUnlocked() {
        mCurrentSubsidyState = new DeviceUnlockedState();
        mCurrentSubsidyState.init(mContext);
        if (DEBUG) {
            Log.d(TAG, " UnRegistered From  SLC");
        }
        mContext.unregisterReceiver(mSubsidyLockReceiver);
        KeyguardUpdateMonitor.getInstance(mContext)
                   .dispatchSubsidyLockStateChanged(false);
    }

    private boolean isDeviceUnLocked() {
        return mCurrentSubsidyState instanceof DeviceUnlockedState;
    }

    public void stopStateTransitions(boolean enable) {
        mStopStateTransitions = enable;
    }

    public int getCurrentSubsidyViewId() {
        int viewId = 0;
        if (mCurrentSubsidyState != null) {
            viewId = mCurrentSubsidyState.getViewId();
        } else if (mPreviousSubsidyState != null) {
            viewId = mPreviousSubsidyState.getViewId();
        }
        if (viewId == 0) {
            mCurrentSubsidyState = new UnlockScreenState();
            viewId = mCurrentSubsidyState.getViewId();
        }
        return viewId;
    }

    public int getCurrentSubsidyLayoutId() {
        int layoutId = 0;
        if (mCurrentSubsidyState != null) {
            layoutId = mCurrentSubsidyState.getLayoutId();
        } else if (mPreviousSubsidyState != null) {
            layoutId = mPreviousSubsidyState.getLayoutId();
        }
        if (layoutId == 0) {
            mCurrentSubsidyState = new UnlockScreenState();
            layoutId = mCurrentSubsidyState.getLayoutId();
        }
        return layoutId;
    }

    public abstract class SubsidyState {
        protected int mState;
        protected int mLayoutId;
        protected int mViewId;
        protected String mExtraLaunchIntent;

        protected void init(final Context context) {
            final ConnectivityManager connectivityManager =
                    (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            new Thread(new Runnable() {
                public void run() {
                    disableAirplaneMode(context, connectivityManager);
                    disableWifiTethering(context);
                    disableUsbTethering(context, connectivityManager);
                    disableBluetooth();
                }
            }).start();
        }

        protected String getLaunchIntent(int resId) {
            return null;
        }

        protected abstract int getLayoutId();

        protected abstract int getViewId();

        protected boolean isLocked() {
            return false;
        }

        private void disableWifiTethering(Context context) {
            TetherUtil.setWifiTethering(false, context);
        }

        private void disableUsbTethering(Context context,
                                         ConnectivityManager cm) {
            cm.setUsbTethering(false);
        }

        private void disableBluetooth() {
            try {
                BluetoothAdapter mybluetooth =
                    BluetoothAdapter.getDefaultAdapter();
                mybluetooth.disable();
            } catch (Exception e) {
                Log.e(TAG, "Exception while disabling bluetooth " + e);
            }
        }

        private void disableAirplaneMode(Context context,
                ConnectivityManager cm) {
            cm.setAirplaneMode(false);
        }
    }

    public abstract class ApLockedState extends SubsidyState {
        @Override
        protected void init(Context context) {
            super.init(context);
            SubsidyUtility.writeSubsidyLockStatus(context,
                    SubsidyLockState.AP_LOCKED);
        }

        @Override
        protected boolean isLocked() {
            return true;
        }
    }

    class UnlockScreenState extends ApLockedState {
        public UnlockScreenState() {
            if (DEBUG) {
                Log.d(TAG, " In UnlockScreenState");
            }
            mLayoutId = R.layout.keyguard_subsidy_lock_view;
            mViewId = R.id.keyguard_subsidy_lock_view;
            mExtraLaunchIntent = SubsidyUtility.EXTRA_INTENT_KEY_UNLOCK;
        }

        @Override
        protected int getLayoutId() {
            return mLayoutId;
        }

        @Override
        protected int getViewId() {
            return mViewId;
        }

        @Override
        protected String getLaunchIntent(int resId) {
            return mExtraLaunchIntent;
        }

    }

    class ActivateScreenState extends ApLockedState {
        public ActivateScreenState() {
            if (DEBUG) {
                Log.d(TAG, " In ActivateScreenState");
            }
            mLayoutId = R.layout.keyguard_subsidy_activate_view;
            mViewId = R.id.keyguard_subsidy_activate_view;
        }

        @Override
        public String getLaunchIntent(int resourceId) {
            if (resourceId == R.string.kg_button_unlock) {
                mExtraLaunchIntent = SubsidyUtility.EXTRA_INTENT_KEY_UNLOCK;
            } else if (resourceId == R.string.kg_button_activate) {
                mExtraLaunchIntent =
                    SubsidyUtility.EXTRA_INTENT_KEY_ACTIVATION_DONE;
            }
            return mExtraLaunchIntent;
        }

        @Override
        protected int getLayoutId() {
            return mLayoutId;
        }

        @Override
        protected int getViewId() {
            return mViewId;
        }
    }

    class ApUnlockedState extends SubsidyState {
        public ApUnlockedState() {
            if (DEBUG) {
                Log.d(TAG, " In AppUnlockedState");
            }
        }

        @Override
        protected void init(Context context) {
            SubsidyUtility.writeSubsidyLockStatus(context,
                    SubsidyLockState.AP_UNLOCKED);
        }

        @Override
        protected boolean isLocked() {
            return false;
        }

        @Override
        protected int getLayoutId() {
            return 0;
        }

        @Override
        protected int getViewId() {
            return 0;
        }
    }

    class DeviceLockedState extends SubsidyState {

        public DeviceLockedState() {
            if (DEBUG) {
                Log.d(TAG, " In DeviceLockedState");
            }
            mLayoutId = R.layout.keyguard_subsidy_pin_view;
            mViewId = R.id.keyguard_subsidy_pin_view;
            mExtraLaunchIntent = SubsidyUtility.EXTRA_INTENT_KEY_PIN_VERIFIED;
        }

        @Override
        protected void init(Context context) {
            super.init(context);
            SubsidyUtility.writeSubsidyLockStatus(context,
                    SubsidyLockState.DEVICE_LOCKED);
        }

        @Override
        protected String getLaunchIntent(int resId) {
            return mExtraLaunchIntent;
        }

        @Override
        protected boolean isLocked() {
            return true;
        }

        @Override
        protected int getLayoutId() {
            return mLayoutId;
        }

        @Override
        protected int getViewId() {
            return mViewId;
        }
    }

    class DeviceUnlockedState extends SubsidyState {

        public DeviceUnlockedState() {
            if (DEBUG) {
                Log.d(TAG, " In DeviceUnlockedState");
            }
        }

        @Override
        protected void init(Context context) {
            SubsidyUtility.writeSubsidyLockStatus(context,
                    SubsidyLockState.DEVICE_UNLOCKED);
            Toast toast =
                Toast.makeText(context, R.string.unlock_toast,
                        Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.getWindowParams().type =
                WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG;
            toast.show();
        }

        @Override
        protected boolean isLocked() {
            return false;
        }

        @Override
        protected int getLayoutId() {
            return 0;
        }

        @Override
        protected int getViewId() {
            return 0;
        }
    }
}
