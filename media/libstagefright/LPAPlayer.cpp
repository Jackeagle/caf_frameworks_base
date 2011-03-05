/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (c) 2009-2011, Code Aurora Forum. All rights reserved.
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
#define LOG_TAG "LPAPlayer"
#include <utils/Log.h>
#include <utils/threads.h>

#include <fcntl.h>
#include <sys/prctl.h>
#include <sys/resource.h>

#include <binder/IPCThreadState.h>
#include <media/AudioTrack.h>

#include <media/stagefright/LPAPlayer.h>
#include <media/stagefright/MediaDebug.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/MediaErrors.h>

#include <linux/unistd.h>
#include <linux/msm_audio.h>

#include "include/AwesomePlayer.h"

#define PMEM_BUFFER_SIZE 524288
#define PMEM_BUFFER_COUNT 4

namespace android {
int LPAPlayer::objectsAlive = 0;

LPAPlayer::LPAPlayer(
                    const sp<MediaPlayerBase::AudioSink> &audioSink,
                    AwesomePlayer *observer)
:mInputBuffer(NULL),
mSampleRate(0),
mLatencyUs(0),
mFrameSize(0),
mNumFramesPlayed(0),
mPositionTimeMediaUs(-1),
mPositionTimeRealUs(-1),
mSeeking(false),
mReachedEOS(false),
mFinalStatus(OK),
mStarted(false),
mIsFirstBuffer(false),
mFirstBufferResult(OK),
mFirstBuffer(NULL),
mAudioSink(audioSink),
mObserver(observer),
AudioPlayer(audioSink,observer) {
    a2dpDisconnectPause = false;
    mSeeked = false;
    objectsAlive++;
    timeStarted = 0;
    numChannels =0;
    afd = -1;
    timePlayed = 0;
    isPaused = false;
    bIsA2DPEnabled = false;
    mAudioFlinger = NULL;
    AudioFlingerClient = NULL;
    LOGV("Opening pcm_dec driver");
    afd = open("/dev/msm_pcm_lp_dec", O_WRONLY | O_NONBLOCK);
    mSourceEmpty = true;
    if ( afd < 0 ) {
        LOGE("pcm_lp_dec: cannot open pcm_dec device and the error is %d", errno);
        return;
    } else {
        LOGV("pcm_lp_dec: pcm_lp_dec Driver opened");
    }
    getAudioFlinger();
    mAudioFlinger->registerClient(AudioFlingerClient);
    mAudioSinkOpen = false;
    a2dpThreadStarted = true;
    asyncReset = false;
}

LPAPlayer::~LPAPlayer() {
    if (mStarted) {
        reset();
    }
    objectsAlive--;
}

void LPAPlayer::getAudioFlinger() {
    Mutex::Autolock _l(AudioFlingerLock);

    if ( mAudioFlinger.get() == 0 ) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder;
        do {
            binder = sm->getService(String16("media.audio_flinger"));
            if ( binder != 0 )
                break;
            LOGW("AudioFlinger not published, waiting...");
            usleep(500000); // 0.5 s
        } while ( true );
        if ( AudioFlingerClient == NULL ) {
            AudioFlingerClient = new AudioFlingerLPAdecodeClient(this);
        }

        binder->linkToDeath(AudioFlingerClient);
        mAudioFlinger = interface_cast<IAudioFlinger>(binder);
    }
    LOGE_IF(mAudioFlinger==0, "no AudioFlinger!?");
}

LPAPlayer::AudioFlingerLPAdecodeClient::AudioFlingerLPAdecodeClient(void *obj)
{
    LOGV("LPAPlayer::AudioFlingerLPAdecodeClient::AudioFlingerLPAdecodeClient");
    pBaseClass = (LPAPlayer*)obj;
}

void LPAPlayer::AudioFlingerLPAdecodeClient::binderDied(const wp<IBinder>& who) {
    Mutex::Autolock _l(pBaseClass->AudioFlingerLock);

    pBaseClass->mAudioFlinger.clear();
    LOGW("AudioFlinger server died!");
}

void LPAPlayer::AudioFlingerLPAdecodeClient::ioConfigChanged(int event, int ioHandle, void *param2) {
    LOGV("ioConfigChanged() event %d", event);

    if ( event != AudioSystem::A2DP_OUTPUT_STATE ) {
        return;
    }

    switch ( event ) {
    case AudioSystem::A2DP_OUTPUT_STATE:
        {
            LOGV("ioConfigChanged() A2DP_OUTPUT_STATE iohandle is %d with A2DPEnabled in %d", ioHandle, pBaseClass->bIsA2DPEnabled);

            if ( -1 == ioHandle ) {
                if ( pBaseClass->bIsA2DPEnabled ) {
                    pBaseClass->bIsA2DPEnabled = false;
                    if (pBaseClass->mStarted) {
                        pBaseClass->handleA2DPSwitch();
                    }
                    LOGV("ioConfigChanged:: A2DP Disabled");
                }
            } else {
                if ( !pBaseClass->bIsA2DPEnabled ) {

                    pBaseClass->bIsA2DPEnabled = true;
                    if (pBaseClass->mStarted) {
                        pBaseClass->handleA2DPSwitch();
                    }

                    LOGV("ioConfigChanged:: A2DP Enabled");

                }
            }
        }
        break;
    }

    LOGV("ioConfigChanged Out");
}

void LPAPlayer::handleA2DPSwitch() {
    Mutex::Autolock autoLock(mLock);

    LOGV("handleA2dpSwitch()");
    if (bIsA2DPEnabled) {
        if (!isPaused) {
            if (ioctl(afd, AUDIO_PAUSE, 1) < 0) {
                LOGE("AUDIO PAUSE failed");
            }
            timePlayed += (nanoseconds_to_microseconds(systemTime(SYSTEM_TIME_MONOTONIC)) - timeStarted);
            timeStarted = 0;
            LOGV("paused for bt switch");
        }

        mSeeking = true;
        mReachedEOS = false;
        mSeekTimeUs = timePlayed;

        if (ioctl(afd, AUDIO_FLUSH, 0) < 0) {
            LOGE("Audio flush event failed");
        }
    } else {
        a2dpDisconnectPause = true;
    }


/*

*/
}

void LPAPlayer::setSource(const sp<MediaSource> &source) {
    CHECK_EQ(mSource, NULL);
    LOGV("Setting source from LPA Player");
    mSource = source;
}

status_t LPAPlayer::start(bool sourceAlreadyStarted) {
    CHECK(!mStarted);
    CHECK(mSource != NULL);

    //Check if the source is started, start it
    status_t err;
    if (!sourceAlreadyStarted) {
        err = mSource->start();

        if (err != OK) {
            return err;
        }
    }

    //Create event, decoder and a2dp thread and initialize all the
    //mutexes and coditional variables
    createThreads();
    LOGV("All Threads Created.");

    // We allow an optional INFO_FORMAT_CHANGED at the very beginning
    // of playback, if there is one, getFormat below will retrieve the
    // updated format, if there isn't, we'll stash away the valid buffer
    // of data to be used on the first audio callback.

    CHECK(mFirstBuffer == NULL);

    mFirstBufferResult = mSource->read(&mFirstBuffer);
    if (mFirstBufferResult == INFO_FORMAT_CHANGED) {
        LOGV("INFO_FORMAT_CHANGED!!!");

        CHECK(mFirstBuffer == NULL);
        mFirstBufferResult = OK;
        mIsFirstBuffer = false;
    } else {
        mIsFirstBuffer = true;
    }

    /*TODO: Check for bA2dpEnabled */

    sp<MetaData> format = mSource->getFormat();
    const char *mime;
    bool success = format->findCString(kKeyMIMEType, &mime);
    CHECK(success);
    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    success = format->findInt32(kKeySampleRate, &mSampleRate);
    CHECK(success);

    success = format->findInt32(kKeyChannelCount, &numChannels);
    CHECK(success);

    int64_t durationUs;
    success = format->findInt64(kKeyDuration, &durationUs);
    CHECK(success);

    if ( afd >= 0 ) {
        struct msm_audio_config config;
        if ( ioctl(afd, AUDIO_GET_CONFIG, &config) < 0 ) {
            LOGE("could not get config");
            close(afd);
            afd = -1;
            return BAD_VALUE;
        }

        config.sample_rate = mSampleRate;
        config.channel_count =  numChannels;
        LOGV(" in initate_play, sample_rate=%d and channel count=%d \n", mSampleRate, numChannels);
        if ( ioctl(afd, AUDIO_SET_CONFIG, &config) < 0 ) {
            LOGE("could not set config");
            close(afd);
            afd = -1;
            return BAD_VALUE;
        }
    }

    // Get the session id from the LPA Driver


    //Register the session id with HAL for routing
    if (mAudioSink.get() != NULL) {
        unsigned short decId;
        if ( ioctl(afd, AUDIO_GET_SESSION_ID, &decId) == -1 ) {
            LOGE("AUDIO_GET_SESSION_ID FAILED\n");
            return BAD_VALUE;
        } else {
            sessionId = (int)decId;
            LOGV("AUDIO_GET_SESSION_ID success : decId = %d", decId);

        }

        if (!bIsA2DPEnabled) {

            LOGV("Opening a routing session for audio playback: sessionId = %d", sessionId);
            status_t err = mAudioSink->openSession(AudioSystem::PCM_16_BIT, sessionId);
            if (err != OK) {
                if (mFirstBuffer != NULL) {
                    mFirstBuffer->release();
                    mFirstBuffer = NULL;
                }

                if (!sourceAlreadyStarted) {
                    mSource->stop();
                }

                LOGE("Opening a routing session failed");
                close(afd);
                afd = -1;

                return err;
            }
            LOGV("AudioSink Opened a session(%d)",sessionId);

            //Start the Driver
            if (ioctl(afd, AUDIO_START,0) < 0) {
                LOGE("Driver start failed!");
                return BAD_VALUE;
            }
            LOGV("LPA Driver Started");
        } else {
            LOGV("Before Audio Sink Open");
            status_t ret = mAudioSink->open(mSampleRate, numChannels,AudioSystem::PCM_16_BIT, DEFAULT_AUDIOSINK_BUFFERCOUNT);
            mAudioSink->start();
            LOGV("After Audio Sink Open");
            mAudioSinkOpen = true;
            //pthread_cond_signal(&a2dp_cv);
        }

    } else {
        close(afd);
        afd = -1;
        LOGE("Audiosink is NULL");
        return BAD_VALUE;
    }

    mStarted = true;

    LOGV("Waking up decoder thread");
    pthread_cond_signal(&decoder_cv);
    return OK;
}

status_t LPAPlayer::seekTo(int64_t time_us) {
    Mutex::Autolock autoLock(mLock);
    if ( mReachedEOS ) {
        mReachedEOS = false;
        LOGV("Signalling to Decoder Thread");
        pthread_cond_signal(&decoder_cv);
    }
    mSeeking = true;

    mSeekTimeUs = time_us;
    timePlayed = time_us;
    timeStarted = 0;

    LOGV("In seekTo(), mSeekTimeUs %u",mSeekTimeUs);
    /*TODO: Check A2dp variable */
    if (!bIsA2DPEnabled) {
        if (!isPaused) {
            if (ioctl(afd, AUDIO_PAUSE, 1) < 0) {
                LOGE("Audio Pause failed");
            }
        }
        if (ioctl(afd, AUDIO_FLUSH, 0) < 0) {
            LOGE("Audio Flush failed");
        }
        LOGV("Paused case, %d",isPaused);
        if (isPaused) {
            LOGV("AUDIO pause in seek()");
            if (ioctl(afd, AUDIO_PAUSE, 1) < 0) {
                LOGE("Audio Pause failed");
                return BAD_VALUE;
            }
        }
        LOGV("seek() after pmemBuffersRequestQueue.size() = %d, pmemBuffersResponseQueue.size() = %d ",pmemBuffersRequestQueue.size(),pmemBuffersResponseQueue.size());
        mAudioSink->flush();
    } else {
        mSeeked = true;
        if (!isPaused) {
            mAudioSink->pause();
            mAudioSink->flush();
            mAudioSink->start();
        }
        
    }

    return OK;
}

void LPAPlayer::pause(bool playPendingSamples) {
    CHECK(mStarted);

    if (playPendingSamples) {
        /* TODO: Check what needs to be in this case */
    } else {
        if (a2dpDisconnectPause) {

        } else {
            /* TODO: Check A2DP variable */
            if (!bIsA2DPEnabled) {
                LOGE("LPAPlayer::Pause - Pause driver");
                if (ioctl(afd, AUDIO_PAUSE, 1) < 0) {
                    LOGE("Audio Pause failed");
                }
                if (mAudioSink.get() != NULL) {
                    mAudioSink->pauseSession();
                }
            } else {
                isPaused = true;
                mAudioSink->pause();
            }
        }
    }
    CHECK(mSource != NULL);
    mSource->pause();
    isPaused = true;
    timePlayed += (nanoseconds_to_microseconds(systemTime(SYSTEM_TIME_MONOTONIC)) - timeStarted);
}

void LPAPlayer::resume() {


    if ( isPaused) {
        CHECK(mStarted);
        if (mSource->isPaused()==true) {
            CHECK(mSource != NULL);
            mSource->start();
        }
        /* TODO: Check A2dp variable */
        if (a2dpDisconnectPause) {
            LOGV("A2DP disconnect resume");
            mAudioSink->pause();
            mAudioSink->stop();
            mAudioSink->close();
            mAudioSinkOpen = false;
            status_t err = mAudioSink->openSession(AudioSystem::PCM_16_BIT, sessionId);
            a2dpDisconnectPause = false;
            mSeeking = true;
            mReachedEOS = false;
            mSeekTimeUs = timePlayed;
            mSeeked = true;

            if (ioctl(afd, AUDIO_START,0) < 0) {
                LOGE("Driver start failed!");
            }
            LOGV("LPA Driver Started");

            pthread_cond_signal(&event_cv);
            pthread_cond_signal(&a2dp_cv);
            pthread_cond_signal(&decoder_cv);

        } else {
            if (!bIsA2DPEnabled) {
                LOGE("LPAPlayer::resume - Resuming Driver");
                if (ioctl(afd, AUDIO_PAUSE, 0) < 0) {
                    LOGE("Audio resume failed");
                }
                if (mAudioSink.get() != NULL) {
                    mAudioSink->resumeSession();
                }
            } else {
                isPaused = false;
                if (mSeeked) {
                    mAudioSink->flush();
                }
                mAudioSink->start();
                LOGV("Waking up A2dp thread");
                pthread_cond_signal(&a2dp_cv);
                if (mSeeked) {
                    mAudioSink->flush();
                }
            }
            if (!mSeeked) {
                timeStarted = nanoseconds_to_microseconds(systemTime(SYSTEM_TIME_MONOTONIC));
            }
        }
        isPaused = false;


    }
}

void LPAPlayer::reset() {
    CHECK(mStarted);
    LOGV("Reset called!!!!!");
    asyncReset = true;

    if(bIsA2DPEnabled || a2dpDisconnectPause) {
        LOGV("AudioSink close");
        mAudioSink->pause();
        mAudioSink->stop();
        mAudioSink->close();
    }
    else {
        ioctl(afd,AUDIO_FLUSH,0);
        ioctl(afd,AUDIO_STOP,0);
        mAudioSink->closeSession();
    }

    LOGV("reset() pmemBuffersRequestQueue.size() = %d, pmemBuffersResponseQueue.size() = %d ",pmemBuffersRequestQueue.size(),pmemBuffersResponseQueue.size());
    
    // make sure Decoder thread has exited
    requestAndWaitForDecoderThreadExit();

    // make sure the event thread also has exited if it has not done before.
    requestAndWaitForEventThreadExit();

    requestAndWaitForA2DPThreadExit();
    



    // Make sure to release any buffer we hold onto so that the
    // source is able to stop().
    if (mFirstBuffer != NULL) {
        mFirstBuffer->release();
        mFirstBuffer = NULL;
    }

    if (mInputBuffer != NULL) {
        LOGV("AudioPlayer releasing input buffer.");
        mInputBuffer->release();
        mInputBuffer = NULL;
    }

    mSource->stop();

    // The following hack is necessary to ensure that the OMX
    // component is completely released by the time we may try
    // to instantiate it again.
    wp<MediaSource> tmp = mSource;
    mSource.clear();
    while (tmp.promote() != NULL) {
        usleep(1000);
    }

    if ( afd >= 0 ) {
        pmemBufferDeAlloc();
        close(afd);
        afd = -1;
    }

    LOGV("reset() after pmemBuffersRequestQueue.size() = %d, pmemBuffersResponseQueue.size() = %d ",pmemBuffersRequestQueue.size(),pmemBuffersResponseQueue.size());

    mNumFramesPlayed = 0;
    mPositionTimeMediaUs = -1;
    mPositionTimeRealUs = -1;
    mSeeking = false;
    mReachedEOS = false;
    mFinalStatus = OK;
    mStarted = false;
}


bool LPAPlayer::isSeeking() {
    Mutex::Autolock autoLock(mLock);
    return mSeeking;
}

bool LPAPlayer::reachedEOS(status_t *finalStatus) {
    *finalStatus = OK;

    Mutex::Autolock autoLock(mLock);
    *finalStatus = mFinalStatus;
    return mReachedEOS;
}


void *LPAPlayer::decoderThreadWrapper(void *me) {
    static_cast<LPAPlayer *>(me)->decoderThreadEntry();
    return NULL;
}

void LPAPlayer::decoderThreadEntry() {

    pthread_mutex_lock(&decoder_mutex);

    setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_AUDIO);
    prctl(PR_SET_NAME, (unsigned long)"LPAPlayer", 0, 0, 0);

    LOGV("decoderThreadEntry wait for signal \n");
    if (!mStarted) {
        pthread_cond_wait(&decoder_cv, &decoder_mutex);
    }
    LOGV("decoderThreadEntry ready to work \n");
    pthread_mutex_unlock(&decoder_mutex);

    void *pmem_buf; int32_t pmem_fd;
    struct msm_audio_pmem_info pmem_info;
    for (int i = 0; i < PMEM_BUFFER_COUNT; i++) {

        pthread_mutex_lock(&pmem_request_mutex);
        pmem_buf = pmemBufferAlloc(PMEM_BUFFER_SIZE, &pmem_fd);
        memset(&pmem_info, 0, sizeof(msm_audio_pmem_info));
        LOGV("Registering PMEM with fd %d and address as %x", pmem_fd, pmem_buf);
        pmem_info.fd = pmem_fd;
        pmem_info.vaddr = pmem_buf;
        if ( ioctl(afd, AUDIO_REGISTER_PMEM, &pmem_info) < 0 ) {
            LOGE("Registration of PMEM with the Driver failed with fd %d and memory %x",
                 pmem_info.fd, (unsigned int)pmem_info.vaddr);
        }
        pthread_mutex_unlock(&pmem_request_mutex);
    }

    if (pmemBuffersRequestQueue.empty()) {
        LOGV("Request Queue Empty");
    } else {
        LOGV("Request Queue Size: %d",pmemBuffersRequestQueue.size());
    }

    while (1) {
        pthread_mutex_lock(&pmem_request_mutex);

        if (killDecoderThread) {
            pthread_mutex_unlock(&pmem_request_mutex);
            break;
        }
        if (pmemBuffersRequestQueue.empty() || a2dpDisconnectPause || mReachedEOS || (bIsA2DPEnabled && !mAudioSinkOpen) || asyncReset) {
            pthread_cond_wait(&decoder_cv, &pmem_request_mutex);
        }


        List<BuffersAllocated>::iterator it = pmemBuffersRequestQueue.begin();
        BuffersAllocated buf = *it;
        pmemBuffersRequestQueue.erase(it);
        pthread_mutex_unlock(&pmem_request_mutex);

        LOGV("decoder pmemBuffersRequestQueue.size() = %d, pmemBuffersResponseQueue.size() = %d ",pmemBuffersRequestQueue.size(),pmemBuffersResponseQueue.size());

        //Queue the buffers back to Request queue
        if (mReachedEOS || (bIsA2DPEnabled && !mAudioSinkOpen) || asyncReset || a2dpDisconnectPause) {
            pthread_mutex_lock(&pmem_request_mutex);
            pmemBuffersRequestQueue.push_back(buf);
            //pthread_cond_wait(&decoder_cv, &pmem_request_mutex);
            pthread_mutex_unlock(&pmem_request_mutex);
        } 
        //Queue up the buffers for writing either for A2DP or LPA Driver
        else {
            LOGV("Calling fillBuffer for size %d",PMEM_BUFFER_SIZE);
            buf.bytesToWrite = fillBuffer(buf.pmemBuf, PMEM_BUFFER_SIZE);
            LOGV("fillBuffer returned size %d",buf.bytesToWrite);

            /* TODO: Check if we have to notify the app if an error occurs */
            if (!bIsA2DPEnabled) {
                if ( buf.bytesToWrite > 0) {
                    struct msm_audio_aio_buf aio_buf_local;

                    memset(&aio_buf_local, 0, sizeof(msm_audio_aio_buf));
                    aio_buf_local.buf_addr = buf.pmemBuf;
                    aio_buf_local.buf_len = buf.bytesToWrite;
                    aio_buf_local.data_len = buf.bytesToWrite;
                    aio_buf_local.private_data = (void*) buf.pmemFd;

                    if ( (buf.bytesToWrite % 2) != 0 ) {
                        LOGV("Increment for even bytes");
                        aio_buf_local.data_len += 1;
                    }

                    if (timeStarted == 0) {
                        timeStarted = nanoseconds_to_microseconds(systemTime(SYSTEM_TIME_MONOTONIC));
                    }
                    if (!asyncReset) {
                        LOGV("Wrting buffer to driver with pmem fd %d", buf.pmemFd);
                        if ( ioctl(afd, AUDIO_ASYNC_WRITE, &aio_buf_local) < 0 ) {
                            LOGE("error on async write\n");
                        }
                    }
                } else {
                    /* Put the buffer back into requestQ */
                    pthread_mutex_lock(&pmem_request_mutex);
                    pmemBuffersRequestQueue.push_back(buf);
                    pthread_mutex_unlock(&pmem_request_mutex);

                }
            }
            pthread_mutex_lock(&pmem_response_mutex);
            pmemBuffersResponseQueue.push_back(buf);
            pthread_mutex_unlock(&pmem_response_mutex);

            if (!bIsA2DPEnabled)
                pthread_cond_signal(&event_cv);
            else
                pthread_cond_signal(&a2dp_cv);
        }
    }
    decoderThreadAlive = false;
    LOGV("decoder Thread is dying");
}

void *LPAPlayer::eventThreadWrapper(void *me) {
    static_cast<LPAPlayer *>(me)->eventThreadEntry();
    return NULL;
}

void LPAPlayer::eventThreadEntry() {
    struct msm_audio_event cur_pcmdec_event;

    pthread_mutex_lock(&event_mutex);
    int rc = 0;
    setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_AUDIO);
    prctl(PR_SET_NAME, (unsigned long)"LPAPlayer", 0, 0, 0);

    LOGV("eventThreadEntry wait for signal \n");
    pthread_cond_wait(&event_cv, &event_mutex);
    LOGV("eventThreadEntry ready to work \n");
    pthread_mutex_unlock(&event_mutex);

    if (killEventThread) {
        LOGV("Event Thread is dying.");
        return;
    }

    while (1) {
        //Wait for an event to occur
        rc = ioctl(afd, AUDIO_GET_EVENT, &cur_pcmdec_event);
        LOGV("pcm dec Event Thread rc = %d and errno is %d",rc, errno);

        if ( (rc < 0) && (errno == ENODEV ) ) {
            LOGV("AUDIO_ABORT_GET_EVENT called. Exit the thread");
            break;
        }

        switch ( cur_pcmdec_event.event_type ) {
        case AUDIO_EVENT_WRITE_DONE:
            {
                LOGV("WRITE_DONE: addr %p len %d and fd is %d\n",
                     cur_pcmdec_event.event_payload.aio_buf.buf_addr,
                     cur_pcmdec_event.event_payload.aio_buf.data_len,
                     (int32_t) cur_pcmdec_event.event_payload.aio_buf.private_data);
                Mutex::Autolock autoLock(mLock);
                mNumFramesPlayed += cur_pcmdec_event.event_payload.aio_buf.buf_len/ mFrameSize;
                pthread_mutex_lock(&pmem_response_mutex);
                BuffersAllocated buf = *(pmemBuffersResponseQueue.begin());
                for (List<BuffersAllocated>::iterator it = pmemBuffersResponseQueue.begin();
                    it != pmemBuffersResponseQueue.end(); ++it) {
                    if (it->pmemBuf == cur_pcmdec_event.event_payload.aio_buf.buf_addr) {
                        buf = *it;
                        pmemBuffersResponseQueue.erase(it);
                        break;
                    }
                }

                /* If the rendering is complete report EOS to the AwesomePlayer */
                if (mObserver && mReachedEOS && pmemBuffersResponseQueue.empty()) {
                    LOGV("Posting EOS event to AwesomePlayer");
                    mObserver->postAudioEOS();
                }
                if (pmemBuffersResponseQueue.empty() && bIsA2DPEnabled) {
                    LOGV("Close Session");
                    if (mAudioSink.get() != NULL) {
                        mAudioSink->closeSession();
                        LOGV("mAudioSink close session");
                        ioctl(afd, AUDIO_STOP,0);
                    } else {
                        LOGE("close session NULL");
                    }

                    sp<MetaData> format = mSource->getFormat();
                    const char *mime;
                    bool success = format->findCString(kKeyMIMEType, &mime);
                    CHECK(success);
                    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

                    success = format->findInt32(kKeySampleRate, &mSampleRate);
                    CHECK(success);

                    success = format->findInt32(kKeyChannelCount, &numChannels);
                    CHECK(success);
                    LOGV("Before Audio Sink Open");
                    status_t ret = mAudioSink->open(mSampleRate, numChannels,AudioSystem::PCM_16_BIT, DEFAULT_AUDIOSINK_BUFFERCOUNT);
                    mAudioSink->start();
                    LOGV("After Audio Sink Open");
                    mAudioSinkOpen = true;

                }


                pthread_mutex_unlock(&pmem_response_mutex);

                pthread_mutex_lock(&pmem_request_mutex);
                pmemBuffersRequestQueue.push_back(buf);
                pthread_mutex_unlock(&pmem_request_mutex);

                pthread_cond_signal(&decoder_cv);
            }
            break;
        case AUDIO_EVENT_SUSPEND:
            {
                LOGV("AUDIO_EVENT_SUSPEND received\n");
            }
            break;
        case AUDIO_EVENT_RESUME:
            {
                LOGV("AUDIO_EVENT_RESUME received\n");
            }
            break;
        default:
            LOGV("Received Invalid Event from driver\n");
            break;
        }
    }
    eventThreadAlive = false;
    LOGV("Event Thread is dying.");

}

void *LPAPlayer::A2DPThreadWrapper(void *me) {
    static_cast<LPAPlayer *>(me)->A2DPThreadEntry();
    return NULL;
}

void LPAPlayer::A2DPThreadEntry() {
    setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_AUDIO);
    prctl(PR_SET_NAME, (unsigned long)"LPAPlayer", 0, 0, 0);

    //TODO: Remove this
/*
    LOGV("a2dpThreadEntry wait for signal \n");
    pthread_cond_wait(&a2dp_cv, &a2dp_mutex);
    LOGV("a2dpThreadEntry ready to work \n");
    pthread_mutex_unlock(&a2dp_mutex);

    a2dpThreadStarted = true;

    if (killA2DPThread) {
        a2dpThreadAlive = false;
        return;
    }
*/
    while (1) {
        //TODO: Remove this

        pthread_mutex_lock(&pmem_response_mutex);
        if (pmemBuffersResponseQueue.empty() || !mAudioSinkOpen || isPaused || !bIsA2DPEnabled) {
            pthread_cond_wait(&a2dp_cv, &pmem_response_mutex);
        }

        if (killA2DPThread) {
            break;            
        }
        // A2DP got disabled -- Queue up everything back to Request Queue
        if (!bIsA2DPEnabled) {
            pthread_mutex_lock(&pmem_request_mutex);
            pthread_mutex_lock(&pmem_response_mutex);
            while (!pmemBuffersResponseQueue.empty()) {
                LOGV("BUF transfer");
                List<BuffersAllocated>::iterator it = pmemBuffersResponseQueue.begin();
                BuffersAllocated buf = *it;
                pmemBuffersRequestQueue.push_back(buf);
                pmemBuffersResponseQueue.erase(it);
            }
            pthread_mutex_unlock(&pmem_response_mutex);
            pthread_mutex_unlock(&pmem_request_mutex);
        } 
        //A2DP is enabled -- Continue normal Playback
        else {
            List<BuffersAllocated>::iterator it = pmemBuffersResponseQueue.begin();
            BuffersAllocated buf = *it;
            pmemBuffersResponseQueue.erase(it);
            pthread_mutex_unlock(&pmem_response_mutex);
            bytesToWrite = buf.bytesToWrite;
            LOGV("bytes To write:%d",bytesToWrite);
            if (timeStarted == 0) {
                LOGV("Time started in A2DP thread");
                timeStarted = nanoseconds_to_microseconds(systemTime(SYSTEM_TIME_MONOTONIC));
            }
            //LOGV("16 bit :: cmdid = %d, len = %u, bytesAvailInBuffer = %u, bytesToWrite = %u", cmdid, len, bytesAvailInBuffer, bytesToWrite);

            uint32_t bytesWritten = 0;
            uint32_t numBytesRemaining = 0;
            uint32_t bytesAvailInBuffer = 0;
            void* data = buf.pmemBuf;
            while (bytesToWrite) {
                bytesAvailInBuffer = mAudioSink->bufferSize();

                uint32_t writeLen = bytesAvailInBuffer > bytesToWrite ? bytesToWrite : bytesAvailInBuffer;
                //LOGV("16 bit :: cmdid = %d, len = %u, bytesAvailInBuffer = %u, bytesToWrite = %u", cmdid, len, bytesAvailInBuffer, bytesToWrite);
                bytesWritten = mAudioSink->write(data, writeLen);
                /*if ( bytesWritten != writeLen ) {


                    if (mSeeked) {
                        break;
                    }
                    LOGE("Error writing audio data");
                    pthread_mutex_lock(&a2dp_mutex);
                    pthread_cond_wait(&a2dp_cv, &a2dp_mutex);
                    pthread_mutex_unlock(&a2dp_mutex);
                    if (mSeeked) {
                        break;
                    }

                }*/
                if ( bytesWritten != writeLen ) {

                    //Paused - Wait till resume
                    if (isPaused) {
                        LOGV("Pausing A2DP playback");
                        pthread_mutex_lock(&a2dp_mutex);
                        pthread_cond_wait(&a2dp_cv, &a2dp_mutex);
                        pthread_mutex_unlock(&a2dp_mutex);
                    }

                    //Seeked: break out of loop, flush old buffers and write new buffers
                    LOGV("@_@bytes To write1:%d",bytesToWrite);
                    if (mSeeked) {
                        LOGV("Seeking A2DP Playback");
                        break;
                    }
                    
                }
                data += bytesWritten;
                bytesToWrite -= bytesWritten;
                LOGV("@_@bytes To write2:%d",bytesToWrite);
                
            }
            if (mObserver && mReachedEOS && pmemBuffersResponseQueue.empty()) {
                LOGV("Posting EOS event to AwesomePlayer");
                mObserver->postAudioEOS();
            }
            pthread_mutex_lock(&pmem_request_mutex);
            pmemBuffersRequestQueue.push_back(buf);
            if (killA2DPThread) {
                pthread_mutex_unlock(&pmem_request_mutex);
                break;            
            }
            //flush out old buffer
            if (mSeeked) {
                mSeeked = false;
                pthread_mutex_lock(&pmem_response_mutex);
                while (!pmemBuffersResponseQueue.empty()) {
                    List<BuffersAllocated>::iterator it = pmemBuffersResponseQueue.begin();
                    BuffersAllocated buf = *it;
                    pmemBuffersRequestQueue.push_back(buf);
                    pmemBuffersResponseQueue.erase(it);
                }
                pthread_mutex_unlock(&pmem_response_mutex);
            }
            pthread_mutex_unlock(&pmem_request_mutex);
        }
        if (bIsA2DPEnabled) {
            pthread_cond_signal(&decoder_cv);
        }
    }
    a2dpThreadAlive = false;
    LOGV("A2DP Thread is dying.");
}

void *LPAPlayer::pmemBufferAlloc(int32_t nSize, int32_t *pmem_fd){
    int32_t pmemfd = -1;
    void  *pmem_buf = NULL;

    // 1. Open the pmem_audio
    pmemfd = open("/dev/pmem_audio", O_RDWR);
    if (pmemfd < 0) {
        LOGE("pmemBufferAlloc failed to open pmem_audio");
        *pmem_fd = -1;
        return pmem_buf;
    }

    // 2. MMAP to get the virtual address
    pmem_buf = mmap(0, nSize, PROT_READ | PROT_WRITE, MAP_SHARED, pmemfd, 0);
    if ( NULL == pmem_buf) {
        LOGE("pmemBufferAlloc failed to mmap");
        *pmem_fd = -1;
        return NULL;
    }

    // 3. Store this information for internal mapping / maintanence
    BuffersAllocated buf(pmem_buf, nSize, pmemfd);
    pmemBuffersRequestQueue.push_back(buf);

    // 4. Send the pmem fd information
    *pmem_fd = pmemfd;
    LOGV("pmemBufferAlloc calling with required size %d", nSize);
    LOGV("The PMEM that is allocated is %d and buffer is %x", pmemfd, (unsigned int)pmem_buf);

    // 5. Return the virtual address
    return pmem_buf;
}

void LPAPlayer::pmemBufferDeAlloc()
{
    //Remove all the buffers from request queue
    while (!pmemBuffersRequestQueue.empty())  {
        List<BuffersAllocated>::iterator it = pmemBuffersRequestQueue.begin();
        BuffersAllocated &pmemBuffer = *it;
        struct msm_audio_pmem_info pmem_info;
        pmem_info.vaddr = (*it).pmemBuf;
        pmem_info.fd = (*it).pmemFd;
        if (ioctl(afd, AUDIO_DEREGISTER_PMEM, &pmem_info) < 0) {
            LOGE("PMEM deregister failed");
        }
        LOGV("Unmapping the address %u, size %d, fd %d from Request",pmemBuffer.pmemBuf,pmemBuffer.bytesToWrite,pmemBuffer.pmemFd);
        munmap(pmemBuffer.pmemBuf, PMEM_BUFFER_SIZE);
        LOGV("closing the pmem fd");
        close(pmemBuffer.pmemFd);
        LOGV("Removing from request Q");
        pmemBuffersRequestQueue.erase(it);
    }
    //Remove all the buffers from response queue
    while(!pmemBuffersResponseQueue.empty()){
        List<BuffersAllocated>::iterator it = pmemBuffersResponseQueue.begin();
        BuffersAllocated &pmemBuffer = *it;
        struct msm_audio_pmem_info pmem_info;
        pmem_info.vaddr = (*it).pmemBuf;
        pmem_info.fd = (*it).pmemFd;
        if (ioctl(afd, AUDIO_DEREGISTER_PMEM, &pmem_info) < 0) {
            LOGE("PMEM deregister failed");
        }
        LOGV("Unmapping the address %u, size %d, fd %d from Response",pmemBuffer.pmemBuf,PMEM_BUFFER_SIZE ,pmemBuffer.pmemFd);
        munmap(pmemBuffer.pmemBuf, PMEM_BUFFER_SIZE);
        LOGV("closing the pmem fd");
        close(pmemBuffer.pmemFd);
        LOGV("Removing from response Q");
        pmemBuffersResponseQueue.erase(it);
    }
}

void LPAPlayer::createThreads() {

    //Initialize all the Mutexes and Condition Variables
    pthread_mutex_init(&pmem_request_mutex, NULL);
    pthread_mutex_init(&pmem_response_mutex, NULL);
    pthread_mutex_init(&decoder_mutex, NULL);
    pthread_mutex_init(&event_mutex, NULL);
    pthread_mutex_init(&a2dp_mutex, NULL);

    pthread_cond_init (&event_cv, NULL);
    pthread_cond_init (&decoder_cv, NULL);
    pthread_cond_init (&a2dp_cv, NULL);

    // Create 4 threads LPA, decoder, event and A2dp
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    killDecoderThread = false;
    killEventThread = false;
    killA2DPThread = false;
    decoderThreadAlive = true;
    eventThreadAlive = true;
    a2dpThreadAlive = true;
    LOGV("Creating Event Thread");
    pthread_create(&eventThread, &attr, eventThreadWrapper, this);


    LOGV("Creating decoder Thread");
    pthread_create(&decoderThread, &attr, decoderThreadWrapper, this);

    LOGV("Creating A2dp Thread");
    pthread_create(&A2DPThread, &attr, A2DPThreadWrapper, this);
    pthread_attr_destroy(&attr);


}


size_t LPAPlayer::fillBuffer(void *data, size_t size) {
    LOGE("fillBuffer");
    if (mNumFramesPlayed == 0) {
        LOGV("AudioCallback");
    }

    LOGV("Number of Frames Played: %u", mNumFramesPlayed);
    if (mReachedEOS) {
        return 0;
    }

    size_t size_done = 0;
    size_t size_remaining = size;
    while (size_remaining > 0) {
        MediaSource::ReadOptions options;
        {
            Mutex::Autolock autoLock(mLock);

            if (mSeeking) {
                if (mIsFirstBuffer) {
                    if (mFirstBuffer != NULL) {
                        mFirstBuffer->release();
                        mFirstBuffer = NULL;
                    }
                    mIsFirstBuffer = false;
                }

                options.setSeekTo(mSeekTimeUs);

                if (mInputBuffer != NULL) {
                    mInputBuffer->release();
                    mInputBuffer = NULL;
                }

                mSeeking = false;
                if (mObserver) {
                    mObserver->postAudioSeekComplete();
                }
            }
        }

        if (mInputBuffer == NULL) {
            status_t err;

            if (mIsFirstBuffer) {
                mInputBuffer = mFirstBuffer;
                mFirstBuffer = NULL;
                err = mFirstBufferResult;

                mIsFirstBuffer = false;
            } else {
                err = mSource->read(&mInputBuffer, &options);
            }

            CHECK((err == OK && mInputBuffer != NULL)
                  || (err != OK && mInputBuffer == NULL));

            Mutex::Autolock autoLock(mLock);

            if (err != OK) {
                if (err == INFO_FORMAT_CHANGED) {
                    sp<MetaData> format = mSource->getFormat();
                    const char *mime;
                    bool success = format->findCString(kKeyMIMEType, &mime);
                    CHECK(success);
                    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

                    success = format->findInt32(kKeySampleRate, &mSampleRate);
                    CHECK(success);

                    int32_t numChannels;
                    success = format->findInt32(kKeyChannelCount, &numChannels);
                    CHECK(success);

                    mAudioSink->stop();
                    mAudioSink->closeSession();
                    LOGV("Opening a routing session in fillBuffer: sessionId = %d", sessionId);
                    status_t err = mAudioSink->openSession(AudioSystem::PCM_16_BIT, sessionId);
                    if (err != OK) {
                        mSource->stop();
                        return err;
                    }
                    mLatencyUs = 96;//(int64_t)mAudioSink->latency() * 1000;
                    mFrameSize = mAudioSink->frameSize();
                    mAudioSink->start();
                    break;
                } else {
                    mReachedEOS = true;
                    mFinalStatus = err;
                    break;
                }
            }

            CHECK(mInputBuffer->meta_data()->findInt64(
                                                      kKeyTime, &mPositionTimeMediaUs));

            mPositionTimeRealUs =
            ((mNumFramesPlayed + size_done / mFrameSize) * 1000000)
            / mSampleRate;

            //   LOGV("buffer->size() = %d, "
            //       "mPositionTimeMediaUs=%.2f mPositionTimeRealUs=%.2f",
            //       mInputBuffer->range_length(),
            //      mPositionTimeMediaUs / 1E6, mPositionTimeRealUs / 1E6);
        }

        if (mInputBuffer->range_length() == 0) {
            mInputBuffer->release();
            mInputBuffer = NULL;

            continue;
        }

        size_t copy = size_remaining;
        if (copy > mInputBuffer->range_length()) {
            copy = mInputBuffer->range_length();
        }

        memcpy((char *)data + size_done,
               (const char *)mInputBuffer->data() + mInputBuffer->range_offset(),
               copy);

        mInputBuffer->set_range(mInputBuffer->range_offset() + copy,
                                mInputBuffer->range_length() - copy);

        size_done += copy;
        size_remaining -= copy;
    }
    return size_done;
}

int64_t LPAPlayer::getRealTimeUs() {
    Mutex::Autolock autoLock(mLock);
    return getRealTimeUsLocked();
}


int64_t LPAPlayer::getRealTimeUsLocked(){
    /* struct msm_audio_stats stats;
 
     // 1. Get the Byte count that is consumed
     if ( ioctl(afd, AUDIO_GET_STATS, &stats)  < 0 ) {
         LOGE("AUDIO_GET_STATUS failed");
     } 
 
     //mNumFramesDspPlayed = mNumFramesPlayed - ((PMEM_BUFFER_SIZE - stats.byte_count)/mFrameSize);
     LOGE("AUDIO_GET_STATUS bytes %u, mNumFramesPlayed %u", stats.byte_count/mFrameSize,mNumFramesPlayed);
     //mNumFramesDspPlayed = mNumFramesPlayed + stats.byte_count/mFrameSize;
     
     int64_t temp = (stats.byte_count/mFrameSize)+mNumFramesPlayed;
     LOGE("Number of frames played by the DSP is %u", temp);
     int64_t temp1 = -mLatencyUs + (temp * 1000000) / mSampleRate;
     LOGE("getRealTimeUsLocked() %u", temp1);
     return temp1;*/

    return nanoseconds_to_microseconds(systemTime(SYSTEM_TIME_MONOTONIC)) - timeStarted + timePlayed;
}

int64_t LPAPlayer::getMediaTimeUs() {
    Mutex::Autolock autoLock(mLock);
/*
if (mPositionTimeMediaUs < 0 || mPositionTimeRealUs < 0) {
return 0;
}

int64_t realTimeOffset = getRealTimeUsLocked() - mPositionTimeRealUs;
if (realTimeOffset < 0) {
realTimeOffset = 0;
}

return mPositionTimeMediaUs + realTimeOffset;
*/
    struct msm_audio_stats stats;
    if ( ioctl(afd, AUDIO_GET_STATS, &stats)  < 0 ) {
        LOGE("AUDIO_GET_STATUS failed");
    }


    if (isPaused || timeStarted == 0)
        return timePlayed;
    else
        return nanoseconds_to_microseconds(systemTime(SYSTEM_TIME_MONOTONIC)) - timeStarted + timePlayed;

    /*int64_t bytes = (int64_t)stats.byte_count;
    LOGV("stats %u    %u",bytes,stats.byte_count);
    LOGV("secs played %u", ((stats.byte_count/4) * 1000000)/mSampleRate );
    return((stats.byte_count/4) * 1000000)/mSampleRate;*/
}

bool LPAPlayer::getMediaTimeMapping(
                                   int64_t *realtime_us, int64_t *mediatime_us) {
    Mutex::Autolock autoLock(mLock);

    *realtime_us = mPositionTimeRealUs;
    *mediatime_us = mPositionTimeMediaUs;

    return mPositionTimeRealUs != -1 && mPositionTimeMediaUs != -1;
}

void LPAPlayer::requestAndWaitForDecoderThreadExit() {

    if (!decoderThreadAlive)
        return;
    killDecoderThread = true;
    pthread_cond_signal(&decoder_cv);
    pthread_join(decoderThread,NULL);
    LOGV("decoder thread killed");

}

void LPAPlayer::requestAndWaitForEventThreadExit() {
    if (!eventThreadAlive)
        return;
    killEventThread = true;
    pthread_cond_signal(&event_cv);
    if (ioctl(afd, AUDIO_ABORT_GET_EVENT, 0) < 0) {
        LOGE("Audio Abort event failed");
    }
    /*pthread_cond_wait(&event_cv, &event_mutex);
    pthread_mutex_unlock(&event_mutex);
    */
    pthread_join(eventThread,NULL);
    LOGV("event thread killed");
}

void LPAPlayer::requestAndWaitForA2DPThreadExit() {
    if (!a2dpThreadAlive)
        return;
    killA2DPThread = true;
    pthread_cond_signal(&a2dp_cv);
    pthread_join(A2DPThread,NULL);
    LOGV("a2dp thread killed");
}

}
