/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Not a Contribution. Apache license notifications and license are retained
 * for attribution purposes only.
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

package com.android.internal.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

public class ChooserActivity extends ResolverActivity {

    private String mTitleResource = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Parcelable targetParcelable = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (!(targetParcelable instanceof Intent)) {
            Log.w("ChooseActivity", "Target is not an intent: " + targetParcelable);
            finish();
            return;
        }
        Intent target = (Intent)targetParcelable;
        String title = (String)intent.getCharSequenceExtra(Intent.EXTRA_TITLE);
        if (title == null) {
            title = (String)getResources().getText(com.android.internal.R.string.chooseActivity);
        } else {
            // If the format of title is resource name format, getTitleFromResource()
            // save the resource name and return the real title get from resource, else
            // getTitleFromResource() return the original title.
            title = getTitleFromResource(title);
        }
        Parcelable[] pa = intent.getParcelableArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
        Intent[] initialIntents = null;
        if (pa != null) {
            initialIntents = new Intent[pa.length];
            for (int i=0; i<pa.length; i++) {
                if (!(pa[i] instanceof Intent)) {
                    Log.w("ChooseActivity", "Initial intent #" + i
                            + " not an Intent: " + pa[i]);
                    finish();
                    return;
                }
                initialIntents[i] = (Intent)pa[i];
            }
        }
        super.onCreate(savedInstanceState, target, title, initialIntents, null, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Make sure the title can be translated when locale has changed.
        if (mTitleResource != null) {
            String title = getTitleFromResource(mTitleResource);
            if (title != null) {
                mAlert.setTitle(title);
            }
        }
    }

    private String getTitleFromResource(String titleResource) {
        // If the title string is a string which descibed the resource name, the format
        // must be "package_name:resource_type/resource_name", and we only need the resource
        // of string type, so the resource string must be "package_name:string/resource_name",
        // otherwise the title string is a normal string and we don't do special treatment with it.
        if (titleResource.contains(":string/")) {
            PackageManager pm = getPackageManager();
            try {
                // The sub string before symbol ":" is pacakge name.
                int packageIndex = titleResource.indexOf(":");
                if (packageIndex > 0) {
                    String packageName = titleResource.substring(0, packageIndex);
                    Resources res = pm.getResourcesForApplication(packageName);
                    int resourcId = res.getIdentifier(titleResource, null, null);
                    String title = res.getString(resourcId);
                    if (!TextUtils.isEmpty(title)) {
                        // Get title form resourc sucessful, save resource name
                        // and return the real title.
                        mTitleResource = titleResource;
                        return title;
                    }
                }
            } catch (NameNotFoundException e) {
                mTitleResource = null;
            } catch (Resources.NotFoundException e) {
                mTitleResource = null;
            }
        }
        // Get title from resource failed, return the original title.
        return titleResource;
    }
}
