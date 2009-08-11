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

/**
 * This class contains all interfaces and types needed to mControl the FM receiver.
 *    @hide
 */
public class FmReceiver extends FmTransceiver {

    /**
     * Creates Receiver object
     */
    public FmReceiver(){
        mControl = new FmRxControls();
        mRdsData = new FmRxRdsData();
        mRxEvents = new FmRxEventListner();
    }

    /**
     * Contains enums for emphasis settings
     *
     */
    public enum Emphasis {
        FM_DE_EMP75 ,
        FM_DE_EMP50
    }

    /**
     * Contains enums to be used for seek/scan direction
     *
     */
    public enum FmRxSrchDir {
        FM_RX_SEARCHDIR_UP ,
        FM_RX_SEARCHDIR_DOWN
    }

    /**
     * Contains enums to be used for scan dwell duration
     *
     */
    public enum FmRxScanTime {
        FM_RX_DWELL_PERIOD_1S,
        FM_RX_DWELL_PERIOD_2S,
        FM_RX_DWELL_PERIOD_3S,
        FM_RX_DWELL_PERIOD_4S,
        FM_RX_DWELL_PERIOD_5S,
        FM_RX_DWELL_PERIOD_6S,
        FM_RX_DWELL_PERIOD_7S
    }

    /**
     * Contains enums to be used for searching via RDS
     *
     */
    public enum FmRxSrchRdsMode {
        FM_RX_SRCHRDS_MODE_SEEK_PTY,
        FM_RX_SRCHRDS_MODE_SCAN_PTY,
        FM_RX_SRCHRDS_MODE_SEEK_PI,
        FM_RX_SRCHRDS_MODE_SEEK_AF
    }

    /**
     * Contains enums to be used for seach list mode
     *
     */
    public enum FmRxSrchListMode {
        FM_RX_SRCHLIST_MODE_STRONG,
        FM_RX_SRCHLIST_MODE_WEEK,
        FM_RX_SRCHLIST_MODE_STRONGEST,
        FM_RX_SRCHLIST_MODE_WEAKEST,
        FM_RX_SRCHLIST_MODE_PTY
    }

    /**
     * Contains enums to be used for mono/stereo mode
     *
     */
    public enum FmRxStereoMonoMode {
        FM_RX_NOMUTE,
        FM_RX_MUTELEFT,
        FM_RX_MUTERIGHT,
        FM_RX_MUTEBOTH
    }

    /**
     * Contains enums to be used for seek/scan mode
     *
     *
     */
    public enum FmRxSrchMode {
        FM_RX_SRCH_MODE_SEEK_UP,
        FM_RX_SRCH_MODE_SEEK_DOWN,
        FM_RX_SRCH_MODE_SCAN_UP,
        FM_RX_SRCH_MODE_SCAN_DOWN,
    }

    /**
     * Contains enums to be used for selecting RDS standard type
     *
     */
    public enum RdsStd {
        FM_RDS_STD_RBDS,
        FM_RDS_STD_RDS,
        FM_RDS_STD_NONE
    }

    /**
     * Contains enums to be used for radio band setting
     *
     */
    public enum RadioBand {
        FM_US_EUROPE_BAND,
        FM_JAPAN_WIDE_BAND,
        FM_JAPAN_STANDARD_BAND,
        FM_USER_DEFINED_BAND;
    }

    /**
     *    Contains enums to be used for signal threshold
     *
     */
    public enum FmRxSignalThreshold {
        FM_RX_SIGNAL_THRESHOLD_VERY_WEAK,
        FM_RX_SIGNAL_THRESHOLD_WEAK,
        FM_RX_SIGNAL_THRESHOLD_STRONG,
        FM_RX_SIGNAL_THRESHOLD_VERY_STRONG
    }

    /**
     * Contains enums to be used for power settings
     *
     */
    public enum FmRxPwrMode {
        FM_RX_NORMAL_MODE,
        FM_RX_LOW_POWER_MODE
    }

    /**
     *    Contains enums to be used for mute settings
     *
     */
    public enum FmRxMute {
        FM_RX_NOMUTE,
        FM_RX_MUTELEFT,
        FM_RX_MUTERIGHT,
        FM_RX_MUTEBOTH
    }

    /**
     *    Contains enums for channel spacing settings
     *
     */
    public enum ChSpacing {
        FM_CHSPACE_200_KHZ ,
        FM_CHSPACE_100_KHZ,
        FM_CHSPACE_50_KHZ
    }

    public enum FmPrgmIdType {
    }

    /**
     *
     * Contains enums used for event types
     *
     */
    public enum FmRxEventType {
        FM_RX_EV_ENABLE_RECEIVER,
        FM_RX_EV_DISABLE_RECEIVER,
        FM_RX_EV_CFG_RECEIVER,
        FM_RX_EV_MUTE_MODE_SET,
        FM_RX_EV_STEREO_MODE_SET,
        FM_RX_EV_RADIO_STATION_SET,
        FM_RX_EV_PWR_MODE_SET,
        FM_RX_EV_SET_SIGNAL_THRESHOLD,
        FM_RX_EV_RADIO_TUNE_STATUS,
        FM_RX_EV_STATION_PARAMETERS,
        FM_RX_EV_RDS_LOCK_STATUS,
        FM_RX_EV_STEREO_STATUS,
        FM_RX_EV_SERVICE_AVAILABLE,
        FM_RX_EV_GET_SIGNAL_THRESHOLD,
        FM_RX_EV_SEARCH_IN_PROGRESS,
        FM_RX_EV_SEARCH_RDS_IN_PROGRESS,
        FM_RX_EV_SEARCH_LIST_IN_PROGRESS,
        FM_RX_EV_SEARCH_COMPLETE,
        FM_RX_EV_SEARCH_RDS_COMPLETE,
        FM_RX_EV_SEARCH_LIST_COMPLETE,
        FM_RX_EV_SEARCH_CANCELLED,
        FM_RX_EV_RDS_GROUP_DATA,
        FM_RX_EV_RDS_PS_INFO,
        FM_RX_EV_RDS_RT_INFO,
        FM_RX_EV_RDS_AF_INFO,
        FM_RX_EV_RDS_PI_MATCH_AVAILABLE,
        FM_RX_EV_RDS_GROUP_OPTIONS_SET,
        FM_RX_EV_RDS_PROC_REG_DONE,
        FM_RX_EV_RDS_PI_MATCH_REG_DONE
    }

    /**
     * Constructor for the receiver class that takes path to radio and
     * event callback adapter
     */
    public FmReceiver(String devicePath, FmRxEvCallbacksAdaptor callbackAdaptor){
        mControl = new FmRxControls();
        mRdsData = new FmRxRdsData();
        mRxEvents = new FmRxEventListner();
        FmApi_Acquire(devicePath);
        FmApi_RegisterClient(callbackAdaptor);
    }

    /** Allows the muting and un-muting of the audio coming from the FM receiver.
     * <p>
     * This is a command used to mute or un-mute the FM audio.
     * This command mutes the audio coming from the FM device. It
     * is important to note that this only affects the FM audio and
     * to any other audio system being used.
     * <p>
     * This command can only be issued by the owner of an FM receiver.
     * To issue this command, the client must first successfully call FmRxApi_Acquire.
     * <p>
     * Once completed, this command will generate an FM_RX_EV_MUTE_MODE_SET
     * event to the registered client.
     * @param mode FM hardware mute settings to apply
     */
    public void FmRxApi_SetMuteMode (FmRxMute mode) {
        switch(mode){
        case FM_RX_NOMUTE:
            mControl.muteControl(sFd, false);
            break;
        case FM_RX_MUTEBOTH:
            mControl.muteControl(sFd, true);
            break;
        default:
            break;
        }

    }
    /** Sets the mono/stereo mode of the FM device.
     * <p>
     * This command allows the user to set the mono/stereo mode
     * of the FM device. Using this function,
     * the user can allow mono/stereo mixing or force the reception
     * of mono audio only.
     * <p>
     * This command can only be issued by the owner of an FM receiver.
     * To issue this command, the client must first successfully call FmRxApi_Acquire.
     * <p>
     * Once completed, this command will generate an
     * FM_RX_EV_STEREO_MODE_SET event to the registered client.
     * @param strMode FM mono/stereo mode settings to apply
     */
    public void FmRxApi_SetStereoMode (FmRxStereoMonoMode strMode) {
        mControl.streoControl(true);
    }

    /** This function sets the threshold which the FM driver uses
     * to determine which stations have service available.
     * <p>
     * This information is used to determine which stations are
     * tuned during searches and Alternative Frequency jumps, as
     * well as at what threshold FM_RX_EV_SERVICE_AVAILABLE events are generated.
     * <p>
     * This is a command used to set the threshold used by the FM driver
     * and/or hardware to determine which stations are "good" stations.
     * Using this function, the client can allow very weak stations,
     * relatively weak stations, relatively strong stations, or very.
     * strong stations to be found during searches. Additionally,
     * this threshold will be used to determine at what threshold a
     * FM_RX_EV_SERVICE_AVAILABLE event is generated.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_RX_EV_SET_SIGNAL_THRESHOLD event to the registered client.
     * @param threshold    The new signal threshold.
     */
    public void FmRxApi_SetSignalThreshold (FmRxSignalThreshold threshold) {
    }

    /** Returns various statistics related to the currently tuned station.
     * <p>
      * This is a command which returns various statistics related
     * to the currently tuned station. Included in these statistics
     * are the currently tuned frequency, the RDS/RBDS sync status,
     * the RSSI level, current mute settings and the stereo/mono status.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_RX_EV_STATION_PARAMETERS event to the registered client.
     * This event will contain the station parameters.
     *
     */
    public void FmRxApi_GetStationParameters () {
    }

    /**
     * Returns the current RDS/RBDS Program Service Information.
     * <p>
     * This is a command which returns the last complete RDS/RBDS Program
     * Service information for the currently tuned station. To use this command,
     * the client must first register for Program Service info by receiving either
     * the FM_RX_RDS_GRP_PS_EBL or FM_RX_RDS_GRP_PS_SIMPLE_EBL event.
     * Under normal operating mode, this information will automatically be sent
     * to the client. However, if the client requires this information be sent
     * again, this function can be used.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_RX_EV_RDS_PS_INFO event to the registered client.
     *    This event will contains the current RDS program service information.
     *
     */
    public void FmRxApi_RxGetPSInfo(StringBuffer ps) {

    }

    /**
     * Returns the current RDS/RBDS RadioText Information.
     * <p>
     * This is a command which returns the last complete RadioText information
     * for the currently tuned station. For this command to return meaningful
     * information, the client must first register for RadioText events by registerring
     * the FM_RX_RDS_GRP_RT_EBL callback function. Under normal operating mode, this information
     * will automatically be sent to the client. However, if the client requires
     * this information be sent again, this function can be used.
     * <p>
     * Once completed, this command will generate an FM_RX_EV_RDS_RT_INFO event
     * to all registered clients. This event will contain the current RDS/RBDS
     * RadioText Information.
     * @param radiotext String buffer to be populated with radio text
     */
    public void FmRxApi_RxGetRTInfo (StringBuffer radiotext) {
        byte [] buff = new byte[120];
        FmReceiverJNI.listenForEventsNative(sFd, buff, 0);
        String rdsStr = new String(buff);
        rdsStr = rdsStr.substring(5, (int) buff[0]+ 5);
        radiotext.append(rdsStr);
    }
    /** Puts the driver into or out of low power mode.
     * <p>
     * This is an asynchronous command which can put the FM
     *    device and driver into and out of low power mode. Low power mode
     *    should be used when the receiver is tuned to a station and only
     *    the FM audio is required. The typical scenario for low power mode
     *    is when the FM application is no longer visible.
     *    <p>
     *    While in low power mode, all normal FM and RDS indications from
     *    the FM driver will be suppressed. By disabling these indications,
     *    low power mode can result in fewer interruptions and this may lead
     *    to a power savings.
     *    <p>
     *    Once completed, this command will generate an asynchronous
     *    FM_RX_EV_PWR_MODE_SET event to the registered client.
     *    @param pwrMode The new driver operating mode.
     */
    public    void FmRxApi_setPwrMode(FmRxPwrMode pwrMode){

    }

    /** Returns the RSSI thresholds for the FM driver.
     * <p>
     * This is command which returns the RSSI thresholds for the FM driver.
     * This function returns a structure containing the minimum RSSI needed
     * for reception and the minimum RSSI value where reception is perfect.
     * The minimum RSSI value for reception is the recommended threshold where
     * an average user would consider the station listenable. Similarly,
     * the minimum RSSI threshold for perfect reception is the point where
     * reception quality will improve only marginally even if the RSSI level
     * improves greatly.
     * <p>
     * These settings should only be used as a guide for describing
     * the RSSI values returned by the FM driver. Used in conjunction
     * with FmRxApi_GetRssiInfo, the client can use the values from this
     * function to give meaning to the RSSI levels returned by the driver.
     *
     */
    public FmRxSignalThreshold FmRxApi_GetRssiLimit () {
        return null;
    }

    /** This function returns the currently set signal threshold.
     * <p>
     * This value used by the FM driver/hardware to determine which
     * stations are tuned during searches and Alternative Frequency jumps.
     * Additionally, this level is used to determine at what threshold FM_RX_EV_SERVICE_AVAILABLE
     * are generated.
     * <p>
     * This is a command used to return the currently set signal threshold
     * used by the FM driver and/or hardware. This value is used to determine.
     * which stations are tuned during searches and Alternative Frequency
     * jumps as well as when Service available events are generated.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_RX_EV_GET_SIGNAL_THRESHOLD event to the registered client. This event will
     * contain the current signal threshold level.
     *
     */
    public FmRxSignalThreshold FmRxApi_GetSignalThreshold () {
        return null;
    }

    /** Returns the current RDS/RBDS Alternative Frequency Information.
     * <p>
     * This is a command which returns the last known Alternative Frequency
     * information for the currently tuned station. For this command to return
     * meaningful information, the client must first register for Alternative
     * Frequency events by registering an FM_RX_RDS_GRP_AF_EBL call back function.
     * Under normal operating mode, this information will automatically be
     * sent to the client. However, if the client requires this information
     * be sent again, this function can be used.
     * <p>
     * Once completed, this command will generate an
     * FM_RX_EV_RDS_AF_INFO event to the registered client.
     * This event will contain the current RDS/RBDS alternative
     * frequency information.
     *
    */
    public void FmRxApi_GetAFInfo    () {
    }

    /** Initiates basic seek and scan operations.
     * <p>
     * This command is used to invoke a basic seek/scan of the FM radio band.
     * <p>
     * <ul>
     * This API is used to:
     * <li> Invoke basic seek operations
     * <li> Invoke basic scan operations
     * </ul>
     * <p>
     * The most basic operation performed by this function
     * is a FM_RX_SRCH_MODE_SEEK command. The seek process is handled
     * incrementing or decrementing the frequency in pre-defined channel
     * steps (defined by the channel spacing) and measuring the resulting signal
     * level. Once a station is successfully tuned and found to meet or exceed
     * this signal level, the seek operation will be completed and a
     * FM_RX_EV_SEARCH_COMPLETE event will be returned to the client.
     * If no stations are found to match the search criteria, the frequency
     * will be returned to the originally tuned station.
     * <p>
     * Since seek always results in a frequency being tuned, each seek
     * operation will also return a single FM_RX_EV_RADIO_TUNE_STATUS event to
     * the client/application layer.
     * <p>
     * Much like FM_RX_SRCH_MODE_SEEK, a FM_RX_SRCH_MODE_SCAN command can be
     * likened to many back to back seeks with a dwell period after
     * each successful seek. Once issued, a scan will either increment or
     * decrement frequencies by the defined channel spacing until a station is
     * found to meet or exceed the set search threshold. Once this station is found,
     * and is successfully tuned, an FM_RX_EV_RADIO_TUNE_STATUS event will be
     * returned to the client and the station will remain tuned for the specific
     * period of time indicated by teScanTime. After that time expires, an
     * FM_RX_EV_SEARCH_IN_PROGRESS event will be sent to the client and a
     * new search will begin for the next station that meets the search threshold.
     * After scanning the entire band, or after a cancel search has been initiated
     * by the client, an FM_RX_EV_SEARCH_COMPLETE event will be sent to the client.
     * Similar to a seek command, each scan will result in at least one station
     * being tuned, even if this is the starting frequency.
     * <p>
     * Each time the driver initiates a search (seek or scan) the client
     * will be notified via an FM_RX_EV_SEARCH_IN_PROGRESS event.
     * Similarly, each time a search completes, the client will be notified via an
     * FM_RX_EV_SEARCH_COMPLETE event.
     * <p>
     * Once issuing a search command, several commands from the client
     * may be disallowed until the search is completed or cancelled.
     * <p>
     * The search can be canceled at any time by using API FmRxApi_CancelSearch ().
     * Once cancelled, each search will tune to the last tuned station
     * and generate both FM_RX_EV_SEARCH_COMPLETE and FM_RX_EV_RADIO_TUNE_STATUS
     * events.
     *
     * @param mode    Basic FM search mode.
     * @param dwelling    FM scan dwell time.
     *
    */
    public void FmRxApi_SearchStations (FmRxSrchMode mode,FmRxScanTime dwelling){
        mControl.searchStations(sFd, mode, dwelling,null);
    }

    /** Initiates RDS based seek and scan operations.
     * <p>
     * This command allows the client to issue seeks and scans similar
     * to commands found in FmRxApi_SearchStations. However, each command has
     * an additional RDS/RBDS component which must be satisfied before a station
     * is successfully tuned. Please see FmRxApi_SearchStations for an
     * understanding of how seeks and scans work.
     * <p>
     * <ul>
     * This API is used to search stations using RDS:
     * <li> Invokes seek based on program type
     * <li> Invokes scan based on program type with specified dwell period
     * <li> Invokes seek based on program identification
     * <li> Invokes seek for alternate frequency
     * </ul>
     * <p>
     * Much like FM_RX_SRCH_MODE_SEEK in FmRxApi_SearchStations,
     * FM_RX_SRCHRDS_MODE_SEEK_PTY allows the client to seek to
     * stations which are broadcasting RDS/RBDS groups with a
     * particular Program Type that matches the supplied Program Type (PTY).
     * The behavior and events generated for a SEARCHMODE_RDS_SEEK_PTY
     * are very similar to that of FM_RX_SRCH_MODE_SEEK, however only
     * stations meeting the set search signal threshold and are also
     * broadcasting the specified RDS Program Type (PTY) will be tuned.
     * If no matching stations can be found, the original station will
     * be re-tuned.
     * <p>
     * Just as SEARCHMODE_RDS_SEEK_PTY's functionality matches
     * FM_RX_SRCH_MODE_SEEK, so does SEARCHMODE_RDS_SCAN_PTY match
     * FM_RX_SRCH_MODE_SCAN. The one of the differences between the two is that
     * only stations meeting the set search threshold and are also broadcasting a
     * RDS Program Type (PTY) matching tucRdsSrchPty are found and tuned. If
     * no station is found to have the PTY as specified by tucRdsSrchPty, then
     * the original station will be re-tuned.
     * <p>
     * SEARCHMODE_RDS_SEEK_PI is used the same way as
     * SEARCHMODE_RDS_SEEK_PTY, but only stations which meet the set
     * search threshold and are also broadcasting the Program Identification
     * RdsSrchPI are tuned.
     * <p>
     * Lastly, SEARCHMODE_RDS_SEEK_AF functionality differs slightly
     * compared to the other commands in this function. This command only
     * seeks to stations which are known ahead of time to be Alternative
     * Frequencies for the currently tune station. If no alternate frequencies
     * are known, or if the Alternative Frequencies have weaker signal strength
     * than the original frequency, the original frequency will be re-tuned.
     * <p>
     * Each time the driver initiates an RDS-based search, the client will be
     * notified via a FM_RX_EV_SEARCH_RDS_IN_PROGRESS event. Similarly, each
     * time an RDS-based search completes, the client will be notified via a
     * FM_RX_EV_SEARCH_RDS_COMPLETE event.
     * <p>
     * Once issuing a search command, several commands from the client may be
     * disallowed until the search is completed or canceled.
     * <p>
     * The search can be canceled at any time by using API FmRxApi_CancelSearch ().
     * Once canceled, each search will tune to the last tuned station and generate
     * both FM_RX_EV_SEARCH_RDS_COMPLETE and FM_RX_EV_RADIO_TUNE_STATUS events.
     *
     * @param mode    Basic FM search mode.
     * @param dwelling    Basic FM scan dwell time
     * @param direction    Basic FM search direction.
     * @param RdsSrchPty FM RDS search Program Type
     * @param RdsSrchPI FM RDS search Program Identification Type
     */
    public void FmRxApi_SearchRdsStations (FmRxSrchMode mode, FmRxScanTime dwelling,
            FmRxSrchDir direction, int RdsSrchPty, int RdsSrchPI){
    }

    /** Initiates station list search operations.
     * <p>
     * This will generate frequency lists based on strong and weak stations as
     * well as stations matching a particular RDS/RBDS program type (e.g. "News").
     * <p>
     * This command is used to generate frequency lists based on stations found
     * in the FM band.
     * <p>
     * <ul>
     * This API is used to generate station lists which consist of:
     *                <li>strong stations.
     * <li>    weak stations.
     * <li>The strongest stations in the band.
     * <li>The weakest stations in the band.
     * <li>Station which match a particular RDS/RBDS program type.
     *</ul>
     * <p>
     * The range of frequencies scanned depends on the currently set band.
     * The driver searches for all valid stations in the band and when complete,
     * returns a channel list based on the client's selection. The client can
     * choose to search for a list of the strongest stations in the band, the
     * weakest stations in the band, or the first N strong or weak stations. In
     * addition, the user can also choose to generate a list of stations which
     * match a particular RDS/RBDS program type. By setting the ulSrchListMax
     * parameter, the client can constrain the number of frequencies returned in
     * the list. If user specifies ulSrchListMax to be 0, the search will generate
     * the maximum number of stations possible.
     * <p>
     * Each time the driver initiates a list-based search, the client will be
     * notified via an FM_RX_EV_SEARCH_LIST_IN_PROGRESS event.
     * Similarly, each time a list-based search completes, the client will be
     * notified via an FM_RX_EV_SEARCH_LIST_COMPLETE event.
     * <p>
     * On completion or cancellation of the search, the originally tuned station
     * will be tuned and the following events will be generated:
     * FM_RX_EV_SEARCH_LIST_COMPLETE - The search has completed.
     * FM_RX_EV_RADIO_TUNE_STATUS - The original frequency has been re-tuned.
     * <p>
     * Once issuing a search command, several commands from the client may be
     * disallowed until the search is completed or cancelled.
     * <p>
     * The search can be canceled at any time by using API FmRxApi_CancelSearch ().
     * A cancelled search is treated as a completed search and the same events
     * will be generated. However, the search list generated may only contain
     * a partial list.
     * @param listMode RDS/RBDS FM RDS search mode.
     * @param direction    RDS/RBDS FM search direction.
     * @param listMax Maximum number of stations that can be returned from a search.
     * @param pgmType    List-based FM search Program Type.
     */
    public void FMRxApi_SearchStationList (FmRxSrchListMode    listMode,
            FmRxSrchDir direction, int listMax,int pgmType){
        mControl.searchStations(sFd, FmRxSrchMode.FM_RX_SRCH_MODE_SEEK_UP,
                FmRxScanTime.FM_RX_DWELL_PERIOD_2S, FmRxSrchDir.FM_RX_SEARCHDIR_DOWN);
    }

    /** This function cancels any ongoing search operation (seek, scan, searchlist, etc).
     * <p>
     * This is a command used to cancel a previously initiated search
     * (e.g. Basic Seek/Scan, RDS Seek/Scans, Search list, etc...).
     * <p>
     * Once completed, this command will generate an
     * FM_RX_EV_SEARCH_CANCELLED event to all registered clients.
     * Following this event, the client may also receive search events related
     * to the ongoing search now being complete.
     *
     */
    public void FmRxApi_CancelSearch () {
        mControl.cancelSearch(sFd);
    }

    /** This function enables or disables various RDS/RBDS group filtering and
     * buffering features.
     * <p>
     * Included in these features are the RDS group enable mask, RDS/RBDS group
     * change filter, and the RDS/RBDS group buffer size.
     * <p>
     * This is a function used to set or unset various Rx RDS/RBDS group filtering
     * and buffering options in the FM driver.
     * <p>
     * Included in these options is the ability for the client to select
     * which RDS/RBDS groups should be sent to the client. By default, all
     * RDS/RBDS groups are filtered out before reaching the client. To allow one
     * or more specific groups to be received, the client must set one or mors bits
     * within the RdsGrpEnableMask bitmask. Each bit in this mask corresponds
     * to a specific RDS/RBDS group type. Once a group is enabled, and when a
     * buffer holding those groups reaches the threshold defined by RdsBufSize,
     * the group or groups will be sent to the client as a FM_RX_EV_RDS_GROUP_DATA
     * event.
     * <p>
     * Additionally, this function also allows the client to enable or
     * disable the RDS/RBDS group change filter. This filter allows the client
     * to prevent duplicate groups of the same group type from being received.
     * This filter only applies to consecutive groups, so identical groups
     * received in different order will not be filtered out.
     * <p>
     * Once completed, this command will generate an
     * FM_RX_EV_RDS_GROUP_OPTIONS_SET event to all registered clients.
     *
     */
    public void FmRxApi_SetRdsGroupOptions () {
    }

    /** This function enables or disables RDS/RBDS group processing features.
     * <p>
     * Included in these features is the ability for the FM driver to return
     * Program Service, RadioText, and Alternative Frequency information.
     * <p>
     * This is an asynchronous function used to set or unset various Rx
     * RDS/RBDS group processing options in the FM driver.
     * <p>
     * These options free the client from the burden of collecting a continuous
     * stream of RDS/RBDS groups and processing them. By setting the
     * FM_RX_RDS_GRP_RT_EBL bit in FmGrpsToProc, the FM hardware or driver
     * will collect RDS/RBDS 2A/2B groups and return complete RadioText strings
     * and information in the form of a FM_RX_EV_RDS_RT_INFO event. This event
     * will be generated only when the RadioText information changes.
     * <p>
     * Similarly, by setting either the FM_RX_RDS_GRP_PS_EBL or
     * FM_RX_RDS_GRP_PS_SIMPLE_EBL bit in OFmGrpsToProc, the FM hardware or
     * driver will collect RDS/RBDS 0A/0B groups and return Program Service
     * information in the form of a FM_RX_EV_RDS_PS_INFO event. This event will
     * be generated whenever the Program Service information changes. This event
     * will include one or more collected Program Service strings which can be
     * continuously displayed by the client.
     * <p>
     * Additionally, by setting the FM_RX_RDS_GRP_AF_EBL bit in OFmGrpsToProc,
     * the FM hardware or driver will collect RDS/RBDS 0A/0B groups and return
     * Alternative Frequency information in the form of a OFM_RX_EV_RDS_AF_INFO
     * event. This event will be generated when the Alternative Frequency
     * information changes and will include an up to date list of all known
     * Alternative Frequencies.
     * <p>
     * Lastly, by setting the FM_RX_RDS_GRP_AF_JUMP_EBL bit in FmGrpsToProc, the
     * FM hardware or driver will collect RDS/RBDS 0A/0B groups and automatically
     * tune to a stronger alternative frequency when the signal level falls below
     * the search threshold.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_RX_EV_RDS_PROC_REG_DONE event to all registered clients.
     *
     */
    public void FmRxApi_RegRdsGroupProcessing (){
    }

    /** This function enables or disables RDS/RBDS Program Identification
     * (PI) match events.
     * <p>
     * This is an asynchronous function used to set or unset RDS/RBDS Program
     * Identification (PI) match events. By passing the desired PI in prgmId,
     * the client will be alerted through a FM_RX_EV_RDS_PI_MATCH_AVAILABLE event
     * when a station matching the supplied PI is found. This can be useful when
     * the client is manually searching through the band for a particular station.
     * An example of this could be that the client has stored the PI of a known
     * station, but that station no longer exists on the known frequency. Here, the
     * client could set the PI match event and can sequentially tune to all
     * stations in the band until an event is returned or the client arrives again
     * on the starting frequency.
     * <p>
     * Once completed, this command will generate an asynchronous
     * FM_RX_EV_RDS_PI_MATCH_REG_DONE event to all registered clients.
     *
     */
    public void FmRxApi_RegRdsPiMatchProcessing () {
    }

    /** This function gets the current power mode.
     * @return Returns the current power mode to the user
     *
     */
    public FmRxPwrMode FmRxApi_getPwrMode(){
        return mPowerMode;
    }
}
