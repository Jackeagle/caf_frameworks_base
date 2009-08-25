/*
 *Copyright (c) 2009, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of Code Aurora nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 *  OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 *  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package com.android.internal.app;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IMountService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.widget.Toast;
import android.util.Log;
import android.location.LocationManager;
import com.android.internal.location.GpsLocationProvider;
import com.android.internal.location.GpsNetInitiatedHandler;

/**
 * This activity is shown to the user for him/her to accept or deny network-initiated
 * requests. It uses the alert dialog style. It will be launched from a notification.
 */
public class NetInitiatedActivity extends AlertActivity implements DialogInterface.OnClickListener {

 private static final String TAG = "NetInitiatedActivity";

 private static final boolean DEBUG = true;
 private static final boolean VERBOSE = false;

    private static final int POSITIVE_BUTTON = AlertDialog.BUTTON1;
    private static final int NEGATIVE_BUTTON = AlertDialog.BUTTON2;

    // Dialog button text
    public static final String BUTTON_TEXT_ACCEPT = "Accept";
    public static final String BUTTON_TEXT_DENY = "Deny";

    // Received ID from intent, -1 when no notification is in progress
    private int notificationId = -1;

    /** Used to detect when NI request is received */
    private BroadcastReceiver mNetInitiatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
         if (DEBUG) Log.d(TAG, "NetInitiatedReceiver onReceive: " + intent.getAction());
            if (intent.getAction() == GpsNetInitiatedHandler.ACTION_NI_VERIFY) {
                handleNIVerify(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the "dialog"
        final Intent intent = getIntent();
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = com.android.internal.R.drawable.ic_dialog_usb;
        p.mTitle = intent.getStringExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_TITLE);
        p.mMessage = intent.getStringExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_MESSAGE);
        p.mPositiveButtonText = BUTTON_TEXT_ACCEPT;
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = BUTTON_TEXT_DENY;
        p.mNegativeButtonListener = this;

        notificationId = intent.getIntExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_NOTIF_ID, -1);
        if (DEBUG) Log.d(TAG, "onCreate, notifId: " + notificationId);

        setupAlert();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (DEBUG) Log.d(TAG, "onResume");
        registerReceiver(mNetInitiatedReceiver, new IntentFilter(GpsNetInitiatedHandler.ACTION_NI_VERIFY));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "onPause");
        unregisterReceiver(mNetInitiatedReceiver);
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(DialogInterface dialog, int which) {

        if (which == POSITIVE_BUTTON) {
         sendUserResponse(GpsNetInitiatedHandler.GPS_NI_RESPONSE_ACCEPT);
        }
        if (which == NEGATIVE_BUTTON) {
         sendUserResponse(GpsNetInitiatedHandler.GPS_NI_RESPONSE_DENY);
        }

        // No matter what, finish the activity
        finish();
        notificationId = -1;
    }

    // Respond to NI Handler under GpsLocationProvider, 1 = accept, 2 = deny
    private void sendUserResponse(int response) {
     if (DEBUG) Log.d(TAG, "sendUserResponse, response: " + response);

      LocationManager locationManager = (LocationManager)
      this.getSystemService(Context.LOCATION_SERVICE);
      locationManager.sendNiResponse(notificationId, response);
    }

    private void handleNIVerify(Intent intent) {
     int notifId = intent.getIntExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_NOTIF_ID, -1);
     notificationId = notifId;

     if (DEBUG) Log.d(TAG, "handleNIVerify action: " + intent.getAction());
    }

    private void showNIError() {
        Toast.makeText(this, "NI error" /* com.android.internal.R.string.usb_storage_error_message */,
          Toast.LENGTH_LONG).show();
    }

}
