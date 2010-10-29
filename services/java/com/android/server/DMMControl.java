/*
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of Code Aurora nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
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

package com.android.server;

import android.os.Power;
import android.os.PowerManager;
import android.os.SystemProperties;

import android.app.AlarmManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.util.Log;

class DMMControl {
    private final String TAG = "DMMControl";
    private Context mContext;
    private boolean mDMM_DPD=false;
    private boolean AlarmSet=false;
    private enum DMM_MEM_STATE {ACTIVE, DISABLED}
    private DMM_MEM_STATE mState;
    private AlarmManager mAlarmManager;
    private Intent mIdleIntent;
    private PendingIntent mPendingIntent;
    private long triggerDelay;
    private static final String ACTION_DMM_TRIGGER =
        "com.android.server.DMMControl.action.DMM_TRIGGER";

    public DMMControl(Context context) {
        mContext = context;

        Log.w(TAG, "ro.dev.dmm = "
                + SystemProperties.getInt("ro.dev.dmm", 0));
        Log.w(TAG, "ro.dev.dmm.dpd.start_address = "
                + SystemProperties.get("ro.dev.dmm.dpd.start_address", "0"));

        if((SystemProperties.getInt("ro.dev.dmm", 0) == 1) &&
           (SystemProperties.get("ro.dev.dmm.dpd.start_address", "0").compareTo("0") != 0)) {
                registerForBroadcasts();
                mIdleIntent = new Intent(ACTION_DMM_TRIGGER, null);
                mPendingIntent = PendingIntent.getBroadcast(mContext, 0, mIdleIntent, 0);
                mDMM_DPD = true;
                mState = DMM_MEM_STATE.ACTIVE;
                Log.w(TAG, "Dynamic Memory Management Initialized");
        }
        else
            Log.w(TAG, "Dynamic Memory Management Disabled.");
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(ACTION_DMM_TRIGGER);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    protected void finalize() {
        Log.w(TAG, "DMMControl Finalize");
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_DMM_TRIGGER)) {
		        enableUnstableMemory(false);
            }
            else if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            }
            else if(action.equals(Intent.ACTION_SCREEN_ON)) {
                enableUnstableMemory(true);
                mAlarmManager.cancel(mPendingIntent);
            }
            else if(action.equals(Intent.ACTION_SCREEN_OFF)) {
                triggerDelay = SystemProperties.getInt("dev.dmm.dpd.trigger_delay", 0);
                triggerDelay = triggerDelay * 60 * 1000; //in milli seconds.
                triggerDelay += System.currentTimeMillis();
                mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerDelay, mPendingIntent);
            }
        }
    };

    private int enableUnstableMemory(boolean flag) {
        if (flag) {
            if(mState == DMM_MEM_STATE.DISABLED) {
                if(mDMM_DPD){
                    if(Power.SetUnstableMemoryState(flag) < 0)
                        Log.e(TAG, "Activating Unstable Memory: Failed !");
                    else
                        mState = DMM_MEM_STATE.ACTIVE;
                }
            }
        } else {
            if(mState == DMM_MEM_STATE.ACTIVE) {
                if(mDMM_DPD){
                    if(Power.SetUnstableMemoryState(flag) < 0)
                        Log.e(TAG, "Disabling Unstable Memory: Failed !");
                    else
                        mState = DMM_MEM_STATE.DISABLED;
                }
            }
        }
        return 0;
    }
}
