/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2010-2011, Code Aurora Forum. All rights reserved.
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

package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Describes the properties of a network link.
 * TODO - consider adding optional fields like Apn and ApnType
 * @hide
 */
public class LinkProperties implements Parcelable {

    private NetworkInterface mIface;
    private Collection<InetAddress> mAddresses;
    private Collection<InetAddress> mDnses;
    private InetAddress mGateway;

    public LinkProperties() {
        clear();
    }

    // copy constructor instead of clone
    public LinkProperties(LinkProperties source) {
        if (source != null) {
            mIface = source.getInterface();
            mAddresses = source.getAddresses();
            mDnses = source.getDnses();
            mGateway = source.getGateway();
        } else {
            clear();
        }
    }

    public void setInterface(NetworkInterface iface) {
        mIface = iface;
    }
    public NetworkInterface getInterface() {
        return mIface;
    }
    public String getInterfaceName() {
        return (mIface == null ? null : mIface.getName());
    }

    public void addAddress(InetAddress address) {
        mAddresses.add(address);
    }
    public Collection<InetAddress> getAddresses() {
        return Collections.unmodifiableCollection(mAddresses);
    }

    public void addDns(InetAddress dns) {
        mDnses.add(dns);
    }
    public Collection<InetAddress> getDnses() {
        return Collections.unmodifiableCollection(mDnses);
    }

    public void setGateway(InetAddress gateway) {
        mGateway = gateway;
    }
    public InetAddress getGateway() {
        return mGateway;
    }

    public void clear() {
        mIface = null;
        mAddresses = new ArrayList<InetAddress>();
        mDnses = new ArrayList<InetAddress>();
        mGateway = null;
    }

    /**
     * Implement the Parcelable interface
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        String ifaceName = (mIface == null ? "" : "InterfaceName: " + mIface.getName() + " ");

        String ip = "IpAddresses: [";
        for (InetAddress addr : mAddresses) ip +=  addr.getHostAddress() + ",";
        ip += "] ";

        String dns = "DnsAddresses: [";
        for (InetAddress addr : mDnses) dns += addr.getHostAddress() + ",";
        dns += "] ";

        String gateway = (mGateway == null ? "" : "Gateway: " + mGateway.getHostAddress() + " ");

        return ifaceName + ip + gateway + dns;
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getInterfaceName());
        dest.writeInt(mAddresses.size());
        //TODO: explore an easy alternative to preserve hostname
        // without doing a lookup
        for(InetAddress a : mAddresses) {
            dest.writeByteArray(a.getAddress());
        }
        dest.writeInt(mDnses.size());
        for(InetAddress d : mDnses) {
            dest.writeByteArray(d.getAddress());
        }
        if (mGateway != null) {
            dest.writeByte((byte)1);
            dest.writeByteArray(mGateway.getAddress());
        } else {
            dest.writeByte((byte)0);
        }
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public static final Creator<LinkProperties> CREATOR =
        new Creator<LinkProperties>() {
            public LinkProperties createFromParcel(Parcel in) {
                LinkProperties netProp = new LinkProperties();
                String iface = in.readString();
                if (iface != null) {
                    try {
                        netProp.setInterface(NetworkInterface.getByName(iface));
                    } catch (Exception e) {
                        return null;
                    }
                }
                int addressCount = in.readInt();
                for (int i=0; i<addressCount; i++) {
                    try {
                        netProp.addAddress(InetAddress.getByAddress(in.createByteArray()));
                    } catch (UnknownHostException e) { }
                }
                addressCount = in.readInt();
                for (int i=0; i<addressCount; i++) {
                    try {
                        netProp.addDns(InetAddress.getByAddress(in.createByteArray()));
                    } catch (UnknownHostException e) { }
                }
                if (in.readByte() == 1) {
                    try {
                        netProp.setGateway(InetAddress.getByAddress(in.createByteArray()));
                    } catch (UnknownHostException e) {}
                }
                return netProp;
            }

            public LinkProperties[] newArray(int size) {
                return new LinkProperties[size];
            }
        };
}
