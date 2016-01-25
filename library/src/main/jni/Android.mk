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

include $(CLEAR_VARS)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/mdnsresponder/mDNSPosix \
                    $(LOCAL_PATH)/mdnsresponder/mDNSCore  \
                    $(LOCAL_PATH)/mdnsresponder/mDNSShared \
                    $(LOCAL_PATH)
LOCAL_SRC_FILES := $(commonSources) \
                    JNISupport.c
LOCAL_MODULE := jdns_sd
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := $(commonFlags)
LOCAL_SYSTEM_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES := $(commonLibs)
LOCAL_EXPORT_C_INCLUDE_DIRS := external/mdnsresponder/mDNSShared
include $(BUILD_SHARED_LIBRARY)