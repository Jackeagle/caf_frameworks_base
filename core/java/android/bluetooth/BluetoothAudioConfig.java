/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents the audio configuration for a Bluetooth A2DP source device.
 *
 * {@see BluetoothA2dpSink}
 *
 * {@hide}
 */
public final class BluetoothAudioConfig implements Parcelable {

    private final int mSampleRate;
    private final int mChannelConfig;
    private final int mAudioFormat;
    private final int mCodecType;
    private static final int codecTypeSBC = 0;
    private static final int codecTypeM24 = 2;

    public BluetoothAudioConfig(int sampleRate, int channelConfig, int audioFormat,
                                int codecType) {
        mSampleRate = sampleRate;
        mChannelConfig = channelConfig;
        mAudioFormat = audioFormat;
        mCodecType = codecType;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BluetoothAudioConfig) {
            BluetoothAudioConfig bac = (BluetoothAudioConfig)o;
            return (bac.mSampleRate == mSampleRate &&
                    bac.mChannelConfig == mChannelConfig &&
                    bac.mAudioFormat == mAudioFormat &&
                    bac.mCodecType == mCodecType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mSampleRate | (mCodecType << 20) |
              (mChannelConfig << 24) | (mAudioFormat << 28);
    }

    @Override
    public String toString() {
        return "{mSampleRate:" + mSampleRate + ",mChannelConfig:" + mChannelConfig
                + ",mAudioFormat:" + mAudioFormat + ",mCodecType:" + mCodecType + "}";
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<BluetoothAudioConfig> CREATOR =
            new Parcelable.Creator<BluetoothAudioConfig>() {
        public BluetoothAudioConfig createFromParcel(Parcel in) {
            int sampleRate = in.readInt();
            int channelConfig = in.readInt();
            int audioFormat = in.readInt();
            int codecType = in.readInt();
            return new BluetoothAudioConfig(sampleRate, channelConfig, audioFormat, codecType);
        }
        public BluetoothAudioConfig[] newArray(int size) {
            return new BluetoothAudioConfig[size];
        }
    };

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mSampleRate);
        out.writeInt(mChannelConfig);
        out.writeInt(mAudioFormat);
        out.writeInt(mCodecType);
    }

    /**
     * Returns the sample rate in samples per second
     * @return sample rate
     */
    public int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Returns the channel configuration (either {@link android.media.AudioFormat#CHANNEL_IN_MONO}
     * or {@link android.media.AudioFormat#CHANNEL_IN_STEREO})
     * @return channel configuration
     */
    public int getChannelConfig() {
        return mChannelConfig;
    }

    /**
     * Returns the channel audio format (either {@link android.media.AudioFormat#ENCODING_PCM_16BIT}
     * or {@link android.media.AudioFormat#ENCODING_PCM_8BIT}
     * @return audio format
     */
    public int getAudioFormat() {
        return mAudioFormat;
    }

    /**
      * Returns the channel audio codec
      * @return audio codec
      */
    public int getCodecType() {
        return mCodecType;
    }
}
