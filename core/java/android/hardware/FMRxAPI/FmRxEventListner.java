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

    FmRxEventType rxEvent;
    private final int READY_EVENT = 0x01;
    private final int TUNE_EVENT = 0x02;
    private final int SEEK_COMPLETE_EVENT = 0x03;
    private final int MUTE_EVENT = 0x04;
    private final int RDS_EVENT = 0x08;
    private Thread mThread;

    public void startListner (final int fd, final FmRxEvCallbacks cb) {
        /* start a thread and listen for messages */
        mThread = new Thread(){
            public void run(){
                while (true) {
                    byte []buff = new byte[128];
                    Log.d("Rec Events", "Starting the listen " + fd);
                    int i =FmReceiverJNI.listenForEventsNative(fd, buff, 5);
                    Log.d("Receiver Events", "Returned with " +buff[0]+ " event. Int:" + i);
                    switch(buff[0]){
                    case READY_EVENT:
                        cb.FmRxEvEnableReceiver();
                    break;
                    case TUNE_EVENT:
                        cb.FmRxEvRadioTuneStatus();
                        break;
                    case SEEK_COMPLETE_EVENT:
                        cb.FmRxEvSerachComplete();
                        break;
                    case RDS_EVENT:
                        Log.d("Receiver Events", "Returned with new RDS tx event.");
                        cb.FmRxEvRdsRtInfo(new StringBuffer ("This is a test RT data"));
                        break;
                    case MUTE_EVENT:
                        Log.d("Receiver Events", "Got Mute Interrupt");
                        break;
                    default:
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
