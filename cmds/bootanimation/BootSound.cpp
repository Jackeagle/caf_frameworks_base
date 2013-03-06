#define LOG_TAG "BootAnimation"

#include <stdint.h>
#include <sys/types.h>
#include <math.h>
#include <fcntl.h>
#include <utils/misc.h>
#include <signal.h>
#include <sys/stat.h>

#include <binder/IPCThreadState.h>
#include <utils/threads.h>
#include <utils/Atomic.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <cutils/properties.h>


#include "BootSound.h"

namespace android {

// ---------------------------------------------------------------------------

BootSound::BootSound() : Thread(false)
{
}

BootSound::~BootSound() {
}

status_t BootSound::readyToRun() {
    return NO_ERROR;
}

bool BootSound::threadLoop()
{
    /*
     * Now we just use mediaplayer to play boot music.
     * play shutdown music in ShutdownThread.java,so we don't care.
     *
     */
    if (checkBootState()) {
    mMediaPlayer->start();
    SLOGD("mMediaPlayer is playing");
    	}
    while (mMediaPlayer->isPlaying()) {
        sleep(1);
    }
    close(mFd);
    return false;
    	
}

status_t readNumber(char const* filename) {
    char buf[80];
    int fd = open(filename, O_RDONLY);
    if (fd < 0) {
        if (errno != ENOENT) SLOGD("Can't open %s: %s", filename, strerror(errno));
        return -1;
    }

    int len = read(fd, buf, sizeof(buf) - 1);
    if (len < 0) {
        SLOGD("Can't read %s: %s", filename, strerror(errno));
        close(fd);
        return -1;
    }

    close(fd);
    buf[len] = '\0';
    return atoll(buf);
}


bool BootSound::checkBootState(void)
{
    char value[PROPERTY_VALUE_MAX];
    bool ret = true;

    property_get("sys.shutdown.requested", value, "null");
	SLOGD("checkBootState =%s",value);
    if (strcmp(value, "null") != 0 || mShutdown) {
        ret = false;
    }

    return ret;
}

void BootSound::setFile(const char *file) {
    sprintf(mFileName, "%s", file);
	SLOGD("the boot sound file is %s", file);
    mFd = open(mFileName, O_RDONLY);
    if (mFd < 0) {
        fprintf(stderr, "Can not open %s\n", mFileName);
    }
    struct stat stat;
    fstat(mFd, &stat);
    mMediaPlayer = new MediaPlayer();
    mMediaPlayer->setVolume(0.7, 0.7); 
    mMediaPlayer->setAudioStreamType(AUDIO_STREAM_SYSTEM);
    mMediaPlayer->setDataSource(mFd, 0, stat.st_size);
    mMediaPlayer->prepare();
	
}
// ---------------------------------------------------------------------------

}
; // namespace android
