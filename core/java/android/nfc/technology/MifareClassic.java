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

import java.io.IOException;

/**
 * Concrete class for TagTechnology.MIFARE_CLASSIC
 *
 * MIFARE Classic has n sectors, with varying sizes, although
 * they are at least the same pattern for any one MIFARE Classic
 * product. Each sector has two keys. Authentication with the correct
 * key is needed before access to any sector.
 *
 * Each sector has k blocks.
 * Block size is constant across the whole MIFARE classic family.
 */
public final class MifareClassic extends BasicTagTechnology {
    /**
     * The well-known, default MIFARE read key.
     * Use this key to effectively make the payload in this sector
     * public.
     */
    public static final byte[] KEY_DEFAULT =
            {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
    /**
     * The well-known, default MIFARE Application Directory read key.
     */
    public static final byte[] KEY_MIFARE_APPLICATION_DIRECTORY =
            {(byte)0xA0,(byte)0xA1,(byte)0xA2,(byte)0xA3,(byte)0xA4,(byte)0xA5};
    /**
     * The well-known, default read key for NDEF data on a MIFARE Classic
     */
    public static final byte[] KEY_NFC_FORUM =
            {(byte)0xD3,(byte)0xF7,(byte)0xD3,(byte)0xF7,(byte)0xD3,(byte)0xF7};

    public static final int TYPE_CLASSIC = 0;
    public static final int TYPE_PLUS = 1;
    public static final int TYPE_PRO = 2;
    public static final int TYPE_DESFIRE = 3;
    public static final int TYPE_ULTRALIGHT = 4;
    public static final int TYPE_UNKNOWN = 5;

    public static final int SIZE_1K = 1024;
    public static final int SIZE_2K = 2048;
    public static final int SIZE_4K = 4096;
    public static final int SIZE_MINI = 320;
    public static final int SIZE_UNKNOWN = 0;

    private boolean mIsEmulated;
    private int mType;
    private int mSize;

    public MifareClassic(NfcAdapter adapter, Tag tag, Bundle extras) throws RemoteException {
        super(adapter, tag, TagTechnology.MIFARE_CLASSIC);

        // Check if this could actually be a Mifare
        NfcA a = (NfcA) adapter.getTechnology(tag, TagTechnology.NFC_A);
        //short[] ATQA = getATQA(tag);

        mIsEmulated = false;
        mType = TYPE_UNKNOWN;
        mSize = SIZE_UNKNOWN;

        switch (a.getSak()) {
            case 0x00:
                // could be UL or UL-C
                mType = TYPE_ULTRALIGHT;
                break;
            case 0x08:
                // Type == classic
                // Size = 1K
                mType = TYPE_CLASSIC;
                mSize = SIZE_1K;
                break;
            case 0x09:
                // Type == classic mini
                // Size == ?
                mType = TYPE_CLASSIC;
                mSize = SIZE_MINI;
                break;
            case 0x10:
                // Type == MF+
                // Size == 2K
                // SecLevel = SL2
                mType = TYPE_PLUS;
                mSize = SIZE_2K;
                break;
            case 0x11:
                // Type == MF+
                // Size == 4K
                // Seclevel = SL2
                mType = TYPE_PLUS;
                mSize = SIZE_4K;
                break;
            case 0x18:
                // Type == classic
                // Size == 4k
                mType = TYPE_CLASSIC;
                mSize = SIZE_4K;
                break;
            case 0x20:
                // TODO this really should be a short, not byte
                if (a.getAtqa()[0] == 0x03) {
                    // Type == DESFIRE
                    mType = TYPE_DESFIRE;
                } else {
                    // Type == MF+
                    // SL = SL3
                    mType = TYPE_PLUS;
                    mSize = SIZE_UNKNOWN;
                }
                break;
            case 0x28:
                // Type == MF Classic
                // Size == 1K
                // Emulated == true
                mType = TYPE_CLASSIC;
                mSize = SIZE_1K;
                mIsEmulated = true;
                break;
            case 0x38:
                // Type == MF Classic
                // Size == 4K
                // Emulated == true
                mType = TYPE_CLASSIC;
                mSize = SIZE_4K;
                mIsEmulated = true;
                break;
            case 0x88:
                // Type == MF Classic
                // Size == 1K
                // NXP-tag: false
                mType = TYPE_CLASSIC;
                mSize = SIZE_1K;
                break;
            case 0x98:
            case 0xB8:
                // Type == MF Pro
                // Size == 4K
                mType = TYPE_PRO;
                mSize = SIZE_4K;
                break;
            default:
                // Unknown mifare
                mType = TYPE_UNKNOWN;
                mSize = SIZE_UNKNOWN;
                break;
        }
    }

    // Immutable data known at discovery time
    public int getSize() {
        return mSize;
    }

    public int getType() {
        return mType;
    }

    public boolean isEmulated() {
        return mIsEmulated;
    }

    public int getSectorCount() {
        switch (mSize) {
            case SIZE_1K: {
                return 16;
            }
            case SIZE_2K: {
                return 32;
            }
            case SIZE_4K: {
                return 40;
            }
            case SIZE_MINI: {
                return 5;
            }
            default: {
                return 0;
            }
        }
    }

    public int getSectorSize(int sector) {
        return getBlockCount(sector) * 16;
    }

    public int getTotalBlockCount() {
        int totalBlocks = 0;
        for (int sec = 0; sec < getSectorCount(); sec++) {
            totalBlocks += getSectorSize(sec);
        }

        return totalBlocks;
    }

    public int getBlockCount(int sector) {
        if (sector >= getSectorCount()) {
            throw new IllegalArgumentException("this card only has " + getSectorCount() +
                    " sectors");
        }

        if (sector <= 32) {
            return 4;
        } else {
            return 16;
        }
    }

    private byte firstBlockInSector(int sector) {
        if (sector < 32) {
            return (byte) ((sector * 4) & 0xff);
        } else {
            return (byte) ((32 * 4 + ((sector - 32) * 16)) & 0xff);
        }
    }

    // Methods that require connect()
    /**
     * Authenticate for a given block.
     * Note that this will authenticate the entire sector the block belongs to.
     */
    public boolean authenticateBlock(int block, byte[] key, boolean keyA) {
        checkConnected();

        byte[] cmd = new byte[12];

        // First byte is the command
        if (keyA) {
            cmd[0] = 0x60; // phHal_eMifareAuthentA
        } else {
            cmd[0] = 0x61; // phHal_eMifareAuthentB
        }

        // Second byte is block address
        cmd[1] = (byte) block;

        // Next 4 bytes are last 4 bytes of UID
        byte[] uid = getTag().getId();
        System.arraycopy(uid, uid.length - 4, cmd, 2, 4);

        // Next 6 bytes are key
        System.arraycopy(key, 0, cmd, 6, 6);

        try {
            if ((transceive(cmd, false) != null)) {
                return true;
            }
        } catch (IOException e) {
            // No need to deal with, will return false anyway
        }
        return false;
    }

    /**
     * Authenticate for a given sector.
     */
    public boolean authenticateSector(int sector, byte[] key, boolean keyA) {
        checkConnected();

        byte addr = (byte) ((firstBlockInSector(sector)) & 0xff);

        // Note that authenticating a block of a sector, will authenticate
        // the entire sector.
        return authenticateBlock(addr, key, keyA);
    }

    /**
     * Sector indexing starts at 0.
     * Block indexing starts at 0, and resets in each sector.
     * @throws IOException
     */
    public byte[] readBlock(int sector, int block) throws IOException {
        checkConnected();

        byte addr = (byte) ((firstBlockInSector(sector) + block) & 0xff);
        return readBlock(addr);

    }

    /**
     * Reads absolute block index.
     * @throws IOException
     */
    public byte[] readBlock(int block) throws IOException {
        checkConnected();

        byte addr = (byte) block;
        byte[] blockread_cmd = { 0x30, addr };

        return transceive(blockread_cmd, false);
    }

    /**
     * Writes absolute block index.
     * @throws IOException
     */
    public void writeBlock(int block, byte[] data) throws IOException {
        checkConnected();

        byte addr = (byte) block;
        byte[] blockwrite_cmd = new byte[data.length + 2];
        blockwrite_cmd[0] = (byte) 0xA0; // MF write command
        blockwrite_cmd[1] = addr;
        System.arraycopy(data, 0, blockwrite_cmd, 2, data.length);

        transceive(blockwrite_cmd, false);
    }

    /**
     * Writes relative block in sector.
     * @throws IOException
     */
    public void writeBlock(int sector, int block, byte[] data) throws IOException {
        checkConnected();

        byte addr = (byte) ((firstBlockInSector(sector) + block) & 0xff);

        writeBlock(addr, data);
    }

    public void increment(int block) throws IOException {
        checkConnected();

        byte addr = (byte) block;
        byte[] incr_cmd = { (byte) 0xC1, (byte) block };

        transceive(incr_cmd, false);
    }

    public void decrement(int block) throws IOException {
        checkConnected();

        byte addr = (byte) block;
        byte[] decr_cmd = { (byte) 0xC0, (byte) block };

        transceive(decr_cmd, false);
    }

    public void transfer(int block) throws IOException {
        checkConnected();

        byte addr = (byte) block;
        byte[] trans_cmd = { (byte) 0xB0, (byte) block };

        transceive(trans_cmd, false);
    }

    public void restore(int block) throws IOException {
        checkConnected();

        byte addr = (byte) block;
        byte[] rest_cmd = { (byte) 0xC2, (byte) block };

        transceive(rest_cmd, false);
    }
}
