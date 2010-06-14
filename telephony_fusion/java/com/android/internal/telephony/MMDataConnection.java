/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.DataPhone.IPVersion;
import com.android.internal.telephony.DataProfile.DataProfileType;

/**
 * {@hide}
 */
public class MMDataConnection extends DataConnection {

    boolean DBG = true;

    private static final String LOG_TAG = "DATA";

    protected MMDataConnection(Context context, CommandsInterface ci) {
        super(context, ci);
    }

    public void clearSettings() {
        super.clearSettings();
    }

    /**
     * Setup a data call with the specified data profile
     *
     * @param dp for this connection.
     * @param onCompleted notify success or not after down
     */
    protected void connect(DataProfile dp, Message onCompleted, IPVersion ipVersion) {

        logi("Connecting : dataProfile = " + dp.toString());

        // error check
        if (state != State.INACTIVE) {
            logd("state was not INACTIVE when connect() called");
        }

        clearSettings();

        state = State.ACTIVATING;
        onConnectCompleted = onCompleted;
        mDataProfile = dp;
        this.ipVersion = ipVersion;

        /* case APN */
        if (dp.getDataProfileType() == DataProfileType.PROFILE_TYPE_3GPP_APN) {
            ApnSetting apn = (ApnSetting) mDataProfile;

            setHttpProxy(apn.proxy, apn.port);

            int authType = apn.authType;
            if (authType == -1) {
                authType = (apn.user != null) ? RILConstants.SETUP_DATA_AUTH_PAP_CHAP
                        : RILConstants.SETUP_DATA_AUTH_NONE;
            }
            this.mCM.setupDataCall(
                    Integer.toString(0),
                    Integer.toString(0), apn.apn, apn.user, apn.password, Integer.toString(authType),
                    Integer.toString(ipVersion == IPVersion.IPV6 ? 1 : 0),
                    obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE));
        } else if (dp.getDataProfileType() == DataProfileType.PROFILE_TYPE_3GPP2_NAI) {
            this.mCM.setupDataCall(Integer.toString(1), null, null, null, null, Integer
                    .toString(RILConstants.SETUP_DATA_AUTH_PAP_CHAP), Integer
                    .toString(ipVersion == IPVersion.IPV6 ? 1 : 0),
                    obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE));
        }
    }

    protected void disconnect(Message msg) {
        onDisconnect = msg;
        if (state == State.ACTIVE) {
            tearDownData(msg);
        } else if (state == State.ACTIVATING) {
            receivedDisconnectReq = true;
        } else {
            // state == INACTIVE. Nothing to do, so notify immediately.
            notifyDisconnect(msg);
        }
    }

    private void tearDownData(Message msg) {
        if (mCM.getRadioState().isOn()) {
            mCM.deactivateDataCall(cid, obtainMessage(EVENT_DEACTIVATE_DONE, msg));
        }
    }

    protected void onSetupConnectionCompleted(AsyncResult ar) {

        if (ar.exception != null) {
            loge("Data call setup failed : " + ar.exception);
            if (receivedDisconnectReq) {
                // Don't bother reporting the error if there's already a
                // pending disconnect request, since DataConnectionTracker
                // has already updated its state.
                notifyDisconnect(onDisconnect);
            } else {
                if (ar.exception instanceof CommandException) {
                    CommandException.Error e = ((CommandException) (ar.exception))
                            .getCommandError();
                    if (e == CommandException.Error.RADIO_NOT_AVAILABLE) {
                        notifyFail(DataConnectionFailCause.RADIO_NOT_AVAILABLE, onConnectCompleted);
                    } else if (e == CommandException.Error.SETUP_DATA_CALL_FAILURE) {
                        DataConnectionFailCause cause = DataConnectionFailCause.UNKNOWN;
                        int rilFailCause = ((int[]) (ar.result))[0];
                        cause = DataConnectionFailCause.getDataCallSetupFailCause(rilFailCause);
                        notifyFail(cause, onConnectCompleted);
                    } else {
                        notifyFail(DataConnectionFailCause.UNKNOWN, onConnectCompleted);
                    }
                }
            }
        } else {
            if (receivedDisconnectReq) {
                // Don't bother reporting success if there's already a
                // pending disconnect request, since DataConnectionTracker
                // has already updated its state.
                tearDownData(onDisconnect);
            } else {
                String[] response = ((String[]) ar.result);
                cid = Integer.parseInt(response[0]);

                if (response.length > 2) {

                    interfaceName = response[1];
                    ipAddress = response[2];

                    String prefix = "net." + interfaceName + ".";
                    gatewayAddress = SystemProperties.get(prefix + "gw");
                    dnsServers[0] = SystemProperties.get(prefix + "dns1");
                    dnsServers[1] = SystemProperties.get(prefix + "dns2");

                    logd("cid=" + cid + ", interface=" + interfaceName + ", ipAddress=" + ipAddress
                            + ", gateway=" + gatewayAddress + ", DNS1=" + dnsServers[0] + ", DNS2="
                            + dnsServers[1]);

// TODO: the following code was probably a hack. We shouldnt be using this.
//                    if (NULL_IP.equals(dnsServers[0]) && NULL_IP.equals(dnsServers[1])
//                            && !isDnsCheckDisabled()) {
//
//                        // Work around a race condition where QMI does not fill
//                        // in DNS: Deactivate PDP and let DataConnectionTracker
//                        // retry. Do not apply the race condition workaround for
//                        // MMS APN, if proxy is an IP-address. Otherwise, the
//                        // default APN will not be restored anymore
//
//                        if (mDataProfile != null
//                                && mDataProfile.getDataProfileType() == DataProfileType.PROFILE_TYPE_3GPP_APN
//                                && mDataProfile
//                                        .canHandleServiceType(DataServiceType.SERVICE_TYPE_MMS)
//                                && isIpAddress(((ApnSetting) mDataProfile).mmsProxy)) {
//                            EventLog.writeEvent(TelephonyEventLog.EVENT_LOG_BAD_DNS_ADDRESS,
//                                    dnsServers[0]);
//                            mCM.deactivateDataCall(cid, obtainMessage(EVENT_FORCE_RETRY));
//                            return;
//                        }
//                    }
                }

                notifySuccess(onConnectCompleted);
                logi("PDP setup on cid = " + cid);
            }
        }
    }

    private boolean isDnsCheckDisabled() {
        // TODO: fusion - fix this
        return false;
    }

    protected void onDeactivated(AsyncResult ar) {
        notifyDisconnect((Message) ar.userObj);
    }

    protected void notifyDisconnect(Message msg) {

        if (msg != null) {
            AsyncResult.forMessage(msg);
            msg.sendToTarget();
        }

        logd("Notify Data Call disconnect.");

        clearSettings();
    }

    protected void notifySuccess(Message onCompleted) {

        state = State.ACTIVE;
        createTime = System.currentTimeMillis();
        onConnectCompleted = null;

        logd("Notify Data Call setup succes at " + createTime);

        if (onCompleted != null) {
            /* on success, we pass DataConnection instance back */
            AsyncResult.forMessage(onCompleted, this, null);
            onCompleted.sendToTarget();
        }
    }

    protected void notifyFail(DataConnectionFailCause cause, Message onCompleted) {

        state = State.INACTIVE;
        lastFailCause = cause;
        lastFailTime = System.currentTimeMillis();
        onConnectCompleted = null;

        logd("Notify Data Call setup fail at " + lastFailTime + " due to " + lastFailCause);

        if (onCompleted != null) {
            /* on failure, we just pass the reason */
            AsyncResult.forMessage(onCompleted, cause, new Exception());
            onCompleted.sendToTarget();
        }
    }

    void logd(String logString) {
        if (DBG) {
            Log.d(LOG_TAG, "[DC cid = " + cid + "]" + logString);
        }
    }

    void logv(String logString) {
        if (DBG) {
            Log.d(LOG_TAG, "[DC cid = " + cid + "]" + logString);
        }
    }

    void logi(String logString) {
        Log.i(LOG_TAG, "[DC cid = " + cid + "]" + logString);
    }

    void loge(String logString) {
        Log.e(LOG_TAG, "[DC cid = " + cid + "]" + logString);
    }

    public String toString() {
        return "Cid=" + cid + ", State=" + state + ", ipv=" + ipVersion + ", create="
                + createTime + ", lastFail=" + lastFailTime + ", lastFailCause=" + lastFailCause
                + ", dp=" + mDataProfile;
    }

    @Override
    protected void log(String s) {
        logv(s);
    }
}
