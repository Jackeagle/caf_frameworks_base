LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	main_surfaceflinger.cpp 

LOCAL_SHARED_LIBRARIES := \
	libsurfaceflinger \
	libbinder \
	libutils

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../../libs/surfaceflinger

ifeq ($(TARGET_USES_SF_BYPASS),true)
LOCAL_CFLAGS += -DSF_BYPASS
LOCAL_SHARED_LIBRARIES += liboverlay
LOCAL_C_INCLUDES += hardware/msm7k/liboverlay
endif

LOCAL_MODULE:= surfaceflinger

include $(BUILD_EXECUTABLE)
