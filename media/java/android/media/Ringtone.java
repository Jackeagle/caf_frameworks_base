/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.media;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.provider.DrmStore;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.text.TextUtils;
import java.io.IOException;
import android.content.ContentUris;
import android.os.SystemProperties;

/**
 * Ringtone provides a quick method for playing a ringtone, notification, or
 * other similar types of sounds.
 * <p>
 * For ways of retrieving {@link Ringtone} objects or to show a ringtone
 * picker, see {@link RingtoneManager}.
 * 
 * @see RingtoneManager
 */
public class Ringtone {
    private static final String TAG = "Ringtone";
    private static final boolean LOGD = true;

    private static final String[] MEDIA_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE
    };

    private static final String[] DRM_COLUMNS = new String[] {
        DrmStore.Audio._ID,
        DrmStore.Audio.DATA,
        DrmStore.Audio.TITLE
    };

    private final Context mContext;
    private final AudioManager mAudioManager;
    private final boolean mAllowRemote;
    private final IRingtonePlayer mRemotePlayer;
    private final Binder mRemoteToken;

    private MediaPlayer mLocalPlayer;

    private Uri mUri;
    private String mTitle;

    private int mStreamType = AudioManager.STREAM_RING;

    /** {@hide} */
    public Ringtone(Context context, boolean allowRemote) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAllowRemote = allowRemote;
        mRemotePlayer = allowRemote ? mAudioManager.getRingtonePlayer() : null;
        mRemoteToken = allowRemote ? new Binder() : null;
    }

    /**
     * Sets the stream type where this ringtone will be played.
     * 
     * @param streamType The stream, see {@link AudioManager}.
     */
    public void setStreamType(int streamType) {
        mStreamType = streamType;

        // The stream type has to be set before the media player is prepared.
        // Re-initialize it.
        setUri(mUri);
    }

    /**
     * Gets the stream type where this ringtone will be played.
     * 
     * @return The stream type, see {@link AudioManager}.
     */
    public int getStreamType() {
        return mStreamType;
    }

    /**
     * Returns a human-presentable title for ringtone. Looks in media and DRM
     * content providers. If not in either, uses the filename
     * 
     * @param context A context used for querying. 
     */
    public String getTitle(Context context) {
        if (mTitle != null) return mTitle;
        return mTitle = getTitle(context, mUri, true);
    }

    private static String getTitle(Context context, Uri uri, boolean followSettingsUri) {
        Cursor cursor = null;
        ContentResolver res = context.getContentResolver();
        
        String title = null;

        if (uri != null) {
            String authority = uri.getAuthority();

            if (Settings.AUTHORITY.equals(authority)) {
                if (followSettingsUri) {
                    Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context,
                            RingtoneManager.getDefaultType(uri));
                    String actualTitle = getTitle(context, actualUri, false);
                    title = context
                            .getString(com.android.internal.R.string.ringtone_default_with_actual,
                                    actualTitle);
                }
            } else {
                try {
                    if (DrmStore.AUTHORITY.equals(authority)) {
                        cursor = res.query(uri, DRM_COLUMNS, null, null, null);
                    } else if (MediaStore.AUTHORITY.equals(authority)) {
                        cursor = res.query(uri, MEDIA_COLUMNS, null, null, null);
                    }
                } catch (SecurityException e) {
                    // missing cursor is handled below
                }

                try {
                    if (cursor != null && cursor.getCount() == 1) {
                        cursor.moveToFirst();
                        return cursor.getString(2);
                    } else {
                        title = uri.getLastPathSegment();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        if (title == null) {
            title = context.getString(com.android.internal.R.string.ringtone_unknown);
            
            if (title == null) {
                title = "";
            }
        }
        
        return title;
    }

    /**
     * Set {@link Uri} to be used for ringtone playback. Attempts to open
     * locally, otherwise will delegate playback to remote
     * {@link IRingtonePlayer}.
     *
     * @hide
     */
    public void setUri(Uri uri) {
        destroyLocalPlayer();

        mUri = uri;
        if (mUri == null) {
            return;
        }
        /* return if ringtone is set to Silent to avoid fall back to valid ringtone */
        if (mUri.toString().startsWith(Settings.System.DEFAULT_RINGTONE_URI.toString())) {
            String ringUri = Settings.System.getString(mContext.getContentResolver(),
                                                       mUri.getLastPathSegment());
        if (TextUtils.isEmpty(ringUri)) {
                Log.w(TAG, "ringUri " + ringUri);
                return;
            }
        }
  
        // TODO: detect READ_EXTERNAL and specific content provider case, instead of restore to default ringtone.
        restoreRingtoneIfNotExist(Settings.System.RINGTONE);
        restoreRingtoneIfNotExist(Settings.System.RINGTONE_2);

        // try opening uri locally before delegating to remote player
        mLocalPlayer = new MediaPlayer();
        try {
            mLocalPlayer.setDataSource(mContext, mUri);
            mLocalPlayer.setAudioStreamType(mStreamType);
            mLocalPlayer.prepare();

        } catch (SecurityException e) {
            destroyLocalPlayer();
            if (!mAllowRemote) {
                Log.w(TAG, "Remote playback not allowed: " + e);
            }
        } catch (IOException e) {
            destroyLocalPlayer();
            if (!mAllowRemote) {
                Log.w(TAG, "Remote playback not allowed: " + e);
            }
        }

        if(mLocalPlayer == null && !mAllowRemote) {
           Log.e(TAG,"failed to play current ringtone, fallback to any valid ringtone");
           mLocalPlayer = new MediaPlayer();
           try {
               mLocalPlayer.setDataSource(mContext, RingtoneManager.getValidRingtoneUri(mContext));
               mLocalPlayer.setAudioStreamType(mStreamType);
               mLocalPlayer.prepare();
           } catch (SecurityException e) {
               destroyLocalPlayer();
               Log.w(TAG, "fallback remote playback not allowed: " + e);
           } catch (IOException e) {
               destroyLocalPlayer();
               Log.w(TAG, "fallback remote playback not allowed: " + e);
           }
        }

        if (LOGD) {
            if (mLocalPlayer != null) {
                Log.d(TAG, "Successfully created local player");
            } else {
                Log.d(TAG, "Problem opening; delegating to remote player");
            }
        }
    }

    /** {@hide} */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Plays the ringtone.
     */
    public void play() {
        if (mLocalPlayer != null) {
            // do not play ringtones if stream volume is 0
            // (typically because ringer mode is silent).
            if (mAudioManager.getStreamVolume(mStreamType) != 0) {
                mLocalPlayer.start();
            }
        } else if (mAllowRemote) {
            try {
                mRemotePlayer.play(mRemoteToken, mUri, mStreamType);
            } catch (RemoteException e) {
                Log.w(TAG, "Problem playing ringtone: " + e);
            }
        } else {
            Log.w(TAG, "Neither local nor remote playback available");
        }
    }

    /**
     * Stops a playing ringtone.
     */
    public void stop() {
        if (mLocalPlayer != null) {
            destroyLocalPlayer();
        } else if (mAllowRemote) {
            try {
                mRemotePlayer.stop(mRemoteToken);
            } catch (RemoteException e) {
                Log.w(TAG, "Problem stopping ringtone: " + e);
            }
        }
    }

    private void destroyLocalPlayer() {
        if (mLocalPlayer != null) {
            mLocalPlayer.reset();
            mLocalPlayer.release();
            mLocalPlayer = null;
        }
    }

    /**
     * Whether this ringtone is currently playing.
     * 
     * @return True if playing, false otherwise.
     */
    public boolean isPlaying() {
        if (mLocalPlayer != null) {
            return mLocalPlayer.isPlaying();
        } else if (mAllowRemote) {
            try {
                return mRemotePlayer.isPlaying(mRemoteToken);
            } catch (RemoteException e) {
                Log.w(TAG, "Problem checking ringtone: " + e);
                return false;
            }
        } else {
            Log.w(TAG, "Neither local nor remote playback available");
            return false;
        }
    }

    void setTitle(String title) {
        mTitle = title;
    }

    /**
     * When playing ringtone or in Phone ringtone interface, check the corresponding file get from media
     * with the uri get from setting. If the file is not exist, restore to default ringtone.
     */
    private void restoreRingtoneIfNotExist(String settingName) {
        String ringtoneUri = Settings.System.getString(mContext.getContentResolver(),
                settingName);
        Cursor c = null;
        ContentResolver res = mContext.getContentResolver();
        try {
            if (ringtoneUri != null) {
                c = mContext.getContentResolver().query(
                        Uri.parse(ringtoneUri),
                        new String[] { MediaStore.Audio.Media.TITLE },
                        null, null, null);
            } else {
                Log.w(TAG,"It should be silent mode, and needn't to restore it.");
                return;
            }
            // Check whether the corresponding file of Uri is exist.
            if (!hasData(c)) {
                String defaultRingtoneFilename = SystemProperties.get("ro.config." + Settings.System.RINGTONE);
                if (c != null) {
                    c.close();
                }
                c = res.acquireProvider("media").query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                        new String[]{"_id"},
                        MediaStore.Audio.Media.DISPLAY_NAME + "=?",
                        new String[]{defaultRingtoneFilename},
                        null,null);

                // Set the setting to the Uri of default ringtone.
                if (hasData(c)) {
                    c.moveToFirst();
                    int rowId = c.getInt(0);
                    Log.e(TAG, "update Uri to new Uri " + ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, rowId).toString());
                    Settings.System.putString(mContext.getContentResolver(), settingName,
                            ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, rowId).toString());
                         
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in restoreRingtoneIfNotExist()", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private boolean hasData(Cursor c) {
        if (c != null && c.getCount() > 0) {
            return true;
        } else {
            return false;
        }
    }
}
