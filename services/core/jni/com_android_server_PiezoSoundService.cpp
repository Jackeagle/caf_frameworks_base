/*
*
* Copyright (c) 2015, The Linux Foundation. All rights reserved.
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
*
*/


#define LOG_TAG "PiezoSoundService-JNI"
#define PIEZO_JAVA_CLASS_NAME "com/android/server/PiezoSoundService"

#include "jni.h"
#include "JNIHelp.h"
#include "JniInvocation.h"
#include "android_runtime/AndroidRuntime.h"
#include "android_runtime/Log.h"


#include <utils/misc.h>
#include <utils/Log.h>
#include <hardware/hardware.h>
#include <hardware/piezosound.h>

#include <stdio.h>
#include <pthread.h>

static jobject mCallbacksObj = NULL;
static jmethodID method_runCmdNotification;

struct piezo_jni_invocation_info {
    JavaVM *vm;
    jclass javaClass;
};

struct piezo_server_jni_info{
    int id;
    int dev;
    piezo_params_t params;
    pthread_t thread;
    struct piezo_jni_invocation_info invoc;
};

static struct piezo_server_jni_info piezo_jni_instance;
static int piezo_is_cmd_completed = 0;
static pthread_mutex_t piezo_jni_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t piezo_jni_wait_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t piezo_jni_cmd_wait_cond = PTHREAD_COND_INITIALIZER;
static piezo_device_t* piezo_device_hal = NULL;

namespace android
{
static void check_clear_exception_from_callback(JNIEnv* env, const char* methodName) {
    if (env->ExceptionCheck()) {
        ALOGE("An exception was thrown by callback '%s'.", methodName);
        LOGE_EX(env);
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

static void run_cmd_callback(void *pData)
{
    ALOGD("%s: called cmd callback", __func__);
    pthread_mutex_lock(&piezo_jni_wait_mutex);
    piezo_is_cmd_completed = 1;
    pthread_cond_signal(&piezo_jni_cmd_wait_cond);
    pthread_mutex_unlock(&piezo_jni_wait_mutex);
}

piezo_callbacks sPiezoCallbacks = {
    sizeof(piezo_callbacks),
    run_cmd_callback
};

static int get_piezo_device(hw_module_t* module)
{
    int rc = 0;

    rc = module->methods->open(module, PIEZO_HARDWARE_MODULE_ID,
                               (hw_device_t**)&piezo_device_hal);
    if (rc != 0) {
        piezo_device_hal = NULL;
        ALOGE("unable to get piezo device");
    }

    return rc;
}

static jint init_module(JNIEnv *env, jobject clazz)
{
    int rc;
    hw_module_t* pModule;

    ALOGD("%s: called", __func__);
    rc = hw_get_module(PIEZO_HARDWARE_MODULE_ID, (hw_module_t const**)&pModule);
    if (rc == 0) {
        rc = get_piezo_device(pModule);
        ALOGD("piezoDev(%d), module(%d)", (int)piezo_device_hal, (int)pModule);

        if (rc == 0) {
            if (!mCallbacksObj)
            mCallbacksObj = env->NewGlobalRef(clazz);

            if(piezo_device_hal != NULL)
                piezo_device_hal->register_callbacks(&sPiezoCallbacks);
            else
                ALOGE("piezo_device NULL");

            memset((void*)&piezo_jni_instance, 0,
                   sizeof(struct piezo_server_jni_info));
            pthread_mutex_init(&piezo_jni_mutex, NULL);
            pthread_mutex_init(&piezo_jni_wait_mutex, NULL);
            pthread_cond_init(&piezo_jni_cmd_wait_cond, NULL);
        }
        else {
            ALOGE("get piezo device error rc:%d", rc);
        }
    }

    return (jint)piezo_device_hal;
}

static void finalize_module(JNIEnv *env, jobject clazz, int ptr)
{
    piezo_device_t *pPiezoDev = (piezo_device_t *)ptr;

    if (pPiezoDev != NULL) {
        free(pPiezoDev);
    }

    return;
}

void run_cmd_thread_cleanup(void *data)
{
    pthread_mutex_lock(&piezo_jni_mutex);
    piezo_jni_instance.id = 0;
    pthread_mutex_unlock(&piezo_jni_mutex);

}

static void exec_run_piezo(int ptr, piezo_params_t *ptrParams)
{
    piezo_device_t *ptrPiezoDev = (piezo_device_t *)ptr;

    if (ptrPiezoDev != NULL){
        ptrPiezoDev->run_piezo(ptrParams);
    } else {
        ALOGE("run_piezo ptr null");
    }
}

void *run_cmd_thread(void *arg)
{
    JavaVM *vm;
    jclass objClass;
    JNIEnv* env;
    int result, piezoDev;

    ALOGD("%s: entered", __func__);
    piezo_is_cmd_completed = 0;
    pthread_cleanup_push(run_cmd_thread_cleanup, arg);

    pthread_mutex_lock(&piezo_jni_mutex);
    vm = piezo_jni_instance.invoc.vm;
    objClass = piezo_jni_instance.invoc.javaClass;

    if (vm == NULL) {
        ALOGE("VM is null\n");
    }

    ALOGD("%s: attach curr thread\n", __func__);
    vm->AttachCurrentThread(&env, NULL);

    ALOGD("%s: thread attached\n", __func__);

    method_runCmdNotification = env->GetMethodID(objClass,
        "cmdDoneNotification", "()V");

    ALOGD("%s: get method id \n", __func__);

    exec_run_piezo(piezo_jni_instance.dev, &piezo_jni_instance.params);
    pthread_mutex_unlock(&piezo_jni_mutex);

    ALOGD("%s: wait for cmd to finish", __func__);
    /* Wait for run command to finish. Piezo HAL will invoke the callback
        * The callback will signal the condition
        */
    pthread_mutex_lock(&piezo_jni_wait_mutex);
    if (piezo_is_cmd_completed != 1) {
        pthread_cond_wait(&piezo_jni_cmd_wait_cond, &piezo_jni_wait_mutex);
    }
    pthread_mutex_unlock(&piezo_jni_wait_mutex);

    ALOGD("%s: wait done", __func__);

    if (method_runCmdNotification != NULL) {
        ALOGD("%s: calling Java cmdDone", __func__);
        env->CallVoidMethod(mCallbacksObj, method_runCmdNotification);
        check_clear_exception_from_callback(env, __func__);
    }

    ALOGD("%s: detach run cmd thread", __func__);
    vm->DetachCurrentThread();
    pthread_cleanup_pop(1);

    return NULL;
}

static void run_piezo_native(JNIEnv *env, jobject clazz, int ptr, int id,
        int freq, int duration, int dutyCycle)
{
    piezo_device_t *ptrPiezoDev = (piezo_device_t *)ptr;
    struct piezo_jni_invocation_info info;
    JavaVM *vm;
    jint result;

    ALOGD("piezoDev : %d \n", piezo_device_hal);
    pthread_mutex_lock(&piezo_jni_mutex);

    result = env->GetJavaVM(&piezo_jni_instance.invoc.vm);
    if (piezo_jni_instance.invoc.vm == NULL) {
        ALOGE("VM is null\n");
    }

    jclass objClass = jclass(env->NewGlobalRef(clazz));

    piezo_jni_instance.invoc.javaClass = objClass;
    piezo_jni_instance.dev = ptr;
    piezo_jni_instance.params.frequency = freq;
    piezo_jni_instance.params.duration = duration;
    piezo_jni_instance.params.duty_cycle = dutyCycle;
    piezo_jni_instance.id = id;
    pthread_mutex_unlock(&piezo_jni_mutex);

    result = pthread_create(&(piezo_jni_instance.thread), NULL,
                            run_cmd_thread, NULL);

}

static void cancel_piezo_native(JNIEnv *env, jobject clazz, int ptr, int id)
{
    piezo_device_t *ptrPiezoDev = (piezo_device_t *)ptr;

    pthread_mutex_lock(&piezo_jni_mutex);
    if (piezo_jni_instance.id == id) {

        ALOGD("%s: piezo running\n", __func__);
        if (pthread_kill(piezo_jni_instance.thread, SIGUSR1) != 0) {
           ALOGE("%s:g_monitor_thread kill failure!\n", __func__);
        }
        ALOGD("%s: thread canceled \n", __func__);

        if (ptrPiezoDev != NULL){
            ptrPiezoDev->cancel_piezo();
        } else {
            ALOGE("cancel_piezo ptr null");
        }
        piezo_jni_instance.id = 0;
    }
    else {
        ALOGE("%s: cancel id not match:%d saved:%d \n", __func__, id, piezo_jni_instance.id);
    }
    pthread_mutex_unlock(&piezo_jni_mutex);
}

static JNINativeMethod method_table[] = {
    { "init_module", "()I", (void*)init_module },
    { "finalize_module", "(I)V", (void*)finalize_module },
    { "run_piezo_native", "(IIIII)V", (void*)run_piezo_native },
    { "cancel_piezo_native", "(II)V", (void*)cancel_piezo_native },
};

int register_android_server_PiezoSoundService(JNIEnv *env)
{
    return jniRegisterNativeMethods(env, "com/android/server/PiezoSoundService",
            method_table, NELEM(method_table));
}

};


