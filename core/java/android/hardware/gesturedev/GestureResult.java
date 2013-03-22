/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.hardware.gesturedev;

/**
 * Information about gesture processing result.
 *
 * @see GestureDevice#GestureListener
 * @hide
 */
public class GestureResult {
    private static final String TAG = "GestureResult";

    public GestureResult() {
    }

    public static class GSVector {
        public float x = 0;
        public float y = 0;
        public float z = 0;
        public float error = 0; // radius of accuracy

        public GSVector() {
        }
    };

    /* result type */
    public static final int GESTURE_EVENT_RESULT_TYPE_NONE = 0;
    public static final int GESTURE_EVENT_RESULT_TYPE_DETECTION = 201;
    public static final int GESTURE_EVENT_RESULT_TYPE_ENGAGEMENT = 202;
    public static final int GESTURE_EVENT_RESULT_TYPE_TRACKING = 203;
    public static final int GESTURE_EVENT_RESULT_TYPE_SWIPE = 204;
    public static final int GESTURE_EVENT_RESULT_TYPE_MOUSE = 205;

    /* result subtype for detection and engagement */
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_LEFT_OPEN_PALM = 301;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_RIGHT_OPEN_PALM = 302;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_LEFT_FIST = 303;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_RIGHT_FIST = 304;

    /* result subtype for tracking */
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_NORMALIZED = 401;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_SCREEN = 402;

    /* result subtype for swipe */
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_LEFT = 501;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_RIGHT = 502;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_UP = 503;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_DOWN = 504;

    /* result subtype for mouse */
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_MOUSE_UP = 601;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_MOUSE_DOWN = 602;
    public static final int GESTURE_EVENT_RESULT_SUBTYPE_MOUSE_CLICK = 603;

    /**
     * Version level, currently 0. Determines the contents of {@link #extension}
     */
    public int version = 0;

    /**
     * Gesture result type
     *
     * @see #GESTURE_EVENT_RESULT_TYPE_NONE
     * @see #GESTURE_EVENT_RESULT_TYPE_DETECTION
     * @see #GESTURE_EVENT_RESULT_TYPE_ENGAGEMENT
     * @see #GESTURE_EVENT_RESULT_TYPE_TRACKING
     * @see #GESTURE_EVENT_RESULT_TYPE_SWIPE
     * @see #GESTURE_EVENT_RESULT_TYPE_MOUSE
     */
    public int type = GESTURE_EVENT_RESULT_TYPE_NONE;

    /**
     * Gesture result subtype, depends on type
     *
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_LEFT_OPEN_PALM
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_RIGHT_OPEN_PALM
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_LEFT_FIST
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_RIGHT_FIST
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_NORMALIZED
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_SCREEN
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_LEFT
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_RIGHT
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_UP
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_DOWN
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_MOUSE_UP
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_MOUSE_DOWN
     * @see #GESTURE_EVENT_RESULT_SUBTYPE_MOUSE_CLICK
     */
    public int subtype = 0;

    /**
     * Detection camera frame time in microseconds
     */
    public long timestamp = 0;

    /**
     * Identifies this outcome as the same object over time
     */
    public int id = -1;

    /**
     * The gesture detection confidence (0.0 = 0%, 1.0 = 100%)
     */
    public float confidence = 0;

    /**
     * Region for pose gesture, start position for motion gesture
     */
    public GSVector[] location = null;

    /**
     * Gesture velocity
     */
    public float velocity = 0;

    /**
     * Additional information about the gesture depending on the version level.
     *
     * @see #version
     */
    public byte[] extension = null;
}
