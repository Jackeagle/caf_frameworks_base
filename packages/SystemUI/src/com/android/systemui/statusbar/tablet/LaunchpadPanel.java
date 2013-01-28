/*

Copyright (c) 2013, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/
package com.android.systemui.statusbar.tablet;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LaunchpadPanel {

   private static final String TAG = "LaunchpadPanel";
   private static final String ACTION_SHOW_LAUNCHPAD = "ACTION_SHOW_LAUNCHPAD";

   // Dimensions & positions for Launchpad / Apps grid dialogs
   private static final int POSITION_X = -720;
   private static final int POSITION_Y = 550;
   private static final int LAUNCHPAD_WIDTH = 1620;
   private static final int LAUNCHPAD_HEIGHT = 360;
   private static final int APPS_GRID_WIDTH = 1920;
   private static final int APPS_GRID_HEIGHT = 1080;

   private ArrayList<String> mLaunchpadApplications = null;
   private IntentFilter mShowLaunchpadFilter = null;

   private Context mContext = null;
   private GridView mLaunchpadGrid = null;
   private PackageManager mPkgMgr = null;
   private LaunchpadAdapter mLaunchpadAdapter = null;

   private ProgressBar mIndefiniteProgress;
   private GridView mAppsGridView;
   protected List<AppEntry> mAppDetailsList;
   private AppsGridAdapter mAppsGridAdapter;

   // Dialogs containing the views
   private Dialog mLaunchpadDialog = null;
   private Dialog mAppsGridDialog = null;

   // Background thread to retrieve apps list
   private GetInstalledAppsTask mGetAppsTask;
   private PackageIntentReceiver mPackageObserver;

   public LaunchpadPanel(final Context context) {
      mContext = context;
      mShowLaunchpadFilter = new IntentFilter(ACTION_SHOW_LAUNCHPAD);
      mContext.registerReceiver(mShowLaunchpadReceiver, mShowLaunchpadFilter);

      mPkgMgr = mContext.getPackageManager();

      mGetAppsTask = new GetInstalledAppsTask(this);
      mGetAppsTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

      mPackageObserver = new PackageIntentReceiver(this);

      // prepare mini launchpad dialog
      prepareMiniLaunchpad();
      // prepare all apps grid dialog
      prepareAppsGridDialog();

   }

   protected void cleanUp() {
      // Stop monitoring for changes.
      if (mPackageObserver != null) {
         mContext.unregisterReceiver(mPackageObserver);
         mPackageObserver = null;
      }
   }

   protected void onRetrievingInstalledApps(List<AppEntry> newList) {
      mAppDetailsList = newList;
      Log.d(TAG, "retrieved installed apps. count : " + mAppDetailsList.size());

      // The list should now be shown.
      mIndefiniteProgress.setVisibility(View.GONE);

      // update the adapter
      mAppsGridAdapter.changeDataSet(mAppDetailsList);
   }

   private void rebuildAppsList() {
      // show the animation until apps details are fetched
	  mIndefiniteProgress.setVisibility(View.VISIBLE);
	  
      mGetAppsTask = new GetInstalledAppsTask(this);
      mGetAppsTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
   }

   protected Context getContext() {
      return mContext;
   }

   private void prepareMiniLaunchpad() {
      LayoutInflater inflater = (LayoutInflater) mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View launchpadView = inflater.inflate(R.layout.launchpad, null);

      mLaunchpadGrid = (GridView) launchpadView
            .findViewById(R.id.launchPadGrid);
      mLaunchpadGrid.setOnItemClickListener(new OnItemClickListener() {

         public void onItemClick(AdapterView<?> arg0, View arg1, int position,
               long arg3) {
            Log.d(TAG, "Inside Launchpad's item click listener : " + position);

            if (position == 6) {
               // hide launchpad grid
               showLaunchpad();

               // show all apps grid
               showAppsGrid();
            } else {
               String packageName = mLaunchpadApplications.get(position);
               ApplicationInfo appInfo = null;
               try {
                  appInfo = mPkgMgr.getApplicationInfo(packageName, 0);
                  Intent launchApp = mPkgMgr
                        .getLaunchIntentForPackage(appInfo.packageName);

                  mContext.startActivity(launchApp);
               } catch (NameNotFoundException e1) {
                  e1.printStackTrace();
               }

               // hide the dialog after launching the app
               showLaunchpad();
            }

         }
      });

      if (mLaunchpadApplications == null) {
         mLaunchpadApplications = new ArrayList<String>();

         mLaunchpadApplications.add("com.android.browser");
         mLaunchpadApplications.add("com.google.android.gallery3d");
         mLaunchpadApplications.add("com.android.music");
         mLaunchpadApplications.add("com.android.settings");
         mLaunchpadApplications.add("com.qualcomm.wfd.client");
         mLaunchpadApplications.add("com.android.qualcomm");
      }

      mLaunchpadAdapter = new LaunchpadAdapter(this);
      mLaunchpadGrid.setAdapter(mLaunchpadAdapter);

      mLaunchpadDialog = new Dialog(mContext, R.style.CustomDialogTheme);
      mLaunchpadDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      mLaunchpadDialog.setTitle("Launchpad");
      mLaunchpadDialog.setContentView(launchpadView);

      // Change some window properties
      Window window = mLaunchpadDialog.getWindow();
      window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
      LayoutParams lp = window.getAttributes();
      lp.token = null;
      lp.format = PixelFormat.TRANSLUCENT;
      lp.type = LayoutParams.TYPE_VOLUME_OVERLAY;
      lp.windowAnimations = com.android.internal.R.style.Animation_Toast;

      lp.width = LAUNCHPAD_WIDTH;
      lp.height = LAUNCHPAD_HEIGHT;

      window.setAttributes(lp);
      window.addFlags(LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
   }

   private void prepareAppsGridDialog() {
      LayoutInflater inflater = (LayoutInflater) mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View appsGridView = inflater.inflate(R.layout.apps_grid, null);

      mAppsGridView = (GridView) appsGridView.findViewById(R.id.appsGridView);
      mIndefiniteProgress = (ProgressBar) appsGridView
            .findViewById(R.id.progressBar);

      mAppsGridView.setOnItemClickListener(new OnItemClickListener() {

         public void onItemClick(AdapterView<?> arg0, View arg1, int position,
               long arg3) {
            Log.d(TAG, "Inside Launchpad's item click listener : " + position);

            AppEntry appDetails = mAppDetailsList.get(position);
            String packageName = appDetails.getPackage();
            ApplicationInfo appInfo = null;
            try {
               appInfo = mPkgMgr.getApplicationInfo(packageName, 0);
               Intent launchApp = mPkgMgr
                     .getLaunchIntentForPackage(appInfo.packageName);

               mContext.startActivity(launchApp);
            } catch (NameNotFoundException e1) {
               e1.printStackTrace();
            }

            // hide the dialog after launching the app
            showAppsGrid();

         }

      });

      mIndefiniteProgress.setVisibility(View.VISIBLE);

      mAppsGridAdapter = new AppsGridAdapter(this);
      mAppsGridView.setAdapter(mAppsGridAdapter);

      mAppsGridDialog = new Dialog(mContext, R.style.CustomDialogTheme);
      mAppsGridDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      mAppsGridDialog.setTitle("AppsGrid");
      mAppsGridDialog.setContentView(appsGridView);

      // Change some window properties
      Window window = mAppsGridDialog.getWindow();
      window.setGravity(Gravity.CENTER);
      LayoutParams lp = window.getAttributes();
      lp.token = null;
      lp.format = PixelFormat.TRANSLUCENT;
      lp.type = LayoutParams.TYPE_VOLUME_OVERLAY;
      lp.windowAnimations = com.android.internal.R.style.Animation_Toast;

      lp.width = APPS_GRID_WIDTH;
      lp.height = APPS_GRID_HEIGHT;

      window.setAttributes(lp);
      window.addFlags(LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
   }

   protected void showLaunchpad() {
      if (!mLaunchpadDialog.isShowing())
         mLaunchpadDialog.show();
      else
         mLaunchpadDialog.dismiss();
   }

   protected void showAppsGrid() {
      if (!mAppsGridDialog.isShowing())
         mAppsGridDialog.show();
      else
         mAppsGridDialog.dismiss();
   }

   private final BroadcastReceiver mShowLaunchpadReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {

         String actionStr = intent.getAction();
         Log.d(TAG, "Received intent broadcast : " + actionStr);

         if (actionStr.equalsIgnoreCase(ACTION_SHOW_LAUNCHPAD)) {
            if (!mAppsGridDialog.isShowing() && !mLaunchpadDialog.isShowing())
               showLaunchpad();
            else {
               // hide both dialogs
               mAppsGridDialog.dismiss();
               mLaunchpadDialog.dismiss();
            }
         }
      }
   };

   private static class LaunchpadAdapter extends BaseAdapter {
      private final LayoutInflater mInflater;
      LaunchpadPanel mPanel;

      public LaunchpadAdapter(LaunchpadPanel panel) {
         mPanel = panel;
         mInflater = (LayoutInflater) panel.mContext
               .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      }

      public int getCount() {
         return mPanel.mLaunchpadApplications.size() + 1;
      }

      /**
       * Populate new items in the list.
       */
      public View getView(int position, View convertView, ViewGroup parent) {
         View view;
         ImageView iconView;
         TextView iconTitle;

         if (convertView == null) {
            view = mInflater.inflate(R.layout.grid_elem, parent, false);
         } else {
            view = convertView;
         }

         iconView = (ImageView) view.findViewById(R.id.appIcon);
         iconTitle = (TextView) view.findViewById(R.id.appTitle);

         // More button
         if (position == 6) {
            iconTitle.setText("All Apps");
            iconView.setImageDrawable(mPanel.mContext.getResources()
                  .getDrawable(R.drawable.all_apps));
         } else {
            String packageName = mPanel.mLaunchpadApplications.get(position);
            ApplicationInfo appInfo = null;
            try {
               appInfo = mPanel.mPkgMgr.getApplicationInfo(packageName, 0);
               iconView.setImageDrawable(appInfo.loadIcon(mPanel.mPkgMgr));
               iconTitle.setText(appInfo.loadLabel(mPanel.mPkgMgr));
            } catch (NameNotFoundException e1) {
               e1.printStackTrace();
            }
         }

         return view;
      }

      public Object getItem(int position) {
         // TODO Auto-generated method stub
         return null;
      }

      public long getItemId(int position) {
         // TODO Auto-generated method stub
         return 0;
      }
   }

   private static class PackageIntentReceiver extends BroadcastReceiver {

      LaunchpadPanel mPanel;

      public PackageIntentReceiver(LaunchpadPanel panel) {
         mPanel = panel;

         IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
         filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
         filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
         filter.addDataScheme("package");
         mPanel.mContext.registerReceiver(this, filter);

         // Register for events related to sdcard installation.
         IntentFilter sdFilter = new IntentFilter();
         sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
         sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
         mPanel.mContext.registerReceiver(this, sdFilter);
      }

      @Override
      public void onReceive(Context context, Intent intent) {
         // rebuild the list
         mPanel.rebuildAppsList();
      }
   }

   // static to save the reference to the outer class and to avoid access to
   // any members of the containing class
   static class ViewHolder {
      public RelativeLayout singleRow;
      public ImageView appIcon;
      public TextView appTitle;
   }

   private static class AppsGridAdapter extends BaseAdapter {

      private static final String TAG = "AppsGridAdapter";

      int count = 0;

      LaunchpadPanel mPanel;
      private final LayoutInflater mInflater;

      public AppsGridAdapter(LaunchpadPanel panel) {
         mPanel = panel;
         mInflater = (LayoutInflater) panel.mContext
               .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      }

      public int getCount() {
         if (mPanel.mAppDetailsList != null)
            return mPanel.mAppDetailsList.size();
         else
            return count;
      }

      public void changeDataSet(List<AppEntry> newList) {
         notifyDataSetInvalidated();
         mPanel.mAppDetailsList = newList;
         notifyDataSetChanged();
      }

      private View fetchAppropriateView(final int position, View convertView,
            ViewGroup parent) {

         ViewHolder holder;
         View rowView = convertView;

         if (rowView == null) {

            rowView = mInflater.inflate(R.layout.grid_elem, null, true);
            holder = new ViewHolder();

            holder.singleRow = (RelativeLayout) rowView
                  .findViewById(R.id.appsGridElem);
            holder.appIcon = (ImageView) rowView.findViewById(R.id.appIcon);
            holder.appTitle = (TextView) rowView.findViewById(R.id.appTitle);

            rowView.setTag(holder);
         } else {
            holder = (ViewHolder) rowView.getTag();
         }

         if (mPanel.mAppDetailsList != null) {
            AppEntry item = mPanel.mAppDetailsList.get(position);
            holder.appIcon.setImageDrawable(item.getIcon());
            holder.appTitle.setText(item.getLabel());

            Log.d(TAG, "Loading app : " + item.getLabel() + ", in adapter");
         } else
            Log.d(TAG, "Invalid app details list");

         return rowView;

      }

      public View getView(int position, View convertView, ViewGroup parent) {
         return fetchAppropriateView(position, convertView, parent);
      }

      public Object getItem(int position) {
         return null;
      }

      public long getItemId(int position) {
         return -1;
      }

   }

}

