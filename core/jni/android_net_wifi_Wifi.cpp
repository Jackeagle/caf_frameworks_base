/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "wifi"

#include "jni.h"
#include <ScopedUtfChars.h>
#include <android_runtime/AndroidRuntime.h>

#include "wifi.h"

#define WIFI_PKG_NAME "android/net/wifi/WifiNative"
#define BUF_SIZE 256
#define EVENT_BUF_SIZE 2048

#define CONVERT_LINE_LEN 2048
#define CHARSET_CN ("gbk")

#include "android_net_wifi_Gbk2Utf.h"

namespace android {

static jint DBG = false;

extern struct accessPointObjectItem *g_pItemList;
extern struct accessPointObjectItem *g_pLastNode;
extern pthread_mutex_t *g_pItemListMutex;
extern String8 *g_pCurrentSSID;

static int doCommand(const char *ifname, const char *cmd, char *replybuf, int replybuflen)
{
    size_t reply_len = replybuflen - 1;

    if (::wifi_command(ifname, cmd, replybuf, &reply_len) != 0)
        return -1;
    else {
        // Strip off trailing newline
        if (reply_len > 0 && replybuf[reply_len-1] == '\n')
            replybuf[reply_len-1] = '\0';
        else
            replybuf[reply_len] = '\0';
        return 0;
    }
}

static jint doIntCommand(const char *ifname, const char* fmt, ...)
{
    char buf[BUF_SIZE];
    va_list args;
    va_start(args, fmt);
    int byteCount = vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    if (byteCount < 0 || byteCount >= BUF_SIZE) {
        return -1;
    }
    char reply[BUF_SIZE];
    if (doCommand(ifname, buf, reply, sizeof(reply)) != 0) {
        return -1;
    }
    return static_cast<jint>(atoi(reply));
}

static jboolean doBooleanCommand(const char *ifname, const char* expect, const char* fmt, ...)
{
    char buf[BUF_SIZE];
    jboolean ret = JNI_TRUE;
    va_list args;
    va_start(args, fmt);
    int byteCount = vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    if (byteCount < 0 || byteCount >= BUF_SIZE) {
        return JNI_FALSE;
    }
    char reply[BUF_SIZE];
    if (strstr(buf, "SET_NETWORK")) {
        ret = setNetworkVariable(buf);
    }
    if (ret == JNI_TRUE) {
        if (doCommand(ifname, buf, reply, sizeof(reply)) != 0) {
            return JNI_FALSE;
        }
    } else
        return JNI_FALSE;
    return (strcmp(reply, expect) == 0);
}

// Send a command to the supplicant, and return the reply as a String
static jstring doStringCommand(JNIEnv* env, const char *ifname, const char* fmt, ...) {
    char buf[BUF_SIZE];
    va_list args;
    va_start(args, fmt);
    int byteCount = vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    if (byteCount < 0 || byteCount >= BUF_SIZE) {
        return NULL;
    }
    char reply[4096];
    if (doCommand(ifname, buf, reply, sizeof(reply)) != 0) {
        return NULL;
    }
    // TODO: why not just NewStringUTF?
    if (DBG) ALOGD("cmd = %s, reply: %s", buf, reply);
    String16 str;

    if (strstr(buf, "BSS RANGE=")) {
        parseScanResults(str, reply);
    } else if (strstr(buf, "GET_NETWORK") && strstr(buf, "ssid")
                && !strstr(buf,"bssid") && !strstr(buf,"scan_ssid")){
        constructSsid(str, reply);
    } else {
        str += String16((char *)reply);
    }
    return env->NewString((const jchar *)str.string(), str.size());
}

static jboolean android_net_wifi_isDriverLoaded(JNIEnv* env, jobject)
{
    return (jboolean)(::is_wifi_driver_loaded() == 1);
}

static jboolean android_net_wifi_loadDriver(JNIEnv* env, jobject)
{
    g_pItemListMutex = new pthread_mutex_t;
    if (NULL == g_pItemListMutex) {
        ALOGE("Failed to allocate memory for g_pItemListMutex!");
        return JNI_FALSE;
    }
    pthread_mutex_init(g_pItemListMutex, NULL);

    return (jboolean)(::wifi_load_driver() == 0);
}

static jboolean android_net_wifi_unloadDriver(JNIEnv* env, jobject)
{
    if (g_pItemListMutex != NULL) {
        pthread_mutex_lock(g_pItemListMutex);
        struct accessPointObjectItem *pCurrentNode = g_pItemList;
        struct accessPointObjectItem *pNextNode = NULL;
        while (pCurrentNode) {
            pNextNode = pCurrentNode->pNext;
            if (NULL != pCurrentNode->ssid) {
                delete pCurrentNode->ssid;
                pCurrentNode->ssid = NULL;
            }
            if (NULL != pCurrentNode->ssid_utf8) {
                delete pCurrentNode->ssid_utf8;
                pCurrentNode->ssid_utf8 = NULL;
            }
            delete pCurrentNode;
            pCurrentNode = pNextNode;
        }
        g_pItemList = NULL;
        g_pLastNode = NULL;
        pthread_mutex_unlock(g_pItemListMutex);
        pthread_mutex_destroy(g_pItemListMutex);
        delete g_pItemListMutex;
        g_pItemListMutex = NULL;
    }
    return (jboolean)(::wifi_unload_driver() == 0);
}

static jboolean android_net_wifi_startSupplicant(JNIEnv* env, jobject, jboolean p2pSupported)
{
    return (jboolean)(::wifi_start_supplicant(p2pSupported) == 0);
}

static jboolean android_net_wifi_killSupplicant(JNIEnv* env, jobject, jboolean p2pSupported)
{
    return (jboolean)(::wifi_stop_supplicant(p2pSupported) == 0);
}

static jboolean android_net_wifi_connectToSupplicant(JNIEnv* env, jobject, jstring jIface)
{
    ScopedUtfChars ifname(env, jIface);
    return (jboolean)(::wifi_connect_to_supplicant(ifname.c_str()) == 0);
}

static void android_net_wifi_closeSupplicantConnection(JNIEnv* env, jobject, jstring jIface)
{
    ScopedUtfChars ifname(env, jIface);
    ::wifi_close_supplicant_connection(ifname.c_str());
}

static jstring android_net_wifi_waitForEvent(JNIEnv* env, jobject, jstring jIface)
{
    char buf[EVENT_BUF_SIZE];
    ScopedUtfChars ifname(env, jIface);
    int nread = ::wifi_wait_for_event(ifname.c_str(), buf, sizeof buf);
    if (nread > 0) {
	    if (strstr(buf, " SSID=") || strstr(buf, " SSID ")){
	        constructEventSsid(buf);
	    }
	    return env->NewStringUTF(buf);
    } else {
        return NULL;
    }
}

static jboolean android_net_wifi_doBooleanCommand(JNIEnv* env, jobject, jstring jIface,
        jstring jCommand)
{
    ScopedUtfChars ifname(env, jIface);
    ScopedUtfChars command(env, jCommand);

    if (command.c_str() == NULL) {
        return JNI_FALSE;
    }
    if (DBG) ALOGD("doBoolean: %s", command.c_str());
    return doBooleanCommand(ifname.c_str(), "OK", "%s", command.c_str());
}

static jint android_net_wifi_doIntCommand(JNIEnv* env, jobject, jstring jIface,
        jstring jCommand)
{
    ScopedUtfChars ifname(env, jIface);
    ScopedUtfChars command(env, jCommand);

    if (command.c_str() == NULL) {
        return -1;
    }
    if (DBG) ALOGD("doInt: %s", command.c_str());
    return doIntCommand(ifname.c_str(), "%s", command.c_str());
}

static jstring android_net_wifi_doStringCommand(JNIEnv* env, jobject, jstring jIface,
        jstring jCommand)
{
    ScopedUtfChars ifname(env, jIface);

    ScopedUtfChars command(env, jCommand);
    if (command.c_str() == NULL) {
        return NULL;
    }
    if (DBG) ALOGD("doString: %s", command.c_str());
    return doStringCommand(env, ifname.c_str(), "%s", command.c_str());
}



// ----------------------------------------------------------------------------

/*
 * JNI registration.
 */
static JNINativeMethod gWifiMethods[] = {
    /* name, signature, funcPtr */

    { "loadDriver", "()Z",  (void *)android_net_wifi_loadDriver },
    { "isDriverLoaded", "()Z",  (void *)android_net_wifi_isDriverLoaded },
    { "unloadDriver", "()Z",  (void *)android_net_wifi_unloadDriver },
    { "startSupplicant", "(Z)Z",  (void *)android_net_wifi_startSupplicant },
    { "killSupplicant", "(Z)Z",  (void *)android_net_wifi_killSupplicant },
    { "connectToSupplicant", "(Ljava/lang/String;)Z",
            (void *)android_net_wifi_connectToSupplicant },
    { "closeSupplicantConnection", "(Ljava/lang/String;)V",
            (void *)android_net_wifi_closeSupplicantConnection },
    { "waitForEvent", "(Ljava/lang/String;)Ljava/lang/String;",
            (void*) android_net_wifi_waitForEvent },
    { "doBooleanCommand", "(Ljava/lang/String;Ljava/lang/String;)Z",
            (void*) android_net_wifi_doBooleanCommand },
    { "doIntCommand", "(Ljava/lang/String;Ljava/lang/String;)I",
            (void*) android_net_wifi_doIntCommand },
    { "doStringCommand", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            (void*) android_net_wifi_doStringCommand },
};

int register_android_net_wifi_WifiManager(JNIEnv* env)
{
    return AndroidRuntime::registerNativeMethods(env,
            WIFI_PKG_NAME, gWifiMethods, NELEM(gWifiMethods));
}

}; // namespace android
