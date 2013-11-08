/*
 * Copyright (c) 2013, The Linux Foundation. All Rights Reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2010 Google Inc.
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

package com.android.systemui.usb;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.hardware.usb.UsbManager;

import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Slog;
import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class StorageNotification extends StorageEventListener {
    private static final String TAG = "StorageNotification";
    private static final boolean DEBUG = false;

    private static final boolean POP_UMS_ACTIVITY_ON_CONNECT = true;

    /**
     * Binder context for this service
     */
    private Context mContext;
    
    /**
     * The notification that is shown when a USB mass storage host
     * is connected. 
     * <p>
     * This is lazily created, so use {@link #setUsbStorageNotification()}.
     */
    private Notification mUsbStorageNotification;

    /**
     * The notification that is shown when the following media events occur:
     *     - Media is being checked
     *     - Media is blank (or unknown filesystem)
     *     - Media is corrupt
     *     - Media is safe to unmount
     *     - Media is missing
     * <p>
     * This is lazily created, so use {@link #setMediaStorageNotification()}.
     */
    /*
     *Now we have two storages, one is internal dedicated FAT32 partition
     *the other is external SD card.
     *mMediaStorageNotification for primary storage
     *mSecondaryStorageNotification for secondary storage
     */
    private Notification   mMediaStorageNotification;
    private Notification   mPhoneStorageNotification;
    private boolean        mUmsAvailable;
    /*
     *It's very strange. sometimes, the media state change from {nofs} to {unmount}
     *this will cause the Notification be clear.
     *Add two flags to avoid the nofs or damaged notification be covered by
     *{unmount} state
     */
    private boolean        mMediaStorageNeedFormat  = false;
    private boolean        mSecondaryStorageNeedFormat = false;
    private StorageManager mStorageManager;

    private Handler        mAsyncEventHandler;

    public StorageNotification(Context context) {
        mContext = context;

        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        final boolean connected = mStorageManager.isUsbMassStorageConnected();
        if (DEBUG) Slog.d(TAG, String.format( "Startup with UMS connection %s (media state %s)",
                mUmsAvailable, Environment.getExternalStorageState()));

        HandlerThread thr = new HandlerThread("SystemUI StorageNotification");
        thr.start();
        mAsyncEventHandler = new Handler(thr.getLooper());

        onUsbMassStorageConnectionChanged(connected);
    }

    /*
     * @override com.android.os.storage.StorageEventListener
     */
    @Override
    public void onUsbMassStorageConnectionChanged(final boolean connected) {
        mAsyncEventHandler.post(new Runnable() {
            @Override
            public void run() {
                onUsbMassStorageConnectionChangedAsync(connected);
            }
        });
    }

    private void onUsbMassStorageConnectionChangedAsync(boolean connected) {
        mUmsAvailable = connected;
        /*
         * Even though we may have a UMS host connected, we the SD card
         * may not be in a state for export.
         */
        String st = Environment.getExternalStorageState();

        if (DEBUG) Slog.i(TAG, String.format("UMS connection changed to %s (media state %s)",
                connected, st));

        if (connected && (st.equals(
                Environment.MEDIA_REMOVED) || st.equals(Environment.MEDIA_CHECKING))) {
            /*
             * No card or card being checked = don't display
             */
            connected = false;
        }
        updateUsbMassStorageNotification(connected);
    }

    /*
     * @override com.android.os.storage.StorageEventListener
     */
    @Override
    public void onStorageStateChanged(final String path, final String oldState, final String newState) {
        mAsyncEventHandler.post(new Runnable() {
            @Override
            public void run() {
                onStorageStateChangedAsync(path, oldState, newState);
            }
        });
    }

    private boolean isExternalStorageDeviceExist() {
        final File externalStorageDevice = new File("/dev/block/mmcblk1");
        return externalStorageDevice.exists();
    }

    private StorageVolume getStorageVolumebyPath(String path) {
        StorageVolume[] StorageVolume = mStorageManager.getVolumeList();
        if (StorageVolume == null)
            return null;

        int count = StorageVolume.length;
        for (int i = 0; i < count; i++) {
            if (StorageVolume[i].getPath().equals(path)){
                return StorageVolume[i];
            }
        }
        return null;
    }

    private void updateFormatFlag(boolean isExternalStorage, boolean state) {
        if (isExternalStorage)
            mSecondaryStorageNeedFormat = state;
        else
            mMediaStorageNeedFormat = state;
    }

    private void onStorageStateChangedAsync(String path, String oldState, String newState) {
        if (DEBUG) Slog.i(TAG, String.format(
                "Media {%s} state changed from {%s} -> {%s}", path, oldState, newState));
        StorageVolume volume = getStorageVolumebyPath(path);
        final boolean isExternalStorage =
                         !Environment.getExternalStorageDirectory().toString().equals(path);

        if (newState.equals(Environment.MEDIA_SHARED)) {
            String usbMode = new UsbManager(null, null).getDefaultFunction();
            final boolean isUmsMode = UsbManager.USB_FUNCTION_MASS_STORAGE.equals(usbMode);
            if (!isUmsMode )
                mStorageManager.disableUsbMassStorage();
            /*
             * Storage is now shared. Modify the UMS notification
             * for stopping UMS.
             */
            Intent intent = new Intent();
            intent.setClass(mContext, com.android.systemui.usb.UsbStorageActivity.class);
            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
            setUsbStorageNotification(
                    com.android.internal.R.string.usb_storage_stop_notification_title,
                    com.android.internal.R.string.usb_storage_stop_notification_message,
                    com.android.internal.R.drawable.stat_sys_warning, false, true, pi);
            /* Clear Notification when enable UMS */
            setMediaStorageNotification(isExternalStorage, 0, 0, 0, false, false, null);
        } else if (newState.equals(Environment.MEDIA_CHECKING)) {
            /*
             *if MEDIA_UNMOUNTABLE or MEDIA_NOFS before.skip checking Notification here to
             *avoid the format notification being covered
             */
            if ((mMediaStorageNeedFormat && !isExternalStorage)
                || (isExternalStorage && mSecondaryStorageNeedFormat
                && isExternalStorageDeviceExist()))
                 return;
            /*
             * Storage is now checking. Update media notification and disable
             * UMS notification.
             */
            if (isExternalStorage)
                setMediaStorageNotification(isExternalStorage,
                        com.android.internal.R.string.ext_media_checking_notification_title,
                        com.android.internal.R.string.ext_media_checking_notification_message,
                        com.android.internal.R.drawable.stat_notify_sdcard_prepare,
                        true, false, null);
            else
                setMediaStorageNotification(isExternalStorage,
                        com.android.internal.R.string.phone_media_checking_notification_title,
                        com.android.internal.R.string.phone_media_checking_notification_message,
                        com.android.internal.R.drawable.stat_notify_sdcard_prepare,
                        true, false, null);
            updateUsbMassStorageNotification(false);
        } else if (newState.equals(Environment.MEDIA_MOUNTED)) {
            /*
             * Clear format flag when storage is mounted successfully.
             */
            updateFormatFlag(isExternalStorage, false);

            /*
             * Storage is now mounted. Dismiss any media notifications,
             * and enable UMS notification if connected.
             */
            setMediaStorageNotification(isExternalStorage, 0, 0, 0, false, false, null);
            updateUsbMassStorageNotification(mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_UNMOUNTED)) {
            if ((mMediaStorageNeedFormat && !isExternalStorage)
                || (isExternalStorage && mSecondaryStorageNeedFormat
                && isExternalStorageDeviceExist()))
                 return;
            /*
             * Storage is now unmounted. We may have been unmounted
             * because the user is enabling/disabling UMS, in which case we don't
             * want to display the 'safe to unmount' notification.
             */
            if (!mStorageManager.isUsbMassStorageEnabled()) {
                if (oldState.equals(Environment.MEDIA_SHARED)) {
                    /*
                     * The unmount was due to UMS being enabled. Dismiss any
                     * media notifications, and enable UMS notification if connected
                     */
                    setMediaStorageNotification(isExternalStorage, 0, 0, 0, false, false, null);
                    updateUsbMassStorageNotification(mUmsAvailable);
                } else {
                    /*
                     * Show safe to unmount media notification, and enable UMS
                     * notification if connected.
                     */
                    if (getStorageVolumebyPath(path).isRemovable()) {
                        setMediaStorageNotification(isExternalStorage,
                                com.android.internal.R.string.ext_media_safe_unmount_notification_title,
                                com.android.internal.R.string.ext_media_safe_unmount_notification_message,
                                com.android.internal.R.drawable.stat_notify_sdcard, true, true, null);
                    } else {
                        // This device does not have removable storage, so
                        // don't tell the user they can remove it.
                        setMediaStorageNotification(isExternalStorage, 0, 0, 0, false, false, null);
                    }
                    updateUsbMassStorageNotification(mUmsAvailable);
                }
            } else {
                /*
                 * The unmount was due to UMS being enabled. Dismiss any
                 * media notifications, and disable the UMS notification
                 */
                setMediaStorageNotification(isExternalStorage, 0, 0, 0, false, false, null);
                updateUsbMassStorageNotification(false);
            }
        } else if (newState.equals(Environment.MEDIA_NOFS)) {
            updateFormatFlag(isExternalStorage, true);
            /*
             * Storage has no filesystem. Show blank media notification,
             * and enable UMS notification if connected.
             */
            Intent intent = new Intent();
            if (isExternalStorage)
                intent.setClass(mContext, com.android.internal.app.ExternalMediaFormatActivity.class);
            else
                intent.setClass(mContext, com.android.internal.app.PhoneMediaFormatActivity.class);

            if (volume != null)
                intent.putExtra(volume.EXTRA_STORAGE_VOLUME, volume);

            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);

            if (isExternalStorage)
                setMediaStorageNotification(isExternalStorage,
                        com.android.internal.R.string.ext_media_nofs_notification_title,
                        com.android.internal.R.string.ext_media_nofs_notification_message,
                        com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi);
            else
                setMediaStorageNotification(isExternalStorage,
                        com.android.internal.R.string.phone_media_nofs_notification_title,
                        com.android.internal.R.string.phone_media_nofs_notification_message,
                        com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi);
            updateUsbMassStorageNotification(mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_UNMOUNTABLE)) {
            updateFormatFlag(isExternalStorage, true);
            /*
             * Storage is corrupt. Show corrupt media notification,
             * and enable UMS notification if connected.
             */
            Intent intent = new Intent();
            if (isExternalStorage)
                intent.setClass(mContext, com.android.internal.app.ExternalMediaFormatActivity.class);
            else
                intent.setClass(mContext, com.android.internal.app.PhoneMediaFormatActivity.class);

            if (volume != null)
                intent.putExtra(volume.EXTRA_STORAGE_VOLUME, volume);

            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);

            if (isExternalStorage)
                setMediaStorageNotification(isExternalStorage,
                        com.android.internal.R.string.ext_media_unmountable_notification_title,
                        com.android.internal.R.string.ext_media_unmountable_notification_message,
                        com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi);
            else
                setMediaStorageNotification(isExternalStorage,
                        com.android.internal.R.string.phone_media_unmountable_notification_title,
                        com.android.internal.R.string.phone_media_unmountable_notification_message,
                        com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi);
            updateUsbMassStorageNotification(mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_REMOVED)) {
            updateFormatFlag(isExternalStorage, false);
            /*
             * Storage has been removed. Show nomedia media notification,
             * and disable UMS notification regardless of connection state.
             */
            setMediaStorageNotification(isExternalStorage,
                    com.android.internal.R.string.ext_media_nomedia_notification_title,
                    com.android.internal.R.string.ext_media_nomedia_notification_message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb,
                    true, false, null);
            updateUsbMassStorageNotification(false);
        } else if (newState.equals(Environment.MEDIA_BAD_REMOVAL)) {
            updateFormatFlag(isExternalStorage, false);
            /*
             * Storage has been removed unsafely. Show bad removal media notification,
             * and disable UMS notification regardless of connection state.
             */
            setMediaStorageNotification(isExternalStorage,
                    com.android.internal.R.string.ext_media_badremoval_notification_title,
                    com.android.internal.R.string.ext_media_badremoval_notification_message,
                    com.android.internal.R.drawable.stat_sys_warning,
                    true, true, null);
            updateUsbMassStorageNotification(false);
        } else {
            Slog.w(TAG, String.format("Ignoring unknown state {%s}", newState));
        }
    }

    /**
     * Update the state of the USB mass storage notification
     */
    void updateUsbMassStorageNotification(boolean available) {

        if (available) {
            Intent intent = new Intent();
            intent.setClass(mContext, com.android.systemui.usb.UsbStorageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
            setUsbStorageNotification(
                    com.android.internal.R.string.usb_storage_notification_title,
                    com.android.internal.R.string.usb_storage_notification_message,
                    com.android.internal.R.drawable.stat_sys_data_usb,
                    false, true, pi);
        } else {
            setUsbStorageNotification(0, 0, 0, false, false, null);
        }
    }

    /**
     * Sets the USB storage notification.
     */
    private synchronized void setUsbStorageNotification(int titleId, int messageId, int icon,
            boolean sound, boolean visible, PendingIntent pi) {
        // force to show UsbSettings screen to select usb mode if property it true
        if (SystemProperties.getBoolean("persist.env.settings.multiusb", true)) {
            titleId = 0;
            messageId = 0;
            icon = 0;
            sound = false;
            visible = false;
            pi = null;
        }

        if (!visible && mUsbStorageNotification == null) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }
        
        if (visible) {
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(titleId);
            CharSequence message = r.getText(messageId);

            if (mUsbStorageNotification == null) {
                mUsbStorageNotification = new Notification();
                mUsbStorageNotification.icon = icon;
                mUsbStorageNotification.when = 0;
            }

            if (sound) {
                mUsbStorageNotification.defaults |= Notification.DEFAULT_SOUND;
            } else {
                mUsbStorageNotification.defaults &= ~Notification.DEFAULT_SOUND;
            }
                
            mUsbStorageNotification.flags = Notification.FLAG_ONGOING_EVENT;

            mUsbStorageNotification.tickerText = title;
            if (pi == null) {
                Intent intent = new Intent();
                pi = PendingIntent.getBroadcastAsUser(mContext, 0, intent, 0,
                        UserHandle.CURRENT);
            }

            mUsbStorageNotification.setLatestEventInfo(mContext, title, message, pi);
            final boolean adbOn = 1 == Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED,
                0);

            if (POP_UMS_ACTIVITY_ON_CONNECT && !adbOn) {
                // Pop up a full-screen alert to coach the user through enabling UMS. The average
                // user has attached the device to USB either to charge the phone (in which case
                // this is harmless) or transfer files, and in the latter case this alert saves
                // several steps (as well as subtly indicates that you shouldn't mix UMS with other
                // activities on the device).
                //
                // If ADB is enabled, however, we suppress this dialog (under the assumption that a
                // developer (a) knows how to enable UMS, and (b) is probably using USB to install
                // builds or use adb commands.
                mUsbStorageNotification.fullScreenIntent = pi;
            }
        }
    
        final int notificationId = mUsbStorageNotification.icon;
        if (visible) {
            notificationManager.notifyAsUser(null, notificationId, mUsbStorageNotification,
                    UserHandle.ALL);
        } else {
            notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
        }
    }

    private synchronized boolean getMediaStorageNotificationDismissable() {
        if ((mMediaStorageNotification != null) &&
            ((mMediaStorageNotification.flags & Notification.FLAG_AUTO_CANCEL) ==
                    Notification.FLAG_AUTO_CANCEL))
            return true;

        return false;
    }

    private Notification getMediaNotification(boolean isExternalStorage) {
        if (isExternalStorage)
            return mMediaStorageNotification;
        else
            return mPhoneStorageNotification;
    }

    private  void setMediaNotification(boolean isExternalStorage,Notification notf) {
        if (isExternalStorage)
            mMediaStorageNotification = notf;
        else
            mPhoneStorageNotification = notf;
    }

    /**
     * Sets the media storage notification.
     */
    private synchronized void setMediaStorageNotification(boolean isExternalStorage, int titleId,
                int messageId,int icon, boolean visible, boolean dismissable, PendingIntent pi) {
        if (DEBUG)
            Slog.i(TAG, String.format("isExternalStorage %s, titleId %s, messageId %s,icon %s, visible %s",
                  isExternalStorage, titleId, messageId, icon, visible));
        Notification storageNotification = getMediaNotification(isExternalStorage);

        if (!visible && storageNotification  == null)
                return;

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        if (storageNotification != null && visible) {
            /*
             * Dismiss the previous notification - we're about to
             * re-use it.
             */
            int notificationId;
            if (isExternalStorage)
                notificationId = storageNotification.icon;
            else
                notificationId = storageNotification.icon + 1;

            if (DEBUG)
                Slog.i(TAG, String.format("notificationManager.cancel notificationId = %s",
                                             notificationId));
            notificationManager.cancel(notificationId);
        }
        
        if (visible) {
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(titleId);
            CharSequence message = r.getText(messageId);

            if (storageNotification == null) {
                storageNotification = new Notification();
                storageNotification.when = 0;
                setMediaNotification(isExternalStorage, storageNotification);
            }

            storageNotification.defaults &= ~Notification.DEFAULT_SOUND;

            if (dismissable) {
                storageNotification.flags = Notification.FLAG_AUTO_CANCEL;
            } else {
                storageNotification.flags = Notification.FLAG_ONGOING_EVENT;
            }

            storageNotification.tickerText = title;
            if (pi == null) {
                Intent intent = new Intent();
                pi = PendingIntent.getBroadcastAsUser(mContext, 0, intent, 0,
                        UserHandle.CURRENT);
            }

            storageNotification.icon = icon;
            storageNotification.setLatestEventInfo(mContext, title, message, pi);
            if (DEBUG)
                Slog.i(TAG, String.format("title =  %s, message = %s", title, message));
        }

        int notificationId;
        if (isExternalStorage)
            notificationId =  storageNotification.icon;
        else
            notificationId =  storageNotification.icon + 1;

        if (visible) {
            if (DEBUG)
                Slog.i(TAG, String.format("notificationId = %s", notificationId));
            notificationManager.notifyAsUser(null, notificationId,
                    storageNotification, UserHandle.ALL);
        } else {
            notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
        }
    }
}
