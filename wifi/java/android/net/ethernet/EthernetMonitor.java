/*
 * Author: Matthew Ranostay <Matt_Ranostay@mentor.com>
 * Copyright (C) 2009 Mentor Graphics
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

package android.net.ethernet;

import android.util.Log;
import android.util.Config;
import android.content.Context;

import android.app.Notification;
import android.app.NotificationManager;

/**
 * Listens for events on the ethernet device. Runs in its own thread.
 *
 * @hide
 */
public class EthernetMonitor {

    private static final boolean DBG = false;
    private static final String TAG = "EthernetMonitor";

    /**
     * The NotificationManager object.
     */
    private NotificationManager mNotificationManager;

    /**
     * Keep track of Context object.
     */
    private static Context mContext;

    /**
      * The icons to show a connected ethernet connection states.
      */
    private static final int ICON_ETHERNET_DHCP_CONNECTED =
            com.android.internal.R.drawable.connect_established;

    private static final int ICON_ETHERNET_UP_CONNECTED =
            com.android.internal.R.drawable.connect_creating;

    /**
      * The icon to show a disconnected ethernet connection.
      */
    private static final int ICON_ETHERNET_DISCONNECTED =
            com.android.internal.R.drawable.connect_no;


    public EthernetMonitor(Context context) {
        mContext = context;
    }

    public void startMonitoring() {
        mNotificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

        new MonitorThread().start();
    }

    class MonitorThread extends Thread {
        public MonitorThread() {
            super("EthernetMonitor");
        }

        public void run() {
            /**
             * Keep last hash value of message to be sure
             * we don't double report a message.
             */
            int message_hash = 0;

            for (;;) {
                boolean status = EthernetNative.getEthernetStatus();
                int icon;
                CharSequence message;

                /**
                 * Poll every 500 milliseconds
                 */
                nap(500);

                if (status) {
                    String address = EthernetNative.getEthernetAddress();

                    if (DBG)
                        Log.i(TAG, "Ethernet device link up");

                    if (address.equals("0.0.0.0")) {
                        icon = ICON_ETHERNET_UP_CONNECTED;
                        message = "Ethernet connection is up without an IP address";
                    } else {
                        icon = ICON_ETHERNET_DHCP_CONNECTED;
                        message = "Ethernet connection is up at '" + address + "'";
                    }
                } else {
                    if (DBG)
                        Log.i(TAG, "Ethernet device link down");

                    icon = ICON_ETHERNET_DISCONNECTED;
                    message = "Ethernet connection is down";
                }

                Notification n = new Notification(mContext,
                        icon, null,
                        System.currentTimeMillis(),
                        message, "", null);

                if (message_hash == n.hashCode())
                    continue;

                mNotificationManager.notify(1, n);

                message_hash = n.hashCode();
            }
        }

        /**
         * Sleep for a period of time.
         * @param secs the number of millseconds to sleep
         */
        private void nap(int millisecs) {
            try {
               Thread.sleep(millisecs);
            } catch (InterruptedException ignore) {
            }
        }
    }
}
