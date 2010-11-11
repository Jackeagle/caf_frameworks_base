/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.internal.telephony;

import android.app.PendingIntent;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.util.Log;

import com.android.internal.util.HexDump;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.telephony.TelephonyManager;
import com.android.internal.telephony.PhoneProxy;
import static android.telephony.SmsManager.STATUS_ON_ICC_FREE;
import static android.telephony.SmsManager.STATUS_ON_ICC_READ;
import static android.telephony.SmsManager.STATUS_ON_ICC_UNREAD;

/**
 * IccSmsInterfaceManager to provide an inter-process communication to
 * access Sms in Icc.
 */
public class IccSmsInterfaceManager extends ISms.Stub {
    static final String LOG_TAG = "RIL_IccSms";
    static final boolean DBG = true;

    private final Object mLock = new Object();
    private boolean mSuccess;
    private ArrayList<SmsRawData> [] mSms;

    private static final int EVENT_LOAD_DONE = 1;
    private static final int EVENT_UPDATE_DONE = 2;

    protected VoicePhone[] mPhone;
    protected Context[] mContext;
    protected SMSDispatcher[] mDispatcher;
    protected CommandsInterface[] mCm;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            int subscription = ((Integer) ar.userObj).intValue();

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
                            mSms[subscription]  = (ArrayList<SmsRawData>)
                                    buildValidRawData((ArrayList<byte[]>) ar.result);
                            //Mark SMS as read after importing it from card.
                            markMessagesAsRead((ArrayList<byte[]>) ar.result, subscription);
                        } else {
                            if(DBG) log("Cannot load Sms records");
                            if (mSms[subscription] != null)
                                mSms[subscription].clear();
                        }
                        mLock.notifyAll();
                    }
                    break;
            }
        }
    };

    /**
     * markMessagesAsRead
     */
    private void markMessagesAsRead(ArrayList<byte[]> messages, int subscription) {
        if (messages == null) {
            return;
        }

        //IccFileHandler can be null, if icc card is absent.
        IccFileHandler fh = getIccFileHandler(subscription);
        if (fh == null) {
            //shouldn't really happen, as messages are marked as read, only
            //after importing it from icc.
            Log.e(LOG_TAG, "markMessagesAsRead - aborting, no icc card present.");
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
                 fh.updateEFLinearFixed(IccConstants.EF_SMS, i + 1, record, null, null);
                 log("SMS " + (i + 1) + " marked as read");
             }
        }
    }

    /*Non-DSDS case*/

    protected IccSmsInterfaceManager(VoicePhone phone, CommandsInterface cm){
        int numPhones = TelephonyManager.getPhoneCount();
        mPhone = new VoicePhone[numPhones];
        mPhone[0] = phone;
        mDispatcher = new ImsSMSDispatcher [numPhones];
        mSms = new ArrayList[numPhones];
        mCm = new CommandsInterface[numPhones];
        mContext = new Context[numPhones];

        mCm[0] = cm;
        mContext[0] = phone.getContext();
        mDispatcher[0] = new ImsSMSDispatcher (phone, mCm[0]);
        mSms[0] = new ArrayList<SmsRawData> ();
        if(ServiceManager.getService("isms") == null) {
            ServiceManager.addService("isms", this);
        }
    }

    /* DSDS case */
    protected IccSmsInterfaceManager(Phone[] phone, CommandsInterface[] cm){
        int numPhones = TelephonyManager.getPhoneCount();
        mPhone = new VoicePhone[numPhones];
        mDispatcher = new ImsSMSDispatcher [numPhones];
        mSms = new ArrayList[numPhones];
        mCm = new CommandsInterface[numPhones];
        mContext = new Context[numPhones];

        for (int i = 0; i < numPhones ; i++) {
            mPhone[i] = phone[i].getVoicePhone();
            mCm[i] = cm[i];
            mContext[i] =  mPhone[i].getContext();
            mDispatcher[i] = new ImsSMSDispatcher (mPhone[i], cm[i]);
            mSms[i] = new ArrayList<SmsRawData> ();
        }
        if(ServiceManager.getService("isms") == null) {
            ServiceManager.addService("isms", this);
        }
    }

    public void dispose() {
        for (int i = 0; i < TelephonyManager.getPhoneCount(); i++) {
            mDispatcher[i].dispose();
        }
    }

    protected void finalize() {
        if(DBG) Log.d(LOG_TAG, "IccSmsInterfaceManager finalized");
    }

    protected void updatePhoneObject(VoicePhone phone) {
        Log.d(LOG_TAG, "IccSmsInterfaceManager updatePhoneObject");
        mPhone[0] = phone;
        mDispatcher[0].updatePhoneObject(phone);
    }

    protected void updatePhoneObject(VoicePhone phone, int subscription) {
        Log.d(LOG_TAG, "IccSmsInterfaceManager updatePhoneObject");
        mPhone[subscription] = phone;
        mDispatcher[subscription].updatePhoneObject(phone);
    }

    protected void enforceReceiveAndSend(String message, int subscription) {
        mContext[subscription].enforceCallingPermission(
                "android.permission.RECEIVE_SMS", message);
        mContext[subscription].enforceCallingPermission(
                "android.permission.SEND_SMS", message);
    }

    /**
     * Gets User Preferred SMS subscription
     * @hide
     */
    public int getPreferredSmsSubscription() {
        return PhoneFactory.getSMSSubscription();
    }

    /**
     * Update the specified message on the UIcc.
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
        return updateMessageOnIccEfOnSubscription(index, status, pdu,
                 getPreferredSmsSubscription());
    }

    public boolean
    updateMessageOnIccEfOnSubscription(int index, int status, byte[] pdu, int subscription) {
        if (DBG) log("updateMessageOnIccEf: index=" + index +
                " status=" + status + " ==> " +
                "("+ pdu + ")");
        enforceReceiveAndSend("Updating message on UIcc", subscription);
        synchronized(mLock) {
            mSuccess = false;
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE, subscription);

            if (status == STATUS_ON_ICC_FREE) {
                // RIL_REQUEST_DELETE_SMS_ON_SIM vs RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM
                // Special case FREE: call deleteSmsOnSim/Ruim instead of
                // manipulating the record
                if (VoicePhone.PHONE_TYPE_GSM == mPhone[subscription].getPhoneType()) {
                    mCm[subscription].deleteSmsOnSim(index, response);
                } else {
                    mCm[subscription].deleteSmsOnRuim(index, response);
                }
            } else {
                //IccFilehandler can be null if ICC card is not present.
                IccFileHandler fh = getIccFileHandler(subscription);
                if (fh == null) {
                    response.recycle();
                    return mSuccess; /* is false */
                }
                byte[] record = makeSmsRecordData(status, pdu);
                fh.updateEFLinearFixed(
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
     * Copy a raw SMS PDU to the UIcc.
     *
     * @param pdu the raw PDU to store
     * @param status message status (STATUS_ON_ICC_READ, STATUS_ON_ICC_UNREAD,
     *               STATUS_ON_ICC_SENT, STATUS_ON_ICC_UNSENT)
     * @return success or not
     *
     */
    public boolean copyMessageToIccEf(int status, byte[] pdu, byte[] smsc) {
        return copyMessageToIccEfOnSubscription(status, pdu, smsc, getPreferredSmsSubscription());
    }

    public boolean copyMessageToIccEfOnSubscription(int status, byte[] pdu, byte[] smsc, int subscription) {
        //NOTE smsc not used in RUIM
        if (DBG) log("copyMessageToIccEf: status=" + status + " ==> " +
                "pdu=("+ pdu + "), smsm=(" + smsc +")");
        enforceReceiveAndSend("Copying message to UIcc", subscription);
        synchronized(mLock) {
            mSuccess = false;
            Message response = mHandler.obtainMessage(EVENT_UPDATE_DONE, subscription);

            //RIL_REQUEST_WRITE_SMS_TO_SIM vs RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM
            if (VoicePhone.PHONE_TYPE_GSM == mPhone[subscription].getPhoneType()) {
                mCm[subscription].writeSmsToSim(status, IccUtils.bytesToHexString(smsc),
                        IccUtils.bytesToHexString(pdu), response);
            } else {
                mCm[subscription].writeSmsToRuim(status, IccUtils.bytesToHexString(pdu),
                        response);
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
     * Retrieves all messages currently stored on UIcc.
     *
     * @return list of SmsRawData of all sms on UIcc
     */
    public List<SmsRawData> getAllMessagesFromIccEf() {
        return getAllMessagesFromIccEfOnSubscription(getPreferredSmsSubscription());
    }

    public List<SmsRawData> getAllMessagesFromIccEfOnSubscription(int subscription) {
        if (DBG) log("getAllMessagesFromEF");

        mContext[subscription].enforceCallingPermission(
                "android.permission.RECEIVE_SMS",
                "Reading messages from SIM");
        synchronized(mLock) {

            IccFileHandler fh = getIccFileHandler(subscription);
            if (fh == null) {
                Log.e(LOG_TAG, "Cannot load Sms records. No icc card?");
                if (mSms[subscription] != null) {
                    mSms[subscription].clear();
                    return mSms[subscription];
                }
            }

            Message response = mHandler.obtainMessage(EVENT_LOAD_DONE, subscription);
            fh.loadEFLinearFixedAll(IccConstants.EF_SMS, response);

            try {
                mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to load from the UIcc");
            }
        }
        return mSms[subscription];
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param destPort the port to deliver the message to
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    public void sendData(String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        sendDataOnSubscription(destAddr, scAddr, destPort, data, sentIntent,
                deliveryIntent, getPreferredSmsSubscription());
    }

    public void sendDataOnSubscription(String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, int subscription) {
        mContext[subscription].enforceCallingPermission(
                "android.permission.SEND_SMS",
                "Sending SMS message");
        if (Log.isLoggable("SMS", Log.VERBOSE)) {
            log("sendData: destAddr=" + destAddr + " scAddr=" + scAddr + " destPort=" +
                destPort + " data='"+ HexDump.toHexString(data)  + "' sentIntent=" +
                sentIntent + " deliveryIntent=" + deliveryIntent);
        }
        mDispatcher[subscription].sendData(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
    }

    /**
     * Send a text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK<code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     */
    public void sendText(String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        sendTextOnSubscription(destAddr, scAddr, text, sentIntent,
                deliveryIntent, getPreferredSmsSubscription() );
    }

    public void sendTextOnSubscription(String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent, int subscription) {
        mContext[subscription].enforceCallingPermission(
                "android.permission.SEND_SMS",
                "Sending SMS message");
        if (Log.isLoggable("SMS", Log.VERBOSE)) {
            log("sendText: destAddr=" + destAddr + " scAddr=" + scAddr +
                " text='"+ text + "' sentIntent=" +
                sentIntent + " deliveryIntent=" + deliveryIntent);
        }
        mDispatcher[subscription].sendText(destAddr, scAddr, text, sentIntent, deliveryIntent);
    }

    /**
     * Send a multi-part text based SMS.
     *
     * @param destAddr the address to send the message to
     * @param scAddr is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK<code> for success,
     *   or one of these errors:
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code>
     *   <code>RESULT_ERROR_RADIO_OFF</code>
     *   <code>RESULT_ERROR_NULL_PDU</code>.
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     */

    public void sendMultipartText(String destAddr, String scAddr, List<String> parts,
            List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) {
        sendMultipartTextOnSubscription(destAddr, scAddr, (ArrayList<String>) parts,
                (ArrayList<PendingIntent>) sentIntents,
                (ArrayList<PendingIntent>) deliveryIntents,
                getPreferredSmsSubscription() );
    }

    public void sendMultipartTextOnSubscription(String destAddr, String scAddr, List<String> parts,
            List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, int subscription) {
        mContext[subscription].enforceCallingPermission(
                "android.permission.SEND_SMS",
                "Sending SMS message");
        if (Log.isLoggable("SMS", Log.VERBOSE)) {
            int i = 0;
            for (String part : parts) {
                log("sendMultipartText: destAddr=" + destAddr + ", srAddr=" + scAddr +
                        ", part[" + (i++) + "]=" + part);
            }
        }
        mDispatcher[subscription].sendMultipartText(destAddr, scAddr, (ArrayList<String>) parts,
                (ArrayList<PendingIntent>) sentIntents, (ArrayList<PendingIntent>) deliveryIntents);
    }

    /**
     * create SmsRawData lists from all sms record byte[]
     * Use null to indicate "free" record
     *
     * @param messages List of message records from EF_SMS.
     * @return SmsRawData list of all in-used records
     */
    protected ArrayList<SmsRawData> buildValidRawData(ArrayList<byte[]> messages) {
        int count = messages.size();
        ArrayList<SmsRawData> ret;

        ret = new ArrayList<SmsRawData>(count);

        for (int i = 0; i < count; i++) {
            byte[] ba = messages.get(i);
            if (ba[0] == STATUS_ON_ICC_FREE) {
                ret.add(null);
            } else {
                ret.add(new SmsRawData(messages.get(i)));
            }
        }

        return ret;
    }

    /**
     * Generates an EF_SMS record from status and raw PDU.
     *
     * @param status Message status.  See TS 51.011 10.5.3.
     * @param pdu Raw message PDU.
     * @return byte array for the record.
     */
    protected byte[] makeSmsRecordData(int status, byte[] pdu) {
        byte[] data = new byte[IccConstants.SMS_RECORD_LENGTH];

        // Status bits for this record.  See TS 51.011 10.5.3
        data[0] = (byte)(status & 7);

        System.arraycopy(pdu, 0, data, 1, pdu.length);

        // Pad out with 0xFF's.
        for (int j = pdu.length+1; j < IccConstants.SMS_RECORD_LENGTH; j++) {
            data[j] = -1;
        }

        return data;
    }

    protected void log(String msg) {
        Log.d(LOG_TAG, "[IccSmsInterfaceManager] " + msg);
    }

    private IccFileHandler getIccFileHandler(int subscription) {
        return mDispatcher[subscription].getIccFileHandler();
    }
}
