LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#Definition to log RGB
LOCAL_CFLAGS := -UOUTPUT_RGB565_LOGGING

LOCAL_SRC_FILES:=                     \
        ColorConverter.cpp            \
        SoftwareRenderer.cpp

LOCAL_C_INCLUDES := \
        $(TOP)/frameworks/base/include/media/stagefright/openmax \
        $(TOP)/frameworks/base/include/media/stagefright         \
        $(TOP)/hardware/msm7k/libgralloc-qsd8k                   \

LOCAL_SHARED_LIBRARIES :=       \
        libbinder               \
        libmedia                \
        libutils                \
        libui                   \
        libcutils               \
        libsurfaceflinger_client\
        libcamera_client

LOCAL_MODULE:= libstagefright_color_conversion

include $(BUILD_SHARED_LIBRARY)
