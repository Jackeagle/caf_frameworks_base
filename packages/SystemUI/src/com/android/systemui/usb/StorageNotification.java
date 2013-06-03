/*
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
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Slog;
import android.util.Log;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import android.os.SystemProperties;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;

public class StorageNotification extends StorageEventListener {
    private static final String TAG = "StorageNotification";

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
    private Notification   mMediaStorageNotification;
    private Notification   mPhoneStorageNotification;
    //On /storage/sdcard1 default ,but if set "persist.sys.emmcsdcard.enabled" ,On /storage/sdcard0
    private boolean        mUmsStorageDependency = false;
    /*
     *it is very strange ,sometimes ,the media status may change from {nofs} to {unmount}
     *so add mMediaStorageNeedFormat here to avoid the nofs or damaged notification be covered
     *by {unmount} notification
     */
    private boolean        mMediaStorageNeedFormat  = false;
    private boolean        mPhoneStorageNeedFormat  = false;
    private boolean        mUmsAvailable;
    private StorageManager mStorageManager;

    private Handler        mAsyncEventHandler;

    public StorageNotification(Context context) {
        mContext = context;
        final String prop = SystemProperties.get("persist.sys.emmcsdcard.enabled");
        if(prop != null)
           if(prop.equals("true") || prop.equals("1"))
                //depend on /storage/sdcard0
                mUmsStorageDependency = true;
        Slog.d(TAG,String.format("set mUmsStorageDependency = %s", mUmsStorageDependency));
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        final boolean connected = mStorageManager.isUsbMassStorageConnected();
        Slog.d(TAG, String.format( "Startup with UMS connection %s (media state %s)", mUmsAvailable,
                Environment.getExternalStorageState()));
        
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
        String sti = Environment.getInternalStorageState();
        Slog.i(TAG, String.format("UMS connection(/storage/sdcard0) changed to %s (media state %s)",connected, st));
        Slog.i(TAG, String.format("UMS connection(/storage/sdcard1) changed to %s (media state %s)",connected, sti));
        if (connected && (st.equals(Environment.MEDIA_REMOVED) || st.equals(Environment.MEDIA_CHECKING)) && (sti.equals(Environment.MEDIA_REMOVED) || sti.equals(Environment.MEDIA_CHECKING))) {
            /*
             * No card or card being checked = don't display
             */
            connected = false;
        }
        // The state needn't to be updated if the storage state is shared & connected
        if (!(connected && (st.equals(Environment.MEDIA_SHARED) || (sti.equals(Environment.MEDIA_SHARED)))))
            updateUsbMassStorageNotification(mUmsStorageDependency,connected);
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


    private StorageVolume getStorageVolumebyPath(String path) {
       StorageVolume[] StorageVolume = mStorageManager.getVolumeList();
       if(StorageVolume == null){
            return null;
       }
       int count = StorageVolume.length;
       for (int i = 0; i < count; i++) {
            if(StorageVolume[i].getPath().equals(path)){
                 return StorageVolume[i];
            }
       }
       return null;
    }

    private boolean IsExternalStorageDeviceExist(){
       final String[] DEVICE_PATH  = {
          "/dev/block/mmcblk1p1",
          "/dev/block/mmcblk0p20"
       };
       final String prop = SystemProperties.get("persist.sys.emmcsdcard.enabled");
       if(prop == null)
           return false;
       File ExternalStorageDevice;
       if(prop.equals("true") || prop.equals("1"))
           ExternalStorageDevice = new File(DEVICE_PATH[1]);
       else
           ExternalStorageDevice = new File(DEVICE_PATH[0]);
       return ExternalStorageDevice.exists();
    }

    private void onStorageStateChangedAsync(String path, String oldState, String newState) {
        Log.e(TAG,"Media {" + path +"} state changed from { " + oldState + "} -> { " + newState + "}");
        StorageVolume volume = getStorageVolumebyPath(path);
        final boolean IsExternalStorage = Environment.getExternalStorageDirectory().toString().equals(path);
        if (newState.equals(Environment.MEDIA_SHARED)) {
            /*
             * If USB disconnected, should not show the notification.
             * Enable Phone's SD storage.
             */

            if(!UsbStorageActivity.mUsbIsConnected) {
                mStorageManager.disableUsbMassStorage();
                return;
            }
            if(!mUmsAvailable) {
                mStorageManager.disableUsbMassStorage();
                return;
            }
            /*
             * Storage is now shared. Modify the UMS notification
             * for stopping UMS.
             */
            if(IsExternalStorage == mUmsStorageDependency){
                 Intent intent = new Intent();

                 intent.setClass(mContext, com.android.systemui.usb.UsbStorageActivity.class);
                 PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
                 setUsbStorageNotification(
                      com.android.internal.R.string.usb_storage_stop_notification_title,
                      com.android.internal.R.string.usb_storage_stop_notification_message,
                      com.android.internal.R.drawable.stat_sys_warning, false, true, pi);
            }
            //clear the "Format" notification ,because the storage which is shared can't be formated.
            if((mMediaStorageNeedFormat && IsExternalStorage) || (!IsExternalStorage && mPhoneStorageNeedFormat))
                  setMediaStorageNotification(IsExternalStorage,0, 0, 0, false, false, null);

            Intent intent = new Intent();           
            intent.setClass(mContext, com.android.systemui.usb.UsbStorageActivity.class);				
            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
            setUsbStorageNotification(
                    com.android.internal.R.string.usb_storage_stop_notification_title,
                    com.android.internal.R.string.usb_storage_stop_notification_message,
                    com.android.internal.R.drawable.stat_sys_warning, false, true, pi);

        } else if (newState.equals(Environment.MEDIA_CHECKING)) {
       /*
        * case 1: if MEDIA_UNMOUNTABLE or MEDIA_NOFS before ,external storage skip checking here to avoid the notification
        * to be overwrited.because the external storage is removeable. so must check whether this card is removed
        * case 2: if MEDIA_UNMOUNTABLE or MEDIA_NOFS before ,internal storage just skip checking here ,because internal
        * storage is un-removable
        */
         if((IsExternalStorage && mMediaStorageNeedFormat && IsExternalStorageDeviceExist())
               || (!IsExternalStorage && mPhoneStorageNeedFormat))
                 return ;
            /*
             * Storage is now checking. Update media notification and disable
             * UMS notification.
             */
            setMediaStorageNotification(IsExternalStorage,
                    com.android.internal.R.string.ext_media_checking_notification_title,
                    com.android.internal.R.string.ext_media_checking_notification_message,
                    com.android.internal.R.drawable.stat_notify_sdcard_prepare, true, false, null);
            updateUsbMassStorageNotification(IsExternalStorage,false);
        } else if (newState.equals(Environment.MEDIA_NO_SPACE)) {
            //show AlertDialog to user
            AlertDialog NoSpaceDialog = new AlertDialog.Builder(mContext).setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(com.android.internal.R.string.primary_storage_no_space_title)
            .setMessage(com.android.internal.R.string.primary_storage_no_space_message)
            .setPositiveButton(com.android.internal.R.string.primary_storage_no_space_confirm, new DialogInterface.OnClickListener(){
                 public void onClick(DialogInterface dialog, int which) {
            }}).create();
            NoSpaceDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
            NoSpaceDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            WindowManager.LayoutParams lp = NoSpaceDialog.getWindow().getAttributes();
            //lp.alpha = 0.3f;
            lp.dimAmount = 0.5f;
            NoSpaceDialog.getWindow().setAttributes(lp);
            NoSpaceDialog.show();
        } else if (newState.equals(Environment.MEDIA_MOUNTED)) {
          /*
           *storage is mounted successfully ,so clear mMediaStorageNeedFormat or mPhoneStorageNeedFormat flags
           */
           if(IsExternalStorage)
               mMediaStorageNeedFormat = false;
           else
               mPhoneStorageNeedFormat = false;
            /*
             * Storage is now mounted. Dismiss any media notifications,
             * and enable UMS notification if connected.
             */
            setMediaStorageNotification(IsExternalStorage,0, 0, 0, false, false, null);
            updateUsbMassStorageNotification(IsExternalStorage,mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_UNMOUNTED)) {
            if((IsExternalStorage && mMediaStorageNeedFormat && IsExternalStorageDeviceExist())
               || (!IsExternalStorage && mPhoneStorageNeedFormat))
                    return ;
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
                    setMediaStorageNotification(IsExternalStorage,0, 0, 0, false, false, null);
                    updateUsbMassStorageNotification(IsExternalStorage,mUmsAvailable);
                } else {
                    /*
                     * Show safe to unmount media notification, and enable UMS
                     * notification if connected.
                     */
                       if (Environment.isExternalStorageRemovable() && IsExternalStorage) {
                             setMediaStorageNotification(IsExternalStorage,
                                com.android.internal.R.string.ext_media_safe_unmount_notification_title,
                                com.android.internal.R.string.ext_media_safe_unmount_notification_message,
                                com.android.internal.R.drawable.stat_notify_sdcard, true, true, null);
                    } else {
                        // This device does not have removable storage, so
                        // don't tell the user they can remove it.
                        setMediaStorageNotification(IsExternalStorage,0, 0, 0, false, false, null);
                    }
                    updateUsbMassStorageNotification(IsExternalStorage,mUmsAvailable);
                }
            } else {
                /*
                 * The unmount was due to UMS being enabled. Dismiss any
                 * media notifications, and disable the UMS notification
                 */
                setMediaStorageNotification(IsExternalStorage,0, 0, 0, false, false, null);
                updateUsbMassStorageNotification(IsExternalStorage,false);
            }
        } else if (newState.equals(Environment.MEDIA_NOFS)) {
            if(IsExternalStorage)
                mMediaStorageNeedFormat = true;
            else
                mPhoneStorageNeedFormat = true;
            /*
             * Storage has no filesystem. Show blank media notification,
             * and enable UMS notification if connected.
             */
            Intent intent = new Intent();
            if(IsExternalStorage)
                intent.setClass(mContext, com.android.internal.app.ExternalMediaFormatActivity.class);
            else
                intent.setClass(mContext, com.android.internal.app.PhoneMediaFormatActivity.class);
            if(volume != null){
                Log.e(TAG,"set volume" + volume.getPath() + "as variable");
                intent.putExtra(volume.EXTRA_STORAGE_VOLUME, volume);
            }
            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
            if(IsExternalStorage)
                setMediaStorageNotification(IsExternalStorage,
                    com.android.internal.R.string.ext_media_nofs_notification_title,
                    com.android.internal.R.string.ext_media_nofs_notification_message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi);
            else
                setMediaStorageNotification(IsExternalStorage,
                    com.android.internal.R.string.phone_media_nofs_notification_title,
                    com.android.internal.R.string.phone_media_nofs_notification_message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi);
            updateUsbMassStorageNotification(IsExternalStorage,mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_UNMOUNTABLE)) {
            if(IsExternalStorage)
                mMediaStorageNeedFormat = true;
            else
                mPhoneStorageNeedFormat = true;
            /*
             * Storage is corrupt. Show corrupt media notification,
             * and enable UMS notification if connected.
             */
            Intent intent = new Intent();
            if(IsExternalStorage)
                intent.setClass(mContext, com.android.internal.app.ExternalMediaFormatActivity.class);
            else
                /*
                 *I tried to reuse the code of ExternalMediaFormatActivity.java.
                 *When both two storage are dameged or nofs , The StorageNotification will send two PendingIntents
                 *to ExternalMediaFormatActivity, getintent() in ExternalMediaFormatActivity only get the first one
                 *I add PhoneMediaFormatActivity here to avoid this
                 */
                intent.setClass(mContext, com.android.internal.app.PhoneMediaFormatActivity.class);
            if(volume != null){
                Log.e(TAG,"set volume" + volume.getPath() + "as variable");
                intent.putExtra(volume.EXTRA_STORAGE_VOLUME, volume);
            }
            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
            if(IsExternalStorage)
                 setMediaStorageNotification(IsExternalStorage,
                    com.android.internal.R.string.ext_media_unmountable_notification_title,
                    com.android.internal.R.string.ext_media_unmountable_notification_message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi); 
            else
                 setMediaStorageNotification(IsExternalStorage,
                    com.android.internal.R.string.phone_media_unmountable_notification_title,
                    com.android.internal.R.string.phone_media_unmountable_notification_message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb, true, false, pi);
            updateUsbMassStorageNotification(IsExternalStorage,mUmsAvailable);
        } else if (newState.equals(Environment.MEDIA_REMOVED)) {
            if(IsExternalStorage)
                 mMediaStorageNeedFormat = false;
            else
                 mPhoneStorageNeedFormat = false;
            /*
             * Storage has been removed. Show nomedia media notification,
             * and disable UMS notification regardless of connection state.
             */
            setMediaStorageNotification(IsExternalStorage,
                    com.android.internal.R.string.ext_media_nomedia_notification_title,
                    com.android.internal.R.string.ext_media_nomedia_notification_message,
                    com.android.internal.R.drawable.stat_notify_sdcard_usb,
                    true, false, null);
            updateUsbMassStorageNotification(IsExternalStorage,false);
        } else if (newState.equals(Environment.MEDIA_BAD_REMOVAL)) {
          /*
           *the storage is removed ,so clear the flags whatever this situation it is.
           */
            if(IsExternalStorage)
                 mMediaStorageNeedFormat = false;
            else
                 mPhoneStorageNeedFormat = false;
            /*
             * Storage has been removed unsafely. Show bad removal media notification,
             * and disable UMS notification regardless of connection state.
             */
            setMediaStorageNotification(IsExternalStorage,
                    com.android.internal.R.string.ext_media_badremoval_notification_title,
                    com.android.internal.R.string.ext_media_badremoval_notification_message,
                    com.android.internal.R.drawable.stat_sys_warning,
                    true, true, null);
            updateUsbMassStorageNotification(IsExternalStorage,false);
        } else {
            Slog.w(TAG, String.format("Ignoring unknown state {%s}", newState));
        }
    }

    /**
     * Update the state of the USB mass storage notification
     */
    void updateUsbMassStorageNotification(boolean IsExternalStorage ,boolean available) {
        Slog.d(TAG,String.format("updateUsbMassStorageNotification  IsExternalStorage = %s ,available = %s",IsExternalStorage,available));
        //the UsbMassStorageNotification just depend on PhoneStorage ,because it is unremovable.
        if(IsExternalStorage != mUmsStorageDependency){
            Slog.d(TAG,String.format("skip this ums notification update"));
            return;
        }
        if (available) {

            /*
             * When user enable or disable USB debugging, there is
             * an UEvent sent from USB device with "USB_STATE",
             * UsbDeviceManager will updateState when receive this
             * UEvent and enter this method finally, this behavior
             * led to the notification on StatusBar will be written
             * back to UsbMassStorage disabled state every time
             * user enable/disable USB debugging. I think the UEvent
             * should not be received when user enable/disable USB
             * debugging but I can't modify the logic of send UEvent,
             * so just fix the notification issue here, send the
             * usb_storage_stop notification when UsbMassStorage is
             * enabled to make sure the notificaiton state is correct.
             */
            Intent intent = new Intent();
            intent.setClass(mContext, com.android.systemui.usb.UsbStorageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
            if ((mStorageManager != null) && mStorageManager.isUsbMassStorageEnabled()) {
                setUsbStorageNotification(
                    com.android.internal.R.string.usb_storage_stop_notification_title,
                    com.android.internal.R.string.usb_storage_stop_notification_message,
                    com.android.internal.R.drawable.stat_sys_warning,
                    false, true, pi);
            } else {
                setUsbStorageNotification(
                    com.android.internal.R.string.usb_storage_notification_title,
                    com.android.internal.R.string.usb_storage_notification_message,
                    com.android.internal.R.drawable.stat_sys_data_usb,
                    false, true, pi);
        	    }
			
        } else {
            setUsbStorageNotification(0, 0, 0, false, false, null);
        }
    }

    /**
     * Sets the USB storage notification.
     */
    private synchronized void setUsbStorageNotification(int titleId, int messageId, int icon,
            boolean sound, boolean visible, PendingIntent pi) {

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
		  mUsbStorageNotification.priority = Notification.PRIORITY_MIN;
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

    private Notification getMediaNotification(boolean IsExternalStorage){
        if(IsExternalStorage)
            return mMediaStorageNotification;
        else
            return mPhoneStorageNotification;
    }

    private void setMediaNotification(boolean IsExternalStorage,Notification notf){
        if(IsExternalStorage)
            mMediaStorageNotification = notf;
        else
            mPhoneStorageNotification = notf;
    }

    /**
     * Sets the media storage notification.
     */
    private synchronized void setMediaStorageNotification(boolean IsExternalStorage,int titleId, int messageId, int icon, boolean visible,
                                                          boolean dismissable, PendingIntent pi) {
        Log.e(TAG,"IsExternalStorage " + IsExternalStorage + " titleId " + titleId + " messageId " + messageId + " icon " + icon + " visible " + visible);
        if (!visible)
           if(getMediaNotification(IsExternalStorage) == null)
               return;

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        if (getMediaNotification(IsExternalStorage) != null && visible) {
           /*
            * Dismiss the previous notification - we're about to
            * re-use it.
            */
            int notificationId;
            if(IsExternalStorage)
                notificationId = getMediaNotification(IsExternalStorage).icon;
            else
                notificationId = getMediaNotification(IsExternalStorage).icon + 1;
            Log.e(TAG,"notificationManager.cancel notificationId =" + notificationId);
            notificationManager.cancel(notificationId);
        }

        if (visible) {
            Resources r = Resources.getSystem();
            CharSequence title = r.getText(titleId);
            CharSequence message = r.getText(messageId);
            if (getMediaNotification(IsExternalStorage) == null) {
               Log.e(TAG,"mMediaStorageNotification is null ,new one");
               setMediaNotification(IsExternalStorage,new Notification());
               getMediaNotification(IsExternalStorage).when = 0;
            }
            getMediaNotification(IsExternalStorage).defaults &= ~Notification.DEFAULT_SOUND;

            if (dismissable) {
                getMediaNotification(IsExternalStorage).flags = Notification.FLAG_AUTO_CANCEL;
            } else {
                getMediaNotification(IsExternalStorage).flags = Notification.FLAG_ONGOING_EVENT;
            }
            getMediaNotification(IsExternalStorage).tickerText = title;
            if (pi == null) {
                Intent intent = new Intent();
                pi = PendingIntent.getBroadcastAsUser(mContext, 0, intent, 0,
                        UserHandle.CURRENT);
            }

            getMediaNotification(IsExternalStorage).icon = icon;
            getMediaNotification(IsExternalStorage).setLatestEventInfo(mContext, title, message, pi);
            Log.e(TAG,"title = "+ title);
            Log.e(TAG,"message = "+ message);
        }

        int notificationId;
        if(IsExternalStorage)
           notificationId =  getMediaNotification(IsExternalStorage).icon;
        else
           notificationId =  getMediaNotification(IsExternalStorage).icon + 1;
        if (visible) {
            notificationManager.notifyAsUser(null, notificationId,
                    getMediaNotification(IsExternalStorage), UserHandle.ALL);
        } else {
            notificationManager.cancelAsUser(null, notificationId, UserHandle.ALL);
        }
    }
}
