/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2009, Code Aurora Forum. All rights reserved
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

package com.android.internal.telephony.gsm.stk;


/**
 * Event List for SETUP_EVENT_LIST proactive command.
 *
 * {@hide}
 */

public enum SetEventList {
   
    /** Browser termination event. */
    BROWSER_TERMINATION_EVENT(0x08),
    /**
     * Add code for other events below:-
     * 
     */
    USER_TERMINATION(0x00),
    ERROR_TERMINATION(0x01);

    private int mValue;

    SetEventList(int value) {
       mValue = value;
    }
   
    public int value() {
       return mValue;
    }
    
    public static SetEventList fromInt(int value) {
        for (SetEventList r : SetEventList.values()) {
            if (r.mValue == value) {
                return r;
            }
        }
        return null;
    }

}

