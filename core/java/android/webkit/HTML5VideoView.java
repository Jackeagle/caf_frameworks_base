/*
 * Copyright (c) 2011, 2012, The Linux Foundation. All rights reserved.
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

package android.webkit;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.Metadata;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Display;
import android.view.WindowManager;
import android.widget.MediaController.MediaPlayerControl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @hide This is only used by the browser
 */
public class HTML5VideoView implements MediaPlayer.OnPreparedListener,
    MediaPlayerControl, SurfaceTexture.OnFrameAvailableListener,
    MediaPlayer.OnVideoSizeChangedListener
{
    private static final String LOGTAG = "HTML5VideoView";
    private static final String COOKIE = "Cookie";
    private static final String HIDE_URL_LOGS = "x-hide-urls-from-log";

    // For handling the seekTo before prepared, we need to know whether or not
    // the video is prepared. Therefore, we differentiate the state between
    // prepared and not prepared.
    // When the video is not prepared, we will have to save the seekTo time,
    // and use it when prepared to play.
    // NOTE: these values are in sync with VideoLayerManager.h in webkit side.
    // Please keep them in sync when changed.
    static final int STATE_RELEASED           = 0;
    static final int STATE_INITIALIZED        = 1;
    static final int STATE_PREPARING          = 2;
    static final int STATE_PREPARED           = 3;
    static final int STATE_PLAYING            = 4;
    static final int STATE_BUFFERING          = 5;
    static final int STATE_DETACHED           = 6;

    private HTML5VideoViewProxy mProxy;

    // Save the seek time when not prepared. This can happen when switching
    // video besides initial load.
    private int mSaveSeekTime;

    private MediaPlayer mPlayer;
    private int mCurrentState;

    // We need to save such info.
    private Uri mUri;
    private Map<String, String> mHeaders;

    // The timer for timeupate events.
    // See http://www.whatwg.org/specs/web-apps/current-work/#event-media-timeupdate
    private Timer mTimer;

    protected boolean mPauseDuringPreparing;

    // The spec says the timer should fire every 250 ms or less.
    private static final int TIMEUPDATE_PERIOD = 250;  // ms

    private int mVideoWidth;
    private int mVideoHeight;
    private int mDuration;

    // Data only for Fullscreen MediaController
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private boolean mCanPause;
    private int mCurrentBufferPercentage;

    private static final int SURFACE_TEXTURE_STATE_ATTACHED_INLINE     = 0;
    private static final int SURFACE_TEXTURE_STATE_ATTACHED_FULLSCREEN = 1;
    private static final int SURFACE_TEXTURE_STATE_DETACHED            = 2;
    private int mSurfaceTextureState;

    private SurfaceTexture mSurfaceTexture;
    // m_textureNames is the texture bound with this SurfaceTexture
    // This texture name is used for inline video only.
    // TextureView generates its own GL texture that is attached to
    // this SurfaceTexture in fullscreen mode.
    private int[] mTextureNames;

    // common Video control FUNCTIONS:
    public void start() {
        if (mCurrentState == STATE_PREPARED) {
            // When replaying the same video, there is no onPrepared call.
            // Therefore, the timer should be set up here.
            if (mTimer == null) {
                mTimer = new Timer();
                mTimer.schedule(new TimeupdateTask(mProxy), TIMEUPDATE_PERIOD,
                        TIMEUPDATE_PERIOD);
            }
            mPlayer.start();

            // Notify webkit MediaPlayer that video is playing to make sure
            // webkit MediaPlayer is always synchronized with the proxy.
            // This is particularly important when using the fullscreen
            // MediaController.
            mProxy.dispatchOnPlaying();
            if (isFullscreen()) {
                HTML5VideoFullscreen.instance().switchProgressView(mProxy, false);
                HTML5VideoFullscreen.instance().showMediaControls(mProxy, false);
            }
        } else
            setStartWhenPrepared(true);
    }

    public void pause() {
        if (isPlaying()) {
            mPlayer.pause();
        } else if (mCurrentState == STATE_PREPARING) {
            mPauseDuringPreparing = true;
        }
        // Notify webkit MediaPlayer that video is paused to make sure
        // webkit MediaPlayer is always synchronized with the proxy
        // This is particularly important when using the Fullscreen
        // MediaController.
        mProxy.dispatchOnPaused();

        if (isFullscreen()) {
            // If paused, should show the controller for ever!
            HTML5VideoFullscreen.instance().showMediaControls(mProxy, true);
        }

        // Delete the Timer to stop it since there is no stop call.
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void detachInlineGLContext() {
        if (mSurfaceTexture != null && mSurfaceTextureState == SURFACE_TEXTURE_STATE_ATTACHED_INLINE) {
            try {
                // Detach the inline GL context
                mSurfaceTexture.detachFromGLContext();
                // detachFromGLContext() calls glDeleteTexture in the
                // SurfaceTexture native implementation
                // Need to generate a new texture
                mTextureNames = new int[1];
                GLES20.glGenTextures(1, mTextureNames, 0);
                mSurfaceTextureState = SURFACE_TEXTURE_STATE_DETACHED;
                mProxy.updateVideoLayerPlayerState();
                setInlineFrameAvailableListener(null);
            } catch (RuntimeException e) {
                mSurfaceTextureState = SURFACE_TEXTURE_STATE_ATTACHED_INLINE;
                e.printStackTrace();
            }
        }
    }

    public void attachInlineGlContextIfNeeded() {
        if (mSurfaceTexture != null && mSurfaceTextureState == SURFACE_TEXTURE_STATE_DETACHED
                && !isFullscreen()
                && (mCurrentState == STATE_PREPARED ||
                    mCurrentState == STATE_PREPARING ||
                    mCurrentState == STATE_PLAYING)) {
            // Attach the previous GL texture
            try {
                mSurfaceTexture.attachToGLContext(getTextureName());
                mSurfaceTextureState = SURFACE_TEXTURE_STATE_ATTACHED_INLINE;
                setInlineFrameAvailableListener(this);
                // update player state so that it is no longer DETACHED
                mProxy.updateVideoLayerPlayerState();
            } catch (RuntimeException e) {
                // This can occur when the EGL context has been detached from this view
                // which can occur when going into power standby during fullscreen mode
                // Just try to re-attach at a later time.
            }
        }
    }

    public int getDuration() {
        if (mCurrentState == STATE_PREPARED) {
            return mPlayer.getDuration();
        } else {
            return -1;
        }
    }

    public int getCurrentPosition() {
        if (mCurrentState == STATE_PREPARED) {
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int pos) {
        if (mCurrentState == STATE_PREPARED)
            mPlayer.seekTo(pos);
        else
            mSaveSeekTime = pos;
    }

    public boolean isPlaying() {
        if (mCurrentState == STATE_PREPARED) {
            return mPlayer.isPlaying();
        } else {
            return false;
        }
    }

    public void release() {
        if (mCurrentState != STATE_RELEASED) {
            stopPlayback();
            mPlayer.release();
            if (isFullscreen()) {
                // Hide the TextureView. SurfaceTexture cannot be released until
                // the TextureView is removed.
                HTML5VideoFullscreen.instance().clearSurfaceTexture(mProxy);
            }
            mSurfaceTexture.release();
            mSurfaceTexture = null;
            mTextureNames = null;
        }
        mCurrentState = STATE_RELEASED;
    }

    public void stopPlayback() {
        if (mCurrentState == STATE_PREPARED) {
            mPlayer.stop();
        }
    }

    public boolean getPauseDuringPreparing() {
        return mPauseDuringPreparing;
    }

    public void setVolume(float volume) {
        if (mCurrentState != STATE_RELEASED) {
            mPlayer.setVolume(volume, volume);
        }
    }

    // Every time we start a new Video, we create a VideoView and a MediaPlayer
    HTML5VideoView(HTML5VideoViewProxy proxy, int position) {
        mPlayer = new MediaPlayer();
        mCurrentState = STATE_INITIALIZED;
        mProxy = proxy;
        mSaveSeekTime = position;
        mTimer = null;
        mPauseDuringPreparing = false;
    }

    private static Map<String, String> generateHeaders(String url,
            HTML5VideoViewProxy proxy) {
        boolean isPrivate = proxy.getWebView().isPrivateBrowsingEnabled();
        String cookieValue = CookieManager.getInstance().getCookie(url, isPrivate);
        Map<String, String> headers = new HashMap<String, String>();
        if (cookieValue != null) {
            headers.put(COOKIE, cookieValue);
        }
        if (isPrivate) {
            headers.put(HIDE_URL_LOGS, "true");
        }

        return headers;
    }

    public void setVideoURI(String uri) {
        mUri = Uri.parse(uri);
        mHeaders = generateHeaders(uri, mProxy);
    }

    // When there is a frame ready from surface texture, we should tell WebView
    // to refresh.
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // TODO: This should support partial invalidation too.
        if (mProxy != null && mProxy.isMediaVisible())
            mProxy.getWebView().invalidate();
    }

    public void retrieveMetadata(HTML5VideoViewProxy proxy) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mUri.toString(), mHeaders);
            mVideoWidth = Integer.parseInt(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            mVideoHeight = Integer.parseInt(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            mDuration = Integer.parseInt(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION));
            proxy.updateSizeAndDuration(mVideoWidth, mVideoHeight, mDuration);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            // RuntimeException occurs when connection is not available or
            // the source type is not supported (e.g. HLS). Not calling
            // e.printStackTrace() here since it occurs quite often.
        } finally {
            retriever.release();
        }
    }

    // Listeners setup FUNCTIONS:
    public void setOnCompletionListener(HTML5VideoViewProxy proxy) {
        mPlayer.setOnCompletionListener(proxy);
    }

    private void prepareDataCommon(HTML5VideoViewProxy proxy) {
        try {
            mCurrentState = STATE_INITIALIZED;
            mPlayer.reset();
            mPlayer.setDataSource(proxy.getContext(), mUri, mHeaders);
            mPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCurrentState = STATE_PREPARING;
    }

    public void prepareDataAndDisplayMode() {
        decideDisplayMode();

        mPlayer.setOnCompletionListener(mProxy);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(mProxy);
        mPlayer.setOnInfoListener(mProxy);
        mPlayer.setOnVideoSizeChangedListener(this);

        prepareDataCommon(mProxy);

        // TODO: This is a workaround, after b/5375681 fixed, we should switch
        // to the better way.
        if (mProxy.getContext().checkCallingOrSelfPermission(permission.WAKE_LOCK)
                == PackageManager.PERMISSION_GRANTED) {
            mPlayer.setWakeMode(mProxy.getContext(), PowerManager.FULL_WAKE_LOCK);
        }
        if (isFullscreen()) {
            // If dynamically switching the video in fullscreen mode, show progress view
            // to give a visual cue that new video is loading
            HTML5VideoFullscreen.instance().switchProgressView(mProxy, true);
        } else if (mSurfaceTextureState == SURFACE_TEXTURE_STATE_ATTACHED_INLINE) {
            setInlineFrameAvailableListener(this);
        }
    }

    // This configures the SurfaceTexture OnFrameAvailableListener in inline mode
    private void setInlineFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener) {
        getSurfaceTexture().setOnFrameAvailableListener(listener);
    }

    public int getCurrentState() {
        if (isPlaying()) {
            return STATE_PLAYING;
        } else {
            return mCurrentState;
        }
    }

    public boolean isInlineVideoConfigured() {
        return (mSurfaceTextureState == SURFACE_TEXTURE_STATE_ATTACHED_INLINE);
    }

    private final class TimeupdateTask extends TimerTask {
        private HTML5VideoViewProxy mProxy;

        public TimeupdateTask(HTML5VideoViewProxy proxy) {
            mProxy = proxy;
        }

        @Override
        public void run() {
            mProxy.onTimeupdate();
        }
    }

    private void attachMediaController() {
        Metadata data = mPlayer.getMetadata(MediaPlayer.METADATA_ALL,
                MediaPlayer.BYPASS_METADATA_FILTER);
        if (data != null) {
            mCanPause = !data.has(Metadata.PAUSE_AVAILABLE)
                || data.getBoolean(Metadata.PAUSE_AVAILABLE);
            mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
                || data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
            mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
                || data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
        } else {
            mCanPause = mCanSeekBack = mCanSeekForward = true;
        }
        HTML5VideoFullscreen.instance().attachMediaController(mProxy, this);
    }

    // Helper function to determine if this particular video is fullscreen
    private boolean isFullscreen() {
        return HTML5VideoFullscreen.instance().isFullscreenView()
                && mProxy.isFullscreen();
    }

    public void onPrepared(MediaPlayer mp) {
        mCurrentState = STATE_PREPARED;
        seekTo(mSaveSeekTime);

        if (mProxy != null)
            mProxy.onPrepared(mp);

        if (isFullscreen()) {
            if (mSurfaceTextureState != SURFACE_TEXTURE_STATE_ATTACHED_FULLSCREEN) {
                // This code handles switching of video source in fullscreen mode
                detachInlineGLContext();
                assert(mSurfaceTextureState == SURFACE_TEXTURE_STATE_DETACHED);
                HTML5VideoFullscreen.instance().setSurfaceTextureToFullscreen(
                        mProxy, getSurfaceTexture());
                mSurfaceTextureState = SURFACE_TEXTURE_STATE_ATTACHED_FULLSCREEN;
            }
            // Configure MediaPlayer
            attachMediaController();
            mPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            HTML5VideoFullscreen.instance().switchProgressView(mProxy, false);
            HTML5VideoFullscreen.instance().showMediaControls(mProxy, !getStartWhenPrepared());
        }

        if (mPauseDuringPreparing || !getStartWhenPrepared())
            mPauseDuringPreparing = false;
        else
            start();
    }

    public void decideDisplayMode() {
        SurfaceTexture surfaceTexture = getSurfaceTexture();
        Surface surface = new Surface(surfaceTexture);
        mPlayer.setSurface(surface);
        surface.release();
    }

    // SurfaceTexture will be created lazily here
    public SurfaceTexture getSurfaceTexture() {
        // Create the surface texture.
        if (mSurfaceTexture == null) {
            if (mTextureNames == null) {
                mTextureNames = new int[1];
                GLES20.glGenTextures(1, mTextureNames, 0);
            }
            mSurfaceTexture = new SurfaceTexture(mTextureNames[0]);
            mSurfaceTextureState = SURFACE_TEXTURE_STATE_ATTACHED_INLINE;
        }
        return mSurfaceTexture;
    }

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (isFullscreen())
            HTML5VideoFullscreen.instance().setVideoSize(mProxy, width, height);
        if (mProxy != null) {
            mProxy.onVideoSizeChanged(mp, width, height);
        }
    }

    public int getTextureName() {
        if (mTextureNames != null) {
            return mTextureNames[0];
        } else {
            return 0;
        }
    }

    // This is true only when the player is buffering and paused
    private boolean mPlayerBuffering = false;

    public boolean getPlayerBuffering() {
        return mPlayerBuffering;
    }

    public void setPlayerBuffering(boolean playerBuffering) {
        mPlayerBuffering = playerBuffering;
        if (isFullscreen())
            HTML5VideoFullscreen.instance().switchProgressView(mProxy, playerBuffering);
    }


     public void enterFullscreenVideoState(WebViewClassic webView, float x, float y, float w, float h) {
        detachInlineGLContext();
        if (mSurfaceTextureState != SURFACE_TEXTURE_STATE_DETACHED) {
            Log.w(LOGTAG, "Unable to enter Fullscreen at this time");
            return;
        }
        mCurrentBufferPercentage = 0;

        HTML5VideoFullscreen.instance().enterFullscreen(mProxy, getSurfaceTexture(), x, y, w, h);
        // setVideoSize needs to be called after enterFullscreen because it validates the
        // current fullscreen proxy
        HTML5VideoFullscreen.instance().setVideoSize(mProxy, mVideoWidth, mVideoHeight);
        HTML5VideoFullscreen.instance().switchProgressView(mProxy, mCurrentState != STATE_PREPARED);
        mSurfaceTextureState = SURFACE_TEXTURE_STATE_ATTACHED_FULLSCREEN;
        // Configure MediaPlayer
        mPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
    }

    public void exitFullscreenVideoState(float x, float y, float w, float h) {
        if (!isFullscreen()) {
            return;
        }
        HTML5VideoFullscreen.instance().exitFullscreen(mProxy, x, y, w, h);
    }

    // MediaController FUNCTIONS:
    public boolean canPause() {
        return mCanPause;
    }

    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    public int getBufferPercentage() {
        if (mPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean mStartWhenPrepared = false;

    public void setStartWhenPrepared(boolean willPlay) {
        mStartWhenPrepared  = willPlay;
    }

    public boolean getStartWhenPrepared() {
        return mStartWhenPrepared;
    }

    // Other listeners functions:
    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
        new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
        }
    };

    public void onEnterFullscreen() {
        if (mCurrentState == STATE_PREPARED) {
            attachMediaController();
            HTML5VideoFullscreen.instance().showMediaControls(mProxy, !isPlaying());
        }
    }

    public void onStopFullscreen() {
        mSurfaceTextureState = SURFACE_TEXTURE_STATE_DETACHED;
        // Cannot clear the TextureView until SurfaceTexture has been detached
        attachInlineGlContextIfNeeded();
    }

}
