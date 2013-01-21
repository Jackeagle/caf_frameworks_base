LOCAL_PATH:= $(call my-dir)
WFD_DISABLE_PLATFORM_LIST := msm8610 msm8226

# the library
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
            $(call all-subdir-java-files) \
	    com/android/server/EventLogTags.logtags \
	    com/android/server/am/EventLogTags.logtags

ifeq ($(BOARD_HAVE_BLUETOOTH_BLUEZ), true)
    LOCAL_SRC_FILES := $(filter-out \
                        com/android/server/BluetoothManagerService.java \
                        ,$(LOCAL_SRC_FILES))
endif

LOCAL_MODULE:= services

ifeq ($(call is-vendor-board-platform,QCOM),true)
ifneq ($(call is-board-platform-in-list,$(WFD_DISABLE_PLATFORM_LIST)),true)

LOCAL_STATIC_JAVA_LIBRARIES := WfdCommon

endif #DISABLE PLATFORMS
endif #TARGET_USES_WFD

LOCAL_JAVA_LIBRARIES := android.policy telephony-common

include $(BUILD_JAVA_LIBRARY)

include $(BUILD_DROIDDOC)
