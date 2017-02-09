/**
 * Copyright (c) 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

/** {@hide} */
interface IHardwareService
{
    // obsolete flashlight support
    boolean getFlashlightEnabled();
    void setFlashlightEnabled(boolean on);
    void setButtonLightEnabled(boolean on);

    //borqs_india, start: to toggle the Speaker LED On/Off
    void setSpeakerLedOn(boolean on);
    //borqs_india, end

    //borqs_india, start: to toggle/blink notification led
    void setNotificationRedLedOn(boolean on);
    void setNotificationGreenLedOn(boolean on);
    void setNotificationYellowLedOn(boolean on);
    void setNotificationRedLedBlink(int onMs, int offMs);
    void setNotificationGreenLedBlink(int onMs, int offMs);
    void setNotificationYellowLedBlink(int onMs, int offMs);
    //borqs_india, end
}
