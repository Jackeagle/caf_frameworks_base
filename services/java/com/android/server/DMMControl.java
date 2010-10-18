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
    private boolean mDMM_DPD=false;
    private enum DMM_MEM_STATE {ACTIVE, DISABLED}
    private DMM_MEM_STATE mState;

    public DMMControl(Context context) {
        mContext = context;

        Log.w(TAG, "ro.dev.dmm.dpd.start_address = " + SystemProperties.get("ro.dev.dmm.dpd.start_address", "0"));
        if((SystemProperties.get("ro.dev.dmm.dpd.start_address", "0")).compareTo("0") != 0)
            mDMM_DPD = true;

        mState = DMM_MEM_STATE.ACTIVE;

        Log.w(TAG, "Initializing DMMControl complete");
    }

    protected void finalize() {
        Log.w(TAG, "De-Initializing DMMControl");
    }

    public int enableUnstableMemory(boolean flag) {
        if (flag) {
            if(mState == DMM_MEM_STATE.DISABLED) {
                if(mDMM_DPD){
                    Log.w(TAG, "Activate Unstable Memory");
                    if(Power.SetUnstableMemoryState(flag) < 0)
                        Log.e(TAG, "Activating Unstable Memory: Failed !");
                    else
                        mState = DMM_MEM_STATE.ACTIVE;
                }
            }
        } else {
            if(mState == DMM_MEM_STATE.ACTIVE) {
                if(mDMM_DPD){
                    Log.w(TAG, "De-Activate Unstable Memory");
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
