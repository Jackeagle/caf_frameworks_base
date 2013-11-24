/*
 * Copyright (c) 2011, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of The Linux Foundation nor
 *      the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#define LOG_TAG "ActTriggerJNI"

#include "jni.h"
#include "JNIHelp.h"
#include <android_runtime/AndroidRuntime.h>

#include <dlfcn.h>
#include <limits.h>
#include <string.h>

#include <cutils/properties.h>
#include <utils/Log.h>

#define LIBRARY_PATH_PREFIX_OLD	"/system/lib/"
#define LIBRARY_PATH_PREFIX "/vendor/lib/"
#define NUM_DL_LIBRARIES 2

namespace android
{

// ----------------------------------------------------------------------------
/*
 * Stuct containing handle to dynamically loaded lib as well as function
 * pointers to key interfaces.
 */
struct stateChangeHandler {
    void *dlhandle;
    void (*startActivity)(const char *);
    void (*resumeActivity)(const char *);
    void (*init)(void);
    void (*deinit)(void);
};

/*
 * Array of stateChangeHandler
 * library -both handlers for Start and Resume events.
 */
static struct stateChangeHandler mStateChangeHandlers[NUM_DL_LIBRARIES];
static size_t initIndex = 0;
static size_t gestIndex = 1;

// ----------------------------------------------------------------------------

static void
com_android_internal_app_ActivityTrigger_native_at_init()
{
    const char *rc;
    char buf[PROPERTY_VALUE_MAX];
    int len;
    bool symError = false;

    /* Retrieve name of vendor extension library */
    if (property_get("ro.vendor.extension_library", buf, NULL) <= 0) {
        return;
    }

    /* Sanity check - ensure */
    buf[PROPERTY_VALUE_MAX-1] = '\0';
    if (((strncmp(buf, LIBRARY_PATH_PREFIX,
            sizeof(LIBRARY_PATH_PREFIX) - 1) != 0)
    &&
    (strncmp(buf, LIBRARY_PATH_PREFIX_OLD,
            sizeof(LIBRARY_PATH_PREFIX_OLD) - 1) != 0))
        ||
        (strstr(buf, "..") != NULL)) {
        return;
    }

    // ----------------------------------
    /*
     * Clear error and load lib.
     */
    dlerror();
    mStateChangeHandlers[initIndex].dlhandle =
            dlopen(buf, RTLD_NOW | RTLD_LOCAL);
    if(mStateChangeHandlers[initIndex].dlhandle != 0) {
        *(void **)(&mStateChangeHandlers[initIndex].startActivity) =
                dlsym(mStateChangeHandlers[initIndex].dlhandle,
                        "activity_trigger_start");
        if ((rc = dlerror()) != NULL) {
            symError = true;
        }

        *(void **)(&mStateChangeHandlers[initIndex].resumeActivity) =
                dlsym(mStateChangeHandlers[initIndex].dlhandle,
                        "activity_trigger_resume");
        if ((rc = dlerror()) != NULL) {
            symError = true;
        }

        *(void **)(&mStateChangeHandlers[initIndex].init) =
                dlsym(mStateChangeHandlers[initIndex].dlhandle,
                        "activity_trigger_init");
        if ((rc = dlerror()) != NULL) {
            symError = true;
        }

        *(void **)(&mStateChangeHandlers[initIndex].deinit) =
                dlsym(mStateChangeHandlers[initIndex].dlhandle,
                        "activity_trigger_deinit");
        if ((rc = dlerror()) != NULL) {
            //may not be an error condition, deinit is not always available
        }

        //check for sym load errors
        if (symError == true) {
            if (mStateChangeHandlers[initIndex].dlhandle)
            {
                dlclose(mStateChangeHandlers[initIndex].dlhandle);
                memset(&mStateChangeHandlers[initIndex], 0,
                        sizeof(struct stateChangeHandler));
            }
        } else {
            (*mStateChangeHandlers[initIndex].init)();
        }
    }
    // ----------------------------------

    /*
     * Clear error and load gesture activity trigger
     */
    dlerror();
    symError = false;
    mStateChangeHandlers[gestIndex].dlhandle =
            dlopen("/vendor/lib/libmmgesture-activity-trigger.so",
                    RTLD_NOW | RTLD_LOCAL);
    if(mStateChangeHandlers[gestIndex].dlhandle != 0) {
        *(void **)(&mStateChangeHandlers[gestIndex].startActivity) =
                dlsym(mStateChangeHandlers[gestIndex].dlhandle,
                        "activity_trigger_start");
        if ((rc = dlerror()) != NULL) {
            symError = true;
        }

        *(void **)(&mStateChangeHandlers[gestIndex].resumeActivity) =
                dlsym(mStateChangeHandlers[gestIndex].dlhandle,
                        "activity_trigger_resume");
        if ((rc = dlerror()) != NULL) {
            symError = true;
        }

        *(void **)(&mStateChangeHandlers[gestIndex].init) =
                dlsym(mStateChangeHandlers[gestIndex].dlhandle,
                        "activity_trigger_init");
        if ((rc = dlerror()) != NULL) {
            symError = true;
        }

        *(void **)(&mStateChangeHandlers[gestIndex].deinit) =
                dlsym(mStateChangeHandlers[gestIndex].dlhandle,
                        "activity_trigger_deinit");
        if ((rc = dlerror()) != NULL) {
            symError = true;
        }

        if (symError == true) {
            if (mStateChangeHandlers[gestIndex].dlhandle)
            {
                dlclose(mStateChangeHandlers[gestIndex].dlhandle);
                memset(&mStateChangeHandlers[gestIndex], 0,
                        sizeof(struct stateChangeHandler));
            }
        } else {
            (*mStateChangeHandlers[gestIndex].init)();
        }
    }

    return;
}

static void
com_android_internal_app_ActivityTrigger_native_at_deinit(JNIEnv *env,
        jobject clazz)
{
    for(size_t i = 0; i < NUM_DL_LIBRARIES; i++){
        if(mStateChangeHandlers[i].deinit) {
            (*mStateChangeHandlers[i].deinit)();
        }
    }
}

static void
com_android_internal_app_ActivityTrigger_native_at_startActivity(JNIEnv *env,
        jobject clazz, jstring activity)
{
    for(size_t i = 0; i < NUM_DL_LIBRARIES; i++){
        if(mStateChangeHandlers[i].startActivity && activity) {
            const char *actStr = env->GetStringUTFChars(activity, NULL);
            if (actStr) {
                (*mStateChangeHandlers[i].startActivity)(actStr);
                env->ReleaseStringUTFChars(activity, actStr);
            }
        }
    }
}

static void
com_android_internal_app_ActivityTrigger_native_at_resumeActivity(JNIEnv *env,
        jobject clazz, jstring activity)
{
    for(size_t i = 0; i < NUM_DL_LIBRARIES; i++){
        if(mStateChangeHandlers[i].resumeActivity && activity) {
            const char *actStr = env->GetStringUTFChars(activity, NULL);
            if (actStr) {
                (*mStateChangeHandlers[i].resumeActivity)(actStr);
                env->ReleaseStringUTFChars(activity, actStr);
            }
        }
    }
}

// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
    {"native_at_startActivity",  "(Ljava/lang/String;)V",
            (void *)com_android_internal_app_ActivityTrigger_native_at_startActivity},
    {"native_at_resumeActivity", "(Ljava/lang/String;)V",
            (void *)com_android_internal_app_ActivityTrigger_native_at_resumeActivity},
    {"native_at_deinit",         "()V",
            (void *)com_android_internal_app_ActivityTrigger_native_at_deinit},
};


int register_com_android_internal_app_ActivityTrigger(JNIEnv *env)
{
    com_android_internal_app_ActivityTrigger_native_at_init();

    return AndroidRuntime::registerNativeMethods(env,
            "com/android/internal/app/ActivityTrigger", gMethods, NELEM(gMethods));
}

}   // namespace android
