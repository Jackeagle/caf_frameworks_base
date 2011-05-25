/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.nfc.technology;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.RemoteException;

/**
 * A low-level connection to a {@link Tag} using the NFC-B technology, also known as
 * ISO1443-3B.
 *
 * <p>You can acquire this kind of connection with {@link Tag#getTechnology}.
 * Use this class to send and receive data with {@link #transceive transceive()}.
 *
 * <p>Applications must implement their own protocol stack on top of
 * {@link #transceive transceive()}.
 *
 * <p class="note"><strong>Note:</strong>
 * Use of this class requires the {@link android.Manifest.permission#NFC}
 * permission.
 */
public final class NfcB extends BasicTagTechnology {
    /** @hide */
    public static final String EXTRA_APPDATA = "appdata";
    /** @hide */
    public static final String EXTRA_PROTINFO = "protinfo";

    private byte[] mAppData;
    private byte[] mProtInfo;

    public NfcB(NfcAdapter adapter, Tag tag, Bundle extras)
            throws RemoteException {
        super(adapter, tag, TagTechnology.NFC_B);
        mAppData = extras.getByteArray(EXTRA_APPDATA);
        mProtInfo = extras.getByteArray(EXTRA_PROTINFO);
    }

    /**
     * Returns the Application Data bytes from the ATQB/SENSB_RES
     * bytes discovered at tag discovery.
     */
    public byte[] getApplicationData() {
        return mAppData;
    }

    /**
     * Returns the Protocol Info bytes from the ATQB/SENSB_RES
     * bytes discovered at tag discovery.
     */
    public byte[] getProtocolInfo() {
        return mProtInfo;
    }

}
