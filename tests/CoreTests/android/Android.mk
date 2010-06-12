LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := \
	$(call all-subdir-java-files)

ifeq ($(BUILD_FUSION), true)
LOCAL_SRC_FILES += \
	$(call all-java-files-under, ../telephony_fusion/com)
else
LOCAL_SRC_FILES += \
	$(call all-java-files-under, ../com)
endif

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_PACKAGE_NAME := CoreTests

include $(BUILD_PACKAGE)
