/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
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

#define LOG_TAG "HDMIDaemon"

#include <ctype.h>
#include <stdint.h>
#include <sys/types.h>
#include <math.h>
#include <fcntl.h>
#include <utils/misc.h>
#include <signal.h>

#include <binder/IPCThreadState.h>
#include <utils/threads.h>
#include <utils/Atomic.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/AssetManager.h>

#include <ui/DisplayInfo.h>
#include <ui/FramebufferNativeWindow.h>
#include <linux/msm_mdp.h>
#include <linux/fb.h>
#include <sys/ioctl.h>

#include <cutils/properties.h>

#include "HDMIDaemon.h"

namespace android {

// ---------------------------------------------------------------------------

#define DEVICE_ROOT "/sys/class/graphics"
#define DEVICE_NODE "fb1"

#define HDMI_SOCKET_NAME        "hdmid"

#define HDMI_EVT_CONNECTED      "hdmi_connected"
#define HDMI_EVT_DISCONNECTED   "hdmi_disconnected"

#define HDMI_CMD_ENABLE_HDMI    "enable_hdmi"
#define HDMI_CMD_DISABLE_HDMI   "disable_hdmi"
#define HDMI_CMD_CHANGE_MODE    "change_mode: "
#define HDMI_CMD_MIRROR         "hdmi_mirror: "
#define HDMI_CMD_SET_ASWIDTH    "set_aswidth: "
#define HDMI_CMD_SET_ASHEIGHT   "set_asheight: "

#define SYSFS_CONNECTED         DEVICE_ROOT "/" DEVICE_NODE "/connected"
#define SYSFS_EDID_MODES        DEVICE_ROOT "/" DEVICE_NODE "/edid_modes"

HDMIDaemon::HDMIDaemon() : Thread(false),
           mFrameworkSock(-1), mAcceptedConnection(-1), mUeventSock(-1),
           mHDMIUeventQueueHead(NULL), fd1(-1)
{
}

HDMIDaemon::~HDMIDaemon() {
    HDMIUeventQueue* tmp = mHDMIUeventQueueHead, *tmp1;
    while (tmp != NULL) {
        tmp1 = tmp;
        tmp = tmp->next;
        delete tmp1;
    }
    mHDMIUeventQueueHead = NULL;
    if (fd1 > 0)
        close(fd1);
}

void HDMIDaemon::onFirstRef() {
    run("HDMIDaemon", PRIORITY_AUDIO);
}

sp<SurfaceComposerClient> HDMIDaemon::session() const {
    return mSession;
}


void HDMIDaemon::binderDied(const wp<IBinder>& who)
{
    requestExit();
}

status_t HDMIDaemon::readyToRun() {

    if ((mFrameworkSock = android_get_control_socket(HDMI_SOCKET_NAME)) < 0) {
        LOGE("Obtaining file descriptor socket '%s' failed: %s",
             HDMI_SOCKET_NAME, strerror(errno));
        return -1;
    }

    if (listen(mFrameworkSock, 4) < 0) {
        LOGE("Unable to listen on fd '%d' for socket '%s': %s",
             mFrameworkSock, HDMI_SOCKET_NAME, strerror(errno));
        return -1;
    }

    struct sockaddr_nl nladdr;
    memset(&nladdr, 0, sizeof(nladdr));
    nladdr.nl_family = AF_NETLINK;
    nladdr.nl_pid = getpid();
    nladdr.nl_groups = 0xffffffff;

    if ((mUeventSock = socket(PF_NETLINK,
                             SOCK_DGRAM,NETLINK_KOBJECT_UEVENT)) < 0) {
        LOGE("Unable to create uevent socket: %s", strerror(errno));
        return -1;
    }

    int uevent_sz = 64 * 1024;
    if (setsockopt(mUeventSock, SOL_SOCKET, SO_RCVBUFFORCE, &uevent_sz,
                   sizeof(uevent_sz)) < 0) {
        LOGE("Unable to set uevent socket options: %s", strerror(errno));
        return -1;
    }

    if (bind(mUeventSock, (struct sockaddr *) &nladdr, sizeof(nladdr)) < 0) {
        LOGE("Unable to bind uevent socket: %s", strerror(errno));
        return -1;
    }

    LOGD("readyToRun: success");

    return NO_ERROR;
}

bool HDMIDaemon::threadLoop()
{
    int max = -1;
    fd_set read_fds;
    FD_ZERO(&read_fds);

    FD_SET(mFrameworkSock, &read_fds);
    if (max < mFrameworkSock)
        max = mFrameworkSock;
    FD_SET(mUeventSock, &read_fds);
    if (max < mUeventSock)
        max = mUeventSock;

    if (mAcceptedConnection != -1) {
        FD_SET(mAcceptedConnection, &read_fds);
        if (max < mAcceptedConnection)
            max = mAcceptedConnection;
    }

    struct timeval to;
    to.tv_sec = (60 * 60);
    to.tv_usec = 0;

    int ret;
    if ((ret = select(max + 1, &read_fds, NULL, NULL, &to)) < 0) {
        LOGE("select() failed (%s)", strerror(errno));
        sleep(1);
        return true;
    }

    if (!ret) {
        return true;
    }

    if (mAcceptedConnection != -1 && FD_ISSET(mAcceptedConnection, &read_fds)) {
        if (processFrameworkCommand() == -1)
            mAcceptedConnection = -1;
    }

    if (FD_ISSET(mFrameworkSock, &read_fds)) {
        struct sockaddr addr;
        socklen_t alen;
        alen = sizeof(addr);

        if (mAcceptedConnection != -1) {
            close(mAcceptedConnection);
            mAcceptedConnection = accept(mFrameworkSock, &addr, &alen);
            return true;
        }

        if ((mAcceptedConnection = accept(mFrameworkSock, &addr, &alen)) < 0) {
            LOGE("Unable to accept framework connection (%s)",
                strerror(errno));
        }
        else {
            mSession = new SurfaceComposerClient();
            processUeventQueue();

            if (!mDriverOnline) {
                LOGE("threadLoop: driver not online; use state-file");
                sendCommandToFramework(cableConnected());
            }
        }

        LOGD("threadLoop: Accepted connection from framework");
    }

    if (FD_ISSET(mUeventSock, &read_fds)) {
        if (mAcceptedConnection == -1)
            queueUevent();
        else
            processUevent();
    }

    return true;
}

bool HDMIDaemon::cableConnected(bool defaultValue) const
{
    int hdmiStateFile = open(SYSFS_CONNECTED, O_RDONLY, 0);
    if (hdmiStateFile < 0) {
        LOGE("cableConnected: state file '%s' not found", SYSFS_CONNECTED);
        return defaultValue;
    } else {
        char buf;
        bool ret = defaultValue;
        int err = read(hdmiStateFile, &buf, 1);
        if (err <= 0) {
            LOGE("cableConnected: empty state file '%s'", SYSFS_CONNECTED);
        } else {
            if (buf == '1') {
                LOGD("cableConnected: %s indicates CONNECTED", SYSFS_CONNECTED);
                ret = true;
            } else {
                LOGD("cableConnected: %s indicates DISCONNECTED", SYSFS_CONNECTED);
                ret = false;
            }
        }
        close(hdmiStateFile);
        return ret;
    }
}

bool HDMIDaemon::processUeventMessage(uevent& event)
{
    char buffer[64 * 1024];
    int count;
    char *s = buffer;
    char *end;
    int param_idx = 0;
    int i;
    bool first = true;

    if ((count = recv(mUeventSock, buffer, sizeof(buffer), 0)) < 0) {
        LOGE("Error receiving uevent (%s)", strerror(errno));
        return false;
    }

    end = s + count;
    while (s < end) {
        if (first) {
            char *p;
            for (p = s; *p != '@'; p++);
            p++;
            if (!strcasestr(p, DEVICE_NODE)) {
                return false;
            }
            LOGD("device uevent (%s)", buffer);
            event.path = new char[strlen(p) + 1];
            strcpy(event.path, p);
            first = false;
        } else {
            if (!strncmp(s, "ACTION=", strlen("ACTION="))) {
                char *a = s + strlen("ACTION=");

                if (!strcmp(a, "add"))
                    event.action = action_add;
                else if (!strcmp(a, "change"))
                    event.action = action_change;
                else if (!strcmp(a, "remove"))
                    event.action = action_remove;
                else if (!strcmp(a, "online"))
                    event.action = action_online;
                else if (!strcmp(a, "offline"))
                    event.action = action_offline;
                else
                    LOGD("%s: action (%s) unknown", __func__, a);
            } else if (!strncmp(s, "SEQNUM=", strlen("SEQNUM=")))
                event.seqnum = atoi(s + strlen("SEQNUM="));
            else if (!strncmp(s, "SUBSYSTEM=", strlen("SUBSYSTEM="))) {
                event.subsystem = new char[strlen(s + strlen("SUBSYSTEM=")) + 1];
                strcpy(event.subsystem, (s + strlen("SUBSYSTEM=")));
            }
            else {
                event.param[param_idx] = new char[strlen(s) + 1];
                strcpy(event.param[param_idx], s);
                param_idx++;
            }
        }
        s += strlen(s) + 1;
    }
    return true;
}

void HDMIDaemon::queueUevent()
{
    HDMIUeventQueue* tmp = mHDMIUeventQueueHead, *tmp1;
    while (tmp != NULL && tmp->next != NULL)
        tmp = tmp->next;
    if (!tmp) {
        tmp = new HDMIUeventQueue();
        tmp->next = NULL;
        if(!processUeventMessage(tmp->mEvent))
            delete tmp;
        else
            mHDMIUeventQueueHead = tmp;
    }
    else {
        tmp1 = new HDMIUeventQueue();
        tmp1->next = NULL;
        if(!processUeventMessage(tmp1->mEvent))
            delete tmp1;
        else
            tmp->next = tmp1;
    }
}

void HDMIDaemon::processUeventQueue()
{
    HDMIUeventQueue* tmp = mHDMIUeventQueueHead, *tmp1;
    while (tmp != NULL) {
        tmp1 = tmp;
	if (tmp->mEvent.action == action_offline) {
            LOGD("processUeventQueue: event.action == offline");
            mDriverOnline = true;
            sendCommandToFramework(false);
        } else if (tmp->mEvent.action == action_online) {
            LOGD("processUeventQueue: event.action == online");
            mDriverOnline = true;
            sendCommandToFramework(true);
        }
        tmp = tmp->next;
        delete tmp1;
    }
    mHDMIUeventQueueHead = NULL;
}

void HDMIDaemon::processUevent()
{
    uevent event;
    if(processUeventMessage(event)) {
        if (event.action == action_offline) {
            LOGD("processUevent: event.action == offline");
            mDriverOnline = true;
            sendCommandToFramework(false);
        } else if (event.action == action_online) {
            LOGD("processUevent: event.action == online");
            mDriverOnline = true;
            sendCommandToFramework(true);
        }
    }
}

struct disp_mode_timing_type {
    int  video_format;

    int  active_h;
    int  active_v;

    int  front_porch_h;
    int  pulse_width_h;
    int  back_porch_h;

    int  front_porch_v;
    int  pulse_width_v;
    int  back_porch_v;

    int  pixel_freq;
    bool interlaced;

    void set_info(struct fb_var_screeninfo &info) const;
};

void disp_mode_timing_type::set_info(struct fb_var_screeninfo &info) const
{
    info.reserved[0] = 0;
    info.reserved[1] = 0;
    info.reserved[2] = 0;
    info.reserved[3] = video_format;

    info.xoffset = 0;
    info.yoffset = 0;
    info.xres = active_h;
    info.yres = active_v;

    info.pixclock = pixel_freq*1000;
    info.vmode = interlaced ? FB_VMODE_INTERLACED : FB_VMODE_NONINTERLACED;

    info.right_margin = front_porch_h;
    info.hsync_len = pulse_width_h;
    info.left_margin = back_porch_h;
    info.lower_margin = front_porch_v;
    info.vsync_len = pulse_width_v;
    info.upper_margin = back_porch_v;
}

/* Video formates supported by the HDMI Standard */
/* Indicates the resolution, pix clock and the aspect ratio */
#define m640x480p60_4_3         1
#define m720x480p60_4_3         2
#define m720x480p60_16_9        3
#define m1280x720p60_16_9       4
#define m1920x1080i60_16_9      5
#define m1440x480i60_4_3        6
#define m1440x480i60_16_9       7
#define m1920x1080p60_16_9      16
#define m720x576p50_4_3         17
#define m720x576p50_16_9        18
#define m1280x720p50_16_9       19
#define m1440x576i50_4_3        21
#define m1440x576i50_16_9       22
#define m1920x1080p50_16_9      31
#define m1920x1080p24_16_9      32
#define m1920x1080p25_16_9      33
#define m1920x1080p30_16_9      34

static struct disp_mode_timing_type supported_video_mode_lut[] = {
    {m640x480p60_4_3,     640,  480,  16,  96,  48, 10, 2, 33,  25200, false},
    {m720x480p60_4_3,     720,  480,  16,  62,  60,  9, 6, 30,  27030, false},
    {m720x480p60_16_9,    720,  480,  16,  62,  60,  9, 6, 30,  27030, false},
    {m1280x720p60_16_9,  1280,  720, 110,  40, 220,  5, 5, 20,  74250, false},
    {m1920x1080i60_16_9, 1920,  540,  88,  44, 148,  2, 5,  5,  74250, false},
    {m1440x480i60_4_3,   1440,  240,  38, 124, 114,  4, 3, 15,  27000, true},
    {m1440x480i60_16_9,  1440,  240,  38, 124, 114,  4, 3, 15,  27000, true},
    {m1920x1080p60_16_9, 1920, 1080,  88,  44, 148,  4, 5, 36, 148500, false},
    {m720x576p50_4_3,     720,  576,  12,  64,  68,  5, 5, 39,  27000, false},
    {m720x576p50_16_9,    720,  576,  12,  64,  68,  5, 5, 39,  27000, false},
    {m1280x720p50_16_9,  1280,  720, 440,  40, 220,  5, 5, 20,  74250, false},
    {m1440x576i50_4_3,   1440,  288,  24, 126, 138,  2, 3, 19,  27000, true},
    {m1440x576i50_16_9,  1440,  288,  24, 126, 138,  2, 3, 19,  27000, true},
    {m1920x1080p50_16_9, 1920, 1080, 528,  44, 148,  4, 5, 36, 148500, false},
    {m1920x1080p24_16_9, 1920, 1080, 638,  44, 148,  4, 5, 36,  74250, false},
    {m1920x1080p25_16_9, 1920, 1080, 528,  44, 148,  4, 5, 36,  74250, false},
    {m1920x1080p30_16_9, 1920, 1080,  88,  44, 148,  4, 5, 36,  74250, false},
};

bool HDMIDaemon::readResolution()
{
    int hdmiEDIDFile = open(SYSFS_EDID_MODES, O_RDONLY, 0);

    memset(mEDIDs, 0, sizeof(mEDIDs));
    if (hdmiEDIDFile < 0) {
        LOGE("%s: edid_modes file '%s' not found", __func__, SYSFS_EDID_MODES);
        return false;
    } else {
        int r = read(hdmiEDIDFile, mEDIDs, sizeof(mEDIDs)-1);
        if (r <= 0)
            LOGE("%s: edid_modes file empty '%s'", __func__, SYSFS_EDID_MODES);
        else {
            while (r > 1 && isspace(mEDIDs[r-1]))
                --r;
            mEDIDs[r] = 0;
        }
    }
    close(hdmiEDIDFile);

    return (strlen(mEDIDs) > 0);
}

bool HDMIDaemon::openFramebuffer()
{
    if (fd1 == -1) {
        fd1 = open("/dev/graphics/fb1", O_RDWR);
        if (fd1 < 0)
            LOGE("ERROR: /dev/graphics/fb1 not available\n");
    }
    return (fd1 > 0);
}

void HDMIDaemon::setResolution(int ID)
{
    if (mCurrentID == ID || !openFramebuffer())
        return;

    const struct disp_mode_timing_type *mode = &supported_video_mode_lut[0];
    for (unsigned int i = 0; i < sizeof(supported_video_mode_lut)/sizeof(*supported_video_mode_lut); ++i) {
        const struct disp_mode_timing_type *cur = &supported_video_mode_lut[i];
        if (cur->video_format == ID)
            mode = cur;
    }
    SurfaceComposerClient::enableHDMIOutput(0);
    struct fb_var_screeninfo info;
    ioctl(fd1, FBIOGET_VSCREENINFO, &info);
    LOGD("GET Info<ID=%d %dx%d (%d,%d,%d), (%d,%d,%d) %dMHz>",
        info.reserved[3], info.xres, info.yres,
        info.right_margin, info.hsync_len, info.left_margin,
        info.lower_margin, info.vsync_len, info.upper_margin,
        info.pixclock/1000/1000);
    mode->set_info(info);
    LOGD("SET Info<ID=%d => Info<ID=%d %dx%d (%d,%d,%d), (%d,%d,%d) %dMHz>", ID,
        info.reserved[3], info.xres, info.yres,
        info.right_margin, info.hsync_len, info.left_margin,
        info.lower_margin, info.vsync_len, info.upper_margin,
        info.pixclock/1000/1000);
    info.activate = FB_ACTIVATE_NOW | FB_ACTIVATE_ALL | FB_ACTIVATE_FORCE;
    ioctl(fd1, FBIOPUT_VSCREENINFO, &info);
    SurfaceComposerClient::enableHDMIOutput(1);
    mCurrentID = ID;
}

int HDMIDaemon::processFrameworkCommand()
{
    char buffer[128];
    int ret;

    if ((ret = read(mAcceptedConnection, buffer, sizeof(buffer) -1)) < 0) {
        LOGE("Unable to read framework command (%s)", strerror(errno));
        return -1;
    }
    else if (!ret)
        return -1;

    buffer[ret] = 0;

    if (!strcmp(buffer, HDMI_CMD_ENABLE_HDMI)) {
        if (!openFramebuffer())
            return -1;
        struct fb_var_screeninfo info;
        LOGD(HDMI_CMD_ENABLE_HDMI);
        ioctl(fd1, FBIOBLANK, FB_BLANK_UNBLANK);
        ioctl(fd1, FBIOGET_VSCREENINFO, &info);
        info.activate = FB_ACTIVATE_NOW | FB_ACTIVATE_ALL | FB_ACTIVATE_FORCE;
        ioctl(fd1, FBIOPUT_VSCREENINFO, &info);
        property_set("hw.hdmiON", "1");
        int en = 1;
        ioctl(fd1, MSMFB_OVERLAY_PLAY_ENABLE, &en);
        SurfaceComposerClient::enableHDMIOutput(1);
    } else if (!strcmp(buffer, HDMI_CMD_DISABLE_HDMI)) {
        LOGD(HDMI_CMD_DISABLE_HDMI);

        if (!openFramebuffer())
            return -1;
        int en = 0;
        ioctl(fd1, MSMFB_OVERLAY_PLAY_ENABLE, &en);
        property_set("hw.hdmiON", "0");
        SurfaceComposerClient::enableHDMIOutput(0);
        ioctl(fd1, FBIOBLANK, FB_BLANK_POWERDOWN);
        close(fd1);
        fd1 = -1;
    } else if (!strncmp(buffer, HDMI_CMD_MIRROR, strlen(HDMI_CMD_MIRROR))) {
        int mode;
        int ret = sscanf(buffer, HDMI_CMD_MIRROR "%d", &mode);
        if (ret == 1) {
            LOGD(HDMI_CMD_MIRROR "%d", mode);
            SurfaceComposerClient::enableHDMIOutput(mode);
            if(mode){
                 property_set("hw.hdmiON", "1");
            } else {
                 property_set("hw.hdmiON", "0");
            }
        }
    } else if (!strncmp(buffer, HDMI_CMD_SET_ASWIDTH, strlen(HDMI_CMD_SET_ASWIDTH))) {
        float asWidthRatio;
        int ret = sscanf(buffer, HDMI_CMD_SET_ASWIDTH "%f", &asWidthRatio);
        if(ret==1) {
            SurfaceComposerClient::setActionSafeWidthRatio(asWidthRatio);
        }
    } else if (!strncmp(buffer, HDMI_CMD_SET_ASHEIGHT, strlen(HDMI_CMD_SET_ASHEIGHT))) {
        float asHeightRatio;
        int ret = sscanf(buffer, HDMI_CMD_SET_ASHEIGHT "%f", &asHeightRatio);
        if(ret==1) {
            SurfaceComposerClient::setActionSafeHeightRatio(asHeightRatio);
        }
    } else {
        int mode;
        int ret = sscanf(buffer, HDMI_CMD_CHANGE_MODE "%d", &mode);
        if (ret == 1) {
            LOGE(HDMI_CMD_CHANGE_MODE);

            /* To change the resolution */
            char prop_val[PROPERTY_VALUE_MAX];
            property_get("enable.hdmi.edid", prop_val, "0");
            int val = atoi(prop_val);
            if(val == 1) {
                 /* Based on the hw.yRes set the resolution */
                 char property_value[PROPERTY_VALUE_MAX];
                 property_get("hdmi.yRes", property_value, "0");
                 int yres = atoi(property_value);
                 switch(yres){
                 case 480:
                     mode = 3;
                     break;
                 case 720:
                    mode = 4;
                    break;
                 case 1080:
                    mode = 16;
                    break;
                default:
                    break;
                 }
            }
            LOGD("Setting Mode to =  %d", mode);
            setResolution(mode);
        }
    }

    return 0;
}

bool HDMIDaemon::sendCommandToFramework(bool connected)
{
    char message[512];

    if (connected) {
        readResolution();
        snprintf(message, sizeof(message), "%s: %s", HDMI_EVT_CONNECTED, mEDIDs);
    } else
        strncpy(message, HDMI_EVT_DISCONNECTED, sizeof(message));

    int result = write(mAcceptedConnection, message, strlen(message) + 1);
    LOGE("sendCommandToFramework: '%s' %s", message, result >= 0 ? "successful" : "failed");
    return result >= 0;
}

// ---------------------------------------------------------------------------

}
; // namespace android
