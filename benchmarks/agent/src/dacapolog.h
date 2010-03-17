#ifndef DACAPO_LOG_H
#define DACAPO_LOG_H

#include "dacapo.h"

extern jrawMonitorID       lockLog;
extern FILE*               logFile;
extern jboolean            logState;

_Bool dacapo_log_init();

#endif
