#ifndef DACAPO_LOCK_H
#define DACAPO_LOCK_H

#include "dacapo.h"

void     rawMonitorEnter(jrawMonitorID* lock);
void     rawMonitorExit(jrawMonitorID* lock);
jboolean rawMonitorWait(jrawMonitorID* lock,jlong timeout);
void     rawMonitorNotify(jrawMonitorID* lock);

#endif
