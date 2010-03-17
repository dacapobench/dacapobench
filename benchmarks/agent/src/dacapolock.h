#ifndef DACAPO_LOCK_H
#define DACAPO_LOCK_H

#include "dacapo.h"

void enterCriticalSection(jrawMonitorID* lock);
void exitCriticalSection(jrawMonitorID* lock);

#endif
