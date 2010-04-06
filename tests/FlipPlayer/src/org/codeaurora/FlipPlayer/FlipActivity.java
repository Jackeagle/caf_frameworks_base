// Copyright (c) 2010, Code Aurora Forum. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of Code Aurora Forum, Inc. nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
// ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
// BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
// OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
// IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// Author: Ariya Hidayat <ahidayat@codeaurora.org>
// Contributor: Julien Chaffraix <jchaffraix@codeaurora.org>

package org.codeaurora.FlipPlayer;

import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class FlipActivity extends Activity {

        static final String LOGTAG = "FlipPlayer";

        private Handler mHandler = new Handler();


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d(LOGTAG, "Starting");

        Picture pictureOne = loadPicture("/sdcard/page1.picture");
        if (pictureOne == null)
            return;

        Picture pictureTwo = loadPicture("/sdcard/page2.picture");
        if (pictureTwo == null)
            return;

        mRenderPictureOne = new RenderPictureRunnable(pictureOne);
        mRenderPictureTwo = new RenderPictureRunnable(pictureTwo);
        mRenderPictureOne.setNextRunnable(mRenderPictureTwo);
        mRenderPictureTwo.setNextRunnable(mRenderPictureOne);

        mHandler.postDelayed(mRenderPictureOne, 100);
    }

    private RenderPictureRunnable mRenderPictureOne;
    private RenderPictureRunnable mRenderPictureTwo;

    private synchronized void setTimeAfterImageDispatch() {
        long meanRenderOne = mRenderPictureOne.mean();
        long meanRenderTwo = mRenderPictureTwo.mean();
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText("First image: " + meanRenderOne + " ms. Second image: " + meanRenderTwo + " ms");
    }

    private boolean mShouldStop;

    private class RenderPictureRunnable implements Runnable {
        public RenderPictureRunnable(Picture picture){
            mPicture = picture;
            mBitmap = Bitmap.createBitmap(mPicture.getWidth(), mPicture.getHeight(), Bitmap.Config.ARGB_8888);

            clearMeasure();

            // Set up the clipping information.
            // Our image view is centered on the image, so we need to calculate
            // bounds and offset accordingly!
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int offsetTop = (mPicture.getHeight() - metrics.heightPixels) / 2;
            if (offsetTop < 0)
                offsetTop = 0;
            int offsetLeft = (mPicture.getWidth() - metrics.widthPixels) / 2;
            if (offsetLeft < 0)
                offsetLeft = 0;
            mClipRect = new Rect(offsetLeft, offsetTop, offsetLeft + metrics.widthPixels, offsetTop + metrics.heightPixels);
            mClipped = true; // By default we clip.
        }

        private void clearMeasure() {
            mTotalTime = 0;
            mCount = 0;
        }
        private Bitmap mBitmap;
        private Picture mPicture;
        private Runnable mNextRunnable;
        private Rect mClipRect;
        private boolean mClipped;

        public void toggleClip() {
            mClipped = !mClipped;
            clearMeasure();
        }
        public long mean() {
            if (mCount == 0)
                return 0;
            return mTotalTime / mCount;
        }
        private long mTotalTime;
        private long mCount;


        public void setNextRunnable(Runnable nextRunnable) { mNextRunnable = nextRunnable; }
        public void run() {
            long startingTime = Process.getElapsedCpuTime();
            Canvas canvas = new Canvas(mBitmap);
            if (mClipped) {
                if (!canvas.clipRect(mClipRect))
                    Log.d(LOGTAG, "Issue when clipping!");
            }

            canvas.drawPicture(mPicture);
            mTotalTime = mTotalTime + (Process.getElapsedCpuTime() - startingTime);
            mCount = mCount + 1;

            ImageView view = (ImageView) findViewById(R.id.img);
            view.setImageBitmap(mBitmap);
            setTimeAfterImageDispatch();
            if (!mShouldStop)
                mHandler.postDelayed(mNextRunnable, 0);
        }

    }

    public Picture loadPicture(String file) {
        Log.d(LOGTAG, "Opening file" + file);
        File pictureFile = new File(file);
        if (!pictureFile.canRead()) {
            Log.d(LOGTAG, "Picture cannot be read!");
            return null;
        }
        try {
            FileInputStream fileStream = new FileInputStream(pictureFile);
            Picture result = Picture.createFromStream(fileStream);
            Log.d(LOGTAG, "Picture size (h = " + result.getHeight() + ", w = " + result.getWidth() + ")");
            return result;
        } catch(Exception e) {
            Log.d(LOGTAG, "Exception received: " + e.toString());
            return null;
        }
    }

    public void onStop() {
        super.onStop();
        Log.d(LOGTAG, "onStop called!");
        mShouldStop = true;
    }

    public void onPause() {
        super.onPause();
        Log.d(LOGTAG, "onPause called!");
        mShouldStop = true;
    }

    // Quit the application on any key press.
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(LOGTAG, "Called onkeydown (with keycode = " + String.valueOf(keyCode) + ", finishing");
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mRenderPictureOne.toggleClip();
            mRenderPictureTwo.toggleClip();
            return false;
        }

        finish();
        return true;
    }


}
