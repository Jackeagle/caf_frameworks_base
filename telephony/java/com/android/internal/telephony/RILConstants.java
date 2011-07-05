/*
 * Copyright (C) 2006,2011 The Android Open Source Project
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

package com.android.internal.telephony;

/**
 * TODO: This should probably not be an interface see
 * http://www.javaworld.com/javaworld/javaqa/2001-06/01-qa-0608-constants.html and google with
 * http://www.google.com/search?q=interface+constants&ie=utf-8&oe=utf-8&aq=t&rls=com.ubuntu:en-US:unofficial&client=firefox-a
 *
 * Also they should all probably be static final.
 */

/**
 * {@hide}
 */
public interface RILConstants {
    int RIL_MAX_NETWORKS = 1;                 // (from ril.h)
    // From the top of ril.cpp
    int RIL_ERRNO_INVALID_RESPONSE = -1;

    // from RIL_Errno
    int SUCCESS = 0;
    int RADIO_NOT_AVAILABLE = 1;              /* If radio did not start or is resetting */
    int GENERIC_FAILURE = 2;
    int PASSWORD_INCORRECT = 3;               /* for PIN/PIN2 methods only! */
    int SIM_PIN2 = 4;                         /* Operation requires SIM PIN2 to be entered */
    int SIM_PUK2 = 5;                         /* Operation requires SIM PIN2 to be entered */
    int REQUEST_NOT_SUPPORTED = 6;
    int REQUEST_CANCELLED = 7;
    int OP_NOT_ALLOWED_DURING_VOICE_CALL = 8; /* data operation is not allowed during voice call in
                                                 class C */
    int OP_NOT_ALLOWED_BEFORE_REG_NW = 9;     /* request is not allowed before device registers to
                                                 network */
    int SMS_SEND_FAIL_RETRY = 10;             /* send sms fail and need retry */
    int SIM_ABSENT = 11;                      /* ICC card is absent */
    int SUBSCRIPTION_NOT_AVAILABLE = 12;      /* fail to find CDMA subscription from specified
                                                 location */
    int MODE_NOT_SUPPORTED = 13;              /* HW does not support preferred network type */
    int FDN_CHECK_FAILURE = 14;               /* send operation barred error when FDN is enabled */
    int ILLEGAL_SIM_OR_ME = 15;               /* network selection failure due
                                                 to wrong SIM/ME and no
                                                 retries needed */
    int DIAL_MODIFIED_TO_USSD = 17;           /* DIAL request modified to USSD */
    int DIAL_MODIFIED_TO_SS = 18;             /* DIAL request modified to SS */
    int DIAL_MODIFIED_TO_DIAL = 19;           /* DIAL request modified to DIAL with different data */
    int USSD_MODIFIED_TO_DIAL = 20;           /* USSD request modified to DIAL */
    int USSD_MODIFIED_TO_SS = 21;             /* USSD request modified to SS */
    int USSD_MODIFIED_TO_USSD = 22;           /* USSD request modified to different USSD request */
    int SS_MODIFIED_TO_DIAL = 23;             /* SS request modified to DIAL */
    int SS_MODIFIED_TO_USSD = 24;             /* SS request modified to USSD */
    int SS_MODIFIED_TO_SS = 25;               /* SS request modified to different SS request */
    int SUBSCRIPTION_NOT_SUPPORTED = 26;      /* Subscription not supported */

    /* NETWORK_MODE_* See ril.h RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE */
    int NETWORK_MODE_WCDMA_PREF     = 0; /* GSM/WCDMA (WCDMA preferred) */
    int NETWORK_MODE_GSM_ONLY       = 1; /* GSM only */
    int NETWORK_MODE_WCDMA_ONLY     = 2; /* WCDMA only */
    int NETWORK_MODE_GSM_UMTS       = 3; /* GSM/WCDMA (auto mode, according to PRL)
                                            AVAILABLE Application Settings menu*/
    int NETWORK_MODE_CDMA           = 4; /* CDMA and EvDo (auto mode, according to PRL)
                                            AVAILABLE Application Settings menu*/
    int NETWORK_MODE_CDMA_NO_EVDO   = 5; /* CDMA only */
    int NETWORK_MODE_EVDO_NO_CDMA   = 6; /* EvDo only */
    int NETWORK_MODE_GLOBAL         = 7; /* GSM/WCDMA, CDMA, and EvDo (auto mode, according to PRL)
                                            AVAILABLE Application Settings menu*/
    int NETWORK_MODE_CDMA_AND_LTE_EVDO  = 8;  /* CDMA + LTE/EvDo auto */
    int NETWORK_MODE_GSM_WCDMA_LTE      = 9;  /* GSM/WCDMA/LTE auto */
    int NETWORK_MODE_GLOBAL_LTE         = 10; /* CDMA/EvDo/GSM/WCDMA/LTE auto */
    int NETWORK_MODE_LTE_ONLY           = 11; /* LTE only */
    int PREFERRED_NETWORK_MODE      = NETWORK_MODE_WCDMA_PREF;

    /* CDMA subscription source. See ril.h RIL_REQUEST_CDMA_SET_SUBSCRIPTION */
    int SUBSCRIPTION_FROM_RUIM      = 0; /* CDMA subscription from RUIM when available */
    int SUBSCRIPTION_FROM_NV        = 1; /* CDMA subscription from NV */
    int PREFERRED_CDMA_SUBSCRIPTION = SUBSCRIPTION_FROM_NV;

    int CDMA_CELL_BROADCAST_SMS_DISABLED = 1;
    int CDMA_CELL_BROADCAST_SMS_ENABLED  = 0;

    int NO_PHONE = 0;
    int GSM_PHONE = 1;
    int CDMA_PHONE = 2;
    int SIP_PHONE  = 3;

    int CDM_TTY_MODE_DISABLED = 0;
    int CDM_TTY_MODE_ENABLED = 1;

    int CDM_TTY_FULL_MODE = 1;
    int CDM_TTY_HCO_MODE = 2;
    int CDM_TTY_VCO_MODE = 3;

    /* Setup a packet data connection. See ril.h RIL_REQUEST_SETUP_DATA_CALL */
    int SETUP_DATA_TECH_CDMA      = 0;
    int SETUP_DATA_TECH_GSM       = 1;

    int SETUP_DATA_AUTH_NONE      = 0;
    int SETUP_DATA_AUTH_PAP       = 1;
    int SETUP_DATA_AUTH_CHAP      = 2;
    int SETUP_DATA_AUTH_PAP_CHAP  = 3;

    String SETUP_DATA_PROTOCOL_IP     = "IP";
    String SETUP_DATA_PROTOCOL_IPV6   = "IPV6";
    String SETUP_DATA_PROTOCOL_IPV4V6 = "IPV4V6";

    /* QoS constants */
    public class RIL_QosClass {
        public static final int RIL_QOS_CONVERSATIONAL = 0;
        public static final int RIL_QOS_STREAMING       = 1;
        public static final int RIL_QOS_INTERACTIVE     = 2;
        public static final int RIL_QOS_BACKGROUND      = 3;

        public static String getName(int val) {
            switch(val) {
                case RIL_QOS_CONVERSATIONAL: return "RIL_QOS_CONVERSATIONAL";
                case RIL_QOS_STREAMING: return "RIL_QOS_STREAMING";
                case RIL_QOS_INTERACTIVE: return "RIL_QOS_INTERACTIVE";
                case RIL_QOS_BACKGROUND: return "RIL_QOS_BACKGROUND";
                default: return null;
            }
        }
    }

    public class RIL_QosDirection {
        public static final int RIL_QOS_TX = 0;
        public static final int RIL_QOS_RX = 1;

        public static String getName(int val) {
            switch(val) {
                case RIL_QOS_TX: return "RIL_QOS_TX";
                case RIL_QOS_RX: return "RIL_QOS_RX";
                default: return null;
            }
        }
    }

    public static class RIL_QosSpecKeys {
        /* Positive numerical value */
        public static final int RIL_QOS_SPEC_INDEX = 0;

        /* RIL_QosDirection */
        public static final int RIL_QOS_FLOW_DIRECTION = 1;
        /* RIL_QosClass */
        public static final int RIL_QOS_FLOW_TRAFFIC_CLASS = 2;
        /* Positive number in kbps */
        public static final int RIL_QOS_FLOW_DATA_RATE_MIN = 3;
        /* Positive number in kbps */
        public static final int RIL_QOS_FLOW_DATA_RATE_MAX = 4;
        /* Positive number in milliseconds */
        public static final int RIL_QOS_FLOW_LATENCY = 5;

        /* Positive numerical value */
        public static final int RIL_QOS_FLOW_3GPP2_PROFILE_ID = 6;
        /* Positive numerical value */
        public static final int RIL_QOS_FLOW_3GPP2_PRIORITY = 7;

        /* RIL_QosDirection */
        public static final int RIL_QOS_FILTER_DIRECTION = 8;
        /* Format: xxx.xxx.xxx.xxx/yy */
        public static final int RIL_QOS_FILTER_IPV4_SOURCE_ADDR = 9;
        /* Format: xxx.xxx.xxx.xxx/yy */
        public static final int RIL_QOS_FILTER_IPV4_DESTINATION_ADDR = 10;
        /* Positive numerical Value (max 6-bit number) */
        public static final int RIL_QOS_FILTER_IPV4_TOS = 11;
        /* Mask for the 6 bit TOS value */
        public static final int RIL_QOS_FILTER_IPV4_TOS_MASK = 12;

        /**
         * *PORT_START is the starting port number
         * *PORT_RANGE is the number of continuous ports from *PORT_START key
         */
        public static final int RIL_QOS_FILTER_TCP_SOURCE_PORT_START = 13;
        public static final int RIL_QOS_FILTER_TCP_SOURCE_PORT_RANGE = 14;
        public static final int RIL_QOS_FILTER_TCP_DESTINATION_PORT_START = 15;
        public static final int RIL_QOS_FILTER_TCP_DESTINATION_PORT_RANGE = 16;
        public static final int RIL_QOS_FILTER_UDP_SOURCE_PORT_START = 17;
        public static final int RIL_QOS_FILTER_UDP_SOURCE_PORT_RANGE = 18;
        public static final int RIL_QOS_FILTER_UDP_DESTINATION_PORT_START = 19;
        public static final int RIL_QOS_FILTER_UDP_DESTINATION_PORT_RANGE = 20;

        /* TBD: For future implemenations based on requirements */
        public static final int RIL_QOS_FILTER_IP_NEXT_HEADER_PROTOCOL = 21;
        /* Format: xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx/yyy */
        public static final int RIL_QOS_FILTER_IPV6_SOURCE_ADDR = 22;
        /* Format: xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx/yyy */
        public static final int RIL_QOS_FILTER_IPV6_DESTINATION_ADDR = 23;
        public static final int RIL_QOS_FILTER_IPV6_TRAFFIC_CLASS = 24;
        public static final int RIL_QOS_FILTER_IPV6_FLOW_LABEL = 25;

        public static String getName(int val) {
            switch(val) {
                case RIL_QOS_SPEC_INDEX:
                    return "RIL_QOS_SPEC_INDEX";
                case RIL_QOS_FLOW_DIRECTION:
                    return "RIL_QOS_FLOW_DIRECTION";
                case RIL_QOS_FLOW_TRAFFIC_CLASS:
                    return "RIL_QOS_FLOW_TRAFFIC_CLASS";
                case RIL_QOS_FLOW_DATA_RATE_MIN:
                    return "RIL_QOS_FLOW_DATA_RATE_MIN";
                case RIL_QOS_FLOW_DATA_RATE_MAX:
                    return "RIL_QOS_FLOW_DATA_RATE_MAX";
                case RIL_QOS_FLOW_LATENCY:
                    return "RIL_QOS_FLOW_LATENCY";
                case RIL_QOS_FLOW_3GPP2_PROFILE_ID:
                    return "RIL_QOS_FLOW_3GPP2_PROFILE_ID";
                case RIL_QOS_FLOW_3GPP2_PRIORITY:
                    return "RIL_QOS_FLOW_3GPP2_PRIORITY";
                case RIL_QOS_FILTER_DIRECTION:
                    return "RIL_QOS_FILTER_DIRECTION";
                case RIL_QOS_FILTER_IPV4_SOURCE_ADDR:
                    return "RIL_QOS_FILTER_IPV4_SOURCE_ADDR";
                case RIL_QOS_FILTER_IPV4_DESTINATION_ADDR:
                    return "RIL_QOS_FILTER_IPV4_DESTINATION_ADDR";
                case RIL_QOS_FILTER_IPV4_TOS:
                    return "RIL_QOS_FILTER_IPV4_TOS";
                case RIL_QOS_FILTER_IPV4_TOS_MASK:
                    return "RIL_QOS_FILTER_IPV4_TOS_MASK";
                case RIL_QOS_FILTER_TCP_SOURCE_PORT_START:
                    return "RIL_QOS_FILTER_TCP_SOURCE_PORT_START";
                case RIL_QOS_FILTER_TCP_SOURCE_PORT_RANGE:
                    return "RIL_QOS_FILTER_TCP_SOURCE_PORT_RANGE";
                case RIL_QOS_FILTER_TCP_DESTINATION_PORT_START:
                    return "RIL_QOS_FILTER_TCP_DESTINATION_PORT_START";
                case RIL_QOS_FILTER_TCP_DESTINATION_PORT_RANGE:
                    return "RIL_QOS_FILTER_TCP_DESTINATION_PORT_RANGE";
                case RIL_QOS_FILTER_UDP_SOURCE_PORT_START:
                    return "RIL_QOS_FILTER_UDP_SOURCE_PORT_START";
                case RIL_QOS_FILTER_UDP_SOURCE_PORT_RANGE:
                    return "RIL_QOS_FILTER_UDP_SOURCE_PORT_RANGE";
                case RIL_QOS_FILTER_UDP_DESTINATION_PORT_START:
                    return "RIL_QOS_FILTER_UDP_DESTINATION_PORT_START";
                case RIL_QOS_FILTER_UDP_DESTINATION_PORT_RANGE:
                    return "RIL_QOS_FILTER_UDP_DESTINATION_PORT_RANGE";
                case RIL_QOS_FILTER_IP_NEXT_HEADER_PROTOCOL:
                    return "RIL_QOS_FILTER_IP_NEXT_HEADER_PROTOCOL";
                case RIL_QOS_FILTER_IPV6_SOURCE_ADDR:
                    return "RIL_QOS_FILTER_IPV6_SOURCE_ADDR";
                case RIL_QOS_FILTER_IPV6_DESTINATION_ADDR:
                    return "RIL_QOS_FILTER_IPV6_DESTINATION_ADDR";
                case RIL_QOS_FILTER_IPV6_TRAFFIC_CLASS:
                    return "RIL_QOS_FILTER_IPV6_TRAFFIC_CLASS";
                case RIL_QOS_FILTER_IPV6_FLOW_LABEL:
                    return "RIL_QOS_FILTER_IPV6_FLOW_LABEL";
                default:
                    return null;
            }
        }
    }

    /* Overall QoS status */
    public class RIL_QosStatus {
        /* Qos not active */
        public static final int RIL_QOS_STATUS_NONE      = 0;
        /* Qos currently active */
        public static final int RIL_QOS_STATUS_ACTIVATED = 1;
        /* Qos Suspended */
        public static final int RIL_QOS_STATUS_SUSPENDED = 2;
    }

    public static class RIL_QosIndStates {
        /* QoS operation complete */
        public static final int RIL_QOS_SUCCESS         = 0;
        /* QoS setup resulted in a neogtiated value */
        public static final int RIL_QOS_NEGOTIATED      = 1;
        /* QoS released by the user */
        public static final int RIL_QOS_USER_RELEASE    = 2;
        /* QoS released by the network */
        public static final int RIL_QOS_NETWORK_RELEASE = 3;
        /* Any other error */
        public static final int RIL_QOS_ERROR_UNKNOWN   = 4;
    }

/*
cat include/telephony/ril.h | \
   egrep '^#define' | \
   sed -re 's/^#define +([^ ]+)* +([^ ]+)/    int \1 = \2;/' \
   >>java/android/com.android.internal.telephony/gsm/RILConstants.java
*/

    /**
     * No restriction at all including voice/SMS/USSD/SS/AV64
     * and packet data.
     */
    int RIL_RESTRICTED_STATE_NONE = 0x00;
    /**
     * Block emergency call due to restriction.
     * But allow all normal voice/SMS/USSD/SS/AV64.
     */
    int RIL_RESTRICTED_STATE_CS_EMERGENCY = 0x01;
    /**
     * Block all normal voice/SMS/USSD/SS/AV64 due to restriction.
     * Only Emergency call allowed.
     */
    int RIL_RESTRICTED_STATE_CS_NORMAL = 0x02;
    /**
     * Block all voice/SMS/USSD/SS/AV64
     * including emergency call due to restriction.
     */
    int RIL_RESTRICTED_STATE_CS_ALL = 0x04;
    /**
     * Block packet data access due to restriction.
     */
    int RIL_RESTRICTED_STATE_PS_ALL = 0x10;

    /** Data profile for RIL_REQUEST_SETUP_DATA_CALL */
    static final int DATA_PROFILE_DEFAULT   = 0;
    static final int DATA_PROFILE_TETHERED  = 1;
    static final int DATA_PROFILE_OEM_BASE  = 1000;


    /** Tethered mode on/off indication for RIL_UNSOL_TETHERED_MODE_STATE_CHANGED */
    int RIL_TETHERED_MODE_ON = 1;
    int RIL_TETHERED_MODE_OFF = 0;

    /** Deactivate data call reasons */
    int DEACTIVATE_REASON_NONE      = 0;
    int DEACTIVATE_REASON_RADIO_OFF = 1;

    /* Modem transmit power levels as per FCC regulations */
    static final int TRANSMIT_POWER_DEFAULT = 0;
    static final int TRANSMIT_POWER_WIFI_HOTSPOT = 1;

    int RIL_REQUEST_GET_SIM_STATUS = 1;
    int RIL_REQUEST_ENTER_SIM_PIN = 2;
    int RIL_REQUEST_ENTER_SIM_PUK = 3;
    int RIL_REQUEST_ENTER_SIM_PIN2 = 4;
    int RIL_REQUEST_ENTER_SIM_PUK2 = 5;
    int RIL_REQUEST_CHANGE_SIM_PIN = 6;
    int RIL_REQUEST_CHANGE_SIM_PIN2 = 7;
    int RIL_REQUEST_ENTER_DEPERSONALIZATION_CODE = 8;
    int RIL_REQUEST_GET_CURRENT_CALLS = 9;
    int RIL_REQUEST_DIAL = 10;
    int RIL_REQUEST_GET_IMSI = 11;
    int RIL_REQUEST_HANGUP = 12;
    int RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND = 13;
    int RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND = 14;
    int RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE = 15;
    int RIL_REQUEST_CONFERENCE = 16;
    int RIL_REQUEST_UDUB = 17;
    int RIL_REQUEST_LAST_CALL_FAIL_CAUSE = 18;
    int RIL_REQUEST_SIGNAL_STRENGTH = 19;
    int RIL_REQUEST_VOICE_REGISTRATION_STATE = 20;
    int RIL_REQUEST_DATA_REGISTRATION_STATE = 21;
    int RIL_REQUEST_OPERATOR = 22;
    int RIL_REQUEST_RADIO_POWER = 23;
    int RIL_REQUEST_DTMF = 24;
    int RIL_REQUEST_SEND_SMS = 25;
    int RIL_REQUEST_SEND_SMS_EXPECT_MORE = 26;
    int RIL_REQUEST_SETUP_DATA_CALL = 27;
    int RIL_REQUEST_SIM_IO = 28;
    int RIL_REQUEST_SEND_USSD = 29;
    int RIL_REQUEST_CANCEL_USSD = 30;
    int RIL_REQUEST_GET_CLIR = 31;
    int RIL_REQUEST_SET_CLIR = 32;
    int RIL_REQUEST_QUERY_CALL_FORWARD_STATUS = 33;
    int RIL_REQUEST_SET_CALL_FORWARD = 34;
    int RIL_REQUEST_QUERY_CALL_WAITING = 35;
    int RIL_REQUEST_SET_CALL_WAITING = 36;
    int RIL_REQUEST_SMS_ACKNOWLEDGE = 37;
    int RIL_REQUEST_GET_IMEI = 38;
    int RIL_REQUEST_GET_IMEISV = 39;
    int RIL_REQUEST_ANSWER = 40;
    int RIL_REQUEST_DEACTIVATE_DATA_CALL = 41;
    int RIL_REQUEST_QUERY_FACILITY_LOCK = 42;
    int RIL_REQUEST_SET_FACILITY_LOCK = 43;
    int RIL_REQUEST_CHANGE_BARRING_PASSWORD = 44;
    int RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE = 45;
    int RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC = 46;
    int RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL = 47;
    int RIL_REQUEST_QUERY_AVAILABLE_NETWORKS = 48;
    int RIL_REQUEST_DTMF_START = 49;
    int RIL_REQUEST_DTMF_STOP = 50;
    int RIL_REQUEST_BASEBAND_VERSION = 51;
    int RIL_REQUEST_SEPARATE_CONNECTION = 52;
    int RIL_REQUEST_SET_MUTE = 53;
    int RIL_REQUEST_GET_MUTE = 54;
    int RIL_REQUEST_QUERY_CLIP = 55;
    int RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE = 56;
    int RIL_REQUEST_DATA_CALL_LIST = 57;
    int RIL_REQUEST_RESET_RADIO = 58;
    int RIL_REQUEST_OEM_HOOK_RAW = 59;
    int RIL_REQUEST_OEM_HOOK_STRINGS = 60;
    int RIL_REQUEST_SCREEN_STATE = 61;
    int RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION = 62;
    int RIL_REQUEST_WRITE_SMS_TO_SIM = 63;
    int RIL_REQUEST_DELETE_SMS_ON_SIM = 64;
    int RIL_REQUEST_SET_BAND_MODE = 65;
    int RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE = 66;
    int RIL_REQUEST_STK_GET_PROFILE = 67;
    int RIL_REQUEST_STK_SET_PROFILE = 68;
    int RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND = 69;
    int RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE = 70;
    int RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM = 71;
    int RIL_REQUEST_EXPLICIT_CALL_TRANSFER = 72;
    int RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE = 73;
    int RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE = 74;
    int RIL_REQUEST_GET_NEIGHBORING_CELL_IDS = 75;
    int RIL_REQUEST_SET_LOCATION_UPDATES = 76;
    int RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE = 77;
    int RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE = 78;
    int RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE = 79;
    int RIL_REQUEST_SET_TTY_MODE = 80;
    int RIL_REQUEST_QUERY_TTY_MODE = 81;
    int RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE = 82;
    int RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE = 83;
    int RIL_REQUEST_CDMA_FLASH = 84;
    int RIL_REQUEST_CDMA_BURST_DTMF = 85;
    int RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY = 86;
    int RIL_REQUEST_CDMA_SEND_SMS = 87;
    int RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE = 88;
    int RIL_REQUEST_GSM_GET_BROADCAST_CONFIG = 89;
    int RIL_REQUEST_GSM_SET_BROADCAST_CONFIG = 90;
    int RIL_REQUEST_GSM_BROADCAST_ACTIVATION = 91;
    int RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG = 92;
    int RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG = 93;
    int RIL_REQUEST_CDMA_BROADCAST_ACTIVATION = 94;
    int RIL_REQUEST_CDMA_SUBSCRIPTION = 95;
    int RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM = 96;
    int RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM = 97;
    int RIL_REQUEST_DEVICE_IDENTITY = 98;
    int RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE = 99;
    int RIL_REQUEST_GET_SMSC_ADDRESS = 100;
    int RIL_REQUEST_SET_SMSC_ADDRESS = 101;
    int RIL_REQUEST_REPORT_SMS_MEMORY_STATUS = 102;
    int RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING = 103;
    int RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE = 104;
    int RIL_REQUEST_VOICE_RADIO_TECH = 105;
    int RIL_REQUEST_IMS_REGISTRATION_STATE = 106;
    int RIL_REQUEST_IMS_SEND_SMS = 107;
    int RIL_REQUEST_GET_DATA_CALL_PROFILE = 108;
    int RIL_REQUEST_SET_UICC_SUBSCRIPTION = 109;
    int RIL_REQUEST_SET_DATA_SUBSCRIPTION = 110;
    int RIL_REQUEST_GET_UICC_SUBSCRIPTION = 111;
    int RIL_REQUEST_GET_DATA_SUBSCRIPTION = 112;
    int RIL_REQUEST_SET_SUBSCRIPTION_MODE = 113;
    int RIL_REQUEST_SET_TRANSMIT_POWER = 114;
    int RIL_REQUEST_SETUP_QOS = 115;
    int RIL_REQUEST_RELEASE_QOS = 116;
    int RIL_REQUEST_GET_QOS_STATUS = 117;
    int RIL_REQUEST_MODIFY_QOS = 118;
    int RIL_REQUEST_SUSPEND_QOS = 119;
    int RIL_REQUEST_RESUME_QOS = 120;
    int RIL_UNSOL_RESPONSE_BASE = 1000;
    int RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED = 1000;
    int RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED = 1001;
    int RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED = 1002;
    int RIL_UNSOL_RESPONSE_NEW_SMS = 1003;
    int RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT = 1004;
    int RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM = 1005;
    int RIL_UNSOL_ON_USSD = 1006;
    int RIL_UNSOL_ON_USSD_REQUEST = 1007;
    int RIL_UNSOL_NITZ_TIME_RECEIVED = 1008;
    int RIL_UNSOL_SIGNAL_STRENGTH = 1009;
    int RIL_UNSOL_DATA_CALL_LIST_CHANGED = 1010;
    int RIL_UNSOL_SUPP_SVC_NOTIFICATION = 1011;
    int RIL_UNSOL_STK_SESSION_END = 1012;
    int RIL_UNSOL_STK_PROACTIVE_COMMAND = 1013;
    int RIL_UNSOL_STK_EVENT_NOTIFY = 1014;
    int RIL_UNSOL_STK_CALL_SETUP = 1015;
    int RIL_UNSOL_SIM_SMS_STORAGE_FULL = 1016;
    int RIL_UNSOL_SIM_REFRESH = 1017;
    int RIL_UNSOL_CALL_RING = 1018;
    int RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED = 1019;
    int RIL_UNSOL_RESPONSE_CDMA_NEW_SMS = 1020;
    int RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS = 1021;
    int RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL = 1022;
    int RIL_UNSOL_RESTRICTED_STATE_CHANGED = 1023;
    int RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE = 1024;
    int RIL_UNSOL_CDMA_CALL_WAITING = 1025;
    int RIL_UNSOL_CDMA_OTA_PROVISION_STATUS = 1026;
    int RIL_UNSOL_CDMA_INFO_REC = 1027;
    int RIL_UNSOL_OEM_HOOK_RAW = 1028;
    int RIL_UNSOL_RINGBACK_TONE = 1029;
    int RIL_UNSOL_RESEND_INCALL_MUTE = 1030;
    int RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 1031;
    int RIL_UNSOL_CDMA_PRL_CHANGED = 1032;
    int RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE = 1033;
    int RIL_UNSOL_VOICE_RADIO_TECH_CHANGED = 1034;
    int RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED = 1035;
    int RIL_UNSOL_TETHERED_MODE_STATE_CHANGED = 1036;
    int RIL_UNSOL_RESPONSE_DATA_NETWORK_STATE_CHANGED = 1037;
    int RIL_UNSOL_ON_SS = 1038;
    int RIL_UNSOL_STK_CC_ALPHA_NOTIFY = 1039;
    int RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED = 1040;
    int RIL_UNSOL_QOS_STATE_CHANGED_IND = 1041;
}
