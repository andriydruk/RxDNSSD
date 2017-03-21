LOCAL_PATH := $(call my-dir)

commonSources := \
    mDNSShared/dnssd_clientlib.c  \
    mDNSShared/dnssd_clientstub.c \
    mDNSShared/dnssd_ipc.c

commonLibs := libcutils liblog

commonFlags := \
    -O2 -g \
    -fno-strict-aliasing \
    -D_GNU_SOURCE \
    -DHAVE_IPV6 \
    -DNOT_HAVE_SA_LEN \
    -DPLATFORM_NO_RLIMIT \
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

daemonSources := mDNSCore/mDNS.c            \
                 mDNSCore/DNSDigest.c       \
                 mDNSCore/uDNS.c            \
                 mDNSCore/DNSCommon.c       \
                 mDNSShared/uds_daemon.c    \
                 mDNSShared/mDNSDebug.c     \
                 mDNSShared/dnssd_ipc.c     \
                 mDNSShared/GenLinkedList.c \
                 mDNSShared/PlatformCommon.c \
                 mDNSPosix/PosixDaemon.c    \
                 mDNSPosix/mDNSPosix.c      \
                 mDNSPosix/mDNSUNP.c

daemonIncludes := external/mdnsresponder/mDNSCore  \
                  external/mdnsresponder/mDNSShared \
                  external/mdnsresponder/mDNSPosix

#########################

include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  $(daemonSources)
LOCAL_MODULE := mdnsd
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES := $(daemonIncludes)

LOCAL_CFLAGS := \
  $(commonFlags) \
  -DTARGET_OS_LINUX \
  -DMDNS_VERSIONSTR_NODTS=1 \
  -DHAVE_LINUX \
  -DUSES_NETLINK \

LOCAL_STATIC_LIBRARIES := $(commonLibs) libc
LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_INIT_RC := mdnsd.rc
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  $(daemonSources)
LOCAL_MODULE := mdnsd
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES := $(daemonIncludes)

LOCAL_CFLAGS := \
  $(commonFlags) \
  -DMDNS_VERSIONSTR_NODTS=1 \

LOCAL_CFLAGS_linux := -DTARGET_OS_LINUX -DHAVE_LINUX -DUSES_NETLINK
LOCAL_CFLAGS_darwin := -DTARGET_OS_MAC

LOCAL_STATIC_LIBRARIES := $(commonLibs)
include $(BUILD_HOST_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
  mDNSWindows/SystemService/main.c    \
  mDNSWindows/SystemService/Service.c \
  mDNSWindows/Secret.c                \
  mDNSWindows/mDNSWin32.c             \
  mDNSShared/dnssd_ipc.c              \
  mDNSShared/uds_daemon.c             \
  mDNSShared/mDNSDebug.c              \
  mDNSShared/GenLinkedList.c          \
  mDNSShared/DebugServices.c          \
  mDNSCore/DNSDigest.c                \
  mDNSCore/DNSCommon.c                \
  mDNSCore/uDNS.c                     \
  mDNSCore/mDNS.c                     \
  android/windows_firewall_dummy.c

LOCAL_MODULE := mdnsd
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := EXECUTABLES

mdnsd_generated_sources := $(call local-generated-sources-dir)
event_log_h := $(mdnsd_generated_sources)/EventLog.h
event_log_o := $(mdnsd_generated_sources)/EventLog.o
event_log_rc := $(mdnsd_generated_sources)/EventLog.rc

event_log_mc := external/mdnsresponder/mDNSWindows/SystemService/EventLog.mc
mingw_bin := prebuilts/gcc/$(HOST_PREBUILT_TAG)/host/x86_64-w64-mingw32-4.8/bin
windmc := $(mingw_bin)/x86_64-w64-mingw32-windmc
windres := $(mingw_bin)/x86_64-w64-mingw32-windres

$(event_log_h) $(event_log_o): $(event_log_mc)
	@echo "Generating EventLog messages"
	@mkdir -p $(mdnsd_generated_sources)
	@$(windmc) -r$(mdnsd_generated_sources) -h$(mdnsd_generated_sources) $<
	@$(windres) -F pe-i386 -I$(mdnsd_generated_sources) $(event_log_rc) $(event_log_o)

LOCAL_GENERATED_SOURCES := \
  $(mdnsd_generated_sources)/EventLog.h \
  $(mdnsd_generated_sources)/EventLog.o

LOCAL_C_INCLUDES := \
  external/mdnsresponder/mDNSShared \
  external/mdnsresponder/mDNSCore \
  external/mdnsresponder/mDNSWindows/SystemService \
  external/mdnsresponder/mDNSWindows \
  external/mdnsresponder/android/caseMapping \
  $(mdnsd_generated_sources)

LOCAL_CFLAGS := $(commonFlags) \
  -DMDNS_VERSIONSTR_NODTS=1 \
  -DTARGET_OS_WINDOWS \
  -DTARGET_OS_WIN32 \
  -DWIN32 \
  -DNDEBUG \
  -D_CONSOLE \
  -D_WIN32_LEAN_AND_MEAN \
  -DUSE_TCP_LOOPBACK \
  -DPLATFORM_NO_STRSEP \
  -DPLATFORM_NO_EPIPE \
  -DPLATFORM_NO_RLIMIT \
  -DPID_FILE='""' \
  -DUNICODE \
  -D_UNICODE \
  -D_CRT_SECURE_NO_DEPRECATE \
  -D_CRT_SECURE_CPP_OVERLOAD_STANDARD_NAMES=1 \
  -D_LEGACY_NAT_TRAVERSAL \
  -include stdint.h \
  -include winsock2.h \
  -include ws2ipdef.h \
  -include wincrypt.h \
  -include netioapi.h \
  -Wno-sign-compare \
  -Wno-empty-body

LOCAL_LDFLAGS := -lws2_32 -liphlpapi -lpowrprof -lnetapi32 -municode


LOCAL_MODULE_HOST_OS := windows

LOCAL_STATIC_LIBRARIES := $(commonLibs)
include $(BUILD_HOST_EXECUTABLE)
##########################

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(commonSources)
LOCAL_MODULE := libmdnssd
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := $(commonFlags) -DTARGET_OS_LINUX -DHAVE_LINUX -DUSES_NETLINK
LOCAL_SYSTEM_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES := $(commonLibs)
LOCAL_EXPORT_C_INCLUDE_DIRS := external/mdnsresponder/mDNSShared
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(commonSources)
LOCAL_MODULE := libmdnssd
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := $(commonFlags) -DTARGET_OS_LINUX -DHAVE_LINUX -DUSES_NETLINK
LOCAL_STATIC_LIBRARIES := $(commonLibs)
LOCAL_EXPORT_C_INCLUDE_DIRS := external/mdnsresponder/mDNSShared
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(commonSources)
LOCAL_SRC_FILES_windows := mDNSWindows/DLL/dllmain.c
LOCAL_MODULE := libmdnssd
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := $(commonFlags)
LOCAL_CFLAGS_windows := \
  -DTARGET_OS_WINDOWS \
  -DWIN32 \
  -DNDEBUG \
  -D_WINDOWS \
  -D_USERDLL \
  -D_MDNS_DEBUGMSGS=0 \
  -D_WIN32_LEAN_AND_MEAN \
  -D_SSIZE_T \
  -DUSE_TCP_LOOPBACK \
  -D_CRT_SECURE_NO_DEPRECATE \
  -D_CRT_SECURE_CPP_OVERLOAD_STANDARD_NAMES=1 \
  -DNOT_HAVE_SA_LENGTH \
  -Wno-unknown-pragmas \
  -Wno-sign-compare \
  -Wno-overflow \
  -include stdint.h \
  -include winsock2.h \
  -include ws2ipdef.h \
  -include wincrypt.h \
  -include iphlpapi.h \
  -include netioapi.h \
  -include stdlib.h \
  -include stdio.h

LOCAL_CFLAGS_linux := -DTARGET_OS_LINUX -DHAVE_LINUX -DUSES_NETLINK
LOCAL_CFLAGS_darwin := -DTARGET_OS_MAC
LOCAL_STATIC_LIBRARIES := $(commonLibs)
LOCAL_EXPORT_C_INCLUDE_DIRS := external/mdnsresponder/mDNSShared
LOCAL_C_INCLUDES_windows := external/mdnsresponder/mDNSShared external/mdnsresponder/mDNSWindows
LOCAL_C_INCLUDES_windows += external/mdnsresponder/android/caseMapping
LOCAL_MODULE_HOST_OS := darwin linux windows
include $(BUILD_HOST_STATIC_LIBRARY)

############################

include $(CLEAR_VARS)
LOCAL_SRC_FILES := Clients/dns-sd.c Clients/ClientCommon.c
LOCAL_MODULE := dnssd
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := $(commonFlags) -DTARGET_OS_LINUX -DHAVE_LINUX -DUSES_NETLINK
LOCAL_SYSTEM_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES := libmdnssd libcutils liblog
include $(BUILD_EXECUTABLE)

############################
# This builds an mDns that is embeddable within GmsCore for the nearby connections API

### STATIC LIB ###
include $(CLEAR_VARS)

LOCAL_SDK_VERSION := 8
LOCAL_MODULE    := libmdns_jni_static
LOCAL_SRC_FILES :=  /mDNSCore/mDNS.c \
                    /mDNSCore/DNSDigest.c \
                    /mDNSCore/uDNS.c \
                    /mDNSCore/DNSCommon.c \
                    /mDNSPosix/mDNSPosix.c \
                    /mDNSPosix/mDNSUNP.c \
                    /mDNSShared/mDNSDebug.c \
                    /mDNSShared/dnssd_clientlib.c \
                    /mDNSShared/dnssd_clientshim.c \
                    /mDNSShared/dnssd_ipc.c \
                    /mDNSShared/GenLinkedList.c \
                    /mDNSShared/PlatformCommon.c

LOCAL_C_INCLUDES := external/mdnsresponder/mDNSPosix \
                    external/mdnsresponder/mDNSCore  \
                    external/mdnsresponder/mDNSShared

LOCAL_CFLAGS += -Os -fvisibility=hidden
LOCAL_CFLAGS += $(commonFlags) \
                -UMDNS_DEBUGMSGS \
                -DMDNS_DEBUGMSGS=0 \
                -DSO_REUSEADDR \
                -DUNICAST_DISABLED \
                -DTARGET_OS_LINUX \
                -DHAVE_LINUX \
                -DUSES_NETLINK

ifeq ($(TARGET_BUILD_TYPE),debug)
  LOCAL_CFLAGS += -O0 -UNDEBUG -fno-omit-frame-pointer
endif

include $(BUILD_STATIC_LIBRARY)
