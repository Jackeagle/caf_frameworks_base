/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * wpa_supplicant/hostapd / common helper functions, etc.
 * Copyright (c) 2002-2007, Jouni Malinen <j@w1.fi>
 *
 * This software may be distributed under the terms of the BSD license.
 * See README for more details.
 */

#define LOG_TAG "wifi_gbk2utf"

#include "jni.h"
#include "android_net_wifi_Gbk2Utf.h"

#define BUF_SIZE 256
#define CONVERT_LINE_LEN 2048
#define CHARSET_CN ("gbk")

namespace android {

static jint DBG = false;

struct accessPointObjectItem *g_pItemList = NULL;
struct accessPointObjectItem *g_pLastNode = NULL;
pthread_mutex_t *g_pItemListMutex = NULL;
String8 *g_pCurrentSSID = NULL;
bool g_isSbMs = false;
bool g_isInList = false;

static void addAPObjectItem(const char *ssid, const char *bssid)
{
    if (NULL == ssid || NULL == bssid) {
        ALOGE("ssid or bssid is NULL");
        return;
    }

    struct accessPointObjectItem *pTmpItemNode = NULL;
    struct accessPointObjectItem *pItemNode = NULL;
    bool foundItem = false;
    bool foundSbMs = false;
    pthread_mutex_lock(g_pItemListMutex);
    pTmpItemNode = g_pItemList;
    while (pTmpItemNode) {
        if (pTmpItemNode->bssid && (*(pTmpItemNode->bssid) == bssid)) {
            if (pTmpItemNode->ssid && (*(pTmpItemNode->ssid) == ssid)) {
                foundItem = true;
                break;
            } else {
                foundSbMs = true;
                pTmpItemNode->isSbMs = true;
            }
        }
        pTmpItemNode = pTmpItemNode->pNext;
    }
    if (foundItem) {
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
        if (foundSbMs)
            pItemNode->isSbMs = true;
        else
            pItemNode->isSbMs = false;
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

/* parse SSID string encoded from wpa_supplicant to normal string  */
static size_t ssid_decode(char *buf, size_t maxlen, const char *str)
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

/* This function can be used to convert SSIDs into printable form. Since wifi
 * framework layer needs to parse printable form string.
*/
static void ssid_encode(char *txt, size_t maxlen, const char *data, unsigned int len)
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

/* check if the SSID string is UTF coded */
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

static size_t createToHex(char *buf, size_t buf_size, const char *str, unsigned int len)
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

void parseScanResults(String16& str, const char *reply)
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
                        ALOGD("add AP Object Item, bssid:%s, ssid:%s",
                            bssid, ssid);
                    addAPObjectItem(buf, bssid);
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

void constructSsid(String16& str, const char *reply)
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

jboolean setNetworkVariable(char *buf)
{
    struct accessPointObjectItem *pTmpItemNode = NULL;
    char pos[BUF_SIZE] = {0};
    bool isCh = false;
    int i;

    unsigned int netId;
    char name[BUF_SIZE] = {0};
    char value[BUF_SIZE] = {0};

    /* parse SET_NETWORK command*/
    sscanf(buf + 12 , "%d %s %s", &netId, name, value);

    if (DBG)
        ALOGD("parse SET_NETWORK command success, netId = %d, name = %s, value =%s",
               netId, name, value);

    if (strncmp(name, "bssid", 5) == 0) {
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
                value)) && pTmpItemNode->ssid) {
                g_isInList = true;
                if (!pTmpItemNode->isSbMs) {
                    *g_pCurrentSSID = *(pTmpItemNode->ssid);
                     if (DBG)
                         ALOGD("Found bssid:%s, g_pCurrentSSID:%s, g_isSbMs:%d",
                                pTmpItemNode->bssid->string(), g_pCurrentSSID->string(),
                                g_isSbMs);
                } else {
                    g_isSbMs = true;
                    if (DBG)
                        ALOGD("Multi SSIDs Single BSSID case. bssid:%s",pTmpItemNode->bssid->string());
                }
                break;
            }
            pTmpItemNode = pTmpItemNode->pNext;
        }
        pthread_mutex_unlock(g_pItemListMutex);
    }

    if (0 == strncmp(name, "ssid", 4) && g_isInList) {
        g_isInList = false;
        isCh = false;
        memcpy(pos,value,strlen(value));
        for (i = 0; i < strlen(value); i++) {
            if (0x80 == (pos[i] & 0x80)) {
                isCh = true;
                break;
            }
        }
        if (DBG)
            ALOGD("setNetworkVariableCommand: isCh:%d",isCh);
        if (isCh) {
           if (!g_isSbMs) {
               snprintf(buf, BUF_SIZE, "SET_NETWORK %d ssid \"%s\"", netId, g_pCurrentSSID->string());
               if (DBG)
                   ALOGD("new SET_NETWORK command is: %s", buf);
            } else {
                g_isSbMs = false;
                UChar dest[CONVERT_LINE_LEN] = {0};
                UErrorCode err = U_ZERO_ERROR;
                UConverter* pConverter = ucnv_open("utf-8", &err);
                char ssid_gbk[CONVERT_LINE_LEN] = {0};
                char ssid[CONVERT_LINE_LEN] = {0};
                if (U_FAILURE(err)) {
                    ALOGE("ucnv_open error");
                    return JNI_FALSE;
                }
                int len = ucnv_toUChars(pConverter, dest, CONVERT_LINE_LEN,
                                        value, strlen(value), &err);
                if (U_FAILURE(err)) {
                    ALOGE("ucnv_toUChars error");
                    ucnv_close(pConverter);
                    return JNI_FALSE;
                }
                ucnv_close(pConverter);
                pConverter = ucnv_open(CHARSET_CN, &err);
                if (U_FAILURE(err)) {
                    ALOGE("ucnv_open error");
                    return JNI_FALSE;
                }
                ucnv_fromUChars(pConverter, ssid_gbk, CONVERT_LINE_LEN, dest, len, &err);
                if (U_FAILURE(err)) {
                    ALOGE("ucnv_toUChars error");
                    ucnv_close(pConverter);
                    return JNI_FALSE;
                }
                ucnv_close(pConverter);
                /* strip off quotation mark */
                memcpy(ssid, ssid_gbk + 1, strlen(ssid_gbk) - 2);
                if (DBG)
                    ALOGD("%s, ssid_gbk = %s, ssid = %s", __FUNCTION__,ssid_gbk,ssid);
                snprintf(buf, BUF_SIZE, "SET_NETWORK %d ssid \"%s\"", netId, ssid);
                if (DBG)
                    ALOGD("new SET_NETWORK command is: %s", buf);
            }
        }
    }
    return JNI_TRUE;
}

} //namespace android
