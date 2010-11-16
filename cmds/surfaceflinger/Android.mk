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

ifneq (, $(filter msm7630_surf msm7630_1x msm8660_surf msm8660_csfb msm7630_fusion, $(QCOM_TARGET_PRODUCT)))
LOCAL_SHARED_LIBRARIES += liboverlay
LOCAL_C_INCLUDES += hardware/msm7k/liboverlay
endif

LOCAL_MODULE:= surfaceflinger

include $(BUILD_EXECUTABLE)
