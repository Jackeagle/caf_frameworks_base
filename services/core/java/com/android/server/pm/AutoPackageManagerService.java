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
import static android.content.pm.PackageManager.INSTALL_FAILED_INTERNAL_ERROR;
import static android.content.pm.PackageManager.INSTALL_FAILED_INVALID_APK;

import android.util.ArrayMap;

import com.android.internal.R;
import com.android.internal.content.NativeLibraryHelper;
import com.android.server.pm.Settings.DatabaseVersion;
import com.android.server.pm.Settings.VersionInfo;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;

import android.os.storage.StorageManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageScanOpt;
import android.content.pm.IPackageScanOpt.PackageScanOptException;
import android.content.pm.KeySet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.PackageParser;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Environment.UserEnvironment;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.os.FileUtils;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import dalvik.system.PathClassLoader;

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

import libcore.io.IoUtils;


public class AutoPackageManagerService extends PackageManagerService {

    final static File mScanListFile = new File(Environment.getRootDirectory(),
            "etc/package_scan_list.conf");
    private static ArraySet<String> mPrivAppList;
    private static ArraySet<String> mSystemAppList;
    private static ArraySet<String> mVendorAppList;
    private static ArraySet<String> mDataAppList;
    private static boolean mUseAppScanList = false;
    private static final String TAG = "AutoPackageManagerService";

    private static final String OPTIMIZATION_JAR = "/system/framework/PackageScanOpt.jar";
    private static final String OPTIMIZATION_CLASS
            = "com.qualcomm.qti.packagescanopt.PackageScanOpt";
    private static boolean mIsOptLoaded = false;
    private static IPackageScanOpt iPkgOpt = null;

    protected long recordPeroidT(final int num, final long lastMs) {
        long ret = 0;
        if (mIsOptLoaded) {
            ret = iPkgOpt.recordPeroidT(num, lastMs);
        }
        return ret;
    }

    protected void dumpPeroidT() {
        if (mIsOptLoaded) {
            iPkgOpt.dumpPeroidT();
        }
    }

    protected PackageParser.Package parsePackage(PackageParser pp,
            File scanFile, int parseFlags, int scanFlags)
        throws PackageParserException {
        PackageParser.Package pkg = null;
        if (mIsOptLoaded) {
            try {
                pkg = iPkgOpt.optParsePackage(pp, scanFile, parseFlags, scanFlags);
            } catch (PackageScanOptException ex) {
                pkg = super.parsePackage(pp, scanFile, parseFlags, scanFlags);
            }
        } else {
            pkg = super.parsePackage(pp, scanFile, parseFlags, scanFlags);
        }
        return pkg;
    }

    @Override
    public void derivePackageAbi(PackageParser.Package pkg, File scanFile,
            String cpuAbiOverride, boolean extractLibs)
        throws PackageManagerException {
        boolean needCheckNativeLib = true;
        IPackageScanOpt.PkgAbiInfo pkgAbiInfo = new IPackageScanOpt.PkgAbiInfo();

        if (mIsOptLoaded) {
            needCheckNativeLib = iPkgOpt.readNativeInfo(pkg, pkgAbiInfo);
            derivePackageAbi(pkg, scanFile, cpuAbiOverride, extractLibs,
                    needCheckNativeLib,
                    pkgAbiInfo.primaryCpuAbi, pkgAbiInfo.secondaryCpuAbi);
            if (needCheckNativeLib) {
                iPkgOpt.writeNativeInfo(pkg, scanFile);
            }
        } else {
            derivePackageAbi(pkg, scanFile, cpuAbiOverride, extractLibs, true, null, null);
        }
    }

    public void cleanUselessPkgInfo() {
        if (mIsOptLoaded) {
            iPkgOpt.cleanUselessPkgInfo();
        }
    }

    public static class AutoPackageManagerLoader {
        private static AutoSQLiteHelper mAutoDb;
        public AutoPackageManagerLoader(Context context) {
            mAutoDb = new AutoSQLiteHelper(context,
                    "/data/misc/bootopt", 1, "conf_table");
        }
        public static void loadAppScanList() {
            Slog.i(TAG, "Load App Scan List.....");
            mUseAppScanList = false;
            mPrivAppList = new ArraySet<String>();
            mSystemAppList = new ArraySet<String>();
            mDataAppList = new ArraySet<String>();
            mVendorAppList = new ArraySet<String>();
            loadList(mScanListFile);
            if ((!mSystemAppList.isEmpty() || !mDataAppList.isEmpty()
                    || !mVendorAppList.isEmpty() ||  !mPrivAppList.isEmpty())
                    && mScanListFile.exists()) {
                mUseAppScanList = true;
            }
            if (mAutoDb == null) return; // Don't try to read when null.
            if ((mAutoDb.getProfilesRowCount() != 0) && (mUseAppScanList == true)) {
                List<String> dbList_privapp = mAutoDb.readFromDb(Pair.create("priv-app", ""));
                if (dbList_privapp != null && dbList_privapp.size() > 0) {
                    for (String str : dbList_privapp) {
                        mPrivAppList.add(str);
                    }
                }
                List<String> dbList_data = mAutoDb.readFromDb(Pair.create("data-app", ""));
                if (dbList_data != null && dbList_data.size() > 0) {
                    for (String str : dbList_data) {
                        mDataAppList.add(str);
                    }
                }
                List<String> dbList_sys = mAutoDb.readFromDb(Pair.create("system-app", ""));
                if (dbList_sys != null &&  dbList_sys.size() > 0)  {
                    for (String str : dbList_sys) {
                        mSystemAppList.add(str);
                    }
                }
            }
        }

        public static void loadList(File file) {
            if (file.exists()) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(file), 256);
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
                } catch (IOException e) {
                    mSystemAppList.clear();
                    mVendorAppList.clear();
                    mDataAppList.clear();
                    mPrivAppList.clear();
                    mSystemAppList = null;
                    mVendorAppList = null;
                    mDataAppList = null;
                    mPrivAppList = null;
                    mUseAppScanList = false;
                    Log.e(TAG, "Error reading " + file.getPath() + ".", e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }

        public static void loadScanOpt() {
            if (mIsOptLoaded == false) {
                try {
                    Class optClass;
                    PathClassLoader optClassLoader;

                    optClassLoader = new PathClassLoader(OPTIMIZATION_JAR,
                            ClassLoader.getSystemClassLoader());

                    if (optClassLoader != null) {
                        optClass = optClassLoader.loadClass(OPTIMIZATION_CLASS);
                        Constructor<? extends IPackageScanOpt> mConstructor =
                                optClass.getConstructor();
                        if (mConstructor != null) {
                            iPkgOpt = mConstructor.newInstance();
                        }
                        if (iPkgOpt != null) {
                            mIsOptLoaded = true;
                        }
                    }
                } catch (ClassNotFoundException | InstantiationException
                        | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException ex) {
                    // Any Exception hint that ScanOpt load failed.
                    Log.w(TAG, "Load Optimization PMS failed: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Derive the ABI of a non-system package located at {@code scanFile}. This information
     * is derived purely on the basis of the contents of {@code scanFile} and
     * {@code cpuAbiOverride}.
     *
     * If {@code extractLibs} is true, native libraries are extracted from the app if required.
     */
    public void derivePackageAbi(PackageParser.Package pkg, File scanFile,
            String cpuAbiOverride, boolean extractLibs,
            boolean needCheckNativeLib,
            String primaryCpuAbi, String secondaryCpuAbi)
        throws PackageManagerException {
        // TODO: We can probably be smarter about this stuff. For installed apps,
        // we can calculate this information at install time once and for all. For
        // system apps, we can probably assume that this information doesn't change
        // after the first boot scan. As things stand, we do lots of unnecessary work.

        // Give ourselves some initial paths; we'll come back for another
        // pass once we've determined ABI below.
        setNativeLibraryPaths(pkg);

        // We would never need to extract libs for forward-locked and external packages,
        // since the container service will do it for us. We shouldn't attempt to
        // extract libs from system app when it was not updated.
        if (pkg.isForwardLocked() || pkg.applicationInfo.isExternalAsec() ||
                ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 &&
                 !pkg.isUpdatedSystemApp())) {
            extractLibs = false;
        }

        final String nativeLibraryRootStr = pkg.applicationInfo.nativeLibraryRootDir;
        final boolean useIsaSpecificSubdirs = pkg.applicationInfo.nativeLibraryRootRequiresIsa;

        if (needCheckNativeLib) {
            NativeLibraryHelper.Handle handle = null;
            try {
                handle = NativeLibraryHelper.Handle.create(scanFile);
                // TODO(multiArch): This can be null for apps that didn't go through the
                // usual installation process. We can calculate it again, like we
                // do during install time.
                //
                // TODO(multiArch): Why do we need to rescan ASEC apps again ? It seems totally
                // unnecessary.
                final File nativeLibraryRoot = new File(nativeLibraryRootStr);

                // Null out the abis so that they can be recalculated.
                pkg.applicationInfo.primaryCpuAbi = null;
                pkg.applicationInfo.secondaryCpuAbi = null;
                if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_MULTIARCH) != 0) {
                    // Warn if we've set an abiOverride for multi-lib packages..
                    // By definition, we need to copy both 32 and 64 bit libraries for
                    // such packages.
                    if (pkg.cpuAbiOverride != null
                            && !NativeLibraryHelper.CLEAR_ABI_OVERRIDE.equals(pkg.cpuAbiOverride)) {
                        Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
                    }

                    int abi32 = PackageManager.NO_NATIVE_LIBRARIES;
                    int abi64 = PackageManager.NO_NATIVE_LIBRARIES;
                    if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                        if (extractLibs) {
                            abi32 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle,
                                    nativeLibraryRoot, Build.SUPPORTED_32_BIT_ABIS,
                                    useIsaSpecificSubdirs);
                        } else {
                            abi32 = NativeLibraryHelper.findSupportedAbi(handle,
                                    Build.SUPPORTED_32_BIT_ABIS);
                        }
                    }

                    if (abi32 < 0) {
                        if (abi32 != PackageManager.NO_NATIVE_LIBRARIES &&
                                abi32 != PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS) {
                            throw new PackageManagerException(abi32,
                                    "Error unpackaging 32 bit native libs for multiarch app.");
                        }
                    }

                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                        if (extractLibs) {
                            abi64 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle,
                                    nativeLibraryRoot, Build.SUPPORTED_64_BIT_ABIS,
                                    useIsaSpecificSubdirs);
                        } else {
                            abi64 = NativeLibraryHelper.findSupportedAbi(handle,
                                    Build.SUPPORTED_64_BIT_ABIS);
                        }
                    }

                    if (abi64 < 0) {
                        if (abi64 != PackageManager.NO_NATIVE_LIBRARIES &&
                                abi64 != PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS) {
                            throw new PackageManagerException(abi64,
                                    "Error unpackaging 64 bit native libs for multiarch app.");
                        }
                    }

                    if (abi64 >= 0) {
                        pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[abi64];
                    }

                    if (abi32 >= 0) {
                        final String abi = Build.SUPPORTED_32_BIT_ABIS[abi32];
                        if (abi64 >= 0) {
                            pkg.applicationInfo.secondaryCpuAbi = abi;
                        } else {
                            pkg.applicationInfo.primaryCpuAbi = abi;
                        }
                    }
                } else {
                    String[] abiList = (cpuAbiOverride != null) ?
                            new String[] { cpuAbiOverride } : Build.SUPPORTED_ABIS;

                    // Enable gross and lame hacks for apps that are built with old
                    // SDK tools. We must scan their APKs for renderscript bitcode and
                    // not launch them if it's present. Don't bother checking on devices
                    // that don't have 64 bit support.
                    boolean needsRenderScriptOverride = false;
                    if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && cpuAbiOverride == null &&
                            NativeLibraryHelper.hasRenderscriptBitcode(handle)) {
                        abiList = Build.SUPPORTED_32_BIT_ABIS;
                        needsRenderScriptOverride = true;
                    }

                    final int copyRet;
                    if (extractLibs) {
                        copyRet = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle,
                                nativeLibraryRoot, abiList, useIsaSpecificSubdirs);
                    } else {
                        copyRet = NativeLibraryHelper.findSupportedAbi(handle, abiList);
                    }

                    if (copyRet < 0 && copyRet != PackageManager.NO_NATIVE_LIBRARIES) {
                        throw new PackageManagerException(INSTALL_FAILED_INTERNAL_ERROR,
                                "Error unpackaging native libs for app, errorCode=" + copyRet);
                    }

                    if (copyRet >= 0) {
                        pkg.applicationInfo.primaryCpuAbi = abiList[copyRet];
                    } else if (copyRet == PackageManager.NO_NATIVE_LIBRARIES
                            && cpuAbiOverride != null) {
                        pkg.applicationInfo.primaryCpuAbi = cpuAbiOverride;
                    } else if (needsRenderScriptOverride) {
                        pkg.applicationInfo.primaryCpuAbi = abiList[0];
                    }
                }
            } catch (IOException ioe) {
                Slog.e(TAG, "Unable to get canonical file " + ioe.toString());
            } finally {
                IoUtils.closeQuietly(handle);
            }
        } else {
            pkg.applicationInfo.primaryCpuAbi = primaryCpuAbi;
            pkg.applicationInfo.secondaryCpuAbi = secondaryCpuAbi;
        }

        // Now that we've calculated the ABIs and determined if it's an internal app,
        // we will go ahead and populate the nativeLibraryPath.
        setNativeLibraryPaths(pkg);
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
        if (isFirstBoot() || !mUseAppScanList) {
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
                    Slog.d(TAG, "Failed to locate apk " + file.getPath());
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
        } else {
            // When Packages list is empty, please call super version of scanDirLI
            super.scanDirLI(dir, parseFlags, scanFlags, 0);
            return;
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
        dumpPeroidT();

        if (!mUseAppScanList) return;
        final int scanFlags = SCAN_NO_PATHS | SCAN_DEFER_DEX | SCAN_BOOTING | SCAN_INITIAL;
        Slog.i(TAG, " ********** LaterscanApp ****************");
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
                                Slog.i(TAG, "Sending broadcast for core package...:" + name);
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
                            Slog.i(TAG, "Sending BC Intent to stopped package: " + name);
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
                                Slog.i(TAG, "Sending broadcast for stopped package...:" + name);
                                am.broadcastIntent(null, bcIntent, null, null, 0, null, null,
                                        receiverPermission,
                                        android.app.AppOpsManager.OP_NONE, extras,
                                        false, false, uidds);
                            }
                        }
                    }
                } catch(RemoteException e) {
                    Slog.e(TAG, "Exception while sending broadcast to stopped package");
                }
                mSystemAppList.clear();
                mPrivAppList.clear();
                mDataAppList.clear();
                mVendorAppList.clear();
                mSystemAppList = null;
                mPrivAppList = null;
                mVendorAppList = null;
                mDataAppList = null;

                cleanUselessPkgInfo();
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
                            false /* force dexopt */, false /* defer dexopt */,
                            false /* boot complete */);
            }
        }
    }
}

class AutoSQLiteHelper extends SQLiteOpenHelper {
    private int dbVersion;
    private String dbName;
    private String tableName;
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String[] COLUMNS = {KEY,VALUE};
    private static final String TAG = "AutoSQlite";

    public AutoSQLiteHelper(Context context, String dbName, int dbVersion, String tableName) {
        super(context, dbName, null, dbVersion);
        this.dbName = dbName;
        this.dbVersion = dbVersion;
        this.tableName = tableName;
        SQLiteDatabase db = getWritableDatabase();
        Log.i(TAG, "AutoSQLiteHelper : " + dbName + " " + tableName);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Create Table, Doesn't exist earlier....");
        createTable(db, tableName);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        // create fresh table
        onCreate(db);
    }

    public void writeToDb(Pair<String, String> data) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        if (data != null) {
            values.put(KEY, data.first);
            values.put(VALUE, data.second);
        }

        if (db != null) {
            db.insert(tableName, null, values);
            db.close();
        } else {
            Log.e(TAG,"No writable database");
        }
    }

    public List<String> readFromDb(Pair<String, String> data) {
        List<String> dbList = new ArrayList<String>();
        if (data == null) {
            Log.e(TAG,"DbObject is null");
            return null;
        }

        SQLiteDatabase db = getReadableDatabase();
        if (db != null) {
            Cursor cursor = db.query(tableName,
                    new String[] {KEY,VALUE},
                    KEY + "=?",
                    new String[] { data.first },
                    null,
                    null,
                    null,
                    null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToNext();
                do {
                    Log.i(TAG, "Value read from db is: " + cursor.getString(1));
                    dbList.add(cursor.getString(1));
                } while (cursor.moveToNext());
                cursor.close();
            }
            return dbList;
        }
        return null;
    }

    public int getProfilesRowCount() {
        String query = "SELECT  * FROM conf_table";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        int count = cursor.getCount();
        cursor.close();
        Log.i(TAG, "Row count is : " + count);
        return count;
    }

    private void createTable(SQLiteDatabase db, String tableName) {
        String CREATE_TABLE = "CREATE TABLE " + tableName + "( " +
                "key TEXT, " + "value TEXT )";
        // create table
        db.execSQL(CREATE_TABLE);
    }

    public void deleteEntry(Pair<String, String> data) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(tableName,
                KEY + " = ?",
                new String[] { data.first });
        db.close();
    }
}
