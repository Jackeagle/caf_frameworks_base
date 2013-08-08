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
#include <utils/misc.h>
#include <android_runtime/AndroidRuntime.h>
#include <utils/Log.h>
#include <utils/String16.h>

#include "wifi.h"

#define WIFI_PKG_NAME "android/net/wifi/WifiNative"
#define BUF_SIZE 256
#define EVENT_BUF_SIZE 2048
#include <utils/String8.h>
#include <pthread.h>
#include <unicode/ucnv.h>
#include <unicode/ucsdet.h>
#include <net/if.h>
#include <sys/socket.h>
#include <linux/wireless.h>



#define CONVERT_LINE_LEN 2048
#define CHARSET_CN ("gbk")


namespace android {

static jint DBG = false;
struct accessPointObjectItem {
	String8 *ssid;
	String8 *bssid;
	bool    isCh;
	struct  accessPointObjectItem *pNext;
};

struct accessPointObjectItem *g_pItemList = NULL;
struct accessPointObjectItem *g_pLastNode = NULL;
pthread_mutex_t *g_pItemListMutex = NULL;
String8 *g_pCurrentSSID = NULL;
bool g_isChSSID = false;

static void addAPObjectItem(const char *ssid, const char *bssid, bool isCh)
{
	if (NULL == ssid || NULL == bssid) {
		ALOGE("ssid or bssid is NULL");
		return;
	}

	struct accessPointObjectItem *pTmpItemNode = NULL;
	struct accessPointObjectItem *pItemNode = NULL;
	bool foundItem = false;
	pthread_mutex_lock(g_pItemListMutex);
	pTmpItemNode = g_pItemList;
	while (pTmpItemNode) {
		if (pTmpItemNode->bssid && (*(pTmpItemNode->bssid) == bssid)) {
			foundItem = true;
			break;
		}
		pTmpItemNode = pTmpItemNode->pNext;
	}
	if (foundItem) {
		*(pTmpItemNode->ssid) = ssid;
		pTmpItemNode->isCh = isCh;
		if (DBG)
			ALOGD("Found AP %s", pTmpItemNode->ssid->string());
	} else {
		pItemNode = new struct accessPointObjectItem();
		if (NULL == pItemNode) {
			ALOGE("Failed to allocate memory for new item!");
			goto EXIT;
		}
		memset(pItemNode, 0, sizeof(accessPointObjectItem));
		pItemNode->bssid = new String8(bssid);
		if (NULL == pItemNode->bssid) {
			ALOGE("Failed to allocate memory for new bssid!");
			delete pItemNode;
			goto EXIT;
		}
		pItemNode->ssid = new String8(ssid);
		if (NULL == pItemNode->ssid) {
			ALOGE("Failed to allocate memory for new ssid!");
			delete pItemNode->bssid;
			delete pItemNode;
			goto EXIT;
		}
		pItemNode->isCh = isCh;
		pItemNode->pNext = NULL;
		if (DBG)
			ALOGD("AP doesn't exist, new one for %s", ssid);
		if (NULL == g_pItemList) {
			g_pItemList = pItemNode;
			g_pLastNode = g_pItemList;
		} else {
			g_pLastNode->pNext = pItemNode;
			g_pLastNode = pItemNode;
		}
	}

EXIT:
	pthread_mutex_unlock(g_pItemListMutex);
}

static int hex2num(char c)
{
	if (c >= '0' && c <= '9')
		return c - '0';
	if (c >= 'a' && c <= 'f')
		return c - 'a' + 10;
	if (c >= 'A' && c <= 'F')
		return c - 'A' + 10;
	return -1;
}


static int hex2byte(const char *hex)
{
	int a, b;
	a = hex2num(*hex++);
	if (a < 0)
		return -1;
	b = hex2num(*hex++);
	if (b < 0)
		return -1;
	return (a << 4) | b;
}

size_t ssid_decode(char *buf, size_t maxlen, const char *str)
{
	const char *pos = str;
	size_t len = 0;
	int val;

	while (*pos) {
		if (len == maxlen)
			break;
		switch (*pos) {
		case '\\':
			pos++;
			switch (*pos) {
			case '\\':
				buf[len++] = '\\';
				pos++;
				break;
			case '"':
				buf[len++] = '"';
				pos++;
				break;
			case 'n':
				buf[len++] = '\n';
				pos++;
				break;
			case 'r':
				buf[len++] = '\r';
				pos++;
				break;
			case 't':
				buf[len++] = '\t';
				pos++;
				break;
			case 'e':
				buf[len++] = '\e';
				pos++;
				break;
			case 'x':
				pos++;
				val = hex2byte(pos);
				if (val < 0) {
					val = hex2num(*pos);
					if (val < 0)
						break;
					buf[len++] = val;
					pos++;
				} else {
					buf[len++] = val;
					pos += 2;
				}
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
				val = *pos++ - '0';
				if (*pos >= '0' && *pos <= '7')
					val = val * 8 + (*pos++ - '0');
				if (*pos >= '0' && *pos <= '7')
					val = val * 8 + (*pos++ - '0');
				buf[len++] = val;
				break;
			default:
				break;
			}
			break;
		default:
			buf[len++] = *pos++;
			break;
		}
	}

	return len;
}

void ssid_encode(char *txt, size_t maxlen, const char *data, unsigned int len)
{
	char *end = txt + maxlen;
	size_t i;

	for (i = 0; i < len; i++) {
		if (txt + 4 > end)
			break;

		switch (data[i]) {
		case '\"':
			*txt++ = '\\';
			*txt++ = '\"';
			break;
		case '\\':
			*txt++ = '\\';
			*txt++ = '\\';
			break;
		case '\e':
			*txt++ = '\\';
			*txt++ = 'e';
			break;
		case '\n':
			*txt++ = '\\';
			*txt++ = 'n';
			break;
		case '\r':
			*txt++ = '\\';
			*txt++ = 'r';
			break;
		case '\t':
			*txt++ = '\\';
			*txt++ = 't';
			break;
		default:
			if (DBG)
				ALOGD("%s, data[%d] = %d",__FUNCTION__, i, data[i]);
			if (data[i] >= 32 && data[i] <= 127) {
				*txt++ = data[i];
			} else {
				txt += snprintf(txt, end - txt, "\\x%02x",
						data[i]);
			}
			break;
		}
	}
	*txt = '\0';
}


static bool isUTF8String(const char* str, long length)
{
	unsigned int nBytes = 0;
	unsigned char chr;
	bool bAllAscii = true;
	for (int i = 0; i < length; i++) {
		chr = *(str+i);
		if ((chr & 0x80) != 0) {
			bAllAscii = false;
		}
		if (0 == nBytes) {
			if (chr >= 0x80) {
				if (chr >= 0xFC && chr <= 0xFD) {
					nBytes = 6;
				} else if (chr >= 0xF8) {
					nBytes = 5;
				} else if (chr >= 0xF0) {
					nBytes = 4;
				} else if (chr >= 0xE0) {
					nBytes = 3;
				} else if (chr >= 0xC0) {
					nBytes = 2;
				} else {
					return false;
				}
				nBytes--;
			}
		} else {
			if ((chr & 0xC0) != 0x80) {
			return false;
			}
			nBytes--;
		}
	}

	if (nBytes > 0 || bAllAscii) {
		return false;
	}
	return true;
}

static void createFromHex(char *buf, int maxlen, const char *str)
{
	const char *pos = str;
	int len = 0;
	int val;

	while(*pos){
		if (len == maxlen)
			break;
		val = hex2byte(pos);
	if (val < 0) {
			val = hex2num(*pos);
			if (val < 0)
				break;
			buf[len++] = val;
	} else {
			buf[len++] = val;
			pos += 2;
		}
	}
}

static int createToHex(char *buf, int buf_size, const char *str, int len)
{
	size_t i;
	char *pos = buf, *end = buf + buf_size;
	int ret;
	if (buf_size == 0)
		return 0;
	for (i = 0; i < len; i++) {
		ret = snprintf(pos, end - pos, "%02x", str[i]);
		if (ret < 0 || ret >= end - pos) {
			end[-1] = '\0';
			return pos - buf;
		}
		pos += ret;
	}
	end[-1] = '\0';
	return pos - buf;
}
static void parseScanResults(String16& str, const char *reply)
{
	unsigned int lineBeg = 0, lineEnd = 0;
	size_t  replyLen = strlen(reply);
	char    *pos = NULL;
	char    bssid[BUF_SIZE] = {0};
	char    ssid[BUF_SIZE] = {0};
	char    ssid_txt[BUF_SIZE] ={0};
	bool    isUTF8 = false, isCh = false;
	char    buf[BUF_SIZE] = {0};
	String8 line;

	UConverterType conType = UCNV_UTF8;
	char dest[CONVERT_LINE_LEN] = {0};
	UErrorCode err = U_ZERO_ERROR;
	UConverter* pConverter = ucnv_open(CHARSET_CN, &err);
	if (U_FAILURE(err)) {
		ALOGE("ucnv_open error");
		return;
	}
	/* Parse every line of the reply to construct accessPointObjectItem list */
	for (lineBeg = 0, lineEnd = 0; lineEnd <= replyLen; ++lineEnd) {
		if (lineEnd == replyLen || '\n' == reply[lineEnd]) {
			line.setTo(reply + lineBeg, lineEnd - lineBeg + 1);
			if (DBG)
				ALOGD("%s, line=%s ", __FUNCTION__, line.string());
			if (strncmp(line.string(), "bssid=", 6) == 0) {
				sscanf(line.string() + 6, "%[^\n]", bssid);
			} else if (strncmp(line.string(), "ssid=", 5) == 0) {
				sscanf(line.string() + 5, "%[^\n]", ssid);
				ssid_decode(buf,BUF_SIZE,ssid);
				isUTF8 = isUTF8String(buf,sizeof(buf));
				isCh = false;
				for (pos = buf; '\0' != *pos; pos++) {
					if (0x80 == (*pos & 0x80)) {
						isCh = true;
						break;
					}
				}
				if (DBG)
					ALOGD("%s, ssid = %s, buf = %s,isUTF8= %d, isCh = %d",
						__FUNCTION__, ssid, buf ,isUTF8, isCh);
				if (!isUTF8 && isCh) {
					ucnv_toAlgorithmic(conType, pConverter, dest, CONVERT_LINE_LEN,
								buf, strlen(buf), &err);
					if (U_FAILURE(err)) {
						ALOGE("ucnv_toUChars error");
						goto EXIT;
					}
					ssid_encode(ssid_txt, BUF_SIZE, dest, strlen(dest));
					if (DBG)
						ALOGD("%s, ssid_txt = %s", __FUNCTION__,ssid_txt);
					str += String16("ssid=");
					str += String16(ssid_txt);
					str += String16("\n");
					memset(dest, 0, CONVERT_LINE_LEN);
					memset(ssid_txt, 0, BUF_SIZE);
				} else {
					memset(buf, 0, BUF_SIZE);
					str += String16(line.string());
				}
			} else if (strncmp(line.string(), "====", 4) == 0) {
				if (DBG)
					ALOGD("After sscanf, bssid:%s, ssid:%s, isCh:%d",
						bssid, ssid, isCh);
				if( !isUTF8 && isCh){
					if (DBG)
						ALOGD("add AP Object Item, bssid:%s, ssid:%s, isCh:%d",
							bssid, ssid, isCh);
					addAPObjectItem(buf, bssid, isCh);
					memset(buf, 0, BUF_SIZE);
				}
			}
			if (strncmp(line.string(), "ssid=", 5) != 0)
				str += String16(line.string());
			lineBeg = lineEnd + 1;
		}
	}

EXIT:
	ucnv_close(pConverter);
}

static void constructSsid(String16& str, const char *reply)
{
	size_t  replyLen = strlen(reply);
	char    ssid[BUF_SIZE] = {0};
	char    buf[BUF_SIZE] = {0};
	char    ssid_txt[BUF_SIZE] ={0};
	char    *pos = NULL;
	bool    isUTF8 = false, isCh = false;

	char    dest[CONVERT_LINE_LEN] = {0};
	UConverterType conType = UCNV_UTF8;
	UErrorCode err = U_ZERO_ERROR;
	UConverter*  pConverter = ucnv_open(CHARSET_CN, &err);
	if (U_FAILURE(err)) {
		ALOGE("ucnv_open error");
		return;
	}
	sscanf(reply, "%[^\n]", ssid);
	if (DBG)
		ALOGD("%s, ssid = %s", __FUNCTION__, ssid);
	createFromHex(buf, BUF_SIZE, ssid);
	isUTF8 = isUTF8String(buf, sizeof(buf));
	isCh = false;
	for (pos = buf; '\0' != *pos; pos++) {
		if (0x80 == (*pos & 0x80)) {
			isCh = true;
			break;
		}
	}
	if (!isUTF8 && isCh) {
		ucnv_toAlgorithmic(conType, pConverter, dest, CONVERT_LINE_LEN,
							buf, strlen(buf), &err);
		if (U_FAILURE(err)) {
			ALOGE("ucnv_toUChars error");
			goto EXIT;
		}
		createToHex(ssid_txt, strlen(dest)*2 + 1, dest, strlen(dest));
		if (DBG)
			ALOGD("%s, ssid_txt = %s, dest = %s \n" ,
					__FUNCTION__, ssid_txt, dest);
		str += String16(ssid_txt);
		str += String16("\n");
		memset(dest, 0, CONVERT_LINE_LEN);
		memset(buf, 0, BUF_SIZE);
		memset(ssid_txt, 0, BUF_SIZE);
	} else {
		memset(buf, 0, BUF_SIZE);
		str += String16(reply);
	}

EXIT:
	ucnv_close(pConverter);
}

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
    va_list args;
    va_start(args, fmt);
    int byteCount = vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    if (byteCount < 0 || byteCount >= BUF_SIZE) {
        return JNI_FALSE;
    }
    char reply[BUF_SIZE];
    if (doCommand(ifname, buf, reply, sizeof(reply)) != 0) {
        return JNI_FALSE;
    }
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
	g_pCurrentSSID = new String8();
	if (NULL == g_pCurrentSSID) {
		ALOGE("Failed to allocate memory for g_pCurrentSSID!");
		return JNI_FALSE;
	}
	return (jboolean)(::wifi_load_driver() == 0);
}

static jboolean android_net_wifi_unloadDriver(JNIEnv* env, jobject)
{
	if (g_pCurrentSSID != NULL) {
		delete g_pCurrentSSID;
		g_pCurrentSSID = NULL;
	}
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
			if (NULL != pCurrentNode->bssid) {
				delete pCurrentNode->bssid;
				pCurrentNode->bssid = NULL;
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

static jboolean android_net_wifi_setNetworkVariableCommand(JNIEnv* env,
                                                           jobject,
                                                           jstring jIface,
                                                           jint netId,
                                                           jstring javaName,
                                                           jstring javaValue)
{
	ScopedUtfChars name(env, javaName);
	if (name.c_str() == NULL) {
		return JNI_FALSE;
	}
	ScopedUtfChars value(env, javaValue);
	if (value.c_str() == NULL) {
		return JNI_FALSE;
	}
	ScopedUtfChars ifname(env, jIface);
	if (ifname.c_str() == NULL) {
		return JNI_FALSE;
	}
	if (DBG)
		ALOGD("setNetworkVariableCommand, name:%s, value:%s, netId:%d",
				name.c_str(), value.c_str(), netId);
	struct accessPointObjectItem *pTmpItemNode = NULL;
	if (0 == strcmp(name.c_str(), "bssid")) {
		if (NULL == g_pCurrentSSID) {
			ALOGE("g_pCurrentSSID is NULL");
			g_pCurrentSSID = new String8();
			if (NULL == g_pCurrentSSID) {
				ALOGE("Failed to allocate memory for g_pCurrentSSID!");
				return JNI_FALSE;
			}
		}
		pthread_mutex_lock(g_pItemListMutex);
		pTmpItemNode = g_pItemList;
		if (NULL == pTmpItemNode) {
			ALOGE("g_pItemList is NULL");
		}
		while (pTmpItemNode) {
			if (pTmpItemNode->bssid && (0 == strcmp(pTmpItemNode->bssid->string(),
				value.c_str())) && pTmpItemNode->ssid) {
				*g_pCurrentSSID = *(pTmpItemNode->ssid);
				g_isChSSID = pTmpItemNode->isCh;
				if (DBG)
					ALOGD("Found bssid:%s, g_pCurrentSSID:%s, g_isChSSID:%d",
							pTmpItemNode->bssid->string(), g_pCurrentSSID->string(),
							g_isChSSID);
				break;
			}
			pTmpItemNode = pTmpItemNode->pNext;
		}
		pthread_mutex_unlock(g_pItemListMutex);
		return JNI_TRUE;
	}

	if (0 == strcmp(name.c_str(), "ssid") && g_isChSSID) {
		g_isChSSID = false;
		return doBooleanCommand(ifname.c_str(), "OK", "SET_NETWORK %d %s \"%s\"",
					netId, name.c_str(), g_pCurrentSSID->string());
	}
	return doBooleanCommand(ifname.c_str(), "OK", "SET_NETWORK %d %s %s",
				netId, name.c_str(), value.c_str());
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

    { "setNetworkVariableCommand", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)Z",
            (void*) android_net_wifi_setNetworkVariableCommand },

};

int register_android_net_wifi_WifiManager(JNIEnv* env)
{
    return AndroidRuntime::registerNativeMethods(env,
            WIFI_PKG_NAME, gWifiMethods, NELEM(gWifiMethods));
}

}; // namespace android
