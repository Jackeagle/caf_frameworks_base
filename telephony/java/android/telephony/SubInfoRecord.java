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

import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Parcelable class for Subscription Information.
 */
public class SubInfoRecord implements Parcelable {

    /**
     * Subscription Identifier, this is a device unique number
     * and not an index into an array
     */
    public int subId;
    /** The GID for a SIM that maybe associated with this subscription, empty if unknown */
    public String iccId;
    /**
     * The slot identifier for that currently contains the subscription
     * and not necessarily unique and maybe INVALID_SLOT_ID if unknown
     */
    public int slotId;
    /**
     * The string displayed to the user that identifies this subscription
     */
    public String displayName;
    /**
     * The source of the name, NAME_SOURCE_UNDEFINED, NAME_SOURCE_DEFAULT_SOURCE,
     * NAME_SOURCE_SIM_SOURCE or NAME_SOURCE_USER_INPUT.
     */
    public int nameSource;
    /**
     * The color to be used for when displaying to the user
     */
    public int color;
    /**
     * A number presented to the user identify this subscription
     */
    public String number;
    /**
     * How to display the phone number, DISPLAY_NUMBER_NONE, DISPLAY_NUMBER_FIRST,
     * DISPLAY_NUMBER_LAST
     */
    public int displayNumberFormat;
    /**
     * Data roaming state, DATA_RAOMING_ENABLE, DATA_RAOMING_DISABLE
     */
    public int dataRoaming;
    /**
     * SIM Icon resource identifiers. FIXME: Check with MTK what it really is
     */
    public int[] simIconRes;
    /**
     * Mobile Country Code
     */
    public int mcc;
    /**
     * Mobile Network Code
     */
    public int mnc;
    public int mStatus;
    public int mNwMode;

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
        this.mcc = 0;
        this.mnc = 0;
        this.mStatus = SubscriptionManager.ACTIVE;
        this.mNwMode = SubscriptionManager.DEFAULT_NW_MODE;
    }

    public SubInfoRecord(int subId, String iccId, int slotId, String displayName, int nameSource,
            int color, String number, int displayFormat, int roaming, int[] iconRes,
            int mcc, int mnc) {
        this.subId = subId;
        this.iccId = iccId;
        this.slotId = slotId;
        this.displayName = displayName;
        this.nameSource = nameSource;
        this.color = color;
        this.number = number;
        this.displayNumberFormat = displayFormat;
        this.dataRoaming = roaming;
        this.simIconRes = iconRes;
        this.mcc = mcc;
        this.mnc = mnc;
        this.mStatus = status;
        this.mNwMode = nwMode;
    }

    /**
     * Returns the string displayed to the user that identifies this subscription
     */
    public String getLabel() {
        return this.displayName;
    }

    /**
     * Return the icon used to identify this SIM.
     * TODO: return the correct drawable.
     */
    public BitmapDrawable getIconDrawable() {
        return new BitmapDrawable();
    }

    /**
     * Return the color to be used for when displaying to the user. This is the value of the color.
     * ex: 0x00ff00
     */
    public int getColor() {
        // Note: This color is currently an index into a list of drawables, but this is soon to
        // change.
        return this.color;
    }

    public static final Parcelable.Creator<SubInfoRecord> CREATOR = new Parcelable.Creator<SubInfoRecord>() {
        @Override
        public SubInfoRecord createFromParcel(Parcel source) {
            int subId = source.readInt();
            String iccId = source.readString();
            int slotId = source.readInt();
            String displayName = source.readString();
            int nameSource = source.readInt();
            int color = source.readInt();
            String number = source.readString();
            int displayNumberFormat = source.readInt();
            int dataRoaming = source.readInt();
            int[] iconRes = new int[2];
            source.readIntArray(iconRes);
            int mcc = source.readInt();
            int mnc = source.readInt();
            int status = source.readInt();
            int nwMode = source.readInt();

            return new SubInfoRecord(mSubId, mIccId, mSlotId, mDisplayName, mNameSource, mColor, mNumber,
                mDisplayNumberFormat, mDataRoaming, iconRes, mcc, mnc, status, nwMode);
        }

        @Override
        public SubInfoRecord[] newArray(int size) {
            return new SubInfoRecord[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(subId);
        dest.writeString(iccId);
        dest.writeInt(slotId);
        dest.writeString(displayName);
        dest.writeInt(nameSource);
        dest.writeInt(color);
        dest.writeString(number);
        dest.writeInt(displayNumberFormat);
        dest.writeInt(dataRoaming);
        dest.writeIntArray(simIconRes);
        dest.writeInt(mcc);
        dest.writeInt(mnc);
        dest.writeInt(mStatus);
        dest.writeInt(mNwMode);
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
                + " mSimIconRes=" + mSimIconRes + " mMcc " + mcc + " mMnc " + mnc
                + " mSubStatus=" + mStatus + " mNwMode=" + mNwMode + "}";
    }
}
