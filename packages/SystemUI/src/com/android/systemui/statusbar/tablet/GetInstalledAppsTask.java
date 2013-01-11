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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;

public class GetInstalledAppsTask extends AsyncTask {

   private static final String TAG = "GetInstalledAppsTask";

   private PackageManager mPm = null;

   private final LaunchpadPanel mPanel;
   private List<AppEntry> mInstalledApps;
   private final Hashtable<String, String> mLoadedApps = new Hashtable<String, String>();

   protected GetInstalledAppsTask(LaunchpadPanel panel) {
      mPanel = panel;
      mPm = mPanel.getContext().getPackageManager();
   }

   @Override
   protected void onPostExecute(Object result) {
      Log.d(TAG,
            "Finished loading apps list. Sending this list to LaunchpadPanel");
      mPanel.onRetrievingInstalledApps(mInstalledApps);

   }

   @Override
   protected Object doInBackground(Object... params) {
      // By default, AsyncTask sets the worker thread to have background
      // thread priority. Setting a little higher priority here
      Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

      // Retrieve all known applications.
      final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
      mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

      List<ResolveInfo> apps = null;
      apps = mPm.queryIntentActivities(mainIntent, 0);

      if (apps == null) {
         apps = new ArrayList<ResolveInfo>();
      }

      Log.d(TAG, "number of apps returned by package manager : " + apps.size());

      final Context context = mPanel.getContext();

      // Create corresponding array of entries and load their labels.
      mInstalledApps = new ArrayList<AppEntry>(apps.size());

      for (int i = 0; i < apps.size(); i++) {
         AppEntry entry = new AppEntry(context,
               apps.get(i).activityInfo.applicationInfo);
         entry.loadLabel();
         String appLabel = entry.getLabel();

         // check if this package is detected already
         if (mLoadedApps.get(appLabel) == null) {
            mInstalledApps.add(entry);
            mLoadedApps.put(appLabel, "");

            Log.d(TAG, "app label : " + entry.getLabel());
         } else
            Log.d(TAG, "already this label is in hashtable : " + appLabel);
      }

      return null;
   }

}

