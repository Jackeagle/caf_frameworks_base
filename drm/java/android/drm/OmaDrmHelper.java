/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package android.drm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.drm.DrmRights;
import android.drm.DrmStore.Action;
import android.drm.DrmStore.RightsStatus;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.R;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Utility APIs for OMA DRM v1 supports.
 *
 * @hide
 */
public class OmaDrmHelper {

    public static final String TAG = "OmaDrm/OmaDrmHelper";

    /** The MIME type of special DRM files */
    public static final String MIMETYPE_DRM_MESSAGE = "application/vnd.oma.drm.message";
    public static final String MIMETYPE_DRM_CONTENT = "application/vnd.oma.drm.content";
    public static final String DRM_MIMETYPE_RIGHTS_XML = "application/vnd.oma.drm.rights+xml";
    public static final String DRM_MIMETYPE_RIGHTS_WXML = "application/vnd.oma.drm.rights+wbxml";

    private static final String OMA_DRM_SCHEMA = "omadrm://";
    private static final String ACTION_STRING_COPY_DATA = "copy";
    private static final String FAKE_ACTION = "0";

    /** The extensions of DRM files */
    public static final String EXTENSION_DM = ".dm";
    public static final String EXTENSION_FL = ".fl";
    public static final String EXTENSION_DCF = ".dcf";

    /**
     * Package name of the OmaDrmEngineApp which is basically used for manage
     * resources like strings
     */
    private static String OMA_DRM_ENGINE_APP = "com.oma.drm";

    public static final String MIDI_MIME_TYPES[] = { "audio/mid", "audio/midi",
            "audio/x-mid", "audio/x-midi", "audio/sp-mid", "audio/sp-midi",
            "audio/imy", "audio/imelody" };

    private static boolean sIsOmaDrmEnabled = false;

    static {
        File file1 = new File("/vendor/lib/drm/libomadrmengine.so");
        File file2 = new File("/vendor/lib64/drm/libomadrmengine.so");
        File file3 = new File("/system/lib/drm/libomadrmengine.so");
        File file4 = new File("/system/lib64/drm/libomadrmengine.so");
        if (file1.exists() || file2.exists() || file3.exists()
                || file4.exists()) {
            sIsOmaDrmEnabled = true;
            File fileomadrmhelper1 = new File(
                    "/system/lib/libomadrmhelper_jni.so");
            File fileomadrmhelper2 = new File(
                    "/system/lib64/libomadrmhelper_jni.so");
            if (fileomadrmhelper1.exists() || fileomadrmhelper2.exists()) {
                System.loadLibrary("omadrmhelper_jni");
            }
        }
    }

    /**
     * Defines the drm delivery methods. This should be in sync with svc_drm.h
     *
     * @hide
     */
    public static class DrmDeliveryType {
        /**
         * Forward_lock
         */
        public static final int FORWARD_LOCK = 0x01;
        /**
         * Combined delivery
         */
        public static final int COMBINED_DELIVERY = 0x02;
        /**
         * Separate delivery
         */
        public static final int SEPARATE_DELIVERY = 0x03;
        /**
         * Separate delivery but DCF is forward-lock
         */
        public static final int SEPARATE_DELIVERY_FL = 0x04;
    }

    /**
     * Get Strings from OmaDrmEngine App
     *
     * @hide
     */
    public static class OmaDrmStrings {

        public static final String PLAY_UNLIMITED = "play_unlimited";

        public static final String PLAY_PERMISSION = "play_permission";

        public static final String DISPLAY_PERMISSION = "display_permission";

        public static final String NO_RIGHTS = "no_rights";

        public static final String LICENSE_INFO = "license_information";

        public static final String COUNT = "count";

        public static final String INTERVAL = "interval";

        public static final String SECONDS = "seconds";

        public static final String START_DATE = "start_date";

        public static final String END_DATE = "end_date";

        public static final String LICENSE_EXPIRED = "license_expired";

        public static final String LICENSE_RENEW_NOT_POSSIBLE = "license_renew_not_possible";

        public static final String LICENSE_RENEW = "license_renew";

        public static final String ACTIVITY_NOT_FOUND = "activity_not_found";

        public static final String SHARE_NOT_ALLOWED = "share_not_allowed";

        public static String getString(Context context, String stringResource) {
            final String packageName = OMA_DRM_ENGINE_APP;
            CharSequence drmString = null;
            try {
                PackageManager manager = context.getPackageManager();
                Resources DrmResources = manager
                        .getResourcesForApplication(packageName);
                int resourceId = DrmResources.getIdentifier(OMA_DRM_ENGINE_APP
                        + ":string/" + stringResource, null, null);
                if (0 != resourceId) {
                    drmString = manager.getText(OMA_DRM_ENGINE_APP, resourceId,
                            null);
                }
                if (drmString != null) {
                    return drmString.toString();
                }
                return null;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    /**
     * Defines OmdDrmConstraints
     *
     * @hide
     */
    public static class OmdDrmConstraints {

        public static final String COUNT = "count";

        public static final String START_DATE_TIME = "startDateTime";

        public static final String END_DATE_TIME = "endDateTime";

        public static final String INTERVAL = "interval";

    }

    /**
     * @hide Wrapper to interact with OmaDrmEngine.
     */
    public static class DrmManagerClientWrapper extends DrmManagerClient {
        private static final String TAG = "DrmManagerClientWrapper";
        private static final String OMA_DRM_SCHEMA = "omadrm://";
        private static final String ACTION_STRING_RIGHTS = "rights";
        private static final String ACTION_STRING_METADATA = "metadata";
        private static final String ACTION_STRING_CONSTRAINTS = "constraints";
        private static final String FAKE_ACTION = "0";

        public DrmManagerClientWrapper(Context context) {
            super(context);
        }

        @Override
        public ContentValues getConstraints(String path, int action) {
            Log.i(TAG, "getConstraints of " + path);
            String actionPath = OMA_DRM_SCHEMA + ACTION_STRING_CONSTRAINTS
                    + action + ":" + path;

            if (null == path || path.equals("")) {
                throw new IllegalArgumentException(
                        "Given path should be non null");
            }

            String info = null;

            FileInputStream is = null;
            try {
                FileDescriptor fd = null;
                File file = new File(path);
                if (file.exists()) {
                    is = new FileInputStream(file);
                    fd = is.getFD();
                }
                info = getInternalInfo(actionPath, fd);
            } catch (IOException ioe) {
                Log.e(TAG, "getConstraints failed! Exception : " + ioe);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG,
                                "getConstraints failed to close stream! Exception : "
                                        + e);
                    }
                }
            }
            Log.i(TAG, "Got Constraints info! info = " + info);
            return parseConstraints(info);
        }

        @Override
        public int checkRightsStatus(String path, int action) {
            Log.i(TAG, "checkRightsStatus of " + path);
            String actionPath = OMA_DRM_SCHEMA + ACTION_STRING_RIGHTS + action
                    + ":" + path;

            if (null == path || path.equals("")) {
                throw new IllegalArgumentException(
                        "Given path should be non null");
            }

            String info = null;

            FileInputStream is = null;
            try {
                FileDescriptor fd = null;
                File file = new File(path);
                if (file.exists()) {
                    is = new FileInputStream(file);
                    fd = is.getFD();
                }
                info = getInternalInfo(actionPath, fd);
            } catch (IOException ioe) {
                Log.e(TAG, "checkRightsStatus failed! Exception : " + ioe);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG,
                                "checkRightsStatus failed to close stream! Exception : "
                                        + e);
                    }
                }
            }
            Log.i(TAG, "Got RightsStatus info! info = " + info);
            return parseRightsStatus(info);
        }

        @Override
        public ContentValues getMetadata(String path) {
            Log.i(TAG, "getMetadata of " + path);
            String actionPath = OMA_DRM_SCHEMA + ACTION_STRING_METADATA
                    + FAKE_ACTION + ":" + path;

            if (null == path || path.equals("")) {
                throw new IllegalArgumentException(
                        "Given path should be non null");
            }

            String info = null;

            FileInputStream is = null;
            try {
                FileDescriptor fd = null;
                File file = new File(path);
                if (file.exists()) {
                    is = new FileInputStream(file);
                    fd = is.getFD();
                }
                info = getInternalInfo(actionPath, fd);
            } catch (IOException ioe) {
                Log.e(TAG, "getMetadata failed! IOException : " + ioe);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG,
                                "getMetadata failed to close stream! Exception : "
                                        + e);
                    }
                }
            }
            Log.i(TAG, "Got Metadata info! info = " + info);
            return parseMetadata(info);
        }

        private ContentValues parseConstraints(String constraints) {
            ContentValues contentValues = new ContentValues();
            String attrs[] = TextUtils.split(constraints, "\n");
            for (String attr : attrs) {
                String values[] = TextUtils.split(attr, "\t");
                if (values.length > 0 && !TextUtils.isEmpty(values[0])) {
                    contentValues.put(values[0], values[1]);
                }
            }
            return contentValues;
        }

        private ContentValues parseMetadata(String message) {
            ContentValues contentValues = new ContentValues();
            if (!TextUtils.isEmpty(message)) {
                String attrs[] = TextUtils.split(message, "\n");
                for (String attr : attrs) {
                    String values[] = TextUtils.split(attr, "\t");
                    if (values[0].equals("DRM-TYPE")) {
                        if (values[1] != null) {
                            contentValues.put("DRM-TYPE",
                                    Integer.parseInt(values[1]));
                        }
                    }
                    if (values[0].equals("Rights-Issuer")) {
                        if (values[1] != null) {
                            contentValues.put("Rights-Issuer", values[1]);
                        }
                    }
                }
            }
            return contentValues;
        }

        private int parseRightsStatus(String message) {
            if (!TextUtils.isEmpty(message)) {
                return Integer.parseInt(message.trim());
            }
            return -1;
        }
    }

    /**
     * @hide Internal DRM api
     */
    public static final int checkRightsStatus(Context context, String path,
            String mimeType) {
        int status = -1;
        if (isDrmFile(path)) {
            DrmManagerClientWrapper drmClient = null;
            try {
                drmClient = new DrmManagerClientWrapper(context);
                status = checkRightsStatus(drmClient, path, mimeType);
            } catch (Exception e) {
                Log.e(TAG, "Exception while checking rights. Exception : " + e);
            } finally {
                if (drmClient != null) {
                    drmClient.release();
                }
            }
        }
        return status;
    }

    /**
     * @hide Internal DRM api
     */
    public static final int checkRightsStatus(
            DrmManagerClientWrapper drmClient, String path, String mimeType) {
        int status = -1;
        if (isDrmFile(path)) {
            if (!TextUtils.isEmpty(mimeType) && isSupportedMimeType(mimeType)) {
                if (mimeType.startsWith("image")) {
                    status = drmClient.checkRightsStatus(path, Action.DISPLAY);
                } else {
                    status = drmClient.checkRightsStatus(path, Action.PLAY);
                }
            } else {
                mimeType = drmClient.getOriginalMimeType(path);
                if (isSupportedMimeType(mimeType)) {
                    if (mimeType.startsWith("image")) {
                        status = drmClient.checkRightsStatus(path,
                                Action.DISPLAY);
                    } else {
                        status = drmClient.checkRightsStatus(path, Action.PLAY);
                    }
                }
            }
        }

        return status;
    }

    /**
     * @hide Internal DRM api
     */
    public static boolean consumeDrmRights(String path, String mimeType) {
        if (!isDrmFile(path)) {
            Log.e(TAG, "Could not consume rights from non-drm file. path = "
                    + path);
            return false;
        }

        return consumeDrmRights(path);
    }

    /**
     * @hide Internal DRM api
     */
    public static BitmapRegionDecoder createBitmapRegionDecoder(String path,
            boolean isShareable) {
        if (!isDrmFile(path)) {
            Log.e(TAG, "Could not decode non-drm file. path = " + path);
            return null;
        }

        try {
            byte[] data = getDrmDecryptedData(path);
            if (data != null) {
                return BitmapRegionDecoder.newInstance(data, 0, data.length,
                        isShareable);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not decode non-drm file. error = " + e);

        }
        return null;
    }

    /**
     * @hide Internal DRM api
     */
    public static Bitmap getBitmap(String path) {
        return getBitmap(path, null);
    }

    /**
     * @hide Internal DRM api
     */
    public static Bitmap getBitmap(String path, BitmapFactory.Options options) {
        if (!isDrmFile(path)) {
            Log.e(TAG, "Could not decode non-drm file. path = " + path);
            return null;
        }
        if (options == null) {
            options = new Options();
            options.inPreferredConfig = Config.ARGB_8888;
        }

        byte[] data = getDrmDecryptedData(path);
        try {
            if (data != null) {
                return BitmapFactory.decodeByteArray(data, 0, data.length);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not decode non-drm file. error = " + e);

        }
        return null;
    }

    /**
     * @hide Internal DRM api
     */
    public static byte[] getDrmImageBytes(String path) {
        if (!isDrmFile(path)) {
            Log.e(TAG, "Could not decode non-drm file. path = " + path);
            return null;
        }

        try {
            return getDrmDecryptedData(path);
        } catch (Exception e) {
            Log.w(TAG, "Could not decode non-drm file. Exception : " + e);
            return null;
        }
    }

    /**
     * @hide Internal DRM api
     */
    public static final String getFilePath(Context context, Uri uri) {
        String path = null;
        Cursor cursor = null;
        if (isDrmFile(uri.toString())) {
            // uri is a direct drm file path, so return as it is.
            return uri.toString();
        }

        if (isOmaDrmEnabled()) {
            try {
                cursor = context.getContentResolver().query(uri,
                        new String[] { MediaStore.Files.FileColumns.DATA },
                        null, null, null);
                if (cursor.moveToFirst()) {
                    path = cursor.getString(0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not get drm file path : Exception : " + e);
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        return path;
    }

    /**
     * @hide Internal DRM api
     */
    public static final String getFilePath(Context context, Cursor cursor) {
        String path = null;
        if (isOmaDrmEnabled()) {
            try {
                path = cursor
                        .getString(cursor
                                .getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
            } catch (Exception e) {
                Log.e(TAG, "Could not get drm file path : Exception : " + e);
            }
        }
        return path;
    }

    /**
     * @hide Internal DRM api
     */
    public static final String getDrmMidiFilePath(Context context, String path,
            String cachePath, String mimeType) {
        if (isDrmFile(path) && context != null) {
            if (isDrmMidiFile(context, path, mimeType)) {
                byte[] data = getDrmDecryptedData(path);
                FileOutputStream out = null;
                if (data != null) {
                    // CheckHere
                    // consumeDrmRightsNonBlocking(path, mimeType);
                    try {
                        File cacheFile = new File(cachePath, "drmmidifile.dm");
                        if (cacheFile.exists()) {
                            cacheFile.delete();
                        }
                        out = new FileOutputStream(cacheFile);
                        out.write(data, 0, data.length);
                        return cacheFile.getAbsolutePath();
                    } catch (Exception e) {
                        Log.e(TAG,
                                "Exception while checking midi file path! Exception = "
                                        + e);
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (Exception e) {
                            Log.e(TAG,
                                    "Exception while closing midi file! Exception = "
                                            + e);
                        }
                    }
                }
            }
        }
        return path;
    }

    /**
     * @hide Internal DRM api
     */
    public static final String getOriginalMimeType(Context context, String path) {
        String mime = "";
        if (isDrmFile(path)) {
            DrmManagerClientWrapper client = new DrmManagerClientWrapper(
                    context);
            try {
                if (client.canHandle(path, null)) {
                    mime = client.getOriginalMimeType(path);
                }
            } finally {
                if (client != null) {
                    client.release();
                }
            }
        }
        return mime;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmMimeType(String mimeType) {
        if (!TextUtils.isEmpty(mimeType)) {
            if (MIMETYPE_DRM_MESSAGE.equals(mimeType)
                    || MIMETYPE_DRM_CONTENT.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmMidiFile(Context context, String path,
            String mimeType) {

        if (isDrmFile(path) && context != null) {
            if (TextUtils.isEmpty(mimeType)) {
                mimeType = getOriginalMimeType(context, path);
            }

            for (String mime : MIDI_MIME_TYPES) {
                if (mime.equals(mimeType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmCD(String path) {
        if (isDrmFile(path)) {
            if (path.endsWith(EXTENSION_DM)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmFile(String path) {
        if (isOmaDrmEnabled()) {
            if (!TextUtils.isEmpty(path)) {
                if (path.endsWith(EXTENSION_FL) || path.endsWith(EXTENSION_DM)
                        || path.endsWith(EXTENSION_DCF)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmFL(String path) {
        if (isDrmFile(path)) {
            if (path.endsWith(EXTENSION_FL)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmSD(String path) {
        if (isDrmFile(path)) {
            if (path.endsWith(EXTENSION_DCF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmFLBlocking(Context context, String path) {
        if (context != null && isDrmFile(path)) {
            DrmManagerClientWrapper client = new DrmManagerClientWrapper(
                    context);
            try {
                ContentValues metadada = client.getMetadata(path);
                if (metadada != null) {
                    int drmType = metadada.getAsInteger("DRM-TYPE");
                    if (drmType == DrmDeliveryType.FORWARD_LOCK) {
                        return true;
                    }
                }
            } finally {
                if (client != null) {
                    client.release();
                }
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmCDBlocking(Context context, String path) {
        if (context != null && isDrmFile(path)) {
            DrmManagerClientWrapper client = new DrmManagerClientWrapper(
                    context);
            try {
                ContentValues metadada = client.getMetadata(path);
                if (metadada != null) {
                    int drmType = metadada.getAsInteger("DRM-TYPE");
                    if (drmType == DrmDeliveryType.COMBINED_DELIVERY) {
                        return true;
                    }
                }
            } finally {
                if (client != null) {
                    client.release();
                }
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isDrmSDBlocking(Context context, String path) {
        if (context != null && isDrmFile(path)) {
            DrmManagerClientWrapper client = new DrmManagerClientWrapper(
                    context);
            try {
                ContentValues metadada = client.getMetadata(path);
                if (metadada != null) {
                    int drmType = metadada.getAsInteger("DRM-TYPE");
                    if (drmType == DrmDeliveryType.SEPARATE_DELIVERY) {
                        return true;
                    }
                }
            } finally {
                if (client != null) {
                    client.release();
                }
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isOmaDrmEnabled() {
        return sIsOmaDrmEnabled;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isLicenseableDrmFile(String path) {
        if (isDrmFile(path)
                && (path.endsWith(EXTENSION_DM) || path.endsWith(EXTENSION_DCF))) {
            return true;
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isShareableDrmFile(String path) {
        if (isDrmFile(path) && !path.endsWith(EXTENSION_DCF)) {
            return false;
        }
        return true;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean isSupportedMimeType(String mime) {
        if (!TextUtils.isEmpty(mime)) {
            if (mime.startsWith("image/") || mime.startsWith("audio/")
                    || mime.startsWith("video/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @hide Internal DRM api
     */
    public static final void manageDrmLicense(Context context, String path,
            String mimeType) {
        if (isDrmFile(path)) {
            if (validateLicense(context, path, mimeType)) {
                consumeDrmRights(path, mimeType);
            }
        }
    }

    /**
     * @hide Internal DRM api
     */
    public static final void manageDrmLicense(final Context context,
            Handler handler, final String path, final String mimeType) {
        if (isDrmFile(path)) {
            if (handler != null) {
                // Start consume right thread 1s delayed,
                // because, let animation or transition complete smoothly,
                // then start the blocking operation.
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (isDrmFile(path)) {
                            if (validateLicense(context, path, mimeType)) {
                                consumeDrmRights(path, mimeType);
                            }
                        }
                    }
                }, 1000);
            } else {
                manageDrmLicense(context, path, mimeType);
            }
        }
    }

    /**
     * @hide Internal DRM api
     */
    public static final void showDrmInfo(Context context, String path) {
        if (isDrmFile(path)) {
            showProperties(context, path, null);
        }
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean validateLicense(Context context, String path,
            String mimeType, DialogInterface.OnClickListener okListener,
            DialogInterface.OnClickListener cancelListener) {
        boolean result = true;
        DrmManagerClientWrapper drmClient = null;

        if (isDrmFile(path)) {
            File f = new File(path);
            if (!f.exists()) {
                Log.w(TAG,
                        "Cannot validateLicense! Drm file does not exist in the location : "
                                + path);
                return false;
            }
            try {
                drmClient = new DrmManagerClientWrapper(context);
                int status = checkRightsStatus(drmClient, path, mimeType);
                if (RightsStatus.RIGHTS_VALID != status) {
                    ContentValues values = drmClient.getMetadata(path);
                    String address = values.getAsString("Rights-Issuer");
                    buyLicense(context, address, okListener, cancelListener);
                    Log.w(TAG, "Drm License expared! can not proceed ahead");
                    result = false;
                }
            } catch (Exception e) {
                Log.e(TAG,
                        "Exception while valicating drm license. Exception : "
                                + e);
            } finally {
                if (drmClient != null) {
                    drmClient.release();
                }
            }
        }

        return result;
    }

    /**
     * @hide Internal DRM api
     */
    public static final boolean validateLicense(Context context, String path,
            String mimeType) {
        return validateLicense(context, path, mimeType, null, null);
    }

    public static final boolean copyData(DrmManagerClient client, byte[] bytes,
            FileDescriptor fd) {
        if (bytes != null) {
            return copyData(client, new String(bytes), fd);
        }
        return false;
    }

    public static final boolean copyData(Context context, String path,
            FileDescriptor fd) {
        if (isDrmFile(path) && fd != null) {
            DrmManagerClientWrapper client = new DrmManagerClientWrapper(
                    context);
            try {
                Log.e(TAG, "Copy data called");
                return copyData(client, path, fd);
            } finally {
                if (client != null) {
                    client.release();
                }
            }
        }
        return false;
    }

    public static final boolean copyData(DrmManagerClient client, String path,
            FileDescriptor fd) {
        if (isDrmFile(path) && fd != null && client != null) {
            Log.e(TAG, "Copy data called");
            String actionPath = OMA_DRM_SCHEMA + ACTION_STRING_COPY_DATA
                    + FAKE_ACTION + ":" + path;

            if (null == path || path.equals("")) {
                throw new IllegalArgumentException(
                        "Given path should be non null");
            }

            String info = null;

            try {
                info = client.getInternalInfo(actionPath, fd);
            } catch (Exception e) {
                Log.e(TAG, "copy data failed! Exception : " + e);
            }

            if (!TextUtils.isEmpty(info)) {
                if (info.equals("true")) {
                    Log.i(TAG, "Copy drm data success");
                    return true;
                } else {
                    Log.i(TAG, "Copy drm data failed!");
                }
            }
        }
        return false;
    }

    /**
     * @hide Internal Drm API
     */
    public static boolean consumeDrmRights(String path) {
        FileInputStream is = null;
        boolean result = false;
        try {
            FileDescriptor fd = null;
            File file = new File(path);
            if (file.exists()) {
                is = new FileInputStream(file);
                fd = is.getFD();
            }
            result = nativeConsumeDrmRights(fd);
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to decode drm file: " + ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to close drm file: " + e);
                }
            }
        }
        return result;
    }

    /**
     * @hide Internal Drm API
     */
    public static boolean consumeDrmRightsNonBlocking(final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                consumeDrmRights(path);
            }
        }, "consumeDrmRightsNonBlocking");
        return true;
    }

    /**
     * @hide Internal Drm API
     */
    public static byte[] getDrmDecryptedData(String path) {
        if (!isOmaDrmEnabled()) {
            return null;
        }
        FileInputStream is = null;
        byte[] result = null;
        try {
            FileDescriptor fd = null;
            File file = new File(path);
            if (file.exists()) {
                is = new FileInputStream(file);
                fd = is.getFD();
            }
            result = nativeGetDrmDecryptedData(fd);
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to decode drm file: " + ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to close drm file: " + e);
                }
            }
        }
        return result;
    }

    /**
     * @hide Internal Drm API
     */
    public static final void updateDrmFileTitle(Context context, Uri uri,
            String filePath) {
        if (isDrmFile(getFilePath(context, Uri.parse(filePath)))) {
            File f = new File(getFilePath(context, Uri.parse(filePath)));
            String name = f.getName();
            if (isDrmFile(name)) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Files.FileColumns.TITLE, name);
                context.getContentResolver().update(uri, values, null, null);
            }
        }
    }

    public static String getSelectAudioPath(Context context, long mSelectedId) {
        String result = "";
        if (!isOmaDrmEnabled()) {
            return result;
        }

        if (null == context) {
            return result;
        }
        try {
            final String[] ccols = new String[] { MediaStore.Audio.Media.DATA };
            String where = MediaStore.Audio.Media._ID + "='" + mSelectedId
                    + "'";
            Cursor cursor = query(context,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where,
                    null, null);
            if (null != cursor && 0 != cursor.getCount()) {
                cursor.moveToFirst();

                result = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                cursor.close();
                return result;
            }

            if (null != cursor) {
                cursor.close();
                return result;
            }
        } catch (Exception ex) {
        }

        return result;
    }

    public static Cursor query(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            return null;
        }
        return context.getContentResolver().query(uri, projection, selection,
                selectionArgs, sortOrder);
    }

    /**
     * @hide installs the rights.
     *
     * @param context
     *            The context
     * @param path
     *            Path to the file
     * @param mimeType
     *            mimeType
     */
    public static void saveRights(Context context, String path, String mimeType) {
        if (!isOmaDrmEnabled()) {
            return;
        }
        DrmRights drmRights = new DrmRights(path, mimeType);
        DrmManagerClient drmClient = new DrmManagerClient(context);
        try {
            drmClient.saveRights(drmRights, path, null);
        } catch (Exception ex) {
            Log.i(TAG, "Exception while saveRights : " + ex.toString());
        } finally {
            if (drmClient != null) {
                drmClient.release();
            }
        }
    }

    /**
     * @hide Show DRM license.
     *
     */
    private static void showProperties(Context context, String path,
            String drmType) {
        if (!isDrmFile(getFilePath(context, Uri.parse(path)))) {
            return;
        }

        String message = new String();
        ContentValues playContentValues = null;
        ContentValues displayContentValues = null;
        DrmManagerClientWrapper drmClient = new DrmManagerClientWrapper(context);

        try {
            playContentValues = drmClient.getConstraints(path, 1);
            displayContentValues = drmClient.getConstraints(path, 7);
        } catch (Exception ex) {
            Log.e(TAG, "Exception while showProperties :" + ex.toString());
        } finally {
            if (drmClient != null) {
                drmClient.release();
            }
        }

        if (playContentValues != null
                && playContentValues.getAsInteger("valid") != 0) {
            if (playContentValues.getAsInteger("unlimited") != 0) {
                message += OmaDrmStrings.getString(context,
                        OmaDrmStrings.PLAY_UNLIMITED) + "\n";
            } else {
                message += OmaDrmStrings.getString(context,
                        OmaDrmStrings.PLAY_PERMISSION) + "\n";
                message = formatMsg(context, message, playContentValues);
            }
        } else if (displayContentValues != null
                && displayContentValues.getAsInteger("valid") != 0) {
            if (displayContentValues.getAsInteger("unlimited") != 0) {
                message += OmaDrmStrings.getString(context,
                        OmaDrmStrings.PLAY_UNLIMITED) + "\n";
            } else {
                message += OmaDrmStrings.getString(context,
                        OmaDrmStrings.DISPLAY_PERMISSION) + "\n";
                message = formatMsg(context, message, displayContentValues);
            }
        } else {
            message += OmaDrmStrings
                    .getString(context, OmaDrmStrings.NO_RIGHTS) + "\n";
        }

        new AlertDialog.Builder(context)
                .setTitle(
                        OmaDrmStrings.getString(context,
                                OmaDrmStrings.LICENSE_INFO))
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, null)
                .setIcon(android.R.drawable.ic_dialog_info).show();
    }

    private static String formatMsg(Context context, String message,
            ContentValues constraints) {
        if (!isOmaDrmEnabled()) {
            return null;
        }
        int count = 0;
        long interval = 0;
        Date startDate = null;
        Date endDate = null;

        count = constraints.getAsInteger(OmdDrmConstraints.COUNT);
        long startDateL = constraints
                .getAsLong(OmdDrmConstraints.START_DATE_TIME);
        long endDateL = constraints.getAsLong(OmdDrmConstraints.END_DATE_TIME);
        interval = constraints.getAsLong(OmdDrmConstraints.INTERVAL);

        if (startDateL > 0) {
            startDate = new Date(startDateL);
        }

        if (endDateL > 0) {
            endDate = new Date(endDateL);
        }

        if (count > 0) {
            message += OmaDrmStrings.getString(context, OmaDrmStrings.COUNT)
                    + ": " + String.valueOf(count) + "\n";
        }

        if (interval > 0) {
            message += OmaDrmStrings.getString(context, OmaDrmStrings.INTERVAL)
                    + ": " + String.valueOf(interval / 1000) + " "
                    + OmaDrmStrings.getString(context, OmaDrmStrings.SECONDS)
                    + "\n";
        }

        if (startDate != null) {
            message += OmaDrmStrings.getString(context,
                    OmaDrmStrings.START_DATE)
                    + ": "
                    + startDate.toString()
                    + "\n";
        }

        if (endDate != null) {
            message += OmaDrmStrings.getString(context, OmaDrmStrings.END_DATE)
                    + ": " + endDate.toString() + "\n\n";
        }

        return message;
    }

    private static void buyLicense(final Context context, final String address,
            final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {
        if (TextUtils.isEmpty(address)) {
            new AlertDialog.Builder(context)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(
                            OmaDrmStrings.getString(context,
                                    OmaDrmStrings.LICENSE_EXPIRED))
                    .setMessage(
                            OmaDrmStrings.getString(context,
                                    OmaDrmStrings.LICENSE_RENEW_NOT_POSSIBLE))
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (cancelListener != null) {
                                        cancelListener.onClick(dialog, which);
                                    }

                                }
                            })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    if (cancelListener != null) {
                                        cancelListener.onClick(dialog, -1);
                                    }

                                }
                            }).show();
        } else {
            new AlertDialog.Builder(context)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(
                            OmaDrmStrings.getString(context,
                                    OmaDrmStrings.LICENSE_EXPIRED))
                    .setMessage(
                            OmaDrmStrings.getString(context,
                                    OmaDrmStrings.LICENSE_RENEW))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse(address));
                                    try {
                                        context.startActivity(i);
                                    } catch (Exception e) {
                                        Log.e(TAG, "ActivityNotFoundException="
                                                + e.toString());
                                        String str = OmaDrmStrings
                                                .getString(
                                                        context,
                                                        OmaDrmStrings.ACTIVITY_NOT_FOUND);
                                        Toast.makeText(context, str + address,
                                                Toast.LENGTH_LONG).show();
                                    }

                                    if (okListener != null) {
                                        okListener.onClick(dialog, which);
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (cancelListener != null) {
                                        cancelListener.onClick(dialog, which);
                                    }

                                }
                            })
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    if (cancelListener != null) {
                                        cancelListener.onClick(dialog, -1);
                                    }

                                }
                            }).show();
        }
    }

    public static boolean isDrmRightsFile(String mimeType) {
        if (mimeType == null)
            return false;

        String type = mimeType.toLowerCase();
        if (type.equals(DRM_MIMETYPE_RIGHTS_XML)
                || type.equals(DRM_MIMETYPE_RIGHTS_WXML)) {
            return true;
        }
        return false;
    }

    private static native boolean nativeConsumeDrmRights(FileDescriptor fd);

    private static native byte[] nativeGetDrmDecryptedData(FileDescriptor fd);
}
