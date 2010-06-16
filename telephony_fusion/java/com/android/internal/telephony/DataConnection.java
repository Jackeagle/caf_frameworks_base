/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;

import com.android.internal.telephony.DataPhone.IPVersion;

/**
 * {@hide}
 */
public abstract class DataConnection extends Handler {

    // the inherited class

    public enum State {
        ACTIVE, /* has active data connection */
        ACTIVATING, /* during connecting process */
        INACTIVE; /* has empty data connection */

        public String toString() {
            switch (this) {
                case ACTIVE:
                    return "active";
                case ACTIVATING:
                    return "setting up";
                default:
                    return "inactive";
            }
        }

        public boolean isActive() {
            return this == ACTIVE;
        }

        public boolean isInactive() {
            return this == INACTIVE;
        }
    }

    // ***** Event codes
    protected static final int EVENT_SETUP_DATA_CONNECTION_DONE = 1;
    protected static final int EVENT_DEACTIVATE_DONE = 2;

    // ***** Member Variables
    protected CommandsInterface mCM;
    protected Context mContext;

    // data call state
    protected State state;

    // if state==ACTIVE, has a valid cid
    protected int cid;

    // if state == ACTIVE, the data profile in use
    protected DataProfile mDataProfile;

    // call back data for connect()
    protected Message onConnectCompleted;

    // call back data for disconnect()
    protected Message onDisconnect;

    protected String interfaceName;
    protected String ipAddress;
    protected String gatewayAddress;
    protected String[] dnsServers;
    protected IPVersion ipVersion;

    protected long createTime;
    protected long lastFailTime;

    protected DataConnectionFailCause lastFailCause;

    // receivedDisconnectReq is set if disconnect() was
    // done at state == ACTIVATING
    protected boolean receivedDisconnectReq;

    // call back for SETUP_DATA_CALL
    protected abstract void onSetupConnectionCompleted(AsyncResult ar);

    // call back for DEACTIVATE_DATA_CALL
    protected abstract void onDeactivated(AsyncResult ar);

    // setup a data call with the specified data profile, and IP version
    protected abstract void connect(DataProfile dp, Message onCompleted, IPVersion ipVersion);

    // disconnect()
    protected abstract void disconnect(Message msg);

    // notify connect success
    protected abstract void notifySuccess(Message onCompleted);

    // notify connect fail
    protected abstract void notifyFail(DataConnectionFailCause cause, Message onCompleted);

    // notify disconnect complete
    protected abstract void notifyDisconnect(Message msg);

    public abstract String toString();

    protected abstract void log(String s);

    // ***** Constructor
    protected DataConnection(Context context, CommandsInterface ci) {
        super();

        mCM = ci;
        mContext = context;

        this.dnsServers = new String[2];

        clearSettings();
    }

    protected void setHttpProxy(String httpProxy, String httpPort) {
        if (httpProxy == null || httpProxy.length() == 0) {
            SystemProperties.set("net.gprs.http-proxy", null);
            return;
        }

        if (httpPort == null || httpPort.length() == 0) {
            httpPort = "8080"; // Default to port 8080
        }

        SystemProperties.set("net.gprs.http-proxy", "http://" + httpProxy + ":" + httpPort + "/");
    }

    public void clearSettings() {

        cid = -1;
        state = State.INACTIVE;
        mDataProfile = null;

        createTime = -1;
        lastFailTime = -1;
        lastFailCause = DataConnectionFailCause.NONE;

        receivedDisconnectReq = false;

        onDisconnect = null;
        onConnectCompleted = null;

        ipVersion = null;
        interfaceName = null;
        ipAddress = null;
        gatewayAddress = null;
        dnsServers[0] = null;
        dnsServers[1] = null;
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {

            case EVENT_SETUP_DATA_CONNECTION_DONE:
                onSetupConnectionCompleted((AsyncResult) msg.obj);
                break;

            case EVENT_DEACTIVATE_DONE:
                onDeactivated((AsyncResult) msg.obj);
                break;
        }
    }

    public State getState() {
        return state;
    }

    public long getConnectionTime() {
        return createTime;
    }

    public long getLastFailTime() {
        return lastFailTime;
    }

    public DataConnectionFailCause getLastFailCause() {
        return lastFailCause;
    }

    public String getInterface() {
        return interfaceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public String[] getDnsServers() {
        return dnsServers;
    }

    public DataProfile getDataProfile() {
        return mDataProfile;
    }

    public IPVersion getIpVersion() {
        return ipVersion;
    }
}
