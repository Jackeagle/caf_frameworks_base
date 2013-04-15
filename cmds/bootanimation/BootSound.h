#ifndef ANDROID_BOOTSOUND_H
#define ANDROID_BOOTSOUND_H

#include <stdint.h>
#include <sys/types.h>
#include <media/mediaplayer.h>
#include <media/AudioSystem.h>

#include <utils/threads.h>

namespace android {

// ---------------------------------------------------------------------------

class BootSound : public Thread
{
public:
                 BootSound();
    virtual     ~BootSound();
    void setFile(const char *file);
    bool checkBootState();
    bool mShutdown; 
private:
    virtual bool        threadLoop();
    virtual status_t    readyToRun();
     
    char mFileName[128];
    int mFd;
    MediaPlayer *mMediaPlayer;
};

// ---------------------------------------------------------------------------

}; // namespace android

#endif // ANDROID_BOOTSOUND_H
