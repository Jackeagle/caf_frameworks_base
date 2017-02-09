/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.EventLog;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.android.systemui.EventLogTags;

public class PanelHolder extends FrameLayout {
    public static final boolean DEBUG_GESTURES = true;

    private int mSelectedPanelIndex = -1;
    private PanelBar mBar;

    public PanelHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setChildrenDrawingOrderEnabled(true);
    }

    public int getPanelIndex(PanelView pv) {
        final int N = getChildCount();
        for (int i=0; i<N; i++) {
            final PanelView v = (PanelView) getChildAt(i);
            if (pv == v) return i;
        }
        return -1;
    }

    public void setSelectedPanel(PanelView pv) {
        mSelectedPanelIndex = getPanelIndex(pv);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mSelectedPanelIndex == -1) {
            return i;
        } else {
            if (i == childCount - 1) {
                return mSelectedPanelIndex;
            } else if (i >= mSelectedPanelIndex) {
                return i + 1;
            } else {
                return i;
            }
        }
    }

    // Add SystemUI support keyboard start
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_CLEAR:
                case KeyEvent.KEYCODE_DEL:
                case KeyEvent.KEYCODE_MENU:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    View v = getFocusedChild();
                    return v.dispatchKeyEvent(event);
            }
        return super.dispatchKeyEvent(event);

    }
    // Add SystemUI support keyboard end

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (DEBUG_GESTURES) {
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                EventLog.writeEvent(EventLogTags.SYSUI_PANELHOLDER_TOUCH,
                        event.getActionMasked(), (int) event.getX(), (int) event.getY());
            }
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                PanelBar.LOG("PanelHolder got touch in open air, closing panels");
                mBar.collapseAllPanels(true);
                break;
        }
        return false;
    }

    public void setBar(PanelBar panelBar) {
        mBar = panelBar;
    }
}
