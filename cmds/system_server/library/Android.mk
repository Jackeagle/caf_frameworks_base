LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	system_init.cpp

base = $(LOCAL_PATH)/../../..

LOCAL_C_INCLUDES := \
	$(base)/camera/libcameraservice \
	$(base)/libs/audioflinger \
	$(base)/libs/surfaceflinger \
	$(base)/media/libmediaplayerservice \
	$(JNI_H_INCLUDE)

LOCAL_SHARED_LIBRARIES := \
	libandroid_runtime \
	libsurfaceflinger \
	libaudioflinger \
    libcameraservice \
    libmediaplayerservice \
	libutils \
	libbinder \
	libcutils

ifneq (, $(filter msm7630_surf msm7630_1x msm8660_surf msm8660_csfb msm7630_fusion, $(QCOM_TARGET_PRODUCT)))
LOCAL_SHARED_LIBRARIES += liboverlay
LOCAL_C_INCLUDES += hardware/msm7k/liboverlay
endif

LOCAL_MODULE:= libsystem_server

include $(BUILD_SHARED_LIBRARY)
