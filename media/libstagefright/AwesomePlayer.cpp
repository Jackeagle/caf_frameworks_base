/*
 * Copyright (C) 2009 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "AwesomePlayer"
#include <utils/Log.h>

#include <ctype.h>
#include <dlfcn.h>

#include "include/AwesomePlayer.h"
#include "include/Prefetcher.h"
#include "include/SoftwareRenderer.h"

#include <binder/IPCThreadState.h>
#include <media/stagefright/AudioPlayer.h>
#include <media/stagefright/CachingDataSource.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaDebug.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/OMXCodec.h>

#include <surfaceflinger/ISurface.h>
#include <cutils/properties.h>

namespace android {

struct AwesomeEvent : public TimedEventQueue::Event {
    AwesomeEvent(
            AwesomePlayer *player,
            void (AwesomePlayer::*method)())
        : mPlayer(player),
          mMethod(method) {
    }

protected:
    virtual ~AwesomeEvent() {}

    virtual void fire(TimedEventQueue *queue, int64_t /* now_us */) {
        (mPlayer->*mMethod)();
    }

private:
    AwesomePlayer *mPlayer;
    void (AwesomePlayer::*mMethod)();

    AwesomeEvent(const AwesomeEvent &);
    AwesomeEvent &operator=(const AwesomeEvent &);
};

struct AwesomeRemoteRenderer : public AwesomeRenderer {
    AwesomeRemoteRenderer(const sp<IOMXRenderer> &target)
        : mTarget(target) {
    }

    virtual void render(MediaBuffer *buffer) {
        void *id;
        if (buffer->meta_data()->findPointer(kKeyBufferID, &id)) {
            mTarget->render((IOMX::buffer_id)id);
        }
    }

private:
    sp<IOMXRenderer> mTarget;

    AwesomeRemoteRenderer(const AwesomeRemoteRenderer &);
    AwesomeRemoteRenderer &operator=(const AwesomeRemoteRenderer &);
};

struct AwesomeLocalRenderer : public AwesomeRenderer {
    AwesomeLocalRenderer(
            bool previewOnly,
            const char *componentName,
            OMX_COLOR_FORMATTYPE colorFormat,
            const sp<ISurface> &surface,
            size_t displayWidth, size_t displayHeight,
            size_t decodedWidth, size_t decodedHeight)
        : mTarget(NULL),
          mLibHandle(NULL) {
            init(previewOnly, componentName,
                 colorFormat, surface, displayWidth,
                 displayHeight, decodedWidth, decodedHeight);
    }

    virtual void render(MediaBuffer *buffer) {
        render((const uint8_t *)buffer->data() + buffer->range_offset(),
               buffer->range_length());
    }

    void render(const void *data, size_t size) {
        mTarget->render(data, size, NULL);
    }

protected:
    virtual ~AwesomeLocalRenderer() {
        delete mTarget;
        mTarget = NULL;

        if (mLibHandle) {
            dlclose(mLibHandle);
            mLibHandle = NULL;
        }
    }

private:
    VideoRenderer *mTarget;
    void *mLibHandle;

    void init(
            bool previewOnly,
            const char *componentName,
            OMX_COLOR_FORMATTYPE colorFormat,
            const sp<ISurface> &surface,
            size_t displayWidth, size_t displayHeight,
            size_t decodedWidth, size_t decodedHeight);

    AwesomeLocalRenderer(const AwesomeLocalRenderer &);
    AwesomeLocalRenderer &operator=(const AwesomeLocalRenderer &);;
};

void AwesomeLocalRenderer::init(
        bool previewOnly,
        const char *componentName,
        OMX_COLOR_FORMATTYPE colorFormat,
        const sp<ISurface> &surface,
        size_t displayWidth, size_t displayHeight,
        size_t decodedWidth, size_t decodedHeight) { //not passing any rotation information for now
    if (!previewOnly) {
        // We will stick to the vanilla software-color-converting renderer
        // for "previewOnly" mode, to avoid unneccessarily switching overlays
        // more often than necessary.

        mLibHandle = dlopen("libstagefrighthw.so", RTLD_NOW);

        if (mLibHandle) {
            typedef VideoRenderer *(*CreateRendererFunc)(
                    const sp<ISurface> &surface,
                    const char *componentName,
                    OMX_COLOR_FORMATTYPE colorFormat,
                    size_t displayWidth, size_t displayHeight,
                    size_t decodedWidth, size_t decodedHeight,
                    size_t rotation );

            CreateRendererFunc func =
                (CreateRendererFunc)dlsym(
                        mLibHandle,
                        "_Z14createRendererRKN7android2spINS_8ISurfaceEEEPKc20"
                        "OMX_COLOR_FORMATTYPEjjjj");

            if (func) {
                mTarget =
                    (*func)(surface, componentName, colorFormat,
                        displayWidth, displayHeight,
                            decodedWidth, decodedHeight, 0); //rotation comes here
            }
        }
    }

    if (mTarget == NULL) {
        mTarget = new SoftwareRenderer(
                colorFormat, surface, displayWidth, displayHeight,
                decodedWidth, decodedHeight);
    }
}

AwesomePlayer::AwesomePlayer()
    : mQueueStarted(false),
      mTimeSource(NULL),
      mFallbackTimeSource(NULL),
      mAudioEOSOccurred(false),
      mVideoEOSOccurred(false),
      mVideoDurationUs(0),
      mAudioDurationUs(0),
      mPauseAudioTillSync(false),
      mVideoRendererIsPreview(false),
      mAudioPlayer(NULL),
      mFlags(0),
      mExtractorFlags(0),
      mCodecFlags(0),
      mFramesDropped(0),
      mConsecutiveFramesDropped(0),
      mCatchupTimeStart(0),
      mNumTimesSyncLoss(0),
      mMaxEarlyDelta(0),
      mMaxLateDelta(0),
      mMaxTimeSyncLoss(0),
      mTotalFrames(0),
      mSuspensionState(NULL) {

    mVideoBuffer = new MediaBuffer*[BUFFER_QUEUE_CAPACITY];
    for(int i=0;i<BUFFER_QUEUE_CAPACITY;i++)
        mVideoBuffer[i] = NULL;

    CHECK_EQ(mClient.connect(), OK);

    DataSource::RegisterDefaultSniffers();

    mVideoEvent = new AwesomeEvent(this, &AwesomePlayer::onVideoEvent);
    mVideoEventPending = false;
    mStreamDoneEvent = new AwesomeEvent(this, &AwesomePlayer::onStreamDone);
    mStreamDoneEventPending = false;
    mBufferingEvent = new AwesomeEvent(this, &AwesomePlayer::onBufferingUpdate);
    mBufferingEventPending = false;

    mCheckAudioStatusEvent = new AwesomeEvent(
            this, &AwesomePlayer::onCheckAudioStatus);

    mAudioStatusEventPending = false;

    mVideoQueueFront = 0;
    mVideoQueueBack  = 0;
    mVideoQueueLastRendered = 0;
    mVideoQueueSize  = 0;

    //for statistics profiling
    char value[PROPERTY_VALUE_MAX];
    mStatistics = false;
    property_get("persist.debug.sf.statistics", value, "0");
    if(atoi(value)) mStatistics = true;

    reset();
}

AwesomePlayer::~AwesomePlayer() {
    if (mQueueStarted) {
        mQueue.stop();
    }

    reset();
    delete [] mVideoBuffer;

    mClient.disconnect();
}

void AwesomePlayer::cancelPlayerEvents(bool keepBufferingGoing) {
    mQueue.cancelEvent(mVideoEvent->eventID());
    mVideoEventPending = false;
    mQueue.cancelEvent(mStreamDoneEvent->eventID());
    mStreamDoneEventPending = false;
    mQueue.cancelEvent(mCheckAudioStatusEvent->eventID());
    mAudioStatusEventPending = false;

    if (!keepBufferingGoing) {
        mQueue.cancelEvent(mBufferingEvent->eventID());
        mBufferingEventPending = false;
    }
}

void AwesomePlayer::setListener(const wp<MediaPlayerBase> &listener) {
    Mutex::Autolock autoLock(mLock);
    mListener = listener;
}

status_t AwesomePlayer::setDataSource(
        const char *uri, const KeyedVector<String8, String8> *headers) {
    Mutex::Autolock autoLock(mLock);
    return setDataSource_l(uri, headers);
}

status_t AwesomePlayer::setDataSource_l(
        const char *uri, const KeyedVector<String8, String8> *headers) {
    reset_l();

    mUri = uri;

    if (headers) {
        mUriHeaders = *headers;
    }

    // The actual work will be done during preparation in the call to
    // ::finishSetDataSource_l to avoid blocking the calling thread in
    // setDataSource for any significant time.

    return OK;
}

status_t AwesomePlayer::setDataSource(
        int fd, int64_t offset, int64_t length) {
    Mutex::Autolock autoLock(mLock);

    reset_l();

    sp<DataSource> dataSource = new FileSource(fd, offset, length);

    status_t err = dataSource->initCheck();

    if (err != OK) {
        return err;
    }

    mFileSource = dataSource;

    return setDataSource_l(dataSource);
}

status_t AwesomePlayer::setDataSource_l(
        const sp<DataSource> &dataSource) {
    sp<MediaExtractor> extractor = MediaExtractor::Create(dataSource);

    if (extractor == NULL) {
        return UNKNOWN_ERROR;
    }

    return setDataSource_l(extractor);
}

status_t AwesomePlayer::setDataSource_l(const sp<MediaExtractor> &extractor) {
    bool haveAudio = false;
    bool haveVideo = false;
    for (size_t i = 0; i < extractor->countTracks(); ++i) {
        sp<MetaData> meta = extractor->getTrackMetaData(i);

        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));

        if (!haveVideo && !strncasecmp(mime, "video/", 6)) {
            setVideoSource(extractor->getTrack(i));
            haveVideo = true;
        } else if (!haveAudio && !strncasecmp(mime, "audio/", 6)) {
            setAudioSource(extractor->getTrack(i));
            haveAudio = true;
        }

        if (haveAudio && haveVideo) {
            break;
        }
    }

    if (!haveAudio && !haveVideo) {
        return UNKNOWN_ERROR;
    }

    mExtractorFlags = extractor->flags();

    return OK;
}

void AwesomePlayer::reset() {
    Mutex::Autolock autoLock(mLock);
    reset_l();
}

void AwesomePlayer::reset_l() {
    if (mFlags & PREPARING) {
        mFlags |= PREPARE_CANCELLED;
        if (mConnectingDataSource != NULL) {
            LOGI("interrupting the connection process");
            mConnectingDataSource->disconnect();
        }
    }

    while (mFlags & PREPARING) {
        mPreparedCondition.wait(mLock);
    }

    cancelPlayerEvents();

    if (mStatistics && mVideoSource != NULL) {
        logStatistics();
        logSyncLoss();
    }

    if (mPrefetcher != NULL) {
        CHECK_EQ(mPrefetcher->getStrongCount(), 1);
    }
    if (mAudioPlayer != NULL) {
        mAudioPlayer->pause();
    }
    mPrefetcher.clear();

    mAudioTrack.clear();
    mVideoTrack.clear();

    // Shutdown audio first, so that the respone to the reset request
    // appears to happen instantaneously as far as the user is concerned
    // If we did this later, audio would continue playing while we
    // shutdown the video-related resources and the player appear to
    // not be as responsive to a reset request.
    if (mAudioPlayer == NULL && mAudioSource != NULL) {
        // If we had an audio player, it would have effectively
        // taken possession of the audio source and stopped it when
        // _it_ is stopped. Otherwise this is still our responsibility.
        mAudioSource->stop();
    }
    mAudioSource.clear();

    if (mTimeSource != mAudioPlayer) {
        delete mTimeSource;
    }
    delete mFallbackTimeSource;

    mTimeSource = mFallbackTimeSource = NULL;

    delete mAudioPlayer;
    mAudioPlayer = NULL;

    mVideoRenderer.clear();

    for (int i=0;i<BUFFER_QUEUE_CAPACITY;i++) {
        if (mVideoBuffer[i] != NULL) {
            mVideoBuffer[i]->release();
            mVideoBuffer[i] = NULL;
        }
    }
    mVideoQueueFront = 0;
    mVideoQueueBack  = 0;
    mVideoQueueLastRendered = 0;
    mVideoQueueSize  = 0;

    if (mVideoSource != NULL) {
        mVideoSource->stop();

        // The following hack is necessary to ensure that the OMX
        // component is completely released by the time we may try
        // to instantiate it again.
        wp<MediaSource> tmp = mVideoSource;
        mVideoSource.clear();
        while (tmp.promote() != NULL) {
            usleep(1000);
        }
        IPCThreadState::self()->flushCommands();
    }

    mDurationUs = -1;
    mFlags = 0;
    mExtractorFlags = 0;
    mVideoWidth = mVideoHeight = -1;
    mTimeSourceDeltaUs = 0;
    mVideoTimeUs = 0;

    mSeeking = false;
    mSeekNotificationSent = false;
    mSeekTimeUs = 0;

    mUri.setTo("");
    mUriHeaders.clear();

    mFileSource.clear();

    delete mSuspensionState;
    mSuspensionState = NULL;
}

void AwesomePlayer::notifyListener_l(int msg, int ext1, int ext2) {
    if (mListener != NULL) {
        sp<MediaPlayerBase> listener = mListener.promote();

        if (listener != NULL) {
            listener->sendEvent(msg, ext1, ext2);
        }
    }
}

void AwesomePlayer::onBufferingUpdate() {
    Mutex::Autolock autoLock(mLock);
    if (!mBufferingEventPending) {
        return;
    }
    mBufferingEventPending = false;

    int64_t durationUs;
    {
        Mutex::Autolock autoLock(mMiscStateLock);
        durationUs = mDurationUs;
    }

    if (durationUs >= 0) {
        int64_t cachedDurationUs = mPrefetcher->getMaxCachedDurationUs();

        LOGV("cache holds %.2f secs worth of data.", cachedDurationUs / 1E6);

        int64_t positionUs;
        getPosition(&positionUs);

        cachedDurationUs += positionUs;

        double percentage = (double)cachedDurationUs / durationUs;
        notifyListener_l(MEDIA_BUFFERING_UPDATE, percentage * 100.0);

        postBufferingEvent_l();
    } else {
        LOGE("Not sending buffering status because duration is unknown.");
    }
}

void AwesomePlayer::onStreamDone() {
    // Posted whenever any stream finishes playing.

    Mutex::Autolock autoLock(mLock);
    if (!mStreamDoneEventPending) {
        return;
    }
    mStreamDoneEventPending = false;

    if (mStreamDoneStatus == ERROR_END_OF_STREAM && (mFlags & LOOPING)) {
        seekTo_l(0);

        if (mVideoSource != NULL) {
            postVideoEvent_l();
            if (mStatistics) {
                logStatistics();
                logSyncLoss();
            }
        }
    } else {
        if (mStreamDoneStatus == ERROR_END_OF_STREAM) {
            LOGV("MEDIA_PLAYBACK_COMPLETE");
            notifyListener_l(MEDIA_PLAYBACK_COMPLETE);
        } else {
            LOGV("MEDIA_ERROR %d", mStreamDoneStatus);

            notifyListener_l(
                    MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, mStreamDoneStatus);
        }

        pause_l();

        mFlags |= AT_EOS;
    }
}

status_t AwesomePlayer::play() {
    Mutex::Autolock autoLock(mLock);
    return play_l();
}

status_t AwesomePlayer::play_l() {
    if (mFlags & PLAYING) {
        return OK;
    }

    if (!(mFlags & PREPARED)) {
        status_t err = prepare_l();

        if (err != OK) {
            return err;
        }
    }

    mFlags |= PLAYING;
    mFlags |= FIRST_FRAME;

    bool deferredAudioSeek = false;

    if (mAudioSource != NULL) {
        if (mAudioPlayer == NULL) {
            if (mAudioSink != NULL) {
                mAudioPlayer = new AudioPlayer(mAudioSink);
                mAudioPlayer->setSource(mAudioSource);

                // We've already started the MediaSource in order to enable
                // the prefetcher to read its data.
                status_t err = mAudioPlayer->start(
                        true /* sourceAlreadyStarted */);

                if (err != OK) {
                    delete mAudioPlayer;
                    mAudioPlayer = NULL;

                    mFlags &= ~(PLAYING | FIRST_FRAME);

                    return err;
                }
                if( mVideoSource != NULL && mSeeking){
                   LOGV("play_l() Pause AudioPlayer till video reaches a sync frame");
                   mAudioPlayer->pause();
                   mPauseAudioTillSync = true;
                }

                delete mTimeSource;
                delete mFallbackTimeSource;
                mTimeSource = mAudioPlayer;
                mFallbackTimeSource = new SystemTimeSource;

                deferredAudioSeek = true;

                mWatchForAudioSeekComplete = false;
                mWatchForAudioEOS = true;
            }
        } else {
            mAudioPlayer->resume();
        }

        postCheckAudioStatusEvent_l();
    }

    if (mTimeSource == NULL && mAudioPlayer == NULL) {
        mTimeSource = new SystemTimeSource;
        /*
            Fallback timer isn't needed because for video only clip
            we don't need to worry about audio ending early or the
            system timer not being available
        */
        mFallbackTimeSource = NULL;
    }

    if (mStatistics) {
        mFirstFrameLatencyStartUs = getTimeOfDayUs();
        mVeryFirstFrame = true;
    }

    if (mVideoSource != NULL) {
        // Kick off video playback
        postVideoEvent_l();
    }

    if (deferredAudioSeek) {
        // If there was a seek request while we were paused
        // and we're just starting up again, honor the request now.
        seekAudioIfNecessary_l();
    }

    if (mFlags & AT_EOS) {
        // Legacy behaviour, if a stream finishes playing and then
        // is started again, we play from the start...
        seekTo_l(0);
    }

    return OK;
}

void AwesomePlayer::initRenderer_l() {
    if (mISurface != NULL) {
        sp<MetaData> meta = mVideoSource->getFormat();

        int32_t format;
        const char *component;
        int32_t decodedWidth = 0, decodedHeight = 0, rotation = 0;
        CHECK(meta->findInt32(kKeyColorFormat, &format));
        CHECK(meta->findCString(kKeyDecoderComponent, &component));
        //Update width, height stride and slice height from metadata
        //and use this to create renderer
        CHECK(meta->findInt32(kKeyWidth, &mVideoWidth));
        CHECK(meta->findInt32(kKeyHeight, &mVideoHeight));
        //Software decoder doesnot use stride and slice height
        //Update decode width and height to width and height
        // if the stride or slice height returned from decoder
        // is zero.
        if (!(meta->findInt32(kKeyStride, &decodedWidth)))
            decodedWidth = mVideoWidth;
        if(!(meta->findInt32(kKeySliceHeight, &decodedHeight)))
            decodedHeight = mVideoHeight;

        if( meta->findInt32(kKeyRotation, &rotation ) == false ){
          LOGV("Rotation information not present in metadata");
          rotation = 0;
        }

        //temporarily setting flags to 0.
        //GPU composition flag will come in
        //in the meta structure.
        int32_t flags = 0;
        mVideoRenderer.clear();

        // Must ensure that mVideoRenderer's destructor is actually executed
        // before creating a new one.
        IPCThreadState::self()->flushCommands();

        if (!strncmp("OMX.", component, 4)) {
            // Our OMX codecs allocate buffers on the media_server side
            // therefore they require a remote IOMXRenderer that knows how
            // to display them.
            mVideoRenderer = new AwesomeRemoteRenderer(
                mClient.interface()->createRenderer(
                        mISurface, component,
                        (OMX_COLOR_FORMATTYPE)format,
                        decodedWidth, decodedHeight,
                        mVideoWidth, mVideoHeight,
                        rotation, flags ));
        } else {
            // Other decoders are instantiated locally and as a consequence
            // allocate their buffers in local address space.
            mVideoRenderer = new AwesomeLocalRenderer(
                false,  // previewOnly
                component,
                (OMX_COLOR_FORMATTYPE)format,
                mISurface,
                mVideoWidth, mVideoHeight,
                decodedWidth, decodedHeight); //no rotation/flags being passed for now
        }
    }
}

status_t AwesomePlayer::pause() {
    Mutex::Autolock autoLock(mLock);
    return pause_l();
}

status_t AwesomePlayer::pause_l() {
    if (!(mFlags & PLAYING)) {
        return OK;
    }

    cancelPlayerEvents(true /* keepBufferingGoing */);

    if (mAudioPlayer != NULL) {
        mAudioPlayer->pause();
    }

    mFlags &= ~PLAYING;

    if(mStatistics && !(mFlags & AT_EOS)) logPause();
    return OK;
}

bool AwesomePlayer::isPlaying() const {
    return mFlags & PLAYING;
}

void AwesomePlayer::setISurface(const sp<ISurface> &isurface) {
    Mutex::Autolock autoLock(mLock);

    mISurface = isurface;
}

void AwesomePlayer::setAudioSink(
        const sp<MediaPlayerBase::AudioSink> &audioSink) {
    Mutex::Autolock autoLock(mLock);

    mAudioSink = audioSink;
}

status_t AwesomePlayer::setLooping(bool shouldLoop) {
    Mutex::Autolock autoLock(mLock);

    mFlags = mFlags & ~LOOPING;

    if (shouldLoop) {
        mFlags |= LOOPING;
    }

    return OK;
}

status_t AwesomePlayer::getDuration(int64_t *durationUs) {
    Mutex::Autolock autoLock(mMiscStateLock);

    if (mDurationUs < 0) {
        return UNKNOWN_ERROR;
    }

    *durationUs = mDurationUs;

    return OK;
}

status_t AwesomePlayer::getPosition(int64_t *positionUs) {
    /*
        In determining the position, use the video source's
        position whenever possible, unless the video track
        is shorter than the audio track.

        The conditions below assume that mVideoEOSOccurred !=
        mAudioEOSOccurred since if both EOSes have occurred,
        the playback would have ended.

        For the second "else if" we don't need to check if
        Audio EOS occurred because:
            a) if we have video, we'll always use video's
               time. If we don't have video then,
               notifyStreamDone() will have been triggered
               and playback would have ended by now
            b) if video EOS has occurred then audio EOS
               hasn't occurred. Otherwise we wouldn't be
               here.
    */
    if (mSeeking) {
        *positionUs = mSeekTimeUs;
    } else if (mVideoSource != NULL && !mVideoEOSOccurred) {
        Mutex::Autolock autoLock(mMiscStateLock);
        *positionUs = mVideoTimeUs;
    } else if (mAudioPlayer != NULL) {
        *positionUs = mAudioPlayer->getMediaTimeUs();
    } else {
        *positionUs = 0;
    }

    return OK;
}

status_t AwesomePlayer::seekTo(int64_t timeUs) {
    if (mExtractorFlags
            & (MediaExtractor::CAN_SEEK_FORWARD
                | MediaExtractor::CAN_SEEK_BACKWARD)) {
        Mutex::Autolock autoLock(mLock);
        return seekTo_l(timeUs);
    }

    return OK;
}

status_t AwesomePlayer::seekTo_l(int64_t timeUs) {
    mSeeking = true;
    if (mStatistics) mFirstFrameLatencyStartUs = getTimeOfDayUs();
    mSeekNotificationSent = false;
    mSeekTimeUs = timeUs;
    mFlags &= ~AT_EOS;

    //Reset the Audio EOS occurred flag unless
    //we are still past the EOS even after the seek
    if (timeUs < mAudioDurationUs)
        mAudioEOSOccurred = false;

    //Ditto for Video EOS
    if (timeUs < mVideoDurationUs)
        mVideoEOSOccurred = false;

    seekAudioIfNecessary_l();
    if (!(mFlags & PLAYING)) {
        LOGV("seeking while paused, sending SEEK_COMPLETE notification"
             " immediately.");

        notifyListener_l(MEDIA_SEEK_COMPLETE);
        mSeekNotificationSent = true;
    }

    return OK;
}

void AwesomePlayer::seekAudioIfNecessary_l() {
    if (mSeeking && mAudioPlayer != NULL && !mAudioEOSOccurred) {
        mAudioPlayer->seekTo(mSeekTimeUs);
        mWatchForAudioSeekComplete = true;
        mWatchForAudioEOS = true;
        mSeekNotificationSent = false;
    }
}

status_t AwesomePlayer::getVideoDimensions(
        int32_t *width, int32_t *height) const {
    Mutex::Autolock autoLock(mLock);

    if (mVideoWidth < 0 || mVideoHeight < 0) {
        return UNKNOWN_ERROR;
    }

    *width = mVideoWidth;
    *height = mVideoHeight;

    return OK;
}

void AwesomePlayer::setAudioSource(sp<MediaSource> source) {
    CHECK(source != NULL);

    if (mPrefetcher != NULL) {
        source = mPrefetcher->addSource(source);
    }

    mAudioTrack = source;
}

status_t AwesomePlayer::initAudioDecoder() {
    sp<MetaData> meta = mAudioTrack->getFormat();

    const char *mime;
    CHECK(meta->findCString(kKeyMIMEType, &mime));

    if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW)) {
        mAudioSource = mAudioTrack;
    } else {
        uint32_t flags = 0;
        if(mVideoTrack != NULL) {
             const char *videoMimeType;
             sp<MetaData> videoMeta = mVideoTrack->getFormat();

             CHECK(videoMeta->findCString(kKeyMIMEType, &videoMimeType));

             //use software audio decoder for DIVX/AVI+MP3 format
             if ((!strcasecmp(videoMimeType, MEDIA_MIMETYPE_CONTAINER_AVI) ||
                  !strcasecmp(videoMimeType, MEDIA_MIMETYPE_VIDEO_DIVX)) &&
                  !strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_MPEG))
                  flags = OMXCodec::kPreferSoftwareCodecs;
        }
        mAudioSource = OMXCodec::Create(
                mClient.interface(), mAudioTrack->getFormat(),
                false, // createEncoder
                mAudioTrack, NULL, mCodecFlags | flags);
    }

    if (mAudioSource != NULL) {
        int64_t durationUs;
        if (mAudioTrack->getFormat()->findInt64(kKeyDuration, &durationUs)) {
            Mutex::Autolock autoLock(mMiscStateLock);
            if (mDurationUs < 0 || durationUs > mDurationUs) {
                mDurationUs = durationUs;
            }
            mAudioDurationUs = durationUs;
        }

        status_t err = mAudioSource->start();

        if (err != OK) {
            mAudioSource.clear();
            return err;
        }
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_QCELP)) {
        // For legacy reasons we're simply going to ignore the absence
        // of an audio decoder for QCELP instead of aborting playback
        // altogether.
        return OK;
    }

    return mAudioSource != NULL ? OK : UNKNOWN_ERROR;
}

void AwesomePlayer::setVideoSource(sp<MediaSource> source) {
    CHECK(source != NULL);

    if (mPrefetcher != NULL) {
        source = mPrefetcher->addSource(source);
    }

    mVideoTrack = source;
}

status_t AwesomePlayer::initVideoDecoder() {
    mVideoSource = OMXCodec::Create(
            mClient.interface(), mVideoTrack->getFormat(),
            false, // createEncoder
            mVideoTrack, NULL, mCodecFlags);

    if (mVideoSource != NULL) {
        int64_t durationUs;
        if (mVideoTrack->getFormat()->findInt64(kKeyDuration, &durationUs)) {
            Mutex::Autolock autoLock(mMiscStateLock);
            if (mDurationUs < 0 || durationUs > mDurationUs) {
                mDurationUs = durationUs;
            }
        }

        CHECK(mVideoTrack->getFormat()->findInt32(kKeyWidth, &mVideoWidth));
        CHECK(mVideoTrack->getFormat()->findInt32(kKeyHeight, &mVideoHeight));

        status_t err = mVideoSource->start();

        if (err != OK) {
            mVideoSource.clear();
            return err;
        }
    }

    return mVideoSource != NULL ? OK : UNKNOWN_ERROR;
}

void AwesomePlayer::onVideoEvent() {
    Mutex::Autolock autoLock(mLock);
    if (!mVideoEventPending) {
        // The event has been cancelled in reset_l() but had already
        // been scheduled for execution at that time.
        return;
    }
    mVideoEventPending = false;

    if (mVideoEOSOccurred && !mAudioEOSOccurred)
    {
        //Continue polling to see if Video EOS went away due to seek
        postVideoEvent_l();
        return;
    }

    if (mSeeking) {
        for(int i=0;i<BUFFER_QUEUE_CAPACITY;i++){
            if (mVideoBuffer[i] != NULL) {
                mVideoBuffer[i]->release();
                mVideoBuffer[i] = NULL;
            }
        }
        mVideoQueueFront = 0;
        mVideoQueueBack  = 0;
        mVideoQueueLastRendered = 0;
        mVideoQueueSize  = 0;
    }

    if (mVideoBuffer[mVideoQueueBack] == NULL) {
        MediaSource::ReadOptions options;
        if (mSeeking) {
            LOGV("seeking to %lld us (%.2f secs)", mSeekTimeUs, mSeekTimeUs / 1E6);

            options.setSeekTo(mSeekTimeUs);
        }
        for (;;) {
            status_t err = mVideoSource->read(&mVideoBuffer[mVideoQueueBack], &options);
            options.clearSeekTo();

            if (err != OK) {
                CHECK_EQ(mVideoBuffer[mVideoQueueBack], NULL);

                if (err == INFO_FORMAT_CHANGED) {
                    LOGV("VideoSource signalled format change.");

                    if (mVideoRenderer != NULL) {
                        mVideoRendererIsPreview = false;
                        initRenderer_l();
                    }
                    continue;
                }

                mVideoEOSOccurred = true;
                mVideoDurationUs = mVideoTimeUs;

                if (mAudioEOSOccurred || mAudioSource == NULL)
                    postStreamDoneEvent_l(err);
                else
                {
                    //Seek audio in case we happened reach video EOS
                    //and audio hasn't had a chance to seek
                    seekAudioIfNecessary_l();

                    //Check if audio was paused and resume
                    if( mAudioPlayer != NULL && mPauseAudioTillSync ) {
                        LOGV("onVideoEvent, video eos occured, resume paused audio");
                        mAudioPlayer->resume();
                        mPauseAudioTillSync = false;
                    }

                    //Post a video event to start polling
                    //if video EOS went away due to seek
                    postVideoEvent_l();
                }
                return;
            }

            if (mVideoBuffer[mVideoQueueBack]->range_length() == 0) {
                // Some decoders, notably the PV AVC software decoder
                // return spurious empty buffers that we just want to ignore.

                mVideoBuffer[mVideoQueueBack]->release();
                mVideoBuffer[mVideoQueueBack] = NULL;
                continue;
            }

            break;
        }
    }

    int64_t timeUs;
    CHECK(mVideoBuffer[mVideoQueueBack]->meta_data()->findInt64(kKeyTime, &timeUs));

    {
        Mutex::Autolock autoLock(mMiscStateLock);
        mVideoTimeUs = timeUs;
    }

    if (mSeeking) {
        if (mAudioPlayer != NULL && !mAudioEOSOccurred) {
            LOGV("seeking audio to %lld us (%.2f secs).", timeUs, timeUs / 1E6);

            mAudioPlayer->seekTo(timeUs);
            mWatchForAudioSeekComplete = true;
            mWatchForAudioEOS = true;
        } else if (!mSeekNotificationSent) {
            // If we're playing video only, report seek complete now,
            // otherwise audio player will notify us later.
            notifyListener_l(MEDIA_SEEK_COMPLETE);
        }

        mFlags |= FIRST_FRAME;
        mSeeking = false;
        mSeekNotificationSent = false;
        if (mStatistics) logSeek();
    }
    //Check if audio was paused and resume
    if( mAudioPlayer != NULL && mPauseAudioTillSync ) {
        LOGV("onVideoEvent, resume paused audio at video sync");
        mAudioPlayer->resume();
        mPauseAudioTillSync = false;
    }

    if (mFlags & FIRST_FRAME) {
        mFlags &= ~FIRST_FRAME;
        mTimeSourceDeltaUs =(mAudioEOSOccurred ? mFallbackTimeSource : mTimeSource)->getRealTimeUs() - timeUs;
        setNumFramesToHold();
        if (mStatistics && mVeryFirstFrame) logFirstFrame();
    }

    int64_t realTimeUs, mediaTimeUs;
    if (mAudioPlayer != NULL
        && !mAudioEOSOccurred
        && mAudioPlayer->getMediaTimeMapping(&realTimeUs, &mediaTimeUs)) {
        mTimeSourceDeltaUs = realTimeUs - mediaTimeUs;
    }

    int64_t nowUs = (mAudioEOSOccurred ? mFallbackTimeSource : mTimeSource)->getRealTimeUs() - mTimeSourceDeltaUs;
    LOGV("using %s timesource", mAudioEOSOccurred ? "fallback" : "original");

    int64_t latenessUs = nowUs - timeUs;

    if (latenessUs > 200000) {
        // We're more than 200ms late.
        LOGV("we're late by %lld us (%.2f secs)", latenessUs, latenessUs / 1E6);

        mVideoBuffer[mVideoQueueBack]->release();
        mVideoBuffer[mVideoQueueBack] = NULL;

        if (mStatistics) {
            mFramesDropped++;
            mConsecutiveFramesDropped++;
            if (mConsecutiveFramesDropped == 1) {
                mCatchupTimeStart = mTimeSource->getRealTimeUs();
            }
            if (!(mFlags & AT_EOS)) logLate(timeUs,nowUs,latenessUs);
        }
        postVideoEvent_l();
        return;
    }

    if (latenessUs < -50000) {
        // We're more than 50ms early.
        if (mStatistics) {
            logOnTime(timeUs,nowUs,latenessUs);
            mConsecutiveFramesDropped = 0;
        }
        postVideoEvent_l(10000);
        return;
    }

    if (mVideoRendererIsPreview || mVideoRenderer == NULL) {
        mVideoRendererIsPreview = false;

        initRenderer_l();
    }

    if (mVideoRenderer != NULL) {
        mVideoRenderer->render(mVideoBuffer[mVideoQueueBack]);
        if (mStatistics) {
            mTotalFrames++;
            logOnTime(timeUs,nowUs,latenessUs);
            mConsecutiveFramesDropped = 0;
        }
    }

    mVideoQueueLastRendered = mVideoQueueBack;
    mVideoQueueBack = (++mVideoQueueBack)%(BUFFER_QUEUE_CAPACITY);
    mVideoQueueSize++;
    if (mVideoQueueSize > mNumFramesToHold) {
        mVideoBuffer[mVideoQueueFront]->release();
        mVideoBuffer[mVideoQueueFront] = NULL;
        mVideoQueueFront = (++mVideoQueueFront)%(BUFFER_QUEUE_CAPACITY);
        mVideoQueueSize--;
    }

    postVideoEvent_l();
}

void AwesomePlayer::postVideoEvent_l(int64_t delayUs) {
    if (mVideoEventPending) {
        return;
    }

    mVideoEventPending = true;
    mQueue.postEventWithDelay(mVideoEvent, delayUs < 0 ? 10000 : delayUs);
}

void AwesomePlayer::postStreamDoneEvent_l(status_t status) {
    if (mStreamDoneEventPending) {
        return;
    }
    mStreamDoneEventPending = true;

    mStreamDoneStatus = status;
    mQueue.postEvent(mStreamDoneEvent);
}

void AwesomePlayer::postBufferingEvent_l() {
    if (mPrefetcher == NULL) {
        return;
    }

    if (mBufferingEventPending) {
        return;
    }
    mBufferingEventPending = true;
    mQueue.postEventWithDelay(mBufferingEvent, 1000000ll);
}

void AwesomePlayer::postCheckAudioStatusEvent_l() {
    if (mAudioStatusEventPending) {
        return;
    }
    mAudioStatusEventPending = true;
    mQueue.postEventWithDelay(mCheckAudioStatusEvent, 100000ll);
}

void AwesomePlayer::onCheckAudioStatus() {
    Mutex::Autolock autoLock(mLock);
    if (!mAudioStatusEventPending) {
        // Event was dispatched and while we were blocking on the mutex,
        // has already been cancelled.
        return;
    }

    mAudioStatusEventPending = false;

    if (mWatchForAudioSeekComplete && !mAudioPlayer->isSeeking()) {
        mWatchForAudioSeekComplete = false;

        if (!mSeekNotificationSent) {
            notifyListener_l(MEDIA_SEEK_COMPLETE);
            mSeekNotificationSent = true;
        }

        mSeeking = false;
    }

    status_t finalStatus;
    if (mWatchForAudioEOS && mAudioPlayer->reachedEOS(&finalStatus)) {
        mWatchForAudioEOS = false;
        mAudioEOSOccurred = true;

        //Pretend it is the first frame so we can reset the sync point
        mFlags |= FIRST_FRAME;

        if (mVideoEOSOccurred || mVideoSource == NULL)
            postStreamDoneEvent_l(finalStatus);
    }

    postCheckAudioStatusEvent_l();
}

status_t AwesomePlayer::prepare() {
    Mutex::Autolock autoLock(mLock);
    return prepare_l();
}

status_t AwesomePlayer::prepare_l() {
    if (mFlags & PREPARED) {
        return OK;
    }

    if (mFlags & PREPARING) {
        return UNKNOWN_ERROR;
    }

    mIsAsyncPrepare = false;
    status_t err = prepareAsync_l();

    if (err != OK) {
        return err;
    }

    while (mFlags & PREPARING) {
        mPreparedCondition.wait(mLock);
    }

    return mPrepareResult;
}

status_t AwesomePlayer::prepareAsync() {
    Mutex::Autolock autoLock(mLock);

    if (mFlags & PREPARING) {
        return UNKNOWN_ERROR;  // async prepare already pending
    }

    mIsAsyncPrepare = true;
    return prepareAsync_l();
}

status_t AwesomePlayer::prepareAsync_l() {
    if (mFlags & PREPARING) {
        return UNKNOWN_ERROR;  // async prepare already pending
    }

    if (!mQueueStarted) {
        mQueue.start();
        mQueueStarted = true;
    }

    mFlags |= PREPARING;
    mAsyncPrepareEvent = new AwesomeEvent(
            this, &AwesomePlayer::onPrepareAsyncEvent);

    mQueue.postEvent(mAsyncPrepareEvent);

    return OK;
}

status_t AwesomePlayer::finishSetDataSource_l() {
    sp<DataSource> dataSource;

    if (!strncasecmp("http://", mUri.string(), 7)) {
        mConnectingDataSource = new HTTPDataSource(mUri, &mUriHeaders);

        mLock.unlock();
        status_t err = mConnectingDataSource->connect();
        mLock.lock();

        if (err != OK) {
            mConnectingDataSource.clear();

            LOGI("mConnectingDataSource->connect() returned %d", err);
            return err;
        }

        dataSource = new CachingDataSource(
                mConnectingDataSource, 64 * 1024, 10);

        mConnectingDataSource.clear();
    } else {
        dataSource = DataSource::CreateFromURI(mUri.string(), &mUriHeaders);
    }

    if (dataSource == NULL) {
        return UNKNOWN_ERROR;
    }

    sp<MediaExtractor> extractor = MediaExtractor::Create(dataSource);

    if (extractor == NULL) {
        return UNKNOWN_ERROR;
    }

    if (dataSource->flags() & DataSource::kWantsPrefetching) {
        mPrefetcher = new Prefetcher;
    }

    return setDataSource_l(extractor);
}

void AwesomePlayer::abortPrepare(status_t err) {
    CHECK(err != OK);

    if (mIsAsyncPrepare) {
        notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, err);
    }

    mPrepareResult = err;
    mFlags &= ~(PREPARING|PREPARE_CANCELLED);
    mAsyncPrepareEvent = NULL;
    mPreparedCondition.broadcast();
}

// static
bool AwesomePlayer::ContinuePreparation(void *cookie) {
    AwesomePlayer *me = static_cast<AwesomePlayer *>(cookie);

    return (me->mFlags & PREPARE_CANCELLED) == 0;
}

void AwesomePlayer::onPrepareAsyncEvent() {
    sp<Prefetcher> prefetcher;

    {
        Mutex::Autolock autoLock(mLock);

        if (mFlags & PREPARE_CANCELLED) {
            LOGI("prepare was cancelled before doing anything");
            abortPrepare(UNKNOWN_ERROR);
            return;
        }

        if (mUri.size() > 0) {
            status_t err = finishSetDataSource_l();

            if (err != OK) {
                abortPrepare(err);
                return;
            }
        }

        if (mVideoTrack != NULL && mVideoSource == NULL) {
            status_t err = initVideoDecoder();

            if (err != OK) {
                abortPrepare(err);
                return;
            }
        }

        if (mAudioTrack != NULL && mAudioSource == NULL) {
            status_t err = initAudioDecoder();

            if (err != OK) {
                abortPrepare(err);
                return;
            }
        }

        prefetcher = mPrefetcher;
    }

    if (prefetcher != NULL) {
        {
            Mutex::Autolock autoLock(mLock);
            if (mFlags & PREPARE_CANCELLED) {
                LOGI("prepare was cancelled before preparing the prefetcher");

                prefetcher.clear();
                abortPrepare(UNKNOWN_ERROR);
                return;
            }
        }

        LOGI("calling prefetcher->prepare()");
        status_t result =
            prefetcher->prepare(&AwesomePlayer::ContinuePreparation, this);

        prefetcher.clear();

        if (result == OK) {
            LOGI("prefetcher is done preparing");
        } else {
            Mutex::Autolock autoLock(mLock);

            CHECK_EQ(result, -EINTR);

            LOGI("prefetcher->prepare() was cancelled early.");
            abortPrepare(UNKNOWN_ERROR);
            return;
        }
    }

    Mutex::Autolock autoLock(mLock);

    if (mIsAsyncPrepare) {
        if (mVideoWidth < 0 || mVideoHeight < 0) {
            notifyListener_l(MEDIA_SET_VIDEO_SIZE, 0, 0);
        } else {
            notifyListener_l(MEDIA_SET_VIDEO_SIZE, mVideoWidth, mVideoHeight);
        }

        notifyListener_l(MEDIA_PREPARED);
    }

    mPrepareResult = OK;
    mFlags &= ~(PREPARING|PREPARE_CANCELLED);
    mFlags |= PREPARED;
    mAsyncPrepareEvent = NULL;
    mPreparedCondition.broadcast();

    postBufferingEvent_l();
}

status_t AwesomePlayer::suspend() {
    LOGV("suspend");
    Mutex::Autolock autoLock(mLock);

    if (mSuspensionState != NULL) {
        if (mVideoBuffer[mVideoQueueLastRendered] == NULL) {
            //go into here if video is suspended again
            //after resuming without being played between
            //them
            SuspensionState *state = mSuspensionState;
            mSuspensionState = NULL;
            reset_l();
            mSuspensionState = state;
            return OK;
        }

        delete mSuspensionState;
        mSuspensionState = NULL;
    }

    if (mFlags & PREPARING) {
        mFlags |= PREPARE_CANCELLED;
        if (mConnectingDataSource != NULL) {
            LOGI("interrupting the connection process");
            mConnectingDataSource->disconnect();
        }
    }

    while (mFlags & PREPARING) {
        mPreparedCondition.wait(mLock);
    }

    SuspensionState *state = new SuspensionState;
    state->mUri = mUri;
    state->mUriHeaders = mUriHeaders;
    state->mFileSource = mFileSource;

    state->mFlags = mFlags & (PLAYING | LOOPING | AT_EOS);
    getPosition(&state->mPositionUs);

    if (mVideoBuffer[mVideoQueueLastRendered] != NULL) {
        size_t size = mVideoBuffer[mVideoQueueLastRendered]->range_length();
        if (size) {
            state->mLastVideoFrameSize = size;
            state->mLastVideoFrame = malloc(size);
            memcpy(state->mLastVideoFrame,
                   (const uint8_t *)mVideoBuffer[mVideoQueueLastRendered]->data()
                        + mVideoBuffer[mVideoQueueLastRendered]->range_offset(),
                   size);

            state->mVideoWidth = mVideoWidth;
            state->mVideoHeight = mVideoHeight;

            sp<MetaData> meta = mVideoSource->getFormat();
            CHECK(meta->findInt32(kKeyColorFormat, &state->mColorFormat));
            //Update the decode width and height using stride and slice
            //height key value pair.Fall back to width and height if
            //the stride or slice height returned from decoder is zero.
            //Software decoder doesnot use stride and slice height.
            if(!(meta->findInt32(kKeyStride, &state->mDecodedWidth)))
                state->mDecodedWidth = mVideoWidth;
            if(!(meta->findInt32(kKeySliceHeight, &state->mDecodedHeight)))
                state->mDecodedHeight = mVideoHeight;
        }
    }

    reset_l();

    mSuspensionState = state;

    return OK;
}

status_t AwesomePlayer::resume() {
    LOGV("resume");
    Mutex::Autolock autoLock(mLock);

    if (mSuspensionState == NULL) {
        return INVALID_OPERATION;
    }

    SuspensionState *state = mSuspensionState;
    mSuspensionState = NULL;

    status_t err;
    if (state->mFileSource != NULL) {
        err = setDataSource_l(state->mFileSource);

        if (err == OK) {
            mFileSource = state->mFileSource;
        }
    } else {
        err = setDataSource_l(state->mUri, &state->mUriHeaders);
    }

    if (err != OK) {
        delete state;
        state = NULL;

        return err;
    }

    seekTo_l(state->mPositionUs);

    mFlags = state->mFlags & (LOOPING | AT_EOS);

    if (state->mLastVideoFrame && mISurface != NULL) {
        mVideoRenderer =
            new AwesomeLocalRenderer(
                    true,  // previewOnly
                    "",
                    (OMX_COLOR_FORMATTYPE)state->mColorFormat,
                    mISurface,
                    state->mVideoWidth,
                    state->mVideoHeight,
                    state->mDecodedWidth,
                    state->mDecodedHeight);

        mVideoRendererIsPreview = true;

        ((AwesomeLocalRenderer *)mVideoRenderer.get())->render(
                state->mLastVideoFrame, state->mLastVideoFrameSize);
    }

    if (state->mFlags & PLAYING) {
        play_l();
    }

    mSuspensionState = state;
    state = NULL;

    return OK;
}

uint32_t AwesomePlayer::flags() const {
    return mExtractorFlags;
}

void AwesomePlayer::setNumFramesToHold() {
#if 0
    char value1[128],value2[128];
    property_get("ro.product.device",value1,"0");
    property_get("hw.hdmiON", value2, "0");

    // set value of mNumFramesToHold to 2 for targets 8250,8650A,8660, 7x27
    // set value of mNumFramesToHold to 2 for 7x30 only if HDMI is on and its not 720p playback
    // or if GPU composition is enabled
    if(strcmp("qsd8250_surf",value1) == 0 ||
       strcmp("qsd8250_ffa",value1) == 0  ||
       strcmp("qsd8650a_st1x",value1) == 0||
       strcmp("msm7627_7x_surf",value1) == 0||
       strcmp("msm7627_7x_ffa",value1) == 0||
       strncmp("msm8660",value1,strlen("msm8660")) == 0 ||
       (strcmp("msm7630_surf",value1) == 0 &&
           ((atoi(value2) && (!(mVideoWidth == 1280 && mVideoHeight == 720))) ||
               (mCodecFlags & OMXCodec::kEnableGPUComposition))))
        mNumFramesToHold = 2;
    else
        mNumFramesToHold = 1;
#endif
    //Number of frames to hold made to 2 to resolve flicker during
    //multiple instances of video playback
    mNumFramesToHold = 2;
}

// Trim both leading and trailing whitespace from the given string.
static void TrimString(String8 *s) {
    size_t num_bytes = s->bytes();
    const char *data = s->string();

    size_t leading_space = 0;
    while (leading_space < num_bytes && isspace(data[leading_space])) {
        ++leading_space;
    }

    size_t i = num_bytes;
    while (i > leading_space && isspace(data[i - 1])) {
        --i;
    }

    s->setTo(String8(&data[leading_space], i - leading_space));
}

status_t AwesomePlayer::setParameter(const String8& key, const String8& value) {
    if (key == "gpu-composition") {
        LOGV("setParameter : gpu-composition : key = %s value = %s\n", key.string(), value.string());
        int enableGPU = 0;
        enableGPU = atoi(value.string());
        LOGV("setParameter : gpu-composition : %d \n", enableGPU);
        if (enableGPU) {
            mCodecFlags |= OMXCodec::kEnableGPUComposition;
            LOGV("GPU composition flag set");
        }
    }
    return OK;
}

status_t AwesomePlayer::setParameters(const String8& params) {
    LOGV("setParameters(%s)", params.string());
    status_t ret = OK;
    const char *key_start = params;
    for (;;) {
        const char *equal_pos = strchr(key_start, '=');
        if (equal_pos == NULL) {
            // This key is missing a value.
            ret = UNKNOWN_ERROR;
            break;
        }

        String8 key(key_start, equal_pos - key_start);
        TrimString(&key);

        if (key.length() == 0) {
            ret = UNKNOWN_ERROR;
            break;
        }
        LOGV("setParameters key = %s\n", key.string());

        const char *value_start = equal_pos + 1;
        const char *semicolon_pos = strchr(value_start, ';');
        String8 value;
        if (semicolon_pos == NULL) {
            value.setTo(value_start);
            LOGV("setParameters value = %s\n", value.string());
        } else {
            value.setTo(value_start, semicolon_pos - value_start);
            LOGV("setParameters semicolon value = %s\n", value.string());
        }

        ret = setParameter(key, value);

        if (ret != OK) {
           LOGE("setParameter(%s = %s) failed with result %d",
                 key.string(), value.string(), ret);
           break;
        }

        if (semicolon_pos == NULL) {
            break;
        }

        key_start = semicolon_pos + 1;
    }

    if (ret != OK) {
        LOGE("Ln %d setParameters(\"%s\") error", __LINE__, params.string());
    }

    return ret;
}

//Statistics profiling
void AwesomePlayer::logStatistics() {
    const char *mime;
    mVideoTrack->getFormat()->findCString(kKeyMIMEType, &mime);
    LOGW("=====================================================");
    if (mFlags & LOOPING) {LOGW("Looping Update");}
    LOGW("Mime Type: %s",mime);
    LOGW("Clip duration: %lld ms",mDurationUs/1000);
    LOGW("Number of frames dropped: %u",mFramesDropped);
    LOGW("Number of frames rendered: %u",mTotalFrames);
    LOGW("=====================================================");
}

inline void AwesomePlayer::logFirstFrame() {
    LOGW("=====================================================");
    LOGW("First frame latency: %lld ms",(getTimeOfDayUs()-mFirstFrameLatencyStartUs)/1000);
    LOGW("=====================================================");
    mVeryFirstFrame = false;
}

inline void AwesomePlayer::logSeek() {
    LOGW("=====================================================");
    LOGW("Seek position: %lld ms",mSeekTimeUs/1000);
    LOGW("Seek latency: %lld ms",(getTimeOfDayUs()-mFirstFrameLatencyStartUs)/1000);
    LOGW("=====================================================");
}

inline void AwesomePlayer::logPause() {
    LOGW("=====================================================");
    LOGW("Pause position: %lld ms",mVideoTimeUs/1000);
    LOGW("=====================================================");
}

inline void AwesomePlayer::logCatchUp(int64_t ts, int64_t clock, int64_t delta)
{
    if (mConsecutiveFramesDropped > 0) {
        mNumTimesSyncLoss++;
        if (mMaxTimeSyncLoss < (clock - mCatchupTimeStart) && clock > 0 && ts > 0) {
            mMaxTimeSyncLoss = clock - mCatchupTimeStart;
        }
    }
}

inline void AwesomePlayer::logLate(int64_t ts, int64_t clock, int64_t delta)
{
    if (mMaxLateDelta < delta && clock > 0 && ts > 0) {
        mMaxLateDelta = delta;
    }
}

inline void AwesomePlayer::logOnTime(int64_t ts, int64_t clock, int64_t delta)
{
    logCatchUp(ts, clock, delta);
    if (delta <= 0) {
        if ((-delta) > (-mMaxEarlyDelta) && clock > 0 && ts > 0) {
            mMaxEarlyDelta = delta;
        }
    }
    else logLate(ts, clock, delta);
}

void AwesomePlayer::logSyncLoss()
{
    LOGW("=====================================================");
    LOGW("Number of times AV Sync Losses = %u", mNumTimesSyncLoss);
    LOGW("Max Video Ahead time delta = %u", -mMaxEarlyDelta/1000);
    LOGW("Max Video Behind time delta = %u", mMaxLateDelta/1000);
    LOGW("Max Time sync loss = %u",mMaxTimeSyncLoss/1000);
    LOGW("=====================================================");

}

inline int64_t AwesomePlayer::getTimeOfDayUs() {
    struct timeval tv;
    gettimeofday(&tv, NULL);

    return (int64_t)tv.tv_sec * 1000000 + tv.tv_usec;
}

}  // namespace android

