LOCAL_PATH := $(call my-dir)

commonSources := \
    mdnsresponder/mDNSShared/dnssd_clientlib.c  \
    mdnsresponder/mDNSShared/dnssd_clientstub.c \
    mdnsresponder/mDNSShared/dnssd_ipc.c

commonLibs := libcutils liblog

commonFlags := \
    -O2 -g \
    -fno-strict-aliasing \
    -D_GNU_SOURCE \
    -DHAVE_IPV6 \
    -DHAVE_LINUX \
    -DNOT_HAVE_SA_LEN \
    -DPLATFORM_NO_RLIMIT \
    -DTARGET_OS_LINUX \
    -DUSES_NETLINK \
    -DMDNS_DEBUGMSGS=0 \
    -DMDNS_UDS_SERVERPATH=\"/dev/socket/mdnsd\" \
    -DMDNS_USERNAME=\"mdnsr\" \
    -W \
    -Wall \
    -Wextra \
    -Wno-array-bounds \
    -Wno-pointer-sign \
    -Wno-unused \
    -Wno-unused-but-set-variable \
    -Wno-unused-parameter \
    -Werror=implicit-function-declaration \

### STATIC LIB ###
include $(CLEAR_VARS)

LOCAL_SDK_VERSION := 8
LOCAL_MODULE    := jdns_sd
LOCAL_SRC_FILES :=  mdnsresponder/mDNSCore/mDNS.c \
                    mdnsresponder/mDNSCore/DNSDigest.c \
                    mdnsresponder/mDNSCore/uDNS.c \
                    mdnsresponder/mDNSPosix/mDNSPosix.c \
                    mdnsresponder/mDNSPosix/mDNSUNP.c \
                    mdnsresponder/mDNSShared/mDNSDebug.c \
                    mdnsresponder/mDNSShared/dnssd_clientlib.c \
                    mdnsresponder/mDNSShared/dnssd_clientshim.c \
                    mdnsresponder/mDNSShared/dnssd_ipc.c \
                    mdnsresponder/mDNSShared/GenLinkedList.c \
                    mdnsresponder/mDNSShared/PlatformCommon.c \
                    mdnsresponder/mDNSCore/DNSCommon.c \
                    mdnsresponder/mDNSPosix/PosixDaemon.c \
                    mdnsresponder/mDNSShared/uds_daemon.c \
                    JNISupport.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/mdnsresponder/mDNSPosix \
                    $(LOCAL_PATH)/mdnsresponder/mDNSCore  \
                    $(LOCAL_PATH)/mdnsresponder/mDNSShared \
                    $(LOCAL_PATH)

LOCAL_CFLAGS += -Os -fvisibility=hidden
LOCAL_CFLAGS += $(commonFlags) \
                -UMDNS_DEBUGMSGS \
                -DMDNS_DEBUGMSGS=0 \
                -DSO_REUSEADDR \
                -DUNICAST_DISABLED \
                -DMDNS_VERSIONSTR_NODTS=1 \
                -DAUTO_CALLBACKS=1 \
                -DEMBEDDED

LOCAL_MODULE_TAGS := optional
LOCAL_SYSTEM_SHARED_LIBRARIES := libc libcutils
LOCAL_SHARED_LIBRARIES := $(commonLibs)
LOCAL_EXPORT_C_INCLUDE_DIRS := external/mdnsresponder/mDNSShared

ifeq ($(TARGET_BUILD_TYPE),debug)
  LOCAL_CFLAGS += -O0 -UNDEBUG -fno-omit-frame-pointer
endif

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)

#------------------------------------------------------------


#include $(CLEAR_VARS)
#LOCAL_C_INCLUDES := $(LOCAL_PATH)/mdnsresponder/mDNSPosix \
#                    $(LOCAL_PATH)/mdnsresponder/mDNSCore  \
#                    $(LOCAL_PATH)/mdnsresponder/mDNSShared \
#                    $(LOCAL_PATH)
#LOCAL_SRC_FILES := $(commonSources) \
#                    JNISupport.c
#LOCAL_MODULE := jdns_sd
#LOCAL_MODULE_TAGS := optional
#LOCAL_CFLAGS := $(commonFlags)
#LOCAL_SYSTEM_SHARED_LIBRARIES := libc
#LOCAL_SHARED_LIBRARIES := $(commonLibs)
#LOCAL_EXPORT_C_INCLUDE_DIRS := external/mdnsresponder/mDNSShared
#include $(BUILD_SHARED_LIBRARY)