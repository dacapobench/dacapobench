#ifndef DACAPO_LOG_H
#define DACAPO_LOG_H

#include "dacapo.h"

#include "dacapolock.h"

#define LOG_PREFIX_START                      "START"
#define LOG_PREFIX_STOP                       "STOP"

#define LOG_PREFIX_ALLOCATION                 "HA"
#define LOG_PREFIX_FREE                       "HF"
#define LOG_PREFIX_POINTER                    "HC"
#define LOG_PREFIX_STATIC_POINTER             "HS"

#define LOG_PREFIX_CLASS_PREPARE              "LD"
#define LOG_PREFIX_METHOD_PREPARE             "LM"
#define LOG_PREFIX_CLASS_INITIALIZATION       "CI"

#define LOG_PREFIX_THREAD_START               "TS"
#define LOG_PREFIX_THREAD_STOP                "TE"
#define LOG_PREFIX_THREAD_STATUS              "TA"
#define LOG_PREFIX_THREAD_TIME                "TT"               

#define LOG_PREFIX_METHOD_ENTER               "CS"
#define LOG_PREFIX_METHOD_EXIT                "CE"

#define LOG_PREFIX_MONITOR_ACQUIRE            "MS"
#define LOG_PREFIX_MONITOR_RELEASE            "ME"
#define LOG_PREFIX_MONITOR_NOTIFY             "MN"
#define LOG_PREFIX_MONITOR_CONTENTED_ENTER    "MC"
#define LOG_PREFIX_MONITOR_CONTENTED_ENTERED  "Mc"
#define LOG_PREFIX_MONITOR_WAIT               "MW"
#define LOG_PREFIX_MONITOR_WAITED             "Mw"

#define LOG_PREFIX_VOLATILE                   "VF"
#define LOG_PREFIX_VOLATILE_ACCESS            "VA"

#define LOG_PREFIX_EXCEPTION                  "XT"
#define LOG_PREFIX_EXCEPTION_CATCH            "XC"

#define LOG_PREFIX_TIME                       "TM"

#define LOG_PREFIX_GC                         "GC"
#define LOG_PREFIX_HEAP_REPORT                "HR"

#define LOG_PREFIX_CALL_CHAIN_START           "ES"
#define LOG_PREFIX_CALL_CHAIN_FRAME           "EF"
#define LOG_PREFIX_CALL_CHAIN_STOP            "EE"


extern MonitorLockType       lockLog;
extern MonitorLockType       agentLock;

/* extern FILE*               logFile; */
extern jboolean            logState;

_Bool dacapo_log_init();
_Bool logFileOpen();
void  dacapo_log_stop();

void  callReportHeap(JNIEnv *env);
void  setReportHeap(JNIEnv *env, jboolean flag);
void  setReportCallChain(JNIEnv *env, jlong frequency, jboolean enable);

void  log_field_string(const char* text);
void  log_field_string_n(const char* text, int text_length);
void  log_field_jboolean(jboolean v);
void  log_field_int(int v);
void  log_field_pointer(const void* p);
void  log_field_jlong(jlong v);
void  log_field_long(long v);
void  log_field_current_time();
void  log_thread(jthread thread, jlong thread_tag, jboolean thread_has_new_tag, jobject klass, jlong klass_tag, jboolean klass_has_new_tag);
void  log_class(jobject klass, jlong klass_tag, jboolean klass_has_new_tag);
void  log_field_time(struct timeval* tv);
void  log_eol();

#endif
