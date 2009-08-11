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

#define LOG_TAG "FMRxAPI"

#include "jni.h"
#include "nativehelper/JNIHelp.h"
#include "utils/Log.h"
#include "utils/misc.h"
#include "android_runtime/AndroidRuntime.h"
#include <fcntl.h>
#include <sys/ioctl.h>
#include <media/tavarua.h>
#include <linux/videodev2.h>
#include <math.h>

#define RADIO "/dev/radio0"
#define FM_JNI_SUCCESS 0L
#define FM_JNI_FAILURE -1L
#define SEARCH_UP 0
#define SEARCH_DOWN 1
#define TUNE_MULT 16000.
enum search_t {
    SEEK_UP,
    SEEK_DN,
    SCAN_UP,
    SCAN_DN
};

using namespace android;

/* native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_acquireFdNative
        (JNIEnv* env, jobject thiz, jstring path)
{
    int fd;
    const char* radio_path = env->GetStringUTFChars(path, NULL);
    if(radio_path == NULL){
        fd = open(RADIO, O_RDONLY);
    }
    else{
        fd = open(radio_path, O_RDONLY);
    }
    if(fd < 0){
         return FM_JNI_FAILURE;
    }
    env->ReleaseStringUTFChars(path, NULL);
    return fd;
}

/* native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_closeFdNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    close(fd);
    return FM_JNI_SUCCESS;
}

/*native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_changeFreqNative
    (JNIEnv * env, jobject thiz, jdouble freq, jint fd)
{
    int err;
    double tune;

    struct v4l2_frequency freq_struct;
    freq_struct.type = V4L2_TUNER_RADIO;
    freq_struct.frequency = rint( freq * TUNE_MULT);
    err = ioctl(fd, VIDIOC_S_FREQUENCY, &freq_struct);
    if(err < 0){
            return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

/* native interface */
static jdouble android_hardware_FMRxAPI_FmReceiverJNI_getFrequencyNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    int err;
    unsigned long fq;
    struct v4l2_frequency freq;
    freq.type = V4L2_TUNER_RADIO;
    fq = (double)ioctl(fd, VIDIOC_G_FREQUENCY, &freq);
    return (TUNE_MULT/fq);
}

/* native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_changeRadioStateNative
    (JNIEnv * env, jobject thiz, jint state, jint fd)
{
    struct v4l2_control control;
    int err;

    control.value = state;
    control.id = V4L2_CID_PRIVATE_TAVARUA_STATE;
    err = ioctl(fd,VIDIOC_S_CTRL,&control);
    if(err < 0){
        return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

static int seekScan(int fd,int mode, int dir, int dwell)
{
    struct v4l2_control control;
    struct v4l2_hw_freq_seek seek;
    control.id=V4L2_CID_PRIVATE_TAVARUA_SRHCMODE;
    control.value=mode;
    if(ioctl(fd, VIDIOC_S_CTRL, &control) < 0){
        return FM_JNI_FAILURE;
    }
    seek.seek_upward=dir;
    if(ioctl(fd, VIDIOC_S_HW_FREQ_SEEK, &seek) < 0){
        return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

/* native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_seekScanControlNative
(JNIEnv * env, jobject thiz, jint fd, jint searchControl, jint dwell_time)
{
    int ret;
    switch((enum search_t)searchControl)
    {
    /* Seek Up */
    case SEEK_UP:
        ret=seekScan(fd,SEEK,SEARCH_UP,dwell_time);
        break;
    /* Seek Down */
    case SEEK_DN:
        ret=seekScan(fd,SEEK,SEARCH_DOWN,dwell_time);
        break;
    /* Scan Up */
    case SCAN_UP:
        ret=seekScan(fd,SCAN,SEARCH_UP,dwell_time);
        break;
    /* Scan Down */
    case SCAN_DN:
        ret=seekScan(fd,SCAN,SEARCH_DOWN,dwell_time);
        break;
    default:
        ret=FM_JNI_FAILURE;
        break;
    }
    return ret;
}

/* native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_audioControlNative
(JNIEnv * env, jobject thiz, jint fd, jint control, jint field)
{
    struct v4l2_control va;

    int err = 0;
    int ret;
    va.id = V4L2_CID_AUDIO_MUTE;
    va.value=control;
    err = ioctl(fd, VIDIOC_S_CTRL, &va);
    if (err < 0){
        return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

/* native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_cancelSearchNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    struct v4l2_control control;
    int err;
    control.id=V4L2_CID_PRIVATE_TAVARUA_SRCHON;
    control.value=0;
    err = ioctl(fd,VIDIOC_S_CTRL,&control);
    if(err < 0){
        return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

/* native interface */
static jint android_hardware_FMRxAPI_FmReceiverJNI_listenForEventsNative
 (JNIEnv * env, jobject thiz, jint fd, jbooleanArray buff, jint index)
{
    int err;
    jboolean isCopy;
    struct v4l2_requestbuffers reqbuf;
    struct v4l2_buffer v4l2_buf;
    memset(&reqbuf, 0, sizeof (reqbuf));
    enum v4l2_buf_type type = V4L2_BUF_TYPE_PRIVATE;
    reqbuf.type = V4L2_BUF_TYPE_PRIVATE;
    reqbuf.memory = V4L2_MEMORY_USERPTR;
    jboolean *bool_buffer = env->GetBooleanArrayElements(buff,&isCopy);
    memset(&v4l2_buf, 0, sizeof (v4l2_buf));
    v4l2_buf.index = index;
    v4l2_buf.type = type;
    v4l2_buf.length = 128;
    v4l2_buf.m.userptr = (unsigned long)bool_buffer;
    err = ioctl(fd,VIDIOC_DQBUF,&v4l2_buf) ;
    if(err < 0){
        return FM_JNI_FAILURE;
    }
    if(isCopy == JNI_TRUE){
        env->ReleaseBooleanArrayElements(buff, bool_buffer, 0);
    }
    return v4l2_buf.bytesused;
}

/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] = {
        /* name, signature, funcPtr */
        { "acquireFdNative", "(Ljava/lang/String;)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_acquireFdNative},
        { "closeFdNative", "(I)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_closeFdNative},
        { "getFrequencyNative", "(I)D",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_getFrequencyNative},
        { "changeFreqNative", "(DI)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_changeFreqNative},
        { "changeRadioStateNative", "(II)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_changeRadioStateNative},
        { "seekScanControlNative", "(III)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_seekScanControlNative},
        { "audioControlNative", "(III)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_audioControlNative},
        { "cancelSearchNative", "(I)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_cancelSearchNative},
        { "listenForEventsNative", "(I[BI)I",
            (void*)android_hardware_FMRxAPI_FmReceiverJNI_listenForEventsNative},
};

int register_android_hardware_fm_FMRxAPI(JNIEnv* env)
{
        return jniRegisterNativeMethods(env, "android/hardware/FMRxAPI/FmReceiverJNI",
                gMethods, NELEM(gMethods));
}


