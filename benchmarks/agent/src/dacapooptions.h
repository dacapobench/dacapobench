#ifndef DACAPO_OPTION_LIST_H
#define DACAPO_OPTION_LIST_H

#include "dacapo.h"

#define OPT_CLINIT           "clinit"
#define OPT_STORE_CLASS_FILE "store"
#define OPT_METHOD_EVENTS    "method_events"
#define OPT_METHOD_INSTR     "method_instr"
#define OPT_LOG_START        "log_start"
#define OPT_LOG_STOP         "log_stop"
#define OPT_LOAD_CLASSES     "load_classes"
#define OPT_THREAD           "thread"
#define OPT_ALLOCATE         "allocate"
#define OPT_BREAK            "break"
#define OPT_MONITOR          "monitor"
#define OPT_LOG_FILE         "log_file"
#define OPT_EXCEPTION        "exception"
#define OPT_REPORT_OPTIONS   "report_options"
#define OPT_GC               "gc"
#define OPT_CALL_CHAIN       "call_chain"
#define OPT_LOG_FILE_LIMIT   "log_limit"

_Bool isSelected(const char* option, char* argument);
_Bool hasArgument(const char* option);
void reportOptionsList();
void makeOptionList(const char* options);

#endif
