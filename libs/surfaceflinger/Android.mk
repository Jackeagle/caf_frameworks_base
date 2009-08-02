LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    clz.cpp.arm \
    DisplayHardware/DisplayHardware.cpp \
    DisplayHardware/DisplayHardwareBase.cpp \
    GPUHardware/GPUHardware.cpp \
    BlurFilter.cpp.arm \
    CPUGauge.cpp \
    Layer.cpp \
    LayerBase.cpp \
    LayerBuffer.cpp \
    LayerBlur.cpp \
    LayerBitmap.cpp \
    LayerDim.cpp \
    LayerOrientationAnim.cpp \
    LayerOrientationAnimRotate.cpp \
    OrientationAnimation.cpp \
    SurfaceFlinger.cpp \
    Tokenizer.cpp \
    Transform.cpp \
    VRamHeap.cpp


# need "-lrt" on Linux simulator to pick up clock_gettime
ifeq ($(TARGET_SIMULATOR),true)
	ifeq ($(HOST_OS),linux)
		LOCAL_LDLIBS += -lrt
	endif
endif


ifneq (, $(filter qsd8250_surf qsd8250_ffa, $(TARGET_PRODUCT)))
  LOCAL_CFLAGS += -DQCOM_SCORPION -DSURF8K
endif

ifneq (, $(filter msm7627_surf msm7627_ffa, $(TARGET_PRODUCT)))
    LOCAL_CFLAGS += -DSURF7X2X
endif

ifeq ($(TARGET_PRODUCT),msm7201a_surf msm7201a_ffa)
    LOCAL_CFLAGS += -DFEATURE_7K_PMEM
endif

ifeq ($(strip $(BOARD_USES_ADRENO_200)), true)
  LOCAL_CFLAGS += -DADRENO_200
endif

LOCAL_SHARED_LIBRARIES := \
	libhardware \
	libutils \
	libcutils \
	libui \
	libcorecg \
	libsgl \
	libpixelflinger \
	libEGL \
	libGLESv1_CM

LOCAL_C_INCLUDES := \
	$(call include-path-for, corecg graphics)

LOCAL_MODULE:= libsurfaceflinger

include $(BUILD_SHARED_LIBRARY)
