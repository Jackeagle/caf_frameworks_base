LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	Camera.cpp \
	CameraParameters.cpp \
	EGLDisplaySurface.cpp \
	EGLNativeWindowSurface.cpp \
	EventHub.cpp \
	EventRecurrence.cpp \
	KeyLayoutMap.cpp \
	KeyCharacterMap.cpp \
	ICamera.cpp \
	ICameraClient.cpp \
	ICameraService.cpp \
	IOverlay.cpp \
	ISurfaceComposer.cpp \
	ISurface.cpp \
	ISurfaceFlingerClient.cpp \
	LayerState.cpp \
	Overlay.cpp \
	PixelFormat.cpp \
	Rect.cpp \
	Region.cpp \
	Surface.cpp \
	SurfaceComposerClient.cpp \
	SurfaceFlingerSynchro.cpp 

ifneq (, $(filter msm7201a_surf msm7201a_ffa, $(TARGET_PRODUCT)))
  LOCAL_CFLAGS += -DSURF7201A
endif

ifneq (, $(filter msm7201a_ffa msm7627_ffa, $(TARGET_PRODUCT)))
  LOCAL_CFLAGS += -DFFA7K
endif

ifneq (, $(filter qsd8650_ffa qsd8250_ffa, $(TARGET_PRODUCT)))
  LOCAL_CFLAGS += -DFFA8K
endif

ifeq ($(BOARD_USE_QCOM_TESTONLY),true)
  LOCAL_CFLAGS += -DQCOM_TEST_ONLY
endif

ifeq ($(strip $(BOARD_USES_ADRENO_200)), true)
  LOCAL_CFLAGS += -DADRENO_200
endif

LOCAL_SHARED_LIBRARIES := \
	libcorecg \
	libcutils \
	libutils \
	libpixelflinger \
	libhardware \
	libhardware_legacy

ifeq ($(TARGET_HAVE_TSLIB),true)
	LOCAL_CFLAGS += -DHAVE_TSLIB
	LOCAL_SHARED_LIBRARIES += libtslib
	LOCAL_C_INCLUDES += external/tslib/src
endif

LOCAL_MODULE:= libui

include $(BUILD_SHARED_LIBRARY)
