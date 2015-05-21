/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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
 *     * Neither the name of The Linux Foundation nor the names of its
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
/*
 * Copyright 2014 The Android Open Source Project
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

#define LOG_TAG "TvInputHal"

//#define LOG_NDEBUG 0

#include "android_os_MessageQueue.h"
#include "android_runtime/AndroidRuntime.h"
#include "android_runtime/android_view_Surface.h"
#include "JNIHelp.h"
#include "jni.h"

#include <gui/Surface.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/Log.h>
#include <utils/Looper.h>
#include <utils/NativeHandle.h>
#include <hardware/tv_input.h>

#define ATRACE_TAG ATRACE_TAG_VIDEO

#include <cutils/trace.h>
#include <utils/Trace.h>

namespace android {

static struct {
    jmethodID deviceAvailable;
    jmethodID deviceUnavailable;
    jmethodID streamConfigsChanged;
    jmethodID firstFrameCaptured;
} gTvInputHalClassInfo;

static struct {
    jclass clazz;
} gTvStreamConfigClassInfo;

static struct {
    jclass clazz;

    jmethodID constructor;
    jmethodID streamId;
    jmethodID type;
    jmethodID maxWidth;
    jmethodID maxHeight;
    jmethodID generation;
    jmethodID build;
} gTvStreamConfigBuilderClassInfo;

static struct {
    jclass clazz;

    jmethodID constructor;
    jmethodID deviceId;
    jmethodID type;
    jmethodID hdmiPortId;
    jmethodID audioType;
    jmethodID audioAddress;
    jmethodID build;
} gTvInputHardwareInfoBuilderClassInfo;

////////////////////////////////////////////////////////////////////////////////

class BufferProducerThread : public Thread {
public:
    BufferProducerThread(tv_input_device_t* device, int deviceId, const tv_stream_t* stream);

    virtual status_t readyToRun();

    void setSurface(const sp<Surface>& surface);
    void onCaptured(uint32_t seq, bool succeeded);
    void shutdown();

private:
    Mutex mLock;
    Condition mCondition;
    sp<Surface> mSurface;
    tv_input_device_t* mDevice;
    int mDeviceId;
    tv_stream_t mStream;
    int mHoldBufCount;

    enum  BufferState {
           CAPTURE,
           RELEASED,
       };

    struct BufferEntry {
       sp<ANativeWindowBuffer_t> buffer;
       enum BufferState bufferState;
       uint32_t seqNum;
    };

    struct BufferEntry* mBuffers;
    uint32_t mReleasedBufCnt;
    bool mStartUp;
    uint32_t mSeq;
    bool mShutdown;

    virtual bool threadLoop();

    void setSurfaceLocked(const sp<Surface>& surface);
    int findBufferIndex(uint32_t seq);
    int findBufferIndex(buffer_handle_t buffer);
};

BufferProducerThread::BufferProducerThread(
        tv_input_device_t* device, int deviceId, const tv_stream_t* stream)
    : Thread(false),
      mDevice(device),
      mDeviceId(deviceId),
      mHoldBufCount(0),
      mBuffers(NULL),
      mReleasedBufCnt(0),
      mStartUp(true),
      mSeq(0u),
      mShutdown(false) {
    memcpy(&mStream, stream, sizeof(mStream));
}

status_t BufferProducerThread::readyToRun() {
    sp<ANativeWindow> anw(mSurface);
    status_t err = native_window_set_usage(anw.get(), mStream.buffer_producer.usage);
    if (err != NO_ERROR) {
        return err;
    }
    err = native_window_set_buffers_dimensions(
            anw.get(), mStream.buffer_producer.width, mStream.buffer_producer.height);
    if (err != NO_ERROR) {
        return err;
    }
    err = native_window_set_scaling_mode(anw.get(), NATIVE_WINDOW_SCALING_MODE_SCALE_CROP);
    if (err != NO_ERROR) {
        return err;
    }
    err = native_window_set_buffers_format(anw.get(), mStream.buffer_producer.format);
    if (err != NO_ERROR) {
        return err;
    }
    err = native_window_set_buffer_count(anw.get(), mStream.buffer_producer.buffer_count);
    if (err != NO_ERROR) {
        return err;
    }
    err = anw->query(anw.get(), NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS, &mHoldBufCount);
    if (err != NO_ERROR) {
        return err;
    }

    for (uint32_t k = 0; k < mStream.buffer_producer.buffer_count; k++)
    {
        ANativeWindowBuffer_t* buffer = NULL;
        err = native_window_dequeue_buffer_and_wait(anw.get(), &buffer);
        if (err != NO_ERROR) {
            ALOGE("error %d while dequeueing buffer to surface", err);
            break;
        }
        mSeq++;
        mBuffers[k].buffer = buffer;
        mBuffers[k].bufferState = CAPTURE;
        mDevice->request_capture(mDevice, mDeviceId, mStream.stream_id,
                                    mBuffers[k].buffer->handle, mSeq);
        mBuffers[k].seqNum = mSeq;
    }
    return NO_ERROR;
}

void BufferProducerThread::setSurface(const sp<Surface>& surface) {
    Mutex::Autolock autoLock(&mLock);
    setSurfaceLocked(surface);
}

void BufferProducerThread::setSurfaceLocked(const sp<Surface>& surface) {
    if (surface == mSurface) {
        return;
    }

    if (NULL != surface.get()) {
        mBuffers = new struct BufferEntry[mStream.buffer_producer.buffer_count];
        if (!mBuffers){
            ALOGE("Buffers malloc failed");
            return;
        }
        else
        {
            for (uint32_t idx = 0; idx < mStream.buffer_producer.buffer_count; idx++)
            {
                mBuffers[idx].bufferState = RELEASED;
            }
        }
    }
    for (uint32_t idx = 0; idx < mStream.buffer_producer.buffer_count; idx++)
    {
       if (CAPTURE == mBuffers[idx].bufferState)
       {
           mDevice->cancel_capture(mDevice, mDeviceId, mStream.stream_id, mBuffers[idx].seqNum);

           while (CAPTURE == mBuffers[idx].bufferState)
           {
              status_t err = mCondition.waitRelative(mLock, s2ns(1));
              if (err != NO_ERROR) {
                  ALOGE("error %d while wating for buffer state to change.", err);
                  break;
              }
           }
           mBuffers[idx].bufferState = RELEASED;
           mBuffers[idx].buffer.clear();
       }
    }

    mSurface = surface;
    mCondition.broadcast();
}

void BufferProducerThread::onCaptured(uint32_t seq, bool succeeded) {
    Mutex::Autolock autoLock(&mLock);
    sp<ANativeWindow> anw(mSurface);

    sp<ANativeWindowBuffer_t> nwBuffer = NULL;
    int idx = findBufferIndex(seq);
    if (-1 == idx)
    {
       ALOGE("cannot find buffer with seq num = 0x%x in buffer list", seq);
       return;
    }
    if (mBuffers[idx].bufferState == CAPTURE)
    {
        mBuffers[idx].bufferState = RELEASED;
        if (succeeded) {
            nwBuffer = mBuffers[idx].buffer;

            ATRACE_INT("renderer enqueue buffer", 1);

            status_t err = anw->queueBuffer(anw.get(), nwBuffer.get(), -1);

            ATRACE_INT("renderer enqueue buffer", 0);

            if (err != NO_ERROR) {
                ALOGE("error %d while queueing buffer to surface", err);
            }
            else
            {
                mReleasedBufCnt++;
            }
        }
    }
    else
    {
       ALOGE("incorrect buffer state for buffer = 0x%x", nwBuffer->handle);
    }
    mCondition.broadcast();
}

void BufferProducerThread::shutdown() {
    mShutdown = true;
    {
        Mutex::Autolock autoLock(&mLock);
        setSurfaceLocked(NULL);
        mCondition.broadcast();
    }
    requestExitAndWait();
    if (mBuffers)
    {
        Mutex::Autolock autoLock(&mLock);

        for (uint32_t k = 0; k < mStream.buffer_producer.buffer_count; k++)
        {
           if (mBuffers[k].buffer.get())
           {
              mBuffers[k].buffer.clear();
           }
        }
        delete mBuffers;
        mBuffers = NULL;
    }
}

int BufferProducerThread::findBufferIndex(uint32_t seq)
{
   int index = -1;

   for (uint32_t k = 0; k < mStream.buffer_producer.buffer_count; k++)
   {
      if (mBuffers[k].seqNum == seq)
      {
         index = k;
         break;
      }
   }
   return index;
}

int BufferProducerThread::findBufferIndex(buffer_handle_t buffer)
{
   int index = -1;
   native_handle_t *h = (native_handle_t *)buffer;

   for (uint32_t k = 0; k < mStream.buffer_producer.buffer_count; k++)
   {
      if (((native_handle_t *)mBuffers[k].buffer->handle)->data[0] == h->data[0])
      {
         index = k;
         break;
      }
   }
   return index;
}

bool BufferProducerThread::threadLoop() {

    status_t err = NO_ERROR;

    if (mShutdown)
    {
        usleep(1000);
        return true;
    }
    else
    {
        {
            Mutex::Autolock autoLock(&mLock);

            if (mSurface == NULL) {
                err = mCondition.waitRelative(mLock, s2ns(1));
                // It's OK to time out here.
                if (err != NO_ERROR && err != TIMED_OUT) {
                    ALOGE("error %d while wating for non-null surface to be set", err);
                    return false;
                }
                return true;
            }

            if (err != NO_ERROR)
            {
                return false;
            }
        }

        if ((mReleasedBufCnt > (uint32_t)mHoldBufCount) && (!mShutdown))
        {
           ANativeWindowBuffer_t* buffer = NULL;
           sp<ANativeWindow> anw(mSurface);
           int idx = -1;

           ATRACE_INT("renderer dequeue buffer", 1);

           err = native_window_dequeue_buffer_and_wait(anw.get(), &buffer);
           if (err != NO_ERROR) {
                ATRACE_INT("renderer dequeue buffer", 0);
                ALOGE("error %d while dequeueing buffer to surface", err);
                usleep(1000);
                return true;
           }
           else
           {
               Mutex::Autolock autoLock(&mLock);

               if (!mShutdown)
               {
                   ATRACE_INT("renderer dequeue buffer", 0);

                   mReleasedBufCnt--;

                   idx = findBufferIndex(buffer->handle);
                   if (-1 == idx)
                   {
                      ALOGE("error can not find buffer handle 0x%x in buffer list", buffer->handle);
                      return false;
                   }

                   if (RELEASED == mBuffers[idx].bufferState)
                   {
                      mBuffers[idx].buffer = buffer;
                      mBuffers[idx].bufferState = CAPTURE;

                      mDevice->request_capture(mDevice, mDeviceId, mStream.stream_id,
                                                buffer->handle, ++mSeq);
                      mBuffers[idx].seqNum = mSeq;
                   }
                   else
                   {
                      ALOGE("error buffer state = 0x%x, but expected buffer state = 0x%x", mBuffers[idx].bufferState, RELEASED);
                      return false;
                   }
               }
               else
               {
                  sp<ANativeWindowBuffer_t> nwBuffer = NULL;

                  ATRACE_INT("renderer dequeue buffer", 0);

                  idx = findBufferIndex(buffer->handle);
                  nwBuffer = mBuffers[idx].buffer;
                  status_t err = anw->queueBuffer(anw.get(), nwBuffer.get(), -1);
                  if (err != NO_ERROR) {
                    ALOGE("error in buffer queue");
                  }
               }
            }
        }
    }

    return true;
}

////////////////////////////////////////////////////////////////////////////////

class JTvInputHal {
public:
    ~JTvInputHal();

    static JTvInputHal* createInstance(JNIEnv* env, jobject thiz, const sp<Looper>& looper);

    int addOrUpdateStream(int deviceId, int streamId, const sp<Surface>& surface);
    int removeStream(int deviceId, int streamId);
    const tv_stream_config_t* getStreamConfigs(int deviceId, int* numConfigs);

    void onDeviceAvailable(const tv_input_device_info_t& info);
    void onDeviceUnavailable(int deviceId);
    void onStreamConfigurationsChanged(int deviceId);
    void onCaptured(int deviceId, int streamId, uint32_t seq, bool succeeded);

private:
    // Connection between a surface and a stream.
    class Connection {
    public:
        Connection() {}

        sp<Surface> mSurface;
        tv_stream_type_t mStreamType;

        // Only valid when mStreamType == TV_STREAM_TYPE_INDEPENDENT_VIDEO_SOURCE
        sp<NativeHandle> mSourceHandle;
        // Only valid when mStreamType == TV_STREAM_TYPE_BUFFER_PRODUCER
        sp<BufferProducerThread> mThread;
    };

    class NotifyHandler : public MessageHandler {
    public:
        NotifyHandler(JTvInputHal* hal, const tv_input_event_t* event);
        ~NotifyHandler();

        virtual void handleMessage(const Message& message);

    private:
        tv_input_event_t mEvent;
        JTvInputHal* mHal;
    };

    JTvInputHal(JNIEnv* env, jobject thiz, tv_input_device_t* dev, const sp<Looper>& looper);

    static void notify(
            tv_input_device_t* dev, tv_input_event_t* event, void* data);

    static void cloneTvInputEvent(
            tv_input_event_t* dstEvent, const tv_input_event_t* srcEvent);

    Mutex mLock;
    jweak mThiz;
    tv_input_device_t* mDevice;
    tv_input_callback_ops_t mCallback;
    sp<Looper> mLooper;

    KeyedVector<int, KeyedVector<int, Connection> > mConnections;
};

JTvInputHal::JTvInputHal(JNIEnv* env, jobject thiz, tv_input_device_t* device,
        const sp<Looper>& looper) {
    mThiz = env->NewWeakGlobalRef(thiz);
    mDevice = device;
    mCallback.notify = &JTvInputHal::notify;
    mLooper = looper;

    mDevice->initialize(mDevice, &mCallback, this);
}

JTvInputHal::~JTvInputHal() {
    mDevice->common.close((hw_device_t*)mDevice);

    JNIEnv* env = AndroidRuntime::getJNIEnv();
    env->DeleteWeakGlobalRef(mThiz);
    mThiz = NULL;
}

JTvInputHal* JTvInputHal::createInstance(JNIEnv* env, jobject thiz, const sp<Looper>& looper) {
    tv_input_module_t* module = NULL;
    status_t err = hw_get_module(TV_INPUT_HARDWARE_MODULE_ID,
            (hw_module_t const**)&module);
    if (err) {
        ALOGE("Couldn't load %s module (%s)",
                TV_INPUT_HARDWARE_MODULE_ID, strerror(-err));
        return 0;
    }

    tv_input_device_t* device = NULL;
    err = module->common.methods->open(
            (hw_module_t*)module,
            TV_INPUT_DEFAULT_DEVICE,
            (hw_device_t**)&device);
    if (err) {
        ALOGE("Couldn't open %s device (%s)",
                TV_INPUT_DEFAULT_DEVICE, strerror(-err));
        return 0;
    }

    return new JTvInputHal(env, thiz, device, looper);
}

int JTvInputHal::addOrUpdateStream(int deviceId, int streamId, const sp<Surface>& surface) {
    KeyedVector<int, Connection>& connections = mConnections.editValueFor(deviceId);
    if (connections.indexOfKey(streamId) < 0) {
        connections.add(streamId, Connection());
    }
    Connection& connection = connections.editValueFor(streamId);
    if (connection.mSurface == surface) {
        // Nothing to do
        return NO_ERROR;
    }
    // Clear the surface in the connection.
    if (connection.mSurface != NULL) {
        if (connection.mStreamType == TV_STREAM_TYPE_INDEPENDENT_VIDEO_SOURCE) {
            if (Surface::isValid(connection.mSurface)) {
                connection.mSurface->setSidebandStream(NULL);
            }
        }
        connection.mSurface.clear();
    }
    if (connection.mSourceHandle == NULL && connection.mThread == NULL) {
        // Need to configure stream
        int numConfigs = 0;
        const tv_stream_config_t* configs = NULL;
        if (mDevice->get_stream_configurations(
                mDevice, deviceId, &numConfigs, &configs) != 0) {
            ALOGE("Couldn't get stream configs");
            return UNKNOWN_ERROR;
        }
        int configIndex = -1;
        for (int i = 0; i < numConfigs; ++i) {
            if (configs[i].stream_id == streamId) {
                configIndex = i;
                break;
            }
        }
        if (configIndex == -1) {
            ALOGE("Cannot find a config with given stream ID: %d", streamId);
            return BAD_VALUE;
        }
        connection.mStreamType = configs[configIndex].type;

        tv_stream_t stream;
        stream.stream_id = configs[configIndex].stream_id;
        if (connection.mStreamType == TV_STREAM_TYPE_BUFFER_PRODUCER) {
            stream.buffer_producer.width = configs[configIndex].max_video_width;
            stream.buffer_producer.height = configs[configIndex].max_video_height;
        }
        if (mDevice->open_stream(mDevice, deviceId, &stream) != 0) {
            ALOGE("Couldn't add stream");
            return UNKNOWN_ERROR;
        }
        if (connection.mStreamType == TV_STREAM_TYPE_INDEPENDENT_VIDEO_SOURCE) {
            connection.mSourceHandle = NativeHandle::create(
                    stream.sideband_stream_source_handle, false);
        } else if (connection.mStreamType == TV_STREAM_TYPE_BUFFER_PRODUCER) {
            if (connection.mThread != NULL) {
                connection.mThread->shutdown();
            }
            connection.mThread = new BufferProducerThread(mDevice, deviceId, &stream);
        }
    }
    connection.mSurface = surface;
    if (connection.mStreamType == TV_STREAM_TYPE_INDEPENDENT_VIDEO_SOURCE) {
        connection.mSurface->setSidebandStream(connection.mSourceHandle);
    } else if (connection.mStreamType == TV_STREAM_TYPE_BUFFER_PRODUCER) {
        connection.mThread->setSurface(surface);
        connection.mThread->run();
    }
    return NO_ERROR;
}

int JTvInputHal::removeStream(int deviceId, int streamId) {
    KeyedVector<int, Connection>& connections = mConnections.editValueFor(deviceId);
    if (connections.indexOfKey(streamId) < 0) {
        return BAD_VALUE;
    }
    Connection& connection = connections.editValueFor(streamId);
    if (connection.mSurface == NULL) {
        // Nothing to do
        return NO_ERROR;
    }
    if (Surface::isValid(connection.mSurface)) {
        connection.mSurface.clear();
    }
    if (connection.mSurface != NULL) {
        connection.mSurface->setSidebandStream(NULL);
        connection.mSurface.clear();
    }
    if (connection.mThread != NULL) {
        connection.mThread->shutdown();
        connection.mThread.clear();
    }
    if (mDevice->close_stream(mDevice, deviceId, streamId) != 0) {
        ALOGE("Couldn't remove stream");
        return BAD_VALUE;
    }
    if (connection.mSourceHandle != NULL) {
        connection.mSourceHandle.clear();
    }
    return NO_ERROR;
}

const tv_stream_config_t* JTvInputHal::getStreamConfigs(int deviceId, int* numConfigs) {
    const tv_stream_config_t* configs = NULL;
    if (mDevice->get_stream_configurations(
            mDevice, deviceId, numConfigs, &configs) != 0) {
        ALOGE("Couldn't get stream configs");
        return NULL;
    }
    return configs;
}

// static
void JTvInputHal::notify(
        tv_input_device_t* dev, tv_input_event_t* event, void* data) {
    JTvInputHal* thiz = (JTvInputHal*)data;
    thiz->mLooper->sendMessage(new NotifyHandler(thiz, event), event->type);
}

// static
void JTvInputHal::cloneTvInputEvent(
        tv_input_event_t* dstEvent, const tv_input_event_t* srcEvent) {
    memcpy(dstEvent, srcEvent, sizeof(tv_input_event_t));
    if ((srcEvent->type == TV_INPUT_EVENT_DEVICE_AVAILABLE ||
            srcEvent->type == TV_INPUT_EVENT_DEVICE_UNAVAILABLE ||
            srcEvent->type == TV_INPUT_EVENT_STREAM_CONFIGURATIONS_CHANGED) &&
            srcEvent->device_info.audio_address != NULL){
        char* audio_address = new char[strlen(srcEvent->device_info.audio_address) + 1];
        strcpy(audio_address, srcEvent->device_info.audio_address);
        dstEvent->device_info.audio_address = audio_address;
    }
}

void JTvInputHal::onDeviceAvailable(const tv_input_device_info_t& info) {
    {
        Mutex::Autolock autoLock(&mLock);
        mConnections.add(info.device_id, KeyedVector<int, Connection>());
    }
    JNIEnv* env = AndroidRuntime::getJNIEnv();

    jobject builder = env->NewObject(
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            gTvInputHardwareInfoBuilderClassInfo.constructor);
    env->CallObjectMethod(
            builder, gTvInputHardwareInfoBuilderClassInfo.deviceId, info.device_id);
    env->CallObjectMethod(
            builder, gTvInputHardwareInfoBuilderClassInfo.type, info.type);
    if (info.type == TV_INPUT_TYPE_HDMI) {
        env->CallObjectMethod(
                builder, gTvInputHardwareInfoBuilderClassInfo.hdmiPortId, info.hdmi.port_id);
    }
    env->CallObjectMethod(
            builder, gTvInputHardwareInfoBuilderClassInfo.audioType, info.audio_type);
    if (info.audio_type != AUDIO_DEVICE_NONE) {
        jstring audioAddress = env->NewStringUTF(info.audio_address);
        env->CallObjectMethod(
                builder, gTvInputHardwareInfoBuilderClassInfo.audioAddress, audioAddress);
        env->DeleteLocalRef(audioAddress);
    }

    jobject infoObject = env->CallObjectMethod(builder, gTvInputHardwareInfoBuilderClassInfo.build);

    env->CallVoidMethod(
            mThiz,
            gTvInputHalClassInfo.deviceAvailable,
            infoObject);

    env->DeleteLocalRef(builder);
    env->DeleteLocalRef(infoObject);
}

void JTvInputHal::onDeviceUnavailable(int deviceId) {
    {
        Mutex::Autolock autoLock(&mLock);
        KeyedVector<int, Connection>& connections = mConnections.editValueFor(deviceId);
        for (size_t i = 0; i < connections.size(); ++i) {
            removeStream(deviceId, connections.keyAt(i));
        }
        connections.clear();
        mConnections.removeItem(deviceId);
    }
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    env->CallVoidMethod(
            mThiz,
            gTvInputHalClassInfo.deviceUnavailable,
            deviceId);
}

void JTvInputHal::onStreamConfigurationsChanged(int deviceId) {
    {
        Mutex::Autolock autoLock(&mLock);
        KeyedVector<int, Connection>& connections = mConnections.editValueFor(deviceId);
        for (size_t i = 0; i < connections.size(); ++i) {
            removeStream(deviceId, connections.keyAt(i));
        }
        connections.clear();
    }
    JNIEnv* env = AndroidRuntime::getJNIEnv();
    env->CallVoidMethod(
            mThiz,
            gTvInputHalClassInfo.streamConfigsChanged,
            deviceId);
}

void JTvInputHal::onCaptured(int deviceId, int streamId, uint32_t seq, bool succeeded) {
    sp<BufferProducerThread> thread;
    {
        Mutex::Autolock autoLock(&mLock);
        KeyedVector<int, Connection>& connections = mConnections.editValueFor(deviceId);
        Connection& connection = connections.editValueFor(streamId);
        if (connection.mThread == NULL) {
            ALOGE("capture thread not existing.");
            return;
        }
        thread = connection.mThread;
    }
    thread->onCaptured(seq, succeeded);
    if (seq == 0) {
        JNIEnv* env = AndroidRuntime::getJNIEnv();
        env->CallVoidMethod(
                mThiz,
                gTvInputHalClassInfo.firstFrameCaptured,
                deviceId,
                streamId);
    }
}

JTvInputHal::NotifyHandler::NotifyHandler(JTvInputHal* hal, const tv_input_event_t* event) {
    mHal = hal;
    cloneTvInputEvent(&mEvent, event);
}

JTvInputHal::NotifyHandler::~NotifyHandler() {
    if ((mEvent.type == TV_INPUT_EVENT_DEVICE_AVAILABLE ||
            mEvent.type == TV_INPUT_EVENT_DEVICE_UNAVAILABLE ||
            mEvent.type == TV_INPUT_EVENT_STREAM_CONFIGURATIONS_CHANGED) &&
            mEvent.device_info.audio_address != NULL) {
        delete mEvent.device_info.audio_address;
    }
}

void JTvInputHal::NotifyHandler::handleMessage(const Message& message) {
    switch (mEvent.type) {
        case TV_INPUT_EVENT_DEVICE_AVAILABLE: {
            mHal->onDeviceAvailable(mEvent.device_info);
        } break;
        case TV_INPUT_EVENT_DEVICE_UNAVAILABLE: {
            mHal->onDeviceUnavailable(mEvent.device_info.device_id);
        } break;
        case TV_INPUT_EVENT_STREAM_CONFIGURATIONS_CHANGED: {
            mHal->onStreamConfigurationsChanged(mEvent.device_info.device_id);
        } break;
        case TV_INPUT_EVENT_CAPTURE_SUCCEEDED: {
            mHal->onCaptured(mEvent.capture_result.device_id,
                             mEvent.capture_result.stream_id,
                             mEvent.capture_result.seq,
                             true /* succeeded */);
        } break;
        case TV_INPUT_EVENT_CAPTURE_FAILED: {
            mHal->onCaptured(mEvent.capture_result.device_id,
                             mEvent.capture_result.stream_id,
                             mEvent.capture_result.seq,
                             false /* succeeded */);
        } break;
        default:
            ALOGE("Unrecognizable event");
    }
}

////////////////////////////////////////////////////////////////////////////////

static jlong nativeOpen(JNIEnv* env, jobject thiz, jobject messageQueueObj) {
    sp<MessageQueue> messageQueue =
            android_os_MessageQueue_getMessageQueue(env, messageQueueObj);
    return (jlong)JTvInputHal::createInstance(env, thiz, messageQueue->getLooper());
}

static int nativeAddOrUpdateStream(JNIEnv* env, jclass clazz,
        jlong ptr, jint deviceId, jint streamId, jobject jsurface) {
    JTvInputHal* tvInputHal = (JTvInputHal*)ptr;
    if (!jsurface) {
        return BAD_VALUE;
    }
    sp<Surface> surface(android_view_Surface_getSurface(env, jsurface));
    return tvInputHal->addOrUpdateStream(deviceId, streamId, surface);
}

static int nativeRemoveStream(JNIEnv* env, jclass clazz,
        jlong ptr, jint deviceId, jint streamId) {
    JTvInputHal* tvInputHal = (JTvInputHal*)ptr;
    return tvInputHal->removeStream(deviceId, streamId);
}

static jobjectArray nativeGetStreamConfigs(JNIEnv* env, jclass clazz,
        jlong ptr, jint deviceId, jint generation) {
    JTvInputHal* tvInputHal = (JTvInputHal*)ptr;
    int numConfigs = 0;
    const tv_stream_config_t* configs = tvInputHal->getStreamConfigs(deviceId, &numConfigs);

    jobjectArray result = env->NewObjectArray(numConfigs, gTvStreamConfigClassInfo.clazz, NULL);
    for (int i = 0; i < numConfigs; ++i) {
        jobject builder = env->NewObject(
                gTvStreamConfigBuilderClassInfo.clazz,
                gTvStreamConfigBuilderClassInfo.constructor);
        env->CallObjectMethod(
                builder, gTvStreamConfigBuilderClassInfo.streamId, configs[i].stream_id);
        env->CallObjectMethod(
                builder, gTvStreamConfigBuilderClassInfo.type, configs[i].type);
        env->CallObjectMethod(
                builder, gTvStreamConfigBuilderClassInfo.maxWidth, configs[i].max_video_width);
        env->CallObjectMethod(
                builder, gTvStreamConfigBuilderClassInfo.maxHeight, configs[i].max_video_height);
        env->CallObjectMethod(
                builder, gTvStreamConfigBuilderClassInfo.generation, generation);

        jobject config = env->CallObjectMethod(builder, gTvStreamConfigBuilderClassInfo.build);

        env->SetObjectArrayElement(result, i, config);

        env->DeleteLocalRef(config);
        env->DeleteLocalRef(builder);
    }
    return result;
}

static void nativeClose(JNIEnv* env, jclass clazz, jlong ptr) {
    JTvInputHal* tvInputHal = (JTvInputHal*)ptr;
    delete tvInputHal;
}

static JNINativeMethod gTvInputHalMethods[] = {
    /* name, signature, funcPtr */
    { "nativeOpen", "(Landroid/os/MessageQueue;)J",
            (void*) nativeOpen },
    { "nativeAddOrUpdateStream", "(JIILandroid/view/Surface;)I",
            (void*) nativeAddOrUpdateStream },
    { "nativeRemoveStream", "(JII)I",
            (void*) nativeRemoveStream },
    { "nativeGetStreamConfigs", "(JII)[Landroid/media/tv/TvStreamConfig;",
            (void*) nativeGetStreamConfigs },
    { "nativeClose", "(J)V",
            (void*) nativeClose },
};

#define FIND_CLASS(var, className) \
        var = env->FindClass(className); \
        LOG_FATAL_IF(! var, "Unable to find class " className)

#define GET_METHOD_ID(var, clazz, methodName, fieldDescriptor) \
        var = env->GetMethodID(clazz, methodName, fieldDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find method" methodName)

int register_android_server_tv_TvInputHal(JNIEnv* env) {
    int res = jniRegisterNativeMethods(env, "com/android/server/tv/TvInputHal",
            gTvInputHalMethods, NELEM(gTvInputHalMethods));
    LOG_FATAL_IF(res < 0, "Unable to register native methods.");

    jclass clazz;
    FIND_CLASS(clazz, "com/android/server/tv/TvInputHal");

    GET_METHOD_ID(
            gTvInputHalClassInfo.deviceAvailable, clazz,
            "deviceAvailableFromNative", "(Landroid/media/tv/TvInputHardwareInfo;)V");
    GET_METHOD_ID(
            gTvInputHalClassInfo.deviceUnavailable, clazz, "deviceUnavailableFromNative", "(I)V");
    GET_METHOD_ID(
            gTvInputHalClassInfo.streamConfigsChanged, clazz,
            "streamConfigsChangedFromNative", "(I)V");
    GET_METHOD_ID(
            gTvInputHalClassInfo.firstFrameCaptured, clazz,
            "firstFrameCapturedFromNative", "(II)V");

    FIND_CLASS(gTvStreamConfigClassInfo.clazz, "android/media/tv/TvStreamConfig");
    gTvStreamConfigClassInfo.clazz = jclass(env->NewGlobalRef(gTvStreamConfigClassInfo.clazz));

    FIND_CLASS(gTvStreamConfigBuilderClassInfo.clazz, "android/media/tv/TvStreamConfig$Builder");
    gTvStreamConfigBuilderClassInfo.clazz =
            jclass(env->NewGlobalRef(gTvStreamConfigBuilderClassInfo.clazz));

    GET_METHOD_ID(
            gTvStreamConfigBuilderClassInfo.constructor,
            gTvStreamConfigBuilderClassInfo.clazz,
            "<init>", "()V");
    GET_METHOD_ID(
            gTvStreamConfigBuilderClassInfo.streamId,
            gTvStreamConfigBuilderClassInfo.clazz,
            "streamId", "(I)Landroid/media/tv/TvStreamConfig$Builder;");
    GET_METHOD_ID(
            gTvStreamConfigBuilderClassInfo.type,
            gTvStreamConfigBuilderClassInfo.clazz,
            "type", "(I)Landroid/media/tv/TvStreamConfig$Builder;");
    GET_METHOD_ID(
            gTvStreamConfigBuilderClassInfo.maxWidth,
            gTvStreamConfigBuilderClassInfo.clazz,
            "maxWidth", "(I)Landroid/media/tv/TvStreamConfig$Builder;");
    GET_METHOD_ID(
            gTvStreamConfigBuilderClassInfo.maxHeight,
            gTvStreamConfigBuilderClassInfo.clazz,
            "maxHeight", "(I)Landroid/media/tv/TvStreamConfig$Builder;");
    GET_METHOD_ID(
            gTvStreamConfigBuilderClassInfo.generation,
            gTvStreamConfigBuilderClassInfo.clazz,
            "generation", "(I)Landroid/media/tv/TvStreamConfig$Builder;");
    GET_METHOD_ID(
            gTvStreamConfigBuilderClassInfo.build,
            gTvStreamConfigBuilderClassInfo.clazz,
            "build", "()Landroid/media/tv/TvStreamConfig;");

    FIND_CLASS(gTvInputHardwareInfoBuilderClassInfo.clazz,
            "android/media/tv/TvInputHardwareInfo$Builder");
    gTvInputHardwareInfoBuilderClassInfo.clazz =
            jclass(env->NewGlobalRef(gTvInputHardwareInfoBuilderClassInfo.clazz));

    GET_METHOD_ID(
            gTvInputHardwareInfoBuilderClassInfo.constructor,
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            "<init>", "()V");
    GET_METHOD_ID(
            gTvInputHardwareInfoBuilderClassInfo.deviceId,
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            "deviceId", "(I)Landroid/media/tv/TvInputHardwareInfo$Builder;");
    GET_METHOD_ID(
            gTvInputHardwareInfoBuilderClassInfo.type,
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            "type", "(I)Landroid/media/tv/TvInputHardwareInfo$Builder;");
    GET_METHOD_ID(
            gTvInputHardwareInfoBuilderClassInfo.hdmiPortId,
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            "hdmiPortId", "(I)Landroid/media/tv/TvInputHardwareInfo$Builder;");
    GET_METHOD_ID(
            gTvInputHardwareInfoBuilderClassInfo.audioType,
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            "audioType", "(I)Landroid/media/tv/TvInputHardwareInfo$Builder;");
    GET_METHOD_ID(
            gTvInputHardwareInfoBuilderClassInfo.audioAddress,
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            "audioAddress", "(Ljava/lang/String;)Landroid/media/tv/TvInputHardwareInfo$Builder;");
    GET_METHOD_ID(
            gTvInputHardwareInfoBuilderClassInfo.build,
            gTvInputHardwareInfoBuilderClassInfo.clazz,
            "build", "()Landroid/media/tv/TvInputHardwareInfo;");

    return 0;
}

} /* namespace android */
