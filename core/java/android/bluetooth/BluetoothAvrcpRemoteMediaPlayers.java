/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
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

package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.String;
import java.util.ArrayList;
/**
 * Class used to identify Remote Media Players.
 *
 * {@hide}
 */
public final class BluetoothAvrcpRemoteMediaPlayers implements Parcelable {
    public static final String TAG = "BluetoothAvrcpRemoteMediaPlayers";

    /*
     *  Play State Values
     */
    public static final int PLAY_STATUS_STOPPED = 0x00;
    public static final int PLAY_STATUS_PLAYING = 0x01;
    public static final int PLAY_STATUS_PAUSED  = 0x02;
    public static final int PLAY_STATUS_FWD_SEEK = 0x03;
    public static final int PLAY_STATUS_REV_SEEK = 0x04;
    public static final int PLAY_STATUS_ERROR    = 0xFF;

    public static final int PLAYER_FEATURE_MASK_SIZE = 16;
    static final short PLAYER_BITMASK_PLAY_BIT_NO = 40;
    static final short PLAYER_BITMASK_STOP_BIT_NO = 41;
    static final short PLAYER_BITMASK_PAUSE_BIT_NO = 42;
    static final short PLAYER_BITMASK_REWIND_BIT_NO = 44;
    static final short PLAYER_BITMASK_FAST_FWD_BIT_NO = 45;
    static final short PLAYER_BITMASK_FORWARD_BIT_NO = 47;
    static final short PLAYER_BITMASK_BACKWARD_BIT_NO = 48;
    static final short PLAYER_BITMASK_ADV_CTRL_BIT_NO = 58;
    static final short PLAYER_BITMASK_BROWSE_BIT_NO = 59;
    static final short PLAYER_BITMASK_ADD2NOWPLAY_BIT_NO = 61;
    static final short PLAYER_BITMASK_UID_UNIQUE_BIT_NO = 62;
    static final short PLAYER_BITMASK_NOW_PLAY_BIT_NO = 65;

    class MediaPlayerInfo {
        public byte mPlayStatus;
        public int subType;
        public int mPlayerId;
        public byte majorType;
        public byte[] mFeatureMask;
        public String mPlayerName;
        private void resetPlayer() {
            mPlayStatus = PLAY_STATUS_STOPPED;
            mPlayerId   = 0;
            subType = 0; majorType = 0; mPlayerName = null;
            mFeatureMask = new byte[PLAYER_FEATURE_MASK_SIZE];
        }
        public MediaPlayerInfo() {
            resetPlayer();
        }
    }
    /**
     * List of supported media players
     */
    private ArrayList<MediaPlayerInfo> mRemoteMediaPlayerList;

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<BluetoothAvrcpRemoteMediaPlayers> CREATOR
            = new Parcelable.Creator<BluetoothAvrcpRemoteMediaPlayers>() {
        public BluetoothAvrcpRemoteMediaPlayers createFromParcel(Parcel in) {
            return new BluetoothAvrcpRemoteMediaPlayers(in);
        }

        public BluetoothAvrcpRemoteMediaPlayers[] newArray(int size) {
            return new BluetoothAvrcpRemoteMediaPlayers[size];
        }
    };
    /*
     * Flattened in following format
     * num_player(int), sub_type(4), player_id(2), major_type(1), play_status(1), features(16),
     * Name
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mRemoteMediaPlayerList.size());
        for (MediaPlayerInfo mediaPlayerInfo: mRemoteMediaPlayerList) {
            out.writeInt(mediaPlayerInfo.subType);
            out.writeInt(mediaPlayerInfo.mPlayerId);
            out.writeByte(mediaPlayerInfo.majorType);
            out.writeByte(mediaPlayerInfo.mPlayStatus);
            out.writeByteArray(mediaPlayerInfo.mFeatureMask);
            out.writeString(mediaPlayerInfo.mPlayerName);
        }
    }
    private BluetoothAvrcpRemoteMediaPlayers(Parcel in) {
        int numPlayers = in.readInt();
        mRemoteMediaPlayerList = new ArrayList<MediaPlayerInfo>();
        for (int i = 0; i < numPlayers; i++) {
            MediaPlayerInfo mPlayerInfo = new MediaPlayerInfo();
            mPlayerInfo.subType = in.readInt();
            mPlayerInfo.mPlayerId = in.readInt();
            mPlayerInfo.majorType = in.readByte();
            mPlayerInfo.mPlayStatus = in.readByte();
            in.readByteArray(mPlayerInfo.mFeatureMask);
            mPlayerInfo.mPlayerName = in.readString();
            mRemoteMediaPlayerList.add(mPlayerInfo);
        }
    }

    public BluetoothAvrcpRemoteMediaPlayers() {
        Log.d(TAG," constructor BluetoothAvrcpRemoteMediaPlayers");
        mRemoteMediaPlayerList = new ArrayList<MediaPlayerInfo>();
    }
    public void addPlayer (byte playStatus, int subType, int playerId,
                           byte majorType, byte[] featureMask, String name) {
        MediaPlayerInfo mPlayerInfo = new MediaPlayerInfo();
        mPlayerInfo.mPlayStatus = playStatus;
        mPlayerInfo.subType = subType;
        mPlayerInfo.mPlayerId = playerId;
        mPlayerInfo.majorType = majorType;
        mPlayerInfo.mFeatureMask = featureMask;
        mPlayerInfo.mPlayerName = name;
        mRemoteMediaPlayerList.add(mPlayerInfo);
    }

    public int[] getPlayerIds() {
        if ((mRemoteMediaPlayerList == null) || (mRemoteMediaPlayerList.isEmpty()))
            return  null;
        int[] playerIds = new int[mRemoteMediaPlayerList.size()];
        int i = 0;
        for (MediaPlayerInfo mediaPlayerInfo: mRemoteMediaPlayerList) {
            playerIds[i++] = mediaPlayerInfo.mPlayerId;
        }
        return playerIds;
    }
    public byte getPlayStatus(int playerId) {
        if ((mRemoteMediaPlayerList == null) || (mRemoteMediaPlayerList.isEmpty()))
            return  (byte)PLAY_STATUS_ERROR;
        for (MediaPlayerInfo mediaPlayerInfo: mRemoteMediaPlayerList) {
            if (mediaPlayerInfo.mPlayerId == playerId)
                return mediaPlayerInfo.mPlayStatus;
        }
        return (byte)PLAY_STATUS_ERROR;
    }
    public int getSubType(int playerId) {
        if ((mRemoteMediaPlayerList == null) || (mRemoteMediaPlayerList.isEmpty()))
            return  0;
        for (MediaPlayerInfo mediaPlayerInfo: mRemoteMediaPlayerList) {
            if (mediaPlayerInfo.mPlayerId == playerId)
                return mediaPlayerInfo.subType;
        }
        return 0;
    }
    public byte getMajorType(int playerId) {
        if ((mRemoteMediaPlayerList == null) || (mRemoteMediaPlayerList.isEmpty()))
            return  0;
        for (MediaPlayerInfo mediaPlayerInfo: mRemoteMediaPlayerList) {
            if (mediaPlayerInfo.mPlayerId == playerId)
                return mediaPlayerInfo.majorType;
        }
        return 0;
    }
    public byte[] getFeatureMask(int playerId) {
        if ((mRemoteMediaPlayerList == null) || (mRemoteMediaPlayerList.isEmpty()))
            return  null;
        for (MediaPlayerInfo mediaPlayerInfo: mRemoteMediaPlayerList) {
            if (mediaPlayerInfo.mPlayerId == playerId)
                return mediaPlayerInfo.mFeatureMask;
        }
        return null;
    }
    public String getPlayerName(int playerId) {
        if ((mRemoteMediaPlayerList == null) || (mRemoteMediaPlayerList.isEmpty()))
            return  null;
        for (MediaPlayerInfo mediaPlayerInfo: mRemoteMediaPlayerList) {
            if (mediaPlayerInfo.mPlayerId == playerId)
                return mediaPlayerInfo.mPlayerName;
        }
        return null;
    }
}
