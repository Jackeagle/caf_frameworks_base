/*
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.systemui.statusbar.tablet;

import java.io.File;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public class AppEntry {

   private final Context mContext;
   private final PackageManager mPm;
   private final ApplicationInfo mInfo;
   private final File mApkFile;
   private String mLabel;
   private Drawable mIcon;
   private boolean mMounted;

   public AppEntry(Context context, ApplicationInfo info) {
      mContext = context;
      mPm = context.getPackageManager();

      mInfo = info;
      mApkFile = new File(info.sourceDir);
   }

   public ApplicationInfo getApplicationInfo() {
      return mInfo;
   }

   public String getLabel() {
      return mLabel;
   }

   public String getPackage() {
      return mInfo.packageName;
   }

   public Drawable getIcon() {
      if (mIcon == null) {
         if (mApkFile.exists()) {
            mIcon = mInfo.loadIcon(mPm);
            return mIcon;
         } else {
            mMounted = false;
         }
      } else if (!mMounted) {
         // If the app wasn't mounted but is now mounted, reload
         // its icon.
         if (mApkFile.exists()) {
            mMounted = true;
            mIcon = mInfo.loadIcon(mPm);
            return mIcon;
         }
      } else {
         return mIcon;
      }

      return mContext.getResources().getDrawable(
            android.R.drawable.sym_def_app_icon);
   }

   @Override
   public String toString() {
      return mLabel;
   }

   void loadLabel() {
      if (mLabel == null || !mMounted) {
         if (!mApkFile.exists()) {
            mMounted = false;
            mLabel = mInfo.packageName;
         } else {
            mMounted = true;
            CharSequence label = mInfo.loadLabel(mPm);
            mLabel = label != null ? label.toString() : mInfo.packageName;
         }
      }
   }

}

