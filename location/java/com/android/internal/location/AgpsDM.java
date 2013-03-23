package com.android.internal.location;

import android.os.Bundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

public class AgpsDM {
    public static final boolean DEBUG_AGPS_DM = true;
    private static final String TAG = "AgpsDM";
	
    public static final String AGPS_SUPL_HOST = "host";
    public static final String AGPS_SUPL_PORT = "port";
    public static final String AGPS_PROVID = "providerid";
    public static final String AGPS_NETWORK = "network";
    public static final String AGPS_RESET_TYPE = "resettype";

    private Context mContext;
    private ContentResolver mContentResolver;
    private Object mLock = new Object();
    private Bundle mSettings;

    public AgpsDM(Context context) {
	mContext = context;
	mContentResolver = mContext.getContentResolver();
    }
    public boolean updateSettings(Bundle bundle) {
        synchronized(mLock) {
		if(null == bundle){
			return false;
		}
		String supl_host = bundle.getString(AgpsDM.AGPS_SUPL_HOST);
		String supl_port = bundle.getString(AgpsDM.AGPS_SUPL_PORT);
		String agps_provid = bundle.getString(AgpsDM.AGPS_PROVID);
		String agps_network = bundle.getString(AgpsDM.AGPS_NETWORK);
		String agps_reset_type = bundle.getString(AgpsDM.AGPS_RESET_TYPE);

		if(null != supl_host && supl_host.length() > 0){
			Settings.Global.putString(mContentResolver, Settings.Global.SUPL_HOST, supl_host);
		}

		if(null != supl_port){
			Settings.Global.putString(mContentResolver, Settings.Global.SUPL_PORT, supl_port);
		}

		if(null != agps_provid && agps_provid.length() > 0){
			Settings.Global.putString(mContentResolver, Settings.Global.AGPS_PROVID, agps_provid);
		}

		if(null != agps_network && agps_network.length() > 0){
			Settings.Global.putString(mContentResolver, Settings.Global.AGPS_NETWORK, agps_network);
		}
		if(null != agps_reset_type && agps_reset_type.length() > 0){
			Settings.Global.putString(mContentResolver, Settings.Global.AGPS_RESET_TYPE, agps_reset_type);
		}
		dump();
		return true;
        }
    }

    public Bundle getSettings() {
        synchronized(mLock) {
		if(null == mSettings){
			mSettings = new Bundle();
		}
		mSettings.putString(AgpsDM.AGPS_SUPL_HOST, getAgpsSuplHost());
		mSettings.putString(AgpsDM.AGPS_SUPL_PORT, getAgpsSuplPort());
		mSettings.putString(AgpsDM.AGPS_PROVID, getAgpsProvId());
		mSettings.putString(AgpsDM.AGPS_RESET_TYPE, getAgpsResetType());
		mSettings.putString(AgpsDM.AGPS_NETWORK, getAgpsNetwork());
		dump();
		return mSettings;
        }
    }
    public String getAgpsSuplHost() {
        return Settings.Global.getString(mContentResolver, Settings.Global.SUPL_HOST);
    }
    public String getAgpsSuplPort() {
        return Settings.Global.getString(mContentResolver, Settings.Global.SUPL_PORT);
    }
 
    public String getAgpsProvId() {
        return Settings.Global.getString(mContentResolver, Settings.Global.AGPS_PROVID);
    }

	
    public String getAgpsResetType() {
        return Settings.Global.getString(mContentResolver, Settings.Global.AGPS_RESET_TYPE);
    }
	
    public String getAgpsNetwork() {
        return Settings.Global.getString(mContentResolver, Settings.Global.AGPS_NETWORK);
    }

    public void dump() {
        if(DEBUG_AGPS_DM) {
            Log.d("AgpsDM","dump() settings from system settings.");
            Log.d("AgpsDM", "mSuplHost:" + getAgpsSuplHost()
                  + ",mSuplPort:" + getAgpsSuplPort()         
                  + ",mProvId:" + getAgpsProvId()        
                  + ", mResetType:" + getAgpsResetType()
                  + ", mNetwork:" + getAgpsNetwork());
        }
    }
}
