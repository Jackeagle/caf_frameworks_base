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

import android.os.SystemProperties;
import android.os.Power;
import android.os.PowerManager;
import android.util.Log;
import android.os.Process;

import android.content.pm.ApplicationInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.ActivityManager;
import android.app.ListActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


class DMMControl {
    private final String TAG = "DMMControl";
    private Context mContext;
    private boolean mDMM_DPD=false, mDMM_SR=false;
    private enum DMM_MEM_STATE {ACTIVE, DEEP_POWER_DOWN, SELF_REFRESH}
    private DMM_MEM_STATE mState;

    public DMMControl(Context context) {
        mContext = context;

        Log.w(TAG, "ro.dev.dmm.dpd.start_address = " + SystemProperties.get("ro.dev.dmm.dpd.start_address", "0"));
        if((SystemProperties.get("ro.dev.dmm.dpd.start_address", "0")).compareTo("0") != 0)
            mDMM_DPD = true;

        Log.w(TAG, "ro.dev.dmm.sr.start_address = " + SystemProperties.get("ro.dev.dmm.sr.start_address", "0"));
        if((SystemProperties.get("ro.dev.dmm.sr.start_address", "0")).compareTo("0") != 0)
            mDMM_SR = true;

        mState = DMM_MEM_STATE.ACTIVE;

        Log.w(TAG, "Initializing DMMControl complete");
    }

    protected void finalize() {
        Log.w(TAG, "De-Initializing DMMControl");
    }

    private void KillEmptyProcesses() {
        ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
        Log.i(TAG, "Application Process list length = " + appList.size());
        for (ActivityManager.RunningAppProcessInfo app : appList) {
            Log.i(TAG, "process name: " + app.processName + "; importance: " + app.importance + " app.pid: " + app.pid);
            if (app.importance > 300 ) {
                Log.w(TAG, "Killing Process " + app.processName + "(" + app.pid + ")");
                Process.killProcess(app.pid);
            }
        }
    }

    public int enableUnstableMemory(boolean flag) {
        if (flag) {
            if(mState != DMM_MEM_STATE.ACTIVE) {
                if(mDMM_SR) {
                    Log.w(TAG, "Set Unstable memory to from SR to Active !");
                    if(Power.setUMtoSR(false) < 0)
                        Log.e(TAG, "SR to Active: Failed !");
                    else
                        mState = DMM_MEM_STATE.ACTIVE;
                }
                else if(mDMM_DPD){
                    Log.w(TAG, "Set Unstable memory to from DPD to Active !");
                    if(Power.setUMtoDPD(false) < 0)
                        Log.e(TAG, "DPD to Active: Failed !");
                    else
                        mState = DMM_MEM_STATE.ACTIVE;
                }
            }
            else
                Log.e(TAG, "Unstable Memory is already in ACTIVE state !!!");
        } else {
            if(mState == DMM_MEM_STATE.ACTIVE) {
                if (mDMM_SR) {
                    Log.w(TAG, "Set Unstable memory to SelfRefresh Only!");
                    if(Power.setUMtoSR(true) < 0)
                        Log.e(TAG, "Unstable memory to SelfRefresh: Failed !");
                    else
                        mState = DMM_MEM_STATE.SELF_REFRESH;
                }
                else if(mDMM_DPD){
                    Log.w(TAG, "Set Unstable memory to DPD or SR !");
                    KillEmptyProcesses();
                    if(Power.setUMtoDPD(true) < 0)
                        Log.e(TAG, "Unstable memory to DPD: Failed !");
                    else
                        mState = DMM_MEM_STATE.DEEP_POWER_DOWN;
                }
            }
            else
                Log.e(TAG, "Unstable Memory is not in ACTIVE state !!!");
        }
        return 0;
    }
}
