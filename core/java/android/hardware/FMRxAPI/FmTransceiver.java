/*
 * Copyright (c) 2009, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of Code Aurora nor
 *      the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.hardware.FMRxAPI;

import android.hardware.FMRxAPI.FmReceiver.FmRxPwrMode;
import android.hardware.FMRxAPI.FmReceiver.FmRxEventType;
import android.util.Log;

/** <code>FmTransceiver</code> is the superclass of classes
 * <code>FmReceiver</code> and <code>FmTransmitter</code>
 * @hide
 */
public class FmTransceiver {

    private final int READY_EVENT = 0x01;
    private final int TUNE_EVENT = 0x02;
    private final int RDS_EVENT = 0x08;
    private final int MUTE_EVENT = 0x04;
    private final int SEEK_COMPLETE_EVENT = 0x03;

    static int sFd;
    FmRxControls mControl;
    FmRxPwrMode mPowerMode;
    FmRxEventListner mRxEvents;
    FmRxRdsData mRdsData;

    /** Allows access to the FM Interface.
     * <p>
     * This command allows a client to use the FM interface. This must be the first command issued by
     * the client before any receiver interfaces can be used.
     *     @param device String that is path to radio device
     */
    public void FmApi_Acquire(String device){
        if(sFd == 0){
            sFd = FmReceiverJNI.acquireFdNative("/dev/radio0");
            Log.d("FmAcquire", "** Opened "+ sFd);
        }
        else{
            Log.d("FmAcquire", "Already opened");
        }
    }

    /** Releases access to the OpenFM interface.
     * <p>
     * This synchronous command allows a client to release control of
     * the V4L2 FM device. This function should be called when the V4L2
     * device is no longer needed. This should be the last command issued by
     * the FM client. Once called, the client must call FmRxApi_Acquire to
     * re-aquire V4L2 device control before the device can be used again.
     *     @param device String that is path to radio device
     */
    public void FmApi_Release(String device) {
        if(sFd!=0){
            FmReceiverJNI.closeFdNative(sFd);
            sFd =0;
            Log.d("FMRelease", "Turned off: " + sFd);
        }else{
            Log.d("FMRelease", "Err turning off");
        }
    }
    /** Registers a callback for FM event notifications.
     * <p>
     * This is a command used to register for event
     * notifications from the FM receiver driver.
     * <p>
     * When calling this function, the client must pass
     * a callback function which will be used to deliver
     * events. The parameter adaptor must be a non-NULL value.
     * If a NULL value is passed to this function, the
     * registration will fail.
     * <p>
     * The client can choose which events will be sent from the receiver
     * driver by simply implementing functions for events it wishes to receive.
     * @param adaptor FmRxEvCallbacksAdaptor class created to handle callback events
     * from the FM receiver.
     */
    public void FmApi_RegisterClient(FmRxEvCallbacks event_handler){
        if(event_handler!=null){
            mRxEvents.startListner(sFd, event_handler);
        }else{
            Log.d("FMRegister", "Null, do nothing");
        }
    }

    /** UnRegisters a client's event notification callback.
     * <p>
     * This is a command used to unregister a client's event callback.
     */
    public void FmApi_UnRegister () {
        mRxEvents.stopListener();
    }

    /** Powers on and initializes the FM device.
     * <p>
     * This is a command used to power on the FM device.
     * If not already enabled, this function will intialize the
     * receiver with default settings. Only after successfully
     * calling this function can many of the FM device interfaces be used.
     * <p>
     * When enabling the receiver, the client must also provide the regional
     * settings in which the receiver will operate. These settings
     * (included in configSettings) are typically used for setting up
     * the FM receiver for operating in a particular geographical region.
     * These settings can be changed after the FM driver is enabled through
     * the use of the function FmRxApi_ConfigureReceiver.
     * <p>
     * This command can only be issued by the owner of an FM receiver.
     * To issue this command, the client must first successfully call
     * FmRxApi_Aquire.
     * <p>
     * Once completed, this command will generate an asynchronous FM_RX_EV_ENABLE_RECEIVER
     * event to the registered client.
     * @param configSettings    Settings to be applied when turning
     *     on the radio
     */
    public void FmApi_Enable (RxConfig configSettings){
        mControl.fmOn(sFd);
    }

    /** Powers off and disables the FM device.
     * <p>
     * This is a command used to power-off and disable the
     * FM receiver. This function is expected to be used when
     * the client no longer requires use of the FM receiver.
     * While powered off, most functionality offered by the
     * FM receiver will also be disabled until the client re-enables
     * the receiver again via FmRxApi_EnableReceiver.
     * <p>
     * This command can only be issued by the owner of an FM receiver.
     * To issue this command, the client must first successfully call
     * FmRxApi_Acquire.
     * <p>
     * Once completed, this command will generate an
     * FM_RX_EV_DISABLE_RECEIVER event to the registered client.
     *
     */
    public void FmApi_Disable(){
        mControl.fmOff(sFd);
    }

    /** Reconfigures the device's regional settings
     * (FM Band, De-Emphasis, Channel Spacing, RDS/RBDS mode).
     * <p>
     * This is a command used to reconfigure various settings
     * on the FM receiver. Included in the passed structure are
     * settings which typically differ from one geographical region to another.
     * <p>
     * This command can only be issued by the owner of an FM device.
     * To issue this command, the client must first successfully call FmRxApi_Acquire.
     * <p>
     * Once completed, this command will generate an FM_RX_EV_CFG_RECEIVER
     * event to the registered client.
     * @param configSettings    Contains settings for the FM radio (FM band, De-emphasis, channel spacing, RDS/RBDS mode)
     */
    public void FmApi_Configure(RxConfig configSettings){

    }

    /** Tunes the FM device to the supplied FM frequency.
     * <p>
     * This is a command which tunes the FM device to a station as
     * specified by the supplied frequency. Only valid frequencies within
     * the band set by FmRxApi_Enable or FmRxApi_Configure can
     * be tuned by this function. Attempting to tune to frequencies outside
     * of the set band will result in an error.
     * <p>
     * This command can only be issued by the owner of an FM device.
     * To issue this command, the client must first successfully call FmRxApi_Acquire.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_RX_EV_RADIO_STATION_SET event to the registered client.
     * This event only signals the completion of this command and does
     * not signify the actual tune status of the FM device.
     * In addition to this event, the driver will also generate a
     * FM_RX_EV_RADIO_TUNE_STATUS event to indicate whether the station
     * was successfully tuned or not.
     *
     * @param freq Desired frequency (in kHz) which should be tuned (Example: 96500 = 96.5Mhz)
     */
    public void FmApi_SetStation (double freq) {
        mControl.setFreq(freq);
        mControl.setStation(sFd);
    }



}
