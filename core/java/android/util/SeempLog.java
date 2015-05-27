/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
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

package android.util;

import com.android.internal.os.RuntimeInit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import android.util.Log;
import android.provider.Settings;
import android.util.SeempApiEnum;

/**
 * SeempLog
 *
 * @hide
 */
public final class SeempLog {
    private SeempLog() {
    }

    /**
     * Send a log message to the seemp log.
     * @param api The api triggering this message.
     * @param msg The message you would like logged.
     */
    public static int record(int api, String msg) {
        return seemp_println_native(api, msg);
    }

    /** @hide */ public static native int seemp_println_native(int api, String msg);

    private final static java.util.Map<String,Integer> value_to_get_map;
    static {
        value_to_get_map = new java.util.HashMap<String,Integer>( 198 );
        value_to_get_map.put(Settings.System.NOTIFICATION_SOUND,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_NOTIFICATION_SOUND_);
        value_to_get_map.put(Settings.System.DTMF_TONE_WHEN_DIALING,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_DTMF_TONE_WHEN_DIALING_);
        value_to_get_map.put(Settings.System.LOCK_PATTERN_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_ENABLED_);
        value_to_get_map.put(Settings.System.WIFI_MAX_DHCP_RETRY_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_MAX_DHCP_RETRY_COUNT_);
        value_to_get_map.put(Settings.System.AUTO_TIME,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_AUTO_TIME_);
        value_to_get_map.put(Settings.System.SETUP_WIZARD_HAS_RUN,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SETUP_WIZARD_HAS_RUN_);
        value_to_get_map.put(Settings.System.SYS_PROP_SETTING_VERSION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SYS_PROP_SETTING_VERSION_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS_);
        value_to_get_map.put(Settings.System.LOCATION_PROVIDERS_ALLOWED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_LOCATION_PROVIDERS_ALLOWED_);
        value_to_get_map.put(Settings.System.ALARM_ALERT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_ALARM_ALERT_);
        value_to_get_map.put(Settings.System.VIBRATE_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VIBRATE_ON_);
        value_to_get_map.put(Settings.System.USB_MASS_STORAGE_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_USB_MASS_STORAGE_ENABLED_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_PING_DELAY_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_DELAY_MS_);
        value_to_get_map.put(Settings.System.FONT_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_FONT_SCALE_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_AP_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_AP_COUNT_);
        value_to_get_map.put(Settings.System.ALWAYS_FINISH_ACTIVITIES,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_ALWAYS_FINISH_ACTIVITIES_);
        value_to_get_map.put(Settings.System.ACCELEROMETER_ROTATION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_ACCELEROMETER_ROTATION_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_PING_TIMEOUT_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_TIMEOUT_MS_);
        value_to_get_map.put(Settings.System.VOLUME_NOTIFICATION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VOLUME_NOTIFICATION_);
        value_to_get_map.put(Settings.System.AIRPLANE_MODE_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_AIRPLANE_MODE_ON_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS_);
        value_to_get_map.put(Settings.System.WIFI_STATIC_IP,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_STATIC_IP_);
        value_to_get_map.put(Settings.System.RADIO_BLUETOOTH,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_RADIO_BLUETOOTH_);
        value_to_get_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_BLUETOOTH_DISCOVERABILITY_TIMEOUT_);
        value_to_get_map.put(Settings.System.VOLUME_RING,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VOLUME_RING_);
        value_to_get_map.put(Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_MODE_RINGER_STREAMS_AFFECTED_);
        value_to_get_map.put(Settings.System.VOLUME_SYSTEM,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VOLUME_SYSTEM_);
        value_to_get_map.put(Settings.System.SCREEN_OFF_TIMEOUT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SCREEN_OFF_TIMEOUT_);
        value_to_get_map.put(Settings.System.RADIO_WIFI,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_RADIO_WIFI_);
        value_to_get_map.put(Settings.System.AUTO_TIME_ZONE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_AUTO_TIME_ZONE_);
        value_to_get_map.put(Settings.System.TEXT_AUTO_CAPS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_TEXT_AUTO_CAPS_);
        value_to_get_map.put(Settings.System.WALLPAPER_ACTIVITY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WALLPAPER_ACTIVITY_);
        value_to_get_map.put(Settings.System.ANIMATOR_DURATION_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_ANIMATOR_DURATION_SCALE_);
        value_to_get_map.put(Settings.System.WIFI_NUM_OPEN_NETWORKS_KEPT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_NUM_OPEN_NETWORKS_KEPT_);
        value_to_get_map.put(Settings.System.LOCK_PATTERN_VISIBLE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_VISIBLE_);
        value_to_get_map.put(Settings.System.VOLUME_VOICE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VOLUME_VOICE_);
        value_to_get_map.put(Settings.System.DEBUG_APP,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_DEBUG_APP_);
        value_to_get_map.put(Settings.System.WIFI_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_ON_);
        value_to_get_map.put(Settings.System.TEXT_SHOW_PASSWORD,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_TEXT_SHOW_PASSWORD_);
        value_to_get_map.put(Settings.System.WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY_);
        value_to_get_map.put(Settings.System.WIFI_SLEEP_POLICY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_);
        value_to_get_map.put(Settings.System.VOLUME_MUSIC,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VOLUME_MUSIC_);
        value_to_get_map.put(Settings.System.PARENTAL_CONTROL_LAST_UPDATE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_LAST_UPDATE_);
        value_to_get_map.put(Settings.System.DEVICE_PROVISIONED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_DEVICE_PROVISIONED_);
        value_to_get_map.put(Settings.System.HTTP_PROXY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_HTTP_PROXY_);
        value_to_get_map.put(Settings.System.ANDROID_ID,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_ANDROID_ID_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_MAX_AP_CHECKS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_MAX_AP_CHECKS_);
        value_to_get_map.put(Settings.System.END_BUTTON_BEHAVIOR,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_END_BUTTON_BEHAVIOR_);
        value_to_get_map.put(Settings.System.NEXT_ALARM_FORMATTED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_NEXT_ALARM_FORMATTED_);
        value_to_get_map.put(Settings.System.RADIO_CELL,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_RADIO_CELL_);
        value_to_get_map.put(Settings.System.PARENTAL_CONTROL_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_ENABLED_);
        value_to_get_map.put(Settings.System.BLUETOOTH_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_BLUETOOTH_ON_);
        value_to_get_map.put(Settings.System.WINDOW_ANIMATION_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WINDOW_ANIMATION_SCALE_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED_);
        value_to_get_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_BLUETOOTH_DISCOVERABILITY_);
        value_to_get_map.put(Settings.System.WIFI_STATIC_DNS1,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_STATIC_DNS1_);
        value_to_get_map.put(Settings.System.WIFI_STATIC_DNS2,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_STATIC_DNS2_);
        value_to_get_map.put(Settings.System.HAPTIC_FEEDBACK_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_HAPTIC_FEEDBACK_ENABLED_);
        value_to_get_map.put(Settings.System.SHOW_WEB_SUGGESTIONS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SHOW_WEB_SUGGESTIONS_);
        value_to_get_map.put(Settings.System.PARENTAL_CONTROL_REDIRECT_URL,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_REDIRECT_URL_);
        value_to_get_map.put(Settings.System.DATE_FORMAT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_DATE_FORMAT_);
        value_to_get_map.put(Settings.System.RADIO_NFC,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_RADIO_NFC_);
        value_to_get_map.put(Settings.System.AIRPLANE_MODE_RADIOS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_AIRPLANE_MODE_RADIOS_);
        value_to_get_map.put(Settings.System.LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED_);
        value_to_get_map.put(Settings.System.TIME_12_24,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_TIME_12_24_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT_);
        value_to_get_map.put(Settings.System.VOLUME_BLUETOOTH_SCO,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VOLUME_BLUETOOTH_SCO_);
        value_to_get_map.put(Settings.System.USER_ROTATION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_USER_ROTATION_);
        value_to_get_map.put(Settings.System.WIFI_STATIC_GATEWAY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_STATIC_GATEWAY_);
        value_to_get_map.put(Settings.System.STAY_ON_WHILE_PLUGGED_IN,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_STAY_ON_WHILE_PLUGGED_IN_);
        value_to_get_map.put(Settings.System.SOUND_EFFECTS_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SOUND_EFFECTS_ENABLED_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_PING_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_COUNT_);
        value_to_get_map.put(Settings.System.DATA_ROAMING,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_DATA_ROAMING_);
        value_to_get_map.put(Settings.System.SETTINGS_CLASSNAME,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SETTINGS_CLASSNAME_);
        value_to_get_map.put(Settings.System.TRANSITION_ANIMATION_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_TRANSITION_ANIMATION_SCALE_);
        value_to_get_map.put(Settings.System.WAIT_FOR_DEBUGGER,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WAIT_FOR_DEBUGGER_);
        value_to_get_map.put(Settings.System.INSTALL_NON_MARKET_APPS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_INSTALL_NON_MARKET_APPS_);
        value_to_get_map.put(Settings.System.ADB_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_ADB_ENABLED_);
        value_to_get_map.put(Settings.System.WIFI_USE_STATIC_IP,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_USE_STATIC_IP_);
        value_to_get_map.put(Settings.System.DIM_SCREEN,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_DIM_SCREEN_);
        value_to_get_map.put(Settings.System.VOLUME_ALARM,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_VOLUME_ALARM_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_ON_);
        value_to_get_map.put(Settings.System.WIFI_STATIC_NETMASK,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_STATIC_NETMASK_);
        value_to_get_map.put(Settings.System.NETWORK_PREFERENCE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_NETWORK_PREFERENCE_);
        value_to_get_map.put(Settings.System.SHOW_PROCESSES,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SHOW_PROCESSES_);
        value_to_get_map.put(Settings.System.TEXT_AUTO_REPLACE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_TEXT_AUTO_REPLACE_);
        value_to_get_map.put(Settings.System.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON_);
        value_to_get_map.put(Settings.System.APPEND_FOR_LAST_AUDIBLE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_APPEND_FOR_LAST_AUDIBLE_);
        value_to_get_map.put(Settings.System.SHOW_GTALK_SERVICE_STATUS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SHOW_GTALK_SERVICE_STATUS_);
        value_to_get_map.put(Settings.System.SCREEN_BRIGHTNESS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_);
        value_to_get_map.put(Settings.System.USE_GOOGLE_MAIL,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_USE_GOOGLE_MAIL_);
        value_to_get_map.put(Settings.System.RINGTONE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_RINGTONE_);
        value_to_get_map.put(Settings.System.LOGGING_ID,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_LOGGING_ID_);
        value_to_get_map.put(Settings.System.MODE_RINGER,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_MODE_RINGER_);
        value_to_get_map.put(Settings.System.MUTE_STREAMS_AFFECTED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_MUTE_STREAMS_AFFECTED_);
        value_to_get_map.put(Settings.System.WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE_);
        value_to_get_map.put(Settings.System.TEXT_AUTO_PUNCTUATE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_TEXT_AUTO_PUNCTUATE_);
        value_to_get_map.put(Settings.System.WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS_);
        value_to_get_map.put(Settings.System.SCREEN_BRIGHTNESS_MODE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_MODE_);
    }

    public static int getSeempGetApiIdFromValue( String v )
    {
        Integer result = value_to_get_map.get( v );
        if (result == null)
        {
            result = -1;
        }
        return result;
    }

    private final static java.util.Map<String,Integer> value_to_put_map;
    static {
        value_to_put_map = new java.util.HashMap<String,Integer>( 198 );
        value_to_put_map.put(Settings.System.NOTIFICATION_SOUND,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_NOTIFICATION_SOUND_);
        value_to_put_map.put(Settings.System.DTMF_TONE_WHEN_DIALING,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_DTMF_TONE_WHEN_DIALING_);
        value_to_put_map.put(Settings.System.LOCK_PATTERN_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_ENABLED_);
        value_to_put_map.put(Settings.System.WIFI_MAX_DHCP_RETRY_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_MAX_DHCP_RETRY_COUNT_);
        value_to_put_map.put(Settings.System.AUTO_TIME,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_AUTO_TIME_);
        value_to_put_map.put(Settings.System.SETUP_WIZARD_HAS_RUN,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SETUP_WIZARD_HAS_RUN_);
        value_to_put_map.put(Settings.System.SYS_PROP_SETTING_VERSION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SYS_PROP_SETTING_VERSION_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS_);
        value_to_put_map.put(Settings.System.LOCATION_PROVIDERS_ALLOWED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_LOCATION_PROVIDERS_ALLOWED_);
        value_to_put_map.put(Settings.System.ALARM_ALERT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_ALARM_ALERT_);
        value_to_put_map.put(Settings.System.VIBRATE_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VIBRATE_ON_);
        value_to_put_map.put(Settings.System.USB_MASS_STORAGE_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_USB_MASS_STORAGE_ENABLED_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_PING_DELAY_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_DELAY_MS_);
        value_to_put_map.put(Settings.System.FONT_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_FONT_SCALE_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_AP_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_AP_COUNT_);
        value_to_put_map.put(Settings.System.ALWAYS_FINISH_ACTIVITIES,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_ALWAYS_FINISH_ACTIVITIES_);
        value_to_put_map.put(Settings.System.ACCELEROMETER_ROTATION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_ACCELEROMETER_ROTATION_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_PING_TIMEOUT_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_TIMEOUT_MS_);
        value_to_put_map.put(Settings.System.VOLUME_NOTIFICATION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VOLUME_NOTIFICATION_);
        value_to_put_map.put(Settings.System.AIRPLANE_MODE_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_AIRPLANE_MODE_ON_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS_);
        value_to_put_map.put(Settings.System.WIFI_STATIC_IP,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_STATIC_IP_);
        value_to_put_map.put(Settings.System.RADIO_BLUETOOTH,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_RADIO_BLUETOOTH_);
        value_to_put_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_BLUETOOTH_DISCOVERABILITY_TIMEOUT_);
        value_to_put_map.put(Settings.System.VOLUME_RING,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VOLUME_RING_);
        value_to_put_map.put(Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_MODE_RINGER_STREAMS_AFFECTED_);
        value_to_put_map.put(Settings.System.VOLUME_SYSTEM,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VOLUME_SYSTEM_);
        value_to_put_map.put(Settings.System.SCREEN_OFF_TIMEOUT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SCREEN_OFF_TIMEOUT_);
        value_to_put_map.put(Settings.System.RADIO_WIFI,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_RADIO_WIFI_);
        value_to_put_map.put(Settings.System.AUTO_TIME_ZONE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_AUTO_TIME_ZONE_);
        value_to_put_map.put(Settings.System.TEXT_AUTO_CAPS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_TEXT_AUTO_CAPS_);
        value_to_put_map.put(Settings.System.WALLPAPER_ACTIVITY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WALLPAPER_ACTIVITY_);
        value_to_put_map.put(Settings.System.ANIMATOR_DURATION_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_ANIMATOR_DURATION_SCALE_);
        value_to_put_map.put(Settings.System.WIFI_NUM_OPEN_NETWORKS_KEPT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_NUM_OPEN_NETWORKS_KEPT_);
        value_to_put_map.put(Settings.System.LOCK_PATTERN_VISIBLE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_VISIBLE_);
        value_to_put_map.put(Settings.System.VOLUME_VOICE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VOLUME_VOICE_);
        value_to_put_map.put(Settings.System.DEBUG_APP,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_DEBUG_APP_);
        value_to_put_map.put(Settings.System.WIFI_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_ON_);
        value_to_put_map.put(Settings.System.TEXT_SHOW_PASSWORD,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_TEXT_SHOW_PASSWORD_);
        value_to_put_map.put(Settings.System.WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY_);
        value_to_put_map.put(Settings.System.WIFI_SLEEP_POLICY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_);
        value_to_put_map.put(Settings.System.VOLUME_MUSIC,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VOLUME_MUSIC_);
        value_to_put_map.put(Settings.System.PARENTAL_CONTROL_LAST_UPDATE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_LAST_UPDATE_);
        value_to_put_map.put(Settings.System.DEVICE_PROVISIONED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_DEVICE_PROVISIONED_);
        value_to_put_map.put(Settings.System.HTTP_PROXY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_HTTP_PROXY_);
        value_to_put_map.put(Settings.System.ANDROID_ID,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_ANDROID_ID_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_MAX_AP_CHECKS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_MAX_AP_CHECKS_);
        value_to_put_map.put(Settings.System.END_BUTTON_BEHAVIOR,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_END_BUTTON_BEHAVIOR_);
        value_to_put_map.put(Settings.System.NEXT_ALARM_FORMATTED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_NEXT_ALARM_FORMATTED_);
        value_to_put_map.put(Settings.System.RADIO_CELL,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_RADIO_CELL_);
        value_to_put_map.put(Settings.System.PARENTAL_CONTROL_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_ENABLED_);
        value_to_put_map.put(Settings.System.BLUETOOTH_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_BLUETOOTH_ON_);
        value_to_put_map.put(Settings.System.WINDOW_ANIMATION_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WINDOW_ANIMATION_SCALE_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED_);
        value_to_put_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_BLUETOOTH_DISCOVERABILITY_);
        value_to_put_map.put(Settings.System.WIFI_STATIC_DNS1,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_STATIC_DNS1_);
        value_to_put_map.put(Settings.System.WIFI_STATIC_DNS2,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_STATIC_DNS2_);
        value_to_put_map.put(Settings.System.HAPTIC_FEEDBACK_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_HAPTIC_FEEDBACK_ENABLED_);
        value_to_put_map.put(Settings.System.SHOW_WEB_SUGGESTIONS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SHOW_WEB_SUGGESTIONS_);
        value_to_put_map.put(Settings.System.PARENTAL_CONTROL_REDIRECT_URL,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_REDIRECT_URL_);
        value_to_put_map.put(Settings.System.DATE_FORMAT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_DATE_FORMAT_);
        value_to_put_map.put(Settings.System.RADIO_NFC,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_RADIO_NFC_);
        value_to_put_map.put(Settings.System.AIRPLANE_MODE_RADIOS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_AIRPLANE_MODE_RADIOS_);
        value_to_put_map.put(Settings.System.LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED_);
        value_to_put_map.put(Settings.System.TIME_12_24,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_TIME_12_24_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT_);
        value_to_put_map.put(Settings.System.VOLUME_BLUETOOTH_SCO,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VOLUME_BLUETOOTH_SCO_);
        value_to_put_map.put(Settings.System.USER_ROTATION,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_USER_ROTATION_);
        value_to_put_map.put(Settings.System.WIFI_STATIC_GATEWAY,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_STATIC_GATEWAY_);
        value_to_put_map.put(Settings.System.STAY_ON_WHILE_PLUGGED_IN,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_STAY_ON_WHILE_PLUGGED_IN_);
        value_to_put_map.put(Settings.System.SOUND_EFFECTS_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SOUND_EFFECTS_ENABLED_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_PING_COUNT,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_COUNT_);
        value_to_put_map.put(Settings.System.DATA_ROAMING,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_DATA_ROAMING_);
        value_to_put_map.put(Settings.System.SETTINGS_CLASSNAME,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SETTINGS_CLASSNAME_);
        value_to_put_map.put(Settings.System.TRANSITION_ANIMATION_SCALE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_TRANSITION_ANIMATION_SCALE_);
        value_to_put_map.put(Settings.System.WAIT_FOR_DEBUGGER,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WAIT_FOR_DEBUGGER_);
        value_to_put_map.put(Settings.System.INSTALL_NON_MARKET_APPS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_INSTALL_NON_MARKET_APPS_);
        value_to_put_map.put(Settings.System.ADB_ENABLED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_ADB_ENABLED_);
        value_to_put_map.put(Settings.System.WIFI_USE_STATIC_IP,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_USE_STATIC_IP_);
        value_to_put_map.put(Settings.System.DIM_SCREEN,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_DIM_SCREEN_);
        value_to_put_map.put(Settings.System.VOLUME_ALARM,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_VOLUME_ALARM_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_ON_);
        value_to_put_map.put(Settings.System.WIFI_STATIC_NETMASK,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_STATIC_NETMASK_);
        value_to_put_map.put(Settings.System.NETWORK_PREFERENCE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_NETWORK_PREFERENCE_);
        value_to_put_map.put(Settings.System.SHOW_PROCESSES,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SHOW_PROCESSES_);
        value_to_put_map.put(Settings.System.TEXT_AUTO_REPLACE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_TEXT_AUTO_REPLACE_);
        value_to_put_map.put(Settings.System.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON_);
        value_to_put_map.put(Settings.System.APPEND_FOR_LAST_AUDIBLE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_APPEND_FOR_LAST_AUDIBLE_);
        value_to_put_map.put(Settings.System.SHOW_GTALK_SERVICE_STATUS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SHOW_GTALK_SERVICE_STATUS_);
        value_to_put_map.put(Settings.System.SCREEN_BRIGHTNESS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_);
        value_to_put_map.put(Settings.System.USE_GOOGLE_MAIL,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_USE_GOOGLE_MAIL_);
        value_to_put_map.put(Settings.System.RINGTONE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_RINGTONE_);
        value_to_put_map.put(Settings.System.LOGGING_ID,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_LOGGING_ID_);
        value_to_put_map.put(Settings.System.MODE_RINGER,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_MODE_RINGER_);
        value_to_put_map.put(Settings.System.MUTE_STREAMS_AFFECTED,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_MUTE_STREAMS_AFFECTED_);
        value_to_put_map.put(Settings.System.WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE_);
        value_to_put_map.put(Settings.System.TEXT_AUTO_PUNCTUATE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_TEXT_AUTO_PUNCTUATE_);
        value_to_put_map.put(Settings.System.WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS_);
        value_to_put_map.put(Settings.System.SCREEN_BRIGHTNESS_MODE,
                SeempApiEnum.SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_MODE_);
    }

    public static int getSeempPutApiIdFromValue( String v )
    {
        Integer result = value_to_put_map.get( v );
        if (result == null)
        {
            result = -1;
        }
        return result;
    }
}
