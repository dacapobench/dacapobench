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

#define LOG_PREFIX_EXCEPTION                  "XT"
#define LOG_PREFIX_EXCEPTION_CATCH            "XC"

extern jrawMonitorID       lockLog;
extern FILE*               logFile;
extern jboolean            logState;

_Bool dacapo_log_init();

void  log_field_string(const char* text);
void  log_field_string_n(const char* text, int text_length);
void  log_field_jboolean(jboolean v);
void  log_field_int(int v);
void  log_field_pointer(const void* p);
void  log_field_jlong(jlong v);
void  log_field_long(long v);
void  log_eol();

#endif
