#ifndef DACAPO_LOG_H
#define DACAPO_LOG_H

#include "dacapo.h"

#define LOG_PREFIX_ALLOCATION                 "HA"
#define LOG_PREFIX_FREE                       "HF"

#define LOG_PREFIX_CLASS_PREPARE              "LD"
#define LOG_PREFIX_METHOD_PREPARE             "LM"

#define LOG_PREFIX_THREAD_START               "TS"
#define LOG_PREFIX_THREAD_STOP                "TE"

#define LOG_PREFIX_METHOD_ENTER               "CS"
#define LOG_PREFIX_METHOD_EXIT                "CE"

#define LOG_PREFIX_MONITOR_AQUIRE             "MS"
#define LOG_PREFIX_MONITOR_RELEASE            "ME"
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

extern jrawMonitorID       lockLog;
extern jrawMonitorID       agentLock;

extern FILE*               logFile;
extern jboolean            logState;

_Bool dacapo_log_init();

void  callReportHeap(JNIEnv *env);
void  setReportHeap(JNIEnv *env);
void  setReportCallChain(JNIEnv *env, jlong frequency, jboolean enable);


void  log_field_string(const char* text);
void  log_field_string_n(const char* text, int text_length);
void  log_field_jboolean(jboolean v);
void  log_field_int(int v);
void  log_field_pointer(const void* p);
void  log_field_jlong(jlong v);
void  log_field_long(long v);
void  log_field_time();
void  log_eol();

#endif
