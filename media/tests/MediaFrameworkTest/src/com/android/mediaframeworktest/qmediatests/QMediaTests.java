/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.mediaframeworktest.qmediatests;

import com.android.mediaframeworktest.MediaFrameworkTest;
import com.android.mediaframeworktest.MediaNames;

import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FilenameFilter;
import java.io.FileFilter;

import android.media.MediaMetadataRetriever;

/**
 * Junit / Instrumentation - performance measurement for media player and
 * recorder
 */
public class QMediaTests extends ActivityInstrumentationTestCase<MediaFrameworkTest> {

    private String TAG = "QMediaTests";

    private SQLiteDatabase mDB;
    private SurfaceHolder mSurfaceHolder = null;
    private static final int NUM_STRESS_LOOP = 10;
    private static final int NUM_PLAYBACk_IN_EACH_LOOP = 100;
    private static final long MEDIA_STRESS_WAIT_TIME_5SEC = 5000; //5 seconds
    private static final long MEDIA_STRESS_WAIT_TIME_1SEC = 1000; //1 seconds
    private static final long MEDIA_STRESS_WAIT_TIME = 5000; //5 seconds

    public QMediaTests() {
        super("com.android.mediaframeworktest", MediaFrameworkTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void createDB() {
        mDB = SQLiteDatabase.openOrCreateDatabase("/sdcard/perf.db", null);
        mDB.execSQL("CREATE TABLE perfdata (_id INTEGER PRIMARY KEY," +
                "file TEXT," + "setdatatime LONG," + "preparetime LONG," +
                "playtime LONG" + ");");
    }

    // For 100 times play and pause with 1sec duration
    @LargeTest
    public void testMP3Playback_1sec() throws Exception {
            audiomediaStressPlayback(MediaNames.MP3CBR, MEDIA_STRESS_WAIT_TIME_1SEC);
    }

    // 0. Play for 10 second
    // 1. Play for 1 second
    // 2. FF 3seconds
    // 3. Play for 1 second
    // 4. Rewind 5 seconds
    // 5. Go to step 1
    // duration
    @LargeTest
    public void testMP3Playback_ff_rew() throws Exception {
        MediaPlayer mp = new MediaPlayer();
        try {
        mp.setDataSource(MediaNames.MP3CBR);
        mp.prepare();
        Log.e(TAG, "play 10sec");
        mp.start();
        Thread.sleep(10000); //sleep for 10 second
        mp.pause();
        for (int i = 0; i < NUM_PLAYBACk_IN_EACH_LOOP; i++) {
                Log.e(TAG, "current position " + mp.getCurrentPosition());
                Log.e(TAG, "play 1sec");
                mp.start();
                Thread.sleep(1000); //sleep for 1 second
                mp.pause();
                Log.e(TAG, "current position " + mp.getCurrentPosition());

                Log.e(TAG, "seek 3sec FF");
                mp.seekTo((mp.getCurrentPosition())+ 3000); //ff 3 seconds
                Log.e(TAG, "current position " + mp.getCurrentPosition());
                mp.start();
                Log.e(TAG, "play 1sec");
                Thread.sleep(1000); //sleep for 1 second
                mp.pause();

                Log.e(TAG, "current position " + mp.getCurrentPosition());

                Log.e(TAG, "seek -5sec REW"); //rew 5 seconds
                mp.seekTo((mp.getCurrentPosition())- 5000);

                Log.e(TAG, "current position " + mp.getCurrentPosition());
            }
        } catch (Exception e) {
                mp.release();
                Log.e(TAG, e.toString());
      }
    }

    // Stress audio file playback
    // stress_wait_time value it should be played and then paused
    public void audiomediaStressPlayback(String testFilePath,long stress_wait_time) {
        MediaPlayer mp = new MediaPlayer();
        try {
        mp.setDataSource(testFilePath);
        mp.prepare();
        for (int i = 0; i < NUM_PLAYBACk_IN_EACH_LOOP; i++) {
                Log.e(TAG, "start playback");
                mp.start();
                Thread.sleep(stress_wait_time);
                mp.pause();
                Log.e(TAG, "pause playback");
            }
        } catch (Exception e) {
                mp.release();
                Log.e(TAG, e.toString());
      }
   }

  // List the files with extension "ff" in directory "dirName"
  private static String[] fileListing(String dirName, String ff) {
    final String   extension = ff;
    File           dir     = new File(dirName);
    FilenameFilter filter  = null;

    if (extension != null) {
      filter = new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return name.endsWith(extension);
         }
      };
    }
    return dir.list(filter);
  }


  // Play video file
  // 1. Play for half the duration of the file.
  // 2. Pause for 2 seconds.
  // 3. Seek to 1/4th duration of the file from the start.
  // 3. Play for 1/4th duration of the file.
  // 4. Stop the playback.
  // 5. Goto next file in the path.
  private static boolean playVideoFile(String filePath, String extension){
     int duration = 0;
     String[] files = fileListing(filePath, extension);
     if (files != null) {
        for (int i=0; i<files.length; i++) {
           if (!(new File(filePath + File.separatorChar + files[i])).isDirectory())
              System.out.println(filePath + File.separatorChar + files[i]);

           MediaPlayer mp = new MediaPlayer();
           try{
              mp.setDataSource(filePath + File.separatorChar + files[i]);
              mp.setDisplay(MediaFrameworkTest.mSurfaceView.getHolder());
              mp.prepare();
              duration = mp.getDuration();
              System.out.println("File duration:" + duration);
              mp.start();
              Thread.sleep(duration/2);
              System.out.println("File paused at :" + mp.getCurrentPosition());
              mp.pause();
              Thread.sleep(2000);
              mp.seekTo(duration/4);
              System.out.println("File seekTo :" + mp.getCurrentPosition());
              mp.start();
              Thread.sleep(duration/4);
              System.out.println("File End at :" + mp.getCurrentPosition());
              mp.stop();
           }catch (Exception e){
         //   Log.e(TAG, e.toString());
           }
           mp.stop();
           mp.release();
        }
     }
     return true;
  }

    // Play video file
    // 1. Play for half the duration of the file.
    // 2. Pause for 2 seconds.
    // 3. Seek to 1/4th duration of the file from the start.
    // 3. Play for 1/4th duration of the file.
    // 4. Stop the playback.
    // 5. Goto next file in the path.
    private static boolean playAudioFile(String filePath, String extension){
        int duration = 0;
        String[] files = fileListing(filePath, extension);
        if (files != null) {
         for (int i=0; i<files.length; i++) {
            if (!(new File(filePath + File.separatorChar + files[i])).isDirectory())
                System.out.println(filePath + File.separatorChar + files[i]);

        MediaPlayer mp = new MediaPlayer();
        try{
            mp.setDataSource(filePath + File.separatorChar + files[i]);
            mp.prepare();
            duration = mp.getDuration();
            System.out.println("File duration:" + duration);
            mp.start();
            Thread.sleep(duration/2);
            mp.pause();
            Thread.sleep(2000);
            mp.seekTo(duration/4);
            mp.start();
            Thread.sleep(duration/4);
            mp.stop();
        }catch (Exception e){
//            Log.e(TAG, e.toString());
        }
        mp.stop();
        mp.release();
     }
     }
            return true;
    }

    /* STRESS VIDEO FILES */

    @LargeTest
    public void mp4_stress() throws Exception {
       playVideoFile(MediaNames.VIDEO_MP4_PATH, "mp4");
    }

    @LargeTest
    public void video_3gp_stress() throws Exception {
       playVideoFile(MediaNames.VIDEO_3GP_PATH, "3gp");
    }

    @LargeTest
    public void h263_aac_stress() throws Exception {
       playVideoFile(MediaNames.VIDEO_H263_AAC_PATH, "3gp");
    }

    @LargeTest
    public void h263_amr_stress() throws Exception {
       playVideoFile(MediaNames.VIDEO_H263_AMR_PATH, "3gp");
    }

    @LargeTest
    public void h264_aac_stress() throws Exception {
       playVideoFile(MediaNames.VIDEO_H264_AAC_PATH, "3gp");
    }

    @LargeTest
    public void h264_amr_stress() throws Exception {
       playVideoFile(MediaNames.VIDEO_H264_AMR_PATH, "3gp");
    }

    @LargeTest
    public void wmv_stress() throws Exception {
       playVideoFile(MediaNames.VIDEO_WMV_PATH, "wmv");
    }

    /* STRESS AUDIO FILES */

    @LargeTest
    public void mp3_stress() throws Exception {
       playAudioFile(MediaNames.MP3CBR_PATH, "mp3");
    }

    @LargeTest
    public void midi_stress() throws Exception {
       playAudioFile(MediaNames.MIDI_PATH, "mid");
    }

    @LargeTest
    public void wma9_stress() throws Exception {
       playAudioFile(MediaNames.WMA9_PATH, "wma");
    }

    @LargeTest
    public void wma10_stress() throws Exception {
       playAudioFile(MediaNames.WMA10_PATH, "wma");
    }

    @LargeTest
    public void wav_stress() throws Exception {
       playAudioFile(MediaNames.WAV_PATH, "wav");
    }

    @LargeTest
    public void amr_stress() throws Exception {
       playAudioFile(MediaNames.AMR_PATH, "amr");
    }

    @LargeTest
    public void ogg_stress() throws Exception {
       playAudioFile(MediaNames.OGG_PATH, "ogg");
    }
}
