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

    private static final boolean DBG = true;
    private static final String TAG = "EthernetMonitor";

    /**
     * The NotificationManager object.
     */
    private NotificationManager mNotificationManager;

    /**
     * Keep track of the last state of the ethernet device.
     */
    private static boolean last_state;

    /**
     * Keep track of Context object.
     */
    private static Context mContext;

    /**
      * The icon to show a connected ethernet connection.
      */
    private static final int ICON_ETHERNET_CONNECTED =
            com.android.internal.R.drawable.star_big_on;

    /**
      * The icon to show a disconnected ethernet connection.
      */
    private static final int ICON_ETHERNET_DISCONNECTED =
            com.android.internal.R.drawable.star_big_off;


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
            last_state = false;

            for (;;) {
                boolean status = EthernetNative.getEthernetStatus();
                int icon;
                CharSequence message;

                nap(1);

                /**
                  * Don't notify if userspace already knows state
                  */
                if (last_state == status) {
                    continue;
                }

                if (status) {
                    String address = EthernetNative.getEthernetAddress();
                    if (DBG)
                        Log.i(TAG, "Ethernet device link up");

                    icon = ICON_ETHERNET_CONNECTED;
                    message = "Ethernet connection is up at '" + address + "'";
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

                mNotificationManager.notify(1, n);

                last_state = status;
            }
        }

        /**
         * Sleep for a period of time.
         * @param secs the number of seconds to sleep
         */
        private void nap(int secs) {
            try {
               Thread.sleep(secs * 1000);
            } catch (InterruptedException ignore) {
            }
        }
    }
}
