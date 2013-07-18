/**
 * Copyright (C) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.MSimTelephonyManager;
import android.telephony.ServiceState;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MSimNetworkController;
import com.android.systemui.statusbar.policy.TelephonyIcons;

// Intimately tied to the design of res/layout/msim_signal_cluster_view.xml
public class CTMSimSignalClusterView extends MSimSignalClusterView implements
        MSimNetworkController.MSimSignalCluster {

    private static final boolean DEBUG = false;
    private static final String TAG = "CTMSimSignalCluster";

    public static final int CT_SIGNAL_ICON_NUM = 2;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private boolean mMobileVisible = false;
    private int[][] mMobileStrengthId;
    private int[] mMobileActivityId;
    private int mNoSimIconId[];
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileTypeDescription;
    private String[] mMobileDescription;
    private boolean[] mMNoSimIconVisible;
    private boolean[] mSignalIconVisible;
    private ServiceState[] mServiceState;
    private ConnectivityManager mConnectService;
    private boolean[] dataEnabledsub;
    private boolean[] isSimRoam;
    private boolean[] dataConnected;

    ViewGroup mWifiGroup, mMobileGroup, mMobileGroupSub2;
    ImageView mWifi, mWifiActivity, mMobileActivity, mSignalCDMA3g,
        mSignalCDMA1x, mSignalCDMA1xOnly, mAirplane;
    ImageView mNoSimSlot, mNoSimSlotSub2;
    ImageView mMobileSub2, mMobileActivitySub2, mMobileTypeSub2;
    View mSpacer;
    LinearLayout mSignalCDMAboth;

    public CTMSimSignalClusterView(Context context) {
        this(context, null);
    }

    public CTMSimSignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CTMSimSignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        mMobileStrengthId = new int[numPhones][CT_SIGNAL_ICON_NUM];
        mMobileDescription = new String[numPhones];
        mMobileActivityId = new int[numPhones];
        mServiceState = new ServiceState[numPhones];
        mMNoSimIconVisible = new boolean[numPhones];
        mSignalIconVisible = new boolean[numPhones];
        dataEnabledsub = new boolean[numPhones];
        mNoSimIconId = new int[numPhones];
        isSimRoam = new boolean[numPhones];
        dataConnected = new boolean[numPhones];
        for (int i = 0; i < numPhones; i++) {
            mMNoSimIconVisible[i] = false;
            mSignalIconVisible[i] = false;
            mServiceState[i] = new ServiceState();
        }
        mConnectService = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
    }

    public void setNetworkController(MSimNetworkController nc) {
        if (DEBUG) {
            Slog.d(TAG, "MSimNetworkController=" + nc);
        }
        mMSimNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);

        mWifi = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mNoSimSlot = (ImageView) findViewById(R.id.no_sim);
        mSignalCDMAboth = (LinearLayout) findViewById(R.id.mobile_signal_cdma);
        mSignalCDMA3g = (ImageView) findViewById(R.id.mobile_signal_3g);
        mSignalCDMA1x = (ImageView) findViewById(R.id.mobile_signal_1x);
        mSignalCDMA1xOnly = (ImageView) findViewById(R.id.mobile_signal_1x_only);

        mMobileGroupSub2 = (ViewGroup) findViewById(R.id.mobile_combo_sub2);
        mMobileSub2 = (ImageView) findViewById(R.id.mobile_signal_sub2);
        mNoSimSlotSub2 = (ImageView) findViewById(R.id.no_sim_slot2);
        mMobileActivitySub2 = (ImageView) findViewById(R.id.mobile_inout2);
        mSpacer = findViewById(R.id.spacer);
        mAirplane = (ImageView) findViewById(R.id.airplane);
        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup = null;
        mWifi = null;
        mWifiActivity = null;
        mMobileGroup = null;
        mMobileActivity = null;
        mMobileActivitySub2 = null;
        mNoSimSlot = null;
        mSignalCDMAboth = null;
        mMobileGroupSub2 = null;
        mSignalCDMA3g = null;
        mSignalCDMA1x = null;
        mSignalCDMA1xOnly = null;
        mMobileSub2 = null;
        mNoSimSlotSub2 = null;

        super.onDetachedFromWindow();
    }

    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    public void setMobileDataIndicators(boolean visible, int[] strengthIcon, int activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            int noSimIcon, int subscription, ServiceState simServiceState, boolean isRoaming,
            boolean dataConnect) {
        if (DEBUG) {
            Log.i(TAG, "setMobileDataIndicators subscription=" + subscription);
        }
        mMobileVisible = visible;
        isSimRoam[subscription] = isRoaming;
        dataConnected[subscription] = dataConnect;
        if (subscription == MSimConstants.SUB2) {
            mMobileStrengthId[subscription][0] = getSub1SignalId(strengthIcon[0], isRoaming);
            mMobileStrengthId[subscription][1] = 0;
        } else {
            boolean hasEvdo = (strengthIcon[1] != R.drawable.stat_sys_signal_0)
                    && (strengthIcon[1] != R.drawable.stat_sys_signal_0_fully)
                    && (strengthIcon[1] != 0);
            mMobileStrengthId[subscription][0] = getSub0SignalId(strengthIcon[0], hasEvdo, false,
                    isRoaming);
            mMobileStrengthId[subscription][1] = hasEvdo ? getSub0SignalId(strengthIcon[1],
                    hasEvdo, true, isRoaming) : 0;
        }
        if (DEBUG) {
            Log.i(TAG, "setMobileDataIndicators mMobileStrengthId[0]="
                    + mMobileStrengthId[subscription][0]
                    + " mMobileStrengthId[1]=" + mMobileStrengthId[subscription][1]);
        }

        mMobileActivityId[subscription] =
                getAcitivyTypeIconId(typeIcon, activityIcon, subscription);
        mMobileDescription[subscription] = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId[subscription] = convertNoSimIconIdToCT(subscription);
        mServiceState[subscription] = simServiceState;

        if (noSimIcon != 0) {
            mMNoSimIconVisible[subscription] = true;
            mSignalIconVisible[subscription] = false;
        } else {
            mMNoSimIconVisible[subscription] = false;
            mSignalIconVisible[subscription] = true;
        }

        if (DEBUG)
            Log.i(TAG, "SetMobileDataIndicators MNoSimIconVisible " + subscription + "="
                    + mMNoSimIconVisible[subscription]);

        applySubscription(subscription);
    }

    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;
        applySubscription(MSimTelephonyManager.getDefault().getDefaultSubscription());
    }

    private void applySubscription(int subscription) {
        apply();

        if (mMobileVisible && !mIsAirplaneMode) {
            dataEnabledsub[subscription] = dataConnected[subscription];
            mMobileActivity.setVisibility(dataEnabledsub[0] ? View.VISIBLE : View.GONE);
            mMobileActivitySub2.setVisibility(dataEnabledsub[1] ? View.VISIBLE : View.GONE);
            if (subscription == MSimConstants.SUB1) {
                mMobileGroup.setVisibility(View.VISIBLE);
                mMobileActivity.setImageResource(mMobileActivityId[subscription]);
                mSignalCDMA3g.setImageResource(mMobileStrengthId[subscription][1]);
                mSignalCDMA1x.setImageResource(mMobileStrengthId[subscription][0]);
                mSignalCDMA1xOnly.setImageResource(mMobileStrengthId[subscription][0]);
                mSignalCDMAboth.setVisibility((mMobileStrengthId[subscription][1] != 0
                        && mMobileStrengthId[subscription][1]
                                != R.drawable.stat_sys_signal_null_sim1
                        && mSignalIconVisible[subscription]) ? View.VISIBLE : View.GONE);
                mSignalCDMA1xOnly.setVisibility(((mMobileStrengthId[subscription][1] == 0
                        || mMobileStrengthId[subscription][1]
                                == R.drawable.stat_sys_signal_null_sim1)
                        && mSignalIconVisible[subscription]) ? View.VISIBLE : View.GONE);
                mNoSimSlot.setImageResource(mNoSimIconId[subscription]);
                mNoSimSlot.setVisibility(mMNoSimIconVisible[subscription] ? View.VISIBLE
                        : View.GONE);
            } else {
                mMobileActivitySub2.setImageResource(mMobileActivityId[subscription]);
                mMobileGroupSub2.setVisibility(View.VISIBLE);
                mMobileSub2.setImageResource(mMobileStrengthId[subscription][0]);
                mMobileSub2.setVisibility(mSignalIconVisible[subscription] ? View.VISIBLE
                        : View.GONE);
                mMobileGroupSub2.setContentDescription(mMobileTypeDescription + " "
                        + mMobileDescription[subscription]);
                mNoSimSlotSub2.setImageResource(mNoSimIconId[subscription]);
                mNoSimSlotSub2.setVisibility(mMNoSimIconVisible[subscription] ? View.VISIBLE
                        : View.GONE);
            }
        } else {
            if (subscription == 0) {
                mMobileGroup.setVisibility(View.GONE);
                mMobileActivity.setVisibility(View.GONE);
            } else {
                mMobileGroupSub2.setVisibility(View.GONE);
                mMobileActivitySub2.setVisibility(View.GONE);
            }
        }
        if (mIsAirplaneMode) {
            mAirplane.setVisibility(View.VISIBLE);
            mAirplane.setImageResource(mAirplaneIconId);
        } else {
            mAirplane.setVisibility(View.GONE);
        }
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) {
            return;
        }

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG)
            Slog.d(TAG, String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"), mWifiStrengthId, mWifiActivityId));
    }

    private int convertNoSimIconIdToCT(int subscription) {
        return TelephonyIcons.MULTI_NO_SIM_CU[subscription];
    }

    private int convertSignalNullIconIdToCT(int subscription) {
        return TelephonyIcons.MULTI_SIGNAL_NULL_CU[subscription];
    }

    private int getAcitivyTypeIconId(int dataType, int dataInout, int subscription) {
        int type = 0;
        int inout = 0;
        switch (dataType) {
            case R.drawable.stat_sys_data_connected_e:
                type = TelephonyIcons.DATA_TYPE_E;
                break;
            case R.drawable.stat_sys_data_connected_3g:
                type = TelephonyIcons.DATA_TYPE_3G;
                break;
            case R.drawable.stat_sys_data_connected_h:
                type = TelephonyIcons.DATA_TYPE_H;
                break;
            case R.drawable.stat_sys_data_connected_1x:
                type = TelephonyIcons.DATA_TYPE_1X;
                break;
            case R.drawable.stat_sys_data_connected_g:
                type = TelephonyIcons.DATA_TYPE_G;
                break;
            default:
                int phoneType = MSimTelephonyManager.getDefault().getCurrentPhoneType(subscription);
                if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    type = TelephonyIcons.DATA_TYPE_G;
                } else {
                    type = TelephonyIcons.DATA_TYPE_1X;
                }
                break;
        }
        switch (dataInout) {
            case R.drawable.stat_sys_signal_in:
                inout = TelephonyIcons.DATA_IN;
                break;
            case R.drawable.stat_sys_signal_out:
                inout = TelephonyIcons.DATA_OUT;
                break;
            case R.drawable.stat_sys_signal_inout:
                inout = TelephonyIcons.DATA_INOUT;
                break;
            default:
                inout = TelephonyIcons.DATA_NONE;
                break;
        }
        if (DEBUG) {
            Log.d(TAG, "getAcitivyTypeIconId type=" + type + " inout=" + inout + " subscription"
                    + subscription);
        }
        return TelephonyIcons.DATA_TYPE_ACTIVITY[type][inout];
    }

    private int getSub0SignalId(int originalId, boolean hasEvdo, boolean isEvdo,
            boolean isRoaming) {
        boolean isGSM = false;
        if (mServiceState != null) {
            int radioTech = mServiceState[0].getRilVoiceRadioTechnology();
            isGSM = ServiceState.isGsm(radioTech);
            if (DEBUG) {
                Log.i(TAG, "voice radio technology is" + radioTech + ", isGSM = " + isGSM
                        + " isRoaming = " + isRoaming);
            }
        }
        switch (originalId) {
            case R.drawable.stat_sys_signal_0:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][0]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[0][0];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][0];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[0][0];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[0][0]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[0][0];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][0];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[0][0];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_1:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][1]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[0][1];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][1];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[0][1];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[0][1]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[0][1];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][1];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[0][1];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_2:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][2]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[0][2];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][2];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[0][2];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[0][2]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[0][2];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][2];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[0][2];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_3:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][3]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[0][3];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][3];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[0][3];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[0][3]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[0][3];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][3];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[0][3];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_4:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][4]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[0][4];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][4];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[0][4];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[0][4]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[0][4];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][4];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[0][4];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_0_fully:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][0]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[1][0];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][0];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[1][0];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[1][0]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[1][0];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][0];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[1][0];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_1_fully:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][1]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[1][1];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][1];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[1][1];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[1][1]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[1][1];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][1];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[1][1];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_2_fully:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][2]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[1][2];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][2];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[1][2];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[1][2]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[1][2];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][2];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[1][2];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_3_fully:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][3]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[1][3];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][3];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[1][3];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[1][3]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[1][3];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][3];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[1][3];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_4_fully:
                if (isRoaming) {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][4]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_R_CT[1][4];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][4];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_R_CT[1][4];
                        }
                    }
                } else {
                    if (hasEvdo) {
                        return isEvdo ? TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_CT[1][4]
                                : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_CT[1][4];
                    } else {
                        if (isGSM) {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][4];
                        } else {
                            return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_1X_ONLY_CT[1][4];
                        }
                    }
                }
            case R.drawable.stat_sys_signal_null:
                return convertSignalNullIconIdToCT(0);
            default:
                return originalId;
        }
    }

    private int getSub1SignalId(int originalId, boolean isRoaming) {
        switch (originalId) {
            case R.drawable.stat_sys_signal_0:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][0];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][0];
            case R.drawable.stat_sys_signal_1:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][1];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][1];
            case R.drawable.stat_sys_signal_2:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][2];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][2];
            case R.drawable.stat_sys_signal_3:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][3];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][3];
            case R.drawable.stat_sys_signal_4:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][4];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[0][4];
            case R.drawable.stat_sys_signal_0_fully:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][0];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][0];
            case R.drawable.stat_sys_signal_1_fully:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][1];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][1];
            case R.drawable.stat_sys_signal_2_fully:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][2];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][2];
            case R.drawable.stat_sys_signal_3_fully:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][3];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][3];
            case R.drawable.stat_sys_signal_4_fully:
                if (isRoaming) {
                    return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][4];
                }
                return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_CT[1][4];
            case R.drawable.stat_sys_signal_null:
                return convertSignalNullIconIdToCT(1);
            default:
                return originalId;
        }
    }
}
