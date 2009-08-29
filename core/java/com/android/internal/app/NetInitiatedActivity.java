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

import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

    /** Received ID from intent, -1 when no notification is in progress */
    private int mNotificationId = -1;

    /**
     * Default response when timeout. This value should match the constants
     * defined in GpsNetInitiatedHandler and GpsUserResponseType in gps_ni.h.
     */
    private int mDefaultResponse = GpsNetInitiatedHandler.GPS_NI_RESPONSE_NORESP;

    /** Total timeout time in seconds, 0 for no timeout handling.  */
    private int mTimeout = 0;

    /** Remaining time */
    private long mStartTime = 0L;
    /** Used to detect when NI request as a broadcast intent, not used yet */
    private BroadcastReceiver mNetInitiatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
         if (DEBUG) Log.d(TAG, "NetInitiatedReceiver onReceive: " + intent.getAction());
            if (intent.getAction() == GpsNetInitiatedHandler.ACTION_NI_VERIFY) {
                handleNIVerify(intent);
            }
        }
    };

    /** Used to handle response timeout */
    private Handler mHandler= new Handler();

    // Timer task class
    abstract class UpdateTimeTask extends TimerTask {
        public abstract void displayRemainTime(int minutes, int seconds);
        public abstract void onTimeOut();
        public void run() {
            long millis = System.currentTimeMillis() - mStartTime;
            int totalRemainSeconds = mTimeout - (int) (millis / 1000);
            int minutes = totalRemainSeconds / 60;
            int secs = totalRemainSeconds % 60;

            mHandler.removeCallbacks(mUpdateTimeTask);

            displayRemainTime(minutes, secs);

            if (totalRemainSeconds <= 0)
            {
                onTimeOut();
            }
            else {
                mHandler.postDelayed(mUpdateTimeTask, 1000);
            }
        }
    }

    // Timer handling routines
    private UpdateTimeTask mUpdateTimeTask = new UpdateTimeTask() {
        /**
         * Handles remaining time display on the Activity.
         */
        public void displayRemainTime(int minutes, int seconds) {
            // Handle remaining time diplay
            int whichButton;
            String btnText;

            if (DEBUG) Log.d(TAG, "Remaining time: " + minutes + "m " + seconds + "s");

            if (mDefaultResponse == GpsNetInitiatedHandler.GPS_NI_RESPONSE_ACCEPT)
            {
                whichButton = POSITIVE_BUTTON;
                btnText = BUTTON_TEXT_ACCEPT + " (" + seconds + ")";
            }
            else {
                whichButton = NEGATIVE_BUTTON;
                btnText = BUTTON_TEXT_DENY + " (" + seconds + ")";
            }

            mAlert.getButton(whichButton).setText(btnText);
        }

        /**
         * Handles user response timeout.
         *
         * Usually, this routine should send a response when user input timed out.
         * However, if the GPS driver (under HAL) can handle timeout responses
         * automatically, this routine can simply close the dialog.
         */
        public void onTimeOut() {
            Log.i(TAG, "User response timeout.");

            // Sending either GPS_NI_RESPONSE_NORESP or mDefaultResponse will be OK.
            sendUserResponse(mDefaultResponse);

            // Close the dialog
            finishDialog();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the "dialog"
        final Intent intent = getIntent();
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = com.android.internal.R.drawable.ic_dialog_alert; /* TODO change the icon */
        p.mTitle = intent.getStringExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_TITLE);
        p.mMessage = intent.getStringExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_MESSAGE);
        p.mPositiveButtonText = BUTTON_TEXT_ACCEPT;
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = BUTTON_TEXT_DENY;
        p.mNegativeButtonListener = this;

        mNotificationId = intent.getIntExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_NOTIF_ID, -1);
        mTimeout = intent.getIntExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_TIMEOUT, 0);
        mDefaultResponse = intent.getIntExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_DEFAULT_RESPONSE,
                GpsNetInitiatedHandler.GPS_NI_RESPONSE_NORESP);

        if (DEBUG) Log.d(TAG, "onCreate, notifId: " + mNotificationId + ", timeout: " + mTimeout);

        // Set up timer
        mStartTime = System.currentTimeMillis();
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 1000);

        // setup dialog
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
        finishDialog();
    }

    // Finish the dialog
    private synchronized void finishDialog()
    {
        mHandler.removeCallbacks(mUpdateTimeTask);
        if (mNotificationId != -1)
        {
            mNotificationId = -1;
            finish();
        }
    }

    // Respond to NI Handler under GpsLocationProvider, 1 = accept, 2 = deny
    private void sendUserResponse(int response) {
     if (DEBUG) Log.d(TAG, "sendUserResponse, response: " + response);

      LocationManager locationManager = (LocationManager)
      this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.sendNiResponse(mNotificationId, response);
    }

    private void handleNIVerify(Intent intent) {
     int notifId = intent.getIntExtra(GpsNetInitiatedHandler.NI_INTENT_KEY_NOTIF_ID, -1);
        mNotificationId = notifId;

     if (DEBUG) Log.d(TAG, "handleNIVerify action: " + intent.getAction());
    }



}
