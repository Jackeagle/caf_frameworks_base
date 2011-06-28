/* Copyright (c) 2011, Code Aurora Forum. All rights reserved.
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
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
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
 */

package com.android.internal.atfwd;

import com.android.internal.atfwd.AtCmdHandler.AtCmdParseException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

public class AtCdisCmdHandler extends AtCmdBaseHandler implements AtCmdHandler {

    private static final String TAG = "AtCdisCmdHandler";
    private AtCdispCmdResultBroadcastReceiver atCdispCmdResultBroadcastReceiver = null;
    private int mResultCode;
    private String mResult;
    private String mErrString;
    private Context mContext;
    private long mReqTransactionId;

    // Lock for making sure the AT+CDIS command is
    // handled by the AtDisplayControlActivity
    private final Object mAtCdisRequestLock = new Object();
    private boolean mAtCdisRequestDone = false;

    // Maximum time we wait for the AT command to complete
    // This time should be sufficient to get a response from the
    // AtDisplayControlActivity.
    private static final int MAX_AT_CDIS_CMD_REQUEST_TIME = 5*1000;

    public AtCdisCmdHandler(Context context) throws AtCmdHandlerInstantiationException {
        super(context);
        mContext = context;

        // Register a broadcast receiver
        atCdispCmdResultBroadcastReceiver = new AtCdispCmdResultBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AtCdisAppInterface.AT_CDIS_CMD_RESULT);
        context.registerReceiver(atCdispCmdResultBroadcastReceiver, intentFilter);
    }

    public void finalize() {
        mContext.unregisterReceiver(atCdispCmdResultBroadcastReceiver);
    }

    @Override
    public String getCommandName() {
        return "+CDIS";
    }

    @Override
    public AtCmdResponse handleCommand(AtCmd cmd) {
        String tokens[] = cmd.getTokens();
        mResultCode = AtCmdResponse.RESULT_ERROR;
        mErrString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_UNKNOWN);

        Log.e(TAG, "OpCode:" + cmd.getOpcode());
        switch (cmd.getOpcode()) {
            case AtCmd.ATCMD_OPCODE_NA_EQ_QU:
                // AT+CDIS=?
                Log.d(TAG, "Recieved command AT+CDIS=?");
                sendCmdAndWaitForResult(AtCdisAppInterface.AT_CDIS_CMD_TEST, null);
                break;

            case AtCmd.ATCMD_OPCODE_NA_EQ_AR:
                if (tokens == null || tokens.length == 0) {
                    Log.e(TAG, "Must provide at least 1 token");
                    mErrString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_INCORRECT_PARAMS);
                    break;
                }

                // AT+CDIS=[<text>[,<text>[,...]]]
                ArrayList<String> textElements = new ArrayList<String>();
                for (int i = 0; i < tokens.length; i++) {
                    textElements.add(tokens[i]);
                }
                Bundle bundle = new Bundle();
                bundle.putStringArrayList(AtCdisAppInterface.TEXT_ELEMENTS, textElements);
                Log.d(TAG, "Recieved command AT+CDIS=" + textElements.toString());
                sendCmdAndWaitForResult(AtCdisAppInterface.AT_CDIS_CMD_SET, bundle);
                break;

            case AtCmd.ATCMD_OPCODE_NA_QU:
                // AT+CDIS?
                sendCmdAndWaitForResult(AtCdisAppInterface.AT_CDIS_CMD_READ, null);
                break;

            default:
                mErrString = cmd.getAtCmdErrStr(AtCmd.AT_ERR_OP_NOT_SUPP);
        }

        return (mResultCode == AtCmdResponse.RESULT_OK) ? new AtCmdResponse(
                AtCmdResponse.RESULT_OK, mResult) : new AtCmdResponse(AtCmdResponse.RESULT_ERROR,
                mErrString);
    }

    /**
     * This method starts the AtDisplayControlActivity and waits for the result.
     * This method doesn't return until it receives the response from the
     * activity or the timeout happens
     *
     * @param intent
     */
    private void sendCmdAndWaitForResult(String action, Bundle extras) {
        mAtCdisRequestDone = false;
        mReqTransactionId = SystemClock.elapsedRealtime();

        // Start the AtDisplayControlActivity
        Log.d(TAG, "Starting AtDisplayControlActivity");
        Intent intent = new Intent();
        intent.setClassName("com.android.internal.AtCdis",
                "com.android.internal.AtCdis.AtDisplayControlActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(action);
        intent.putExtra(AtCdisAppInterface.TRANSACTION_ID, mReqTransactionId);
        if (extras != null) {
            intent.putExtras(extras);
        }
        mContext.startActivity(intent);

        // Wait for the result
        final long endTime = SystemClock.elapsedRealtime() + MAX_AT_CDIS_CMD_REQUEST_TIME;
        synchronized (mAtCdisRequestLock) {
            while (!mAtCdisRequestDone) {
                long delay = endTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.d(TAG, "AT+CDIS request timed out");
                    mResultCode = AtCmdResponse.RESULT_ERROR;
                    return;
                }
                try {
                    Log.d(TAG, "Waiting for response for the AT+CDIS command");
                    mAtCdisRequestLock.wait(delay);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * This class extends the broadcast receiver to handle the AT+CDIS commands
     * result from the AtDisplayControlActivity
     */
    class AtCdispCmdResultBroadcastReceiver extends BroadcastReceiver {
        /*
         * (non-Javadoc)
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            long mResTransactionId = intent.getLongExtra(AtCdisAppInterface.TRANSACTION_ID, 0);
            if (mResTransactionId != mReqTransactionId) {
                Log.w(TAG, "Transaction ID of the response " + mResTransactionId
                        + " does not match the transaction id of the request " + mReqTransactionId);
                return;
            }

            // Store the results of the AT command
            mResultCode = intent.getIntExtra(AtCdisAppInterface.RESULT_CODE,
                    AtCmdResponse.RESULT_ERROR);
            mResult = intent.getStringExtra(AtCdisAppInterface.RESULT);
            Log.d(TAG, "Result code: " + mResultCode + " result string is: " + mResult);

            // Notify the waiting thread
            synchronized (mAtCdisRequestLock) {
                mAtCdisRequestDone = true;
                mAtCdisRequestLock.notify();
            }
        }
    }
}
