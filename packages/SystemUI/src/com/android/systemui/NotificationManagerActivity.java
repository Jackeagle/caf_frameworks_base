/*
 * BORQS Software Solutions Pvt Ltd. CONFIDENTIAL
 * Copyright (c) 2014 All rights reserved.
 *
 * The source code contained or described herein and all documents
 * related to the source code ("Material") are owned by BORQS Software
 * Solutions Pvt Ltd. No part of the Material may be used,copied,
 * reproduced, modified, published, uploaded,posted, transmitted,
 * distributed, or disclosed in any way without BORQS Software
 * Solutions Pvt Ltd. prior written permission.
 *
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by BORQS Software Solutions Pvt Ltd. in writing.
 *
 */

package com.android.systemui;

import java.util.ArrayList;
import java.util.Comparator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.os.ServiceManager;
import android.service.notification.INotificationListener;
import android.app.INotificationManager;
import android.os.RemoteException;
import android.content.ComponentName;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.app.ActivityManager;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.view.Gravity;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;

/* NotificationManagerActivity is used to show All Active Notifications
 which are present in the StatusBar */

public class NotificationManagerActivity extends Activity {

    static final String TAG = "NotificationManagerActivity";
    private PackageManager mPm;
    private INotificationManager mNoMan;
    static final boolean debug = false;
    private Context mContext;
    private NotificationHistoryAdapter mAdapter;
    private ListView listView;
    private TextView tx;
    private IStatusBarService mBarService;
    private HistoricalNotificationInfo notifyInfo;
    private boolean mAddClearAll;
    private  int mlistsize = 0;
    private  static String  APPLICATION_USB = "android";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Android actionbar icon/homebutton should not be shown for this
        // acitivity
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        setContentView(R.layout.notifications);
        mContext = this;
        mPm = mContext.getPackageManager();
        // NotificationManager Object created to get all Active notifications
        mNoMan = INotificationManager.Stub.asInterface(ServiceManager
                .getService(Context.NOTIFICATION_SERVICE));
        // Statusbarservice object to be called while clearing a/all
        // notification
        mBarService = IStatusBarService.Stub.asInterface(ServiceManager
                .getService(Context.STATUS_BAR_SERVICE));
        try {
            // register for a listener for which we get onNotificationPosted and
            // onnotification removed callback
            if (mNoMan != null)
                mNoMan.registerListener(mListener,
                        new ComponentName(mContext.getPackageName(), this
                                .getClass().getCanonicalName()), 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        listView = (ListView) findViewById(R.id.notifyList);
        tx = (TextView) findViewById(R.id.notification);
        // Construct the adapter to fill the ListView
        mAdapter = new NotificationHistoryAdapter(mContext);
        listView.setAdapter(mAdapter);
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        // Refresh the active notifications List whenever we resume the activity
        refreshList();

    }

    private Runnable mRefreshListRunnable = new Runnable() {
        @Override
        public void run() {
            refreshList();
        }
    };

    private INotificationListener.Stub mListener = new INotificationListener.Stub() {
        // Callbacks to listen to post of data on the notification
        @Override
        public void onNotificationPosted(StatusBarNotification notification)
                throws RemoteException {
            if(debug)
            Log.d(TAG, "onNotificationPosted: " + notification);

            if (listView != null) {
                final Handler h = listView.getHandler();
                h.removeCallbacks(mRefreshListRunnable);
                h.postDelayed(mRefreshListRunnable, 100);
            }
        }

        // Callbacks to listen to remove of data on the notification
        @Override
        public void onNotificationRemoved(StatusBarNotification notification)
                throws RemoteException {
            if(debug)
            Log.d(TAG,
                    "======================================= onnotificationremoved");

            if (listView != null) {
                // Post to a runnable to Refresh the data
                final Handler h = listView.getHandler();
                h.removeCallbacks(mRefreshListRunnable);
                h.postDelayed(mRefreshListRunnable, 100);

            }
        }
    };

    // Sort using timestamp as Priority
    private final Comparator<HistoricalNotificationInfo> mNotificationSorter = new
                            Comparator<HistoricalNotificationInfo>() {
        @Override
        public int compare(HistoricalNotificationInfo lhs,
                HistoricalNotificationInfo rhs) {
            return (int) (rhs.timestamp - lhs.timestamp);
        }
    };

    private void refreshList() {

        List<HistoricalNotificationInfo> infos = loadNotifications();
        if (infos != null) {
            if(debug)
            Log.d(TAG, "=========================  notification available"
                    + infos.size());

            if (infos.size() == 0) {
                mlistsize = 0;
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();
                tx.setVisibility(View.VISIBLE);
                tx.setText(R.string.no_notification);
                tx.setGravity(Gravity.CENTER_VERTICAL
                        | Gravity.CENTER_HORIZONTAL);
                return;
            }
            // when new notification arrives we refresh the list. if menu option
            // is open close the list
            if(mlistsize !=  infos.size())
            ((Activity) mContext).closeOptionsMenu();
            tx.setVisibility(View.GONE);
            mAdapter.clear();
            mAdapter.addAll(infos);
            mAdapter.sort(mNotificationSorter);
            mAdapter.notifyDataSetChanged();
            mlistsize = infos.size();
        } else {
            if(debug)
            Log.d(TAG, "=========================  notification not available");
            mlistsize = 0;
            tx.setText(R.string.no_notification);
            tx.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        }

    }

    private Drawable loadPackageIconDrawable(String pkg, int userId) {
        Drawable icon = null;
        try {
            icon = mPm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return icon;
    }

    private Resources getResourcesForUserPackage(String pkg, int userId) {
        Resources r = null;

        if (pkg != null) {
            try {
                if (userId == UserHandle.USER_ALL) {
                    userId = UserHandle.USER_OWNER;
                }
                r = mPm.getResourcesForApplicationAsUser(pkg, userId);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e(TAG, "Icon package not found: " + pkg);
                return null;
            }
        } else {
            r = mContext.getResources();
        }
        return r;
    }

    private CharSequence loadPackageName(String pkg) {
        try {
            ApplicationInfo info = mPm.getApplicationInfo(pkg,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            if (info != null)
                return mPm.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pkg;
    }

    private Drawable loadIconDrawable(String pkg, int userId, int resId) {
        Resources r = getResourcesForUserPackage(pkg, userId);

        if (resId == 0) {
            return null;
        }

        try {
            return r.getDrawable(resId);
        } catch (RuntimeException e) {
            Log.w(TAG,
                    "Icon not found in " + (pkg != null ? resId : "<system>")
                    + ": " + Integer.toHexString(resId));
        }

        return null;
    }

    private static class HistoricalNotificationInfo {
        public String pkg;
        public Drawable pkgicon;
        public CharSequence subtitle;
        public Drawable icon;
        public CharSequence title;
        public int priority;
        public int user;
        public long timestamp;
        public boolean active;
        public Notification notification;
        public boolean clearflag;
        public String tag;
        public int Id;
    }

    private List<HistoricalNotificationInfo> loadNotifications() {
        mAddClearAll = false;
        final int currentUserId = ActivityManager.getCurrentUser();
        try {
            StatusBarNotification[] active = mNoMan
                    .getActiveNotifications(mContext.getPackageName());

            List<HistoricalNotificationInfo> list = new ArrayList<HistoricalNotificationInfo>(
                    active.length);

            for (StatusBarNotification sbn : active) {
                final HistoricalNotificationInfo info = new HistoricalNotificationInfo();
                info.pkg = sbn.getPackageName();
                info.clearflag = sbn.isClearable();
                // if any of the notification can be cleared. Assume we can add
                // "clear all" menu.
                if (!mAddClearAll) {
                    if (info.clearflag) {
                        mAddClearAll = true;
                    }
                }
                info.tag = sbn.getTag();
                info.Id = sbn.getId();
                info.user = sbn.getUserId();
                info.icon = loadIconDrawable(info.pkg, info.user,
                        sbn.getNotification().icon);
                info.pkgicon = loadPackageIconDrawable(info.pkg, info.user);
                if (sbn.getNotification() != null) {
                    info.notification = sbn.getNotification();
                    if (sbn.getNotification().extras != null) {
                        info.subtitle = sbn.getNotification().extras
                                .getString(Notification.EXTRA_TEXT);
                        info.title = sbn.getNotification().extras
                                .getString(Notification.EXTRA_TITLE);
                        if (info.title == null || "".equals(info.title)) {
                            info.title = sbn.getNotification().extras
                                    .getString(Notification.EXTRA_TEXT);
                        }
                    }
                    if (info.title == null || "".equals(info.title)) {
                        if(sbn.getNotification().tickerText !=null &&
                                        !(sbn.getNotification().tickerText.equals(""))){
                            info.title = sbn.getNotification().tickerText;
                        }else{
                            info.title = info.subtitle;
                        }
                    }
                    if(debug) {
                    Log.d(TAG,
                            "================ title========"
                                    + sbn.getNotification().extras
                                    .getString(Notification.EXTRA_TEXT));
                    Log.d(TAG,
                            "================ title========"
                                    + sbn.getNotification().extras
                                    .getString(Notification.EXTRA_TITLE));
                    Log.d(TAG,
                            "================ title========"
                                    + sbn.getNotification().extras
                                    .getString(Notification.EXTRA_INFO_TEXT));
                    Log.d(TAG,
                            "================ title========"
                                    + sbn.getNotification().extras
                                    .getString(Notification.EXTRA_SUB_TEXT));
                    }
                    info.priority = sbn.getNotification().priority;
                    if( info.pkg.equals(APPLICATION_USB)) {
                        info.timestamp = sbn.getPostTime();
                    } else {
                        info.timestamp = sbn.getNotification().when;
                    }
                    Log.d(TAG, info.timestamp + " " + info.pkg + " "
                            + info.title);
                }
                if ((info.title != null && info.title.length() != 0)
                        || (info.subtitle != null && info.subtitle.length() != 0))
                    list.add(info);
            }
            return list;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class NotificationHistoryAdapter extends
    ArrayAdapter<HistoricalNotificationInfo> {
        private final LayoutInflater mInflater;

        public NotificationHistoryAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final HistoricalNotificationInfo info = getItem(position);
            if(debug) {
            Log.d(TAG,
                    "=======================================NotificationHistoryAdapter"
                            + info);
            Log.d(TAG, " info.pkg , info.title" + info.pkg + " " + info.title);
            }
            final View row = convertView != null ? convertView
                    : createRow(parent);
            row.setTag(info);
            // bind icon
            if (info.icon != null) {
                ((ImageView) row.findViewById(android.R.id.icon))
                .setVisibility(View.VISIBLE);
                ((ImageView) row.findViewById(android.R.id.icon))
                .setImageDrawable(info.icon);
             }else{
                ((ImageView) row.findViewById(android.R.id.icon))
                .setVisibility(View.GONE);
            }
            if(info.timestamp != 0)
            {
                ((DateTimeView) row.findViewById(R.id.timestamp))
                .setTime(info.timestamp);
            } else {
                ((DateTimeView) row.findViewById(R.id.timestamp))
                .setVisibility(View.GONE);
            }
            // bind caption
            ((TextView) row.findViewById(R.id.title)).setText(info.title);
            // set subtitle
            ((TextView) row.findViewById(R.id.subtitle)).setText(info.subtitle);
            row.setFocusable(true);
            return row;
        }

        private View createRow(ViewGroup parent) {
            final View row = mInflater.inflate(R.layout.notification_log_row,
                    parent, false);
            return row;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // to show "Options" in bottom we need atleast two items.
        // Hence add "clear" and "clear all" options by default.
        // Actual menu items will be decided at onMenuOpened.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean hasNotifications = false;
        if (mAdapter.getCount() >0) {
            hasNotifications = true;
        }
        // if list adapter has no items
        if (!hasNotifications) {
            menu.clear();
            menu.add(R.string.clear).setEnabled(false);
            menu.add(R.string.clearAll).setEnabled(false);
        }
        // if list adapter has some items
        else {
            menu.clear();
            notifyInfo = (HistoricalNotificationInfo) listView.getSelectedItem();
            // to show notification action in menu options
            if (notifyInfo != null) {
                if (notifyInfo.notification != null
                        && notifyInfo.notification.actions != null) {
                    for (int i = 0; i < notifyInfo.notification.actions.length; i++) {
                        menu.add(Menu.NONE, i, Menu.NONE,
                        notifyInfo.notification.actions[i].title);
                    }
                }
                // Add clear based on current notification
                if (notifyInfo.clearflag) {
                    menu.add(R.string.clear);
                } else {
                    menu.add(R.string.clear).setEnabled(false);
                }
                // for "clear all" consider all notifications
                if (mAddClearAll) {
                    menu.add(R.string.clearAll);
                }
                else {
                    menu.add(R.string.clearAll).setEnabled(false);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // TODO Auto-generated method stub
        if (item.getTitle().equals(getString(R.string.clear))) {
            try {
                mBarService.onNotificationClear(notifyInfo.pkg, notifyInfo.tag,
                        notifyInfo.Id);
            mAdapter.notifyDataSetChanged();
                return true;
            } catch (Exception e) {
                Log.d(TAG, "" + e.toString());
            }
        } else if (item.getTitle().equals(getString(R.string.clearAll))) {
            try {
                List<HistoricalNotificationInfo> activenotify = loadNotifications();
                for (int i = 0; i < activenotify.size(); i++) {
                    if (activenotify.get(i).clearflag) {
                        mBarService.onClearAllNotifications();
                    }
                }
                mAdapter.notifyDataSetChanged();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent();
        try {
            notifyInfo.notification.actions[item.getItemId()].actionIntent
            .send(mContext, 0, intent);
            mAdapter.notifyDataSetChanged();
            return true;
        } catch (Exception e) {
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == event.ACTION_UP
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            HistoricalNotificationInfo notify = (HistoricalNotificationInfo) listView
                    .getSelectedItem();
            if (notify != null) {
                if (notify.notification.contentIntent != null) {
                    Intent intent = new Intent();
                    try {
                        notify.notification.contentIntent
                                .send(mContext, 0, intent);
                        /* Status bar service api called to clear the notification
                           when the contentIntent is triggered to launch the application
                        */
                        mBarService.onNotificationClick(notify.pkg,notify.tag,notify.Id);

                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (event.getAction() == event.ACTION_UP
                && event.getKeyCode() == KeyEvent.KEYCODE_CLEAR) {
            HistoricalNotificationInfo clrnotify = (HistoricalNotificationInfo) listView
                    .getSelectedItem();
            if (clrnotify != null) {
                if (clrnotify.clearflag) {
                    try {
                        if (mBarService != null)
                            mBarService.onNotificationClear(clrnotify.pkg,
                                    clrnotify.tag, clrnotify.Id);
                    } catch (Exception e) {
                        Log.d(TAG, "exception" + e);
                        return false;
                    }
                    return true;
                } else {
                    Toast.makeText(mContext,
                            R.string.no_clearable_notification,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        // TODO Auto-generated method stub
        return super.dispatchKeyEvent(event);
    }
}
