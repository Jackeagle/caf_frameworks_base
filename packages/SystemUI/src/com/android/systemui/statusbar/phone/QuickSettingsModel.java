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

package com.android.systemui.statusbar.phone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.BluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.hardware.display.WifiDisplayStatus;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.view.RotationPolicy;
import com.android.systemui.R;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.settings.BrightnessController.BrightnessStateChangeCallback;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.LocationController.LocationGpsStateChangeCallback;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;


class QuickSettingsModel implements BluetoothStateChangeCallback,
        NetworkSignalChangedCallback,
        BatteryStateChangeCallback,
        LocationGpsStateChangeCallback,
        BrightnessStateChangeCallback {

    // Sett InputMethoManagerService
    private static final String TAG_TRY_SUPPRESSING_IME_SWITCHER = "TrySuppressingImeSwitcher";

    /** Represents the state of a given attribute. */
    static class State {
        int iconId;
        String label;
        boolean enabled = false;
    }
    static class BatteryState extends State {
        int batteryLevel;
        boolean pluggedIn;
    }
    static class RSSIState extends State {
        int signalIconId;
        String signalContentDescription;
        int dataTypeIconId;
        String dataContentDescription;
    }
    static class WifiState extends State {
        String signalContentDescription;
        boolean connected;
    }
    static class UserState extends State {
        Drawable avatar;
    }
    static class BrightnessState extends State {
        boolean autoBrightness;
    }
    public static class BluetoothState extends State {
        boolean connected = false;
        String stateContentDescription;
    }

    public class ApnState extends State {
        static final String TAG = "ApnState";
        static final boolean DEBUG = true;

        private final String DEFAULT = "default";
        private final String WAP = "wap";

        private final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
        private final String APN_ID = "apn_id";

        private final String[] PROJECTION = new String[] {
                "_id", "name", "apn", "type"
        };
        private final int INDEX_ID = 0;
        private final int INDEX_NAME = 1;
        private final int INDEX_APN = 2;
        private final int INDEX_TYPE = 3;

        private HashMap<String, Integer> mApnIconMap = new HashMap<String, Integer>();
        private Apn mCurrentApn;
        private Apn mNextApn;

        public ApnState() {
            mApnIconMap.put("ctwap", R.drawable.ic_qs_apn_ctwap);
            mApnIconMap.put("ctnet", R.drawable.ic_qs_apn_ctnet);
            mApnIconMap.put("cmnet", R.drawable.ic_qs_apn_cmnet);
            mApnIconMap.put("cmwap", R.drawable.ic_qs_apn_cmwap);
        }

        /**
         * Switch the current apn to next apn.
         *
         * @param apn The next apn you want to switch.
         *            If the value is null, will use the saved next apn to switch.
         */
        public void switchToNextApn(Apn apn) {
            // if the given next apn is null, we will use the saved next apn to switch.
            if (apn == null) {
                apn = mNextApn;
            }

            // We will only set the prefer apn to the next apn, and then the data base will be
            // changed, so we will update the view when we receive the content changed message.
            if (apn != null) {
                if (DEBUG) Log.i(TAG, "switch to the next apn, and it's id: " + apn.id);
                ContentValues values = new ContentValues();
                values.put(APN_ID, apn.id);
                mContext.getContentResolver().update(PREFERAPN_URI, values, null, null);
            }
        }

        /**
         * To get the current apn name
         * @return the current apn name saved as apn's apn
         */
        public String getCurrentApnName() {
            String apn = "";
            if (mCurrentApn != null && mCurrentApn.apn != null) {
                apn = mCurrentApn.apn;
            }
            return apn;
        }

        void updateIconId() {
            String apn = getCurrentApnName();
            Integer icon = mApnIconMap.get(apn);
            if (icon != null) {
                iconId = icon;
            }

            //if there is no records in apn settings, show ctwap icon as default.
            if (getSelectedApnKey() == null) {
                iconId = R.drawable.ic_qs_apn_ctwap;
            }
        }

        /**
         * To update the apn list, and it will update the current apn and next apn.
         */
        public void updateApnList() {
            if (DEBUG) Log.i(TAG, "Try to update the apn list.");
            Apn currentApn = null;
            Apn nextApn = null;

            ArrayList<Apn> apnList = new ArrayList<Apn>();

            Cursor cursor = null;
            try {
                String where = getOperatorNumericSelection();
                if (TextUtils.isEmpty(where)) {
                    Log.d(TAG, "getOperatorNumericSelection is empty ");
                    return;
                }
                String selectedKey = getSelectedApnKey();

                cursor = mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI,
                        PROJECTION, where, null,
                        Telephony.Carriers.DEFAULT_SORT_ORDER);
                if (cursor == null) {
                    Log.e(TAG, "When update the apn list, the cursor is null.");
                    return;
                }

                boolean getNextApn = false;
                while (cursor.moveToNext()) {
                    Apn apn = new Apn();
                    apn.id = cursor.getString(INDEX_ID);
                    apn.name = cursor.getString(INDEX_NAME);
                    apn.apn = cursor.getString(INDEX_APN);
                    apn.type = cursor.getString(INDEX_TYPE);

                    boolean selectable = ((apn.type == null) || !apn.type.equals("mms"));
                    if (!selectable) {
                        // if the type is mms, we needn't to handle it.
                        continue;
                    }

                    apnList.add(apn);
                    if (getNextApn) {
                        nextApn = apn;
                        getNextApn = false;
                        break;
                    }

                    // As android default, the DUT will not set the default selected apn,
                    //so it maybe null.  And we will deal with this case after.
                    if (selectedKey != null && selectedKey.equals(apn.id)) {
                        currentApn = apn;
                        if (cursor.isLast()) {
                            // If the current apn is the last cursor,
                            //we need set the next apn as the first in the apn list.
                            nextApn = apnList.get(0);
                            getNextApn = false;
                            break;
                        } else {
                            // We need try to get the next apn if next cursor is not mms.
                            getNextApn = true;
                        }
                    }
                }
                if (getNextApn) {
                    // We have read all the cursor, but we also need get the next apn,
                    //it means the next apn is the first in the apn list.
                    nextApn = apnList.get(0);
                }

                // Sometimes we didn't find the default apn for example, after we reset the DUT,
                // the selectedkey will be null.
                // Note: As ct spec, we need set the ctwap as the default apn,
                //          so if the selectedkey is null,
                //          We'd like to set it as the default apn.
                if (currentApn == null) {
                    if (selectedKey == null) {
                        for (int i = 0; i < apnList.size(); i++) {
                            Apn apn = apnList.get(i);
                            if (apn.type != null && apn.type.contains(DEFAULT)
                                    && apn.apn != null && apn.apn.contains(WAP)) {
                                switchToNextApn(apn);
                            }
                        }
                    } else {
                        // If the selectedkey is not null, but we didn't find the current apn.
                        // it maybe the data has been set to slot2.
                        // TODO: if we need add some support for the use set the data to slot2?
                        // Now, we will leave it as last value.
                        Log.w(TAG, "The selected key is: " + selectedKey
                            + ", but we didn't matched the current apn.");
                    }
                } else {
                    mCurrentApn = currentApn;
                    mNextApn = nextApn;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }
        }

        private String getOperatorNumericSelection() {
            String[] mccmncs = getOperatorNumeric();
            String where;
            where = (mccmncs[0] != null) ? "numeric=\"" + mccmncs[0] + "\"" : "";
            where = where + ((mccmncs[1] != null) ? " or numeric=\"" + mccmncs[1] + "\"" : "");
            if (DEBUG) Log.d(TAG, "getOperatorNumericSelection: " + where);
            return where;
        }

        private String[] getOperatorNumeric() {
            ArrayList<String> result = new ArrayList<String>();
            if (SystemProperties.getBoolean("persist.radio.use_nv_for_ehrpd", false)) {
                String mccMncForEhrpd = SystemProperties.get("ro.cdma.home.operator.numeric", null);
                if (mccMncForEhrpd != null && mccMncForEhrpd.length() > 0) {
                    result.add(mccMncForEhrpd);
                }
            }

            String property = TelephonyProperties.PROPERTY_APN_SIM_OPERATOR_NUMERIC;
            String mccMncFromSim = null;
            if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
                MSimTelephonyManager msimTM =
                    (MSimTelephonyManager) mContext.getSystemService(Context.MSIM_TELEPHONY_SERVICE);
                int prefDataSub = msimTM.getPreferredDataSubscription();
                mccMncFromSim =
                    MSimTelephonyManager.getTelephonyProperty(property, prefDataSub, null);
            } else {
                int defaultSub = TelephonyManager.getDefaultSubscription();
                mccMncFromSim = TelephonyManager.getTelephonyProperty(property, defaultSub, null);
            }
            if (mccMncFromSim != null && mccMncFromSim.length() > 0) {
                result.add(mccMncFromSim);
            }
            return result.toArray(new String[2]);
        }

        private String getSelectedApnKey() {
            String key = null;

            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(PREFERAPN_URI, new String[] {"_id"},
                        null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    key = cursor.getString(0);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }
            return key;
        }

        public class Apn {
            String id;
            String name;
            String apn;
            String type;
        }
    }

    /** The callback to update a given tile. */
    interface RefreshCallback {
        public void refreshView(QuickSettingsTileView view, State state);
    }

    public static class BasicRefreshCallback implements RefreshCallback {
        private final QuickSettingsBasicTile mView;
        private boolean mShowWhenEnabled;

        public BasicRefreshCallback(QuickSettingsBasicTile v) {
            mView = v;
        }
        public void refreshView(QuickSettingsTileView ignored, State state) {
            if (mShowWhenEnabled) {
                mView.setVisibility(state.enabled ? View.VISIBLE : View.GONE);
            }
            if (state.iconId != 0) {
                mView.setImageDrawable(null); // needed to flush any cached IDs
                mView.setImageResource(state.iconId);
            }
            if (state.label != null) {
                mView.setText(state.label);
            }
        }
        public BasicRefreshCallback setShowWhenEnabled(boolean swe) {
            mShowWhenEnabled = swe;
            return this;
        }
    }

    /** Broadcast receive to determine if there is an alarm set. */
    private BroadcastReceiver mAlarmIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_ALARM_CHANGED)) {
                onAlarmChanged(intent);
                onNextAlarmChanged();
            }
        }
    };

    /** ContentObserver to determine the next alarm */
    private class NextAlarmObserver extends ContentObserver {
        public NextAlarmObserver(Handler handler) {
            super(handler);
        }

        @Override public void onChange(boolean selfChange) {
            onNextAlarmChanged();
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED), false, this,
                    UserHandle.USER_ALL);
        }
    }

    /** ContentObserver to watch adb */
    private class BugreportObserver extends ContentObserver {
        public BugreportObserver(Handler handler) {
            super(handler);
        }

        @Override public void onChange(boolean selfChange) {
            onBugreportChanged();
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.BUGREPORT_IN_POWER_MENU), false, this);
        }
    }

    /** ContentObserver to watch brightness **/
    private class BrightnessObserver extends ContentObserver {
        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onBrightnessLevelChanged();
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                    false, this, mUserTracker.getCurrentUserId());
            cr.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    false, this, mUserTracker.getCurrentUserId());
        }
    }


    /** ContentObserver to watch mobile data on/off **/
    private class DataSwitchObserver extends ContentObserver {
        public DataSwitchObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onMobileDataSwitch();
        }

        public void startObserving() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.MOBILE_DATA),
                    false, this, mUserTracker.getCurrentUserId());
        }
    }

    /** ContentObserver to watch apn switch **/
    private class ApnObserver extends ContentObserver {
        static final String TAG = "ApnState";
        static final boolean DEBUG = true;

        public ApnObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (DEBUG) Log.i(TAG, "ApnObserver, will try to update the apn views.");
            refreshApnTile();
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Telephony.Carriers.CONTENT_URI, true, this);;
        }
    };

    /** Broadcast receive to determine if there is an apn state change. */
    private class SimStateChangedReceiver extends BroadcastReceiver {
        static final String TAG = "ApnState";
        static final boolean DEBUG = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) Log.i(TAG, "SimStateChangedReceiver, receive the action: " + action);

            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                mApnState.enabled = hasIccCard();
                if (mApnState.enabled) {
                    // we need make sure the icc state has been loaded complete and then
                    // update the apn view. If not we will maybe got the error numeric.
                    String iccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(iccState)) {
                        refreshApnTile();
                    }
                }
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (intent.getBooleanExtra("state", false)) {
                    // The airplane mode is on, set the view as gone.
                    mApnState.enabled = false;
                    refreshApnTile();
                }
            }
        }
    }

    private boolean hasIccCard() {
        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            MSimTelephonyManager msimTM =
                (MSimTelephonyManager) mContext.getSystemService(Context.MSIM_TELEPHONY_SERVICE);
            int prfDataSub = msimTM.getPreferredDataSubscription();
            int simState = msimTM.getSimState(prfDataSub);
            boolean active = simState != TelephonyManager.SIM_STATE_ABSENT
                    && simState != TelephonyManager.SIM_STATE_UNKNOWN;
            return active && msimTM.hasIccCard(prfDataSub);
        } else {
            TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.hasIccCard();
        }
    }

    private final Context mContext;
    private final Handler mHandler;
    private final CurrentUserTracker mUserTracker;
    private final NextAlarmObserver mNextAlarmObserver;
    private final BugreportObserver mBugreportObserver;
    private final BrightnessObserver mBrightnessObserver;
    private final ApnObserver mApnObserver;

    private final boolean mHasMobileData;

    private QuickSettingsTileView mUserTile;
    private RefreshCallback mUserCallback;
    private UserState mUserState = new UserState();

    private QuickSettingsTileView mTimeTile;
    private RefreshCallback mTimeCallback;
    private State mTimeState = new State();

    private QuickSettingsTileView mAlarmTile;
    private RefreshCallback mAlarmCallback;
    private State mAlarmState = new State();

    private QuickSettingsTileView mAirplaneModeTile;
    private RefreshCallback mAirplaneModeCallback;
    private State mAirplaneModeState = new State();

    private QuickSettingsTileView mWifiTile;
    private RefreshCallback mWifiCallback;
    private WifiState mWifiState = new WifiState();

    private QuickSettingsTileView mWifiDisplayTile;
    private RefreshCallback mWifiDisplayCallback;
    private State mWifiDisplayState = new State();

    private QuickSettingsTileView mRSSITile;
    private RefreshCallback mRSSICallback;
    private RSSIState mRSSIState = new RSSIState();

    private QuickSettingsTileView mBluetoothTile;
    private RefreshCallback mBluetoothCallback;
    private BluetoothState mBluetoothState = new BluetoothState();

    private QuickSettingsTileView mBatteryTile;
    private RefreshCallback mBatteryCallback;
    private BatteryState mBatteryState = new BatteryState();

    private QuickSettingsTileView mLocationTile;
    private RefreshCallback mLocationCallback;
    private State mLocationState = new State();

    private QuickSettingsTileView mImeTile;
    private RefreshCallback mImeCallback = null;
    private State mImeState = new State();

    private QuickSettingsTileView mRotationLockTile;
    private RefreshCallback mRotationLockCallback;
    private State mRotationLockState = new State();

    private QuickSettingsTileView mBrightnessTile;
    private RefreshCallback mBrightnessCallback;
    private BrightnessState mBrightnessState = new BrightnessState();

    private QuickSettingsTileView mBugreportTile;
    private RefreshCallback mBugreportCallback;
    private State mBugreportState = new State();

    private QuickSettingsTileView mSettingsTile;
    private RefreshCallback mSettingsCallback;
    private State mSettingsState = new State();

    private QuickSettingsTileView mApnTile;
    private RefreshCallback mApnCallback;
    private ApnState mApnState = new ApnState();

    public QuickSettingsModel(Context context) {
        mContext = context;
        mHandler = new Handler();
        mUserTracker = new CurrentUserTracker(mContext) {
            public void onUserSwitched(int newUserId) {
                mBrightnessObserver.startObserving();
                onRotationLockChanged();
                onBrightnessLevelChanged();
                onNextAlarmChanged();
                onBugreportChanged();
                if (dataSwitchEnabled()) {
                    onMobileDataSwitch();
                }
            }
        };

        mNextAlarmObserver = new NextAlarmObserver(mHandler);
        mNextAlarmObserver.startObserving();
        mBugreportObserver = new BugreportObserver(mHandler);
        mBugreportObserver.startObserving();
        mBrightnessObserver = new BrightnessObserver(mHandler);
        mBrightnessObserver.startObserving();

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mHasMobileData = cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);

        IntentFilter alarmIntentFilter = new IntentFilter();
        alarmIntentFilter.addAction(Intent.ACTION_ALARM_CHANGED);
        context.registerReceiver(mAlarmIntentReceiver, alarmIntentFilter);

        if (dataSwitchEnabled()) {
            new DataSwitchObserver(mHandler).startObserving();
        }

        // APN switcher
        mApnObserver = new ApnObserver(mHandler);
        mApnObserver.startObserving();
        // Register the receiver to handle the sim state changed event.
        // And caused by if we open the airplane mode, we couldn't receive the sim state
        // changed immediately, so we will also listen the airplane mode changed event.
        // If the new state is on, we need set the views as gone.
        IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        context.registerReceiver(new SimStateChangedReceiver(), filter);
    }

    void updateResources() {
        refreshSettingsTile();
        refreshBatteryTile();
        refreshBluetoothTile();
        refreshBrightnessTile();
        refreshRotationLockTile();
        refreshApnTile();
    }

    // Settings
    void addSettingsTile(QuickSettingsTileView view, RefreshCallback cb) {
        mSettingsTile = view;
        mSettingsCallback = cb;
        refreshSettingsTile();
    }
    void refreshSettingsTile() {
        Resources r = mContext.getResources();
        mSettingsState.label = r.getString(R.string.quick_settings_settings_label);
        mSettingsCallback.refreshView(mSettingsTile, mSettingsState);
    }

    // User
    void addUserTile(QuickSettingsTileView view, RefreshCallback cb) {
        mUserTile = view;
        mUserCallback = cb;
        mUserCallback.refreshView(mUserTile, mUserState);
    }
    void setUserTileInfo(String name, Drawable avatar) {
        mUserState.label = name;
        mUserState.avatar = avatar;
        mUserCallback.refreshView(mUserTile, mUserState);
    }

    // Time
    void addTimeTile(QuickSettingsTileView view, RefreshCallback cb) {
        mTimeTile = view;
        mTimeCallback = cb;
        mTimeCallback.refreshView(view, mTimeState);
    }

    // Alarm
    void addAlarmTile(QuickSettingsTileView view, RefreshCallback cb) {
        mAlarmTile = view;
        mAlarmCallback = cb;
        mAlarmCallback.refreshView(view, mAlarmState);
    }
    void onAlarmChanged(Intent intent) {
        mAlarmState.enabled = intent.getBooleanExtra("alarmSet", false);
        mAlarmCallback.refreshView(mAlarmTile, mAlarmState);
    }
    void onNextAlarmChanged() {
        final String alarmText = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED,
                UserHandle.USER_CURRENT);
        mAlarmState.label = alarmText;

        // When switching users, this is the only clue we're going to get about whether the
        // alarm is actually set, since we won't get the ACTION_ALARM_CHANGED broadcast
        mAlarmState.enabled = ! TextUtils.isEmpty(alarmText);

        mAlarmCallback.refreshView(mAlarmTile, mAlarmState);
    }

    // Airplane Mode
    void addAirplaneModeTile(QuickSettingsTileView view, RefreshCallback cb) {
        mAirplaneModeTile = view;
        mAirplaneModeTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAirplaneModeState.enabled) {
                    setAirplaneModeState(false);
                } else {
                    setAirplaneModeState(true);
                }
            }
        });
        mAirplaneModeCallback = cb;
        int airplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        onAirplaneModeChanged(airplaneMode != 0);
    }
    private void setAirplaneModeState(boolean enabled) {
        // TODO: Sets the view to be "awaiting" if not already awaiting

        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                                enabled ? 1 : 0);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabled);
        mContext.sendBroadcast(intent);
    }
    // NetworkSignalChanged callback
    @Override
    public void onAirplaneModeChanged(boolean enabled) {
        // TODO: If view is in awaiting state, disable
        Resources r = mContext.getResources();
        mAirplaneModeState.enabled = enabled;
        mAirplaneModeState.iconId = (enabled ?
                R.drawable.ic_qs_airplane_on :
                R.drawable.ic_qs_airplane_off);
        mAirplaneModeState.label = r.getString(R.string.quick_settings_airplane_mode_label);
        mAirplaneModeCallback.refreshView(mAirplaneModeTile, mAirplaneModeState);
    }

    // Wifi
    void addWifiTile(QuickSettingsTileView view, RefreshCallback cb) {
        mWifiTile = view;
        mWifiCallback = cb;
        mWifiCallback.refreshView(mWifiTile, mWifiState);
    }
    // Remove the double quotes that the SSID may contain
    public static String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }
    // Remove the period from the network name
    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            string.substring(0, length - 1);
        }
        return string;
    }
    // NetworkSignalChanged callback
    @Override
    public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId,
            String wifiSignalContentDescription, String enabledDesc) {
        // TODO: If view is in awaiting state, disable
        Resources r = mContext.getResources();

        boolean wifiConnected = enabled && (wifiSignalIconId > 0) && (enabledDesc != null);
        boolean wifiNotConnected = (wifiSignalIconId > 0) && (enabledDesc == null);
        mWifiState.enabled = enabled;
        mWifiState.connected = wifiConnected;
        if (wifiConnected) {
            mWifiState.iconId = wifiSignalIconId;
            mWifiState.label = removeDoubleQuotes(enabledDesc);
            mWifiState.signalContentDescription = wifiSignalContentDescription;
        } else if (wifiNotConnected) {
            mWifiState.iconId = R.drawable.ic_qs_wifi_0;
            mWifiState.label = r.getString(R.string.quick_settings_wifi_label);
            mWifiState.signalContentDescription = r.getString(R.string.accessibility_no_wifi);
        } else {
            mWifiState.iconId = R.drawable.ic_qs_wifi_no_network;
            mWifiState.label = r.getString(R.string.quick_settings_wifi_off_label);
            mWifiState.signalContentDescription = r.getString(R.string.accessibility_wifi_off);
        }
        mWifiCallback.refreshView(mWifiTile, mWifiState);
    }

    boolean deviceHasMobileData() {
        return mHasMobileData;
    }

    // RSSI
    void addRSSITile(QuickSettingsTileView view, RefreshCallback cb) {
        mRSSITile = view;
        mRSSICallback = cb;
        if (dataSwitchEnabled()) {
            onMobileDataSwitch();
        } else {
            mRSSICallback.refreshView(mRSSITile, mRSSIState);
        }
    }
    // NetworkSignalChanged callback
    @Override
    public void onMobileDataSignalChanged(
            boolean enabled, int mobileSignalIconId, String signalContentDescription,
            int dataTypeIconId, String dataContentDescription, String enabledDesc) {
        if (deviceHasMobileData()) {
            // TODO: If view is in awaiting state, disable
            Resources r = mContext.getResources();
            mRSSIState.signalIconId = enabled && (mobileSignalIconId > 0)
                    ? mobileSignalIconId
                    : R.drawable.ic_qs_signal_no_signal;
            mRSSIState.signalContentDescription = enabled && (mobileSignalIconId > 0)
                    ? signalContentDescription
                    : r.getString(R.string.accessibility_no_signal);
            mRSSIState.dataTypeIconId = enabled && (dataTypeIconId > 0) && !mWifiState.enabled
                    ? dataTypeIconId
                    : 0;
            mRSSIState.dataContentDescription = enabled && (dataTypeIconId > 0) && !mWifiState.enabled
                    ? dataContentDescription
                    : r.getString(R.string.accessibility_no_data);
            mRSSIState.label = enabled
                    ? removeTrailingPeriod(enabledDesc)
                    : r.getString(R.string.quick_settings_rssi_emergency_only);
            mRSSICallback.refreshView(mRSSITile, mRSSIState);
        }
    }

    boolean dataSwitchEnabled() {
        return SystemProperties.getBoolean("persist.env.sb.dataswitch", false);
    }

    void switchMobileData() {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        cm.setMobileDataEnabled(!mRSSIState.enabled);
    }

    private void onMobileDataSwitch() {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mRSSIState.enabled = cm.getMobileDataEnabled();
        if (mRSSICallback != null && mRSSITile != null) {
            mRSSICallback.refreshView(mRSSITile, mRSSIState);
        }
    }

    // Bluetooth
    void addBluetoothTile(QuickSettingsTileView view, RefreshCallback cb) {
        mBluetoothTile = view;
        mBluetoothCallback = cb;

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothState.enabled = adapter.isEnabled();
        mBluetoothState.connected =
                (adapter.getConnectionState() == BluetoothAdapter.STATE_CONNECTED);
        onBluetoothStateChange(mBluetoothState);
    }
    boolean deviceSupportsBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }
    // BluetoothController callback
    @Override
    public void onBluetoothStateChange(boolean on) {
        mBluetoothState.enabled = on;
        onBluetoothStateChange(mBluetoothState);
    }
    public void onBluetoothStateChange(BluetoothState bluetoothStateIn) {
        // TODO: If view is in awaiting state, disable
        Resources r = mContext.getResources();
        mBluetoothState.enabled = bluetoothStateIn.enabled;
        mBluetoothState.connected = bluetoothStateIn.connected;
        if (mBluetoothState.enabled) {
            if (mBluetoothState.connected) {
                mBluetoothState.iconId = R.drawable.ic_qs_bluetooth_on;
                mBluetoothState.stateContentDescription = r.getString(R.string.accessibility_desc_connected);
            } else {
                mBluetoothState.iconId = R.drawable.ic_qs_bluetooth_not_connected;
                mBluetoothState.stateContentDescription = r.getString(R.string.accessibility_desc_on);
            }
            mBluetoothState.label = r.getString(R.string.quick_settings_bluetooth_label);
        } else {
            mBluetoothState.iconId = R.drawable.ic_qs_bluetooth_off;
            mBluetoothState.label = r.getString(R.string.quick_settings_bluetooth_off_label);
            mBluetoothState.stateContentDescription = r.getString(R.string.accessibility_desc_off);
        }
        mBluetoothCallback.refreshView(mBluetoothTile, mBluetoothState);
    }
    void refreshBluetoothTile() {
        if (mBluetoothTile != null) {
            onBluetoothStateChange(mBluetoothState.enabled);
        }
    }

    // Battery
    void addBatteryTile(QuickSettingsTileView view, RefreshCallback cb) {
        mBatteryTile = view;
        mBatteryCallback = cb;
        mBatteryCallback.refreshView(mBatteryTile, mBatteryState);
    }
    // BatteryController callback
    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn) {
        mBatteryState.batteryLevel = level;
        mBatteryState.pluggedIn = pluggedIn;
        mBatteryCallback.refreshView(mBatteryTile, mBatteryState);
    }
    void refreshBatteryTile() {
        mBatteryCallback.refreshView(mBatteryTile, mBatteryState);
    }

    // Location
    void addLocationTile(QuickSettingsTileView view, RefreshCallback cb) {
        mLocationTile = view;
        mLocationCallback = cb;
        mLocationCallback.refreshView(mLocationTile, mLocationState);
    }
    // LocationController callback
    @Override
    public void onLocationGpsStateChanged(boolean inUse, String description) {
        mLocationState.enabled = inUse;
        mLocationState.label = description;
        mLocationCallback.refreshView(mLocationTile, mLocationState);
    }

    // Bug report
    void addBugreportTile(QuickSettingsTileView view, RefreshCallback cb) {
        mBugreportTile = view;
        mBugreportCallback = cb;
        onBugreportChanged();
    }
    // SettingsObserver callback
    public void onBugreportChanged() {
        final ContentResolver cr = mContext.getContentResolver();
        boolean enabled = false;
        try {
            enabled = (Settings.Global.getInt(cr, Settings.Global.BUGREPORT_IN_POWER_MENU) != 0);
        } catch (SettingNotFoundException e) {
        }

        mBugreportState.enabled = enabled;
        mBugreportCallback.refreshView(mBugreportTile, mBugreportState);
    }

    // Wifi Display
    void addWifiDisplayTile(QuickSettingsTileView view, RefreshCallback cb) {
        mWifiDisplayTile = view;
        mWifiDisplayCallback = cb;
    }
    public void onWifiDisplayStateChanged(WifiDisplayStatus status) {
        mWifiDisplayState.enabled =
                (status.getFeatureState() == WifiDisplayStatus.FEATURE_STATE_ON);
        if (status.getActiveDisplay() != null) {
            mWifiDisplayState.label = status.getActiveDisplay().getFriendlyDisplayName();
            mWifiDisplayState.iconId = R.drawable.ic_qs_remote_display_connected;
        } else {
            mWifiDisplayState.label = mContext.getString(
                    R.string.quick_settings_wifi_display_no_connection_label);
            mWifiDisplayState.iconId = R.drawable.ic_qs_remote_display;
        }
        mWifiDisplayCallback.refreshView(mWifiDisplayTile, mWifiDisplayState);

    }

    // IME
    void addImeTile(QuickSettingsTileView view, RefreshCallback cb) {
        mImeTile = view;
        mImeCallback = cb;
        mImeCallback.refreshView(mImeTile, mImeState);
    }
    /* This implementation is taken from
       InputMethodManagerService.needsToShowImeSwitchOngoingNotification(). */
    private boolean needsToShowImeSwitchOngoingNotification(InputMethodManager imm) {
        List<InputMethodInfo> imis = imm.getEnabledInputMethodList();
        final int N = imis.size();
        if (N > 2) return true;
        if (N < 1) return false;
        int nonAuxCount = 0;
        int auxCount = 0;
        InputMethodSubtype nonAuxSubtype = null;
        InputMethodSubtype auxSubtype = null;
        for(int i = 0; i < N; ++i) {
            final InputMethodInfo imi = imis.get(i);
            final List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi,
                    true);
            final int subtypeCount = subtypes.size();
            if (subtypeCount == 0) {
                ++nonAuxCount;
            } else {
                for (int j = 0; j < subtypeCount; ++j) {
                    final InputMethodSubtype subtype = subtypes.get(j);
                    if (!subtype.isAuxiliary()) {
                        ++nonAuxCount;
                        nonAuxSubtype = subtype;
                    } else {
                        ++auxCount;
                        auxSubtype = subtype;
                    }
                }
            }
        }
        if (nonAuxCount > 1 || auxCount > 1) {
            return true;
        } else if (nonAuxCount == 1 && auxCount == 1) {
            if (nonAuxSubtype != null && auxSubtype != null
                    && (nonAuxSubtype.getLocale().equals(auxSubtype.getLocale())
                            || auxSubtype.overridesImplicitlyEnabledSubtype()
                            || nonAuxSubtype.overridesImplicitlyEnabledSubtype())
                    && nonAuxSubtype.containsExtraValueKey(TAG_TRY_SUPPRESSING_IME_SWITCHER)) {
                return false;
            }
            return true;
        }
        return false;
    }
    void onImeWindowStatusChanged(boolean visible) {
        InputMethodManager imm =
                (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> imis = imm.getInputMethodList();

        mImeState.enabled = (visible && needsToShowImeSwitchOngoingNotification(imm));
        mImeState.label = getCurrentInputMethodName(mContext, mContext.getContentResolver(),
                imm, imis, mContext.getPackageManager());
        if (mImeCallback != null) {
            mImeCallback.refreshView(mImeTile, mImeState);
        }
    }
    private static String getCurrentInputMethodName(Context context, ContentResolver resolver,
            InputMethodManager imm, List<InputMethodInfo> imis, PackageManager pm) {
        if (resolver == null || imis == null) return null;
        final String currentInputMethodId = Settings.Secure.getString(resolver,
                Settings.Secure.DEFAULT_INPUT_METHOD);
        if (TextUtils.isEmpty(currentInputMethodId)) return null;
        for (InputMethodInfo imi : imis) {
            if (currentInputMethodId.equals(imi.getId())) {
                final InputMethodSubtype subtype = imm.getCurrentInputMethodSubtype();
                final CharSequence summary = subtype != null
                        ? subtype.getDisplayName(context, imi.getPackageName(),
                                imi.getServiceInfo().applicationInfo)
                        : context.getString(R.string.quick_settings_ime_label);
                return summary.toString();
            }
        }
        return null;
    }

    // Rotation lock
    void addRotationLockTile(QuickSettingsTileView view, RefreshCallback cb) {
        mRotationLockTile = view;
        mRotationLockCallback = cb;
        onRotationLockChanged();
    }
    void onRotationLockChanged() {
        boolean locked = RotationPolicy.isRotationLocked(mContext);
        mRotationLockState.enabled = locked;
        mRotationLockState.iconId = locked
                ? R.drawable.ic_qs_rotation_locked
                : R.drawable.ic_qs_auto_rotate;
        mRotationLockState.label = locked
                ? mContext.getString(R.string.quick_settings_rotation_locked_label)
                : mContext.getString(R.string.quick_settings_rotation_unlocked_label);

        // may be called before addRotationLockTile due to RotationPolicyListener in QuickSettings
        if (mRotationLockTile != null && mRotationLockCallback != null) {
            mRotationLockCallback.refreshView(mRotationLockTile, mRotationLockState);
        }
    }
    void refreshRotationLockTile() {
        if (mRotationLockTile != null) {
            onRotationLockChanged();
        }
    }

    // Brightness
    void addBrightnessTile(QuickSettingsTileView view, RefreshCallback cb) {
        mBrightnessTile = view;
        mBrightnessCallback = cb;
        onBrightnessLevelChanged();
    }
    @Override
    public void onBrightnessLevelChanged() {
        Resources r = mContext.getResources();
        int mode = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                mUserTracker.getCurrentUserId());
        mBrightnessState.autoBrightness =
                (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        mBrightnessState.iconId = mBrightnessState.autoBrightness
                ? R.drawable.ic_qs_brightness_auto_on
                : R.drawable.ic_qs_brightness_auto_off;
        mBrightnessState.label = r.getString(R.string.quick_settings_brightness_label);
        mBrightnessCallback.refreshView(mBrightnessTile, mBrightnessState);
    }
    void refreshBrightnessTile() {
        onBrightnessLevelChanged();
    }

    // APN switch
    void addApnTile(QuickSettingsTileView view, RefreshCallback cb) {
        mApnTile = view;
        mApnCallback = cb;
        refreshApnTile();
    }
    void refreshApnTile() {
        if (mApnTile != null && mApnCallback != null) {
            Resources res = mContext.getResources();
            if (mApnState.enabled) {
                mApnState.updateApnList();
            }
            mApnState.label = res.getString(R.string.quick_settings_apn_switch_label);
            mApnState.updateIconId();
            mApnCallback.refreshView(mApnTile, mApnState);
        }
    }
    public ApnState getApnState() {
        return mApnState;
    }
}
