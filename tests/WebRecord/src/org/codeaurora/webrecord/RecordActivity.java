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

package org.codeaurora.webrecord;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.PictureListener;

import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;

public class RecordActivity extends Activity {

    private WebView mWebView;
    private Picture mPicture;
    private String mFileName;

    static final String LOGTAG = "WebRecord";

    static String pageTitle;
    static int progress = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "Start");

        // simulate the real
        mWebView = new WebView(this);
        mPicture = null;
        mFileName = null;

        setContentView(mWebView);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);

        // "Auto-Fit Pages" is the default in the
        boolean autoFitPage = true;
        if (autoFitPage) {
            mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        } else {
            mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        }

        mWebView.setWebViewClient(new WebViewClient() {

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(LOGTAG, "Start loading " + url);
            }

            public void onPageFinished(WebView view, String url) {
                Log.d(LOGTAG, "Finish loading " + url);
                RecordActivity.this.setTitle("Recording is completed");

                try {
                    URL viewURL = new URL(view.getUrl());
                    String fileName = viewURL.getFile();
                    Log.d(LOGTAG, "getFile(): " + fileName);
                    Log.d(LOGTAG, "fileName.lastIndexOf('/') = " + fileName.lastIndexOf('/'));
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                    Log.d(LOGTAG, "FileName: " + fileName);
                    // Accept only fileName non empty and with an embedded dot.
                    if (fileName.indexOf('/') == -1 && fileName.indexOf('.') != -1 && fileName.length() != 0)
                        mFileName = "/sdcard/" + fileName + ".picture";

                        mFileName = "/sdcard/" + viewURL.getHost() + ".picture";
                } catch (Exception e) {
                    Log.d(LOGTAG, "Can't get the URL: " + e);
                    mFileName = "/sdcard/output.picture";
                }
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int percent) {
                Log.d(LOGTAG, "Loading progress: " + percent + "%");
                RecordActivity.progress = percent;
                if (percent < 100) {
                    String caption = "Recording [" + RecordActivity.progress  + "%]: " + RecordActivity.pageTitle;
                    RecordActivity.this.setTitle(caption);
                } else {
                    RecordActivity.this.setTitle("Recording is completed");
                }
            }

            public void onReceivedTitle(WebView view, String title) {
                RecordActivity.pageTitle = title;
                String caption = "Recording [" + RecordActivity.progress  + "%]: " + RecordActivity.pageTitle;
                RecordActivity.this.setTitle(caption);
                Log.d(LOGTAG, "Page title is " + title);
            }

        });

        mWebView.setPictureListener(new PictureListener() {

            public void onNewPicture(WebView view, Picture picture) {
                Log.d(LOGTAG, "New picture: " + picture.getWidth() + "x" + picture.getHeight());
                if (mFileName == null)
                    return;
                Log.d(LOGTAG, "Final picture: " + picture.getWidth() + "x" + picture.getHeight());
                try {
                    File file = new File(mFileName);
                    if (file.exists()) {
                        Log.d(LOGTAG, "Deleted previous file with the same name");
                        file.delete();
                    }
                    file.createNewFile();
                    FileOutputStream stream = new FileOutputStream(file);
                    picture.writeToStream(stream);
                    stream.close();
                    Log.d(LOGTAG, "Saved to " + mFileName);
                } catch (Exception e) {
                    Log.d(LOGTAG, "Unable to save picture to " + mFileName);
                    Log.d(LOGTAG, "Exception: " + e);
                }
            }

        });
        Intent intent = getIntent();
        String address = intent.getDataString();
        if (address == null)
            address = "http://www.google.com/m/gp";
        Log.d(LOGTAG, "About to load " + address);
        pageTitle = address;
        mWebView.loadUrl(address);
    }
}
