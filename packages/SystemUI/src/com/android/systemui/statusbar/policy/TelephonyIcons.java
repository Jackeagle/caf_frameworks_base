/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
 *
 * Not a Contribution.
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

package com.android.systemui.statusbar.policy;

import com.android.systemui.R;

public class TelephonyIcons {
    //***** Signal strength icons

    //GSM/UMTS
    static final int[][] TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.stat_sys_signal_0,
          R.drawable.stat_sys_signal_1,
          R.drawable.stat_sys_signal_2,
          R.drawable.stat_sys_signal_3,
          R.drawable.stat_sys_signal_4,
          R.drawable.stat_sys_signal_5 },
        { R.drawable.stat_sys_signal_0_fully,
          R.drawable.stat_sys_signal_1_fully,
          R.drawable.stat_sys_signal_2_fully,
          R.drawable.stat_sys_signal_3_fully,
          R.drawable.stat_sys_signal_4_fully,
          R.drawable.stat_sys_signal_5_fully }
    };

    static final int[][] QS_TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.ic_qs_signal_0,
          R.drawable.ic_qs_signal_1,
          R.drawable.ic_qs_signal_2,
          R.drawable.ic_qs_signal_3,
          R.drawable.ic_qs_signal_4,
          R.drawable.ic_qs_signal_4 },
        { R.drawable.ic_qs_signal_full_0,
          R.drawable.ic_qs_signal_full_1,
          R.drawable.ic_qs_signal_full_2,
          R.drawable.ic_qs_signal_full_3,
          R.drawable.ic_qs_signal_full_4,
          R.drawable.ic_qs_signal_full_4 }
    };

    static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING = {
        { R.drawable.stat_sys_signal_0,
          R.drawable.stat_sys_signal_1,
          R.drawable.stat_sys_signal_2,
          R.drawable.stat_sys_signal_3,
          R.drawable.stat_sys_signal_4,
          R.drawable.stat_sys_signal_5 },
        { R.drawable.stat_sys_signal_0_fully,
          R.drawable.stat_sys_signal_1_fully,
          R.drawable.stat_sys_signal_2_fully,
          R.drawable.stat_sys_signal_3_fully,
          R.drawable.stat_sys_signal_4_fully,
          R.drawable.stat_sys_signal_5_fully }
    };

    // The status bar icons for CMCC style.
    public static final int[] MULTI_SIGNAL_NULL_CMCC = {
        R.drawable.c_stat_sys_signal_null_cmcc,
        R.drawable.c_stat_sys_signal_null_cmcc
       };
     public static final int[] MULTI_NO_SIM_CMCC = {
         R.drawable.stat_sys_no_sim,
         R.drawable.stat_sys_no_sim
     };

     //GSM/UMTS
     static final int[][] TELEPHONY_CMCC_SIGNAL_STRENGTH_G = {
      { R.drawable.stat_sys_signal_0_g_cmcc,
        R.drawable.stat_sys_signal_1_g_cmcc,
        R.drawable.stat_sys_signal_2_g_cmcc,
        R.drawable.stat_sys_signal_3_g_cmcc,
        R.drawable.stat_sys_signal_4_g_cmcc,
        R.drawable.stat_sys_signal_5_g_cmcc },
      { R.drawable.stat_sys_signal_0_g_cmcc,
        R.drawable.stat_sys_signal_1_g_cmcc,
        R.drawable.stat_sys_signal_2_g_cmcc,
        R.drawable.stat_sys_signal_3_g_cmcc,
        R.drawable.stat_sys_signal_4_g_cmcc,
        R.drawable.stat_sys_signal_5_g_cmcc }
     };

     static final int[][] TELEPHONY_CMCC_SIGNAL_STRENGTH_ROAMING_G = {
     { R.drawable.stat_sys_signal_0_g_roam_cmcc,
       R.drawable.stat_sys_signal_1_g_roam_cmcc,
       R.drawable.stat_sys_signal_2_g_roam_cmcc,
       R.drawable.stat_sys_signal_3_g_roam_cmcc,
       R.drawable.stat_sys_signal_4_g_roam_cmcc,
       R.drawable.stat_sys_signal_5_g_roam_cmcc },
     { R.drawable.stat_sys_signal_0_g_roam_cmcc,
       R.drawable.stat_sys_signal_1_g_roam_cmcc,
       R.drawable.stat_sys_signal_2_g_roam_cmcc,
       R.drawable.stat_sys_signal_3_g_roam_cmcc,
       R.drawable.stat_sys_signal_4_g_roam_cmcc,
       R.drawable.stat_sys_signal_5_g_roam_cmcc }
     };

     static final int[][] TELEPHONY_CMCC_SIGNAL_STRENGTH_3G = {
     { R.drawable.stat_sys_signal_0_3g_cmcc,
       R.drawable.stat_sys_signal_1_3g_cmcc,
       R.drawable.stat_sys_signal_2_3g_cmcc,
       R.drawable.stat_sys_signal_3_3g_cmcc,
       R.drawable.stat_sys_signal_4_3g_cmcc,
       R.drawable.stat_sys_signal_5_3g_cmcc },
     { R.drawable.stat_sys_signal_0_3g_cmcc,
       R.drawable.stat_sys_signal_1_3g_cmcc,
       R.drawable.stat_sys_signal_2_3g_cmcc,
       R.drawable.stat_sys_signal_3_3g_cmcc,
       R.drawable.stat_sys_signal_4_3g_cmcc,
       R.drawable.stat_sys_signal_5_3g_cmcc }
     };

     static final int[][] TELEPHONY_CMCC_SIGNAL_STRENGTH_ROAMING_3G = {
     { R.drawable.stat_sys_signal_0_3g_roam_cmcc,
       R.drawable.stat_sys_signal_1_3g_roam_cmcc,
       R.drawable.stat_sys_signal_2_3g_roam_cmcc,
       R.drawable.stat_sys_signal_3_3g_roam_cmcc,
       R.drawable.stat_sys_signal_4_3g_roam_cmcc,
       R.drawable.stat_sys_signal_5_3g_roam_cmcc },
     { R.drawable.stat_sys_signal_0_3g_roam_cmcc,
       R.drawable.stat_sys_signal_1_3g_roam_cmcc,
       R.drawable.stat_sys_signal_2_3g_roam_cmcc,
       R.drawable.stat_sys_signal_3_3g_roam_cmcc,
       R.drawable.stat_sys_signal_4_3g_roam_cmcc,
       R.drawable.stat_sys_signal_5_3g_roam_cmcc }
     };

     static final int[][] TELEPHONY_CMCC_SIGNAL_STRENGTH_4G = {
     { R.drawable.stat_sys_signal_0_4g_cmcc,
       R.drawable.stat_sys_signal_1_4g_cmcc,
       R.drawable.stat_sys_signal_2_4g_cmcc,
       R.drawable.stat_sys_signal_3_4g_cmcc,
       R.drawable.stat_sys_signal_4_4g_cmcc,
       R.drawable.stat_sys_signal_5_4g_cmcc },
     { R.drawable.stat_sys_signal_0_4g_cmcc,
       R.drawable.stat_sys_signal_1_4g_cmcc,
       R.drawable.stat_sys_signal_2_4g_cmcc,
       R.drawable.stat_sys_signal_3_4g_cmcc,
       R.drawable.stat_sys_signal_4_4g_cmcc,
       R.drawable.stat_sys_signal_5_4g_cmcc }
     };

     static final int[][] TELEPHONY_CMCC_SIGNAL_STRENGTH_ROAMING_4G = {
     { R.drawable.stat_sys_signal_0_4g_roam_cmcc,
       R.drawable.stat_sys_signal_1_4g_roam_cmcc,
       R.drawable.stat_sys_signal_2_4g_roam_cmcc,
       R.drawable.stat_sys_signal_3_4g_roam_cmcc,
       R.drawable.stat_sys_signal_4_4g_roam_cmcc,
       R.drawable.stat_sys_signal_5_4g_roam_cmcc },
     { R.drawable.stat_sys_signal_0_4g_roam_cmcc,
       R.drawable.stat_sys_signal_1_4g_roam_cmcc,
       R.drawable.stat_sys_signal_2_4g_roam_cmcc,
       R.drawable.stat_sys_signal_3_4g_roam_cmcc,
       R.drawable.stat_sys_signal_4_4g_roam_cmcc,
       R.drawable.stat_sys_signal_5_4g_roam_cmcc }
     };

    public static final int[][] CMCC_DATA_SIGNAL_STRENGTH_G = TELEPHONY_CMCC_SIGNAL_STRENGTH_G;
    public static final int[][] CMCC_DATA_SIGNAL_STRENGTH_R_G = TELEPHONY_CMCC_SIGNAL_STRENGTH_ROAMING_G;
    public static final int[][] CMCC_DATA_SIGNAL_STRENGTH_3G = TELEPHONY_CMCC_SIGNAL_STRENGTH_3G;
    public static final int[][] CMCC_DATA_SIGNAL_STRENGTH_R_3G = TELEPHONY_CMCC_SIGNAL_STRENGTH_ROAMING_3G;
    public static final int[][] CMCC_DATA_SIGNAL_STRENGTH_4G = TELEPHONY_CMCC_SIGNAL_STRENGTH_4G;
    public static final int[][] CMCC_DATA_SIGNAL_STRENGTH_R_4G = TELEPHONY_CMCC_SIGNAL_STRENGTH_ROAMING_4G;

    // The status bar icons for CU style.
    public static final int[] MULTI_NO_SIM_CU = {
        R.drawable.stat_sys_no_sim1_new,
        R.drawable.stat_sys_no_sim2_new
    };

    public static final int[] MULTI_SIGNAL_NULL_CU = {
        R.drawable.stat_sys_signal_null_sim1,
        R.drawable.stat_sys_signal_null_sim2
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM1_G_CU = {
        { R.drawable.stat_sys_signal_0_sim1_g,
          R.drawable.stat_sys_signal_1_sim1_g,
          R.drawable.stat_sys_signal_2_sim1_g,
          R.drawable.stat_sys_signal_3_sim1_g,
          R.drawable.stat_sys_signal_4_sim1_g },
        { R.drawable.stat_sys_signal_0_fully_sim1_g,
          R.drawable.stat_sys_signal_1_fully_sim1_g,
          R.drawable.stat_sys_signal_2_fully_sim1_g,
          R.drawable.stat_sys_signal_3_fully_sim1_g,
          R.drawable.stat_sys_signal_4_fully_sim1_g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM2_G_CU = {
        { R.drawable.stat_sys_signal_0_sim2_g,
          R.drawable.stat_sys_signal_1_sim2_g,
          R.drawable.stat_sys_signal_2_sim2_g,
          R.drawable.stat_sys_signal_3_sim2_g,
          R.drawable.stat_sys_signal_4_sim2_g },
        { R.drawable.stat_sys_signal_0_fully_sim2_g,
          R.drawable.stat_sys_signal_1_fully_sim2_g,
          R.drawable.stat_sys_signal_2_fully_sim2_g,
          R.drawable.stat_sys_signal_3_fully_sim2_g,
          R.drawable.stat_sys_signal_4_fully_sim2_g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_G_CU = {
        { R.drawable.stat_sys_r_signal_0_sim1_g,
          R.drawable.stat_sys_r_signal_1_sim1_g,
          R.drawable.stat_sys_r_signal_2_sim1_g,
          R.drawable.stat_sys_r_signal_3_sim1_g,
          R.drawable.stat_sys_r_signal_4_sim1_g },
        { R.drawable.stat_sys_r_signal_0_fully_sim1_g,
          R.drawable.stat_sys_r_signal_1_fully_sim1_g,
          R.drawable.stat_sys_r_signal_2_fully_sim1_g,
          R.drawable.stat_sys_r_signal_3_fully_sim1_g,
          R.drawable.stat_sys_r_signal_4_fully_sim1_g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_G_CU = {
        { R.drawable.stat_sys_r_signal_0_sim2_g,
          R.drawable.stat_sys_r_signal_1_sim2_g,
          R.drawable.stat_sys_r_signal_2_sim2_g,
          R.drawable.stat_sys_r_signal_3_sim2_g,
          R.drawable.stat_sys_r_signal_4_sim2_g },
        { R.drawable.stat_sys_r_signal_0_fully_sim2_g,
          R.drawable.stat_sys_r_signal_1_fully_sim2_g,
          R.drawable.stat_sys_r_signal_2_fully_sim2_g,
          R.drawable.stat_sys_r_signal_3_fully_sim2_g,
          R.drawable.stat_sys_r_signal_4_fully_sim2_g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM1_3G_CU = {
        { R.drawable.stat_sys_signal_0_sim1_3g,
          R.drawable.stat_sys_signal_1_sim1_3g,
          R.drawable.stat_sys_signal_2_sim1_3g,
          R.drawable.stat_sys_signal_3_sim1_3g,
          R.drawable.stat_sys_signal_4_sim1_3g },
        { R.drawable.stat_sys_signal_0_fully_sim1_3g,
          R.drawable.stat_sys_signal_1_fully_sim1_3g,
          R.drawable.stat_sys_signal_2_fully_sim1_3g,
          R.drawable.stat_sys_signal_3_fully_sim1_3g,
          R.drawable.stat_sys_signal_4_fully_sim1_3g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM2_3G_CU = {
        { R.drawable.stat_sys_signal_0_sim2_3g,
          R.drawable.stat_sys_signal_1_sim2_3g,
          R.drawable.stat_sys_signal_2_sim2_3g,
          R.drawable.stat_sys_signal_3_sim2_3g,
          R.drawable.stat_sys_signal_4_sim2_3g },
        { R.drawable.stat_sys_signal_0_fully_sim2_3g,
          R.drawable.stat_sys_signal_1_fully_sim2_3g,
          R.drawable.stat_sys_signal_2_fully_sim2_3g,
          R.drawable.stat_sys_signal_3_fully_sim2_3g,
          R.drawable.stat_sys_signal_4_fully_sim2_3g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_3G_CU = {
        { R.drawable.stat_sys_r_signal_0_sim1_3g,
          R.drawable.stat_sys_r_signal_1_sim1_3g,
          R.drawable.stat_sys_r_signal_2_sim1_3g,
          R.drawable.stat_sys_r_signal_3_sim1_3g,
          R.drawable.stat_sys_r_signal_4_sim1_3g },
        { R.drawable.stat_sys_r_signal_0_fully_sim1_3g,
          R.drawable.stat_sys_r_signal_1_fully_sim1_3g,
          R.drawable.stat_sys_r_signal_2_fully_sim1_3g,
          R.drawable.stat_sys_r_signal_3_fully_sim1_3g,
          R.drawable.stat_sys_r_signal_4_fully_sim1_3g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_3G_CU = {
        { R.drawable.stat_sys_r_signal_0_sim2_3g,
          R.drawable.stat_sys_r_signal_1_sim2_3g,
          R.drawable.stat_sys_r_signal_2_sim2_3g,
          R.drawable.stat_sys_r_signal_3_sim2_3g,
          R.drawable.stat_sys_r_signal_4_sim2_3g },
        { R.drawable.stat_sys_r_signal_0_fully_sim2_3g,
          R.drawable.stat_sys_r_signal_1_fully_sim2_3g,
          R.drawable.stat_sys_r_signal_2_fully_sim2_3g,
          R.drawable.stat_sys_r_signal_3_fully_sim2_3g,
          R.drawable.stat_sys_r_signal_4_fully_sim2_3g }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM1_H_CU = {
        { R.drawable.stat_sys_signal_0_sim1_h,
          R.drawable.stat_sys_signal_1_sim1_h,
          R.drawable.stat_sys_signal_2_sim1_h,
          R.drawable.stat_sys_signal_3_sim1_h,
          R.drawable.stat_sys_signal_4_sim1_h },
        { R.drawable.stat_sys_signal_0_fully_sim1_h,
          R.drawable.stat_sys_signal_1_fully_sim1_h,
          R.drawable.stat_sys_signal_2_fully_sim1_h,
          R.drawable.stat_sys_signal_3_fully_sim1_h,
          R.drawable.stat_sys_signal_4_fully_sim1_h }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM2_H_CU = {
        { R.drawable.stat_sys_signal_0_sim2_h,
          R.drawable.stat_sys_signal_1_sim2_h,
          R.drawable.stat_sys_signal_2_sim2_h,
          R.drawable.stat_sys_signal_3_sim2_h,
          R.drawable.stat_sys_signal_4_sim2_h },
        { R.drawable.stat_sys_signal_0_fully_sim2_h,
          R.drawable.stat_sys_signal_1_fully_sim2_h,
          R.drawable.stat_sys_signal_2_fully_sim2_h,
          R.drawable.stat_sys_signal_3_fully_sim2_h,
          R.drawable.stat_sys_signal_4_fully_sim2_h }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_H_CU = {
        { R.drawable.stat_sys_r_signal_0_sim1_h,
          R.drawable.stat_sys_r_signal_1_sim1_h,
          R.drawable.stat_sys_r_signal_2_sim1_h,
          R.drawable.stat_sys_r_signal_3_sim1_h,
          R.drawable.stat_sys_r_signal_4_sim1_h },
        { R.drawable.stat_sys_r_signal_0_fully_sim1_h,
          R.drawable.stat_sys_r_signal_1_fully_sim1_h,
          R.drawable.stat_sys_r_signal_2_fully_sim1_h,
          R.drawable.stat_sys_r_signal_3_fully_sim1_h,
          R.drawable.stat_sys_r_signal_4_fully_sim1_h }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_H_CU = {
        { R.drawable.stat_sys_r_signal_0_sim2_h,
          R.drawable.stat_sys_r_signal_1_sim2_h,
          R.drawable.stat_sys_r_signal_2_sim2_h,
          R.drawable.stat_sys_r_signal_3_sim2_h,
          R.drawable.stat_sys_r_signal_4_sim2_h },
        { R.drawable.stat_sys_r_signal_0_fully_sim2_h,
          R.drawable.stat_sys_r_signal_1_fully_sim2_h,
          R.drawable.stat_sys_r_signal_2_fully_sim2_h,
          R.drawable.stat_sys_r_signal_3_fully_sim2_h,
          R.drawable.stat_sys_r_signal_4_fully_sim2_h }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM1_HP_CU = {
        { R.drawable.stat_sys_signal_0_sim1_hp,
          R.drawable.stat_sys_signal_1_sim1_hp,
          R.drawable.stat_sys_signal_2_sim1_hp,
          R.drawable.stat_sys_signal_3_sim1_hp,
          R.drawable.stat_sys_signal_4_sim1_hp },
        { R.drawable.stat_sys_signal_0_fully_sim1_hp,
          R.drawable.stat_sys_signal_1_fully_sim1_hp,
          R.drawable.stat_sys_signal_2_fully_sim1_hp,
          R.drawable.stat_sys_signal_3_fully_sim1_hp,
          R.drawable.stat_sys_signal_4_fully_sim1_hp }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_SIM2_HP_CU = {
        { R.drawable.stat_sys_signal_0_sim2_hp,
          R.drawable.stat_sys_signal_1_sim2_hp,
          R.drawable.stat_sys_signal_2_sim2_hp,
          R.drawable.stat_sys_signal_3_sim2_hp,
          R.drawable.stat_sys_signal_4_sim2_hp },
        { R.drawable.stat_sys_signal_0_fully_sim2_hp,
          R.drawable.stat_sys_signal_1_fully_sim2_hp,
          R.drawable.stat_sys_signal_2_fully_sim2_hp,
          R.drawable.stat_sys_signal_3_fully_sim2_hp,
          R.drawable.stat_sys_signal_4_fully_sim2_hp }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_HP_CU = {
        { R.drawable.stat_sys_r_signal_0_sim1_hp,
          R.drawable.stat_sys_r_signal_1_sim1_hp,
          R.drawable.stat_sys_r_signal_2_sim1_hp,
          R.drawable.stat_sys_r_signal_3_sim1_hp,
          R.drawable.stat_sys_r_signal_4_sim1_hp },
        { R.drawable.stat_sys_r_signal_0_fully_sim1_hp,
          R.drawable.stat_sys_r_signal_1_fully_sim1_hp,
          R.drawable.stat_sys_r_signal_2_fully_sim1_hp,
          R.drawable.stat_sys_r_signal_3_fully_sim1_hp,
          R.drawable.stat_sys_r_signal_4_fully_sim1_hp }
    };

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_HP_CU = {
        { R.drawable.stat_sys_r_signal_0_sim2_hp,
          R.drawable.stat_sys_r_signal_1_sim2_hp,
          R.drawable.stat_sys_r_signal_2_sim2_hp,
          R.drawable.stat_sys_r_signal_3_sim2_hp,
          R.drawable.stat_sys_r_signal_4_sim2_hp },
        { R.drawable.stat_sys_r_signal_0_fully_sim2_hp,
          R.drawable.stat_sys_r_signal_1_fully_sim2_hp,
          R.drawable.stat_sys_r_signal_2_fully_sim2_hp,
          R.drawable.stat_sys_r_signal_3_fully_sim2_hp,
          R.drawable.stat_sys_r_signal_4_fully_sim2_hp }
    };

    // The status bar icons for CT style.
    public static final int[] MULTI_NO_SIM_CT = {
        R.drawable.stat_sys_no_sim_ct,
        R.drawable.stat_sys_no_sim_ct
    };

    public static final int[] MULTI_SIGNAL_NULL_CT = {
        R.drawable.stat_sys_signal_null_ct,
        R.drawable.stat_sys_signal_null_ct
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_3G_CT = {
        { R.drawable.stat_sys_signal_0_3g,
          R.drawable.stat_sys_signal_1_3g,
          R.drawable.stat_sys_signal_2_3g,
          R.drawable.stat_sys_signal_3_3g,
          R.drawable.stat_sys_signal_4_3g },
        { R.drawable.stat_sys_signal_0_3g_fully,
          R.drawable.stat_sys_signal_1_3g_fully,
          R.drawable.stat_sys_signal_2_3g_fully,
          R.drawable.stat_sys_signal_3_3g_fully,
          R.drawable.stat_sys_signal_4_3g_fully }
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_1X_CT = {
        { R.drawable.stat_sys_signal_0_1x,
          R.drawable.stat_sys_signal_1_1x,
          R.drawable.stat_sys_signal_2_1x,
          R.drawable.stat_sys_signal_3_1x,
          R.drawable.stat_sys_signal_4_1x },
        { R.drawable.stat_sys_signal_0_1x_fully,
          R.drawable.stat_sys_signal_1_1x_fully,
          R.drawable.stat_sys_signal_2_1x_fully,
          R.drawable.stat_sys_signal_3_1x_fully,
          R.drawable.stat_sys_signal_4_1x_fully }
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT = {
        { R.drawable.stat_sys_signal_0_1x_only,
          R.drawable.stat_sys_signal_1_1x_only,
          R.drawable.stat_sys_signal_2_1x_only,
          R.drawable.stat_sys_signal_3_1x_only,
          R.drawable.stat_sys_signal_4_1x_only },
        { R.drawable.stat_sys_signal_0_1x_only_fully,
          R.drawable.stat_sys_signal_1_1x_only_fully,
          R.drawable.stat_sys_signal_2_1x_only_fully,
          R.drawable.stat_sys_signal_3_1x_only_fully,
          R.drawable.stat_sys_signal_4_1x_only_fully }
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_G_CT = {
        { R.drawable.stat_sys_signal_0_2g,
          R.drawable.stat_sys_signal_1_2g,
          R.drawable.stat_sys_signal_2_2g,
          R.drawable.stat_sys_signal_3_2g,
          R.drawable.stat_sys_signal_4_2g },
        { R.drawable.stat_sys_signal_0_2g_fully,
          R.drawable.stat_sys_signal_1_2g_fully,
          R.drawable.stat_sys_signal_2_2g_fully,
          R.drawable.stat_sys_signal_3_2g_fully,
          R.drawable.stat_sys_signal_4_2g_fully }
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_3G_R_CT = {
        { R.drawable.stat_sys_signal_0_3g_default_roam,
          R.drawable.stat_sys_signal_1_3g_default_roam,
          R.drawable.stat_sys_signal_2_3g_default_roam,
          R.drawable.stat_sys_signal_3_3g_default_roam,
          R.drawable.stat_sys_signal_4_3g_default_roam },
        { R.drawable.stat_sys_signal_0_3g_default_fully_roam,
          R.drawable.stat_sys_signal_1_3g_default_fully_roam,
          R.drawable.stat_sys_signal_2_3g_default_fully_roam,
          R.drawable.stat_sys_signal_3_3g_default_fully_roam,
          R.drawable.stat_sys_signal_4_3g_default_fully_roam }
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_1X_R_CT = {
        { R.drawable.stat_sys_signal_0_1x_roam,
          R.drawable.stat_sys_signal_1_1x_roam,
          R.drawable.stat_sys_signal_2_1x_roam,
          R.drawable.stat_sys_signal_3_1x_roam,
          R.drawable.stat_sys_signal_4_1x_roam },
        { R.drawable.stat_sys_signal_0_1x_fully_roam,
          R.drawable.stat_sys_signal_1_1x_fully_roam,
          R.drawable.stat_sys_signal_2_1x_fully_roam,
          R.drawable.stat_sys_signal_3_1x_fully_roam,
          R.drawable.stat_sys_signal_4_1x_fully }
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT = {
        { R.drawable.stat_sys_signal_0_1x_only_roam,
          R.drawable.stat_sys_signal_1_1x_only_roam,
          R.drawable.stat_sys_signal_2_1x_only_roam,
          R.drawable.stat_sys_signal_3_1x_only_roam,
          R.drawable.stat_sys_signal_4_1x_only_roam },
        { R.drawable.stat_sys_signal_0_1x_only_fully_roam,
          R.drawable.stat_sys_signal_1_1x_only_fully_roam,
          R.drawable.stat_sys_signal_2_1x_only_fully_roam,
          R.drawable.stat_sys_signal_3_1x_only_fully_roam,
          R.drawable.stat_sys_signal_4_1x_only_fully_roam }
    };

    public static final int[][] TELEPHONY_SIGNAL_STRENGTH_G_R_CT = {
        { R.drawable.stat_sys_signal_0_2g_roam,
          R.drawable.stat_sys_signal_1_2g_roam,
          R.drawable.stat_sys_signal_2_2g_roam,
          R.drawable.stat_sys_signal_3_2g_roam,
          R.drawable.stat_sys_signal_4_2g_roam },
        { R.drawable.stat_sys_signal_0_2g_fully_roam,
          R.drawable.stat_sys_signal_1_2g_fully_roam,
          R.drawable.stat_sys_signal_2_2g_fully_roam,
          R.drawable.stat_sys_signal_3_2g_fully_roam,
          R.drawable.stat_sys_signal_4_2g_fully_roam }
    };

    static final int[][] DATA_SIGNAL_STRENGTH = TELEPHONY_SIGNAL_STRENGTH;

    public static final int[][][] MULTI_SIGNAL_IMAGES_G = {
            TELEPHONY_SIGNAL_STRENGTH_SIM1_G_CU,
            TELEPHONY_SIGNAL_STRENGTH_SIM2_G_CU
        };
    public static final int[][][] MULTI_SIGNAL_IMAGES_R_G = {
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_G_CU,
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_G_CU
        };
    public static final int[][][] MULTI_SIGNAL_IMAGES_3G = {
            TELEPHONY_SIGNAL_STRENGTH_SIM1_3G_CU,
            TELEPHONY_SIGNAL_STRENGTH_SIM2_3G_CU
        };
    public static final int[][][] MULTI_SIGNAL_IMAGES_R_3G = {
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_3G_CU,
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_3G_CU
        };
    public static final int[][][] MULTI_SIGNAL_IMAGES_H = {
            TELEPHONY_SIGNAL_STRENGTH_SIM1_H_CU,
            TELEPHONY_SIGNAL_STRENGTH_SIM2_H_CU
        };
    public static final int[][][] MULTI_SIGNAL_IMAGES_R_H = {
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_H_CU,
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_H_CU
        };

    public static final int[][][] MULTI_SIGNAL_IMAGES_HP = {
            TELEPHONY_SIGNAL_STRENGTH_SIM1_HP_CU,
            TELEPHONY_SIGNAL_STRENGTH_SIM2_HP_CU
        };
    public static final int[][][] MULTI_SIGNAL_IMAGES_R_HP = {
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM1_HP_CU,
            TELEPHONY_SIGNAL_STRENGTH_ROAMING_SIM2_HP_CU
        };

    //***** Data connection icons

    //GSM/UMTS
    static final int[][] DATA_G = {
        { R.drawable.stat_sys_data_connected_g,
          R.drawable.stat_sys_data_connected_g,
          R.drawable.stat_sys_data_connected_g,
          R.drawable.stat_sys_data_connected_g },
        { R.drawable.stat_sys_data_fully_connected_g,
          R.drawable.stat_sys_data_fully_connected_g,
          R.drawable.stat_sys_data_fully_connected_g,
          R.drawable.stat_sys_data_fully_connected_g }
    };

    static final int[][] DATA_3G = {
        { R.drawable.stat_sys_data_connected_3g,
          R.drawable.stat_sys_data_connected_3g,
          R.drawable.stat_sys_data_connected_3g,
          R.drawable.stat_sys_data_connected_3g },
        { R.drawable.stat_sys_data_fully_connected_3g,
          R.drawable.stat_sys_data_fully_connected_3g,
          R.drawable.stat_sys_data_fully_connected_3g,
          R.drawable.stat_sys_data_fully_connected_3g }
    };

    static final int[][] DATA_E = {
        { R.drawable.stat_sys_data_connected_e,
          R.drawable.stat_sys_data_connected_e,
          R.drawable.stat_sys_data_connected_e,
          R.drawable.stat_sys_data_connected_e },
        { R.drawable.stat_sys_data_fully_connected_e,
          R.drawable.stat_sys_data_fully_connected_e,
          R.drawable.stat_sys_data_fully_connected_e,
          R.drawable.stat_sys_data_fully_connected_e }
    };

    //3.5G
    static final int[][] DATA_H = {
        { R.drawable.stat_sys_data_connected_h,
          R.drawable.stat_sys_data_connected_h,
          R.drawable.stat_sys_data_connected_h,
          R.drawable.stat_sys_data_connected_h },
        { R.drawable.stat_sys_data_fully_connected_h,
          R.drawable.stat_sys_data_fully_connected_h,
          R.drawable.stat_sys_data_fully_connected_h,
          R.drawable.stat_sys_data_fully_connected_h }
    };

    // H+
    static final int[][] DATA_HP = {
        { R.drawable.stat_sys_data_connected_hp,
          R.drawable.stat_sys_data_connected_hp,
          R.drawable.stat_sys_data_connected_hp,
          R.drawable.stat_sys_data_connected_hp },
        { R.drawable.stat_sys_data_fully_connected_hp,
          R.drawable.stat_sys_data_fully_connected_hp,
          R.drawable.stat_sys_data_fully_connected_hp,
          R.drawable.stat_sys_data_fully_connected_hp }
    };

    // CDMA
    // Use 3G icons for EVDO data and 1x icons for 1XRTT data
    static final int[][] DATA_1X = {
        { R.drawable.stat_sys_data_connected_1x,
          R.drawable.stat_sys_data_connected_1x,
          R.drawable.stat_sys_data_connected_1x,
          R.drawable.stat_sys_data_connected_1x },
        { R.drawable.stat_sys_data_fully_connected_1x,
          R.drawable.stat_sys_data_fully_connected_1x,
          R.drawable.stat_sys_data_fully_connected_1x,
          R.drawable.stat_sys_data_fully_connected_1x }
    };

    // LTE and eHRPD
    static final int[][] DATA_4G = {
        { R.drawable.stat_sys_data_connected_4g,
          R.drawable.stat_sys_data_connected_4g,
          R.drawable.stat_sys_data_connected_4g,
          R.drawable.stat_sys_data_connected_4g },
        { R.drawable.stat_sys_data_fully_connected_4g,
          R.drawable.stat_sys_data_fully_connected_4g,
          R.drawable.stat_sys_data_fully_connected_4g,
          R.drawable.stat_sys_data_fully_connected_4g }
    };

    public static final int SIGNAL_LEVEL_0 = 0;
    public static final int SIGNAL_LEVEL_1 = 1;
    public static final int SIGNAL_LEVEL_2 = 2;
    public static final int SIGNAL_LEVEL_3 = 3;
    public static final int SIGNAL_LEVEL_4 = 4;
    public static final int SIGNAL_LEVEL_5 = 5;

    public static final int DATA_CONNECTIVITY_NOT_CONNECTED = 0;
    public static final int DATA_CONNECTIVITY_CONNECTED     = 1;

    public static final int DATA_TYPE_E = 0;
    public static final int DATA_TYPE_G = 1;
    public static final int DATA_TYPE_1X = 2;
    public static final int DATA_TYPE_3G = 3;
    public static final int DATA_TYPE_H = 4;
    public static final int DATA_TYPE_HP = 5;
    public static final int DATA_TYPE_2G = 6;

    // LTE branded "LTE"
    static final int[][] DATA_LTE = {
            { R.drawable.stat_sys_data_connected_lte,
                    R.drawable.stat_sys_data_connected_lte,
                    R.drawable.stat_sys_data_connected_lte,
                    R.drawable.stat_sys_data_connected_lte },
            { R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte,
                    R.drawable.stat_sys_data_fully_connected_lte }
    };

    public static final int DATA_NONE = 0;
    public static final int DATA_IN = 1;
    public static final int DATA_OUT = 2;
    public static final int DATA_INOUT = 3;

    public static final int[][] DATA_TYPE_ACTIVITY = {
        { R.drawable.stat_sys_signal_e_no,
          R.drawable.stat_sys_signal_e_in,
          R.drawable.stat_sys_signal_e_out,
          R.drawable.stat_sys_signal_e_inout },
        { R.drawable.stat_sys_signal_g_no,
          R.drawable.stat_sys_signal_g_in,
          R.drawable.stat_sys_signal_g_out,
          R.drawable.stat_sys_signal_g_inout },
        { R.drawable.stat_sys_signal_1x_no,
          R.drawable.stat_sys_signal_1x_in,
          R.drawable.stat_sys_signal_1x_out,
          R.drawable.stat_sys_signal_1x_inout },
        { R.drawable.stat_sys_signal_3g_no,
          R.drawable.stat_sys_signal_3g_in,
          R.drawable.stat_sys_signal_3g_out,
          R.drawable.stat_sys_signal_3g_inout },
        { R.drawable.stat_sys_signal_h_no,
          R.drawable.stat_sys_signal_h_in,
          R.drawable.stat_sys_signal_h_out,
          R.drawable.stat_sys_signal_h_inout },
        { R.drawable.stat_sys_signal_hp_no,
          R.drawable.stat_sys_signal_hp_in,
          R.drawable.stat_sys_signal_hp_out,
          R.drawable.stat_sys_signal_hp_inout },
        { R.drawable.stat_sys_signal_2g_no,
          R.drawable.stat_sys_signal_2g_in,
          R.drawable.stat_sys_signal_2g_out,
          R.drawable.stat_sys_signal_2g_inout }
    };

    public static int MOBILE_DATA_CONNECT_ACTIVITY_IN = 0;
    public static int MOBILE_DATA_CONNECT_ACTIVITY_OUT = 1;
    public static int MOBILE_DATA_CONNECT_ACTIVITY_INOUT = 2;
    public static int MOBILE_DATA_CONNECT_ACTIVITY_IDLE = 3;
    public static int MOBILE_DATA_CONNECT_ACTIVITY_MAX = 4;

    public static int MOBILE_DATA_CONNECT_TYPE_1X = 0;
    public static int MOBILE_DATA_CONNECT_TYPE_G = 1;
    public static int MOBILE_DATA_CONNECT_TYPE_3G = 2;
    public static int MOBILE_DATA_CONNECT_TYPE_4G = 3;
    public static int MOBILE_DATA_CONNECT_TYPE_E = 4;
    public static int MOBILE_DATA_CONNECT_TYPE_H = 5;
    public static int MOBILE_DATA_CONNECT_TYPE_R = 6;
    public static int MOBILE_DATA_CONNECT_TYPE_MAX = 7;

    public static final int[][] MOBILE_DATA_CONNECT_ICON_CMCC = {
            {
                    R.drawable.stat_sys_data_in_1x,
                    R.drawable.stat_sys_data_in_g,
                    R.drawable.stat_sys_data_in_3g,
                    R.drawable.stat_sys_data_in_4g,
                    R.drawable.stat_sys_data_in_e,
                    R.drawable.stat_sys_data_in_h,
                    R.drawable.stat_sys_data_in_e
            },
            {
                    R.drawable.stat_sys_data_out_1x,
                    R.drawable.stat_sys_data_out_g,
                    R.drawable.stat_sys_data_out_3g,
                    R.drawable.stat_sys_data_out_4g,
                    R.drawable.stat_sys_data_out_e,
                    R.drawable.stat_sys_data_out_h,
                    R.drawable.stat_sys_data_out_e
            },
            {
                    R.drawable.stat_sys_data_inout_1x,
                    R.drawable.stat_sys_data_inout_g,
                    R.drawable.stat_sys_data_inout_3g,
                    R.drawable.stat_sys_data_inandout_4g,
                    R.drawable.stat_sys_data_inandout_e,
                    R.drawable.stat_sys_data_inandout_h,
                    R.drawable.stat_sys_data_inandout_e
            },
            {
                    R.drawable.stat_sys_data_idle_1x,
                    R.drawable.stat_sys_data_idle_g,
                    R.drawable.stat_sys_data_idle_3g,
                    R.drawable.stat_sys_data_idle_4g,
                    R.drawable.stat_sys_data_idle_e,
                    R.drawable.stat_sys_data_idle_h,
                    R.drawable.stat_sys_data_idle_e
            },
    };
}

