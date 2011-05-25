/*
** Copyright 2007, The Android Open Source Project
** Copyright (c) 2009-2011, Code Aurora Forum. All rights reserved.
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.internal.telephony.gsm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.IccSmsInterfaceManager;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.SmsRawData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.telephony.SmsManager.STATUS_ON_ICC_FREE;
import static android.telephony.SmsManager.STATUS_ON_ICC_READ;
import static android.telephony.SmsManager.STATUS_ON_ICC_UNREAD;

/**
 * SimSmsInterfaceManager to provide an inter-process communication to
 * access Sms in Sim.
 */
public class SimSmsInterfaceManager extends IccSmsInterfaceManager {
    static final String LOG_TAG = "GSM";
    static final boolean DBG = true;

    private final Object mLock = new Object();
    private boolean mSuccess;
    private List<SmsRawData> mSms;
    private HashMap<Integer, HashSet<String>> mCellBroadcastSubscriptions =
            new HashMap<Integer, HashSet<String>>();

    private static final int EVENT_LOAD_DONE = 1;
    private static final int EVENT_UPDATE_DONE = 2;
    private static final int EVENT_SET_BROADCAST_ACTIVATION_DONE = 3;
    private static final int EVENT_SET_BROADCAST_CONFIG_DONE = 4;
    private static final int SMS_CB_CODE_SCHEME_MIN = 0;
    private static final int SMS_CB_CODE_SCHEME_MAX = 255;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch (msg.what) {
                case EVENT_UPDATE_DONE:
                    ar = (AsyncResult) msg.obj;
                    synchronized (mLock) {
                        mSuccess = (ar.exception == null);
                        mLock.notifyAll();
                    }
                    break;
                case EVENT_LOAD_DONE:
                    ar = (AsyncResult)msg.obj;
                    synchronized (mLock) {
                        if (ar.exception == null) {
                            mSms  = (List<SmsRawData>)
                                    buildValidRawData((ArrayList<byte[]>) ar.result);
                            //Mark SMS as read after importing it from SIM.
                            markMessagesAsRead((ArrayList<byte[]>) ar.result);
                        } else {
                            if(DBG) log("Cannot load Sms records");
                            if (mSms != null)
                                mSms.clear();
                        }
                        mLock.notifyAll();
                    }
                    break;
                case EVENT_SET_BROADCAST_ACTIVATION_DONE:
                case EVENT_SET_BROADCAST_CONFIG_DONE:
                    ar = (AsyncResult) msg.obj;
                    synchronized (mLock) {
                        mSuccess = (ar.exception == null);
                        mLock.notifyAll();
                    }
	            break;
            }
        }
    };

    private void markMessagesAsRead(ArrayList<byte[]> messages) {
        if (messages == null) {
            return;
        }
        int count = messages.size();

        for (int i = 0; i < count; i++) {
             byte[] ba = messages.get(i);
             if (ba[0] == STATUS_ON_ICC_UNREAD) {
                 int n = ba.length;
                 byte[] nba = new byte[n - 1];
                 System.arraycopy(ba, 1, nba, 0, n - 1);
                 byte[] record = makeSmsRecordData(STATUS_ON_ICC_READ, nba);
                 ((SIMFileHandler)mPhone.getIccFileHandler()).updateEFLinearFixed(
                   IccConstants.EF_SMS, i + 1, record, null, null);
                 log("SMS " + (i + 1) + " marked as read");
             }
        }
    }

    public SimSmsInterfaceManager(GSMPhone phone) {
        super(phone);
        mDispatcher = new GsmSMSDispatcher(phone);
    }

    public void dispose() {
    }

    protected void finalize() {
        try {
            super.finalize();
        } catch (Throwable throwable) {
            Log.e(LOG_TAG, "Error while finalizing:", throwable);
        }
        if(DBG) Log.d(LOG_TAG, "SimSmsInterfaceManager finalized");
    }

    /**
     * Update the specified message on the SIM.
     *
     * @param index record index of message to update
     * @param status new message status (STATUS_ON_ICC_READ,
     *                  STATUS_ON_ICC_UNREAD, STATUS_ON_ICC_SENT,
     *                  STATUS_ON_ICC_UNSENT, STATUS_ON_ICC_FREE)
     * @param pdu the raw PDU to store
     * @return success or not
     *
     */
    public boolean
    updateMessageOnIccEf(int index, int status, byte[] pdu) {
        if (DBG) log("updateMessageOnIccEf: index=" + index +
                " status=" + status + " ==> " +
                "("+ Arrays.toString(pdu) + ")");
        enforceReceiveAndSend("Updating message on SIM");
        synchronized(mLock) {
            mSuccess = false;
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE);

            if (status == STATUS_ON_ICC_FREE) {
                // Special case FREE: call deleteSmsOnSim instead of
                // manipulating the SIM record
                mPhone.mCM.deleteSmsOnSim(index, response);
            } else {
                byte[] record = makeSmsRecordData(status, pdu);
                mPhone.getIccFileHandler().updateEFLinearFixed(
                        IccConstants.EF_SMS,
                        index, record, null, response);
            }
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
        }
        return mSuccess;
    }

    /**
     * Copy a raw SMS PDU to the SIM.
     *
     * @param pdu the raw PDU to store
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @return success or not
     *
     */
    public boolean copyMessageToIccEf(int status, byte[] pdu, byte[] smsc) {
        if (DBG) log("copyMessageToIccEf: status=" + status + " ==> " +
                "pdu=("+ Arrays.toString(pdu) +
                "), smsm=(" + Arrays.toString(smsc) +")");
        enforceReceiveAndSend("Copying message to SIM");
        synchronized(mLock) {
            mSuccess = false;
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE);

            mPhone.mCM.writeSmsToSim(status, IccUtils.bytesToHexString(smsc),
                    IccUtils.bytesToHexString(pdu), response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
        }
        return mSuccess;
    }

    /**
     * Retrieves all messages currently stored on ICC.
     *
     * @return list of SmsRawData of all sms on ICC
     */
    public List<SmsRawData> getAllMessagesFromIccEf() {
        if (DBG) log("getAllMessagesFromEF");

        Context context = mPhone.getContext();

        context.enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Reading messages from SIM");
        synchronized(mLock) {
            Message response = mHandler.obtainMessage(EVENT_LOAD_DONE);
            mPhone.getIccFileHandler().loadEFLinearFixedAll(IccConstants.EF_SMS, response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to load from the SIM");
            }
        }
        return mSms;
    }

    public boolean enableCellBroadcast(int messageIdentifier) {
        if (DBG) log("enableCellBroadcast");

        Context context = mPhone.getContext();

        context.enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Enabling cell broadcast SMS");

        String client = context.getPackageManager().getNameForUid(
                Binder.getCallingUid());
        HashSet<String> clients = mCellBroadcastSubscriptions.get(messageIdentifier);

        if (clients == null) {
            // This is a new message identifier
            clients = new HashSet<String>();
            mCellBroadcastSubscriptions.put(messageIdentifier, clients);

            if (!updateCellBroadcastConfig()) {
                mCellBroadcastSubscriptions.remove(messageIdentifier);
                return false;
            }
        }

        clients.add(client);

        if (DBG)
            log("Added cell broadcast subscription for MID " + messageIdentifier
                    + " from client " + client);

        return true;
    }

    public  boolean disableCellBroadcast(int messageIdentifier) {
        if (DBG) log("disableCellBroadcast");

        Context context = mPhone.getContext();

        context.enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Disabling cell broadcast SMS");

        String client = context.getPackageManager().getNameForUid(
                Binder.getCallingUid());
        HashSet<String> clients = mCellBroadcastSubscriptions.get(messageIdentifier);

        if (clients != null && clients.remove(client)) {
            if (DBG)
                log("Removed cell broadcast subscription for MID " + messageIdentifier
                        + " from client " + client);

            if (clients.isEmpty()) {
                mCellBroadcastSubscriptions.remove(messageIdentifier);
                updateCellBroadcastConfig();
            }
            return true;
        }

      return false;
    }

    private boolean updateCellBroadcastConfig() {
        Set<Integer> messageIdentifiers = mCellBroadcastSubscriptions.keySet();

        if (messageIdentifiers.size() > 0) {
            SmsBroadcastConfigInfo[] configs =
                   new SmsBroadcastConfigInfo[messageIdentifiers.size()];
            int i = 0;

            for (int messageIdentifier : messageIdentifiers) {
                configs[i++] = new SmsBroadcastConfigInfo(messageIdentifier, messageIdentifier,
                        SMS_CB_CODE_SCHEME_MIN, SMS_CB_CODE_SCHEME_MAX, true);
            }

            return setCellBroadcastConfig(configs) && setCellBroadcastActivation(true);
        } else {
            return setCellBroadcastActivation(false);
        }
    }

    private boolean setCellBroadcastConfig(SmsBroadcastConfigInfo[] configs) {
        if (DBG)
            log("Calling setGsmBroadcastConfig with " + configs.length + " configurations");

        synchronized (mLock) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_CONFIG_DONE);

            mSuccess = false;
            mPhone.mCM.setGsmBroadcastConfig(configs, response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast config");
            }
        }

        return mSuccess;
    }

    private boolean setCellBroadcastActivation(boolean activate) {
        if (DBG)
            log("Calling setCellBroadcastActivation(" + activate + ")");

        synchronized (mLock) {
            Message response = mHandler.obtainMessage(EVENT_SET_BROADCAST_ACTIVATION_DONE);

            mSuccess = false;
            mPhone.mCM.setGsmBroadcastActivation(activate, response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast activation");
            }
        }

        return mSuccess;
    }

    protected void log(String msg) {
        Log.d(LOG_TAG, "[SimSmsInterfaceManager] " + msg);
    }
}
