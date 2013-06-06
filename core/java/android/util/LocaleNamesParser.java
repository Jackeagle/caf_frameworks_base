/*
 * Copyright (C) 2012, The Linux Foundation. All rights reserved.
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

package android.util;

import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;

/**
 * @hide
 */
public final class LocaleNamesParser {
    private static final String LOG_TAG = "LocaleNamesParser";

    private Context mContext;
    private HashMap<String, Integer> mNames = new HashMap<String, Integer>();

    private int mOriginNamesId;
    private int mLocaleNamesId;

    private String mTag;

    /*
     * Initialize the array of carrier names. synchronize if two instances in
     * one context want to init
     */
    public LocaleNamesParser(Context context, String tag, int originNamesId, int localeNamesId) {
        mTag = tag;
        mContext = context;
        mOriginNamesId = originNamesId;
        mLocaleNamesId = localeNamesId;
        reload();
    }

    /*
     * Here reload strings when locale change. synchronize to if two instances
     * need to reload and wait reloading complete
     */
    public synchronized void reload() {
        Resources res = mContext.getResources();
        String[] origNames = res.getStringArray(mOriginNamesId);
        String[] localeNames = res.getStringArray(mLocaleNamesId);
        Integer localeId = null;
        mNames.clear();
        for (int i = 0; i < origNames.length; i++) {
            localeId = new Integer(res.getIdentifier(localeNames[i], "string",
                    mContext.getPackageName()));
            mNames.put(origNames[i], localeId);
        }
    }

    public CharSequence getLocaleName(CharSequence name) {
        Integer locale = null;
        synchronized (mNames) {
            locale = mNames.get(name);
            if (locale != null) {
                try {
                    name = mContext.getResources().getString(locale.intValue());
                } catch (NotFoundException e) {
                    Slog.e(LOG_TAG, "Resource not found");
                }
            } else {
                Slog.e(LOG_TAG, "locale is null");
            }
        }
        return name;
    }
}
