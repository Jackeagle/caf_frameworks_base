/*
 * Copyright (c) 2009, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of Code Aurora nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.hardware.FMRxAPI;


class FmReceiverJNI {
    /**
     * General success
     */
    static final int FM_JNI_SUCCESS = 0;

    /**
     * General failure
     */
    static final int FM_JNI_FAILURE = -1;

    /**
     * native method: Open device
     * @return The file descriptor of the device
     *
     */
    static native int acquireFdNative(String path);

    /**
     * native method: change frequency of device
     * @param frequency frequency to be set
     * @param fd file descriptor of device
     * @return May return
     *             {@link #FM_JNI_SUCCESS}
     *             {@link #FM_JNI_FAILURE}
     */
    static native int changeFreqNative(double frequency, int fd);

    /**
     * native method: turn device on/off
     * @param state state to put radio in
     * @param fd file descriptor of device
     * @return May return
     *             {@link #FM_JNI_SUCCESS}
     *             {@link #FM_JNI_FAILURE}
     */
    static native int changeRadioStateNative(int state, int fd);

    /**
     * native method: initiate seek/scan
     * @param fd file descriptor of device
     * @param searchControl seek/scan as well as direction
     * @param dwell_time time to dwell on scan
     * @return May return
     *             {@link #FM_JNI_SUCCESS}
     *             {@link #FM_JNI_FAILURE}
     */
    static native int seekScanControlNative(int fd, int searchControl, int dwell_time);

    /**
     * native method:
     * @param fd
     * @param control
     * @param field
     * @return
     */
    static native int audioControlNative(int fd, int control, int field);

    /**
     * native method: cancels search
     * @param fd file descriptor of device
     * @return May return
     *             {@link #FM_JNI_SUCCESS}
     *             {@link #FM_JNI_FAILURE}
     */
    static native int cancelSearchNative(int fd);

    /**
     * native method: listens for events from device
     * @param fd file descriptor of device
     * @param buff event buffer
     * @param index
     * @return May return
     *             {@link #FM_JNI_SUCCESS}
     *             {@link #FM_JNI_FAILURE}
     */
    static native int listenForEventsNative(int fd, byte buff[], int index);

    /**
     * native method: release control of device
     * @param fd file descriptor of device
     * @return May return
     *             {@link #FM_JNI_SUCCESS}
     *             {@link #FM_JNI_FAILURE}
     */
    static native int closeFdNative(int fd);

    /**
     * native method: get frequency of chip
     * @param fd file descriptor of device
     * @return Returns frequency in double form
     */
    static native double getFrequencyNative(int fd);
}
