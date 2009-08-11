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
/**
 * This class contains all interfaces and types needed to control the FM transmitter.
 * @hide
 */
public class FmTransmitter extends FmTransceiver{

    /**
     * Creates a transmitter object
     */
    public FmTransmitter(){
        mControl = new FmRxControls();
        mRdsData = new FmRxRdsData();
        mRxEvents = new FmRxEventListner();
    }

    /**
     * Constructor for the transmitter class that takes path to radio and
     * event callback adapter
     */
    public FmTransmitter(String path, FmRxEvCallbacksAdaptor adaptor){
        FmApi_Acquire(path);
        FmApi_RegisterClient(adaptor);
        mControl = new FmRxControls();
        mRdsData = new FmRxRdsData();
        mRxEvents = new FmRxEventListner();
    }

    /**
     * This function returns the features supported by the FM driver when using
     * FmApi_TxPSStationInfo.
     * <p>
     * This function is used to get the features the FM driver supports
     * when transmitting Program Service information. Included in the returned
     * features is the number of Program Service (PS) characters which can
     * be transmitted using FmApi_TxPSStationInfo. If the driver supports
     * continuous transmission of Program Service Information, this function
     * will return a value greater than 0 for maxNumPsChars. Although the RDS/RBDS
     * standard defines each Program Service (PS) string as eight characters in
     * length, the FM driver may have the ability to accept a string that is greater than eight
     * character. This extended string will thenbe broken up into multiple strings of length eight
     * and transmitted continuously.
     * <p>
     * When transmitting more than one string, the client may want to control the
     * timing of how long each string is transmitted. Included in the features
     * returned from this function is the maximum Program Service string repeat
     * count (maxPsStrRptCnt). When using the function FmApi_TxPSStationInfo,
     * the client can specify how many times each string is repeated before the next
     * string is transmitted.
     */
    public void FmApi_GetPSFeatures(){
    }

    /**
     * This function continuously transmits RDS/RBDS Program Service information
     * over an already tuned station.
     * <p>
     * This is a function used to continuously transmit Program Service information
     * over an already tuned station. While Program Service information can be transmitted
     * using FmApi_TxRdsGroups/FmApi_TxContRdsGroups and 0A/0B groups, this function makes
     * the same output possible with limited input needed from the client.
     * <p>
     * Included in the Program Service information is an RDS/RBDS program type (PTY), and
     * one or more Program Service strings. The program type (PTY) is used to describe the
     * content being transmitted and follows the RDS/RBDS program types described in the
     * RDS/RBDS specifications.
     * <p>
     * Program Service information also includes an eight character string. This string can be
     * used to display any information, but is typically used to display information
     * about the audio being transmitted. Although the RDS/RBDS standard defines a
     * Program Service (PS) string as eight characters in length, the FM driver may
     * have the ability to accept a string that is greater than eight characters.
     * This extended string will then be broken up into multiple eight character
     * strings which will be transmitted continuously. All strings passed to this
     * function must be terminated by a null character (0x00).
     * <p>
     * When transmitting more than one string, the client may want to control the
     * timing of how long each string is transmitted. To control this timing and to
     * ensure that the FM receiver receives each string, the client can specify how
     * many times each string is repeated before the next string is transmitted.
     * This command can only be issued by the owner of an FM transmitter.
     * Once completed, this command will generate an asynchronous FM_TX_EV_TX_PS_INFO_COMPLETE event
     * to all registered clients.
     * @param TxPSStr String containing the program service to transmit
     * @param uIPSStrLen Length of program service string
     * @param TxPSRptCnt Number of times each 8 char string is repeated before next string
     */
    public void FmApi_TxPSStationInfo(String TxPSStr, long uIPSStrLen, long TxPSRptCnt){
    }

    /**
     * This function will stop an active Program Service transmission started by
     * FmTxApi_TxPSStationInfo.
     * <p>
     * This is a function used to stop an active Program Service transmission
     * started by FmApi_TxPSStationInfo.
     * <p>
     * This command can only be issued by the owner of an FM transmitter.
     * <p>
     * Once completed, this command will generate a FM_TX_EV_STOP_PS_INFO_TX_COMPLETE event to
     * the registered client.
     */
    public void FmApi_StopPSStationInfoTx(){
    }
    /**
     * This function continuously transmits RDS/RBDS RadioText information over an
     * already tuned station.
     * <p>
     * This is a function used to continuously transmit RadioText information over
     * an already tuned station. While RadioText information can be transmitted
     * using FmApi_TxRdsGroups/FmApi_TxContRdsGroups and 2A/2B groups, this function makes the
     * same output possible with limited input needed from the client.
     * <p>
     * Included in the RadioText information is an RDS/RBDS program type (PTY),
     * and a single string of up to 64 characters. The program type (PTY) is used
     * to describe the content being transmitted and follows the RDS/RBDS program
     * types described in the RDS/RBDS specifications.
     * <p>
     * RadioText information also includes a string that consists of up to 64
     * characters. This string can be used to display any information, but is
     * typically used to display information about the audio being transmitted.
     * This RadioText string is expected to be at 64 characters in length, or less
     * than 64 characters and terminated by a return carriage (0x0D). All strings
     * passed to this function must be terminated by a null character (0x00).
     * <p>
     * This command can only be issued by the owner of an FM transmitter.
     * <p>
     * Once completed, this command will generate an asynchronous FM_TX_EV_TX_RT_INFO_COMPLETE
     * event to all registered clients.
     * @param TxRTStrPtr
     */
    public void FmApi_TxRTStationInfo(String TxRTStrPtr){
    }
    /**
     * This function will stop an active RadioText transmission started by FmApi_TxRTStationInfo.
     * <p>
     * This is an asynchronous function used to stop an active RadioText transmission
     * started by FmApi_TxRTStationInfo.
     * <p>
     * This command can only be issued by the owner of an FM transmitter. To issue this
     * command, the client must first successfully call FmApi_OwnTransmitter.
     * <p>
     * Once completed, this command will generate an asynchronous FM_TX_EV_STOP_RT_INFO_TX_COMPLETE
     * event to all registered clients.
     */
    public void FmApi_StopRTStationInfoTx(){
    }
    /**
     * This function will transmit RDS/RBDS groups over an already tuned station.
     * <p>
     * This is a function used to transmit RDS/RBDS groups over an already tuned station.
     * <p>
     * This function accepts a buffer (TxRdsGrpsPtr) containing one or more RDS groups. When
     * sending this buffer, the client must also indicate how many groups should be taken from
     * this buffer (NoOfRdsGrpsToTx). It may be possible that the FM driver can not accept the
     * number of group contained in the buffer and will indicate how many group were actually
     * accepted through the NoRdsGrpsProcPtr member.
     * <p>
     * The FM driver will indicate to the client when it is ready to accept more data via both
     * the FM_TX_EV_TX_RDS_GROUPS_AVAIL and FM_TX_EV_TX_RDS_GROUPS_COMPLETE events. The
     * FM_TX_EV_TX_RDS_GROUPS_AVAILevent will indicate to the client that the FM driver can accept
     * additional groups even thoughall groups may not have been passed to the FM transmitter.
     * The FM_TX_EV_TX_RDS_GROUPS_COMPLETE event will indicate when the FM driver has a complete
     * buffer to transmit RDS data. In many cases all data passed to the FM driver will be passed
     * to the FM hardware and only a FM_TX_EV_RDS_GROUPS_COMPLETE event will be generated by the
     * FM driver.
     * <p>
     * If the client attempts to send more groups than the FM driver can handle, the client
     * must wait until it receives a FM_TX_EV_TX_RDS_GROUPS_AVAIL or a FM_TX_EV_RDS_GROUPS_COMPLETE
     * event before attempting to transmit more groups. Failure to do so may result in no group
     * being consumed by the FM driver.
     * <p>
     * It is important to note that switching between continuous and non-continuous transmission
     * of RDS groups can only happen when no RDS/RBDS group transmission is underway. If an
     * RDS/RBDS group transmission is already underway, the client must wait for a
     * FM_TX_EV_TX_RDS_GROUPS_COMPLETE or FM_TX_EV_CONT_RDS_GROUPS_COMPLETE event. If the
     * client wishes to switch from continuous to non-continuous (or vice-versa) without
     * waiting for the current transmission to complete, the client can clear all remaining
     * groups using the FmApi_TxRdsGroupCtrl command.
     * <p>
     * This command can only be issued by the owner of an FM transmitter.
     * <p>
     * Once completed, this command will generate a FM_TX_EV_TX_RDS_GROUPS_COMPLETE event
     * to all registered clients.
     * @param NoOfRdsGrpsToTx The number of groups in the buffer to transmit.
     * @param NoRdsGrpsProcPtr The number of groups the FM driver actually accepted.
     *
     */

    public void FmApi_TxRdsGroups(long NoOfRdsGrpsToTx, long NoRdsGrpsProcPtr){

    }
    /**
     * This function will continuously transmit RDS/RBDS groups over an already tuned station.
     * <p>
     * This is a function used to continuously transmit RDS/RBDS groups over an already tuned
     * station.
     * <p>
     * This function accepts a buffer (TxRdsGrpsPtr) containing one or more RDS groups. When
     * sending this buffer, the client must also indicate how many groups should be taken from
     * this buffer (NoOfRdsGrpsToTx). It may be possible that the FM driver can not accept the
     * number of group contained in the buffer and will indicate how many group were actually
     * accepted through the NoRdsGrpsProcPtr member.
     * <p>
     * The FM client can pass a complete RDS group buffer to the FM driver which will be sent
     * continuously by the FM transmitter or driver. This continuous transmission frees the client
     * from needing to pass a constant stream of RDS/RBDS groups. However, only a single RDS/RBDS
     * group buffer can be continuously transmitted at a time. Because of this fact, it is
     * important that the client only pass the complete buffer it intends to transmit. Attempting
     * to pass a buffer in two calls to this interface will be interpreted as two different
     * RDS/RBDS transmits and all unsent groups may be cleared.
     * <p>
     * Since each continuous RDS/RBDS group transmission may work on a single buffer, the client
     * must wait for a FM_TX_EV_CONT_RDS_GROUPS_COMPLETE event before attempting to transmit more
     * groups. Failure to do so will result in no groups being consumed by the FM driver.
     * <p>
     * It is important to note that switching between continuous and non-continuous transmission
     * of RDS groups can only happen when no RDS/RBDS group transmission is underway. If an
     * RDS/RBDS group transmission is already underway, the client must wait for a
     * FM_TX_EV_TX_RDS_GROUPS_COMPLETE or OFM_TX_EV_CONT_RDS_GROUPS_COMPLETE event. If the
     * client wishes to switch from continuous to non-continuous (or vice-versa) without waiting
     * for the current transmission to complete, the client can clear all remaining groups using
     * the FmTxApi_TxRdsGroupCtrl command.
     * <p>
     * This command can only be issued by the owner of an FM transmitter.
     * <p>
     * Once completed, this command will generate an asynchronous FM_TX_EV_CONT_RDS_GROUPS_COMPLETE
     * event to all registered clients.
     * @param NoOfRdsGrpsToTx The number of groups in the buffer to transmit.
     * @param NoRdsGrpsProcPtr The number of groups the FM driver actually accepted.
     */
    public void FmApi_TxContRdsGroups(long NoOfRdsGrpsToTx, long NoRdsGrpsProcPtr){
    }

    /**
     * This function is used to pause/resume RDS/RBDS group transmission, or stop and clear all RDS groups.
     * <p>
     * This is a function used to used to pause/resume RDS/RBDS group transmission, or stop and
     * clear all RDS groups. This function can be used with to control continuous and
     * non-continuous RDS/RBDS group transmissions.
     * <p>
     * This command can only be issued by the owner of an FM transmitter. To issue
     * this command, the client must first successfully call FmApi_OwnTransmitter.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_TX_EV_TX_RDS_GROUP_CTRL_COMPLETE event to all registered clients.
     * @param FmTxRdsGrpCtrl The Tx RDS group control.
     */
    public void FmApi_TxRdsGroupCtrl(short FmTxRdsGrpCtrl){
    }

    /**
     * This function will return the maximum number of RDS/RBDS groups which can be passed to the
     * FM driver.
     * <p>
     * This is a function used to determine the maximum RDS/RBDS buffer size for use when calling
     * FmTxApi_TxRdsGroups and FmTxApi_TxContRdsGroups.
     * @return int: The maximum number of RDS/RBDS groups which can be passed to the FM driver at
     * any one time.
     */
    public int FmApi_getRdsGroupBufSize(){
        return 0;
    }

}
