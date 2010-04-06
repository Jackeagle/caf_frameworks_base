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
// Author: Julien Chaffraix <jchaffraix@codeaurora.org>

package org.codeaurora.PicturePlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class PicturePlayer extends Activity {

    static final String LOGTAG = "PicturePlayer";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d(LOGTAG, "Starting");
        Intent intent = getIntent();
        String file = intent.getDataString();
        if (file == null) {
            log("No command file specified in the line arguments! Exiting now.");
            return;
        }
        fillImageFromFile(file);
        dumpPicture(file);
    }

    public void fillImageFromFile(String file)
    {
        Log.d(LOGTAG, "Opening the image file");
        File imageFile = new File(file);
        if (!imageFile.canRead()) {
            log("Image cannot be read!");
            return;
        }
        Log.d(LOGTAG, "Can read the file");

        try {
            FileInputStream fileStream = new FileInputStream(imageFile);
            Picture picture = Picture.createFromStream(fileStream);
            fileStream.close();
            Log.d(LOGTAG, "Creating the image with size (h = " + picture.getHeight() + ", w = " + picture.getWidth() + ")");

            mCanvasBitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mCanvasBitmap);

            long currentTime = System.currentTimeMillis();
            canvas.drawPicture(picture);
            long elapsedTime = System.currentTimeMillis() - currentTime;
            log("Replaying took: " + elapsedTime + " ms.");

            // Dump the previous information to the view.
            ImageView view = (ImageView) findViewById(R.id.img);
            view.setImageBitmap(mCanvasBitmap);
        } catch(Exception e) {
            log("Exception received: " + e.toString());
        }
    }

    public void dumpPicture(String originalFile) {
        if (mCanvasBitmap == null) {
            Log.d(LOGTAG, "No bitmap to dump! Bailing out!");
            return;
        }
        File resultFile = new File(originalFile + ".png");
        // FIXME: canWrite is wrong here so we do not check for now :-/
        try {
            FileOutputStream fileStream = new FileOutputStream(resultFile);
            // 100 means full quality.
            mCanvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileStream);
            fileStream.close();
        } catch(Exception e) {
            log("Exception received: " + e.toString());
        }
    }

    private void log(String message) {
        Log.d(LOGTAG, message);
        TextView  textView = (TextView) findViewById(R.id.text);
        textView.setText(message);
    }
    private Bitmap mCanvasBitmap;
}
