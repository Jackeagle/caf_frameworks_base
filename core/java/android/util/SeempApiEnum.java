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

/** @hide */
public class SeempApiEnum {
    public static final int SEEMP_API_kernel__oom_adjust_write                                =   0;
    public static final int SEEMP_API_kernel__sendto                                          =   1;
    public static final int SEEMP_API_kernel__recvfrom                                        =   2;
    public static final int SEEMP_API_View__onTouchEvent                                      =   3;
    public static final int SEEMP_API_View__onKeyDown                                         =   4;
    public static final int SEEMP_API_View__onKeyUp                                           =   5;
    public static final int SEEMP_API_View__onTrackBallEvent                                  =   6;
    public static final int SEEMP_API_PhoneNumberUtils__getDeviceSoftwareVersion              =   7;
    public static final int SEEMP_API_android_provider_Settings__get_ANDROID_ID_              =   8;
    public static final int SEEMP_API_TelephonyManager__getDeviceId                           =   9;
    public static final int SEEMP_API_TelephonyManager__getSimSerialNumber                    =  10;
    public static final int SEEMP_API_TelephonyManager__getLine1Number                        =  11;
    public static final int SEEMP_API_PhoneNumberUtils__getNumberFromIntent                   =  12;
    public static final int SEEMP_API_Telephony__query                                        =  13;
    public static final int SEEMP_API_CallerInfo__getCallerId                                 =  14;
    public static final int SEEMP_API_CallerInfo__getCallerInfo                               =  15;
    public static final int SEEMP_API_ContentResolver__query                                  =  16;
    public static final int SEEMP_API_AccountManagerService__getPassword                      =  17;
    public static final int SEEMP_API_AccountManagerService__getUserData                      =  18;
    public static final int SEEMP_API_AccountManagerService__addAccount                       =  19;
    public static final int SEEMP_API_AccountManagerService__removeAccount                    =  20;
    public static final int SEEMP_API_AccountManagerService__setPassword                      =  21;
    public static final int SEEMP_API_AccountManagerService__clearPassword                    =  22;
    public static final int SEEMP_API_AccountManagerService__setUserData                      =  23;
    public static final int SEEMP_API_AccountManagerService__editProperties                   =  24;
    public static final int SEEMP_API_AccountManager__getPassword                             =  25;
    public static final int SEEMP_API_AccountManager__getUserData                             =  26;
    public static final int SEEMP_API_AccountManager__addAccountExplicitly                    =  27;
    public static final int SEEMP_API_AccountManager__removeAccount                           =  28;
    public static final int SEEMP_API_AccountManager__setPassword                             =  29;
    public static final int SEEMP_API_AccountManager__clearPassword                           =  30;
    public static final int SEEMP_API_AccountManager__setUserData                             =  31;
    public static final int SEEMP_API_AccountManager__addAccount                              =  32;
    public static final int SEEMP_API_AccountManager__editProperties                          =  33;
    public static final int SEEMP_API_AccountManager__doWork                                  =  34;
    public static final int SEEMP_API_Browser__addSearchUrl                                   =  35;
    public static final int SEEMP_API_Browser__canClearHistory                                =  36;
    public static final int SEEMP_API_Browser__ClearHistory                                   =  37;
    public static final int SEEMP_API_Browser__deleteFromHistory                              =  38;
    public static final int SEEMP_API_Browser__deleteHistoryTimeFrame                         =  39;
    public static final int SEEMP_API_Browser__deleteHistoryWhere                             =  40;
    public static final int SEEMP_API_Browser__getAllBookmarks                                =  41;
    public static final int SEEMP_API_Browser__getAllVisitedUrls                              =  42;
    public static final int SEEMP_API_Browser__getVisitedHistory                              =  43;
    public static final int SEEMP_API_Browser__getVisitedLike                                 =  44;
    public static final int SEEMP_API_Browser__requestAllIcons                                =  45;
    public static final int SEEMP_API_Browser__truncateHistory                                =  46;
    public static final int SEEMP_API_Browser__updateVisitedHistory                           =  47;
    public static final int SEEMP_API_Browser__clearSearches                                  =  48;
    public static final int SEEMP_API_WebIconDatabase__bulkRequestIconForPageUrl              =  49;
    public static final int SEEMP_API_ContentResolver__insert                                 =  50;
    public static final int SEEMP_API_CalendarContract__insert                                =  51;
    public static final int SEEMP_API_CalendarContract__alarmExists                           =  52;
    public static final int SEEMP_API_CalendarContract__findNextAlarmTime                     =  53;
    public static final int SEEMP_API_CalendarContract__query                                 =  54;
    public static final int SEEMP_API_LocationManager___requestLocationUpdates                =  55;
    public static final int SEEMP_API_LocationManager__addGpsStatusListener                   =  56;
    public static final int SEEMP_API_LocationManager__addNmeaListener                        =  57;
    public static final int SEEMP_API_LocationManager__addProximityAlert                      =  58;
    public static final int SEEMP_API_LocationManager__getBestProvider                        =  59;
    public static final int SEEMP_API_LocationManager__getLastKnownLocation                   =  60;
    public static final int SEEMP_API_LocationManager__getProvider                            =  61;
    public static final int SEEMP_API_LocationManager__getProviders                           =  62;
    public static final int SEEMP_API_LocationManager__isProviderEnabled                      =  63;
    public static final int SEEMP_API_LocationManager__requestLocationUpdates                 =  64;
    public static final int SEEMP_API_LocationManager__sendExtraCommand                       =  65;
    public static final int SEEMP_API_TelephonyManager__getCellLocation                       =  66;
    public static final int SEEMP_API_TelephonyManager__getNeighboringCellInfo                =  67;
    public static final int SEEMP_API_GeolocationService__registerForLocationUpdates          =  68;
    public static final int SEEMP_API_GeolocationService__setEnableGps                        =  69;
    public static final int SEEMP_API_GeolocationService__start                               =  70;
    public static final int SEEMP_API_WebChromeClient__onGeolocationPermissionsShowPrompt     =  71;
    public static final int SEEMP_API_WifiManager__getScanResults                             =  72;
    public static final int SEEMP_API_android_bluetooth_BluetoothAdapter__enable              =  73;
    public static final int SEEMP_API_android_bluetooth_BluetoothAdapter__disable             =  74;
    public static final int SEEMP_API_android_bluetooth_BluetoothAdapter__startDiscovery      =  75;
    public static final int
           SEEMP_API_android_bluetooth_BluetoothAdapter__listenUsingInsecureRfcommWithServiceRecord
           =  76;
    public static final int
           SEEMP_API_android_bluetooth_BluetoothAdapter__listenUsingSecureRfcommWithServiceRecord
           =  77;
    public static final int SEEMP_API_android_bluetooth_BluetoothAdapter__getBondedDevices    =  78;
    public static final int SEEMP_API_android_bluetooth_BluetoothAdapter__getRemoteDevice     =  79;
    public static final int SEEMP_API_android_bluetooth_BluetoothAdapter__getState            =  80;
    public static final int
           SEEMP_API_android_bluetooth_BluetoothAdapter__getProfileConnectionState            =  81;
    public static final int SEEMP_API_Camera__takePicture                                     =  82;
    public static final int SEEMP_API_Camera__setPreviewCallback                              =  83;
    public static final int SEEMP_API_Camera__setPreviewCallbackWithBuffer                    =  84;
    public static final int SEEMP_API_Camera__setOneShotPreviewCallback                       =  85;
    public static final int SEEMP_API_MediaRecorder__start                                    =  86;
    public static final int SEEMP_API_MediaRecorder__stop                                     =  87;
    public static final int SEEMP_API_AudioRecord__startRecording                             =  88;
    public static final int SEEMP_API_AudioRecord__start                                      =  89;
    public static final int SEEMP_API_AudioRecord__stop                                       =  90;
    public static final int SEEMP_API_SpeechRecognizer__startListening                        =  91;
    public static final int SEEMP_API_android_telephony_SmsManager__sendDataMessage           =  92;
    public static final int SEEMP_API_android_telephony_SmsManager__sendMultipartTextMessage  =  93;
    public static final int SEEMP_API_android_telephony_SmsManager__sendTextMessage           =  94;
    public static final int SEEMP_API_android_telephony_gsm_SmsManager__sendDataMessage       =  95;
    public static final int
           SEEMP_API_android_telephony_gsm_SmsManager__sendMultipartTextMessag                =  96;
    public static final int SEEMP_API_android_telephony_gsm_SmsManager__sendTextMessage       =  97;
    public static final int SEEMP_API_android_telephony_SmsManager__copyMessageToIcc          =  98;
    public static final int SEEMP_API_android_telephony_SmsManager__deleteMessageFromIcc      =  99;
    public static final int SEEMP_API_android_telephony_SmsManager__updateMessageOnIcc        = 100;
    public static final int SEEMP_API_android_telephony_gsm_SmsManager__copyMessageToSim      = 101;
    public static final int SEEMP_API_android_telephony_gsm_SmsManager__deleteMessageFromSim  = 102;
    public static final int SEEMP_API_android_telephony_gsm_SmsManager__updateMessageOnSim    = 103;
    public static final int SEEMP_API_android_telephony_gsm_SmsManager__getAllMessagesFromSim = 104;
    public static final int
           SEEMP_API_android_hardware_SensorEventListener__onAccuracyChanged_ACCELEROMETER_   = 105;
    public static final int
           SEEMP_API_android_hardware_SensorEventListener__onSensorChanged_ACCELEROMETER_     = 106;
    public static final int SEEMP_API_android_hardware_SensorManager__registerListener        = 107;
    public static final int SEEMP_API_ASensorEventQueue__enableSensor                         = 108;
    public static final int SEEMP_API_ContactsContract__getLookupUri                          = 109;
    public static final int SEEMP_API_ContactsContract__lookupContact                         = 110;
    public static final int SEEMP_API_ContactsContract__openContactPhotoInputStream           = 111;
    public static final int SEEMP_API_ContactsContract__getContactLookupUri                   = 112;
    public static final int SEEMP_API_PackageManagerService__installPackage                   = 113;
    public static final int SEEMP_API_TelephonyManager__getSubscriberId                       = 114;
    public static final int SEEMP_API_URL__openConnection                                     = 115;
    public static final int SEEMP_API_AccountManager__confirmCredentials                      = 116;
    public static final int SEEMP_API_AccountManager__invalidateAuthToken                     = 117;
    public static final int SEEMP_API_AccountManager__updateCredentials                       = 118;
    public static final int
           SEEMP_API_AccountManager__checkManageAccountsOrUseCredentialsPermissions           = 119;
    public static final int SEEMP_API_AccountManager__checkManageAccountsPermission           = 120;
    public static final int SEEMP_API_AccountManager__peekAuthToken                           = 121;
    public static final int SEEMP_API_AccountManager__setAuthToken                            = 122;
    public static final int SEEMP_API_AccountManager__checkAuthenticateAccountsPermission     = 123;
    public static final int
           SEEMP_API_android_hardware_SensorEventListener__onAccuracyChanged_ORIENTATION_     = 124;
    public static final int
           SEEMP_API_android_hardware_SensorEventListener__onSensorChanged_ORIENTATION_       = 125;
    public static final int SEEMP_API_android_provider_Settings__get_ACCELEROMETER_ROTATION_  = 126;
    public static final int SEEMP_API_android_provider_Settings__get_USER_ROTATION_           = 127;
    public static final int SEEMP_API_android_provider_Settings__get_ADB_ENABLED_             = 128;
    public static final int SEEMP_API_android_provider_Settings__get_DEBUG_APP_               = 129;
    public static final int SEEMP_API_android_provider_Settings__get_WAIT_FOR_DEBUGGER_       = 130;
    public static final int SEEMP_API_android_provider_Settings__get_AIRPLANE_MODE_ON_        = 131;
    public static final int SEEMP_API_android_provider_Settings__get_AIRPLANE_MODE_RADIOS_    = 132;
    public static final int SEEMP_API_android_provider_Settings__get_ALARM_ALERT_             = 133;
    public static final int SEEMP_API_android_provider_Settings__get_NEXT_ALARM_FORMATTED_    = 134;
    public static final int SEEMP_API_android_provider_Settings__get_ALWAYS_FINISH_ACTIVITIES_= 135;
    public static final int SEEMP_API_android_provider_Settings__get_LOGGING_ID_              = 136;
    public static final int SEEMP_API_android_provider_Settings__get_ANIMATOR_DURATION_SCALE_ = 137;
    public static final int SEEMP_API_android_provider_Settings__get_WINDOW_ANIMATION_SCALE_  = 138;
    public static final int SEEMP_API_android_provider_Settings__get_FONT_SCALE_              = 139;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_       = 140;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_MODE_  = 141;
    public static final int
           SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_MODE_AUTOMATIC_         = 142;
    public static final int
           SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_MODE_MANUAL_            = 143;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_OFF_TIMEOUT_      = 144;
    public static final int SEEMP_API_android_provider_Settings__get_DIM_SCREEN_              = 145;
    public static final int
           SEEMP_API_android_provider_Settings__get_TRANSITION_ANIMATION_SCALE_               = 146;
    public static final int SEEMP_API_android_provider_Settings__get_STAY_ON_WHILE_PLUGGED_IN_= 147;
    public static final int SEEMP_API_android_provider_Settings__get_WALLPAPER_ACTIVITY_      = 148;
    public static final int SEEMP_API_android_provider_Settings__get_SHOW_PROCESSES_          = 149;
    public static final int SEEMP_API_android_provider_Settings__get_SHOW_WEB_SUGGESTIONS_    = 150;
    public static final int
           SEEMP_API_android_provider_Settings__get_SHOW_GTALK_SERVICE_STATUS_                = 151;
    public static final int SEEMP_API_android_provider_Settings__get_USE_GOOGLE_MAIL_         = 152;
    public static final int SEEMP_API_android_provider_Settings__get_AUTO_TIME_               = 153;
    public static final int SEEMP_API_android_provider_Settings__get_AUTO_TIME_ZONE_          = 154;
    public static final int SEEMP_API_android_provider_Settings__get_DATE_FORMAT_             = 155;
    public static final int SEEMP_API_android_provider_Settings__get_TIME_12_24_              = 156;
    public static final int
           SEEMP_API_android_provider_Settings__get_BLUETOOTH_DISCOVERABILITY_                = 157;
    public static final int
           SEEMP_API_android_provider_Settings__get_BLUETOOTH_DISCOVERABILITY_TIMEOUT_        = 158;
    public static final int SEEMP_API_android_provider_Settings__get_BLUETOOTH_ON_            = 159;
    public static final int SEEMP_API_android_provider_Settings__get_DEVICE_PROVISIONED_      = 160;
    public static final int SEEMP_API_android_provider_Settings__get_SETUP_WIZARD_HAS_RUN_    = 161;
    public static final int SEEMP_API_android_provider_Settings__get_DTMF_TONE_WHEN_DIALING_  = 162;
    public static final int SEEMP_API_android_provider_Settings__get_END_BUTTON_BEHAVIOR_     = 163;
    public static final int SEEMP_API_android_provider_Settings__get_RINGTONE_                = 164;
    public static final int SEEMP_API_android_provider_Settings__get_MODE_RINGER_             = 165;
    public static final int SEEMP_API_android_provider_Settings__get_INSTALL_NON_MARKET_APPS_ = 166;
    public static final int
           SEEMP_API_android_provider_Settings__get_LOCATION_PROVIDERS_ALLOWED_               = 167;
    public static final int SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_ENABLED_    = 168;
    public static final int
           SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED_    = 169;
    public static final int SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_VISIBLE_    = 170;
    public static final int SEEMP_API_android_provider_Settings__get_NETWORK_PREFERENCE_      = 171;
    public static final int SEEMP_API_android_provider_Settings__get_DATA_ROAMING_            = 172;
    public static final int SEEMP_API_android_provider_Settings__get_HTTP_PROXY_              = 173;
    public static final int SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_ENABLED_= 174;
    public static final int
           SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_LAST_UPDATE_             = 175;
    public static final int
           SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_REDIRECT_URL_            = 176;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_BLUETOOTH_         = 177;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_CELL_              = 178;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_NFC_               = 179;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_WIFI_              = 180;
    public static final int SEEMP_API_android_provider_Settings__get_SYS_PROP_SETTING_VERSION_= 181;
    public static final int SEEMP_API_android_provider_Settings__get_SETTINGS_CLASSNAME_      = 182;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_AUTO_CAPS_          = 183;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_AUTO_PUNCTUATE_     = 184;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_AUTO_REPLACE_       = 185;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_SHOW_PASSWORD_      = 186;
    public static final int SEEMP_API_android_provider_Settings__get_USB_MASS_STORAGE_ENABLED_= 187;
    public static final int SEEMP_API_android_provider_Settings__get_VIBRATE_ON_              = 188;
    public static final int SEEMP_API_android_provider_Settings__get_HAPTIC_FEEDBACK_ENABLED_ = 189;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_ALARM_            = 190;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_BLUETOOTH_SCO_    = 191;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_MUSIC_            = 192;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_NOTIFICATION_     = 193;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_RING_             = 194;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_SYSTEM_           = 195;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_VOICE_            = 196;
    public static final int SEEMP_API_android_provider_Settings__get_SOUND_EFFECTS_ENABLED_   = 197;
    public static final int
           SEEMP_API_android_provider_Settings__get_MODE_RINGER_STREAMS_AFFECTED_             = 198;
    public static final int SEEMP_API_android_provider_Settings__get_MUTE_STREAMS_AFFECTED_   = 199;
    public static final int SEEMP_API_android_provider_Settings__get_NOTIFICATION_SOUND_      = 200;
    public static final int SEEMP_API_android_provider_Settings__get_APPEND_FOR_LAST_AUDIBLE_ = 201;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_MAX_DHCP_RETRY_COUNT_                = 202;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS_
           = 203;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON_  = 204;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY_     = 205;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_NUM_OPEN_NETWORKS_KEPT_              = 206;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_ON_                 = 207;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_       = 208;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_DEFAULT_                = 209;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_NEVER_ = 210;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED_    = 211;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_DNS1_        = 212;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_DNS2_        = 213;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_GATEWAY_     = 214;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_IP_          = 215;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_NETMASK_     = 216;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_USE_STATIC_IP_      = 217;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE_
           = 218;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_AP_COUNT_  = 219;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS_  = 220;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED_   = 221;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS_= 222;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT_ = 223;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_MAX_AP_CHECKS_              = 224;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_ON_        = 225;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_COUNT_= 226;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_DELAY_MS_              = 227;
    public static final int
           SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_TIMEOUT_MS_            = 228;
    public static final int SEEMP_API_android_provider_Settings__put_ACCELEROMETER_ROTATION_  = 229;
    public static final int SEEMP_API_android_provider_Settings__put_USER_ROTATION_           = 230;
    public static final int SEEMP_API_android_provider_Settings__put_ADB_ENABLED_             = 231;
    public static final int SEEMP_API_android_provider_Settings__put_DEBUG_APP_               = 232;
    public static final int SEEMP_API_android_provider_Settings__put_WAIT_FOR_DEBUGGER_       = 233;
    public static final int SEEMP_API_android_provider_Settings__put_AIRPLANE_MODE_ON_        = 234;
    public static final int SEEMP_API_android_provider_Settings__put_AIRPLANE_MODE_RADIOS_    = 235;
    public static final int SEEMP_API_android_provider_Settings__put_ALARM_ALERT_             = 236;
    public static final int SEEMP_API_android_provider_Settings__put_NEXT_ALARM_FORMATTED_    = 237;
    public static final int SEEMP_API_android_provider_Settings__put_ALWAYS_FINISH_ACTIVITIES_= 238;
    public static final int SEEMP_API_android_provider_Settings__put_ANDROID_ID_              = 239;
    public static final int SEEMP_API_android_provider_Settings__put_LOGGING_ID_              = 240;
    public static final int SEEMP_API_android_provider_Settings__put_ANIMATOR_DURATION_SCALE_ = 241;
    public static final int SEEMP_API_android_provider_Settings__put_WINDOW_ANIMATION_SCALE_  = 242;
    public static final int SEEMP_API_android_provider_Settings__put_FONT_SCALE_              = 243;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_       = 244;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_MODE_  = 245;
    public static final int
           SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_MODE_AUTOMATIC_         = 246;
    public static final int
           SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_MODE_MANUAL_            = 247;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_OFF_TIMEOUT_      = 248;
    public static final int SEEMP_API_android_provider_Settings__put_DIM_SCREEN_              = 249;
    public static final int
           SEEMP_API_android_provider_Settings__put_TRANSITION_ANIMATION_SCALE_               = 250;
    public static final int SEEMP_API_android_provider_Settings__put_STAY_ON_WHILE_PLUGGED_IN_= 251;
    public static final int SEEMP_API_android_provider_Settings__put_WALLPAPER_ACTIVITY_      = 252;
    public static final int SEEMP_API_android_provider_Settings__put_SHOW_PROCESSES_          = 253;
    public static final int SEEMP_API_android_provider_Settings__put_SHOW_WEB_SUGGESTIONS_    = 254;
    public static final int
           SEEMP_API_android_provider_Settings__put_SHOW_GTALK_SERVICE_STATUS_                = 255;
    public static final int SEEMP_API_android_provider_Settings__put_USE_GOOGLE_MAIL_         = 256;
    public static final int SEEMP_API_android_provider_Settings__put_AUTO_TIME_               = 257;
    public static final int SEEMP_API_android_provider_Settings__put_AUTO_TIME_ZONE_          = 258;
    public static final int SEEMP_API_android_provider_Settings__put_DATE_FORMAT_             = 259;
    public static final int SEEMP_API_android_provider_Settings__put_TIME_12_24_              = 260;
    public static final int
           SEEMP_API_android_provider_Settings__put_BLUETOOTH_DISCOVERABILITY_                = 261;
    public static final int
           SEEMP_API_android_provider_Settings__put_BLUETOOTH_DISCOVERABILITY_TIMEOUT_        = 262;
    public static final int SEEMP_API_android_provider_Settings__put_BLUETOOTH_ON_            = 263;
    public static final int SEEMP_API_android_provider_Settings__put_DEVICE_PROVISIONED_      = 264;
    public static final int SEEMP_API_android_provider_Settings__put_SETUP_WIZARD_HAS_RUN_    = 265;
    public static final int SEEMP_API_android_provider_Settings__put_DTMF_TONE_WHEN_DIALING_  = 266;
    public static final int SEEMP_API_android_provider_Settings__put_END_BUTTON_BEHAVIOR_     = 267;
    public static final int SEEMP_API_android_provider_Settings__put_RINGTONE_                = 268;
    public static final int SEEMP_API_android_provider_Settings__put_MODE_RINGER_             = 269;
    public static final int SEEMP_API_android_provider_Settings__put_INSTALL_NON_MARKET_APPS_ = 270;
    public static final int
           SEEMP_API_android_provider_Settings__put_LOCATION_PROVIDERS_ALLOWED_               = 271;
    public static final int SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_ENABLED_    = 272;
    public static final int
           SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED_    = 273;
    public static final int SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_VISIBLE_    = 274;
    public static final int SEEMP_API_android_provider_Settings__put_NETWORK_PREFERENCE_      = 275;
    public static final int SEEMP_API_android_provider_Settings__put_DATA_ROAMING_            = 276;
    public static final int SEEMP_API_android_provider_Settings__put_HTTP_PROXY_              = 277;
    public static final int SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_ENABLED_= 278;
    public static final int
           SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_LAST_UPDATE_             = 279;
    public static final int
           SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_REDIRECT_URL_            = 280;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_BLUETOOTH_         = 281;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_CELL_              = 282;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_NFC_               = 283;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_WIFI_              = 284;
    public static final int SEEMP_API_android_provider_Settings__put_SYS_PROP_SETTING_VERSION_= 285;
    public static final int SEEMP_API_android_provider_Settings__put_SETTINGS_CLASSNAME_      = 286;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_AUTO_CAPS_          = 287;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_AUTO_PUNCTUATE_     = 288;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_AUTO_REPLACE_       = 289;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_SHOW_PASSWORD_      = 290;
    public static final int SEEMP_API_android_provider_Settings__put_USB_MASS_STORAGE_ENABLED_= 291;
    public static final int SEEMP_API_android_provider_Settings__put_VIBRATE_ON_              = 292;
    public static final int SEEMP_API_android_provider_Settings__put_HAPTIC_FEEDBACK_ENABLED_ = 293;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_ALARM_            = 294;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_BLUETOOTH_SCO_    = 295;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_MUSIC_            = 296;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_NOTIFICATION_     = 297;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_RING_             = 298;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_SYSTEM_           = 299;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_VOICE_            = 300;
    public static final int SEEMP_API_android_provider_Settings__put_SOUND_EFFECTS_ENABLED_   = 301;
    public static final int
           SEEMP_API_android_provider_Settings__put_MODE_RINGER_STREAMS_AFFECTED_             = 302;
    public static final int SEEMP_API_android_provider_Settings__put_MUTE_STREAMS_AFFECTED_   = 303;
    public static final int SEEMP_API_android_provider_Settings__put_NOTIFICATION_SOUND_      = 304;
    public static final int SEEMP_API_android_provider_Settings__put_APPEND_FOR_LAST_AUDIBLE_ = 305;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_MAX_DHCP_RETRY_COUNT_                = 306;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS_
           = 307;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON_  = 308;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY_     = 309;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_NUM_OPEN_NETWORKS_KEPT_              = 310;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_ON_                 = 311;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_       = 312;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_DEFAULT_                = 313;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_NEVER_ = 314;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED_    = 315;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_DNS1_        = 316;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_DNS2_        = 317;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_GATEWAY_     = 318;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_IP_          = 319;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_NETMASK_     = 320;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_USE_STATIC_IP_      = 321;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE_
           = 322;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_AP_COUNT_  = 323;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS_  = 324;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED_   = 325;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS_= 326;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT_ = 327;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_MAX_AP_CHECKS_              = 328;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_ON_        = 329;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_COUNT_= 330;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_DELAY_MS_              = 331;
    public static final int
           SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_TIMEOUT_MS_            = 332;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxMBytes                         = 333;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxMBytes                      = 334;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxPackets                        = 335;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxPackets                     = 336;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxMBytes                         = 337;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxMBytes                      = 338;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxPackets                        = 339;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxPackets                     = 340;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxTcpMBytes                      = 341;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxTcpMBytes                   = 342;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxTcpPackets                     = 343;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxTcpPackets                  = 344;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxUdpMBytes                      = 345;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxUdpMBytes                   = 346;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxUdpPackets                     = 347;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxUdpPackets                  = 348;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxOtherMBytes                    = 349;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxOtherMBytes                 = 350;
    public static final int SEEMP_API_Poll__setCumulativeWifiRxOtherPackets                   = 351;
    public static final int SEEMP_API_Poll__setInstantaneousWifiRxOtherPackets                = 352;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxTcpMBytes                      = 353;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxTcpMBytes                   = 354;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxTcpPackets                     = 355;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxTcpPackets                  = 356;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxUdpMBytes                      = 357;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxUdpMBytes                   = 358;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxUdpPackets                     = 359;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxUdpPackets                  = 360;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxOtherMBytes                    = 361;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxOtherMBytes                 = 362;
    public static final int SEEMP_API_Poll__setCumulativeWifiTxOtherPackets                   = 363;
    public static final int SEEMP_API_Poll__setInstantaneousWifiTxOtherPackets                = 364;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxMBytes                       = 365;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxMBytes                    = 366;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxPackets                      = 367;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxPackets                   = 368;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxMBytes                       = 369;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxMBytes                    = 370;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxPackets                      = 371;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxPackets                   = 372;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxTcpMBytes                    = 373;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxTcpMBytes                 = 374;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxTcpPackets                   = 375;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxTcpPackets                = 376;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxUdpMBytes                    = 377;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxUdpMBytes                 = 378;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxUdpPackets                   = 379;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxUdpPackets                = 380;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxOtherMBytes                  = 381;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxOtherMBytes               = 382;
    public static final int SEEMP_API_Poll__setCumulativeMobileRxOtherPackets                 = 383;
    public static final int SEEMP_API_Poll__setInstantaneousMobileRxOtherPackets              = 384;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxTcpMBytes                    = 385;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxTcpMBytes                 = 386;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxTcpPackets                   = 387;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxTcpPackets                = 388;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxUdpMBytes                    = 389;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxUdpMBytes                 = 390;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxUdpPackets                   = 391;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxUdpPackets                = 392;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxOtherMBytes                  = 393;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxOtherMBytes               = 394;
    public static final int SEEMP_API_Poll__setCumulativeMobileTxOtherPackets                 = 395;
    public static final int SEEMP_API_Poll__setInstantaneousMobileTxOtherPackets              = 396;
    public static final int SEEMP_API_Poll__setNumSockets                                     = 397;
    public static final int SEEMP_API_Poll__setNumTcpStateListen                              = 398;
    public static final int SEEMP_API_Poll__setNumTcpStateEstablished                         = 399;
    public static final int SEEMP_API_Poll__setNumLocalIp                                     = 400;
    public static final int SEEMP_API_Poll__setNumLocalPort                                   = 401;
    public static final int SEEMP_API_Poll__setNumRemoteIp                                    = 402;
    public static final int SEEMP_API_Poll__setNumRemotePort                                  = 403;
    public static final int SEEMP_API_Poll__setNumRemoteTuple                                 = 404;
    public static final int SEEMP_API_Poll__setNumInode                                       = 405;
    public static final int SEEMP_API_Instrumentation__startActivitySync                      = 406;
    public static final int SEEMP_API_Instrumentation__execStartActivity                      = 407;
    public static final int SEEMP_API_Instrumentation__execStartActivitiesAsUser              = 408;
    public static final int SEEMP_API_Instrumentation__execStartActivityAsCaller              = 409;
    public static final int SEEMP_API_Instrumentation__execStartActivityFromAppTask           = 410;
}
