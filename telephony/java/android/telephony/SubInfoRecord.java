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
 * limitations under the License.
 */

package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Parcelable class for Subscription Information.
 */
public class SubInfoRecord implements Parcelable {

    public long mSubId;
    public String mIccId;
    public int mSlotId;
    public String mDisplayName;
    public int mNameSource;
    public int mColor;
    public String mNumber;
    public int mDisplayNumberFormat;
    public int mDataRoaming;
    public int[] mSimIconRes;

    public SubInfoRecord() {
        this.mSubId = SubscriptionManager.INVALID_SUB_ID;
        this.mIccId = "";
        this.mSlotId = SubscriptionManager.INVALID_SLOT_ID;
        this.mDisplayName = "";
        this.mNameSource = 0;
        this.mColor = 0;
        this.mNumber = "";
        this.mDisplayNumberFormat = 0;
        this.mDataRoaming = 0;
        this.mSimIconRes = new int[2];
    }

    public SubInfoRecord(long subId, String iccId, int slotId, String displayName,
            int nameSource, int mColor, String mNumber, int displayFormat, int roaming, int[] iconRes) {
        this.mSubId = subId;
        this.mIccId = iccId;
        this.mSlotId = slotId;
        this.mDisplayName = displayName;
        this.mNameSource = nameSource;
        this.mColor = mColor;
        this.mNumber = mNumber;
        this.mDisplayNumberFormat = displayFormat;
        this.mDataRoaming = roaming;
        this.mSimIconRes = iconRes;
    }

    public static final Parcelable.Creator<SubInfoRecord> CREATOR = new Parcelable.Creator<SubInfoRecord>() {
        @Override
        public SubInfoRecord createFromParcel(Parcel source) {
            long mSubId = source.readLong();
            String mIccId = source.readString();
            int mSlotId = source.readInt();
            String mDisplayName = source.readString();
            int mNameSource = source.readInt();
            int mColor = source.readInt();
            String mNumber = source.readString();
            int mDisplayNumberFormat = source.readInt();
            int mDataRoaming = source.readInt();
            int[] iconRes = new int[2];
            source.readIntArray(iconRes);
            int mcc = source.readInt();
            int mnc = source.readInt();
           int status = source.readInt();
           int nwMode = source.readInt();

            return new SubInfoRecord(mSubId, mIccId, mSlotId, mDisplayName, mNameSource, mColor, mNumber,
                mDisplayNumberFormat, mDataRoaming, iconRes);
        }

        @Override
        public SubInfoRecord[] newArray(int size) {
            return new SubInfoRecord[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSubId);
        dest.writeString(mIccId);
        dest.writeInt(mSlotId);
        dest.writeString(mDisplayName);
        dest.writeInt(mNameSource);
        dest.writeInt(mColor);
        dest.writeString(mNumber);
        dest.writeInt(mDisplayNumberFormat);
        dest.writeInt(mDataRoaming);
        dest.writeIntArray(mSimIconRes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "{mSubId=" + mSubId + ", mIccId=" + mIccId + " mSlotId=" + mSlotId
                + " mDisplayName=" + mDisplayName + " mNameSource=" + mNameSource
                + " mColor=" + mColor + " mNumber=" + mNumber
                + " mDisplayNumberFormat=" + mDisplayNumberFormat + " mDataRoaming=" + mDataRoaming
                + " mSimIconRes=" + mSimIconRes + "}";
    }
}
