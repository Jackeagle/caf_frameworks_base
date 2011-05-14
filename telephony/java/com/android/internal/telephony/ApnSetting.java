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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.Phone.BearerType;

/**
 * This class represents a apn setting for create PDP link
 */
public class ApnSetting extends DataProfile {

    static final String V2_FORMAT_REGEX = "^\\[ApnSettingV2\\]\\s*";

    String carrier;
    String apn;
    String proxy;
    String port;
    String mmsc;
    String mmsProxy;
    String mmsPort;
    String user;
    String password;
    int authType;
    @Deprecated String[] types;
    DataServiceType serviceTypes[];
    int id;
    String numeric;
    BearerType bearerType = null;


    ApnSetting(int id, String numeric, String carrier, String apn, String proxy, String port,
            String mmsc, String mmsProxy, String mmsPort,
            String user, String password, int authType, String[] types, String bearerType) {
        super();
        this.id = id;
        this.numeric = numeric;
        this.carrier = carrier;
        this.apn = apn;
        this.proxy = proxy;
        this.port = port;
        this.mmsc = mmsc;
        this.mmsProxy = mmsProxy;
        this.mmsPort = mmsPort;
        this.user = user;
        this.password = password;
        this.authType = authType;
        this.types = types;

        try {
            this.bearerType = Enum.valueOf(BearerType.class, bearerType.toUpperCase());
        } catch (Exception e) {
            this.bearerType = BearerType.IP;
        }
    }

    /*
     * simple way to compare apns - toString() + username + password
     */
    String toHash() {
        return this.toString() + this.user + this.password;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString())
        .append(carrier)
        .append(", ").append(id)
        .append(", ").append(numeric)
        .append(", ").append(apn)
        .append(", ").append(proxy)
        .append(", ").append(mmsc)
        .append(", ").append(mmsProxy)
        .append(", ").append(mmsPort)
        .append(", ").append(port)
        .append(", ").append(authType)
        .append(", ").append(bearerType)
        .append(", [");
        for (String t : types) {
            sb.append(", ").append(t);
        }
        sb.append("]");
        sb.append("]");
        return sb.toString();
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString())
          .append(numeric)
          .append(", ").append(apn)
          .append("]");
        return sb.toString();
    }

    @Deprecated
    boolean canHandleType(String type) {
        for (String t : types) {
            if (t.equals(type) || t.equals(Phone.APN_TYPE_ALL)) {
                return true;
            }
        }
        return false;
    }

    boolean canHandleServiceType(DataServiceType type) {
        for (DataServiceType t : serviceTypes) {
            if (t == type)
                return true;
        }
        return false;
    }

    DataProfileType getDataProfileType() {
        return DataProfileType.PROFILE_TYPE_3GPP_APN;
    }

    @Override
    BearerType getBearerType() {
        return bearerType;
    }
}
