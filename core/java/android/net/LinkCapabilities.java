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

import android.os.Parcelable;
import android.os.Parcel;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A class representing the capabilities of a link
 *
 * @hide
 */
public class LinkCapabilities implements Parcelable {
    private static final String TAG = "LinkCapabilities";
    private static final boolean DBG = false;

    /** The Map of Keys to Values */
    private HashMap<Integer, String> mCapabilities;


    /**
     * The set of keys defined for a links capabilities.
     *
     * Keys starting with RW are read + write, i.e. the application
     * can request for a certain requirement corresponding to that key.
     * Keys starting with RO are read only, i.e. the the application
     * can read the value of that key from the socket but cannot request
     * a corresponding requirement.
     *
     * TODO: Provide a documentation technique for concisely and precisely
     * define the syntax for each value string associated with a key.
     */
    public static final class Key {
        /** No constructor */
        private Key() {}

        /**
         * A string containing the socket's role
         */
        public final static int RW_ROLE = 0;

        /**
         * An integer representing the network type.
         * @see ConnectivityManager
         */
        public final static int RO_NETWORK_TYPE = 1;

        /**
         * Desired minimum forward link (download) bandwidth for the
         * in kilobits per second (kbps). Values should be strings such
         * "50", "100", "1500", etc.
         */
        public final static int RW_DESIRED_FWD_BW = 2;

        /**
         * Required minimum forward link (download) bandwidth, in
         * per second (kbps), below which the socket cannot function.
         * Values should be strings such as "50", "100", "1500", etc.
         */
        public final static int RW_REQUIRED_FWD_BW = 3;

        /**
         * Available forward link (download) bandwidth for the socket.
         * This value is in kilobits per second (kbps).
         * Values will be strings such as "50", "100", "1500", etc.
         */
        public final static int RO_AVAILABLE_FWD_BW = 4;

        /**
         * Desired minimum reverse link (upload) bandwidth for the socket
         * in kilobits per second (kbps).
         * Values should be strings such as "50", "100", "1500", etc.
         * <p>
         * This key is set via the needs map.
         */
        public final static int RW_DESIRED_REV_BW = 5;

        /**
         * Required minimum reverse link (upload) bandwidth, in kilobits
         * per second (kbps), below which the socket cannot function.
         * If a rate is not specified, the default rate of kbps will be
         * Values should be strings such as "50", "100", "1500", etc.
         */
        public final static int RW_REQUIRED_REV_BW = 6;

        /**
         * Available reverse link (upload) bandwidth for the socket.
         * This value is in kilobits per second (kbps).
         * Values will be strings such as "50", "100", "1500", etc.
         */
        public final static int RO_AVAILABLE_REV_BW = 7;

        /**
         * Maximum latency for the socket, in milliseconds, above which
         * socket cannot function.
         * Values should be strings such as "50", "300", "500", etc.
         */
        public final static int RW_MAX_ALLOWED_LATENCY = 8;

        /**
         * Current estimated latency of the socket, in milliseconds.
         * Values will be strings such as "50", "100", "1500", etc.
         */
        public final static int RO_CURRENT_LATENCY = 9;

        /**
         * Interface that the socket is bound to. This can be a virtual
         * interface (e.g. VPN or Mobile IP) or a physical interface
         * (e.g. wlan0 or rmnet0).
         * Values will be strings such as "wlan0", "rmnet0"
         */
        // TODO: consider removing, this is information about the interface, not the socket
        public final static int RO_BOUND_INTERFACE = 10;

        /**
         * Physical interface that the socket is routed on.
         * This can be different from BOUND_INTERFACE in cases such as
         * VPN or Mobile IP. The physical interface may change over time
         * if seamless mobility is supported.
         * Values will be strings such as "wlan0", "rmnet0"
         */
        // TODO: consider removing, this is information about the interface, not the socket
        public final static int RO_PHYSICAL_INTERFACE = 11;

        /**
         * Network types that the socket will restrict itself to.
         * If the network type is not on this list, the socket will
         * not bind to it.
         * Values will be comma separated strings such as "wifi",
         * "mobile", or "mobile, wimax".
         */
        public final static int RW_ALLOWED_NETWORKS = 12;

        /**
         * Network types that the socket will not bind to.
         * Values will be comma separated strings such as "wifi",
         * "mobile", or "mobile, wimax".
         */
        public final static int RW_PROHIBITED_NETWORKS = 13;

        /**
         * If set to true, LinkSocket will not send callback
         * notifications to the application.
         * Values should be a String set to either "true" or "false"
         */
        public final static int RW_DISABLE_NOTIFICATIONS = 14;

        /**
         * A string containing the socket's role, as classified by the carrier.
         */
        public final static int RW_CARRIER_ROLE = 15;
    }

    /**
     * Role informs the LinkSocket about the data usage patterns of your
     * application.
     * <P>
     * {@code Role.DEFAULT} is the default role, and is used whenever
     * a role isn't set.
     */
    public static final class Role {
        /** No constructor */
        private Role() {}

        /** Default Role */
        public static final String DEFAULT = "default";

        /** Video Streaming at 1040p */
        public static final String VIDEO_STREAMING_1040P = "video_streaming_1040p";
    }

    /**
     * The Carrier Role is used to classify LinkSockets for carrier-specified usage.
     * <P>
     * {@code Role.DEFAULT} is the default role, and is used whenever
     * a role isn't set.
     */
    public static final class CarrierRole {
        /** No constructor */
        private CarrierRole() {}

        /** Default Role */
        public static final String DEFAULT = "default";

        /** Delay Sensitive - prioritize latency */
        public static final String DELAY_SENSITIVE = "delay_sensitive";

        /** High Throughput - prioritize throughput */
        public static final String HIGH_THROUGHPUT = "high_throughput";

        /** Short Lived - sockets that will only be open for a few seconds at most */
        public static final String SHORT_LIVED = "short_lived";

        /** Bulk down load - low priority download */
        public static final String BULK_DOWNLOAD = "bulk_download";

        /** Bulk upload - low priority upload */
        public static final String BULK_UPLOAD = "bulk_upload";
    }

    /**
     * Constructor
     */
    public LinkCapabilities() {
        mCapabilities = new HashMap<Integer, String>();
    }

    /**
     * Copy constructor.
     *
     * @param source
     */
    public LinkCapabilities(LinkCapabilities source) {
        if (source != null) {
            mCapabilities = new HashMap<Integer, String>(source.mCapabilities);
        } else {
            mCapabilities = new HashMap<Integer, String>();
        }
    }

    /**
     * Create the {@code LinkCapabilities} with values depending on role type.
     * @param applicationRole a {@code LinkSocket.Role}
     * @return the {@code LinkCapabilities} associated with the applicationRole, empty if none
     */
    public static LinkCapabilities createNeeds(String applicationRole) {
        if (DBG) log("createNeeds(applicationRole) EX");
        LinkCapabilities cap = new LinkCapabilities();
        cap.put(Key.RW_ROLE, applicationRole);

        // Map specific application roles to their respective needs
        if (applicationRole == Role.DEFAULT) {
            cap.put(Key.RW_CARRIER_ROLE, CarrierRole.DEFAULT);
        } else if (applicationRole == Role.VIDEO_STREAMING_1040P) {
            cap.put(Key.RW_CARRIER_ROLE, CarrierRole.HIGH_THROUGHPUT);
            cap.put(Key.RW_REQUIRED_REV_BW, "2500"); // Require 2.5Mbps bandwidth
            cap.put(Key.RW_MAX_ALLOWED_LATENCY, "500"); // 500ms latency
        }

        return cap;
    }

    /**
     * Remove all capabilities
     */
    public void clear() {
        mCapabilities.clear();
    }

    /**
     * Returns whether this map is empty.
     */
    public boolean isEmpty() {
        return mCapabilities.isEmpty();
    }

    /**
     * Returns the number of elements in this map.
     *
     * @return the number of elements in this map.
     */
    public int size() {
        return mCapabilities.size();
    }

    /**
     * Returns the value of the capability string with the specified key.
     *
     * @param key
     * @return the value of the capability string with the specified key,
     * or {@code null} if no mapping for the specified key is found.
     */
    public String get(int key) {
        return mCapabilities.get(key);
    }

    /**
     * Store the key/value capability pair
     *
     * @param key
     * @param value
     */
    public void put(int key, String value) {
        mCapabilities.put(key, value);
    }

    /**
     * Returns whether this map contains the specified key.
     *
     * @param key to search for.
     * @return {@code true} if this map contains the specified key,
     *         {@code false} otherwise.
     */
    public boolean containsKey(int key) {
        return mCapabilities.containsKey(key);
    }

    /**
     * Returns whether this map contains the specified value.
     *
     * @param value to search for.
     * @return {@code true} if this map contains the specified value,
     *         {@code false} otherwise.
     */
    public boolean containsValue(String value) {
        return mCapabilities.containsValue(value);
    }

    /**
     * Returns a set containing all of the mappings in this map. Each mapping is
     * an instance of {@link Map.Entry}. As the set is backed by this map,
     * changes in one will be reflected in the other.
     *
     * @return a set of the mappings.
     */
    public Set<Entry<Integer, String>> entrySet() {
        return mCapabilities.entrySet();
    }

    /**
     * @return the set of the keys.
     */
    public Set<Integer> keySet() {
        return mCapabilities.keySet();
    }

    /**
     * @return the set of values
     */
    public Collection<String> values() {
        return mCapabilities.values();
    }

    /**
     * Implement the Parcelable interface
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Convert to string for debugging
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean firstTime = true;
        for (Entry<Integer, String> entry : mCapabilities.entrySet()) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(", ");
            }
            sb.append(keyName(entry.getKey()));
            sb.append(":\"");
            sb.append(entry.getValue());
            sb.append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCapabilities.size());
        for (Entry<Integer, String> entry : mCapabilities.entrySet()) {
            dest.writeInt(entry.getKey().intValue());
            dest.writeString(entry.getValue());
        }
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public static final Creator<LinkCapabilities> CREATOR =
        new Creator<LinkCapabilities>() {
            public LinkCapabilities createFromParcel(Parcel in) {
                LinkCapabilities capabilities = new LinkCapabilities();
                int size = in.readInt();
                while (size-- != 0) {
                    int key = in.readInt();
                    String value = in.readString();
                    capabilities.mCapabilities.put(key, value);
                }
                return capabilities;
            }

            public LinkCapabilities[] newArray(int size) {
                return new LinkCapabilities[size];
            }
        };

    /**
     * Debug logging
     */
    protected static void log(String s) {
        Log.d(TAG, s);
    }

    /**
     * convert a Key integer to a String name
     */
    private static String keyName(int key) {
        switch (key) {
            case Key.RO_AVAILABLE_FWD_BW:
                return "RO_AVAILABLE_FWD_BW";
            case Key.RO_AVAILABLE_REV_BW:
                return "RO_AVAILABLE_REV_BW";
            case Key.RO_BOUND_INTERFACE:
                return "RO_BOUND_INTERFACE";
            case Key.RO_CURRENT_LATENCY:
                return "RO_CURRENT_LATENCY";
            case Key.RO_NETWORK_TYPE:
                return "RO_NETWORK_TYPE";
            case Key.RO_PHYSICAL_INTERFACE:
                return "RO_PHYSICAL_INTERFACE";
            case Key.RW_ALLOWED_NETWORKS:
                return "RW_ALLOWED_NETWORKS";
            case Key.RW_DESIRED_FWD_BW:
                return "RW_DESIRED_FWD_BW";
            case Key.RW_DESIRED_REV_BW:
                return "RW_DESIRED_REV_BW";
            case Key.RW_DISABLE_NOTIFICATIONS:
                return "RW_DISABLE_NOTIFICATIONS";
            case Key.RW_MAX_ALLOWED_LATENCY:
                return "RW_MAX_ALLOWED_LATENCY";
            case Key.RW_PROHIBITED_NETWORKS:
                return "RW_PROHIBITED_NETWORKS";
            case Key.RW_REQUIRED_FWD_BW:
                return "RW_REQUIRED_FWD_BW";
            case Key.RW_REQUIRED_REV_BW:
                return "RW_REQUIRED_REV_BW";
            case Key.RW_ROLE:
                return "RW_ROLE";
        }
        return "UNKNOWN_KEY";
    }
}
