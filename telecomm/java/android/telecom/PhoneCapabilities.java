/*
 * Copyright 2014, The Android Open Source Project
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

package android.telecom;

import android.annotation.SystemApi;

/**
 * Defines capabilities for {@link Connection}s and {@link Conference}s such as hold, swap, and
 * merge.
 * @hide
 */
public final class PhoneCapabilities {
    /** Call can currently be put on hold or unheld. */
    public static final int HOLD               = 0x00000001;

    /** Call supports the hold feature. */
    public static final int SUPPORT_HOLD       = 0x00000002;

    /**
     * Calls within a conference can be merged. A {@link ConnectionService} has the option to
     * add a {@link Conference} call before the child {@link Connection}s are merged. This is how
     * CDMA-based {@link Connection}s are implemented. For these unmerged {@link Conference}s, this
     * capability allows a merge button to be shown while the conference call is in the foreground
     * of the in-call UI.
     * <p>
     * This is only intended for use by a {@link Conference}.
     */
    public static final int MERGE_CONFERENCE   = 0x00000004;

    /**
     * Calls within a conference can be swapped between foreground and background.
     * See {@link #MERGE_CONFERENCE} for additional information.
     * <p>
     * This is only intended for use by a {@link Conference}.
     */
    public static final int SWAP_CONFERENCE    = 0x00000008;

    /**
     * @hide
     */
    public static final int UNUSED             = 0x00000010;

    /** Call supports responding via text option. */
    public static final int RESPOND_VIA_TEXT   = 0x00000020;

    /** Call can be muted. */
    public static final int MUTE               = 0x00000040;

    /**
     * Call supports conference call management. This capability only applies to {@link Conference}
     * calls which can have {@link Connection}s as children.
     */
    public static final int MANAGE_CONFERENCE = 0x00000080;

    /**
     * Local device supports video telephony.
     * @hide
     */
    public static final int SUPPORTS_VT_LOCAL  = 0x00000100;

    /**
     * Remote device supports video telephony.
     * @hide
     */
    public static final int SUPPORTS_VT_REMOTE = 0x00000200;

    /**
     * Call is using voice over LTE.
     * @hide
     */
    public static final int VoLTE = 0x00000400;

    /**
     * Call is using voice over WIFI.
     * @hide
     */
    public static final int VoWIFI = 0x00000800;

    /**
     * Call is able to be separated from its parent {@code Conference}, if any.
     */
    public static final int SEPARATE_FROM_CONFERENCE = 0x00001000;

    /**
     * Call is able to be individually disconnected when in a {@code Conference}.
     */
    public static final int DISCONNECT_FROM_CONFERENCE = 0x00002000;

    /**
     * Whether the call is a generic conference, where we do not know the precise state of
     * participants in the conference (eg. on CDMA).
     *
     * TODO: Move to CallProperties.
     *
     * @hide
     */
    public static final int GENERIC_CONFERENCE = 0x00004000;

    public static final int ALL = HOLD | SUPPORT_HOLD | MERGE_CONFERENCE | SWAP_CONFERENCE
            | RESPOND_VIA_TEXT | MUTE | MANAGE_CONFERENCE | SEPARATE_FROM_CONFERENCE
            | DISCONNECT_FROM_CONFERENCE;

    /* Add participant in an active or conference call option*/
    /** {@hide} */
    public static final int ADD_PARTICIPANT = 0x00008000;

    /**
     * Call is using voice privacy.
     * @hide
     */
    public static final int VOICE_PRIVACY = 0x00010000;

    /**
     * Call type can be modified for IMS call
     * @hide
     */
    public static final int CALL_TYPE_MODIFIABLE = 0x00020000;

    /**
     * Whether this set of capabilities supports the specified capability.
     * @param capabilities The set of capabilities.
     * @param capability The capability to check capabilities for.
     * @return Whether the specified capability is supported.
     * @hide
     */
    public static boolean can(int capabilities, int capability) {
        return (capabilities & capability) != 0;
    }

    /**
     * Removes the specified capability from the set of capabilities and returns the new set.
     * @param capabilities The set of capabilities.
     * @param capability The capability to remove from the set.
     * @return The set of capabilities, with the capability removed.
     * @hide
     */
    public static int remove(int capabilities, int capability) {
        return capabilities & ~capability;
    }

    public static String toString(int capabilities) {
        StringBuilder builder = new StringBuilder();
        builder.append("[Capabilities:");
        if (can(capabilities, HOLD)) {
            builder.append(" HOLD");
        }
        if (can(capabilities, SUPPORT_HOLD)) {
            builder.append(" SUPPORT_HOLD");
        }
        if (can(capabilities, MERGE_CONFERENCE)) {
            builder.append(" MERGE_CONFERENCE");
        }
        if (can(capabilities, SWAP_CONFERENCE)) {
            builder.append(" SWAP_CONFERENCE");
        }
        if (can(capabilities, RESPOND_VIA_TEXT)) {
            builder.append(" RESPOND_VIA_TEXT");
        }
        if (can(capabilities, MUTE)) {
            builder.append(" MUTE");
        }
        if (can(capabilities, MANAGE_CONFERENCE)) {
            builder.append(" MANAGE_CONFERENCE");
        }
        if (can(capabilities, SUPPORTS_VT_LOCAL)) {
            builder.append(" SUPPORTS_VT_LOCAL");
        }
        if (can(capabilities, SUPPORTS_VT_REMOTE)) {
            builder.append(" SUPPORTS_VT_REMOTE");
        }
        if (can(capabilities, VoLTE)) {
            builder.append(" VoLTE");
        }
        if (can(capabilities, VoWIFI)) {
            builder.append(" VoWIFI");
        }
        if ((capabilities & VOICE_PRIVACY) != 0) {
            builder.append(" VOICE_PRIVACY");
        }
        if ((capabilities & CALL_TYPE_MODIFIABLE) != 0) {
            builder.append(" CALL_TYPE_MODIFIABLE");
        }
        if ((capabilities & ADD_PARTICIPANT) != 0) {
            builder.append(" ADD_PARTICIPANT");
        }
        if (can(capabilities, GENERIC_CONFERENCE)) {
            builder.append(" GENERIC_CONFERENCE");
        }

        builder.append("]");
        return builder.toString();
    }

    private PhoneCapabilities() {}
}
