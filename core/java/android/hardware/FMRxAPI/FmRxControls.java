/*
 * Copyright (c) 2009, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of Code Aurora nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.hardware.FMRxAPI;

import android.hardware.FMRxAPI.FmReceiver.FmRxScanTime;
import android.hardware.FMRxAPI.FmReceiver.FmRxSrchDir;
import android.hardware.FMRxAPI.FmReceiver.FmRxSrchListMode;
import android.hardware.FMRxAPI.FmReceiver.FmRxSrchMode;
import android.util.Log;


class FmRxControls {

    private boolean mStateOn;
    private boolean mStateStereo;
    private boolean mStateMute;
    private double mFreq;

    static final int SEEK_FORWARD = 0;
    static final int SEEK_BACKWARD = 1;
    static final int SCAN_FORWARD = 2;
    static final int SCAN_BACKWARD = 3;
    private FmRxSrchMode mSrchMode;
    private FmRxScanTime mScanTime;
    private FmRxSrchDir mSrchDir;
    private FmRxSrchListMode mSrchListMode;
    private int mPrgmType;
    private int mPrgmId;

    public void fmOn(int fd) {
        mStateOn = true;
        FmReceiverJNI.changeRadioStateNative(1, fd);
    }

    public void fmOff(int fd){
        mStateOn = false;
        FmReceiverJNI.changeRadioStateNative(0, fd);
    }

    public void muteControl(int fd, boolean on) {
        mStateStereo = on;
        if(on){
            int err = FmReceiverJNI.audioControlNative(fd, 1, 1);
            Log.d("ControlMute", "Returned: " + err);
        }else
            FmReceiverJNI.audioControlNative(fd, 0,1);
    }

    public void streoControl(boolean on) {
        mStateMute = on;
    }

    public void setStation(int fd) {
        Log.d("FMTune", "** Tune Using: "+fd);
        double ret = FmReceiverJNI.changeFreqNative(mFreq, fd);
        Log.d("FMTune", "** Returned: "+ret);
    }

    public double getFreq (){
        return mFreq;
    }

    public void setFreq (double f){
        mFreq = f;
    }

    public void searchStations (int fd, FmRxSrchMode mode,
            FmRxScanTime dwelling, FmRxSrchDir direction){
        int type = 0;
        switch(mode){
        case FM_RX_SRCH_MODE_SEEK_UP:
            type = 0;
            break;
        case FM_RX_SRCH_MODE_SEEK_DOWN:
            type = 1;
            break;
        case FM_RX_SRCH_MODE_SCAN_UP:
            type = 2;
            break;
        case FM_RX_SRCH_MODE_SCAN_DOWN:
            type = 3;
            break;
        default:
            break;
        }
        FmReceiverJNI.seekScanControlNative(fd, type, 2);
    }

    public void searchRdsStations(FmRxSrchMode mode,FmRxScanTime dwelling,
        FmRxSrchDir direction, int RdsSrchPty, int RdsSrchPI){
    }

    public void searchStationList(FmRxSrchListMode listMode,FmRxSrchDir direction,
            int listMax,int pgmType) {
    }

    public void cancelSearch (int fd){
        FmReceiverJNI.cancelSearchNative(fd);
    }
}
