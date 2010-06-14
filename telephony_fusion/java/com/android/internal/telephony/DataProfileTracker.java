/*
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
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

package com.android.internal.telephony;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.provider.Telephony;
import android.util.Log;

import com.android.internal.telephony.DataConnectionTracker.State;
import com.android.internal.telephony.DataPhone.IPVersion;
import com.android.internal.telephony.DataProfile.DataProfileType;

/*
 * This class keeps track of the following :
 * requested/active service types. For each service type,
 * - the list of data profiles (ex: APN) that can handle this service type
 * - data connection that handles this data profile (if active)
 */

public class DataProfileTracker extends Handler {

    private static final String LOG_TAG = "DATA";

    private Context mContext;

    private DataProfileDbObserver mDpObserver;

    /*
     * for each service type (apn type), we have an instance of
     * DataServiceTypeInfo, that stores all metadata related to that service
     * type.
     */
    HashMap<DataServiceType, DataServiceInfo> dsMap;

    /* MCC/MNC of the current active operator */
    private String mOperatorNumeric;
    private RegistrantList mDataDataProfileDbChangedRegistrants = new RegistrantList();
    private ArrayList<DataProfile> mAllDataProfilesList;

    private static final int EVENT_DATA_PROFILE_DB_CHANGED = 1;

    /*
     * Observer for keeping track of changes to the APN database.
     */
    private class DataProfileDbObserver extends ContentObserver {
        public DataProfileDbObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {
            sendMessage(obtainMessage(EVENT_DATA_PROFILE_DB_CHANGED));
        }
    }

    DataProfileTracker(Context context) {

        mContext = context;

        /*
         * initialize data service type specific meta data
         */
        dsMap = new HashMap<DataServiceType, DataServiceInfo>();
        for (DataServiceType t : DataServiceType.values()) {
            dsMap.put(t, new DataServiceInfo(mContext, t));
        }

        /*
         * register database observer
         */
        mDpObserver = new DataProfileDbObserver(this);
        mContext.getContentResolver().registerContentObserver(Telephony.Carriers.CONTENT_URI, true,
                mDpObserver);
    }

    public void dispose() {
        // TODO Auto-generated method stub
    }

    public void handleMessage(Message msg) {

        switch (msg.what) {
            case EVENT_DATA_PROFILE_DB_CHANGED:
                onDataprofileDbChanged();
                break;
            default:
        }
    }

    /*
     * data profile database has changed, - reload everything - inform DCT about
     * this.
     */
    private void onDataprofileDbChanged() {

        ArrayList<DataProfile> allDataProfiles = new ArrayList<DataProfile>();

        if (mOperatorNumeric != null) {
            String selection = "numeric = '" + mOperatorNumeric + "'";

            /* fetch all data profiles from the database that matches the specified operator
             * numeric.
             */
            Cursor cursor = mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI,
                    null, selection, null, null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    allDataProfiles = createDataProfileList(cursor);
                }
                cursor.close();
            }
        }

        /*
         * For supporting CDMA, for now, we just create a Data profile of TYPE NAI that supports
         * all service types and add it to all DataProfiles.
         * TODO: this information should be read from apns-conf.xml / carriers db.
         */
        CdmaNAI cdmaNaiProfile = new CdmaNAI();
        allDataProfiles.add(cdmaNaiProfile);

        /*
         * clear the data profile list associated with each service type and
         * re-populate them.
         */
        for (DataServiceType t : DataServiceType.values()) {
            dsMap.get(t).mDataProfileList.clear();
        }

        for (DataProfile dp : allDataProfiles) {
            for (DataServiceType t : DataServiceType.values()) {
                if (dp.canHandleServiceType(t))
                    dsMap.get(t).mDataProfileList.add(dp);
            }
        }

        mAllDataProfilesList = allDataProfiles;

        /*
         * Notify DCT about profile db change.
         * TODO: this should be done only if the data profiles in use,
         * have really changed.
         */
        mDataDataProfileDbChangedRegistrants.notifyRegistrants();
    }

    public void setOperatorNumeric(String newOperatorNumeric) {
        if (newOperatorNumeric != mOperatorNumeric) {
            mOperatorNumeric = newOperatorNumeric;
            obtainMessage(EVENT_DATA_PROFILE_DB_CHANGED).sendToTarget();
        }
    }

    public void resetAllProfilesAsWorking() {
        if (mAllDataProfilesList != null) {
            for (DataProfile dp : mAllDataProfilesList) {
              dp.setWorking(true, IPVersion.IPV4);
              dp.setWorking(true, IPVersion.IPV6);
            }
        }
    }

    void resetAllServiceStates() {
        for (DataServiceType ds : DataServiceType.values()) {
            dsMap.get(ds).resetServiceState();
        }
    }

    void resetServiceState(DataServiceType ds) {
        dsMap.get(ds).resetServiceState();
    }

    /*
     * @param types comma delimited list of APN types
     * @return array of APN types
     */
    @Deprecated
    private String[] parseServiceTypeString(String types) {
        String[] result;
        // If unset, set to DEFAULT.
        if (types == null || types.equals("")) {
            result = new String[1];
            result[0] = DataPhone.APN_TYPE_ALL;
        } else {
            result = types.split(",");
        }
        return result;
    }

    private DataServiceType[] parseServiceTypes(String types) {
        ArrayList<DataServiceType> result = new ArrayList<DataServiceType>();
        if (types == null || types.equals("")) {
            return DataServiceType.values(); /* supports all */
        } else {
            String tempString[] = types.split(",");
            for (String ts : tempString) {
                if (DataServiceType.apnTypeStringToServiceType(ts) != null) {
                    result.add(DataServiceType.apnTypeStringToServiceType(ts));
                }
            }
        }
        return (DataServiceType[]) result.toArray();
    }

    private ArrayList<DataProfile> createDataProfileList(Cursor cursor) {
        ArrayList<DataProfile> result = new ArrayList<DataProfile>();
        if (cursor.moveToFirst()) {
            do {
                String[] types = parseServiceTypeString(cursor.getString(cursor
                        .getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
                /* its all apn now */
                ApnSetting apn = new ApnSetting(
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                        types);
                apn.serviceTypes = parseServiceTypes(cursor.getString(cursor
                        .getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
                result.add(apn);
            } while (cursor.moveToNext());
        }
        return result;
    }

    void setServiceTypeEnabled(DataServiceType ds, boolean enable) {
        dsMap.get(ds).setServiceTypeEnabled(enable);
    }

    boolean isServiceTypeEnabled(DataServiceType ds) {
        return dsMap.get(ds).isDataServiceTypeEnabled();
    }

    void setServiceTypeAsActive(DataServiceType ds, DataConnection dc, IPVersion ipv) {
        dsMap.get(ds).setDataServiceTypeAsActive(dc, ipv);
    }

    void setServiceTypeAsInactive(DataServiceType ds, IPVersion ipv) {
        dsMap.get(ds).setDataServiceTypeAsInactive(ipv);
    }

    boolean isServiceTypeActive(DataServiceType ds, IPVersion ipv) {
        return dsMap.get(ds).isServiceTypeActive(ipv);
    }

    boolean isServiceTypeActive(DataServiceType ds) {
        return dsMap.get(ds).isServiceTypeActive();
    }

    DataConnection getActiveDataConnection(DataServiceType ds, IPVersion ipv) {
        return dsMap.get(ds).getActiveDataConnection(ipv);
    }

    DataProfile getNextWorkingDataProfile(DataServiceType ds, DataProfileType dpt, IPVersion ipv) {
        return dsMap.get(ds).getNextWorkingDataProfile(dpt, ipv);
    }

    void setState(State state, DataServiceType ds, IPVersion ipv) {
        dsMap.get(ds).setState(state, ipv);
    }

    State getState(DataServiceType ds, IPVersion ipv) {
        return dsMap.get(ds).getState(ipv);
    }

    RetryManager getRetryManager(DataServiceType ds) {
        return dsMap.get(ds).getRetryManager();
    }

    void registerForDataProfileDbChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataDataProfileDbChangedRegistrants.add(r);
    }

    void unregisterForDataProfileDbChanged(Handler h) {
        mDataDataProfileDbChangedRegistrants.remove(h);
    }

    private void logv(String msg) {
        Log.v(LOG_TAG, "[DPT] " + msg);
    }

    private void logd(String msg) {
        Log.d(LOG_TAG, "[DPT] " + msg);
    }

    private void logw(String msg) {
        Log.w(LOG_TAG, "[DPT] " + msg);
    }

    private void loge(String msg) {
        Log.e(LOG_TAG, "[DPT] " + msg);
    }
}
