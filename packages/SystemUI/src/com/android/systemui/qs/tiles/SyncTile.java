/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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

package com.android.systemui.qs.tiles;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;

public class SyncTile extends QSTile<QSTile.BooleanState> {
    public SyncTile(Host host) {
        super(host);
    }

    @Override
    protected void handleClick() {
        final boolean newState = !ContentResolver.getMasterSyncAutomatically();
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver.setMasterSyncAutomatically(newState);
                final boolean currentState = ContentResolver.getMasterSyncAutomatically();
                refreshState(currentState ? UserBoolean.USER_TRUE : UserBoolean.USER_FALSE);
            }
        });
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
     public void setListening(boolean listening) {
        // Add some call back listener here if needed.
     }

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        final boolean currentState = ContentResolver.getMasterSyncAutomatically();
        state.value = currentState;
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_sync_label);

        if (currentState) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_sync_on);
            state.contentDescription
                    = mContext.getString(R.string.accessibility_quick_settings_sync_on);
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_sync_off);
            state.contentDescription
                    = mContext.getString(R.string.accessibility_quick_settings_sync_off);
        }
    }
}
