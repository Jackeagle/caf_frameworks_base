/*
*
* Copyright (c) 2015, The Linux Foundation. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are
* met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above
*       copyright notice, this list of conditions and the following
*       disclaimer in the documentation and/or other materials provided
*       with the distribution.
*     * Neither the name of The Linux Foundation nor the names of its
*       contributors may be used to endorse or promote products derived
*       from this software without specific prior written permission.
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
*
*/


package com.android.server;

import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Slog;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.IPiezoSoundService;
import android.os.UserHandle;



public class PiezoSoundService extends IPiezoSoundService.Stub {
    private static final String TAG = "PiezoSoundService";
    private static final int PIEZO_SERVICE_MAX_ID = 0xFFFFFFFF;
    private static final boolean DEBUG = true;

    /**
     * Broadcast intent action indicating piezo command run request has been completed
     */
    public static final String PIEZO_RUN_CMD_COMPLETE =
        "android.server.PIEZO_RUN_CMD_COMPLETE";
    /**
     * The lookup key for integer that indicates the piezo request id
     */
    public static final String PIEZO_CMD_ID = "id";

    private final Context mContext;
    private int mNativePointer = 0;

    int mLastId = 0;
    private LinkedList<PiezoSounder> mListSounder;
    private PiezoSounder mCurrentSounder;

    private class PiezoSounder implements IBinder.DeathRecipient {
        private final int mId;
        private final int mFrequency;
        private final int mDuration;
        private final int mDutyCycle;
        private final long mStartTime;

        PiezoSounder(int id, int frequency, int duration, int dutyCycle) {
            mId = id;
            mFrequency = frequency;
            mDuration = duration;
            mDutyCycle = dutyCycle;
            mStartTime = SystemClock.uptimeMillis();
        }

        public void binderDied() {
            synchronized (mListSounder) {
                mListSounder.remove(this);
                if (this == mCurrentSounder) {
                    cancelPiezoLocked(mCurrentSounder.mId);
                    mCurrentSounder = null;
                    startNextInList();
                }
            }
        }
    }

    PiezoSoundService(Context context) {

        Slog.i(TAG, "starting");

        mNativePointer = init_module();
        Slog.i(TAG, "init module called " + mNativePointer);
        mContext = context;

        mListSounder= new LinkedList<PiezoSounder>();
    };

    public int runPiezo(int frequency, int duration, int dutyCycle) {
        if (mNativePointer != 0) {
            if (DEBUG) Slog.v(TAG, "runPiezo freq:" + frequency + " duration:"
                              + duration + " dutyCycle:"+ dutyCycle);

            if(mLastId == PIEZO_SERVICE_MAX_ID) {
                mLastId = 0;
            }
            mLastId++;

            PiezoSounder piezo = new PiezoSounder(mLastId, frequency, duration, dutyCycle);
            mListSounder.add(piezo);
            if (DEBUG) Slog.v(TAG, "runPiezo add to the queue. Id:" + mLastId
                              + "  pNatve : %d" + mNativePointer);

            if (mCurrentSounder == null) {
                if (DEBUG) Slog.v(TAG, "call jni hal to start piezo");

                mCurrentSounder = piezo;
                startPiezoLocked(piezo);
            }
        }
        else {
            if (DEBUG) Slog.v(TAG, "runPiezo no valid HAL : " + mNativePointer);
        }

        return mLastId;

    }

    public void cancelPiezo(int id) {
        if (mNativePointer != 0) {
            if (DEBUG) Slog.v(TAG, "cancelPiezo id:" + id);

            PiezoSounder piezo = mListSounder.getFirst();
            if (piezo.mId == id) {
                if (DEBUG) Slog.v(TAG, "current piezo being canceled");
                mListSounder.remove(piezo);
                if (id == mCurrentSounder.mId) {
                    mCurrentSounder = null;
                    cancelPiezoLocked(id);
                }
            }
            else {
                ListIterator<PiezoSounder> iter = mListSounder.listIterator(0);
                while (iter.hasNext()) {
                    piezo = iter.next();
                    if (piezo.mId == id) {
                        if (DEBUG) Slog.v(TAG, "req in queue being canceled id:" + id);
                        iter.remove();
                    }
                }
            }
        }
        else {
            if (DEBUG) Slog.v(TAG, "cancelPiezo no valid HAL : " + mNativePointer);
        }
    }

    public boolean hasPiezo() {
        boolean isSupported = false;

        if (mNativePointer != 0) {
            isSupported = true;
        }
        return isSupported;
    }

    private void cmdDoneNotification() {
        int id;
        if (DEBUG) Slog.v(TAG, "cmd done notify: " + mCurrentSounder.mId);

        if (mCurrentSounder != null){
            id = mCurrentSounder.mId;
            mCurrentSounder = null;

            PiezoSounder piezo = mListSounder.getFirst();
            if (id == piezo.mId) {
                if (DEBUG) Slog.v(TAG, "current piezo deleted in queue " + id);
            }
            else {
                Slog.e(TAG, "first piezo ID do not match error : " + piezo.mId);
            }
            mListSounder.remove(piezo);

            /* Notify client that the request has been handled */
            Intent intent = new Intent(PiezoSoundService.PIEZO_RUN_CMD_COMPLETE);
            intent.putExtra(PiezoSoundService.PIEZO_CMD_ID, id);
            mContext.sendBroadcast(intent);

            startNextInList();
        }
        else {
            Slog.e(TAG, "error no current sounder");
        }
    }

    private void startPiezoLocked(final PiezoSounder piezo) {
        synchronized(this){
            run_piezo_native(mNativePointer, piezo.mId,
                piezo.mFrequency, piezo.mDuration, piezo.mDutyCycle);
        }
    }

    private void cancelPiezoLocked(int id) {
        synchronized(this){
            cancel_piezo_native(mNativePointer, id);
        }
    }

    private void startNextInList() {
        int size = mListSounder.size();
        if (size > 0) {
            PiezoSounder piezo = mListSounder.getFirst();

            mCurrentSounder = piezo;
            if (DEBUG) Slog.v(TAG, "start piezo in queue: " + mCurrentSounder.mId);
            startPiezoLocked(piezo);
        }
    }

    protected void finalize() throws Throwable {
        finalize_module(mNativePointer);
        super.finalize();
    }

    private native int init_module();
    private static native void finalize_module(int ptr);
    private static native void run_piezo_native(int ptr, int id, int frequency,
            int duration, int duty_cycle);
    private static native void cancel_piezo_native(int ptr, int id);
}
