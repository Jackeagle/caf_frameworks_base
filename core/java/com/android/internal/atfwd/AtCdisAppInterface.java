/* Copyright (c) 2011, Code Aurora Forum. All rights reserved.
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
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
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

package com.android.internal.atfwd;

/**
 * Interface for communication between AT+CDIS command handler
 * and AT+CDIS command display activity
 *
 * {@hide}
 */public interface AtCdisAppInterface {

     /*
      * Intent's actions which are broadcasted by the AT+CDIS command handler
      * once a new AT+ CDIS command is received and the result sent back by
      * the activity that handles the AT commands
      */
     public static final String AT_CDIS_CMD_SET =
                                     "android.intent.action.at.cdis.cmd.set";
     public static final String AT_CDIS_CMD_READ =
                                     "android.intent.action.at.cdis.cmd.read";
     public static final String AT_CDIS_CMD_TEST =
                                     "android.intent.action.at.cdis.cmd.test";
     public static final String AT_CDIS_CMD_RESULT =
                                     "android.intent.action.at.cdis.cmd.result";

    /*
     * Extra fields to pass information between the activity and the
     * handler
     */

    // Result of the at command. Activity to send RESULT_OK in case of success
    // and RESULT_ERROR in case of failure
    public static final String RESULT_CODE = "result_code";

     // Error code to be sent by the activity in case of error
     public static final String ERROR_CODE = "error_code";

     //  Result of the AT command in case of success
     public static final String RESULT = "result";

     // Field containing the list of string to be displayed
     public static final String TEXT_ELEMENTS = "text_elements";

     // ID to authenticate the response for the given request
     public static final String TRANSACTION_ID = "transaction_id";
}
