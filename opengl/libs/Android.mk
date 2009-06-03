LOCAL_PATH:= $(call my-dir)

#
# Build META EGL library
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= 	\
	EGL/egl.cpp 		\
	EGL/gpu.cpp			\
#

LOCAL_SHARED_LIBRARIES += libcutils libutils libui
LOCAL_LDLIBS := -lpthread -ldl
LOCAL_MODULE:= libEGL

# needed on sim build because of weird logging issues
ifeq ($(TARGET_SIMULATOR),true)
else
    LOCAL_SHARED_LIBRARIES += libdl
    # we need to access the Bionic private header <bionic_tls.h>
    LOCAL_CFLAGS += -I$(LOCAL_PATH)/../../../../bionic/libc/private
endif

LOCAL_CFLAGS += -fvisibility=hidden
ifeq ($(BOARD_USES_ADRENO_200),true)
    LOCAL_CFLAGS += -DHAVE_QCOM_GFX
endif

include $(BUILD_SHARED_LIBRARY)



#
# Build the wrapper OpenGL ES library
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= 	\
	GLES_CM/gl.cpp.arm 		\
#

LOCAL_SHARED_LIBRARIES += libcutils libutils libui libEGL
LOCAL_LDLIBS := -lpthread -ldl
LOCAL_MODULE:= libGLESv1_CM

# needed on sim build because of weird logging issues
ifeq ($(TARGET_SIMULATOR),true)
else
    LOCAL_SHARED_LIBRARIES += libdl
    # we need to access the Bionic private header <bionic_tls.h>
    LOCAL_CFLAGS += -I$(LOCAL_PATH)/../../../../bionic/libc/private
endif

LOCAL_CFLAGS += -fvisibility=hidden

ifneq (, $(filter msm7630_surf msm7630_ffa qsd8250_surf qsd8250_ffa, $(TARGET_PRODUCT)))
  LOCAL_CFLAGS += -DCOPROC_TLS=1
  endif
ifeq ($(BOARD_USES_ADRENO_200),true)
    LOCAL_CFLAGS += -DHAVE_QCOM_GFX
endif
  
include $(BUILD_SHARED_LIBRARY)
