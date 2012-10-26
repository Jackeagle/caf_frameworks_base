/* Copyright (c) 2011-2012, The Linux Foundation. All rights reserved.
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.util.Log;

/**
 * @hide This is only used by the browser
 * Singleton class used for managing the HTML5 video Fullscreen view
 *
 */
public class HTML5VideoFullscreen implements View.OnTouchListener,
       TextureView.SurfaceTextureListener
{
    private static final String LOGTAG = "HTML5VideoFullscreen";

    private static final long ANIMATION_DURATION = 750L; // in ms
    private static final int ANIMATION_STATE_NONE     = 0;
    private static final int ANIMATION_STATE_STARTED  = 1;
    private static final int ANIMATION_STATE_FINISHED = 2;
    private int mAnimationState;

    // The progress view.
    private View mProgressView;
    // The Media Controller only used for full screen mode
    private MediaController mMediaController;

    // The container for the progress view and video view
    private FrameLayout mLayout;
    private boolean mIsFullscreen;

    private final FrameLayout.LayoutParams WRAP_CONTENT_PARAMS =
        new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);

    private HTML5VideoViewProxy mFullscreenProxy = null;
    private VideoTextureView mTextureView;
    // End Fullscreen member variables

    // The video size will be ready when prepared. Used to make sure the aspect
    // ratio is correct.
    private int mVideoWidth;
    private int mVideoHeight;

    private int mFullscreenWidth;
    private int mFullscreenHeight;
    private float mInlineX;
    private float mInlineY;
    private float mInlineWidth;
    private float mInlineHeight;
    private Point mDisplaySize;
    private int[] mWebViewLocation;

    private HTML5VideoFullscreen() {
        mDisplaySize = new Point();
        mWebViewLocation = new int[2];
    }

    private static HTML5VideoFullscreen fullscreen;

    public static HTML5VideoFullscreen instance() {
        if (fullscreen == null) {
            fullscreen = new HTML5VideoFullscreen();
        }
        return fullscreen;
    }

    class VideoTextureView extends TextureView {
        public VideoTextureView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mFullscreenWidth = getDefaultSize(mVideoWidth, widthMeasureSpec);
            mFullscreenHeight = getDefaultSize(mVideoHeight, heightMeasureSpec);
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if ( mVideoWidth * mFullscreenHeight > mFullscreenWidth * mVideoHeight ) {
                    mFullscreenHeight = mFullscreenWidth * mVideoHeight / mVideoWidth;
                } else if ( mVideoWidth * mFullscreenHeight < mFullscreenWidth * mVideoHeight ) {
                    mFullscreenWidth = mFullscreenHeight * mVideoWidth / mVideoHeight;
                }
            }
            setMeasuredDimension(mFullscreenWidth, mFullscreenHeight);

            if (mAnimationState == ANIMATION_STATE_NONE) {
                // Configuring VideoTextureView to inline bounds
                mTextureView.setTranslationX(getInlineXOffset());
                mTextureView.setTranslationY(getInlineYOffset());
                mTextureView.setScaleX(getInlineXScale());
                mTextureView.setScaleY(getInlineYScale());

                // inline to Fullscreen zoom out animation
                mTextureView.animate().setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        mAnimationState = ANIMATION_STATE_FINISHED;
                        // onAnimationEnd is also called if animation is cancelled
                        // by exiting fullscreen during the animation.
                        // Only notify the proxy if still in fullscreen view
                        if (mIsFullscreen)
                            mFullscreenProxy.onEnterFullscreen();
                    }
                });
                mTextureView.animate().setDuration(ANIMATION_DURATION);
                mAnimationState = ANIMATION_STATE_STARTED;
                mTextureView.animate().scaleX(1.0f).scaleY(1.0f).translationX(0.0f).translationY(0.0f);
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            boolean isOpaque = isOpaque();
            // Needed to update the view during orientation change when video is paused
            // Calling setOpaque() to a new value forces the layer to be updated
            setOpaque(!isOpaque);
            setOpaque(isOpaque);
        }
    }

    public void setVideoSize(HTML5VideoViewProxy proxy, int width, int height) {
        if (mFullscreenProxy != proxy)
            return;

        mVideoWidth = width;
        mVideoHeight = height;
        if (mTextureView != null) {
            // Request layout now that mVideoWidth and mVideoHeight are known
            // This will trigger onMeasure to get the display size right
            mTextureView.requestLayout();
        }
    }

    public void setSurfaceTextureToFullscreen(HTML5VideoViewProxy proxy,
            SurfaceTexture surfaceTexture) {
        assert(mLayout != null);
        if (mFullscreenProxy != proxy) {
            // Clear any existing media controller and texture view
            releaseMediaController();
            releaseTextureView();

            // Create VideoTextureView
            mTextureView = new VideoTextureView(proxy.getContext());
            mFullscreenProxy = proxy;
            mTextureView.setSurfaceTexture(surfaceTexture);
            mTextureView.setVisibility(View.VISIBLE);
            mTextureView.setEnabled(true);
            mTextureView.setOnTouchListener(this);
            mTextureView.setFocusable(true);
            mTextureView.setFocusableInTouchMode(true);
            mTextureView.requestFocus();
            mTextureView.setSurfaceTextureListener(this);

            // Attach view to layout
            mLayout.addView(mTextureView, WRAP_CONTENT_PARAMS);
        }
    }

    public void clearSurfaceTexture(HTML5VideoViewProxy proxy) {
        if (mFullscreenProxy != proxy)
            return;

        releaseMediaController();
        releaseTextureView();
    }

    // Return true when video has successfully exited fullscreen mode
    public void webkitExitFullscreen(HTML5VideoViewProxy proxy) {
        // Already exited fullscreen
        if (mLayout == null || mFullscreenProxy == null)
            return;

        // Current proxy is not the fullscreen proxy
        if (mFullscreenProxy != proxy)
            return;

        WebChromeClient client = mFullscreenProxy.getWebView().getWebChromeClient();
        if (client != null)
            client.onHideCustomView();
    }

    public void enterFullscreen(HTML5VideoViewProxy proxy, SurfaceTexture surfaceTexture,
            float x, float y, float width, float height) {
        if (mIsFullscreen == true)
            return;

        assert(mFullscreenProxy == null);
        mIsFullscreen = true;
        mInlineX = x;
        mInlineY = y;
        mInlineWidth = width;
        mInlineHeight = height;
        if (mLayout == null) {
            mLayout = new FrameLayout(proxy.getContext());
            setSurfaceTextureToFullscreen(proxy, surfaceTexture);
            mLayout.setVisibility(View.VISIBLE);
        }
        mAnimationState = ANIMATION_STATE_NONE;
        WebChromeClient client = mFullscreenProxy.getWebView().getWebChromeClient();
        if (client != null) {
            // Only call show custom view if it's not already shown
            client.onShowCustomView(mLayout, mCallback);
            // Plugins like Flash will draw over the video so hide
            // them while we're playing.
            if (mFullscreenProxy.getWebView().getViewManager() != null)
                mFullscreenProxy.getWebView().getViewManager().hideAll();
            // Add progress view
            mProgressView = client.getVideoLoadingProgressView();
            if (mProgressView != null) {
                mLayout.addView(mProgressView, WRAP_CONTENT_PARAMS);
            }
        }
    }

    public void exitFullscreen(HTML5VideoViewProxy proxy, float x, float y, float width, float height) {
        if (mFullscreenProxy != proxy)
            return;

        mIsFullscreen = false;
        mInlineX = x;
        mInlineY = y;
        mInlineWidth = width;
        mInlineHeight = height;
        if (mTextureView != null) {
            if (mAnimationState == ANIMATION_STATE_STARTED) {
                mTextureView.animate().cancel();
            } else {
                mTextureView.animate().setListener(null);
                // Fullscreen to inline zoom in animation
                mTextureView.animate().setDuration(ANIMATION_DURATION);
                mTextureView.animate().scaleX(getInlineXScale()).scaleY(getInlineYScale()).translationX(getInlineXOffset()).translationY(getInlineYOffset());
            }
        }
    }

    // This method will always get called when exiting fullscreen
    private void finishExitingFullscreen() {
        mIsFullscreen = false;
        // Don't show the controller after exiting fullscreen
        releaseMediaController();
        releaseTextureView();
        if (mProgressView != null) {
            mLayout.removeView(mProgressView);
            mProgressView = null;
        }
        assert (mFullscreenProxy != null);
        // Re enable plugin views.
        mFullscreenProxy.getWebView().getViewManager().showAll();
        mFullscreenProxy = null;
        mLayout = null;
    }

    private void releaseTextureView() {
        if (mLayout != null && mTextureView != null) {
            // Cancel animation if it is not finished
            mTextureView.animate().cancel();
            mLayout.removeView(mTextureView);
            mTextureView = null;
        }
    }

    private void releaseMediaController() {
        if (mMediaController != null) {
            mMediaController.hide();
            mMediaController = null;
        }
    }

    private void updateDisplaySize() {
        WindowManager wm = (WindowManager)mFullscreenProxy.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(mDisplaySize);

        mFullscreenProxy.getWebView().getWebView().getLocationOnScreen(mWebViewLocation);
        mWebViewLocation[1] += mFullscreenProxy.getWebView().getVisibleTitleHeight();
    }

    private float getInlineXOffset() {
        updateDisplaySize();
        if (mInlineWidth < 0 || mInlineHeight < 0)
            return 0;
        else
            return mInlineX + mWebViewLocation[0] - (mDisplaySize.x - mInlineWidth) / 2;
    }

    private float getInlineYOffset() {
        updateDisplaySize();
        if (mInlineWidth < 0 || mInlineHeight < 0)
            return 0;
        else
            return mInlineY + mWebViewLocation[1] - (mDisplaySize.y - mInlineHeight) / 2;
    }

    private float getInlineXScale() {
        if (mInlineWidth < 0 || mInlineHeight < 0 || mFullscreenWidth == 0)
            return 0;
        else
            return mInlineWidth / mFullscreenWidth;
    }

    private float getInlineYScale() {
        if (mInlineWidth < 0 || mInlineHeight < 0 || mFullscreenHeight == 0)
            return 0;
        else
            return mInlineHeight / mFullscreenHeight;
    }

    public boolean isFullscreenView() {
        return mIsFullscreen;
    }

    // If MediaPlayer is prepared, enable the buttons
    public void attachMediaController(HTML5VideoViewProxy proxy, MediaController.MediaPlayerControl player) {
        if (mFullscreenProxy != proxy)
            return;

        assert(mLayout != null);
        // Get the capabilities of the player for this stream
        // This should only be called when MediaPlayer is in prepared state
        // Otherwise data will return invalid values
        if (mMediaController == null) {
            MediaController mc = new FullScreenMediaController(proxy.getContext(), mLayout);
            mc.setSystemUiVisibility(mLayout.getSystemUiVisibility());
            mMediaController = mc;
        }
        mMediaController.setEnabled(false);
        mMediaController.setMediaPlayer(player);
        mMediaController.setAnchorView(mTextureView);

        // mMediaController status depends on the Metadata result, so put it
        // after reading the MetaData
        mMediaController.setEnabled(true);
    }

    public void showMediaControls(HTML5VideoViewProxy proxy, boolean showForever) {
        if (mFullscreenProxy != proxy)
            return;

        if (mMediaController != null && mAnimationState == ANIMATION_STATE_FINISHED) {
            if (showForever)
                mMediaController.show(0);
            else
                mMediaController.show();
        }
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing())
            mMediaController.hide();
        else
            mMediaController.show();
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    /**
     * Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
     * If returns true, no rendering should happen inside the surface texture after this method
     * is invoked. If returns false, the client needs to call {@link SurfaceTexture#release()}.
     *
     * @param surface The surface about to be destroyed
     */
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        assert (mFullscreenProxy != null);
        mFullscreenProxy.onStopFullscreen();
        return false;
    }

    private class FullscreenAnimatedCustomViewCallback extends
        AnimatedCustomViewCallback {

        public FullscreenAnimatedCustomViewCallback() {
            super();
        }

        public void onCustomViewHiddenAnimationStart() {
            // Release media controller before starting animation
            releaseMediaController();
            if (mFullscreenProxy != null)
                mFullscreenProxy.prepareExitFullscreen();
        }

        public void onCustomViewHidden() {
            finishExitingFullscreen();
        }

        public long getAnimationDelay() {
            return ANIMATION_DURATION;
        }
    }

    private final AnimatedCustomViewCallback mCallback = new
            FullscreenAnimatedCustomViewCallback();

    public boolean onTouch(View v, MotionEvent event) {
        if (mIsFullscreen && mMediaController != null)
            toggleMediaControlsVisiblity();
        return false;
    }

    public void switchProgressView(HTML5VideoViewProxy proxy, boolean playerBuffering) {
        // Allow the call to switch progress view if the calling proxy is in fullscreen
        // Calling proxy does not need to be the current proxy
        if (!proxy.isFullscreen())
            return;

        if (mProgressView != null) {
            if (playerBuffering)
                mProgressView.setVisibility(View.VISIBLE);
            else
                mProgressView.setVisibility(View.GONE);
        }
    }

    static class FullScreenMediaController extends MediaController {

        View mVideoView;

        public FullScreenMediaController(Context context, View video) {
            super(context);
            mVideoView = video;
        }

        @Override
        public void show() {
            super.show();
            if (mVideoView != null) {
                mVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }

        @Override
        public void hide() {
            if (mVideoView != null) {
                mVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
            super.hide();
        }
    }
}
