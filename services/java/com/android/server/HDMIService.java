/*
 * Copyright 2007, The Android Open Source Project
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
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

package com.android.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.os.IHDMIService;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

/**
 * @hide
 */
class HDMIService extends IHDMIService.Stub {

    private static final String TAG = "HDMIService";

    private Context mContext;

    private HDMIListener mListener;
    private boolean mHDMIUserOption = false;
    public final String HDMICableConnectedEvent = "HDMI_CABLE_CONNECTED";
    public final String HDMICableDisconnectedEvent = "HDMI_CABLE_DISCONNECTED";
    public final String HDMIONEvent = "HDMI_CONNECTED";
    public final String HDMIOFFEvent = "HDMI_DISCONNECTED";

    public HDMIService(Context context) {
        mContext = context;
        // Register a BOOT_COMPLETED handler so that we can start
        // HDMIListener. We defer the startup so that we don't
        // start processing events before we ought-to
        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(Intent.ACTION_BOOT_COMPLETED), null, null);
        mListener =  new HDMIListener(this);
        String hdmiUserOption = Settings.System.getString(
	                          mContext.getContentResolver(),
				  "HDMI_USEROPTION");
        if (hdmiUserOption != null && hdmiUserOption.equals("HDMI_ON"))
	    mHDMIUserOption = true;
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            String targetDevice = SystemProperties.get("ro.product.device");
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)
	             && (targetDevice != null && targetDevice.equals("msm7630_surf"))) {
                Thread thread = new Thread(mListener, HDMIListener.class.getName());
                thread.start();
            }
        }
    };

    public void shutdown() {
        if (mContext.checkCallingOrSelfPermission(
                android.Manifest.permission.SHUTDOWN)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Requires SHUTDOWN permission");
        }

        Log.e(TAG, "Shutting down");
    }

    public boolean isHDMIConnected() {
	if (mListener == null)
	    return false;
        return mListener.isHDMIConnected();
    }

    public void setHDMIOutput(boolean enableHDMI) {
	if (mListener != null && isHDMIConnected()) {
            mListener.enableHDMIOutput(enableHDMI);

	    if (enableHDMI)
                broadcastEvent(HDMIONEvent);
            else
                broadcastEvent(HDMIOFFEvent);
        }

	String hdmiUserOption;
	if (enableHDMI)
	    hdmiUserOption = "HDMI_ON";
	else
	    hdmiUserOption = "HDMI_OFF";
	Settings.System.putString(mContext.getContentResolver(),
	                   "HDMI_USEROPTION", hdmiUserOption);
	mHDMIUserOption = enableHDMI;
    }

    public boolean getHDMIUserOption() {
        return mHDMIUserOption;
    }

    public void broadcastEvent(String eventName) {
        Intent intent = new Intent(eventName);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        mContext.sendBroadcast(intent);
        Log.e(TAG, "Broadcasting ... " + eventName);
    }

    public void notifyHDMIConnected() {
        broadcastEvent(HDMICableConnectedEvent);
        if (getHDMIUserOption()) {
            mListener.enableHDMIOutput(true);
            broadcastEvent(HDMIONEvent);
        }
    }

    public void notifyHDMIDisconnected() {
        broadcastEvent(HDMICableDisconnectedEvent);
        broadcastEvent(HDMIOFFEvent);
    }
}
