/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.server.pm;
import android.content.Context;
import static android.content.pm.PackageManager.INSTALL_FAILED_INVALID_APK;

import android.util.ArrayMap;

import com.android.internal.R;
import com.android.server.pm.Settings.DatabaseVersion;
import com.android.server.pm.Settings.VersionInfo;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;

import android.os.storage.StorageManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.KeySet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Environment.UserEnvironment;
import android.os.RemoteException;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.os.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;



public class AutoPackageManagerService extends PackageManagerService {

    final static File mScanListFile = new File(Environment.getRootDirectory(),
            "etc/package_scan_list.conf");
    private static ArraySet<String> mPrivAppList;
    private static ArraySet<String> mSystemAppList;
    private static ArraySet<String> mVendorAppList;
    private static ArraySet<String> mDataAppList;
    private static boolean mUseAppScanList = false;
    private static final String TAG = "AutoPackageManagerService";

    public static class AutoPackageManagerLoader {
        public static void loadAppScanList() {
            Slog.i(TAG, "Load App Scan List.....");
            mUseAppScanList = false;
            if (mScanListFile.exists()) {
                mPrivAppList = new ArraySet<String>();
                mSystemAppList = new ArraySet<String>();
                mDataAppList = new ArraySet<String>();
                mVendorAppList = new ArraySet<String>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(mScanListFile), 256);
                    String line;
                    ArraySet<String> fillList = null;
                    while ((line = br.readLine()) != null) {
                        // Skip comments and blank lines.
                        line = line.trim();
                        if (line.startsWith("#")
                                || line.equals("")) {
                            continue;
                        }
                        if (line.startsWith("/system/priv-app")) {
                            fillList = mPrivAppList;
                        } else if (line.startsWith("/system/app")) {
                            fillList = mSystemAppList;
                        } else if (line.startsWith("/vendor/app")) {
                            fillList = mVendorAppList;
                        } else if (line.startsWith("/data/app")) {
                            fillList = mDataAppList;
                        } else if (fillList != null
                            && (line != null && line.endsWith(".apk"))) {
                            fillList.add(line);
                        }
                    }
                    if (!mSystemAppList.isEmpty() || !mDataAppList.isEmpty()
                            || !mVendorAppList.isEmpty() ||  !mPrivAppList.isEmpty()) {
                        mUseAppScanList = true;
                    }
                } catch (IOException e) {
                    mSystemAppList.clear();
                    mVendorAppList.clear();
                    mDataAppList.clear();
                    mPrivAppList.clear();
                    mSystemAppList = null;
                    mVendorAppList = null;
                    mDataAppList = null;
                    mPrivAppList = null;
                    Log.e(TAG, "Error reading " + mScanListFile.getPath() + ".", e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            Slog.i(TAG, "mUseAppScanList=" + mUseAppScanList);
        }
    }
    public  final boolean isPackageFilename(String name) {
        return name != null && name.endsWith(".apk");
    }
    public AutoPackageManagerService(Context context, Installer installer,
            boolean factoryTest, boolean onlyCore) {
        super(context, installer, factoryTest, onlyCore);
    }

    protected void scanDirLI(File dir, int parseFlags, int scanFlags, long currentTime) {
        ArraySet<String> list = null;
        if (isFirstBoot()) {
            super.scanDirLI(dir,parseFlags,scanFlags, 0);
            return;
        }
        if (dir != null && dir.getPath().contains("system/priv-app")) {
            list = mPrivAppList;
        } else if (dir != null && dir.getPath().contains("system/app")) {
            list = mSystemAppList;
        } else if (dir != null && dir.getPath().contains("data/app")) {
            list = mDataAppList;
        } else {
            super.scanDirLI(dir,parseFlags,scanFlags, 0);
            return;
        }
        if (list != null && !list.isEmpty()) {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                String s = it.next();
                File file = new File(dir, s);
                if (!file.exists() || !isPackageFilename(s)) {
                    // Ignore entries which are not apk's
                    continue;
                }
                PackageParser.Package pkg = null;
                try {
                    Slog.d(TAG, "prescan apk=" + file.getPath());
                    if (file.getPath().contains("system/priv-app")) {
                        pkg = scanPackageLI(file.getParentFile(),
                                PackageParser.PARSE_IS_SYSTEM
                                | PackageParser.PARSE_IS_SYSTEM_DIR
                                | PackageParser.PARSE_IS_PRIVILEGED, scanFlags, 0, null);
                    }
                    if (file.getPath().contains("system/app")) {
                        pkg = scanPackageLI(file.getParentFile(), PackageParser.PARSE_IS_SYSTEM
                                | PackageParser.PARSE_IS_SYSTEM_DIR, scanFlags, 0, null);
                    }
                    if (file.getPath().contains("data/app")) {
                        pkg = scanPackageLI(file.getParentFile(), 0,
                                scanFlags | SCAN_REQUIRE_KNOWN, 0, null);
                    }
                } catch (PackageManagerException e) {
                    Slog.w(TAG, "Failed  " + file + ": " + e.getMessage());
                    if (pkg == null && (scanFlags & PackageParser.PARSE_IS_SYSTEM) == 0 &&
                        mLastScanError == PackageManager.INSTALL_FAILED_INVALID_APK) {
                        // Delete the apk
                        Slog.w(TAG, "Cleaning up failed install of " + file);
                        file.delete();
                    }
                }
            }
        }
    }

    private void laterScanDir(File dir, ArraySet<String> appList, int flags, int scanMode) {
        String[] files = dir.list();
        if (files == null) {
            Log.e(TAG, "No files in app dir " + dir);
            return;
        }
        int i = 0;
        ArrayList<String> pkgList = new ArrayList<String>();
        for (i = 0; i < files.length; i++) {
            File file = new File(dir, files[i] + "/" + files[i] + ".apk");
            if (appList.contains(files[i] + "/" + files[i] + ".apk")
                || appList.contains(files[i] + ".apk")) {
                continue;
            }
            Slog.d(TAG, "later scan apk=" + file.getPath());
            PackageParser.Package pkg = null;
            try {
                if(file.getPath().contains("system/priv-app")) {
                    pkg = scanPackageLI(file.getParentFile(),
                            PackageParser.PARSE_IS_SYSTEM
                            | PackageParser.PARSE_IS_SYSTEM_DIR
                            | PackageParser.PARSE_IS_PRIVILEGED, scanMode, 0, null);
                }
                if(file.getPath().contains("system/app")) {
                    pkg = scanPackageLI(file.getParentFile(),
                            PackageParser.PARSE_IS_SYSTEM
                            | PackageParser.PARSE_IS_SYSTEM_DIR, scanMode, 0, null);
                }
                if(file.getPath().contains("data/app")) {
                    pkg = scanPackageLI(file.getParentFile(), 0,
                                scanMode | SCAN_REQUIRE_KNOWN, 0, null);
                }
                if (null != pkg) pkgList.add(pkg.packageName);
            } catch (PackageManagerException e) {
                // Don't mess around with apps in system partition.
                if (pkg == null && (flags & PackageParser.PARSE_IS_SYSTEM) == 0 &&
                        mLastScanError == PackageManager.INSTALL_FAILED_INVALID_APK) {
                    // Delete the apk
                    Slog.w(TAG, "Cleaning up failed install of " + file);
                    file.delete();
                }
            }
        }
        Slog.d(TAG, "Sending notification to launcher" );
        sendLaterScanBroadcast(pkgList);
    }

    public void startLaterScanApkThread() {
        if (!mUseAppScanList) return;
        final int scanFlags = SCAN_NO_PATHS | SCAN_DEFER_DEX | SCAN_BOOTING | SCAN_INITIAL;
        Slog.i(TAG," ********** LaterscanApp ****************");
        final VersionInfo ver = mSettings.getInternalVersion();
        new Thread() {
            @Override
            public void run() {
                synchronized (mInstallLock) {
                    // writer
                    int scanMode =  SCAN_NO_PATHS | SCAN_DEFER_DEX;
                    laterScanDir(mPrivilegedAppDir, mPrivAppList,
                            PackageParser.PARSE_IS_SYSTEM
                            | PackageParser.PARSE_IS_SYSTEM_DIR
                            | PackageParser.PARSE_IS_PRIVILEGED, scanMode);
                    laterScanDir(mSystemAppDir, mSystemAppList,
                            PackageParser.PARSE_IS_SYSTEM
                            | PackageParser.PARSE_IS_SYSTEM_DIR, scanMode);
                    laterScanDir(mAppInstallDir, mDataAppList,
                            0, scanMode | SCAN_REQUIRE_KNOWN);
                    // laterScanDir(vendorAppDir, mVendorAppList, PackageParser.PARSE_IS_SYSTEM
                    //         | PackageParser.PARSE_IS_SYSTEM_DIR, scanMode);

                    if (DEBUG_UPGRADE) Log.v(TAG, "Running installd update commands");
                    mInstaller.moveFiles();

                    // Prune any system packages that no longer exist.
                    mPossiblyDeletedUpdatedSystemApps = new ArrayList<String>();
                    if (!mOnlyCore) {
                        Iterator<PackageSetting> psit = mSettings.mPackages.values().iterator();
                        while (psit.hasNext()) {
                            PackageSetting ps = psit.next();

                            /*
                             * If this is not a system app, it can't be a
                             * disable system app.
                             */
                            if ((ps.pkgFlags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                                continue;
                            }

                            /*
                             * If the package is scanned, it's not erased.
                             */
                            final PackageParser.Package scannedPkg = mPackages.get(ps.name);
                            if (scannedPkg != null) {
                                /*
                                 * If the system app is both scanned and in the
                                 * disabled packages list, then it must have been
                                 * added via OTA. Remove it from the currently
                                 * scanned package so the previously user-installed
                                 * application can be scanned.
                                 */
                                if (mSettings.isDisabledSystemPackageLPr(ps.name)) {
                                    logCriticalInfo(Log.WARN,
                                            "Expecting better updated system app for "
                                            + ps.name +
                                            "; removing system app.  Last known codePath="
                                            + ps.codePathString + ", installStatus="
                                            + ps.installStatus + ", versionCode=" +
                                            ps.versionCode + "; scanned versionCode="
                                            + scannedPkg.mVersionCode);
                                    removePackageLI(ps, true);
                                    mExpectingBetter.put(ps.name, ps.codePath);
                                }
                                continue;
                            }
                            removeOrDeleteUpdatedApps(ps, psit);
                        }
                    }

                     //look for any incomplete package installations
                    ArrayList<PackageSetting> deletePkgsList = mSettings.
                            getListOfIncompleteInstallPackagesLPr();
                    //clean up list
                    for(int i = 0; i < deletePkgsList.size(); i++) {
                        //clean up here
                        cleanupInstallFailedPackage(deletePkgsList.get(i));
                    }
                    //delete tmp files
                    deleteTempPackageFiles();

                    // Remove any shared userIDs that have no associated packages
                    mSettings.pruneSharedUsersLPw();
                    if (!mOnlyCore) {
                        scanDirLI(mAppInstallDir, 0, scanFlags | SCAN_REQUIRE_KNOWN, 0);
                        scanDirLI(mDrmAppPrivateInstallDir, PackageParser.PARSE_FORWARD_LOCK,
                                scanFlags | SCAN_REQUIRE_KNOWN, 0);
                        /**
                         * Remove disable package settings for any updated system
                         * apps that were removed via an OTA. If they're not a
                         * previously-updated app, remove them completely.
                         * Otherwise, just revoke their system-level permissions.
                         */
                        for (String deletedAppName : mPossiblyDeletedUpdatedSystemApps) {
                            PackageParser.Package deletedPkg = mPackages.get(deletedAppName);
                            mSettings.removeDisabledSystemPackageLPw(deletedAppName);

                            String msg;
                            if (deletedPkg == null) {
                                msg = "Updated system package " + deletedAppName
                                        + " no longer exists; wiping its data";
                                removeDataDirsLI(null, deletedAppName);
                            } else {
                                msg = "Updated system app + " + deletedAppName
                                        + " no longer present; removing system privileges for "
                                        + deletedAppName;

                                deletedPkg.applicationInfo.flags &= ~ApplicationInfo.FLAG_SYSTEM;

                                PackageSetting deletedPs = mSettings.mPackages.get(deletedAppName);
                                deletedPs.pkgFlags &= ~ApplicationInfo.FLAG_SYSTEM;
                            }
                            logCriticalInfo(Log.WARN, msg);
                        }

                        /**
                         * Make sure all system apps that we expected to appear on
                         * the userdata partition actually showed up. If they never
                         * appeared, crawl back and revive the system version.
                         */
                        for (int i = 0; i < mExpectingBetter.size(); i++) {
                            final String packageName = mExpectingBetter.keyAt(i);
                            if (!mPackages.containsKey(packageName)) {
                                final File scanFile = mExpectingBetter.valueAt(i);

                                logCriticalInfo(Log.WARN, "Expected better " + packageName
                                        + " but never showed up; reverting to system");

                                final int reparseFlags;
                                if (FileUtils.contains(mPrivilegedAppDir, scanFile)) {
                                    reparseFlags = PackageParser.PARSE_IS_SYSTEM
                                            | PackageParser.PARSE_IS_SYSTEM_DIR
                                            | PackageParser.PARSE_IS_PRIVILEGED;
                                } else if (FileUtils.contains(mSystemAppDir, scanFile)) {
                                    reparseFlags = PackageParser.PARSE_IS_SYSTEM
                                            | PackageParser.PARSE_IS_SYSTEM_DIR;
                                } /*else if (FileUtils.contains(vendorAppDir, scanFile)) {
                                    reparseFlags = PackageParser.PARSE_IS_SYSTEM
                                            | PackageParser.PARSE_IS_SYSTEM_DIR;
                                } else if (FileUtils.contains(oemAppDir, scanFile)) {
                                    reparseFlags = PackageParser.PARSE_IS_SYSTEM
                                            | PackageParser.PARSE_IS_SYSTEM_DIR;
                                } */else {
                                    Slog.e(TAG, "Ignoring unexpected fallback path " + scanFile);
                                    continue;
                                }

                                mSettings.enableSystemPackageLPw(packageName);

                                try {
                                    scanPackageLI(scanFile, reparseFlags, scanFlags, 0, null);
                                } catch (PackageManagerException e) {
                                    Slog.e(TAG, "Failed to parse original system package: "
                                            + e.getMessage());
                                }
                            }
                        }
                    }
                    mExpectingBetter.clear();

                    // Now that we know all of the shared libraries, update all clients to have
                    // the correct library paths.
                    updateAllSharedLibrariesLPw();
                    executeAdjustCpuAbisForSharedUserLPw();

                    // Now that we know all the packages we are keeping,
                    // read and update their last usage times.
                    mPackageUsage.readLP();
                    // If the platform SDK has changed since the last time we booted,
                    // we need to re-grant app permission to catch any new ones that
                    // appear.  This is really a hack, and means that apps can in some
                    // cases get permissions that the user didn't initially explicitly
                    // allow...  it would be nice to have some better way to handle
                    // this situation.
                    int updateFlags = UPDATE_PERMISSIONS_ALL;
                    if (ver.sdkVersion != mSdkVersion) {
                        Slog.i(TAG, "Platform changed from " + ver.sdkVersion + " to "
                                + mSdkVersion + "; regranting permissions for internal storage");
                                updateFlags |= UPDATE_PERMISSIONS_REPLACE_PKG
                                | UPDATE_PERMISSIONS_REPLACE_ALL;
                    }
                    updatePermissionsLPw(null, null, updateFlags);
                    ver.sdkVersion = mSdkVersion;
                    // clear only after permissions have been updated
                    mExistingSystemPackages.clear();
                    mPromoteSystemApps = false;

                    // If this is the first boot, and it is a normal boot, then
                    // we need to initialize the default preferred apps.
                    if (!mRestoredSettings && !mOnlyCore) {
                        mSettings.applyDefaultPreferredAppsLPw(AutoPackageManagerService.this,
                                UserHandle.USER_OWNER);
                        applyFactoryDefaultBrowserLPw(UserHandle.USER_OWNER);
                        primeDomainVerificationsLPw(UserHandle.USER_OWNER);
                    }

                    // If this is first boot after an OTA, and a normal boot, then
                    // we need to clear code cache directories.
                    if (mIsUpgrade && !mOnlyCore) {
                        Slog.i(TAG, "Build fingerprint changed; clearing code caches");
                        for (int i = 0; i < mSettings.mPackages.size(); i++) {
                            final PackageSetting ps = mSettings.mPackages.valueAt(i);
                            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, ps.volumeUuid)) {
                            deleteCodeCacheDirsLI(ps.volumeUuid, ps.name);
                            }
                        }
                        ver.fingerprint = Build.FINGERPRINT;
                    }

                    checkDefaultBrowser();

                    // All the changes are done during package scanning.
                    ver.databaseVersion = Settings.CURRENT_DATABASE_VERSION;
                    // can downgrade to reader
                    mSettings.writeLPr();
                    mRequiredVerifierPackage = getRequiredVerifierLPr();
                    mRequiredInstallerPackage = getRequiredInstallerLPr();
                }

                try {
                    String receiverPerm = android.Manifest.permission.
                            RECEIVE_BOOT_COMPLETED;
                    String[] receiverPermission = receiverPerm == null ? null
                            : new String[] {receiverPerm};
                    // Get the ActivityManager object to send missed intents.
                    IActivityManager am = ActivityManagerNative.getDefault();
                    // Prepare intent for Boot completed.
                    Intent intents = new Intent(Intent.ACTION_BOOT_COMPLETED);
                    // Get the package names for the boot completed intent.
                    ArraySet<String> pkgNames = getPackageNamesForIntent(intents);
                    int userIdentifier = UserHandle.USER_OWNER;
                    // Iterate through the received package names to send.
                    for (String name: pkgNames) {
                        int uidd = getPackageUid(name, userIdentifier);
                        PackageSetting pkgSetting = mSettings.mPackages.get(name);
                        // Need to add any core - stopped app which needs to get event.
                        if (name.contains("com.android.phone")) {
                            Intent bcIntent = new Intent(Intent.ACTION_BOOT_COMPLETED)
                                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                                    .setPackage(name);
                            Bundle extras = new Bundle(1);
                            extras.putInt(Intent.EXTRA_UID,
                                    UserHandle.getUid(uidd, pkgSetting.appId));
                            if (bcIntent != null) bcIntent.putExtras(extras);
                            int uidds = getPackageUid(name, -1);
                            // Check the staus of package whether it is running or not.
                            if (am != null && !am.isUserRunning(uidds, true)) {
                                Slog.i(TAG,"Sending broadcast for core package...:" + name);
                                am.broadcastIntent(null, bcIntent, null, null, 0, null, null,
                                        receiverPermission,
                                        android.app.AppOpsManager.OP_NONE, extras,
                                        false, false, uidds);
                            }
                        }
                        // Get the package information who don't have activities.
                        PackageInfo pkInfo = getPackageInfo(name,PackageManager.GET_ACTIVITIES,0);
                        if (pkInfo != null && (pkInfo.activities == null
                                || pkInfo.coreApp == false)) {
                            Slog.i(TAG,"Sending BC Intent to stopped package: " + name);
                            Intent bcIntent = new Intent(Intent.ACTION_BOOT_COMPLETED)
                                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                                    .setPackage(name);
                            Bundle extras = new Bundle(1);
                            extras.putInt(Intent.EXTRA_UID,
                                    UserHandle.getUid(uidd, pkgSetting.appId));
                            if (bcIntent != null) bcIntent.putExtras(extras);
                            int uidds = getPackageUid(name, -1);
                            // Check the staus of package whether it is running or not.
                            if (am != null && !am.isUserRunning(uidds, true)) {
                                Slog.i(TAG,"Sending broadcast for stopped package...:" + name);
                                am.broadcastIntent(null, bcIntent, null, null, 0, null, null,
                                        receiverPermission,
                                        android.app.AppOpsManager.OP_NONE, extras,
                                        false, false, uidds);
                            }
                        }
                    }
                } catch(RemoteException e) {
                    Slog.e(TAG,"Exception while sending broadcast to stopped package");
                }
                mSystemAppList.clear();
                mPrivAppList.clear();
                mDataAppList.clear();
                mVendorAppList.clear();
                mSystemAppList = null;
                mPrivAppList = null;
                mVendorAppList = null;
                mDataAppList = null;
            };
        }.start();
    }

    private void sendLaterScanBroadcast(ArrayList<String> pkgList) {
        int size = pkgList.size();
        if (size > 0) {
            // Send broadcasts here
            Bundle extras = new Bundle();
            extras.putStringArray(Intent.EXTRA_CHANGED_PACKAGE_LIST, pkgList
                    .toArray(new String[size]));
            String action = Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE;
            sendPackageBroadcast(action, null, extras, null, null, null);
        }
    }

    @Override
    protected void removeOrDeleteUpdatedApps(PackageSetting ps, Iterator<PackageSetting> psit) {
    }

    @Override
    protected void executeAdjustCpuAbisForSharedUserLPw() {
        if (isFirstBoot()) {
            for (SharedUserSetting setting : mSettings.getAllSharedUsersLPw()) {
                    // NOTE: We ignore potential failures here during a system scan (like
                    // the rest of the commands above) because there's precious little we
                    // can do about it. A settings error is reported, though.
                    adjustCpuAbisForSharedUserLPw(setting.packages, null /* scanned package */,
                            false /* force dexopt */, false /* defer dexopt */);
            }
        }
    }
}
