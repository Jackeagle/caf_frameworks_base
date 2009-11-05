/*
 * Copyright (c) 2009, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of Code Aurora nor
 *      the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.hardware.FMRxAPI;

import android.hardware.FMRxAPI.FmReceiver.FmRxEventType;
import android.util.Log;

class FmRxEventListner {

    FmRxEventType mRxEvent;
    private final int EVENT_LISTEN = 1;
    private final int READY_EVENT = 0x00;
    private final int TUNE_EVENT = 0x01;
    private final int SEEK_COMPLETE_EVENT = 0x02;
    private final int SCAN_NEXT_EVENT = 0x03;
    private final int SYNC_EVENT = 0x04;
    private final int SIGNAL_EVENT = 0x05;
    private final int AUDIO_EVENT = 0x06;
    private final int RAW_RDS_EVENT = 0x07;
    private final int RT_EVENT = 0x08;
    private final int PS_EVENT = 0x09;
    private final int ERROR_EVENT = 0x0A;
    private final int BELOW_TH_EVENT = 0x0B;
    private final int ABOVE_TH_EVENT = 0x0C;
    private final int STEREO_EVENT = 0x0D;
    private final int MONO_EVENT = 0x0E;
    private final int RDS_AVAL_EVENT = 0x0F;
    private final int RDS_NOT_AVAL_EVENT = 0x10;
    private Thread mThread;
    private static final String TAG = "FMRadio";

    public void startListner (final int fd, final FmRxEvCallbacks cb) {
        /* start a thread and listen for messages */
        mThread = new Thread(){
            public void run(){
                while (true) {
                    byte []buff = new byte[128];
                    Log.d(TAG, "Starting the listener " + fd);
                    int i =FmReceiverJNI.listenForEventsNative(fd, buff, EVENT_LISTEN);
                    Log.d(TAG, "Returned with " +buff[0]+ " event. Int:" + i);
                    switch(buff[0]){
                    case READY_EVENT:
                        Log.d(TAG, "Got READY_EVENT");
                        cb.FmRxEvEnableReceiver();
                        break;
                    case TUNE_EVENT:
                        Log.d(TAG, "Got TUNE_EVENT");
                        cb.FmRxEvRadioTuneStatus();
                        break;
                    case SEEK_COMPLETE_EVENT:
                        Log.d(TAG, "Got SEEK_COMPLETE_EVENT");
                        cb.FmRxEvSearchComplete(FmReceiverJNI.getFrequencyNative(fd));
                        break;
                    case SCAN_NEXT_EVENT:
                        Log.d(TAG, "Got SCAN_NEXT_EVENT");
                        break;
                    case SYNC_EVENT:
                        Log.d(TAG, "Got SYNC_EVENT");
                        break;
                    case SIGNAL_EVENT:
                        Log.d(TAG, "Got SIGNAL_EVENT");
                        break;
                    case AUDIO_EVENT:
                        Log.d(TAG, "Got AUDIO_EVENT");
                        break;
                    case RAW_RDS_EVENT:
                        Log.d(TAG, "Got RAW_RDS_EVENT");
                        break;
                    case RT_EVENT:
                        Log.d(TAG, "Got RT_EVENT");
                        cb.FmRxEvRdsRtInfo(new StringBuffer ("This is a test RT data"));
                        break;
                    case PS_EVENT:
                        Log.d(TAG, "Got PS_EVENT");
                        cb.FmRxEvRdsPsInfo(new StringBuffer ("This is a test PS data"));
                        break;
                    case ERROR_EVENT:
                        Log.d(TAG, "Got ERROR_EVENT");
                        break;
                    case BELOW_TH_EVENT:
                        Log.d(TAG, "Got BELOW_TH_EVENT");
                        break;
                    case ABOVE_TH_EVENT:
                        Log.d(TAG, "Got ABOVE_TH_EVENT");
                        break;
                    case STEREO_EVENT:
                        Log.d(TAG, "Got STEREO_EVENT");
                        break;
                    case MONO_EVENT:
                        Log.d(TAG, "Got MONO_EVENT");
                        break;
                    case RDS_AVAL_EVENT:
                        Log.d(TAG, "Got RDS_AVAL_EVENT");
                        break;
                    case RDS_NOT_AVAL_EVENT:
                        Log.d(TAG, "Got RDS_NOT_AVAL_EVENT");
                        break;
                    default:
                        Log.d(TAG, "Unknown event");
                        break;
                    }
                }
            }
        };
        mThread.start();
    }

    public void stopListener(){
        mThread.stop();
    }

}
