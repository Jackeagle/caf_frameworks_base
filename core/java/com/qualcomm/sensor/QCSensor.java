 /*
 * Copyright (c) 2012-2013 The Linux Foundation. All rights reserved.
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (C) 2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ###############################################################################################
 * ######     SNAPDRAGON_SDK_FOR_ANDROID REQUIRED CLASS FOR SENSOR GESTURES
 * ######
 * ######     Remove file if not supporting sensor gestures via DSPS
 * ###############################################################################################
 */

package com.qualcomm.sensor;

import java.util.ArrayList;

import android.os.Bundle;

import com.qualcomm.snapdragon.util.QCCapabilitiesInterface;

public class QCSensor implements QCCapabilitiesInterface{

    private static String KEY_SENSOR_TYPES = "key_sensor_types";
    private static String KEY_EVENT_TYPES = "key_event_types";

    private static int QC_SENSOR_TYPE_BASE        = 33171000;

    // QC Sensor type values
    public static int SENSOR_TYPE_BASIC_GESTURES = QC_SENSOR_TYPE_BASE;
    public static int SENSOR_TYPE_TAP            = QC_SENSOR_TYPE_BASE + 1;
    public static int SENSOR_TYPE_FACING         = QC_SENSOR_TYPE_BASE + 2;
    public static int SENSOR_TYPE_TILT           = QC_SENSOR_TYPE_BASE + 3;

    // QC Gesture values returned

    // Basic Gestures
    public static int BASIC_GESTURE_PUSH_V01 = 1; /*  Phone is jerked away from the user, in the direction perpendicular to the screen   */
    public static int BASIC_GESTURE_PULL_V01 = 2; /*  Phone is jerked toward  from the user, in the direction perpendicular to the screen   */
    public static int BASIC_GESTURE_SHAKE_AXIS_LEFT_V01 = 3; /*  Phone is shaken toward the left   */
    public static int BASIC_GESTURE_SHAKE_AXIS_RIGHT_V01 = 4; /*  Phone is shaken toward the right   */
    public static int BASIC_GESTURE_SHAKE_AXIS_TOP_V01 = 5; /*  Phone is shaken toward the top   */
    public static int BASIC_GESTURE_SHAKE_AXIS_BOTTOM_V01 = 6; /*  Phone is shaken toward the bottom */

    // Tap Gestures
    public static int GYRO_TAP_LEFT_V01 = 1; /*  Phone is tapped on the left. # */
    public static int GYRO_TAP_RIGHT_V01 = 2; /*  Phone is tapped on the right.  */
    public static int GYRO_TAP_TOP_V01 = 3; /*  Phone is tapped on the top.  */
    public static int GYRO_TAP_BOTTOM_V01 = 4; /*  Phone is tapped on the bottom.  */

    // Facing Gestures
    public static int FACING_UP_V01 = 1; /*  Phone has just moved to a facing-up phone posture, which is defined as screen up   */
    public static int FACING_DOWN_V01 = 2; /*  Phone has just moved to a facing-down phone posture, which is defined as screen down */


    /**
     * Returns the Sensor capabilities of the hardware
     * @param None
     * @return a Bundle which looks like -
     * <p>
     * <pre class="language-java">
     * Bundle(KEY_CONSTANT_FIELD_VALUES,[Bundle{(KEY_SENSOR_TYPES, ArrayList<String>), (KEY_EVENT_TYPES, ArrayList<String>)}])
     *
     * KEY_CONSTANT_FIELD_VALUES => |KEY_SENSOR_TYPES=> | SENSOR_TYPE_BASIC_GESTURES,
     *                              |                   | SENSOR_TYPE_TAP,
     *                              |                   | ...
     *                              |------------------------------------------------
     *                              |KEY_EVENT_TYPES=>  | BASIC_GESTURE_PUSH_V01,
     *                              |                   | BASIC_GESTURE_PULL_V01,
     *                              |                   | ...
     * </pre>
     * </p>
     */

    @Override
    public Bundle getCapabilities(){

        Bundle constantFieldBundle = new Bundle();
        ArrayList<String> sensorTypesList = new ArrayList<String>();
        sensorTypesList.add("SENSOR_TYPE_BASIC_GESTURES");
        sensorTypesList.add("SENSOR_TYPE_TAP");
        sensorTypesList.add("SENSOR_TYPE_FACING");
        sensorTypesList.add("SENSOR_TYPE_TILT");

        constantFieldBundle.putStringArrayList(KEY_SENSOR_TYPES, sensorTypesList);

        ArrayList<String> eventTypesList = new ArrayList<String>();
        eventTypesList.add("BASIC_GESTURE_PUSH_V01");
        eventTypesList.add("BASIC_GESTURE_PULL_V01");
        eventTypesList.add("BASIC_GESTURE_SHAKE_AXIS_LEFT_V01");
        eventTypesList.add("BASIC_GESTURE_SHAKE_AXIS_RIGHT_V01");
        eventTypesList.add("BASIC_GESTURE_SHAKE_AXIS_TOP_V01");
        eventTypesList.add("BASIC_GESTURE_SHAKE_AXIS_BOTTOM_V01");
        eventTypesList.add("GYRO_TAP_LEFT_V01");
        eventTypesList.add("GYRO_TAP_RIGHT_V01");
        eventTypesList.add("GYRO_TAP_TOP_V01");
        eventTypesList.add("GYRO_TAP_BOTTOM_V01");
        eventTypesList.add("FACING_UP_V01");
        eventTypesList.add("FACING_DOWN_V01");

        constantFieldBundle.putStringArrayList(KEY_EVENT_TYPES, eventTypesList);

        Bundle capabilitiesBundle = new Bundle();
        capabilitiesBundle.putBundle(QCCapabilitiesInterface.KEY_CONSTANT_FIELD_VALUES, constantFieldBundle);

        return capabilitiesBundle;
    }
}
