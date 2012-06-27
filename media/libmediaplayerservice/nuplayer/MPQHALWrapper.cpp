/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (c) 2012, Code Aurora Forum. All rights reserved.
 *
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
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


#define LOG_NDEBUG 0
#define LOG_TAG "MPQHALWrapper"
#include <utils/Log.h>

//#include <media/stagefright/MPQHALWrapper.h>

#include "MPQHALWrapper.h"

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/ACodec.h>
#include <media/stagefright/MediaDefs.h>

#define MAX_MPQ_HAL_BUFFER_SIZE 64000
#define MPQ_AUDIO_SESSION_ID 3

namespace android {

NuPlayer::MPQHALWrapper::MPQHALWrapperClient::MPQHALWrapperClient(void *obj)
{
    LOGD("MPQAudioPlayer::MPQHALWrapperClient - Constructor");
    pBaseClass = (NuPlayer::MPQHALWrapper*)obj;
}


//TO-DO :: might not be needed, can be removed
void NuPlayer::MPQHALWrapper::MPQHALWrapperClient::binderDied(const wp<IBinder>& who) {
    Mutex::Autolock _l(pBaseClass->mAudioFlingerLock);

    pBaseClass->mAudioFlinger.clear();
    LOGW("AudioFlinger server died!");
}


//TO-DO :: might not be needed, can be removed
void NuPlayer::MPQHALWrapper::MPQHALWrapperClient::ioConfigChanged(int event, int ioHandle, void *param2) {
    LOGV("ioConfigChanged() event %d", event);

    if (event != AudioSystem::A2DP_OUTPUT_STATE) {
        return;
    }

    switch ( event ) {
    case AudioSystem::A2DP_OUTPUT_STATE:
        break;
    default:
        break;
    }
    LOGV("ioConfigChanged Out");
}


NuPlayer::MPQHALWrapper::MPQHALWrapper(const sp<MediaPlayerBase::AudioSink> &sink, sp<NuPlayer::Renderer> Renderer) {
    LOGE("@@@@:: MPQHALWrapper created");
    mAudioSink = sink;
    mNotify = NULL;
    mSampleRate = 0;
    mNumChannels = 0;
    mInputBufferSize = MAX_MPQ_HAL_BUFFER_SIZE;
    mRenderer = Renderer;
    mAudioBuffer = NULL;
	mLocalBuf = NULL;
}

NuPlayer::MPQHALWrapper::~MPQHALWrapper() {
    mAudioSink = NULL;
    mNotify = NULL;
    mRenderer = NULL;
    mLocalBuf = NULL;
    mAudioBuffer = NULL;
}

void NuPlayer::MPQHALWrapper::setNotificationMessage(const sp<AMessage> &msg) {
    mNotify = msg;
}

void NuPlayer::MPQHALWrapper::initiateSetup(const sp<AMessage> &msg) {
    msg->setWhat(kWhatSetup);
    msg->setTarget(id());
    msg->post();
}

void NuPlayer::MPQHALWrapper::signalFlush() {
    LOGV("signalFlush");
    (new AMessage(kWhatFlush, id()))->post();
}

void NuPlayer::MPQHALWrapper::signalResume() {
    LOGV("signalResume");
    (new AMessage(kWhatResume, id()))->post();
}

void NuPlayer::MPQHALWrapper::initiateShutdown() {
    LOGV("initiateShutdown");
    (new AMessage(kWhatShutdown, id()))->post();
}

void NuPlayer::MPQHALWrapper::onMessageReceived(const sp<AMessage> &msg) {

    switch (msg->what()) {
        case kWhatSetup: {
            onSetup(msg);
            break;
        }

        case kWhatShutdown: {
            onShutDown(msg);
            break;
        }

        case kWhatFlush: {
            onFlush(msg);
            break;
        }

        case kWhatInputBufferFilled: {
            onInputBufferFilled(msg);
            break;
        }

        case kWhatOutputBufferDrained: {
            onOutputBufferDrained(msg);
            break;
        }

        default:
            break;
    }
}


status_t NuPlayer::MPQHALWrapper::getAudioFlinger() {
    Mutex::Autolock _l(mAudioFlingerLock);
    status_t retVal = UNKNOWN_ERROR;
    if ( mAudioFlinger.get() == 0 ) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder = NULL;
        int retryCount = 0;
        do {
            binder = sm->getService(String16("media.audio_flinger"));
            if ( binder != 0 )
                break;
            LOGW("AudioFlinger not published, waiting...");
            usleep(100000); // 0.1 s
        } while ( retryCount++ < 50 ); //waiting for max 5 sec

        if(binder != NULL) {
            if ( mAudioFlingerClient == NULL ) {
                mAudioFlingerClient = new MPQHALWrapperClient(this);
            }
                binder->linkToDeath(mAudioFlingerClient);
                mAudioFlinger = interface_cast<IAudioFlinger>(binder);
                retVal = OK;
        }else {
            LOGE("Not able to get binder handle, return error");
        }
    }
    return retVal;
}

void NuPlayer::MPQHALWrapper::onSetup(const sp<AMessage> &msg) {
    // --- Do what you need to do when Player asks for SetUp ----
    AString mime;
    CHECK(msg->findString("mime", &mime));
    if (!strcasecmp(mime.c_str(), MEDIA_MIMETYPE_AUDIO_AAC)) {
        CHECK(msg->findInt32("channel-count", &mNumChannels));
        CHECK(msg->findInt32("sample-rate", &mSampleRate));
        mAudioFormat = AUDIO_FORMAT_AAC;
    }else if(!strcasecmp(mime.c_str(), MEDIA_MIMETYPE_AUDIO_RAW)) {
        int32_t numChannels = 0;
        CHECK(msg->findInt32("channel-count", &mNumChannels));
        CHECK(msg->findInt32("sample-rate", &mSampleRate));
        mAudioFormat = AUDIO_FORMAT_PCM_16_BIT;
    }else if (!strcasecmp(mime.c_str(), MEDIA_MIMETYPE_AUDIO_AC3)) {
        int32_t numChannels = 0, sampleRate = 0;
        CHECK(msg->findInt32("channel-count", &mNumChannels));
        CHECK(msg->findInt32("sample-rate", &mSampleRate));
        mAudioFormat = AUDIO_FORMAT_AC3;
    }else {
        LOGE("Unsupported Audio Format");
        CHECK(false);
    }

    status_t retVal = getAudioFlinger();

    CHECK(retVal == OK);
    // set up the MPQHAL here and wait for it to be initialized .... all sync call

    mAudioFlinger->registerClient(mAudioFlingerClient);
    LOGD("Opening a routing session for audio playback:\
            mSampleRate = %d mNumChannels =  %d",\
            mSampleRate, mNumChannels);

    retVal = mAudioSink->openSession(
            mAudioFormat, MPQ_AUDIO_SESSION_ID, mSampleRate, mNumChannels);

    mPCMStream = mAudioFlinger->getOutputSession();

    // check if mPCMStream is Valid or not
    CHECK(mPCMStream != NULL);
    mInputBufferSize = mPCMStream->common.get_buffer_size(&mPCMStream->common);

    LOGE("@@@@:: posting FORMAT Change Indication to Player, Format RAW, Channel(%d) mSampleRate(%d)",
        mNumChannels,mSampleRate);
	//Needed for APQ HAL not for MPQ HAL 
    //postAudioChangeEvent();

    LOGE("@@@@:: Allocation Audio Buffer of size %d", mInputBufferSize);
    //Allocate Buffer
    mLocalBuf = new ABuffer(mInputBufferSize);

    CHECK(mLocalBuf != NULL);

    // Once initialized, send request for buffer
    postFillThisBuffer();
    postFillThisBuffer();
    postFillThisBuffer();
}

//Needed for APQ HAL 
void NuPlayer::MPQHALWrapper::postAudioChangeEvent() {
    //--- Post Audio Change event, so that player can do open on Audio Sink
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatOutputFormatChanged);
    notify->setString("mime", MEDIA_MIMETYPE_AUDIO_RAW);
    notify->setInt32("channel-count", mNumChannels);
    notify->setInt32("sample-rate", mSampleRate);
    notify->post();
}

void NuPlayer::MPQHALWrapper::postFillThisBuffer() {
    LOGE("@@@@:: PostFillThisBuffer");
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", ACodec::kWhatFillThisBuffer);

    mLocalBuf->meta()->clear();
    memset(mLocalBuf->data(),0,mLocalBuf->size());
    notify->setObject("buffer", mLocalBuf);

    sp<AMessage> reply = new AMessage(kWhatInputBufferFilled, id());
    notify->setMessage("reply", reply);
    notify->post();
}

void NuPlayer::MPQHALWrapper::onShutDown(const sp<AMessage> &msg) {
    // Then send the response back once done
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatShutdownCompleted);
    notify->post();
}

void NuPlayer::MPQHALWrapper::onFlush(const sp<AMessage> &msg) {
    // Then send the response back once done
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatFlushCompleted);
    notify->post();
}

void NuPlayer::MPQHALWrapper::onOutputBufferDrained(const sp<AMessage> &msg) {
    //Do Nothing
    LOGE("@@@@:: onOutputBufferDrained Received ");
}

void NuPlayer::MPQHALWrapper::onInputBufferFilled(const sp<AMessage> &msg) {
    LOGE("@@@@:: onInputBufferFilled");
    // request for next buffer before processing the current buffer
    postFillThisBuffer();

    sp<RefBase> obj;
    int32_t err = OK;

    //Extract the buffer from Message
    if (!msg->findObject("buffer", &obj)) {
        CHECK(msg->findInt32("err", &err));

        LOGV("saw error %d instead of an input buffer", err);
        obj.clear();
    }
    sp<ABuffer> buffer = static_cast<ABuffer *>(obj.get());


    int64_t mediaTimeUs = 0;
    CHECK(buffer->meta()->findInt64("timeUs", &mediaTimeUs));
    LOGE("@@@@:: I/P audio buffer @ media time %.2f secs recived", mediaTimeUs / 1E6);

#if 0
    mAudioSink->write((void*)buffer->data(),buffer->size());
    //use Audio sink directly

    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", ACodec::kWhatDrainThisBuffer);
    notify->setObject("buffer", buffer);

    sp<AMessage> reply = new AMessage(kWhatOutputBufferDrained, id());
    notify->setMessage("reply", reply);
    notify->post();
#endif

    //Write the buffer to the HAL layer
    writeDataToAudioRenderer(buffer);
}

#if 0
void NuPlayer::MPQHALWrapper::writeDataToAudioRenderer(sp<ABuffer> buffer) {
    if(mAudioBuffer == NULL) {
        mAudioBuffer = new ABuffer(MAX_MPQ_HAL_BUFFER_SIZE);
        mAudioBuffer->setRange(0,0);
    }

    size_t offset=0, length = 0;
    if(buffer != NULL) {
        //copy the content to the original buffer
        offset = mAudioBuffer->offset();
        length = mAudioBuffer->size();

        memcpy(mAudioBuffer->data()+length,buffer->data(),buffer->size());
        length += buffer->size();
        //set the offset after writing the data
        mAudioBuffer->setRange(offset,length);

        LOGE("writeDataToAudioRenderer:: IN:: CurrOffset(%d) CurrLength(%d) -- dataReceived(%d)",mAudioBuffer->offset(),mAudioBuffer->size(),buffer->size());

        //if size is more than what is need to be written to HAL, write it now
        while(length >= mInputBufferSize) {
            ssize_t bytesWritten = mPCMStream->write(mPCMStream,(void*)mAudioBuffer->data(), mInputBufferSize);
            offset +=  bytesWritten;
            length -=  bytesWritten;
            LOGE("writeDataToAudioRenderer:: Data written to HAL (%d)",bytesWritten);
            //set the offset after writing the data
            mAudioBuffer->setRange(offset,length);
        }
        LOGE("writeDataToAudioRenderer:: OUT:: CurrOffset(%d) CurrLength(%d)",mAudioBuffer->offset(),mAudioBuffer->size());
    }

    if(mAudioBuffer->size() == 0) {
        //reset the range to the begning of start
        mAudioBuffer->setRange(0,0);
        LOGE("writeDataToAudioRenderer:: ==================> resetting Size and Offset to (0,0)");
    }
}
#endif

void NuPlayer::MPQHALWrapper::writeDataToAudioRenderer(sp<ABuffer> buffer) {

    if(buffer != NULL && buffer->size() > 0) {
        ssize_t bytesWritten = mPCMStream->write(mPCMStream,buffer->data(), buffer->size());
        char* dataRaw = (char*)buffer->data();
        LOGE("writeDataToAudioRenderer:: Data written to HAL (%d) --- %x %x %x %x ",bytesWritten,dataRaw[0],dataRaw[1],dataRaw[2],dataRaw[3] );
    }
}

} // namespace android
