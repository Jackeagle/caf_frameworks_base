/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
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

package com.android.server;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.media.tv.TvInputManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.webkit.WebViewFactory;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.android.internal.R;
import com.android.internal.os.SamplingProfilerIntegration;
import com.android.server.am.ActivityManagerService;
import com.android.server.clipboard.ClipboardService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.fingerprint.FingerprintService;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.input.InputManagerService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.pm.PackageManagerService;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.tv.TvInputManagerService;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.camera.CameraService;
import dalvik.system.VMRuntime;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
import java.io.File;

public class AutoSystemServer extends SystemServer{
    private static final String TAG = "AutoSystemServer";
    BroadcastReceiver mDefServiceReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                Slog.d(TAG, "***** Calling Deferred Services ******");
                startDeferredCoreServices();
                startDeferredOtherServices();
                mSystemServiceManager.startBootPhase(SystemService.
                        PHASE_SERVICES_DEFER_COMPLETED);
            }
        }
    };

    @Override
    protected void registerReceiverForDeferredServices() {
        Slog.i(TAG,"*****  Start other ref services for automotive *****");
        if (null != mSystemContext) {
            mSystemContext.registerReceiver(mDefServiceReceiver,
                    new IntentFilter(Intent.ACTION_BOOT_COMPLETED));
        }
    }

    protected void startDeferredCoreServices() {
        // Tracks whether the updatable WebView is in a ready state
        // and watches for update installs.
        mSystemServiceManager.startService(WebViewUpdateService.class);
    }

    protected void startDeferredOtherServices() {
        final Context context = mSystemContext;
        boolean disableNonCoreServices = SystemProperties.getBoolean(
                "config.disable_noncore", false);
        boolean disableNetwork = SystemProperties.getBoolean(
                "config.disable_network", false);
        boolean disableMedia = SystemProperties.getBoolean(
                "config.disable_media", false);
        boolean disableLocation = SystemProperties.getBoolean(
                "config.disable_location", false);
        boolean disableAtlas = SystemProperties.getBoolean(
                "config.disable_atlas", false);
        boolean disableNetworkTime = SystemProperties.getBoolean(
                "config.disable_networktime", false);

        Slog.i(TAG, "WebViewFactory preparation");
        WebViewFactory.prepareWebViewInSystemServer();

        //Starting Camera Service.
        Slog.i(TAG, "Camera Service");
        mSystemServiceManager.startService(CameraService.class);

        //Starting vibrator.
        Slog.i(TAG, "Vibrator Service");
        mVibrator = new VibratorService(context);
        ServiceManager.addService("vibrator", mVibrator);
        try {
            mVibrator.systemReady();
        } catch (Throwable e) {
            reportWtf("making Vibrator Service ready", e);
        }

        Slog.i(TAG, "Consumer IR Service");
        mConsumerIr = new ConsumerIrService(context);
        ServiceManager.addService(Context.CONSUMER_IR_SERVICE, mConsumerIr);

        if (!disableNonCoreServices) {
            try {
                Slog.i(TAG, "Clipboard Service");
                ServiceManager.addService(Context.CLIPBOARD_SERVICE,
                        new ClipboardService(context));
            } catch (Throwable e) {
                reportWtf("starting Clipboard Service", e);
            }
        }

        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_ETHERNET) ||
                mPackageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);
        }

        mSystemServiceManager.startService(DeviceStorageMonitorService.class);

        //Starting Country Detector Service.
        if (!disableLocation) {
            try {
                Slog.i(TAG, "Country Detector");
                mCountryDetector = new CountryDetectorService(context);
                ServiceManager.addService(Context.COUNTRY_DETECTOR,mCountryDetector);
            } catch (Throwable e) {
                reportWtf("starting Country Detector", e);
            }
        }

        //starting DockObserver Service.
        if (!disableNonCoreServices) {
            mSystemServiceManager.startService(DockObserver.class);
        }

        //starting Serial  Service.
        if (!disableNonCoreServices) {
            try {
                Slog.i(TAG, "Serial Service");
                // Serial port support
                mSerial = new SerialService(context);
                ServiceManager.addService(Context.SERIAL_SERVICE, mSerial);
            } catch (Throwable e) {
                Slog.e(TAG, "Failure starting SerialService");
            }
        }

        //starting Backup Manager & Voice Recognition Service.
        if (!disableNonCoreServices) {
            if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_BACKUP)) {
                mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);
            }
            if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_VOICE_RECOGNIZERS)) {
                mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
            }
        }

        //Starting DiskStats service.
        try {
            Slog.i(TAG, "DiskStats Service");
            ServiceManager.addService("diskstats",new DiskStatsService(context));
        } catch (Throwable e) {
            reportWtf("starting DiskStats Service", e);
        }

        //Starting SamplingProfiler service.
        try {
            // need to add this service even if
            // SamplingProfilerIntegration.isEnabled()
            // is false, because it is this service that detects system property
            // change and
            // turns on SamplingProfilerIntegration. Plus, when sampling
            // profiler doesn't work,
            // there is little overhead for running this service.
            Slog.i(TAG, "SamplingProfiler Service");
            ServiceManager.addService("samplingprofiler",new SamplingProfilerService(context));
        } catch (Throwable e) {
            reportWtf("starting SamplingProfiler Service", e);
        }

        //Starting NetworkTimeUpdateService.
        if (!disableNetwork && !disableNetworkTime) {
            try {
                Slog.i(TAG, "NetworkTimeUpdateService");
                mNetworkTimeUpdater = new NetworkTimeUpdateService(context);
            } catch (Throwable e) {
                reportWtf("starting NetworkTimeUpdate service", e);
            }
        }

        //Starting CommonTimeManagement Service.
        if (!disableMedia) {
            try {
                Slog.i(TAG, "CommonTimeManagementService");
                mCommonTimeMgmtService = new CommonTimeManagementService(context);
                ServiceManager.addService("commontime_management",mCommonTimeMgmtService);
            } catch (Throwable e) {
                reportWtf("starting CommonTimeManagementService service", e);
            }
        }
        if (!disableNetwork) {
            try {
                Slog.i(TAG, "CertBlacklister");
                CertBlacklister blacklister = new CertBlacklister(context);
            } catch (Throwable e) {
                reportWtf("starting CertBlacklister", e);
            }
        }

        //Starting DreamManager Service.
        if (!disableNonCoreServices) {
            mSystemServiceManager.startService(DreamManagerService.class);
        }

        //Starting AssetAtlas Service.
        if (!disableNonCoreServices) {
            try {
                Slog.i(TAG, "Assets Atlas Service");
                mAtlas = new AssetAtlasService(context);
                ServiceManager.addService(AssetAtlasService.ASSET_ATLAS_SERVICE, mAtlas);
            } catch (Throwable e) {
                reportWtf("starting AssetAtlasService", e);
            }
        }

        //Starting GraphicsStatsService.
        if (!disableNonCoreServices) {
            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE,
                    new GraphicsStatsService(mSystemContext));
        }

        //Starting printManager service.
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_PRINTING)) {
            mSystemServiceManager.startService(PRINT_MANAGER_SERVICE_CLASS);
        }

        //Starting TV Input Manager Service.
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_TV)) {
            mSystemServiceManager.startService(TvInputManagerService.class);
        }

        //Starting HDMI controller service.
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_HDMI_CEC)) {
            mSystemServiceManager.startService(HdmiControlService.class);
        }

        //Starting MMS Service Broker.
        mMmsService = mSystemServiceManager.startService(MmsServiceBroker.class);

        final CountryDetectorService countryDetectorF = mCountryDetector;
        final NetworkTimeUpdateService networkTimeUpdaterF = mNetworkTimeUpdater;
        final CommonTimeManagementService commonTimeMgmtServiceF = mCommonTimeMgmtService;
        final AssetAtlasService atlasF = mAtlas;
        final MmsServiceBroker mmsServiceF = mMmsService;
        // We now tell the activity manager it is okay to run third party
        // code. It will call back into us once it has gotten to the state
        // where third party code can really run (but before it has actually
        // started launching the initial applications), for us to complete our
        // initialization.
        mActivityManagerService.systemReady(new Runnable() {
            @Override
            public void run() {
                try {
                    if (countryDetectorF != null)
                         countryDetectorF.systemRunning();
                } catch (Throwable e) {
                         reportWtf("Notifying CountryDetectorService running", e);
                }
                try {
                    if (networkTimeUpdaterF != null) {
                         networkTimeUpdaterF.systemRunning();
                    }
                } catch (Throwable e) {
                    reportWtf("Notifying NetworkTimeService running", e);
                }
                try {
                    if (commonTimeMgmtServiceF != null) {
                        commonTimeMgmtServiceF.systemRunning();
                    }
                } catch (Throwable e) {
                    reportWtf("Notifying CommonTimeManagementService running",e);
                }
                try {
                    if (atlasF != null)
                        atlasF.systemRunning();
                } catch (Throwable e) {
                        reportWtf("Notifying AssetAtlasService running", e);
                }
                try {
                    if (mmsServiceF != null)
                         mmsServiceF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying MmsService running", e);
                }
            }
        });
    }

    @Override
    protected void startWebViewUpdateService() {
    }

    @Override
    protected void startCameraService() {
    }

    @Override
    protected void startVibratorService() {
    }

    @Override
    protected void startIRService() {
    }

    @Override
    protected void startClipboardService() {
    }

    @Override
    protected void startEthernetService() {
    }

    @Override
    protected void startDeviceStorageMonitorService() {
    }

    @Override
    protected void startCountryDetector() {
    }

    @Override
    protected void startDockObserver() {
    }

    @Override
    protected void startSerialService() {
    }

    @Override
    protected void startBackupManagerService() {
    }

    @Override
    protected void startVoiceRecognizerService() {
    }

    @Override
    protected void startDiskStatsService() {
    }

    @Override
    protected void startSamplingProfilerService() {
    }

    @Override
    protected void startNetworkTimeUpdateService() {
    }

    @Override
    protected void startCommonTimeManagementService() {
    }

    @Override
    protected void startCertificateBlackLister() {
    }

    @Override
    protected void startDreamManagerService() {
    }

    @Override
    protected void startAssetAtlastService() {
    }

    @Override
    protected void startPrintManagerService() {
    }

    @Override
    protected void startHDMIControllerService() {
    }

    @Override
    protected void startTVInputManagerService() {
    }

    @Override
    protected void startMMSServiceBroker() {
    }

    @Override
    protected void makeVibratorReady() {
    }

    @Override
    protected void makeWebViewFactoryPreparation() {
    }

    @Override
    protected void makeCountryDetectorReady(CountryDetectorService countryDetectorF) {
    }

    @Override
    protected void makeNetworkTimeUpdateReady(NetworkTimeUpdateService networkTimeUpdaterF) {
    }

    @Override
    protected void makeCommonTimeManagementReady(CommonTimeManagementService
            commonTimeMgmtServiceF) {
    }

    @Override
    protected void makeAssetAtlastReady(AssetAtlasService atlasF) {
    }

    @Override
    protected void makeMmsReady(MmsServiceBroker mmsServiceF) {
    }

    @Override
    protected void startGraphicStatsService() {
    }
}
