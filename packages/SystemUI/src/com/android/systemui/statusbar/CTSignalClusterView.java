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
import android.telephony.TelephonyManager;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.TelephonyIcons;

// Intimately tied to the design of res/layout/signal_cluster_view_ct.xml
public class CTSignalClusterView extends SignalClusterView implements
        NetworkController.CtSignalCluster {

    private static final boolean DEBUG = true;
    private static final String TAG = "CTSignalCluster";

    public static final int CT_SIGNAL_ICON_NUM = 2;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private boolean mMobileVisible = false;
    private int[] mMobileStrengthId;
    private int mMobileActivityId;
    private int mNoSimIconId;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileTypeDescription;
    private String mMobileDescription;
    private boolean mNoSimIconVisible;
    private boolean mSignalIconVisible;
    private ServiceState mServiceState;
    private ConnectivityManager mConnectService;
    private boolean mDataConnected = false;

    ViewGroup mWifiGroup;
    ImageView mWifi, mWifiActivity;

    ViewGroup mMobileGroup;
    ImageView mNoSimSlot;
    ImageView mMobileActivity;
    ImageView mSignalCDMA3g, mSignalCDMA1x, mSignalCDMA1xOnly;

    ImageView mAirplane;

    View mSpacer;
    LinearLayout mSignalCDMAboth;

    public CTSignalClusterView(Context context) {
        this(context, null);
    }

    public CTSignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CTSignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMobileStrengthId = new int[CT_SIGNAL_ICON_NUM];
        mMobileDescription = new String();
        mServiceState = new ServiceState();

        mConnectService = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG) {
            Slog.d(TAG, "NetworkController=" + nc);
        }
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity = (ImageView) findViewById(R.id.wifi_inout);

        mMobileGroup    = (ViewGroup)findViewById(R.id.mobile_combo);
        mMobileActivity = (ImageView)findViewById(R.id.mobile_inout);
        mNoSimSlot      = (ImageView)findViewById(R.id.no_sim);

        mSignalCDMAboth = (LinearLayout) findViewById(R.id.mobile_signal_cdma);
        mSignalCDMA3g = (ImageView) findViewById(R.id.mobile_signal_3g);
        mSignalCDMA1x = (ImageView) findViewById(R.id.mobile_signal_1x);
        mSignalCDMA1xOnly = (ImageView) findViewById(R.id.mobile_signal_1x_only);

        mSpacer = findViewById(R.id.spacer);
        mAirplane = (ImageView) findViewById(R.id.airplane);

        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup = null;
        mWifi = null;
        mWifiActivity = null;

        mSignalCDMAboth = null;
        mSignalCDMA3g = null;
        mSignalCDMA1x = null;
        mSignalCDMA1xOnly = null;

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int[] strengthIcon, int activityIcon,
                int typeIcon, String contentDescription, String typeContentDescription,
                int noSimIcon, boolean isRoaming, ServiceState simServiceState,
                boolean dataConnected) {
        mMobileVisible = visible;
        mDataConnected = dataConnected;
        mServiceState = simServiceState;
        boolean hasEvdo = (strengthIcon[1] != R.drawable.stat_sys_signal_0)
                && (strengthIcon[1] != R.drawable.stat_sys_signal_0_fully)
                && (strengthIcon[1] != 0);
        mMobileStrengthId[0] = getSignalId(strengthIcon[0], hasEvdo, false,
                isRoaming);
        mMobileStrengthId[1] = hasEvdo ? getSignalId(strengthIcon[1],
                hasEvdo, true, isRoaming) : 0;
        if (DEBUG) {
            Log.i(TAG, "setMobileDataIndicators mMobileStrengthId[0]="
                    + mMobileStrengthId[0]
                    + " mMobileStrengthId[1]=" + mMobileStrengthId[1]);
        }

        mMobileActivityId = getAcitivyTypeIconId(typeIcon, activityIcon, isRoaming);
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId = convertNoSimIconIdToCT();

        if (noSimIcon != 0) {
            mNoSimIconVisible = true;
            mSignalIconVisible = false;
        } else {
            mNoSimIconVisible = false;
            mSignalIconVisible = true;
        }

        if (DEBUG)
            Log.i(TAG, "SetMobileDataIndicators MNoSimIconVisible " + mNoSimIconVisible);

        apply();
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;
        apply();
    }

    private void apply() {
        if (mWifiGroup == null || mMobileGroup == null) {
            return;
        }

        applyWifi();

        if (mMobileVisible && !mIsAirplaneMode) {
            mMobileGroup.setVisibility(View.VISIBLE);
            mMobileGroup.setContentDescription(mMobileTypeDescription + " "
                        + mMobileDescription);
            mMobileActivity.setVisibility(mDataConnected ? View.VISIBLE : View.GONE);
            mMobileActivity.setImageResource(mMobileActivityId);
            mNoSimSlot.setImageResource(mNoSimIconId);
            mNoSimSlot.setVisibility(mNoSimIconVisible ? View.VISIBLE : View.GONE);

            mSignalCDMA3g.setImageResource(mMobileStrengthId[1]);
            mSignalCDMA1x.setImageResource(mMobileStrengthId[0]);
            mSignalCDMA1xOnly.setImageResource(mMobileStrengthId[0]);
            final int signalNullIconId = convertSignalNullIconIdToCT();
            mSignalCDMAboth.setVisibility((mMobileStrengthId[1] != 0
                    && mMobileStrengthId[1] != signalNullIconId
                    && mSignalIconVisible) ? View.VISIBLE : View.GONE);
            mSignalCDMA1xOnly.setVisibility(((mMobileStrengthId[1] == 0
                    || mMobileStrengthId[1] == signalNullIconId)
                    && mSignalIconVisible) ? View.VISIBLE : View.GONE);
        } else {
            mMobileGroup.setVisibility(View.GONE);
            mMobileActivity.setVisibility(View.GONE);
        }
        if (mIsAirplaneMode) {
            mAirplane.setVisibility(View.VISIBLE);
            mAirplane.setImageResource(mAirplaneIconId);
        } else {
            mAirplane.setVisibility(View.GONE);
        }
    }

    // Apply the Wifi change after each indicator change.
    private void applyWifi() {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) {
            Slog.d(TAG, String.format("wifi: %s sig=%d act=%d", (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));
        }
    }

    private int convertNoSimIconIdToCT() {
        return TelephonyIcons.MULTI_NO_SIM_CT[0];
    }

    private int convertSignalNullIconIdToCT() {
        return TelephonyIcons.MULTI_SIGNAL_NULL_CT[0];
    }

    private int getAcitivyTypeIconId(int dataType, int dataInout, boolean isRoaming) {
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
                int phoneType = TelephonyManager.getDefault().getCurrentPhoneType();
                if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    type = TelephonyIcons.DATA_TYPE_G;
                } else {
                    type = TelephonyIcons.DATA_TYPE_1X;
                }
                break;
        }

        if (isRoaming) {
            switch (type) {
                case TelephonyIcons.DATA_TYPE_G:
                case TelephonyIcons.DATA_TYPE_E:
                case TelephonyIcons.DATA_TYPE_1X:
                    type = TelephonyIcons.DATA_TYPE_2G;
                    break;
                case TelephonyIcons.DATA_TYPE_H:
                    type = TelephonyIcons.DATA_TYPE_3G;
                    break;
                default:
                    break;
            }
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
            Log.d(TAG, "getAcitivyTypeIconId type=" + type + " inout=" + inout);
        }
        return TelephonyIcons.DATA_TYPE_ACTIVITY[type][inout];
    }

    private int getSignalId(int originalId, boolean hasEvdo, boolean isEvdo,
            boolean isRoaming) {
        boolean isGSM = false;
        if (mServiceState != null) {
            int radioTech = mServiceState.getRilVoiceRadioTechnology();
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][0];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][0];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][1];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][1];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][2];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][2];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][3];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][3];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[0][4];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[0][4];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][0];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][0];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][1];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][1];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][2];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][2];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][3];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][3];
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
                        return isEvdo ? 0 : TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_3G_R_CT[1][4];
                    } else {
                        return TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_G_R_CT[1][4];
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
                return convertSignalNullIconIdToCT();
            default:
                return originalId;
        }
    }
}
