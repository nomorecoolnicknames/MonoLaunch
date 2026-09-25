LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := MonoLaunch
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current
LOCAL_OVERRIDES_PACKAGES := Home Launcher2 Launcher3 Launcher3QuickStep Trebuchet TrebuchetQuickStep

LOCAL_SRC_FILES := $(call all-java-files-under, app/src/main/java)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/app/src/main/res
LOCAL_MANIFEST_FILE := app/src/main/AndroidManifest.xml

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)
