/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (c) 2011, Code Aurora Forum. All rights reserved.
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

package com.android.internal.telephony.gsm;

import android.os.Parcel;
import android.os.Parcelable;

public class SmsCbHeader implements Parcelable {
    public static final int PDU_HEADER_LENGTH = 6;

    public final int geographicalScope;

    public final int messageCode;

    public final int updateNumber;

    public final int messageIdentifier;

    public final int dataCodingScheme;

    public final int pageIndex;

    public final int nrOfPages;

    public SmsCbHeader(byte[] pdu) throws IllegalArgumentException {
        if (pdu == null || pdu.length < PDU_HEADER_LENGTH) {
            throw new IllegalArgumentException("Illegal PDU");
        }

        geographicalScope = (pdu[0] & 0xc0) >>> 6;
        messageCode = ((pdu[0] & 0x3f) << 4) | ((pdu[1] & 0xf0) >>> 4);
        updateNumber = pdu[1] & 0x0f;
        messageIdentifier = ((pdu[2] & 0xff) << 8) | (pdu[3] & 0xff);
        dataCodingScheme = pdu[4];

        // Check for invalid page parameter
        int pageIndex = (pdu[5] & 0xf0) >>> 4;
        int nrOfPages = pdu[5] & 0x0f;

        if (pageIndex == 0 || nrOfPages == 0 || pageIndex > nrOfPages) {
            pageIndex = 1;
            nrOfPages = 1;
        }

        this.pageIndex = pageIndex;
        this.nrOfPages = nrOfPages;
    }

    // Copy constructor for CB Header
    public SmsCbHeader(SmsCbHeader other) {
        this.dataCodingScheme = other.dataCodingScheme;
        this.geographicalScope = other.geographicalScope;
        this.messageCode = other.messageCode;
        this.messageIdentifier = other.messageIdentifier;
        this.nrOfPages = other.nrOfPages;
        this.pageIndex = other.pageIndex;
        this.updateNumber = other.updateNumber;
    }

    @Override
    public String toString() {
        return ("[Id = " + messageIdentifier + " Code = " + messageCode
                + " Coding Scheme = " + dataCodingScheme + " Gs = "
                + geographicalScope + " Update Number = " + updateNumber + "]");
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(geographicalScope);
        dest.writeInt(messageCode);
        dest.writeInt(updateNumber);
        dest.writeInt(messageIdentifier);
        dest.writeInt(dataCodingScheme);
        dest.writeInt(pageIndex);
        dest.writeInt(nrOfPages);
    }

    public static final Parcelable.Creator<SmsCbHeader> CREATOR = new Parcelable.Creator<SmsCbHeader>() {
        public SmsCbHeader createFromParcel(Parcel in) {
            return new SmsCbHeader(in);
        }

        public SmsCbHeader[] newArray(int size) {
            return new SmsCbHeader[size];
        }
    };

    private SmsCbHeader(Parcel in) {
        geographicalScope = in.readInt();
        messageCode = in.readInt();
        updateNumber = in.readInt();
        messageIdentifier = in.readInt();
        dataCodingScheme = in.readInt();
        pageIndex = in.readInt();
        nrOfPages = in.readInt();
    }
}
