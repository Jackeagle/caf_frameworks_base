/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IHardwareService;
import android.os.ServiceManager;
import android.os.Message;
import android.provider.Settings;
import android.util.Slog;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.content.pm.PackageInfo;
import android.util.Log;

public class LightsService {
    private static final String TAG = "LightsService";
    private static final boolean DEBUG = false;

    public static final int LIGHT_ID_BACKLIGHT = 0;
    public static final int LIGHT_ID_KEYBOARD = 1;
    public static final int LIGHT_ID_BUTTONS = 2;
    public static final int LIGHT_ID_BATTERY = 3;
    public static final int LIGHT_ID_NOTIFICATIONS = 4;
    public static final int LIGHT_ID_ATTENTION = 5;
    public static final int LIGHT_ID_BLUETOOTH = 6;
    public static final int LIGHT_ID_WIFI = 7;
    public static final int LIGHT_ID_COUNT = 8;

    public static final int LIGHT_FLASH_NONE = 0;
    public static final int LIGHT_FLASH_TIMED = 1;
    public static final int LIGHT_FLASH_HARDWARE = 2;

    /**
     * Light brightness is managed by a user setting.
     */
    public static final int BRIGHTNESS_MODE_USER = 0;

    /**
     * Light brightness is managed by a light sensor.
     */
    public static final int BRIGHTNESS_MODE_SENSOR = 1;

    private final Light mLights[] = new Light[LIGHT_ID_COUNT];

    private static final int MSG_BBL_TIMEOUT = 1;

    private int mButtonLightTimeout;

    private int mButtonBrightness;

    private Handler mLightHandler = null;

    public final class Light {

        private Light(int id) {
            mId = id;
        }

        public void setBrightness(int brightness) {
            setBrightness(brightness, BRIGHTNESS_MODE_USER);
        }

        public void setBrightness(int brightness, int brightnessMode) {
            synchronized (this) {
                int color = brightness & 0x000000ff;
                color = 0xff000000 | (color << 16) | (color << 8) | color;
                setLightLocked(color, LIGHT_FLASH_NONE, 0, 0, brightnessMode);
            }
        }

        public void setColor(int color) {
            synchronized (this) {
                setLightLocked(color, LIGHT_FLASH_NONE, 0, 0, 0);
            }
        }

        public void setFlashing(int color, int mode, int onMS, int offMS) {
            synchronized (this) {
                setLightLocked(color, mode, onMS, offMS, BRIGHTNESS_MODE_USER);
            }
        }


        public void pulse() {
            pulse(0x00ffffff, 7);
        }

        public void pulse(int color, int onMS) {
            synchronized (this) {
                if (mColor == 0 && !mFlashing) {
                    setLightLocked(color, LIGHT_FLASH_HARDWARE, onMS, 1000, BRIGHTNESS_MODE_USER);
                    mH.sendMessageDelayed(Message.obtain(mH, 1, this), onMS);
                }
            }
        }

        public void turnOff() {
            synchronized (this) {
                setLightLocked(0, LIGHT_FLASH_NONE, 0, 0, 0);
            }
        }

        private void stopFlashing() {
            synchronized (this) {
                setLightLocked(mColor, LIGHT_FLASH_NONE, 0, 0, BRIGHTNESS_MODE_USER);
            }
        }

        private void setLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            if (color != mColor || mode != mMode || onMS != mOnMS || offMS != mOffMS) {
                if (DEBUG) Slog.v(TAG, "setLight #" + mId + ": color=#"
                        + Integer.toHexString(color));
                mColor = color;
                mMode = mode;
                mOnMS = onMS;
                mOffMS = offMS;
                setLight_native(mNativePointer, mId, color, mode, onMS, offMS, brightnessMode);
            }
        }

        private int mId;
        private int mColor;
        private int mMode;
        private int mOnMS;
        private int mOffMS;
        private boolean mFlashing;
    }

    /* This class implements an obsolete API that was removed after eclair and re-added during the
     * final moments of the froyo release to support flashlight apps that had been using the private
     * IHardwareService API. This is expected to go away in the next release.
     */
    private final IHardwareService.Stub mLegacyFlashlightHack = new IHardwareService.Stub() {

        private static final String FLASHLIGHT_FILE = "/sys/class/leds/spotlight/brightness";

        public boolean getFlashlightEnabled() {
            try {
                FileInputStream fis = new FileInputStream(FLASHLIGHT_FILE);
                int result = fis.read();
                fis.close();
                return (result != '0');
            } catch (Exception e) {
                return false;
            }
        }

        public void setFlashlightEnabled(boolean on) {
            if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.FLASHLIGHT)
                    != PackageManager.PERMISSION_GRANTED &&
                    mContext.checkCallingOrSelfPermission(android.Manifest.permission.HARDWARE_TEST)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires FLASHLIGHT or HARDWARE_TEST permission");
            }
            try {
                FileOutputStream fos = new FileOutputStream(FLASHLIGHT_FILE);
                byte[] bytes = new byte[2];
                bytes[0] = (byte)(on ? '1' : '0');
                bytes[1] = '\n';
                fos.write(bytes);
                fos.close();
            } catch (Exception e) {
                // fail silently
            }
        }

        public void setButtonLightEnabled(boolean on) {
            if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.HARDWARE_TEST)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires FLASHLIGHT permission");
            }

            mLightHandler.removeMessages(MSG_BBL_TIMEOUT);

            if (on) {
                getLight(LIGHT_ID_BUTTONS).setBrightness(mButtonBrightness);

                //Set the display timeout of the device to key backlight as well
                if (mContext != null) {
                    ContentResolver resolver = mContext.getContentResolver();
                    long keypadBacklightTime = Settings.System.getLong(resolver,
                        Settings.System.SCREEN_OFF_TIMEOUT,mButtonLightTimeout);
                    mLightHandler.sendMessageDelayed(
                        mLightHandler.obtainMessage(MSG_BBL_TIMEOUT),
                          keypadBacklightTime);
                } else{
                    mLightHandler.sendMessageDelayed(
                        mLightHandler.obtainMessage(MSG_BBL_TIMEOUT),
                           mButtonLightTimeout);
                }
            } else {
                getLight(LIGHT_ID_BUTTONS).setBrightness(0);
            }
        }

        //borqs_india,start: to toggle speaker led on/off
        private final String SPEAKER_LED = "/sys/class/leds/speaker-led/blink";
        private final byte[] SPEAKER_LIGHT_ON = { '1' };
        private final byte[] SPEAKER_LIGHT_OFF = { '0' };
        //borqs_india, end

        /**
         *@hide
         *borqs_india: turn Speaker led on
         */
        public void setSpeakerLedOn(boolean on){
            synchronized(this){
                try {
                    FileOutputStream spkrLedFos = new FileOutputStream(SPEAKER_LED);
                    if(on)
                        spkrLedFos.write(SPEAKER_LIGHT_ON);
                    else
                        spkrLedFos.write(SPEAKER_LIGHT_OFF);
                    spkrLedFos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

       //borqs_india, start: to toggle notification led on/off
        private final String NOTIFY_RED_LED = "/sys/class/leds/red/brightness";
        private final String NOTIFY_GREEN_LED = "/sys/class/leds/green/brightness";
        private final String NOTIFY_GREEN_LED_BLINK = "/sys/class/leds/green/blink";
        private final String NOTIFY_RED_LED_BLINK = "/sys/class/leds/red/blink";
        private final String NOTIFY_RED_LED_RAMP_STEP_MS = "/sys/class/leds/red/ramp_step_ms";
        private final String NOTIFY_GREEN_LED_RAMP_STEP_MS = "/sys/class/leds/green/ramp_step_ms";
        private final String NOTIFY_RED_LED_DUTY_PCTS = "/sys/class/leds/red/duty_pcts";
        private final String NOTIFY_GREEN_LED_DUTY_PCTS = "/sys/class/leds/green/duty_pcts";

        private final byte[] RAMP_STEP_MS = {'1','0','0'};
        private final byte[] NOTIFICATION_LIGHT_ON = { '2', '5', '5'};
        private final byte[] NOTIFICATION_LIGHT_OFF = { '0' };
        private final byte[] NOTIFICATION_LIGHT_BLINK = {'1'};
       //borqs_india, end
        /**
         * borqs_india: to check whether calling package has permission or not
         */
        private boolean checkCallingPkgPermission(String permission)
        {
            PackageManager pm = mContext.getPackageManager();
            String callingPkg = pm.getNameForUid(getCallingUid());
            if(callingPkg.equals("android.uid.system:"+android.os.Process.SYSTEM_UID))
                return true;
            try {
                PackageInfo info = pm.getPackageInfo(callingPkg, PackageManager.GET_PERMISSIONS);
                    if (info.requestedPermissions != null) {
                        for (String p : info.requestedPermissions) {
                            if (p.equals(permission)) {
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                      Log.e(TAG,"Error processing package:"+callingPkg);
                      e.printStackTrace();
                }
            return false;
        }
       /**
         *@hide
         *borqs_india: turn notification red led on/off
         */
        public void setNotificationRedLedOn(boolean on){
            if (!checkCallingPkgPermission(android.Manifest.permission.NOTIFICATION_LED)) {
                throw new SecurityException("Requires NOTIFICATION_LED permission");
            }
            synchronized(this){
                try {
                    FileOutputStream notifyLedFos = new FileOutputStream(NOTIFY_RED_LED);
                    if(on)
                        notifyLedFos.write(NOTIFICATION_LIGHT_ON);
                    else
                        notifyLedFos.write(NOTIFICATION_LIGHT_OFF);
                    notifyLedFos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
       /**
         *@hide
         *borqs_india: turn notification green led on/off
         */
        public void setNotificationGreenLedOn(boolean on){
            if (!checkCallingPkgPermission(android.Manifest.permission.NOTIFICATION_LED)) {
                throw new SecurityException("Requires NOTIFICATION_LED permission");
            }
            synchronized(this){
                try {
                    FileOutputStream notifyLedFos = new FileOutputStream(NOTIFY_GREEN_LED);
                    if(on)
                        notifyLedFos.write(NOTIFICATION_LIGHT_ON);
                    else
                        notifyLedFos.write(NOTIFICATION_LIGHT_OFF);
                    notifyLedFos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
       /**
         *@hide
         *borqs_india: turn notification yellow led on/off
         */
        public void setNotificationYellowLedOn(boolean on){
            if (!checkCallingPkgPermission(android.Manifest.permission.NOTIFICATION_LED)) {
                throw new SecurityException("Requires NOTIFICATION_LED permission");
            }
            synchronized(this){
                try {
                    FileOutputStream notifyRedLedFos = new FileOutputStream(NOTIFY_RED_LED);
                    FileOutputStream notifyGreenLedFos = new FileOutputStream(NOTIFY_GREEN_LED);
                    if(on){
                        notifyRedLedFos.write(NOTIFICATION_LIGHT_ON);
                        notifyGreenLedFos.write(NOTIFICATION_LIGHT_ON);
                    }else{
                        notifyRedLedFos.write(NOTIFICATION_LIGHT_OFF);
                        notifyGreenLedFos.write(NOTIFICATION_LIGHT_OFF);
                    }
                    notifyRedLedFos.close();
                    notifyGreenLedFos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
       /**
         *@hide
         *borqs_india: to get the duty_pcts based on fixed ramp_step_ms
         */
        public byte[] getDutyPcts(int onMs, int offMs){
            final String rampStr = new String(RAMP_STEP_MS);
            final String offStr = "0";
            if(onMs <= 0 || offMs < 0 ) {
                Log.i(TAG,"Invalid onMs or offMs!");
                return null;
            }
            final int mRamp = Integer.parseInt(rampStr);
            int i,j,ni = onMs / mRamp;
            int nj = offMs / mRamp;
            if((onMs % mRamp) != 0 && (((onMs % mRamp) >= (mRamp/2)) || (onMs < mRamp)))
                ni+=1;
            if((offMs % mRamp) != 0 && (((offMs % mRamp) >= (mRamp/2)) || (offMs < mRamp)))
                nj+=1;
            StringBuffer buf = new StringBuffer();
            for(i=0 ; i < ni ; i++)
                buf.append(rampStr+",");
            for(j = 0 ; j < nj ; j++)
                buf.append(offStr+",");
            buf.deleteCharAt(buf.length()-1);
            Log.i(TAG,"dutyPcts:"+new String(buf.toString().getBytes()));
            return buf.toString().getBytes();
        }
       /**
         *@hide
         *borqs_india: blink notification yellow led for given onMs and offMs
         * passing onMs<=0 will turn off the blinking
         * passing offMs=0 will turn on led instead of blink
         */
        public void setNotificationYellowLedBlink(int onMs, int offMs){
            if (!checkCallingPkgPermission(android.Manifest.permission.NOTIFICATION_LED)) {
                throw new SecurityException("Requires NOTIFICATION_LED permission");
            }
            synchronized(this){
                try {
                    FileOutputStream notifyLedFosGreen = new FileOutputStream(NOTIFY_GREEN_LED_BLINK);
                    FileOutputStream notifyLedFosRed = new FileOutputStream(NOTIFY_RED_LED_BLINK);
                    if(onMs <= 0){
                        Log.i(TAG,"Notification YELLOW Led blink turned off!");
                        notifyLedFosGreen.write(NOTIFICATION_LIGHT_OFF);
                        notifyLedFosRed.write(NOTIFICATION_LIGHT_OFF);
                        notifyLedFosRed.close();
                        notifyLedFosGreen.close();
                        return;
                    }
                    byte dutyPcts[] = getDutyPcts(onMs,offMs);
                    if(dutyPcts != null){
                        FileOutputStream notifyLedRampFosGreen = new FileOutputStream(NOTIFY_GREEN_LED_RAMP_STEP_MS);
                        FileOutputStream notifyLedRampFosRed = new FileOutputStream(NOTIFY_RED_LED_RAMP_STEP_MS);
                        FileOutputStream notifyLedDutyFosGreen = new FileOutputStream(NOTIFY_GREEN_LED_DUTY_PCTS);
                        FileOutputStream notifyLedDutyFosRed = new FileOutputStream(NOTIFY_RED_LED_DUTY_PCTS);
                        notifyLedFosGreen.write(NOTIFICATION_LIGHT_BLINK);
                        notifyLedFosRed.write(NOTIFICATION_LIGHT_BLINK);
                        notifyLedRampFosGreen.write(RAMP_STEP_MS);
                        notifyLedRampFosRed.write(RAMP_STEP_MS);
                        notifyLedDutyFosGreen.write(dutyPcts);
                        notifyLedDutyFosRed.write(dutyPcts);
                        notifyLedFosGreen.close();
                        notifyLedFosRed.close();
                        notifyLedRampFosGreen.close();
                        notifyLedRampFosRed.close();
                        notifyLedDutyFosGreen.close();
                        notifyLedDutyFosRed.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

       /**
         *@hide
         *borqs_india: turn notification green led blink
         * passing onMs<=0 will turn off the blinking
         * passing offMs=0 will turn on led instead of blink
         */
        public void setNotificationGreenLedBlink(int onMs, int offMs){
            if (!checkCallingPkgPermission(android.Manifest.permission.NOTIFICATION_LED)) {
                throw new SecurityException("Requires NOTIFICATION_LED permission");
            }
            synchronized(this){
                try {
                    FileOutputStream notifyLedFos = new FileOutputStream(NOTIFY_GREEN_LED_BLINK);
                    if(onMs <= 0){
                        Log.i(TAG,"Notification GREEN Led blink turned off!");
                        notifyLedFos.write(NOTIFICATION_LIGHT_OFF);
                        notifyLedFos.close();
                        return;
                    }
                    byte dutyPcts[] = getDutyPcts(onMs,offMs);
                    if(dutyPcts != null){
                        FileOutputStream notifyLedRampFos = new FileOutputStream(NOTIFY_GREEN_LED_RAMP_STEP_MS);
                        FileOutputStream notifyLedDutyFos = new FileOutputStream(NOTIFY_GREEN_LED_DUTY_PCTS);
                        notifyLedFos.write(NOTIFICATION_LIGHT_BLINK);
                        notifyLedRampFos.write(RAMP_STEP_MS);
                        notifyLedDutyFos.write(dutyPcts);
                        notifyLedFos.close();
                        notifyLedRampFos.close();
                        notifyLedDutyFos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
       /**
         *@hide
         *borqs_india: turn notification red led blink
         * passing onMs<=0 will turn off the blinking
         * passing offMs=0 will turn on led instead of blink
         */
        public void setNotificationRedLedBlink(int onMs, int offMs){
            if (!checkCallingPkgPermission(android.Manifest.permission.NOTIFICATION_LED)) {
                throw new SecurityException("Requires NOTIFICATION_LED permission");
            }
            synchronized(this){
                try {
                    FileOutputStream notifyLedFos = new FileOutputStream(NOTIFY_RED_LED_BLINK);
                    if(onMs <= 0){
                        Log.i(TAG,"Notification RED Led blink turned off!");
                        notifyLedFos.write(NOTIFICATION_LIGHT_OFF);
                        notifyLedFos.close();
                        return;
                    }
                    byte dutyPcts[] = getDutyPcts(onMs,offMs);
                    if(dutyPcts != null){
                        FileOutputStream notifyLedRampFos = new FileOutputStream(NOTIFY_RED_LED_RAMP_STEP_MS);
                        FileOutputStream notifyLedDutyFos = new FileOutputStream(NOTIFY_RED_LED_DUTY_PCTS);
                        notifyLedFos.write(NOTIFICATION_LIGHT_BLINK);
                        notifyLedRampFos.write(RAMP_STEP_MS);
                        notifyLedDutyFos.write(dutyPcts);
                        notifyLedFos.close();
                        notifyLedRampFos.close();
                        notifyLedDutyFos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    LightsService(Context context) {

        mNativePointer = init_native();
        mContext = context;

        mButtonLightTimeout = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_button_light_timeout_msec);

        mButtonBrightness = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_button_light_bright_level);

        mLightHandler = new Handler() {
            public void handleMessage(Message msg) {
                synchronized(this) {
                    switch(msg.what) {
                    case MSG_BBL_TIMEOUT:
                        getLight(LIGHT_ID_BUTTONS).setBrightness(0);
                        break;
                    }
                }
            }
        };

        ServiceManager.addService("hardware", mLegacyFlashlightHack);

        for (int i = 0; i < LIGHT_ID_COUNT; i++) {
            mLights[i] = new Light(i);
        }
    }

    protected void finalize() throws Throwable {
        finalize_native(mNativePointer);
        super.finalize();
    }

    public Light getLight(int id) {
        return mLights[id];
    }

    private Handler mH = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Light light = (Light)msg.obj;
            light.stopFlashing();
        }
    };

    private static native int init_native();
    private static native void finalize_native(int ptr);

    private static native void setLight_native(int ptr, int light, int color, int mode,
            int onMS, int offMS, int brightnessMode);

    private final Context mContext;

    private int mNativePointer;
}
