/* Copyright (c) 2012, The Linux Foundation. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
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

package com.android.internal.telephony;

import java.util.Map;
import java.util.Map.Entry;

/**
 * CallDetails class takes care of all the additional details like call type
 * and domain needed for IMS calls. This class is not relevant for non-IMS calls
 */
public class CallDetails {

    /*
     * Type of the call based on the media type and the direction of the media.
     */

    public static final int CALL_TYPE_VOICE = 0; /*
                                                  * Voice call-audio in both
                                                  * directions
                                                  */

    public static final int CALL_TYPE_VS_TX = 1; /*
                                                  * Videoshare call-audio in
                                                  * both directions &
                                                  * transmitting video in uplink
                                                  */

    public static final int CALL_TYPE_VS_RX = 2; /*
                                                  * Videoshare call-audio in
                                                  * both directions & receiving
                                                  * video in downlink
                                                  */

    public static final int CALL_TYPE_VT = 3; /*
                                               * Video call-audio & video in
                                               * both directions
                                               */

    public static final int CALL_TYPE_UNKNOWN = 10; /*
                                                     * Unknown Call type, may be
                                                     * used for answering call
                                                     * with same call type as
                                                     * incoming call. This is
                                                     * only for telephony, not
                                                     * meant to be passed to RIL
                                                     */

    public static final int CALL_DOMAIN_UNKNOWN = 0; /*
                                                      * Unknown domain. Sent by
                                                      * RIL when modem has not
                                                      * yet selected a domain
                                                      * for a call
                                                      */
    public static final int CALL_DOMAIN_CS = 1; /*
                                                 * Circuit switched domain
                                                 */
    public static final int CALL_DOMAIN_PS = 2; /*
                                                 * Packet switched domain
                                                 */
    public static final int CALL_DOMAIN_AUTOMATIC = 3; /*
                                                        * Automatic domain. Sent
                                                        * by Android to indicate
                                                        * that the domain for a
                                                        * new call should be
                                                        * selected by modem
                                                        */
    public static final int CALL_DOMAIN_NOT_SET = 4; /*
                                                      * Init value used
                                                      * internally by telephony
                                                      * until domain is set
                                                      */

    public int call_type;
    public int call_domain;

    public String[] extras;

    public CallDetails() {
        call_type = CALL_TYPE_VOICE;
        call_domain = CALL_DOMAIN_NOT_SET;
        extras = null;
    }

    public CallDetails(int callType, int callDomain, String[] extraparams) {
        call_type = callType;
        call_domain = callDomain;
        // TODO Implement this later when extras is implemented
        extras = null;
    }

    public CallDetails(CallDetails srcCall) {
        if (srcCall != null) {
            call_type = srcCall.call_type;
            call_domain = srcCall.call_domain;
            extras = srcCall.extras;
        }
    }

    public void setExtras(String[] extraparams) {
        extras = extraparams;
    }

    public static String[] getExtrasFromMap(Map<String, String> newExtras) {
        String []extras = null;

        if (newExtras == null) {
            return null;
        }

        // TODO: Merge new extras into extras. For now, just serialize and set them
        extras = new String[newExtras.size()];

        if (extras != null) {
            int i = 0;
            for (Entry<String, String> entry : newExtras.entrySet()) {
                extras[i] = "" + entry.getKey() + "=" + entry.getValue();
            }
        }
        return extras;
    }

    public void setExtrasFromMap(Map<String, String> newExtras) {
        this.extras = getExtrasFromMap(newExtras);
    }

    /**
     * @return string representation.
     */
    @Override
    public String toString() {
        return (" " + call_type
                + " " + call_domain
                + " " + extras); // TODO - fix printing string array
    }
}
